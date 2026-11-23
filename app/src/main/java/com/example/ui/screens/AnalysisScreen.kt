package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoGraph
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.formatArabicNumber
import com.example.viewmodel.ActiveMatchUiState

@Composable
fun AnalysisScreen(
    matchState: ActiveMatchUiState,
    onOpenFenSetup: () -> Unit,
    onAnalyzeCurrentMatch: () -> Unit,
    onAnalyzeMultiPvPosition: () -> Unit,
    onOpenImportDialog: () -> Unit,
    onOpenPuzzles: () -> Unit,
    onOpenSavedArchive: () -> Unit
) {
    val moveCount = matchState.recordedMoves.size

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 680.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 104.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "التحليل",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "راجع النقلات على الرقعة واستعن بمحرك Stockfish",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item {
                AnalysisHubCard(
                    title = "اللعب من FEN",
                    subtitle = "ابدأ مباراة أو عدّل وضعية تختارها",
                    icon = Icons.Rounded.EditNote,
                    tag = "analysis_fen_card",
                    onClick = onOpenFenSetup
                )
            }

            item {
                AnalysisHubCard(
                    title = "تحليل المباراة الحالية",
                    subtitle = if (moveCount == 0) {
                        "لا توجد نقلات بعد • ابدأ اللعب أو الاستعراض"
                    } else {
                        "${formatArabicNumber(moveCount)} نقلة • مراجعة قابلة للإيقاف والاستئناف"
                    },
                    icon = Icons.Rounded.AutoGraph,
                    tag = "analysis_current_match_card",
                    onClick = onAnalyzeCurrentMatch
                )
            }

            item {
                AnalysisHubCard(
                    title = "تحليل الوضعية (أفضل ٣ خطوط)",
                    subtitle = "بحث MultiPV ٣ بعمق كامل لزمن ١٫٧ ثانية على الرقعة",
                    icon = Icons.Rounded.Psychology,
                    tag = "analysis_multipv_card",
                    onClick = onAnalyzeMultiPvPosition
                )
            }

            item {
                AnalysisHubCard(
                    title = "استيراد PGN أو FEN",
                    subtitle = "افتح ملفاً أو ألصق وضعية أو مباراة",
                    icon = Icons.Rounded.Download,
                    tag = "analysis_import_card",
                    onClick = onOpenImportDialog
                )
            }

            item {
                AnalysisHubCard(
                    title = "المباريات المحفوظة",
                    subtitle = "اختر مباراة من الأرشيف",
                    icon = Icons.Rounded.History,
                    tag = "analysis_archive_card",
                    onClick = onOpenSavedArchive
                )
            }

            item {
                AnalysisHubCard(
                    title = "الألغاز والتدريب التكتيكي",
                    subtitle = "تحدي يومي وتمارين تكتيكية متدرجة أوفلاين",
                    icon = Icons.Rounded.Extension,
                    tag = "analysis_puzzles_card",
                    onClick = onOpenPuzzles
                )
            }
        }
    }
}

@Composable
private fun AnalysisHubCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    tag: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(tag),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.Rounded.ChevronLeft,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}
