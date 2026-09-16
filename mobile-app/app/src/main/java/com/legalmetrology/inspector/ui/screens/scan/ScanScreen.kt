package com.legalmetrology.inspector.ui.screens.scan

import android.Manifest
import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.os.Build
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.InfiniteTransition
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.legalmetrology.inspector.ar.ArScaleManager
import com.legalmetrology.inspector.ui.theme.ArGlassPanel
import com.legalmetrology.inspector.ui.theme.ArReticleTint
import com.legalmetrology.inspector.ui.theme.ArSearchTint
import com.legalmetrology.inspector.ui.theme.Emerald500
import com.legalmetrology.inspector.ui.theme.Indigo500
import com.legalmetrology.inspector.ui.theme.Navy900
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * ScanScreen — the core AR scanning experience.
 *
 * THREE STATES:
 *   1. SEARCHING — ARCore is detecting surfaces. Pulsing indigo reticle.
 *   2. LOCKED    — AR surface detected & stable. Green ring, haptic feedback.
 *   3. CAPTURED  — Photo taken. Brief green flash animation.
 *
 * AR INTEGRATION:
 * We use SceneView's ArSceneView (a Compose-compatible wrapper around ARCore).
 * The ArScaleManager processes each frame to derive the mm/px ratio.
 *
 * IMPORTANT: This screen requires CAMERA permission.
 * Accompanist Permissions handles the runtime request.
 *
 * COIN FALLBACK:
 * If ARCore is unavailable (device not supported), the user is prompted to
 * place a 1-rupee coin (22mm diameter) in the frame before capturing.
 * The coin's pixel size lets us derive the same mm/px ratio without ARCore.
 *
 * TODO — BACKEND INTEGRATION:
 * After capture, the photo files + ArScaleMetadata are bundled and sent
 * to the backend via InspectionRepository.submitInspection().
 * For now, a mock inspectionId is generated locally.
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ScanScreen(
    packageType: String,
    category: String,
    onProceedToReview: (String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)

    // Scan state machine
    var scanState by remember { mutableStateOf<ScanState>(ScanState.Searching) }
    var photosCaptures by remember { mutableIntStateOf(0) }
    val maxPhotos = 3 // Front, back, side

    // AR tracking quality (would come from ArScaleManager in real integration)
    // For demo: simulate tracking lock after 3 seconds
    var trackingQuality by remember {
        mutableStateOf<ArScaleManager.TrackingQuality>(ArScaleManager.TrackingQuality.Searching)
    }

    LaunchedEffect(Unit) {
        // Request camera permission
        if (!cameraPermission.status.isGranted) {
            cameraPermission.launchPermissionRequest()
        }

        // Simulate AR tracking lock for demo
        // TODO: Replace with actual ArScaleManager.trackingQuality.collectAsState()
        // when SceneView ArSceneView composable is integrated
        delay(3000L)
        trackingQuality = ArScaleManager.TrackingQuality.Tracking(0.42, 0.089)
        scanState = ScanState.Locked(
            distanceMeters = 0.42,
            mmPerPixel = 0.089,
            labelAreaCm2 = 320.0
        )
    }

    fun capturePhoto() {
        scope.launch {
            // Haptic feedback on capture
            vibrate(context)

            scanState = ScanState.Captured
            photosCaptures++

            delay(800L)

            if (photosCaptures >= maxPhotos) {
                // All photos captured — proceed to review
                // TODO: Pass real inspection ID from repository when wiring backend
                val mockInspectionId = UUID.randomUUID().toString()
                onProceedToReview(mockInspectionId)
            } else {
                // Reset for next shot
                scanState = ScanState.Locked(0.42, 0.089, 320.0)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Navy900)) {

        if (!cameraPermission.status.isGranted) {
            // Camera permission not granted
            CameraPermissionRequest(
                onRequestPermission = { cameraPermission.launchPermissionRequest() }
            )
        } else {
            // ─── AR Camera View ───
            // TODO: Replace this placeholder with actual SceneView ArSceneView composable:
            //
            //   ArSceneView(
            //     modifier = Modifier.fillMaxSize(),
            //     onSessionCreated = { session -> arScaleManager.initSession(session) },
            //     onFrame = { arFrame ->
            //       val hitResults = arFrame.hitTest(0.5f, 0.5f)
            //       arScaleManager.onArFrame(arFrame, hitResults)
            //     }
            //   )
            //
            // The SceneView library version 2.2.1 provides ArSceneView as a composable.
            // Integration: https://github.com/SceneView/sceneview-android

            // Camera placeholder (dark + grid lines to simulate viewfinder)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF0D1117))
            ) {
                // Grid lines (rule of thirds guide)
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val thirdW = size.width / 3f
                    val thirdH = size.height / 3f
                    val lineColor = Color.White.copy(alpha = 0.08f)
                    drawLine(lineColor, Offset(thirdW, 0f), Offset(thirdW, size.height), 1f)
                    drawLine(lineColor, Offset(thirdW * 2, 0f), Offset(thirdW * 2, size.height), 1f)
                    drawLine(lineColor, Offset(0f, thirdH), Offset(size.width, thirdH), 1f)
                    drawLine(lineColor, Offset(0f, thirdH * 2), Offset(size.width, thirdH * 2), 1f)
                }
            }

            // ─── AR Overlays (stacked on top of camera view) ───
            ArScanOverlay(scanState = scanState)

            // ─── Top HUD ───
            TopHud(
                packageType = packageType,
                category = category,
                scanState = scanState,
                onBack = onBack
            )

            // ─── Bottom Controls ───
            BottomControls(
                scanState = scanState,
                photosCaptures = photosCaptures,
                maxPhotos = maxPhotos,
                onCapture = { capturePhoto() },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Scan State
// ─────────────────────────────────────────────────────────────

sealed class ScanState {
    object Searching : ScanState()
    data class Locked(
        val distanceMeters: Double,
        val mmPerPixel: Double,
        val labelAreaCm2: Double
    ) : ScanState()
    object Captured : ScanState()
}

// ─────────────────────────────────────────────────────────────
// AR Scan Overlay (reticle + pulse ring + lock indicator)
// ─────────────────────────────────────────────────────────────

@Composable
private fun ArScanOverlay(scanState: ScanState) {
    val infiniteTransition = rememberInfiniteTransition(label = "ar_pulse")

    // Pulsing animation for "searching" state
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    // Rotating scan line for "searching" state
    val scanRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing)
        ),
        label = "scan_rotation"
    )

    val reticleColor = when (scanState) {
        is ScanState.Locked  -> ArReticleTint  // Emerald green when locked
        is ScanState.Captured -> Emerald500
        else                  -> ArSearchTint   // Indigo when searching
    }

    val reticleAlpha by animateFloatAsState(
        targetValue = if (scanState is ScanState.Captured) 0.4f else 1f,
        animationSpec = tween(300),
        label = "reticle_alpha"
    )

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Outer pulse ring (searching only)
        if (scanState is ScanState.Searching) {
            Canvas(
                modifier = Modifier
                    .size(220.dp)
                    .alpha(0.4f)
            ) {
                drawCircle(
                    color = ArSearchTint,
                    radius = size.minDimension / 2f * pulseScale,
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }

        // Main reticle: corner brackets
        Canvas(
            modifier = Modifier
                .size(180.dp)
                .alpha(reticleAlpha)
        ) {
            val cornerLen = size.width * 0.2f
            val strokeWidth = 3.dp.toPx()
            val padding = 0f

            // Top-left corner
            drawLine(reticleColor, Offset(padding, padding), Offset(padding + cornerLen, padding), strokeWidth, StrokeCap.Round)
            drawLine(reticleColor, Offset(padding, padding), Offset(padding, padding + cornerLen), strokeWidth, StrokeCap.Round)

            // Top-right corner
            drawLine(reticleColor, Offset(size.width - padding, padding), Offset(size.width - padding - cornerLen, padding), strokeWidth, StrokeCap.Round)
            drawLine(reticleColor, Offset(size.width - padding, padding), Offset(size.width - padding, padding + cornerLen), strokeWidth, StrokeCap.Round)

            // Bottom-left corner
            drawLine(reticleColor, Offset(padding, size.height - padding), Offset(padding + cornerLen, size.height - padding), strokeWidth, StrokeCap.Round)
            drawLine(reticleColor, Offset(padding, size.height - padding), Offset(padding, size.height - padding - cornerLen), strokeWidth, StrokeCap.Round)

            // Bottom-right corner
            drawLine(reticleColor, Offset(size.width - padding, size.height - padding), Offset(size.width - padding - cornerLen, size.height - padding), strokeWidth, StrokeCap.Round)
            drawLine(reticleColor, Offset(size.width - padding, size.height - padding), Offset(size.width - padding, size.height - padding - cornerLen), strokeWidth, StrokeCap.Round)

            // Scanning arc (searching state)
            if (scanState is ScanState.Searching) {
                drawArc(
                    color = ArSearchTint.copy(alpha = 0.6f),
                    startAngle = scanRotation,
                    sweepAngle = 90f,
                    useCenter = false,
                    topLeft = Offset(padding + strokeWidth, padding + strokeWidth),
                    size = Size(size.width - padding * 2 - strokeWidth * 2, size.height - padding * 2 - strokeWidth * 2),
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }

        // Locked: full circle lock indicator
        if (scanState is ScanState.Locked) {
            Canvas(modifier = Modifier.size(200.dp)) {
                drawCircle(
                    color = ArReticleTint.copy(alpha = 0.15f),
                    radius = size.minDimension / 2f
                )
                drawCircle(
                    color = ArReticleTint,
                    radius = size.minDimension / 2f,
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }

        // Captured: flash overlay
        AnimatedVisibility(
            visible = scanState is ScanState.Captured,
            enter = fadeIn(tween(50)),
            exit = fadeOut(tween(600))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = 0.35f))
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Top HUD
// ─────────────────────────────────────────────────────────────

@Composable
private fun TopHud(
    packageType: String,
    category: String,
    scanState: ScanState,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .background(ArGlassPanel, CircleShape)
                    .size(40.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
            }

            // Package type + category badge
            Card(
                colors = CardDefaults.cardColors(containerColor = ArGlassPanel),
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        packageType.lowercase().replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White
                    )
                    Text(
                        " · $category",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }

            // AR status indicator
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = when (scanState) {
                        is ScanState.Locked   -> ArReticleTint.copy(alpha = 0.2f)
                        is ScanState.Captured -> Emerald500.copy(alpha = 0.2f)
                        else                   -> ArGlassPanel
                    }
                ),
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                when (scanState) {
                                    is ScanState.Locked   -> ArReticleTint
                                    is ScanState.Captured -> Emerald500
                                    else                   -> Color.Yellow
                                },
                                CircleShape
                            )
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = when (scanState) {
                            is ScanState.Locked   -> "AR Locked"
                            is ScanState.Captured -> "Captured"
                            else                   -> "Searching..."
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                }
            }
        }

        // Distance + scale readout (only when locked)
        if (scanState is ScanState.Locked) {
            Spacer(Modifier.height(8.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = ArGlassPanel),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    MetricReadout("Distance", "${String.format("%.2f", scanState.distanceMeters)}m")
                    MetricReadout("Scale", "${String.format("%.3f", scanState.mmPerPixel)}mm/px")
                    MetricReadout("Label Area", "${scanState.labelAreaCm2.toInt()}cm²")
                }
            }
        }
    }
}

