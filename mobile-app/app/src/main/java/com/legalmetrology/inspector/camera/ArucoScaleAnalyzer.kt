package com.legalmetrology.inspector.camera

import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.imgproc.Imgproc
import org.opencv.objdetect.ArucoDetector
import org.opencv.objdetect.DetectorParameters
import org.opencv.objdetect.Objdetect
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt

private const val TAG = "ArucoScaleAnalyzer"

// ─────────────────────────────────────────────────────────────
// Result wrapper
// ─────────────────────────────────────────────────────────────

/**
 * Sealed result type emitted by [ArucoScaleAnalyzer] on every camera frame.
 *
 * - [NotFound] — no ArUco marker is visible in the frame.
 * - [Tilted]   — a marker was detected but its geometry fails the perspective
 *                sanity check (camera too oblique / marker not flat-on).
 * - [Valid]    — marker detected and geometry is within tolerance; the
 *                [mmPerPixel] value is safe to use for measurement.
 */
sealed interface ArucoResult {
    /** No marker detected in this frame. */
    object NotFound : ArucoResult

    /**
     * Marker detected, but the quadrilateral is too distorted for reliable
     * scale measurement. Opposite sides differ by more than [TILT_TOLERANCE]
     * or the quad is not sufficiently square.
     */
    object Tilted : ArucoResult

    /**
     * Marker detected and geometry passes all sanity checks.
     *
     * @param mmPerPixel Physical millimetres per screen pixel, derived from
     *                   the known marker perimeter (160 mm for a 40 × 40 mm
     *                   DICT_6X6_100 marker) divided by the measured pixel
     *                   perimeter.
     */
    data class Valid(val mmPerPixel: Double) : ArucoResult
}

// ─────────────────────────────────────────────────────────────
// Analyzer
// ─────────────────────────────────────────────────────────────

/**
 * ArucoScaleAnalyzer — Deterministic 2D scale calibration via ArUco markers
 * with built-in perspective-distortion rejection.
 *
 * ## How it works
 * 1. Extract the Y-plane (grayscale) from the CameraX [ImageProxy] directly —
 *    no YUV→RGB conversion needed, saving ~4× the memory bandwidth.
 * 2. Detect ArUco markers with OpenCV's [ArucoDetector].
 * 3. **Perspective sanity check** — the four detected corners must satisfy:
 *    - *Parallelism*: top width ≈ bottom width AND left height ≈ right height.
 *      A trapezoid means the camera is angled toward one edge.
 *    - *Aspect ratio*: avgWidth ≈ avgHeight.
 *      A rectangle means the camera is rolled or the marker is not square-on.
 *    Both checks use [TILT_TOLERANCE] (15 %).
 * 4. If the check passes, compute `mmPerPixel = knownPerimeterMm / pixelPerimeter`
 *    and emit [ArucoResult.Valid]; otherwise emit [ArucoResult.Tilted].
 *
 * ## Physical marker spec
 * DICT_6X6_100, physically printed at **exactly 40 mm × 40 mm**.
 * Total perimeter = 160 mm.
 *
 * @param onResult Called on every frame from the background analysis thread.
 *                 Post to the main thread before touching Compose state.
 */
