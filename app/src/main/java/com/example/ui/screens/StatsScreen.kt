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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.chess.GameResult
import com.example.data.AchievementCatalog
import com.example.data.AchievementEntity
import com.example.data.PeriodicReport
import com.example.data.RatingPointEntity
import com.example.data.ReportPeriod
import com.example.data.RuqaaEngineStatsSummary
import com.example.data.SavedGameEntity
import com.example.data.UserPreferences
import com.example.data.WdlRecord
import com.example.data.formatOpponentTitle
import com.example.ui.components.AccuracyTrendBars
import com.example.ui.components.EloProgressionChart
import com.example.ui.components.PhaseGaugeRow
import com.example.ui.theme.formatArabicNumber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun StatsScreen(
    prefs: UserPreferences,
    selectedPeriod: ReportPeriod,
    periodicReport: PeriodicReport,
    ratingHistory: List<RatingPointEntity>,
    completedGames: List<SavedGameEntity>,
    achievements: List<AchievementEntity>,
    unassistedOnly: Boolean,
    engineStatsSummary: RuqaaEngineStatsSummary,
    onSelectPeriod: (ReportPeriod) -> Unit,
    onToggleUnassistedOnly: (Boolean) -> Unit,
    onReviewSavedGame: (SavedGameEntity) -> Unit
) {
    val unlockedIds = remember(achievements) { achievements.map { it.id }.toSet() }
    val dateFormat = remember { SimpleDateFormat("dd MMM • HH:mm", Locale.forLanguageTag("ar-IQ")) }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 680.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 104.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Column {
                    Text("إحصاءاتك وتقاريرك", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                    Text(
                        text = "نتائج مباريات المحرك المنتهية وتشخيص نقاط القوة والضعف",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Periodic Filter Chips
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ReportPeriod.entries.forEach { period ->
                        FilterChip(
                            selected = selectedPeriod == period,
                            onClick = { onSelectPeriod(period) },
                            label = { Text("تقرير ${period.labelAr}") }
                        )
                    }
                }
            }

            // Summary KPI Cards
            item {
                val winRate = if (periodicReport.totalGames == 0) 0 else (periodicReport.wins * 100 / periodicReport.totalGames)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatKpiCard("التصنيف", formatArabicNumber(prefs.playerElo), MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                    StatKpiCard("المباريات", formatArabicNumber(periodicReport.totalGames), MaterialTheme.colorScheme.tertiary, Modifier.weight(1f))
                    StatKpiCard("نسبة الفوز", "${formatArabicNumber(winRate)}٪", MaterialTheme.colorScheme.secondary, Modifier.weight(1f))
                    StatKpiCard("متوسط الدقة", "${formatArabicNumber(periodicReport.avgAccuracy)}٪", MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                }
            }

            // Ruqaa Section 10.3 Engine Stats Filter & W-D-L Breakdown
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Rounded.BarChart, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Column {
                                    Text("دون مساعدة مسجّلة", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Text(
                                        text = "يستبعد التلميحات والوضعيات المعدّلة والقوة المتغيّرة",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Switch(
                                checked = unassistedOnly,
                                onCheckedChange = onToggleUnassistedOnly,
                                modifier = Modifier.testTag("unassisted_filter_switch")
                            )
                        }

                        // Metadata counts pills
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            CountPill(
                                label = "مع مساعدة",
                                count = engineStatsSummary.assistedCount,
                                modifier = Modifier.weight(1f)
                            )
                            CountPill(
                                label = "وضعيات مخصّصة",
                                count = engineStatsSummary.customPositionCount,
                                modifier = Modifier.weight(1f)
                            )
                            CountPill(
                                label = "قوة متغيّرة",
                                count = engineStatsSummary.strengthMixedCount,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        if (engineStatsSummary.byStrength.isNotEmpty()) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                            Text("حسب القوة", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            engineStatsSummary.byStrength.forEach { (strengthLabel, rec) ->
                                WdlRow(title = strengthLabel, rec = rec)
                            }
                        }

                        if (engineStatsSummary.byPersonality.isNotEmpty()) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                            Text("حسب شخصية الخصم", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            engineStatsSummary.byPersonality.forEach { (personalityLabel, rec) ->
                                WdlRow(title = personalityLabel, rec = rec)
                            }
                        }

                        if (engineStatsSummary.byStrength.isEmpty()) {
                            Text(
                                text = "لا توجد مباريات محرك منتهية تطابق هذا المرشّح بعد.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // ELO Progression Chart
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("مسار تطور التصنيف (ELO)", style = MaterialTheme.typography.titleMedium)
                        EloProgressionChart(
                            points = ratingHistory,
                            currentElo = prefs.playerElo,
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (ratingHistory.isNotEmpty()) {
                            Text(
                                text = "دقة آخر المباريات",
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.padding(top = 6.dp)
                            )
                            AccuracyTrendBars(points = ratingHistory)
                        }
                    }
                }
            }

            // Strategic Diagnosis & Phase Analysis Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Rounded.Psychology, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text("تشخيص المدرب وخطة التطوير", style = MaterialTheme.typography.titleMedium)
                        }
                        if (periodicReport.totalGames > 0) {
                            PhaseGaugeRow("الافتتاح", periodicReport.avgOpeningAcc, MaterialTheme.colorScheme.primary)
                            PhaseGaugeRow("وسط اللعب", periodicReport.avgMiddlegameAcc, MaterialTheme.colorScheme.tertiary)
                            PhaseGaugeRow("نهاية اللعب", periodicReport.avgEndgameAcc, MaterialTheme.colorScheme.secondary)
                        }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = periodicReport.diagnosisSummaryAr,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(
                                        Icons.Rounded.AutoAwesome,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = periodicReport.nextActionAr,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Achievements & Trophies
            item {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Rounded.EmojiEvents, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                    Text(
                        text = "الجوائز والأوسمة (${formatArabicNumber(unlockedIds.size)}/${formatArabicNumber(AchievementCatalog.all.size)})",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            items(AchievementCatalog.all.chunked(2)) { pair ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    pair.forEach { ach ->
                        val unlocked = ach.id in unlockedIds
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(
                                containerColor = if (unlocked) {
                                    MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
                                } else {
                                    MaterialTheme.colorScheme.surface
                                }
                            ),
                            border = BorderStroke(
                                1.dp,
                                if (unlocked) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(
                                                if (unlocked) MaterialTheme.colorScheme.secondary.copy(alpha = 0.22f)
                                                else MaterialTheme.colorScheme.surfaceVariant
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (unlocked) ach.icon else Icons.Rounded.Lock,
                                            contentDescription = null,
                                            tint = if (unlocked) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Text(
                                        text = "+${formatArabicNumber(ach.xpBonus)} XP",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Text(
                                    text = ach.titleAr,
                                    style = MaterialTheme.typography.titleSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = ach.descriptionAr,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                    if (pair.size == 1) {
                        Box(modifier = Modifier.weight(1f))
                    }
                }
            }

            // Completed Match Archive
            item {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Rounded.History, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("سجل المباريات السابقة", style = MaterialTheme.typography.titleMedium)
                }
            }

            if (completedGames.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Text(
                            text = "لا توجد مباريات مكتملة بعد. أنهِ مباراتك الأولى لتظهر هنا مع إمكانية مراجعة كل نقلة!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            } else {
                items(completedGames, key = { it.id }) { game ->
                    val oppTitle = remember(game.botId) { formatOpponentTitle(game.botId) }
                    val res = runCatching { GameResult.valueOf(game.resultName) }.getOrDefault(GameResult.ONGOING)
                    Card(
                        onClick = { onReviewSavedGame(game) },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = "$oppTitle • ${res.labelAr}",
                                    style = MaterialTheme.typography.titleSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${game.openingNameAr} • ${dateFormat.format(Date(game.updatedAt))}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "دقة ${formatArabicNumber(game.playerAccuracy)}٪",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.labelLarge
                                )
                                val deltaText = if (game.eloDelta >= 0) "+${formatArabicNumber(game.eloDelta)}" else formatArabicNumber(game.eloDelta)
                                Text(
                                    text = "$deltaText ELO",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CountPill(label: String, count: Int, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 7.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = formatArabicNumber(count),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun WdlRow(title: String, rec: WdlRecord) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = "فوز ${formatArabicNumber(rec.wins)} • تعادل ${formatArabicNumber(rec.draws)} • خسارة ${formatArabicNumber(rec.losses)}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun StatKpiCard(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                fontWeight = FontWeight.Bold,
                color = accent,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}
