package com.legalmetrology.inspector.ui.screens.history

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Help
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.legalmetrology.inspector.ui.theme.Amber500
import com.legalmetrology.inspector.ui.theme.Coral500
import com.legalmetrology.inspector.ui.theme.Emerald500
import com.legalmetrology.inspector.ui.theme.Indigo500
import com.legalmetrology.inspector.ui.theme.Navy800
import com.legalmetrology.inspector.ui.theme.Navy900
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

// Mock inspection history items for demo
// TODO: Replace with Room-backed ViewModel query
data class InspectionHistoryItem(
    val id: String,
    val productName: String,
    val manufacturerName: String,
    val packageType: String,
    val status: String, // PASS, FLAGGED, INCONCLUSIVE
    val failCount: Int,
    val timestamp: Long
)

private val MOCK_HISTORY = listOf(
    InspectionHistoryItem(UUID.randomUUID().toString(), "Lays Classic Salted (26g)", "PepsiCo India Holdings", "Retail · Food", "FLAGGED", 1, System.currentTimeMillis() - 3600_000),
    InspectionHistoryItem(UUID.randomUUID().toString(), "Tata Salt (1kg)", "Tata Consumer Products Ltd.", "Retail · Food", "PASS", 0, System.currentTimeMillis() - 86400_000),
    InspectionHistoryItem(UUID.randomUUID().toString(), "Dove Shampoo (200ml)", "Hindustan Unilever Ltd.", "Retail · Cosmetic", "FLAGGED", 2, System.currentTimeMillis() - 172800_000),
    InspectionHistoryItem(UUID.randomUUID().toString(), "Brooke Bond Red Label (250g)", "Hindustan Unilever Ltd.", "Retail · Food", "PASS", 0, System.currentTimeMillis() - 259200_000),
    InspectionHistoryItem(UUID.randomUUID().toString(), "Johnson & Johnson Baby Powder", "Johnson & Johnson Pvt. Ltd.", "Retail · General", "INCONCLUSIVE", 0, System.currentTimeMillis() - 345600_000),
)

@Composable
fun HistoryScreen(
    onViewReport: (String) -> Unit,
    onBack: () -> Unit
) {
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
                .padding(horizontal = 20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                }
                Column(modifier = Modifier.padding(start = 8.dp)) {
                    Text(
                        "Inspection History",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "${MOCK_HISTORY.size} inspections",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Summary stats
            AnimatedVisibility(visible = contentVisible, enter = fadeIn()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val flagged = MOCK_HISTORY.count { it.status == "FLAGGED" }
                    val compliant = MOCK_HISTORY.count { it.status == "PASS" }
                    StatChip("${MOCK_HISTORY.size} Total", Indigo500, Modifier.weight(1f))
                    StatChip("$compliant Passed", Emerald500, Modifier.weight(1f))
                    StatChip("$flagged Flagged", Coral500, Modifier.weight(1f))
                }
            }

            Spacer(Modifier.height(16.dp))

            // History list
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsIndexed(MOCK_HISTORY) { index, item ->
                    var itemVisible by remember { mutableStateOf(false) }
                    LaunchedEffect(Unit) {
                        kotlinx.coroutines.delay(index * 60L)
                        itemVisible = true
                    }
                    AnimatedVisibility(
                        visible = itemVisible,
                        enter = fadeIn() + slideInVertically { it / 3 }
                    ) {
                        InspectionHistoryCard(item = item, onClick = { onViewReport(item.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun StatChip(label: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.12f)),
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(8.dp).fillMaxWidth()) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun InspectionHistoryCard(item: InspectionHistoryItem, onClick: () -> Unit) {
    val (icon, color) = when (item.status) {
        "PASS"         -> Icons.Default.CheckCircle to Emerald500
        "FLAGGED"      -> Icons.Default.Error to Coral500
        else           -> Icons.Default.Help to Amber500
    }

    val dateStr = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
        .format(Date(item.timestamp))

    Card(
        colors = CardDefaults.cardColors(containerColor = Navy800),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    item.productName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    item.manufacturerName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(modifier = Modifier.padding(top = 4.dp)) {
                    Text(
                        item.packageType,
                        style = MaterialTheme.typography.labelSmall,
                        color = Indigo500
                    )
                    if (item.failCount > 0) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "${item.failCount} violation${if (item.failCount > 1) "s" else ""}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Coral500
                        )
                    }
                }
                Text(
                    dateStr,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
            Icon(
                Icons.Default.ArrowForwardIos,
                null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