class ArucoScaleAnalyzer(
    private val onResult: (ArucoResult) -> Unit
) : ImageAnalysis.Analyzer {

    // ── Constants ────────────────────────────────────────────

    /** Total perimeter of the physical 40 mm × 40 mm marker (4 × 40). */
    private val knownPerimeterMm = 160.0

    /**
     * Maximum allowed fractional difference between opposite sides or between
     * average width and average height before the frame is considered tilted.
     *
     * 0.15 = 15 % → allows for natural hand-shake without false positives,
     * but rejects oblique angles above ~25–30° from flat.
     */
    private val TILT_TOLERANCE = 0.15

    // ── OpenCV objects (created once, reused per frame) ──────

    private val dictionary = Objdetect.getPredefinedDictionary(Objdetect.DICT_6X6_100)
    private val detectorParams = DetectorParameters()
    private val detector = ArucoDetector(dictionary, detectorParams)

    // ── ImageAnalysis.Analyzer ───────────────────────────────

    override fun analyze(image: ImageProxy) {
        try {
            processFrame(image)
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error during frame analysis", e)
            onResult(ArucoResult.NotFound)
        } finally {
            // CRITICAL: always close to unblock the CameraX pipeline.
            image.close()
        }
    }

    // ── Private helpers ──────────────────────────────────────

    private fun processFrame(image: ImageProxy) {
        // ── 1. Build grayscale Mat from the Y-plane ──────────
        // The Y-plane of a YUV_420_888 image IS the grayscale image —
        // no conversion required.
        val yBuffer = image.planes[0].buffer
        val yArray = ByteArray(yBuffer.remaining()).also { yBuffer.get(it) }
        val grayMat = Mat(image.height, image.width, CvType.CV_8UC1)
        grayMat.put(0, 0, yArray)

        val corners = ArrayList<Mat>()
        val ids = Mat()

        try {
            // ── 2. Detect ArUco markers ───────────────────────
            detector.detectMarkers(grayMat, corners, ids)

            if (corners.isEmpty()) {
                onResult(ArucoResult.NotFound)
                return
            }

            // Use the first detected marker only.
            val markerCorners = corners[0]

            // ── 3. Extract corners ────────────────────────────
            // OpenCV returns corners in clockwise order:
            //   index 0 = Top-Left  (TL)
            //   index 1 = Top-Right (TR)
            //   index 2 = Bottom-Right (BR)
            //   index 3 = Bottom-Left  (BL)
            val p0 = markerCorners.pointAt(0)   // TL
            val p1 = markerCorners.pointAt(1)   // TR
            val p2 = markerCorners.pointAt(2)   // BR
            val p3 = markerCorners.pointAt(3)   // BL

            // ── 4. Compute the four edge lengths in pixels ────
            val widthTop    = dist(p0, p1)  // TL → TR
            val heightRight = dist(p1, p2)  // TR → BR
            val widthBottom = dist(p2, p3)  // BR → BL
            val heightLeft  = dist(p3, p0)  // BL → TL

            val avgWidth  = (widthTop    + widthBottom) / 2.0
            val avgHeight = (heightLeft  + heightRight) / 2.0

            // ── 5. Perspective sanity checks ──────────────────
            //
            // Check A — Parallelism (trapezoid rejection):
            //   |topWidth − bottomWidth| / max(topWidth, bottomWidth) < tolerance
            //   |leftHeight − rightHeight| / max(leftHeight, rightHeight) < tolerance
            //
            // A large difference means the camera is pitched toward one edge,
            // making the far edge appear shorter than the near edge.
            val isWidthParallel  = ratio(widthTop,    widthBottom) < TILT_TOLERANCE
            val isHeightParallel = ratio(heightLeft,  heightRight) < TILT_TOLERANCE

            // Check B — Aspect ratio (square check):
            //   |avgWidth − avgHeight| / max(avgWidth, avgHeight) < tolerance
            //
            // A large difference means the camera is rolled, the marker is
            // photographed at a skewed angle, or the printed marker is not square.
            val isSquare = ratio(avgWidth, avgHeight) < TILT_TOLERANCE

            if (!isWidthParallel || !isHeightParallel || !isSquare) {
                Log.d(
                    TAG, "Tilt detected — " +
                        "wTop=${f(widthTop)} wBot=${f(widthBottom)} " +
                        "hLeft=${f(heightLeft)} hRight=${f(heightRight)} " +
                        "widthParallel=$isWidthParallel " +
                        "heightParallel=$isHeightParallel " +
                        "square=$isSquare"
                )
                onResult(ArucoResult.Tilted)
                return
            }

            // ── 6. Compute mm/pixel and emit Valid ────────────
            val points = MatOfPoint2f(p0, p1, p2, p3)
            val pixelPerimeter = Imgproc.arcLength(points, true)
            points.release()

            if (pixelPerimeter <= 0.0) {
                onResult(ArucoResult.NotFound)
                return
            }

            val mmPerPixel = knownPerimeterMm / pixelPerimeter

            Log.d(
                TAG, "Valid — perimeter=${f(pixelPerimeter)}px  " +
                    "mmPerPixel=${String.format("%.4f", mmPerPixel)}"
            )

            onResult(ArucoResult.Valid(mmPerPixel))

        } finally {
            // Always release OpenCV Mats to avoid native memory leaks.
            grayMat.release()
            ids.release()
            corners.forEach { it.release() }
        }
    }

    // ── Tiny utilities ───────────────────────────────────────

    /** Euclidean distance between two OpenCV Points. */
    private fun dist(a: Point, b: Point): Double {
        val dx = a.x - b.x
        val dy = a.y - b.y
        return sqrt(dx * dx + dy * dy)
    }

    /**
     * Fractional asymmetry between two lengths:
     *   |a − b| / max(a, b)
     * Returns 0 when the values are equal, 1 when one is zero.
     */
    private fun ratio(a: Double, b: Double): Double {
        val m = max(a, b)
        return if (m == 0.0) 0.0 else abs(a - b) / m
    }

    /** Read the (row=0, col=index) entry of a 1-row corners Mat as a Point. */
    private fun Mat.pointAt(index: Int): Point {
        val xy = this.get(0, index)
        return Point(xy[0], xy[1])
    }

    /** Short decimal formatter for log output. */
    private fun f(v: Double) = String.format("%.1f", v)
}
