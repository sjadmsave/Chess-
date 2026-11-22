package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chess.ChessPiece
import com.example.chess.GameResult
import com.example.chess.MoveQuality
import com.example.chess.PieceColor
import com.example.chess.PostGameReport
import com.example.chess.RecordedMove
import com.example.chess.Square
import com.example.data.AchievementDef
import com.example.data.UserPreferences
import com.example.ui.components.ChessBoardView
import com.example.ui.components.ChessPieceGraphic
import com.example.ui.components.EvaluationBar
import com.example.ui.components.PhaseGaugeRow
import com.example.ui.components.PromotionPickerDialog
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.MoveBlunderColor
import com.example.ui.theme.MoveInaccuracyColor
import com.example.ui.theme.MoveMistakeColor
import com.example.viewmodel.ActiveMatchUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayScreen(
    matchState: ActiveMatchUiState,
    prefs: UserPreferences,
    onSquareClick: (Square) -> Unit,
    onSelectPromotion: (com.example.chess.PieceType) -> Unit,
    onDismissPromotion: () -> Unit,
    onRequestHint: () -> Unit,
    onUndo: () -> Unit,
    onFlipBoard: () -> Unit,
    onResign: () -> Unit,
    onSelectReviewMove: (Int?) -> Unit,
    onOpenPostGameSheet: () -> Unit,
    onDismissPostGameSheet: () -> Unit,
    onStartRematch: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val displayedBoard = matchState.displayedBoardState
    val capturedPair = remember(displayedBoard) { displayedBoard.capturedPiecesFromStart() }
    val materialDiff = remember(displayedBoard) { displayedBoard.materialBalanceWhite() }
    val moveListState = rememberLazyListState()

    LaunchedEffect(matchState.recordedMoves.size) {
        if (matchState.recordedMoves.isNotEmpty()) {
            moveListState.animateScrollToItem(matchState.recordedMoves.lastIndex)
        }
    }

    val topColor = if (matchState.flippedBoard) PieceColor.WHITE else PieceColor.BLACK
    val bottomColor = topColor.opposite()

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 640.dp)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Bot Persona Header + Dynamic Chat Bubble
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Image(
                            painter = painterResource(id = matchState.bot.avatarRes),
                            contentDescription = matchState.bot.nameAr,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .border(2.dp, matchState.bot.accentColor, CircleShape)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = matchState.bot.nameAr,
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Surface(
                                    color = matchState.bot.accentColor.copy(alpha = 0.16f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = if (matchState.isAdaptiveMatch) "تكيفي • ${prefs.playerElo}" else "${matchState.bot.elo}",
                                        fontFamily = MonoFontFamily,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = matchState.bot.accentColor,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                    )
                                }
                            }
                            Text(
                                text = matchState.botChatMessageAr,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    if (matchState.isBotThinking) {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = matchState.bot.accentColor
                        )
                    }
                }
            }

            // Top Captured Pieces Strip
            CapturedStrip(
                label = if (topColor == matchState.playerColor) "أنت" else matchState.bot.nameAr,
                capturedPieces = if (topColor == PieceColor.WHITE) capturedPair.first else capturedPair.second,
                advantage = if (topColor == PieceColor.WHITE) materialDiff else -materialDiff,
                pieceStyle = prefs.pieceStyle
            )

            // Main Board + Evaluation Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (prefs.showEvalBar) {
                    val displayedEval = if (matchState.reviewIndex != null && matchState.reviewIndex in matchState.recordedMoves.indices) {
                        matchState.recordedMoves[matchState.reviewIndex].evalWhiteCp
                    } else {
                        matchState.evalWhiteCp
                    }
                    EvaluationBar(
                        evalWhiteCp = displayedEval,
                        flipped = matchState.flippedBoard,
                        modifier = Modifier
                            .fillMaxHeight()
                            .height(320.dp)
                    )
                }

                ChessBoardView(
                    boardState = displayedBoard,
                    boardTheme = prefs.boardTheme,
                    pieceStyle = prefs.pieceStyle,
                    selectedSquare = matchState.selectedSquare,
                    legalMovesForSelected = matchState.legalMovesForSelected,
                    lastMove = matchState.displayedLastMove,
                    hintMove = matchState.currentHint?.move,
                    flipped = matchState.flippedBoard,
                    interactive = matchState.gameResult == GameResult.ONGOING && !matchState.isBotThinking,
                    onSquareClick = { sq ->
                        if (prefs.hapticsEnabled) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSquareClick(sq)
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            // Bottom Captured Pieces Strip
            CapturedStrip(
                label = if (bottomColor == matchState.playerColor) "أنت (${prefs.playerElo} ELO)" else matchState.bot.nameAr,
                capturedPieces = if (bottomColor == PieceColor.WHITE) capturedPair.first else capturedPair.second,
                advantage = if (bottomColor == PieceColor.WHITE) materialDiff else -materialDiff,
                pieceStyle = prefs.pieceStyle
            )

            // Live Coach Feedback / Hint / Review Note Banner
            val activeMoveNote: Pair<MoveQuality, String>? = when {
                matchState.reviewIndex != null && matchState.reviewIndex in matchState.recordedMoves.indices -> {
                    val rm = matchState.recordedMoves[matchState.reviewIndex]
                    rm.quality to "${rm.san}: ${rm.coachNoteAr}"
                }
                matchState.currentHint != null -> {
                    MoveQuality.BEST to "تلميح (${matchState.currentHint.san}): ${matchState.currentHint.explanationAr}"
                }
                else -> matchState.lastMoveQualityBadge
            }

            AnimatedVisibility(visible = activeMoveNote != null) {
                if (activeMoveNote != null) {
                    val qColor = Color(activeMoveNote.first.colorHex)
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = qColor.copy(alpha = 0.14f)
                        ),
                        border = BorderStroke(1.dp, qColor.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = qColor
                            ) {
                                Text(
                                    text = "${activeMoveNote.first.badge} ${activeMoveNote.first.labelAr}",
                                    color = Color(0xFF0B111E),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Text(
                                text = activeMoveNote.second,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (matchState.isReviewingPastMove) {
                                IconButton(
                                    onClick = { onSelectReviewMove(null) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Rounded.Close, contentDescription = "العودة للمباراة", modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }

            // Interactive Horizontal Move History Log
            if (matchState.recordedMoves.isNotEmpty()) {
                LazyRow(
                    state = moveListState,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(vertical = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("move_history_row")
                ) {
                    itemsIndexed(matchState.recordedMoves) { idx, rec ->
                        val isSelected = matchState.reviewIndex == idx
                        val qColor = Color(rec.quality.colorHex)
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            border = if (isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                            modifier = Modifier.clickable {
                                onSelectReviewMove(if (isSelected) null else idx)
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (rec.color == PieceColor.WHITE) {
                                    Text(
                                        text = "${rec.moveNumber}.",
                                        fontFamily = MonoFontFamily,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = rec.san,
                                    fontFamily = MonoFontFamily,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (rec.color == matchState.playerColor) {
                                    Text(
                                        text = rec.quality.badge,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = qColor
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Match Action Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (matchState.gameResult == GameResult.ONGOING) {
                    OutlinedButton(
                        onClick = onRequestHint,
                        enabled = !matchState.isBotThinking && !matchState.isAnalyzingHint,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("hint_button"),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Rounded.Lightbulb, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(if (matchState.isAnalyzingHint) "تحليل..." else "تلميح", style = MaterialTheme.typography.labelMedium)
                    }

                    OutlinedButton(
                        onClick = onUndo,
                        enabled = !matchState.isBotThinking && matchState.recordedMoves.isNotEmpty(),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("undo_button"),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.Undo, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("تراجع", style = MaterialTheme.typography.labelMedium)
                    }

                    OutlinedButton(
                        onClick = onFlipBoard,
                        modifier = Modifier.weight(0.9f),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Rounded.SwapVert, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("عكس", style = MaterialTheme.typography.labelMedium)
                    }

                    OutlinedButton(
                        onClick = onResign,
                        modifier = Modifier
                            .weight(0.9f)
                            .testTag("resign_button"),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Rounded.Flag, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("إنهاء", style = MaterialTheme.typography.labelMedium)
                    }
                } else {
                    Button(
                        onClick = onOpenPostGameSheet,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("view_report_button")
                    ) {
                        Icon(Icons.Rounded.Analytics, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("تقرير وتحليل المباراة")
                    }
                    OutlinedButton(
                        onClick = onStartRematch,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("rematch_button")
                    ) {
                        Icon(Icons.Rounded.Refresh, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("مباراة جديدة")
                    }
                }
            }
        }

        // Promotion Dialog
        if (matchState.pendingPromotion != null) {
            PromotionPickerDialog(
                color = matchState.playerColor,
                pieceStyle = prefs.pieceStyle,
                onSelect = onSelectPromotion,
                onDismiss = onDismissPromotion
            )
        }

        // Post-Game Comprehensive Analysis Bottom Sheet
        if (matchState.showPostGameSheet && matchState.postGameReport != null) {
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ModalBottomSheet(
                onDismissRequest = onDismissPostGameSheet,
                sheetState = sheetState
            ) {
                PostGameAnalysisContent(
                    result = matchState.gameResult,
                    report = matchState.postGameReport,
                    unlockedAchievements = matchState.newlyUnlockedAchievements,
                    onReviewMoves = {
                        onDismissPostGameSheet()
                        if (matchState.recordedMoves.isNotEmpty()) {
                            onSelectReviewMove(0)
                        }
                    },
                    onPlayAgain = {
                        onDismissPostGameSheet()
                        onStartRematch()
                    }
                )
            }
        }
    }
}

@Composable
private fun CapturedStrip(
    label: String,
    capturedPieces: List<ChessPiece>,
    advantage: Int,
    pieceStyle: com.example.chess.PieceStyle
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            capturedPieces.take(12).forEach { p ->
                ChessPieceGraphic(piece = p, pieceSize = 18.dp, pieceStyle = pieceStyle)
            }
            if (advantage > 0) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "+$advantage",
                        fontFamily = MonoFontFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PostGameAnalysisContent(
    result: GameResult,
    report: PostGameReport,
    unlockedAchievements: List<AchievementDef>,
    onReviewMoves: () -> Unit,
    onPlayAgain: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp),
        contentPadding = PaddingValues(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = result.labelAr,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "الافتتاح: ${report.openingNameAr}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Accuracy & ELO Delta Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricBox(
                    title = "دقتك",
                    value = "${report.playerAccuracy}%",
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                MetricBox(
                    title = "متوسط الفقد",
                    value = "${report.acpl} cp",
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.weight(1f)
                )
                MetricBox(
                    title = "تغير ELO",
                    value = if (report.eloDelta >= 0) "+${report.eloDelta}" else "${report.eloDelta}",
                    color = if (report.eloDelta >= 0) MaterialTheme.colorScheme.primary else MoveBlunderColor,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Newly Unlocked Trophies
        if (unlockedAchievements.isNotEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Rounded.EmojiEvents, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                            Text("جوائز جديدة مفتوحة!", style = MaterialTheme.typography.titleSmall)
                        }
                        unlockedAchievements.forEach { ach ->
                            Text(
                                text = "• ${ach.titleAr}: ${ach.descriptionAr} (+${ach.xpBonus} XP)",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }

        // Phase Accuracy Breakdown
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("الدقة حسب مراحل المباراة", style = MaterialTheme.typography.titleSmall)
                    PhaseGaugeRow("الافتتاح", report.openingAccuracy, MaterialTheme.colorScheme.primary)
                    PhaseGaugeRow("وسط اللعب", report.middlegameAccuracy, MaterialTheme.colorScheme.tertiary)
                    PhaseGaugeRow("نهاية اللعب", report.endgameAccuracy, MaterialTheme.colorScheme.secondary)
                }
            }
        }

        // Move Quality Counts
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val highlights = listOf(
                    MoveQuality.BRILLIANT,
                    MoveQuality.BEST,
                    MoveQuality.INACCURACY,
                    MoveQuality.MISTAKE,
                    MoveQuality.BLUNDER
                )
                highlights.forEach { q ->
                    val count = report.qualityCounts[q] ?: 0
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        color = Color(q.colorHex).copy(alpha = 0.14f)
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "$count",
                                fontFamily = MonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = Color(q.colorHex),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = q.badge,
                                fontSize = 11.sp,
                                color = Color(q.colorHex)
                            )
                        }
                    }
                }
            }
        }

        // Strengths
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                )
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("نقاط القوة", style = MaterialTheme.typography.titleSmall)
                    }
                    report.strengthsAr.forEach {
                        Text("• $it", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        // Weaknesses & Diagnosis
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MoveMistakeColor.copy(alpha = 0.12f)
                )
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Rounded.WarningAmber, contentDescription = null, tint = MoveInaccuracyColor)
                        Text("نقاط الضعف وتشخيص المشاكل", style = MaterialTheme.typography.titleSmall)
                    }
                    Text(
                        text = report.diagnosisAr,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    report.weaknessesAr.forEach {
                        Text("• $it", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        // Actionable Coaching Advice
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f)
                )
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                        Text("خطة التحسين للمباراة القادمة", style = MaterialTheme.typography.titleSmall)
                    }
                    report.actionPlanAr.forEach {
                        Text("• $it", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        item {
            HorizontalDivider()
            Spacer(Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = onReviewMoves, modifier = Modifier.weight(1f)) {
                    Text("مراجعة النقلات")
                }
                Button(onClick = onPlayAgain, modifier = Modifier.weight(1f)) {
                    Text("مباراة جديدة")
                }
            }
        }
    }
}

@Composable
private fun MetricBox(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                fontFamily = MonoFontFamily,
                fontWeight = FontWeight.Bold,
                color = color,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