@Composable
private fun MetricReadout(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.6f))
        Text(value, style = MaterialTheme.typography.labelMedium, color = ArReticleTint, fontWeight = FontWeight.Bold)
    }
}

// ─────────────────────────────────────────────────────────────
// Bottom Controls
// ─────────────────────────────────────────────────────────────

@Composable
private fun BottomControls(
    scanState: ScanState,
    photosCaptures: Int,
    maxPhotos: Int,
    onCapture: () -> Unit,
    modifier: Modifier = Modifier
) {
    val photoLabels = listOf("Front", "Back", "Side")

    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(bottom = 32.dp, start = 24.dp, end = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Photo progress indicators
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            (0 until maxPhotos).forEach { i ->
                PhotoProgressDot(
                    label = photoLabels.getOrElse(i) { "Photo ${i+1}" },
                    isCaptured = i < photosCaptures,
                    isCurrent = i == photosCaptures
                )
            }
        }

        // Instruction text
        Text(
            text = when {
                scanState is ScanState.Searching ->
                    "Point camera at the package — detecting surface…"
                scanState is ScanState.Locked && photosCaptures == 0 ->
                    "Surface locked ✓  Capture the FRONT of the package"
                scanState is ScanState.Locked && photosCaptures == 1 ->
                    "Now capture the BACK of the package"
                scanState is ScanState.Locked && photosCaptures == 2 ->
                    "Finally, capture the SIDE panel"
                else -> "Processing…"
            },
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.85f),
            modifier = Modifier
                .background(ArGlassPanel, RoundedCornerShape(8.dp))
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )

        Spacer(Modifier.height(20.dp))

        // Shutter button
        Box(contentAlignment = Alignment.Center) {
            // Outer ring
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(
                        if (scanState is ScanState.Locked) ArReticleTint.copy(0.3f)
                        else Color.White.copy(0.1f),
                        CircleShape
                    )
            )
            // Inner shutter button
            Button(
                onClick = onCapture,
                enabled = scanState is ScanState.Locked,
                modifier = Modifier.size(64.dp),
                shape = CircleShape,
                contentPadding = PaddingValues(0.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (scanState is ScanState.Locked) Color.White else Color.White.copy(0.4f),
                    disabledContainerColor = Color.White.copy(0.3f)
                )
            ) {
                Icon(
                    if (scanState is ScanState.Captured) Icons.Default.Check else Icons.Default.CameraAlt,
                    contentDescription = "Capture",
                    tint = if (scanState is ScanState.Locked) Navy900 else Color.White.copy(0.6f),
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Composable
private fun PhotoProgressDot(label: String, isCaptured: Boolean, isCurrent: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(if (isCurrent) 12.dp else 8.dp)
                .background(
                    when {
                        isCaptured -> Emerald500
                        isCurrent  -> Color.White
                        else        -> Color.White.copy(0.3f)
                    },
                    CircleShape
                )
        )
        Spacer(Modifier.height(4.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isCurrent) Color.White else Color.White.copy(0.5f),
            fontSize = 9.sp
        )
    }
}

// ─────────────────────────────────────────────────────────────
// Camera permission request
// ─────────────────────────────────────────────────────────────

@Composable
private fun CameraPermissionRequest(onRequestPermission: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().background(Navy900),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(Icons.Default.Camera, null, modifier = Modifier.size(64.dp), tint = Indigo500)
            Spacer(Modifier.height(16.dp))
            Text("Camera Required", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(
                "Camera access is needed to inspect product labels. Grant access to proceed.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(24.dp))
            Button(onClick = onRequestPermission, colors = ButtonDefaults.buttonColors(containerColor = Indigo500)) {
                Text("Grant Camera Permission")
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Haptic feedback helper
// ─────────────────────────────────────────────────────────────

private fun vibrate(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        val vibrator = vibratorManager.defaultVibrator
        vibrator.vibrate(VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE))
    } else {
        @Suppress("DEPRECATION")
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        @Suppress("DEPRECATION")
        vibrator.vibrate(80)
    }
}
