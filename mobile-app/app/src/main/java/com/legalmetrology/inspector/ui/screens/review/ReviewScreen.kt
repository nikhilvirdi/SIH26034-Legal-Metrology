package com.legalmetrology.inspector.ui.screens.review

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.legalmetrology.inspector.data.api.InspectionSubmitRequest
import com.legalmetrology.inspector.data.api.MockInspectionApiService
import com.legalmetrology.inspector.ui.theme.Emerald500
import com.legalmetrology.inspector.ui.theme.Indigo500
import com.legalmetrology.inspector.ui.theme.Navy800
import com.legalmetrology.inspector.ui.theme.Navy900
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * ReviewScreen — shows captured photos before submission.
 *
 * Features:
 * - Thumbnail grid of captured frames
 * - Barcode detection status (ML Kit result)
 * - AR scale metadata summary
 * - Submit → mock backend analysis
 *
 * TODO — BACKEND INTEGRATION:
 * Replace mock submission with InspectionRepository.submitInspection():
 *   1. Bundle photoFiles + arScaleMetadata
 *   2. POST as multipart/form-data to backend
 *   3. Store the InspectionReportResponse in Room via InspectionRepository
 *   4. Navigate to ReportScreen with the stored inspectionId
 */
@Composable
fun ReviewScreen(
    inspectionId: String,
    onSubmitForAnalysis: (String) -> Unit,
    onRetakePhoto: () -> Unit,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val mockApiService = remember { MockInspectionApiService() }

    var isSubmitting by remember { mutableStateOf(false) }
    var uploadProgress by remember { mutableFloatStateOf(0f) }
    var contentVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { contentVisible = true }

    // Mock photo placeholders — in real app these are captured JPEG file URIs
    val capturedPhotos = listOf(
        PhotoSlot("Front Panel", "📦"),
        PhotoSlot("Back Panel", "📋"),
        PhotoSlot("Side Panel", "🔖")
    )

    fun submitForAnalysis() {
        scope.launch {
            isSubmitting = true

            // Simulate upload progress
            // TODO: Replace with real multipart upload progress tracking via OkHttp
            val uploadSteps = listOf(
                0.2f to "Uploading photos…",
                0.5f to "Running YOLOv8 detection…",
                0.7f to "Reading text with PaddleOCR…",
                0.85f to "Checking against Legal Metrology Rules…",
                1.0f to "Generating report…"
            )
            for ((progress, _) in uploadSteps) {
                uploadProgress = progress
                delay(500L)
            }

            // Mock API call
            // TODO: Replace with real InspectionRepository.submitInspection()
            mockApiService.submitInspection(
                InspectionSubmitRequest(
                    packageType = "RETAIL",
                    commodityCategory = "FOOD",
                    commodityName = "Potato Chips",
                    officerId = "officer_001",
                    arScaleMetadata = null // TODO: Pass real ArScaleMetadata
                )
            )

            onSubmitForAnalysis(inspectionId)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Navy900, Navy800)))
    ) {
        AnimatedVisibility(
            visible = contentVisible && !isSubmitting,
            enter = fadeIn() + slideInVertically { it / 4 }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp)
            ) {
                // Top bar
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                    Column(modifier = Modifier.padding(start = 8.dp)) {
                        Text(
                            "Review Photos",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Verify all panels are captured clearly",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                // Photo grid
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(capturedPhotos.size) { i ->
                        val photo = capturedPhotos[i]
                        PhotoThumbnail(slot = photo)
                    }
                }

                Spacer(Modifier.height(20.dp))

                // Barcode detection card
                // TODO: Replace with real ML Kit barcode scan result
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Emerald500.copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.BarChart,
                            null,
                            tint = Emerald500,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                "Barcode Detected",
                                style = MaterialTheme.typography.labelMedium,
                                color = Emerald500,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "8901058861514  ·  EAN-13",  // Mock barcode
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.weight(1f))
                        Icon(Icons.Default.CheckCircle, null, tint = Emerald500)
                    }
                }

                Spacer(Modifier.height(12.dp))

                // AR scale info card
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Indigo500.copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                "AR Scale Calibrated",
                                style = MaterialTheme.typography.labelMedium,
                                color = Indigo500,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "Distance: 0.42m  ·  0.089 mm/px",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "Label area est: 320 cm²  →  Requires ≥2.5mm numerals",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(Modifier.weight(1f))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onRetakePhoto,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Refresh, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Retake")
                    }

                    Button(
                        onClick = { submitForAnalysis() },
                        modifier = Modifier
                            .weight(2f)
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Indigo500)
                    ) {
                        Icon(Icons.Default.CloudUpload, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Analyse & Check Compliance", style = MaterialTheme.typography.labelMedium)
                    }
                }

                Spacer(Modifier.height(16.dp))
            }
        }

        // Submitting overlay
        if (isSubmitting) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Navy900.copy(alpha = 0.95f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    CircularProgressIndicator(color = Indigo500, modifier = Modifier.size(56.dp))
                    Spacer(Modifier.height(24.dp))
                    Text(
                        "Analysing…",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "YOLOv8 detecting fields · OCR reading text · Checking rules",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(20.dp))
                    LinearProgressIndicator(
                        progress = { uploadProgress },
                        modifier = Modifier.fillMaxWidth(),
                        color = Indigo500
                    )
                }
            }
        }
    }
}

data class PhotoSlot(val label: String, val emoji: String)

@Composable
private fun PhotoThumbnail(slot: PhotoSlot) {
    Box(
        modifier = Modifier
            .aspectRatio(0.75f)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1A1F2E))
            .border(2.dp, Emerald500.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(slot.emoji, style = MaterialTheme.typography.displayMedium.copy(fontSize = 32.sp))
            Spacer(Modifier.height(4.dp))
            Text(
                slot.label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
                .background(Emerald500, RoundedCornerShape(20.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Icon(Icons.Default.CheckCircle, null, tint = Color.White, modifier = Modifier.size(12.dp))
        }
    }
}
