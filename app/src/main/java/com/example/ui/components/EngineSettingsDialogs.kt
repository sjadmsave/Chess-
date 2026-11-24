package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Balance
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Casino
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.CrisisAlert
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material.icons.rounded.Hub
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.SportsMma
import androidx.compose.material.icons.rounded.Timeline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
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
import com.example.chess.PersonalityCategory
import com.example.chess.PersonalityRisk
import com.example.chess.StrengthTier
import com.example.ui.theme.formatArabicNumber
import kotlin.math.roundToInt

@Composable
fun EnginePersonalitySheetContent(
    selectedPersonality: EnginePersonality,
    intensity: Int,
    risk: PersonalityRisk,
    onSelectPersonality: (EnginePersonality) -> Unit,
    onIntensityChange: (Int) -> Unit,
    onSelectRisk: (PersonalityRisk) -> Unit,
    onDismiss: () -> Unit
) {
    var localIntensity by remember(intensity) { mutableIntStateOf(intensity) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(PersonalityCategory.ALL) }

    val filteredPersonalities = remember(searchQuery, selectedCategory) {
        val q = searchQuery.trim()
        EnginePersonality.entries.filter { p ->
            val catOk = selectedCategory == PersonalityCategory.ALL || p.category == selectedCategory
            val qOk = q.isBlank() ||
                p.nameAr.contains(q, ignoreCase = true) ||
                p.styleBadgeAr.contains(q, ignoreCase = true) ||
                p.descriptionAr.contains(q, ignoreCase = true)
            catOk && qOk
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp),
        contentPadding = PaddingValues(bottom = 26.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "شخصيات المحرك (${formatArabicNumber(EnginePersonality.entries.size)})",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "٥٠ شخصية متنوعة هجومية ودفاعية وتكتيكية وأسطورية",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(onClick = onDismiss) {
                    Text("تم", fontWeight = FontWeight.Bold)
                }
            }
        }

        // Intensity Slider (0..100) + Risk Multiplier Selector
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "شدّة الشخصية",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (localIntensity == 0 || selectedPersonality == EnginePersonality.CLASSIC) {
                                "٠٪ (معطّلة)"
                            } else {
                                "${formatArabicNumber(localIntensity)}٪"
                            },
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Slider(
                        value = localIntensity.toFloat(),
                        onValueChange = { localIntensity = it.roundToInt().coerceIn(0, 100) },
                        onValueChangeFinished = { onIntensityChange(localIntensity) },
                        valueRange = 0f..100f,
                        modifier = Modifier.testTag("personality_intensity_slider")
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

                    Text(
                        text = "حدود المخاطرة",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        PersonalityRisk.entries.forEach { option ->
                            val selected = option == risk
                            Surface(
                                onClick = { onSelectRisk(option) },
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("risk_${option.id}"),
                                shape = RoundedCornerShape(12.dp),
                                color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                border = BorderStroke(
                                    1.dp,
                                    if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                )
                            ) {
                                Box(
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = option.nameAr,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("ابحث عن شخصية أو أسلوب...") },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Rounded.Clear, contentDescription = "مسح")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(PersonalityCategory.entries) { cat ->
                        val count = if (cat == PersonalityCategory.ALL) {
                            EnginePersonality.entries.size
                        } else {
                            EnginePersonality.entries.count { it.category == cat }
                        }
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text("${cat.nameAr} (${formatArabicNumber(count)})") }
                        )
                    }
                }
            }
        }

        items(filteredPersonalities, key = { it.id }) { p ->
            val selected = p == selectedPersonality
            Surface(
                onClick = { onSelectPersonality(p) },
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
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                                else MaterialTheme.colorScheme.surfaceVariant
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = personalityIcon(p),
                            contentDescription = p.nameAr,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(21.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = p.nameAr,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = p.styleBadgeAr,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                )
                            }
                        }
                        Text(
                            text = p.descriptionAr,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (selected) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = "محددة",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EngineEloDialog(
    initialElo: Int,
    initialTier: StrengthTier,
    onConfirmPreset: (StrengthTier) -> Unit,
    onConfirmCustomElo: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var usePresetUnlimited by remember {
        mutableStateOf(initialElo <= 0)
    }
    var selectedTier by remember { mutableStateOf(initialTier) }
    var sliderElo by remember {
        mutableIntStateOf(
            if (initialElo <= 0) initialTier.targetElo.let { if (it <= 0) 2600 else it }
            else StrengthTier.snapElo(initialElo)
        )
    }
    var isCustomSlider by remember { mutableStateOf(initialElo > 0) }

    val activeTier = if (usePresetUnlimited) StrengthTier.MAX
    else if (isCustomSlider) StrengthTier.fromElo(sliderElo)
    else selectedTier

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("قوة المحرك", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = if (usePresetUnlimited) {
                        "${StrengthTier.MAX.nameAr} • بلا حد (أقصى قوة)"
                    } else {
                        "${activeTier.nameAr} • نحو ${formatArabicNumber(sliderElo)} إيلو"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "المستويات الخمسة:",
                    style = MaterialTheme.typography.labelLarge
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    StrengthTier.entries.forEach { tier ->
                        val selected = if (tier == StrengthTier.MAX) {
                            usePresetUnlimited || (!isCustomSlider && selectedTier == StrengthTier.MAX)
                        } else {
                            !usePresetUnlimited && (selectedTier == tier || activeTier == tier)
                        }
                        Surface(
                            onClick = {
                                selectedTier = tier
                                isCustomSlider = false
                                if (tier == StrengthTier.MAX) {
                                    usePresetUnlimited = true
                                    sliderElo = 2600
                                } else {
                                    usePresetUnlimited = false
                                    sliderElo = tier.targetElo
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("strength_tier_${tier.id}"),
                            shape = RoundedCornerShape(10.dp),
                            color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            border = if (selected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 6.dp, horizontal = 2.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = tier.nameAr,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (tier.targetElo <= 0) "∞" else formatArabicNumber(tier.targetElo),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                FilterChip(
                    selected = usePresetUnlimited,
                    onClick = {
                        usePresetUnlimited = !usePresetUnlimited
                        if (usePresetUnlimited) {
                            selectedTier = StrengthTier.MAX
                            isCustomSlider = false
                        } else {
                            isCustomSlider = true
                        }
                    },
                    label = { Text("بلا حد (أقصى قوة ممكنة)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("unlimited_strength_chip")
                )

                Text(
                    text = "منزلق مخصّص (${formatArabicNumber(200)} – ${formatArabicNumber(3000)}، خطوة ${formatArabicNumber(50)}):",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Slider(
                    value = sliderElo.toFloat(),
                    enabled = !usePresetUnlimited,
                    onValueChange = {
                        usePresetUnlimited = false
                        isCustomSlider = true
                        sliderElo = StrengthTier.snapElo(it.toInt())
                    },
                    valueRange = 200f..3000f,
                    steps = 55
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            usePresetUnlimited = false
                            isCustomSlider = true
                            sliderElo = (sliderElo - 100).coerceAtLeast(200)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                    ) {
                        Text("−${formatArabicNumber(100)}")
                    }
                    OutlinedButton(
                        onClick = {
                            usePresetUnlimited = false
                            isCustomSlider = true
                            sliderElo = (sliderElo + 100).coerceAtMost(3000)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                    ) {
                        Text("+${formatArabicNumber(100)}")
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (usePresetUnlimited) {
                        onConfirmPreset(StrengthTier.MAX)
                    } else if (!isCustomSlider) {
                        onConfirmPreset(selectedTier)
                    } else {
                        onConfirmCustomElo(sliderElo)
                    }
                    onDismiss()
                },
                modifier = Modifier.testTag("confirm_elo_btn")
            ) {
                Text("اعتماد")
            }
        }
    )
}

private fun personalityIcon(p: EnginePersonality): ImageVector = when (p) {
    EnginePersonality.CLASSIC -> Icons.Rounded.Balance
    EnginePersonality.FIERCE_WARRIOR -> Icons.Rounded.Bolt
    EnginePersonality.IRON_FORTRESS -> Icons.Rounded.Shield
    EnginePersonality.STRATEGIC_GENIUS -> Icons.Rounded.Hub
    EnginePersonality.PATIENT_SERPENT -> Icons.Rounded.Timeline
    EnginePersonality.ENDGAME_MASTER -> Icons.Rounded.Flag
    EnginePersonality.SACRIFICE_KING -> Icons.Rounded.LocalFireDepartment
    EnginePersonality.BRILLIANCY_CREATOR -> Icons.Rounded.AutoAwesome
    EnginePersonality.MISTAKE_CATCHER -> Icons.Rounded.Search
    EnginePersonality.TRICKY_GENIUS -> Icons.Rounded.Extension
    EnginePersonality.OPPORTUNITY_HUNTER -> Icons.Rounded.CrisisAlert
    EnginePersonality.LONG_GAME_MAKER -> Icons.Rounded.HourglassTop
    EnginePersonality.MYSTERY_PLAYER -> Icons.Rounded.Casino
    else -> when (p.category) {
        PersonalityCategory.ALL,
        PersonalityCategory.BASELINE -> Icons.Rounded.Balance
        PersonalityCategory.ATTACKING -> Icons.Rounded.LocalFireDepartment
        PersonalityCategory.DEFENSIVE -> Icons.Rounded.Shield
        PersonalityCategory.POSITIONAL -> Icons.Rounded.Hub
        PersonalityCategory.TACTICAL -> Icons.Rounded.CrisisAlert
        PersonalityCategory.ENDGAME -> Icons.Rounded.Flag
        PersonalityCategory.PSYCHOLOGICAL -> Icons.Rounded.Psychology
    }
}
