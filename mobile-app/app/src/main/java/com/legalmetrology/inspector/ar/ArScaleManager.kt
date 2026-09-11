package com.legalmetrology.inspector.ar

import android.content.Context
import android.util.Log
import com.google.ar.core.Frame
import com.google.ar.core.HitResult
import com.google.ar.core.TrackingState
import com.legalmetrology.inspector.domain.model.ArScaleMetadata
import com.legalmetrology.inspector.domain.model.ScaleSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.pow
import kotlin.math.sqrt

private const val TAG = "ArScaleManager"

/**
 * ArScaleManager — the core of the metric calibration subsystem.
 *
 * WHAT IT DOES:
 * ARCore provides camera intrinsics (focal length in pixels) and 3D hit-test
 * results (real-world surface coordinates). Combined, these let us derive:
 *
 *   Physical Height (mm) = pixelHeight × D_meters × 1000 / fy
 *
 * where:
 *   - D_meters = Euclidean distance from camera to detected surface plane
 *   - fy = Y-axis focal length from ARCore ImageIntrinsics (in pixels)
 *   - pixelHeight = bounding box height from YOLOv8 detection
 *
 * This mm/px ratio is bundled with each captured frame and sent to the backend
 * so it can convert YOLO pixel measurements to physical millimetre heights for
 * font-size compliance checking.
 *
 * FALLBACK:
 * If ARCore is not available on the device, the officer places a standard
 * 1-rupee Indian coin (physical diameter = 22mm) in the frame. The coin's
 * pixel width is measured by the backend YOLO model, giving:
 *   mmPerPixel = 22.0 / coinWidthPixels
 *
 * TODO — BACKEND INTEGRATION:
 * When ARCore scale metadata is sent to the backend, attach it to the
 * InspectionSubmitRequest DTO as `arScaleMetadata`. The backend uses it to
 * multiply YOLO bounding box pixel heights by mmPerPixel to get real-world mm.
 */
