package com.legalmetrology.inspector.ui.screens.report

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.RemoveCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.legalmetrology.inspector.data.api.FieldVerdictDto
import com.legalmetrology.inspector.data.api.MockInspectionApiService
import com.legalmetrology.inspector.ui.theme.Amber500
import com.legalmetrology.inspector.ui.theme.Blue500
import com.legalmetrology.inspector.ui.theme.Coral500
import com.legalmetrology.inspector.ui.theme.Emerald500
import com.legalmetrology.inspector.ui.theme.Indigo500
import com.legalmetrology.inspector.ui.theme.Navy700
import com.legalmetrology.inspector.ui.theme.Navy800
import com.legalmetrology.inspector.ui.theme.Navy900

/**
 * ReportScreen — per-field compliance verdict display.
 *
 * Shows:
 * - Overall compliance summary (PASS/FAIL with count)
 * - Manufacturer registry check result
 * - Per-field verdict cards (rule citation, measured vs required mm)
 * - Export as PDF button
 * - Share / flag pending button
 *
 * TODO — BACKEND INTEGRATION:
 * Load the report from Room (stored after API response) via:
 *   val report by viewModel.inspection.collectAsState()
 * Currently uses mock data from MockInspectionApiService.
 */
@Composable
fun ReportScreen(
    inspectionId: String,
    onViewHistory: () -> Unit,
    onNewInspection: () -> Unit,
    onBack: () -> Unit
) {
    // TODO: Load from ViewModel/Room using inspectionId
    // For demo, re-generate the same mock report
    val mockReport = remember {
        listOf(
            FieldVerdictDto("MRP", "FAIL", "₹42.00 (incl. all taxes)", 1.8, 2.5,
                "Rule 7, Table (100–500 cm²) — G.S.R. 629(E), 2017", 0.94f,
                "Numeral height 1.8mm is below the required 2.5mm for this label area."),
            FieldVerdictDto("NET_QUANTITY", "PASS", "26g", 3.2, 2.5,
                "Rule 6(1)(c), Rule 7", 0.97f, null),
            FieldVerdictDto("MANUFACTURER_DETAILS", "PASS", "PepsiCo India Holdings Pvt. Ltd., Gurugram",
                null, null, "Rule 6(1)(a)", 0.89f, null),
            FieldVerdictDto("GENERIC_NAME", "PASS", "Potato Chips",
                null, null, "Rule 6(1)(b)", 0.95f, null),
            FieldVerdictDto("MANUFACTURING_DATE", "PASS", "JUL 2026",
                null, null, "Rule 6(1)(d)", 0.88f, null),
            FieldVerdictDto("CONSUMER_CARE", "INCONCLUSIVE", null,
                null, null, "Rule 6(1)(f)", 0.41f,
                "Not detected with sufficient confidence. Officer should verify on label."),
            FieldVerdictDto("FSSAI_LICENSE", "PASS", "FSSAI Lic. No. 10013022004641",
                null, null, "Food Safety and Standards Act, 2006", 0.92f, null)
        )
    }

    val failCount = mockReport.count { it.status == "FAIL" }
    val passCount = mockReport.count { it.status == "PASS" }
    val inconclusiveCount = mockReport.count { it.status == "INCONCLUSIVE" }

    var contentVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { contentVisible = true }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Navy900, Navy800)))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            // Top bar
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                }
                Text(
                    "Compliance Report",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            Spacer(Modifier.height(16.dp))

            AnimatedVisibility(
                visible = contentVisible,
                enter = fadeIn() + slideInVertically(animationSpec = spring()) { it / 4 }
            ) {
                Column {
                    // Overall verdict banner
                    OverallVerdictBanner(
                        productName = "Lays Classic Salted (26g)",
                        manufacturerName = "PepsiCo India Holdings Pvt. Ltd.",
                        failCount = failCount,
                        passCount = passCount,
                        inconclusiveCount = inconclusiveCount,
                        labelAreaCm2 = 320.0,
                        registryStatus = "VERIFIED"
                    )

                    Spacer(Modifier.height(20.dp))

                    // Field verdicts
                    Text(
                        "Field-by-Field Verdict",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))

                    mockReport.forEachIndexed { index, verdict ->
                        // Animate each card in with a stagger
                        var cardVisible by remember { mutableStateOf(false) }
                        LaunchedEffect(Unit) {
                            kotlinx.coroutines.delay(index * 80L)
                            cardVisible = true
                        }
                        AnimatedVisibility(
                            visible = cardVisible,
                            enter = fadeIn() + slideInVertically(animationSpec = spring()) { it / 2 }
                        ) {
                            FieldVerdictCard(verdict = verdict)
                        }
                        Spacer(Modifier.height(8.dp))
                    }

                    Spacer(Modifier.height(20.dp))

                    // Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { /* TODO: Generate PDF with iText7 and share */ },
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.PictureAsPdf, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Export PDF", style = MaterialTheme.typography.labelSmall)
                        }
                        OutlinedButton(
                            onClick = { /* TODO: Share inspection report */ },
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.IosShare, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Share", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    Button(
                        onClick = onNewInspection,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Indigo500)
                    ) {
                        Text("New Inspection")
                    }

                    Spacer(Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = onViewHistory,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("View Inspection History")
                    }

                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun OverallVerdictBanner(
    productName: String,
    manufacturerName: String,
    failCount: Int,
    passCount: Int,
    inconclusiveCount: Int,
    labelAreaCm2: Double,
    registryStatus: String
) {
    val hasViolations = failCount > 0
    val bannerColor = if (hasViolations) Coral500.copy(alpha = 0.12f) else Emerald500.copy(alpha = 0.1f)
    val borderColor = if (hasViolations) Coral500 else Emerald500

    Card(
        colors = CardDefaults.cardColors(containerColor = bannerColor),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (hasViolations) Icons.Default.Error else Icons.Default.CheckCircle,
                    null,
                    tint = if (hasViolations) Coral500 else Emerald500,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        if (hasViolations) "NON-COMPLIANT" else "COMPLIANT",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (hasViolations) Coral500 else Emerald500
                    )
                    Text(
                        productName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                VerdictStat("$passCount Passed", Emerald500)
                VerdictStat("$failCount Failed", Coral500)
                VerdictStat("$inconclusiveCount Pending", Amber500)
            }

            Spacer(Modifier.height(12.dp))

            // Registry check
            val registryColor = when (registryStatus) {
                "VERIFIED" -> Emerald500
                "INCONCLUSIVE" -> Amber500
                else -> Coral500
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Registry: ", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(registryStatus, style = MaterialTheme.typography.labelSmall, color = registryColor, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Text("Label: ${labelAreaCm2.toInt()}cm²", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Text(
                manufacturerName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun VerdictStat(label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label.split(" ")[0], style = MaterialTheme.typography.headlineSmall, color = color, fontWeight = FontWeight.Bold)
        Text(label.split(" ")[1], style = MaterialTheme.typography.labelSmall, color = color.copy(alpha = 0.7f))
    }
}

