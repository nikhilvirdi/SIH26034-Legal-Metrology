package com.legalmetrology.inspector.ui.screens.ecommerce

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.legalmetrology.inspector.data.api.InspectionSubmitRequest
import com.legalmetrology.inspector.data.api.MockInspectionApiService
import com.legalmetrology.inspector.ui.theme.Amber500
import com.legalmetrology.inspector.ui.theme.Indigo500
import com.legalmetrology.inspector.ui.theme.Navy800
import com.legalmetrology.inspector.ui.theme.Navy900
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * ECommerceScreen — screenshot-based inspection for online listings.
 *
 * Per the 2017 amendment, e-commerce listings must carry:
 * - Manufacturer's name and address
 * - Country of origin
 * - Generic name
 * - Net quantity
 * - Best-before date (if applicable)
 * - MRP
 * - Dimensions (if relevant)
 *
 * NOTE: Manufacturing date is explicitly NOT required on online listings.
 * AR-based scale calibration is skipped entirely (screenshot has no 3D depth).
 * Font-size measurement is therefore NOT applicable for this path.
 *
 * TODO — BACKEND INTEGRATION:
 * 1. User selects screenshot from gallery (ActivityResultContracts.GetContent)
 * 2. Upload image to backend via InspectionRepository.submitECommerceInspection()
 * 3. Backend runs PaddleOCR/Qwen2.5-VL on the screenshot
 * 4. No YOLO bounding box measurement (no AR scale)
 * 5. Rule engine checks e-commerce-specific field list
 */
@Composable
fun ECommerceScreen(
    onProceedToReport: (String) -> Unit,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val mockApiService = remember { MockInspectionApiService() }

    var screenshotSelected by remember { mutableStateOf(false) }
    var isAnalysing by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var contentVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { contentVisible = true }

    fun analyseScreenshot() {
        scope.launch {
            isAnalysing = true
            val steps = listOf(0.3f, 0.6f, 0.85f, 1.0f)
            steps.forEach { p ->
                progress = p
                delay(600L)
            }
            mockApiService.submitInspection(
                InspectionSubmitRequest(
                    packageType = "RETAIL",
                    commodityCategory = "FOOD",
                    commodityName = null,
                    officerId = "officer_001",
                    arScaleMetadata = null,
                    isOnlineListingInspection = true
                )
            )
            onProceedToReport(UUID.randomUUID().toString())
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Navy900, Navy800)))
    ) {
        AnimatedVisibility(
            visible = contentVisible && !isAnalysing,
            enter = fadeIn() + slideInVertically { it / 4 }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                    Column(modifier = Modifier.padding(start = 8.dp)) {
                        Text(
                            "E-Commerce Inspection",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "2017 Amendment — Online Listing Check",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                // Info card: what's checked
                Card(
                    colors = CardDefaults.cardColors(containerColor = Amber500.copy(0.1f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(modifier = Modifier.padding(16.dp)) {
                        Icon(Icons.Default.Info, null, tint = Amber500, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                "E-Commerce Rules (2017 Amendment)",
                                style = MaterialTheme.typography.labelMedium,
                                color = Amber500,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Checks: manufacturer name & address, country of origin, " +
                                "generic name, net quantity, MRP, best-before date.\n" +
                                "Skipped: manufacturing date, font-size measurement (no AR scale on screenshots).",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                // Screenshot upload area
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .border(
                            2.dp,
                            if (screenshotSelected) Indigo500 else MaterialTheme.colorScheme.outline,
                            RoundedCornerShape(16.dp)
                        )
                        .background(
                            if (screenshotSelected) Indigo500.copy(0.08f) else Navy800,
                            RoundedCornerShape(16.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .padding(24.dp)
                            // TODO: Replace with Activity result launcher for gallery picker
                            // val pickImage = rememberLauncherForActivityResult(
                            //     ActivityResultContracts.GetContent()
                            // ) { uri -> ... }
                    ) {
                        if (!screenshotSelected) {
                            Icon(
                                Icons.Default.AddPhotoAlternate,
                                null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "Tap to select a screenshot",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "Of an Amazon, Flipkart, or other listing page",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.6f)
                            )
                            Spacer(Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    // TODO: Launch gallery picker here
                                    screenshotSelected = true  // Mock: instantly "selected"
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Indigo500),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.AddPhotoAlternate, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Select from Gallery")
                            }
                        } else {
                            // Mock selected screenshot placeholder
                            Icon(
                                Icons.Default.Language,
                                null,
                                modifier = Modifier.size(48.dp),
                                tint = Indigo500
                            )
                            Spacer(Modifier.height(8.dp))
                            Text("amazon_listing_screenshot.jpg", style = MaterialTheme.typography.labelMedium, color = Indigo500)
                            Text("1080 × 2400 px", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                Spacer(Modifier.weight(1f))

                Button(
                    onClick = { analyseScreenshot() },
                    enabled = screenshotSelected,
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Indigo500)
                ) {
                    Icon(Icons.Default.CloudUpload, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Analyse Listing Compliance")
                }

                Spacer(Modifier.height(20.dp))
            }
        }

        // Analysing overlay
        if (isAnalysing) {
            Box(
                modifier = Modifier.fillMaxSize().background(Navy900.copy(0.95f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    CircularProgressIndicator(color = Indigo500, modifier = Modifier.size(56.dp))
                    Spacer(Modifier.height(20.dp))
                    Text("Analysing listing…", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "OCR reading · Checking e-commerce rules",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(20.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth(),
                        color = Indigo500
                    )
                }
            }
        }
    }
}
