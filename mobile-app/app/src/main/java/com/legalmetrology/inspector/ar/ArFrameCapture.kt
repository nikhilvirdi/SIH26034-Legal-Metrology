package com.legalmetrology.inspector.ar

import android.graphics.Bitmap
import android.media.Image
import android.util.Log
import com.google.ar.core.Frame
import com.legalmetrology.inspector.domain.model.ArScaleMetadata
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "ArFrameCapture"

/**
 * ArFrameCapture — captures a full-resolution still image from the AR camera
 * and bundles it with the frozen ARCore spatial metadata.
 *
 * WHY THIS IS NECESSARY:
 * The AR camera stream runs at 30–60fps with varying focus, exposure, and
 * tracking quality. We need to:
 *   1. Wait for TrackingState.TRACKING to be stable
 *   2. Capture a single full-resolution JPEG (not a compressed preview frame)
 *   3. Freeze the camera intrinsics (focal length fx, fy) AT THE MOMENT OF CAPTURE
 *   4. Bundle the mm/px scale factor with the image so the backend can use it
 *
 * If we used intrinsics from a different frame than the captured image, the
 * physical measurement would be incorrect.
 *
 * TODO — BACKEND INTEGRATION:
 * CapturedFrame is what gets sent to the backend. The bundle includes:
 *   - The compressed JPEG bytes (or file URI)
 *   - ArScaleMetadata with mmPerPixel, focalLength, and distance
 * Backend uses mmPerPixel to convert YOLO bounding box pixel heights to mm.
 */
@Singleton
class ArFrameCapture @Inject constructor() {

    data class CapturedFrame(
        val jpegFile: File,                    // Full-resolution JPEG file
        val scaleMetadata: ArScaleMetadata,    // Frozen at capture time
        val captureTimestampMs: Long,
        val imageWidthPx: Int,
        val imageHeightPx: Int
    )

    /**
     * Capture a full-resolution frame from an ARCore Frame.
     *
     * IMPORTANT: Must be called from the GL thread (inside the ARCore session's
     * onDrawFrame callback) because Frame.acquireCameraImage() is only valid
     * during the current frame update.
     *
     * @param frame         The current ARCore Frame (must be TRACKING state)
     * @param scaleMetadata The frozen metadata from ArScaleManager for this frame
     * @param outputDir     Directory to write the JPEG file
     * @param photoIndex    Index of this photo (front=0, back=1, side=2)
     */
    fun captureFrame(
        frame: Frame,
        scaleMetadata: ArScaleMetadata,
        outputDir: File,
        photoIndex: Int = 0
    ): CapturedFrame? {
        return try {
            // acquireCameraImage() returns the full-resolution sensor image
            // (not the compressed preview). Must be closed after use.
            val cameraImage: Image = frame.acquireCameraImage()

            val bitmap = cameraImage.toBitmap()
            cameraImage.close()

            val fileName = "inspection_photo_${photoIndex}_${System.currentTimeMillis()}.jpg"
            val outputFile = File(outputDir, fileName)
            outputFile.parentFile?.mkdirs()

            FileOutputStream(outputFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
            }
            bitmap.recycle()

            Log.d(TAG, "Captured frame $photoIndex: ${outputFile.absolutePath}, " +
                    "size=${outputFile.length()/1024}KB, " +
                    "mmPerPx=${scaleMetadata.mmPerPixel}")

            CapturedFrame(
                jpegFile = outputFile,
                scaleMetadata = scaleMetadata,
                captureTimestampMs = System.currentTimeMillis(),
                imageWidthPx = cameraImage.width,
                imageHeightPx = cameraImage.height
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to capture AR frame", e)
            null
        }
    }

    /**
     * Convert an android.media.Image (YUV_420_888 format from ARCore) to Bitmap.
     *
     * ARCore camera images come in YUV_420_888 format. We convert to RGB Bitmap
     * for JPEG compression and eventual upload to the backend.
     */
    private fun Image.toBitmap(): Bitmap {
        // Simple YUV→RGB conversion via ImageDecoder approach
        // For ARCore, the camera image is YUV_420_888
        val yBuffer = planes[0].buffer
        val uBuffer = planes[1].buffer
        val vBuffer = planes[2].buffer

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)
        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)

        // Use RenderScript-free YUV to JPEG via YuvImage
        val yuvImage = android.graphics.YuvImage(
            nv21,
            android.graphics.ImageFormat.NV21,
            width,
            height,
            null
        )
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(android.graphics.Rect(0, 0, width, height), 100, out)
        val jpegBytes = out.toByteArray()

        return android.graphics.BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size)
    }
}
