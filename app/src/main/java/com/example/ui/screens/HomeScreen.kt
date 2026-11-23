package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.PictureInPictureAlt
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.chess.BotPersona
import com.example.chess.ChessBoardState
import com.example.chess.EnginePersonality
import com.example.chess.GameMode
import com.example.chess.PieceColor
import com.example.chess.PuzzleRepository
import com.example.chess.StrengthTier
import com.example.chess.TimeControl
import com.example.data.SavedGameEntity
import com.example.data.UserPreferences
import com.example.data.formatOpponentTitle
import com.example.ui.components.MiniBoardThumbnail
import com.example.ui.theme.formatArabicNumber

@Composable
fun HomeScreen(
    prefs: UserPreferences,
    ongoingGame: SavedGameEntity?,
    onStartCustomMatch: (PieceColor, Int, EnginePersonality, Boolean, TimeControl) -> Unit,
    onStartBotMatch: (BotPersona, PieceColor, Boolean, TimeControl) -> Unit,
    onStartLocalMatch: (TimeControl) -> Unit,
    onResumeGame: (SavedGameEntity) -> Unit,
    onActivateFloatingWindow: () -> Unit,
    onOpenImportDialog: () -> Unit,
    onOpenDailyPuzzle: () -> Unit,
    onOpenStats: () -> Unit,
    onOpenAboutLicenses: () -> Unit
) {
    var showNewMatchDialog by remember { mutableStateOf(false) }

    val dailyPuzzle = remember(prefs.playerElo) {
        PuzzleRepository.dailyPuzzleForToday(prefs.playerElo)
    }
    val dailySolvedToday = prefs.lastDailySolvedDate == PuzzleRepository.todayDateKey()

    val heroBoard = remember(ongoingGame?.currentFen) {
        ongoingGame?.let {
            runCatching { ChessBoardState.fromFen(it.currentFen) }.getOrNull()
        } ?: ChessBoardState.initial()
    }

    val currentEloText = if (prefs.customEngineElo <= 0) {
        "${prefs.strengthPreset.nameAr} (${if (prefs.strengthPreset.targetElo <= 0) "بلا حد" else "${formatArabicNumber(prefs.strengthPreset.targetElo)} إيلو"})"
    } else {
        "نحو ${formatArabicNumber(prefs.customEngineElo)} إيلو"
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 680.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 104.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Top Header & Badges
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "رُقعة",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "محرك Stockfish ذكي • تحليل وتدريب أوفلاين بالكامل",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.LocalFireDepartment,
                                    contentDescription = "سلسلة الأيام",
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = formatArabicNumber(prefs.dailyStreak),
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Star,
                                    contentDescription = "التصنيف",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = formatArabicNumber(prefs.playerElo),
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }
                }
            }

            // Hero Board Thumbnail + Continue / New Match Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            MiniBoardThumbnail(
                                boardState = heroBoard,
                                boardTheme = prefs.boardTheme,
                                pieceStyle = prefs.pieceStyle,
                                size = 86.dp
                            )

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = if (ongoingGame != null) "أكمل مباراتك" else "ابدأ مواجهة جديدة",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = if (ongoingGame != null) {
                                        "${formatOpponentTitle(ongoingGame.botId)} • ${formatArabicNumber(ongoingGame.moveCount)} نقلة"
                                    } else {
                                        "$currentEloText • ${prefs.enginePersonality.nameAr}"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (ongoingGame != null) {
                                Button(
                                    onClick = { onResumeGame(ongoingGame) },
                                    modifier = Modifier
                                        .weight(1.2f)
                                        .height(48.dp)
                                        .testTag("resume_game_button"),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("استئناف المباراة", fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = { showNewMatchDialog = true },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("new_match_dialog_button"),
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                                ) {
                                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("مباراة جديدة", fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Button(
                                    onClick = { showNewMatchDialog = true },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                        .testTag("start_match_button"),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("مباراة جديدة", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                                }
                            }
                        }
                    }
                }
            }

            // Section: العب بطريقتك
            item {
                Text(
                    text = "العب بطريقتك",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    HomeActionTile(
                        title = "لاعبان محليان على جهاز واحد",
                        subtitle = "تحدَّ صديقاً وجهاً لوجه مع ساعة شطرنج أو لعب حر",
                        icon = Icons.Rounded.Group,
                        tag = "home_local_2p_tile",
                        onClick = { onStartLocalMatch(TimeControl.UNLIMITED) }
                    )
                    HomeActionTile(
                        title = "تنشيط النافذة العائمة",
                        subtitle = "رقعة شطرنج عائمة تفاعلية فوق جميع التطبيقات",
                        icon = Icons.Rounded.PictureInPictureAlt,
                        tag = "home_floating_tile",
                        onClick = onActivateFloatingWindow
                    )
                    HomeActionTile(
                        title = "استيراد PGN أو FEN",
                        subtitle = "ألصق نص مباراة أو افتح ملفاً لتحليله ومواصلته",
                        icon = Icons.Rounded.Download,
                        tag = "home_import_tile",
                        onClick = onOpenImportDialog
                    )
                    HomeActionTile(
                        title = "اللغز اليومي والتحديات",
                        subtitle = if (dailySolvedToday) {
                            "${dailyPuzzle.titleAr} • مكتمل اليوم ✓"
                        } else {
                            "${dailyPuzzle.titleAr} • تصنيف ${formatArabicNumber(dailyPuzzle.rating)} (+${formatArabicNumber(40)} XP)"
                        },
                        icon = Icons.Rounded.Extension,
                        tag = "daily_puzzle_card",
                        onClick = onOpenDailyPuzzle
                    )
                    HomeActionTile(
                        title = "إحصاءاتك وتقاريرك",
                        subtitle = "نتائجك حسب القوة والشخصية وتشخيص نقاط القوة والضعف",
                        icon = Icons.Rounded.BarChart,
                        tag = "home_stats_tile",
                        onClick = onOpenStats
                    )
                    HomeActionTile(
                        title = "حول رُقعة والتراخيص",
                        subtitle = "الإصدار ٣.٢.٣ • تراخيص Stockfish GPLv3 وخط IBM Plex",
                        icon = Icons.Rounded.Info,
                        tag = "home_about_tile",
                        onClick = onOpenAboutLicenses
                    )
                }
            }

            // World Champions & Masters Carousel
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "تحدَّ أساطير الشطرنج",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "${formatArabicNumber(BotPersona.entries.size)} خصوم",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(vertical = 2.dp)
                ) {
                    items(BotPersona.entries) { bot ->
                        Card(
                            onClick = {
                                onStartBotMatch(bot, PieceColor.WHITE, prefs.adaptiveMode, TimeControl.MIN_10)
                            },
                            modifier = Modifier
                                .width(190.dp)
                                .testTag("bot_card_${bot.id}"),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Image(
                                        painter = painterResource(id = bot.avatarRes),
                                        contentDescription = bot.nameAr,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .border(1.5.dp, bot.accentColor, RoundedCornerShape(10.dp))
                                    )
                                    Column {
                                        Text(
                                            text = bot.nameAr,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1
                                        )
                                        Text(
                                            text = "${formatArabicNumber(bot.elo)} إيلو",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = bot.accentColor,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Text(
                                    text = bot.styleTagAr,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showNewMatchDialog) {
        NewMatchWizardDialog(
            prefs = prefs,
            onStartCustom = { color, elo, personality, humanStyle, timeControl ->
                showNewMatchDialog = false
                onStartCustomMatch(color, elo, personality, humanStyle, timeControl)
            },
            onStartBot = { bot, color, adaptive, timeControl ->
                showNewMatchDialog = false
                onStartBotMatch(bot, color, adaptive, timeControl)
            },
            onStartLocal = { timeControl ->
                showNewMatchDialog = false
                onStartLocalMatch(timeControl)
            },
            onDismiss = { showNewMatchDialog = false }
        )
    }
}

@Composable
private fun HomeActionTile(
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
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Icon(
                imageVector = Icons.Rounded.ChevronLeft,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun NewMatchWizardDialog(
    prefs: UserPreferences,
    onStartCustom: (PieceColor, Int, EnginePersonality, Boolean, TimeControl) -> Unit,
    onStartBot: (BotPersona, PieceColor, Boolean, TimeControl) -> Unit,
    onStartLocal: (TimeControl) -> Unit,
    onDismiss: () -> Unit
) {
    var mode by remember { mutableStateOf(GameMode.VS_ENGINE) }
    var selectedColor by remember { mutableStateOf(PieceColor.WHITE) }
    var elo by remember { mutableIntStateOf(prefs.effectiveEngineElo) }
    var personality by remember { mutableStateOf(prefs.enginePersonality) }
    var humanStyle by remember { mutableStateOf(prefs.humanStyleEnabled) }
    var selectedBot by remember { mutableStateOf(BotPersona.byId(prefs.selectedBotId)) }
    var selectedTimeControl by remember { mutableStateOf(TimeControl.UNLIMITED) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إعداد مباراة جديدة", fontWeight = FontWeight.ExtraBold) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(GameMode.entries) { gm ->
                            FilterChip(
                                selected = mode == gm,
                                onClick = { mode = gm },
                                label = { Text(gm.nameAr) },
                                modifier = Modifier.testTag("wizard_mode_${gm.id}")
                            )
                        }
                    }
                }

                if (mode != GameMode.LOCAL_TWO_PLAYER) {
                    item {
                        Text("لون قطعك:", style = MaterialTheme.typography.labelLarge)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = selectedColor == PieceColor.WHITE,
                                onClick = { selectedColor = PieceColor.WHITE },
                                label = { Text("الأبيض ♔") }
                            )
                            FilterChip(
                                selected = selectedColor == PieceColor.BLACK,
                                onClick = { selectedColor = PieceColor.BLACK },
                                label = { Text("الأسود ♚") }
                            )
                        }
                    }
                }

                when (mode) {
                    GameMode.VS_ENGINE -> {
                        item {
                            Text("المستويات الخمسة:", style = MaterialTheme.typography.labelLarge)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(StrengthTier.entries) { tier ->
                                    val selected = if (tier == StrengthTier.MAX) elo <= 0 else elo in tier.eloRange || elo == tier.targetElo
                                    FilterChip(
                                        selected = selected,
                                        onClick = { elo = tier.targetElo },
                                        label = {
                                            Text(
                                                if (tier.targetElo <= 0) "${tier.nameAr} (بلا حد)"
                                                else "${tier.nameAr} (${formatArabicNumber(tier.targetElo)})"
                                            )
                                        }
                                    )
                                }
                            }
                        }

                        item {
                            val strengthDesc = if (elo <= 0) "بلا حد (أقصى قوة)" else "نحو ${formatArabicNumber(elo)} إيلو"
                            Text(
                                text = "قوة المحرك: $strengthDesc",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Slider(
                                value = if (elo <= 0) 3000f else elo.toFloat(),
                                onValueChange = { elo = StrengthTier.snapElo(it.toInt()) },
                                valueRange = 200f..3000f,
                                steps = 55
                            )
                        }

                        item {
                            Text("شخصية المحرك:", style = MaterialTheme.typography.labelLarge)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(EnginePersonality.entries) { p ->
                                    FilterChip(
                                        selected = personality == p,
                                        onClick = { personality = p },
                                        label = { Text(p.nameAr) }
                                    )
                                }
                            }
                        }

                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                    Text("الأسلوب البشري", style = MaterialTheme.typography.bodyMedium)
                                }
                                Switch(checked = humanStyle, onCheckedChange = { humanStyle = it })
                            }
                        }
                    }

                    GameMode.VS_BOT_PERSONA -> {
                        item {
                            Text("اختر الأستاذ:", style = MaterialTheme.typography.labelLarge)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(BotPersona.entries) { bot ->
                                    FilterChip(
                                        selected = selectedBot == bot,
                                        onClick = { selectedBot = bot },
                                        label = { Text("${bot.nameAr} (${formatArabicNumber(bot.elo)})") }
                                    )
                                }
                            }
                        }
                    }

                    GameMode.LOCAL_TWO_PLAYER -> {
                        item {
                            Text(
                                text = "يتناوب لاعبان على الرقعة نفسها دون تدخل المحرك.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                item {
                    Text("إعداد الساعة:", style = MaterialTheme.typography.labelLarge)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(TimeControl.entries) { tc ->
                            FilterChip(
                                selected = selectedTimeControl == tc,
                                onClick = { selectedTimeControl = tc },
                                label = { Text("${tc.labelAr} (${tc.categoryAr})") }
                            )
                        }
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
                    when (mode) {
                        GameMode.VS_ENGINE -> onStartCustom(selectedColor, elo, personality, humanStyle, selectedTimeControl)
                        GameMode.VS_BOT_PERSONA -> onStartBot(selectedBot, selectedColor, prefs.adaptiveMode, selectedTimeControl)
                        GameMode.LOCAL_TWO_PLAYER -> onStartLocal(selectedTimeControl)
                    }
                },
                modifier = Modifier.heightIn(min = 48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("ابدأ اللعب")
            }
        }
    )
}