@Singleton
class ArScaleManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    // --- Observable state ---

    sealed class TrackingQuality {
        object Searching : TrackingQuality()
        data class Tracking(val distanceMeters: Double, val mmPerPixel: Double) : TrackingQuality()
        object Lost : TrackingQuality()
    }

    private val _trackingQuality = MutableStateFlow<TrackingQuality>(TrackingQuality.Searching)
    val trackingQuality: StateFlow<TrackingQuality> = _trackingQuality.asStateFlow()

    private val _lastScaleMetadata = MutableStateFlow<ArScaleMetadata?>(null)
    val lastScaleMetadata: StateFlow<ArScaleMetadata?> = _lastScaleMetadata.asStateFlow()

    // Smoothing: keep a rolling average over the last N frames to reduce jitter
    private val distanceHistory = ArrayDeque<Double>(maxSize = 10)
    private val SMOOTHING_WINDOW = 8

    /**
     * Called on each AR frame update (60fps on capable devices).
     * DO NOT trigger heavy UI recompositions here — only update state
     * on meaningful quality transitions.
     *
     * @param frame         The current ARCore Frame
     * @param hitResults    Hit-test results from raycasting the center of the viewport
     *                      against the detected plane (populated by SceneView's hit-test)
     */
    fun onArFrame(frame: Frame, hitResults: List<HitResult>) {
        val camera = frame.camera
        if (camera.trackingState != TrackingState.TRACKING) {
            if (_trackingQuality.value !is TrackingQuality.Searching) {
                _trackingQuality.value = TrackingQuality.Searching
            }
            return
        }

        val hitResult = hitResults.firstOrNull() ?: run {
            // Camera is tracking but no surface hit — still show as searching
            if (_trackingQuality.value !is TrackingQuality.Searching) {
                _trackingQuality.value = TrackingQuality.Searching
            }
            return
        }

        val scaleMetadata = computeScaleMetadata(frame, hitResult)

        // Smooth distance with rolling average
        if (distanceHistory.size >= SMOOTHING_WINDOW) distanceHistory.removeFirst()
        distanceHistory.addLast(scaleMetadata.distanceMeters)
        val smoothedDistance = distanceHistory.average()

        val intrinsics = frame.camera.imageIntrinsics
        val smoothedMmPerPixel = smoothedDistance * 1000.0 / intrinsics.focalLength[1]

        val smoothedMetadata = scaleMetadata.copy(
            distanceMeters = smoothedDistance,
            mmPerPixel = smoothedMmPerPixel
        )

        _lastScaleMetadata.value = smoothedMetadata
        _trackingQuality.value = TrackingQuality.Tracking(smoothedDistance, smoothedMmPerPixel)

        Log.d(TAG, "AR Scale: D=${String.format("%.3f", smoothedDistance)}m, " +
                "mmPerPx=${String.format("%.4f", smoothedMmPerPixel)}, " +
                "fy=${intrinsics.focalLength[1]}")
    }

    /**
     * Compute the metric scale from a single frame and hit result.
     *
     * The pinhole camera model gives us:
     *   physicalHeight_mm = pixelHeight × D_meters × 1000 / fy
     *
     * Rearranged for the conversion factor:
     *   mmPerPixel = D_meters × 1000 / fy
     */
    fun computeScaleMetadata(frame: Frame, hitResult: HitResult): ArScaleMetadata {
        val surfacePose = hitResult.hitPose
        val cameraPose = frame.camera.pose

        // Euclidean distance from camera origin to hit point on surface
        val surfaceTranslation = surfacePose.translation
        val cameraTranslation = cameraPose.translation
        val distanceMeters = sqrt(
            (surfaceTranslation[0] - cameraTranslation[0]).toDouble().pow(2) +
            (surfaceTranslation[1] - cameraTranslation[1]).toDouble().pow(2) +
            (surfaceTranslation[2] - cameraTranslation[2]).toDouble().pow(2)
        )

        val intrinsics = frame.camera.imageIntrinsics
        val fx = intrinsics.focalLength[0]
        val fy = intrinsics.focalLength[1]

        // The critical formula: mm/pixel conversion factor
        // This is what the backend uses to convert YOLO pixel heights to real mm
        val mmPerPixel = distanceMeters * 1000.0 / fy.toDouble()

        return ArScaleMetadata(
            distanceMeters = distanceMeters,
            focalLengthX = fx,
            focalLengthY = fy,
            mmPerPixel = mmPerPixel,
            source = ScaleSource.ARCORE
        )
    }

    /**
     * Coin fallback: user places a standard 1-rupee Indian coin in the frame.
     * The coin has a known physical diameter of 22mm.
     * The backend YOLO model detects the coin and returns its pixel width.
     *
     * TODO — BACKEND INTEGRATION:
     * The backend should detect the coin in the frame and return coinWidthPx.
     * For now, this is called with a user-entered pixel width from a manual overlay.
     *
     * @param coinWidthPx Width of the coin in the photo, in pixels
     * @return mmPerPixel conversion factor
     */
    fun computeCoinFallbackScale(coinWidthPx: Float): ArScaleMetadata {
        val RUPEE_COIN_DIAMETER_MM = 22.0
        val mmPerPixel = RUPEE_COIN_DIAMETER_MM / coinWidthPx.toDouble()

        Log.d(TAG, "Coin fallback: coinWidthPx=$coinWidthPx, mmPerPx=$mmPerPixel")

        return ArScaleMetadata(
            distanceMeters = 0.0,     // Unknown without ARCore
            focalLengthX = 0f,        // Unknown without ARCore
            focalLengthY = 0f,        // Unknown without ARCore
            mmPerPixel = mmPerPixel,
            source = ScaleSource.COIN_FALLBACK
        )
    }

    fun reset() {
        distanceHistory.clear()
        _trackingQuality.value = TrackingQuality.Searching
        _lastScaleMetadata.value = null
    }
}


