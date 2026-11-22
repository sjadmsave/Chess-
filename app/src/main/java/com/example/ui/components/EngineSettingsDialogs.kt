package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Balance
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CrisisAlert
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material.icons.rounded.Hub
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Timeline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.chess.EnginePersonality
import com.example.ui.theme.MonoFontFamily

@Composable
fun EnginePersonalitySheetContent(
    selectedPersonality: EnginePersonality,
    onSelect: (EnginePersonality) -> Unit,
    onDismiss: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp),
        contentPadding = PaddingValues(bottom = 26.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                text = "اختر شخصية المحرك",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }

        items(EnginePersonality.entries) { p ->
            val selected = p == selectedPersonality
            Surface(
                onClick = {
                    onSelect(p)
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("personality_${p.id}"),
                shape = RoundedCornerShape(16.dp),
                color = if (selected) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                } else {
                    MaterialTheme.colorScheme.surface
                },
                border = BorderStroke(
                    width = if (selected) 1.5.dp else 1.dp,
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = personalityIcon(p),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = p.nameAr,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = p.descriptionAr,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = Icons.Rounded.MoreHoriz,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        item {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Text("رجوع")
            }
        }
    }
}

@Composable
fun EngineEloDialog(
    initialElo: Int,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var elo by remember { mutableIntStateOf(initialElo) }
    val levelLabel = when {
        elo < 700 -> "مبتدئ"
        elo < 1200 -> "متوسط"
        elo < 1800 -> "متقدم"
        elo < 2300 -> "مخصص • خبير"
        else -> "أستاذ دولي كبير"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("قوة المحرك", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "$levelLabel • نحو $elo إيلو",
                    fontFamily = MonoFontFamily,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Slider(
                    value = elo.toFloat(),
                    onValueChange = { elo = ((it.toInt() / 50) * 50).coerceIn(200, 3000) },
                    valueRange = 200f..3000f
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { elo = (elo - 100).coerceAtLeast(200) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("−100")
                    }
                    OutlinedButton(
                        onClick = { elo = (elo + 100).coerceAtMost(3000) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("+100")
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(800, 1400, 2000, 2600).forEach { preset ->
                        Surface(
                            onClick = { elo = preset },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            color = if (elo == preset) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$preset",
                                    fontFamily = MonoFontFamily,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        },
        confirmButton = {
            Button(onClick = { onConfirm(elo); onDismiss() }) {
                Text("اعتماد")
            }
        }
    )
}

private fun personalityIcon(p: EnginePersonality): ImageVector = when (p) {
    EnginePersonality.TRADITIONAL -> Icons.Rounded.Balance
    EnginePersonality.OPPORTUNITY_HUNTER -> Icons.Rounded.CrisisAlert
    EnginePersonality.BRILLIANCY_CREATOR -> Icons.Rounded.AutoAwesome
    EnginePersonality.LONG_GAME_MAKER -> Icons.Rounded.HourglassTop
    EnginePersonality.FIERCE_WARRIOR -> Icons.Rounded.Bolt
    EnginePersonality.IRON_FORTRESS -> Icons.Rounded.Shield
    EnginePersonality.STRATEGIC_GENIUS -> Icons.Rounded.Hub
    EnginePersonality.MISTAKE_CATCHER -> Icons.Rounded.Search
    EnginePersonality.SACRIFICE_KING -> Icons.Rounded.LocalFireDepartment
    EnginePersonality.PATIENT_SERPENT -> Icons.Rounded.Timeline
}