@Composable
private fun FieldVerdictCard(verdict: FieldVerdictDto) {
    val (icon, color, bgColor) = when (verdict.status) {
        "PASS"          -> Triple(Icons.Default.CheckCircle, Emerald500, Emerald500.copy(0.08f))
        "FAIL"          -> Triple(Icons.Default.Error, Coral500, Coral500.copy(0.08f))
        "INCONCLUSIVE"  -> Triple(Icons.Default.Help, Amber500, Amber500.copy(0.08f))
        "NOT_APPLICABLE"-> Triple(Icons.Default.RemoveCircle, Color.Gray, Color.Gray.copy(0.08f))
        else            -> Triple(Icons.Default.Help, Blue500, Blue500.copy(0.08f))
    }

    val fieldDisplayName = verdict.field
        .replace("_", " ")
        .lowercase()
        .split(" ")
        .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }

    Card(
        colors = CardDefaults.cardColors(containerColor = bgColor),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    fieldDisplayName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    verdict.status,
                    style = MaterialTheme.typography.labelSmall,
                    color = color,
                    fontWeight = FontWeight.Bold
                )
            }

            verdict.extractedValue?.let { value ->
                Spacer(Modifier.height(6.dp))
                Text(
                    "\"$value\"",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Font-size measurement readout
            if (verdict.measuredHeightMm != null && verdict.requiredHeightMm != null) {
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Navy700, RoundedCornerShape(8.dp))
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Measured", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            "${String.format("%.1f", verdict.measuredHeightMm)}mm",
                            style = MaterialTheme.typography.titleSmall,
                            color = if (verdict.status == "FAIL") Coral500 else Emerald500,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Required", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            "≥${verdict.requiredHeightMm}mm",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(Modifier.height(6.dp))
            Text(
                verdict.ruleReference,
                style = MaterialTheme.typography.labelSmall,
                color = Indigo500
            )

            verdict.notes?.let { notes ->
                Spacer(Modifier.height(4.dp))
                Text(
                    notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Confidence indicator
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                Text("Confidence: ", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    "${(verdict.confidence * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = when {
                        verdict.confidence > 0.8f -> Emerald500
                        verdict.confidence > 0.5f -> Amber500
                        else                       -> Coral500
                    }
                )
            }
        }
    }
}
