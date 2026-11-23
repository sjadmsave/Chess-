package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.FormatListBulleted
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Balance
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PictureInPictureAlt
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chess.ChessCoachAnalyzer
import com.example.chess.ChessPiece
import com.example.chess.GameMode
import com.example.chess.GameResult
import com.example.chess.HintStage
import com.example.chess.MoveQuality
import com.example.chess.MultiPvLine
import com.example.chess.PieceColor
import com.example.chess.PieceStyle
import com.example.chess.PieceType
import com.example.chess.PostGameReport
import com.example.chess.RecordedMove
import com.example.chess.Square
import com.example.chess.TimeControl
import com.example.data.AchievementDef
import com.example.data.UserPreferences
import com.example.ui.components.ChessBoardView
import com.example.ui.components.ChessPieceGraphic
import com.example.ui.components.CompactMoveHistoryStrip
import com.example.ui.components.EvaluationBar
import com.example.ui.components.EvaluationSwingChart
import com.example.ui.components.MoveHistoryLedgerPanel
import com.example.ui.components.PhaseGaugeRow
import com.example.ui.components.PromotionPickerDialog
import com.example.ui.theme.DangerRed
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.MoveBlunderColor
import com.example.ui.theme.MoveInaccuracyColor
import com.example.ui.theme.MoveMistakeColor
import com.example.ui.theme.formatArabicDigits
import com.example.ui.theme.formatArabicNumber
import com.example.viewmodel.ActiveMatchUiState
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayScreen(
    matchState: ActiveMatchUiState,
    prefs: UserPreferences,
    onNavigateBack: () -> Unit,
    onSquareClick: (Square) -> Unit,
    onSelectPromotion: (PieceType) -> Unit,
    onDismissPromotion: () -> Unit,
    onRequestHint: () -> Unit,
    onUndo: () -> Unit,
    onFlipBoard: () -> Unit,
    onResign: () -> Unit,
    onClaimDraw: () -> Unit,
    onSelectReviewMove: (Int?) -> Unit,
    onOpenMoveLedger: () -> Unit,
    onDismissMoveLedger: () -> Unit,
    onOpenPostGameSheet: () -> Unit,
    onDismissPostGameSheet: () -> Unit,
    onStartOrResumeFullReview: () -> Unit,
    onPauseFullReview: () -> Unit,
    onAnalyzeMultiPv: () -> Unit,
    onExportPgn: () -> Unit,
    onStartRematch: () -> Unit,
    onToggleEvalBar: (Boolean) -> Unit,
    onOpenInFloatingWindow: (String, PieceColor) -> Unit,
    engineTelemetry: String = ""
) {
    val haptic = LocalHapticFeedback.current
    val clipboard = LocalClipboardManager.current
    var showOptionsSheet by remember { mutableStateOf(false) }
    var copiedMessage by remember { mutableStateOf<String?>(null) }

    val displayedBoard = matchState.displayedBoardState
    val capturedPair = remember(displayedBoard) { displayedBoard.capturedPiecesFromStart() }
    val materialDiff = remember(displayedBoard) { displayedBoard.materialBalanceWhite() }

    val topColor = if (matchState.flippedBoard) PieceColor.WHITE else PieceColor.BLACK
    val bottomColor = topColor.opposite()

    val isLocalTwoPlayer = matchState.gameMode == GameMode.LOCAL_TWO_PLAYER
    val topIsPlayer = isLocalTwoPlayer || topColor == matchState.playerColor
    val bottomIsPlayer = isLocalTwoPlayer || bottomColor == matchState.playerColor

    val topTimeSec = if (topColor == PieceColor.WHITE) matchState.whiteTimeSec else matchState.blackTimeSec
    val bottomTimeSec = if (bottomColor == PieceColor.WHITE) matchState.whiteTimeSec else matchState.blackTimeSec

    val isPlayerTurn = matchState.gameResult == GameResult.ONGOING &&
        (isLocalTwoPlayer || matchState.boardState.turn == matchState.playerColor)
    val lastSan = matchState.recordedMoves.lastOrNull()?.san

    val hintStageLabel = when (matchState.currentHintStage) {
        null -> "تلميح ١: فكرة"
        HintStage.CONCEPT -> "تلميح ٢: قطعة"
        HintStage.PIECE, HintStage.EXACT -> "تلميح ٣: نقلة"
    }

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
            // 1) Header: Back + Title + Turn Indicator + Last SAN
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("play_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ChevronRight,
                            contentDescription = "رجوع",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Column {
                        Text(
                            text = if (isLocalTwoPlayer) "مباراة محلية" else "المباراة",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1
                        )
                        Text(
                            text = formatArabicDigits(matchState.matchSubtitleAr),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (lastSan != null) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Text(
                                text = lastSan,
                                fontFamily = MonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isPlayerTurn) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        }
                    ) {
                        Text(
                            text = when {
                                matchState.gameResult != GameResult.ONGOING -> matchState.gameResult.labelAr
                                matchState.isBotThinking -> "يفكر..."
                                isLocalTwoPlayer -> if (matchState.boardState.turn == PieceColor.WHITE) "دور الأبيض" else "دور الأسود"
                                isPlayerTurn -> "دورك"
                                else -> "دور الخصم"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isPlayerTurn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            if (engineTelemetry.isNotBlank()) {
                Text(
                    text = formatArabicDigits(engineTelemetry),
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = MonoFontFamily,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp)
                        .testTag("engine_telemetry_label")
                )
            }

            // 2) Compact Algebraic Move History Strip
            if (matchState.recordedMoves.isNotEmpty()) {
                CompactMoveHistoryStrip(
                    moves = matchState.recordedMoves,
                    openingNameAr = matchState.openingNameAr,
                    activeReviewIndex = matchState.reviewIndex,
                    playerColor = matchState.playerColor,
                    onSelectMove = onSelectReviewMove,
                    onOpenFullLedger = onOpenMoveLedger
                )
            }

            // 3) Top Player Card
            ArenaPlayerBanner(
                isPlayer = topIsPlayer,
                sideColor = topColor,
                matchState = matchState,
                playerElo = prefs.playerElo,
                subtitleAr = when {
                    isLocalTwoPlayer -> if (topColor == PieceColor.WHITE) "اللاعب الأبيض • محلي" else "اللاعب الأسود • محلي"
                    topIsPlayer -> "أنت • ${if (topColor == PieceColor.WHITE) "الأبيض" else "الأسود"}"
                    else -> formatArabicDigits(matchState.botChatMessageAr)
                },
                isThinking = !topIsPlayer && matchState.isBotThinking,
                isActiveTurn = matchState.gameResult == GameResult.ONGOING && matchState.boardState.turn == topColor,
                timeControl = matchState.timeControl,
                remainingSeconds = topTimeSec,
                capturedPieces = if (topColor == PieceColor.WHITE) capturedPair.first else capturedPair.second,
                advantage = if (topColor == PieceColor.WHITE) materialDiff else -materialDiff,
                pieceStyle = prefs.pieceStyle
            )

            // 4) Main Chess Board + Optional Eval Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
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
                            .height(310.dp)
                    )
                }

                ChessBoardView(
                    boardState = displayedBoard,
                    boardTheme = prefs.boardTheme,
                    pieceStyle = prefs.pieceStyle,
                    selectedSquare = matchState.selectedSquare,
                    legalMovesForSelected = matchState.legalMovesForSelected,
                    lastMove = matchState.displayedLastMove,
                    hintMove = if (matchState.currentHintStage == HintStage.EXACT) matchState.currentHint?.move else null,
                    flipped = matchState.flippedBoard,
                    interactive = matchState.gameResult == GameResult.ONGOING && !matchState.isBotThinking,
                    boardMaterial = prefs.boardMaterial,
                    showLastMove = prefs.showLastMove,
                    showLegalMoves = prefs.showLegalMoves,
                    useRingsInsteadOfDots = prefs.useRingsInsteadOfDots,
                    selectedHighlightColor = Color(prefs.selectedColorHex),
                    lastMoveHighlightColor = Color(prefs.lastMoveColorHex),
                    legalMoveIndicatorColor = Color(prefs.legalMoveColorHex),
                    onSquareClick = { sq ->
                        if (prefs.hapticsEnabled) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSquareClick(sq)
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            // 5) Bottom Player Card
            ArenaPlayerBanner(
                isPlayer = bottomIsPlayer,
                sideColor = bottomColor,
                matchState = matchState,
                playerElo = prefs.playerElo,
                subtitleAr = when {
                    isLocalTwoPlayer -> if (bottomColor == PieceColor.WHITE) "اللاعب الأبيض • محلي" else "اللاعب الأسود • محلي"
                    bottomIsPlayer -> "أنت • ${if (bottomColor == PieceColor.WHITE) "الأبيض" else "الأسود"}"
                    else -> formatArabicDigits(matchState.botChatMessageAr)
                },
                isThinking = !bottomIsPlayer && matchState.isBotThinking,
                isActiveTurn = matchState.gameResult == GameResult.ONGOING && matchState.boardState.turn == bottomColor,
                timeControl = matchState.timeControl,
                remainingSeconds = bottomTimeSec,
                capturedPieces = if (bottomColor == PieceColor.WHITE) capturedPair.first else capturedPair.second,
                advantage = if (bottomColor == PieceColor.WHITE) materialDiff else -materialDiff,
                pieceStyle = prefs.pieceStyle
            )

            // 5b) Claimable Draw Banner (50-move or Threefold Repetition)
            AnimatedVisibility(visible = matchState.gameResult == GameResult.ONGOING && matchState.claimableDrawReason != null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Rounded.Balance, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp))
                            Text(
                                text = "متاح: ${formatArabicDigits(matchState.claimableDrawReason.orEmpty())}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Button(
                            onClick = onClaimDraw,
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("claim_draw_button"),
                            contentPadding = PaddingValues(horizontal = 12.dp)
                        ) {
                            Text("طلب تعادل", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // 6) Hint / Move Evaluation / MultiPV-3 / Full Review Banner
            val activeMoveNote: Pair<MoveQuality, String>? = when {
                matchState.reviewIndex != null && matchState.reviewIndex in matchState.recordedMoves.indices -> {
                    val rm = matchState.recordedMoves[matchState.reviewIndex]
                    val grade = ChessCoachAnalyzer.gradeLabelFromLoss(rm.centipawnLoss)
                    rm.quality to "${rm.san} ($grade • ${formatArabicNumber(rm.centipawnLoss)} cp): ${rm.coachNoteAr}"
                }
                matchState.currentHint != null -> {
                    val stage = matchState.currentHintStage ?: HintStage.CONCEPT
                    MoveQuality.BEST to "${stage.titleAr} (${formatArabicNumber(stage.step)}/٣): ${matchState.currentHint.explanationAr}"
                }
                else -> matchState.lastMoveQualityBadge
            }

            AnimatedVisibility(visible = activeMoveNote != null || copiedMessage != null) {
                val note = activeMoveNote
                if (copiedMessage != null) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(copiedMessage!!, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            IconButton(onClick = { copiedMessage = null }, modifier = Modifier.size(48.dp)) {
                                Icon(Icons.Rounded.Close, contentDescription = "إغلاق", modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                } else if (note != null) {
                    val qColor = Color(note.first.colorHex)
                    Card(
                        colors = CardDefaults.cardColors(containerColor = qColor.copy(alpha = 0.14f)),
                        border = BorderStroke(1.dp, qColor.copy(alpha = 0.55f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(shape = RoundedCornerShape(6.dp), color = qColor) {
                                Text(
                                    text = "${note.first.badge} ${note.first.labelAr}",
                                    color = Color(0xFF141414),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Text(
                                text = note.second,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (matchState.currentHint != null &&
                                matchState.currentHintStage != null &&
                                matchState.currentHintStage != HintStage.EXACT
                            ) {
                                TextButton(
                                    onClick = onRequestHint,
                                    modifier = Modifier
                                        .heightIn(min = 48.dp)
                                        .testTag("next_hint_stage_btn")
                                ) {
                                    Text("التالي", fontWeight = FontWeight.Bold)
                                }
                            }
                            if (matchState.isReviewingPastMove) {
                                IconButton(
                                    onClick = { onSelectReviewMove(null) },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(Icons.Rounded.Close, contentDescription = "العودة للوضع المباشر", modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }

            // 6b) Post-Game Pausable Review Progress Card
            AnimatedVisibility(visible = matchState.gameResult != GameResult.ONGOING && matchState.recordedMoves.isNotEmpty()) {
                val total = matchState.recordedMoves.size.coerceAtLeast(1)
                val done = matchState.fullReviewNextIndex.coerceIn(0, total)
                val progress = done.toFloat() / total.toFloat()
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = when {
                                    matchState.fullReviewCompleted -> "مراجعة المحرك مكتملة (${formatArabicNumber(total)} / ${formatArabicNumber(total)})"
                                    matchState.isFullReviewRunning -> "جارٍ تحليل النقلات (${formatArabicNumber(done)} / ${formatArabicNumber(total)})..."
                                    matchState.isFullReviewPaused -> "تم إيقاف التحليل مؤقتاً (${formatArabicNumber(done)} / ${formatArabicNumber(total)})"
                                    else -> "تحليل المباراة الكامل جاهز"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                            )
                        }
                        if (!matchState.fullReviewCompleted) {
                            OutlinedButton(
                                onClick = {
                                    if (matchState.isFullReviewRunning) onPauseFullReview() else onStartOrResumeFullReview()
                                },
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("toggle_full_review_button"),
                                contentPadding = PaddingValues(horizontal = 10.dp)
                            ) {
                                Icon(
                                    imageVector = if (matchState.isFullReviewRunning) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = when {
                                        matchState.isFullReviewRunning -> "إيقاف"
                                        matchState.isFullReviewPaused -> "استئناف"
                                        else -> "تحليل"
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // 6c) Interactive MultiPV-3 Card (best 3 engine lines for displayed position)
            AnimatedVisibility(visible = matchState.isAnalyzingMultiPv || matchState.multiPvLines.isNotEmpty()) {
                MultiPvLinesCard(
                    lines = matchState.multiPvLines,
                    isLoading = matchState.isAnalyzingMultiPv,
                    onRefresh = onAnalyzeMultiPv
                )
            }

            // 7) Clean Bottom Bar (>= 48.dp touch targets)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (matchState.gameResult == GameResult.ONGOING) {
                    OutlinedButton(
                        onClick = { showOptionsSheet = true },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("match_options_button"),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        contentPadding = PaddingValues(horizontal = 6.dp)
                    ) {
                        Icon(Icons.Rounded.Tune, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("خيارات", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onOpenMoveLedger,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("toolbar_ledger_button"),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        contentPadding = PaddingValues(horizontal = 6.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.FormatListBulleted, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("السجل", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onFlipBoard,
                        modifier = Modifier
                            .weight(1.1f)
                            .heightIn(min = 48.dp)
                            .testTag("flip_board_button"),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        contentPadding = PaddingValues(horizontal = 6.dp)
                    ) {
                        Icon(Icons.Rounded.SwapVert, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("قلب الرقعة", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = onOpenPostGameSheet,
                        modifier = Modifier
                            .weight(1.2f)
                            .heightIn(min = 48.dp)
                            .testTag("view_report_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Rounded.Analytics, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("التقرير", fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = onAnalyzeMultiPv,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("analyze_multipv_button"),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp)
                    ) {
                        Icon(Icons.Rounded.Psychology, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("٣ خطوط", maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = onExportPgn,
                        modifier = Modifier
                            .weight(0.85f)
                            .heightIn(min = 48.dp)
                            .testTag("post_export_pgn_button"),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp)
                    ) {
                        Icon(Icons.Rounded.FileDownload, contentDescription = "تصدير PGN", modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(3.dp))
                        Text("PGN", maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = onStartRematch,
                        modifier = Modifier
                            .weight(0.95f)
                            .heightIn(min = 48.dp)
                            .testTag("rematch_button"),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp)
                    ) {
                        Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("جديدة", maxLines = 1)
                    }
                }
            }
        }

        // Promotion Dialog (handles both local players seamlessly)
        if (matchState.pendingPromotion != null) {
            PromotionPickerDialog(
                color = matchState.boardState.turn,
                pieceStyle = prefs.pieceStyle,
                onSelect = onSelectPromotion,
                onDismiss = onDismissPromotion
            )
        }

        // In-Match Options Bottom Sheet
        if (showOptionsSheet) {
            val optionsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ModalBottomSheet(
                onDismissRequest = { showOptionsSheet = false },
                sheetState = optionsSheetState
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "خيارات المباراة",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )

                    OptionSheetActionRow(
                        title = if (matchState.isAnalyzingHint) "جاري حساب التلميح..." else hintStageLabel,
                        subtitle = "نظام تلميح من ٣ مراحل: فكرة عامة ← القطعة ← النقلة",
                        icon = Icons.Rounded.Lightbulb,
                        enabled = !matchState.isBotThinking && !matchState.isAnalyzingHint,
                        tag = "hint_button",
                        onClick = {
                            showOptionsSheet = false
                            onRequestHint()
                        }
                    )

                    OptionSheetActionRow(
                        title = if (matchState.isAnalyzingMultiPv) "جاري تحليل أفضل ٣ خطوط..." else "تحليل الوضعية (أفضل ٣ خطوط)",
                        subtitle = "يعرض المحرك أفضل ٣ خيارات مع التقييم والعمق",
                        icon = Icons.Rounded.Psychology,
                        enabled = !matchState.isBotThinking && !matchState.isAnalyzingMultiPv,
                        tag = "options_multipv_btn",
                        onClick = {
                            showOptionsSheet = false
                            onAnalyzeMultiPv()
                        }
                    )

                    OptionSheetActionRow(
                        title = "تراجع عن النقلة",
                        subtitle = if (isLocalTwoPlayer) "الرجوع نقلة واحدة للخلف" else "الرجوع لدورك السابق",
                        icon = Icons.AutoMirrored.Rounded.Undo,
                        enabled = !matchState.isBotThinking && matchState.recordedMoves.isNotEmpty(),
                        tag = "undo_button",
                        onClick = {
                            showOptionsSheet = false
                            onUndo()
                        }
                    )

                    if (matchState.claimableDrawReason != null) {
                        OptionSheetActionRow(
                            title = "طلب تعادل",
                            subtitle = matchState.claimableDrawReason,
                            icon = Icons.Rounded.Balance,
                            enabled = true,
                            tag = "options_claim_draw_btn",
                            onClick = {
                                showOptionsSheet = false
                                onClaimDraw()
                            }
                        )
                    }

                    OptionSheetActionRow(
                        title = "نقل إلى النافذة العائمة",
                        subtitle = "أكمل نفس الوضعية في نافذة عائمة فوق التطبيقات",
                        icon = Icons.Rounded.PictureInPictureAlt,
                        enabled = true,
                        tag = "open_in_floating_btn",
                        onClick = {
                            showOptionsSheet = false
                            onOpenInFloatingWindow(matchState.boardState.toFen(), matchState.playerColor)
                        }
                    )

                    OptionSheetActionRow(
                        title = "تصدير ملف PGN (Ruqaa.pgn)",
                        subtitle = "حفظ المباراة عبر منتقي الملفات",
                        icon = Icons.Rounded.FileDownload,
                        enabled = true,
                        tag = "export_pgn_file_btn",
                        onClick = {
                            showOptionsSheet = false
                            onExportPgn()
                        }
                    )

                    OptionSheetActionRow(
                        title = "نسخ FEN للحافظة",
                        subtitle = "نسخ نص الوضعية الحالية",
                        icon = Icons.Rounded.ContentCopy,
                        enabled = true,
                        tag = "copy_fen_btn",
                        onClick = {
                            clipboard.setText(AnnotatedString(matchState.displayedBoardState.toFen()))
                            copiedMessage = "تم نسخ FEN إلى الحافظة بنجاح"
                            showOptionsSheet = false
                        }
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("شريط التقييم الحي", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Switch(checked = prefs.showEvalBar, onCheckedChange = onToggleEvalBar)
                    }

                    OptionSheetActionRow(
                        title = "استسلام وإنهاء الدور",
                        subtitle = "حفظ المباراة وعرض تقرير الأخطاء",
                        icon = Icons.Rounded.Flag,
                        enabled = matchState.gameResult == GameResult.ONGOING,
                        tint = DangerRed,
                        tag = "resign_button",
                        onClick = {
                            showOptionsSheet = false
                            onResign()
                        }
                    )

                    Spacer(Modifier.height(16.dp))
                }
            }
        }

        // Full Algebraic Move Ledger Bottom Sheet
        if (matchState.showMoveLedgerSheet) {
            val ledgerSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ModalBottomSheet(
                onDismissRequest = onDismissMoveLedger,
                sheetState = ledgerSheetState
            ) {
                MoveHistoryLedgerPanel(
                    moves = matchState.recordedMoves,
                    openingNameAr = matchState.openingNameAr,
                    activeReviewIndex = matchState.reviewIndex,
                    playerColor = matchState.playerColor,
                    onSelectMove = { idx -> onSelectReviewMove(idx) }
                )
            }
        }

        // Post-Game Comprehensive Game Review Bottom Sheet
        if (matchState.showPostGameSheet && matchState.postGameReport != null) {
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ModalBottomSheet(
                onDismissRequest = onDismissPostGameSheet,
                sheetState = sheetState
            ) {
                PostGameAnalysisContent(
                    result = matchState.gameResult,
                    opponentLabel = when {
                        isLocalTwoPlayer -> "الخصم"
                        matchState.usingCustomEngine -> matchState.customPersonality.nameAr
                        else -> matchState.bot.nameAr
                    },
                    opponentColor = if (matchState.usingCustomEngine || isLocalTwoPlayer) MaterialTheme.colorScheme.primary else matchState.bot.accentColor,
                    moves = matchState.recordedMoves,
                    report = matchState.postGameReport,
                    unlockedAchievements = matchState.newlyUnlockedAchievements,
                    onReviewMoves = {
                        onDismissPostGameSheet()
                        if (matchState.recordedMoves.isNotEmpty()) {
                            onSelectReviewMove(0)
                        }
                    },
                    onExportPgn = {
                        onDismissPostGameSheet()
                        onExportPgn()
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
private fun MultiPvLinesCard(
    lines: List<MultiPvLine>,
    isLoading: Boolean,
    onRefresh: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "أفضل ٣ خطوط للوضعية",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                TextButton(
                    onClick = onRefresh,
                    enabled = !isLoading,
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text(if (isLoading) "جاري..." else "تحديث", fontWeight = FontWeight.Bold)
                }
            }
            if (isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            } else {
                lines.take(3).forEach { line ->
                    val evalLabel = if (line.mateIn != null) {
                        val m = line.mateIn
                        if (m > 0) "+M${formatArabicNumber(m)}" else "-M${formatArabicNumber(-m)}"
                    } else {
                        val pawns = line.evalWhiteCp / 100.0
                        val formatted = String.format(Locale.US, "%+.1f", pawns)
                        formatArabicDigits(formatted)
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                            .padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = evalLabel,
                                fontFamily = MonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                            Text(
                                text = if (line.pvSan.isNotEmpty()) line.pvSan.take(4).joinToString(" ") else line.san,
                                fontFamily = MonoFontFamily,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = "عمق ${formatArabicNumber(line.depth)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OptionSheetActionRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    enabled: Boolean,
    tag: String,
    tint: Color = MaterialTheme.colorScheme.primary,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .testTag(tag),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
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
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(tint.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun ArenaPlayerBanner(
    isPlayer: Boolean,
    sideColor: PieceColor,
    matchState: ActiveMatchUiState,
    playerElo: Int,
    subtitleAr: String,
    isThinking: Boolean,
    isActiveTurn: Boolean,
    timeControl: TimeControl,
    remainingSeconds: Long,
    capturedPieces: List<ChessPiece>,
    advantage: Int,
    pieceStyle: PieceStyle
) {
    val bot = matchState.bot
    val isLocalTwoPlayer = matchState.gameMode == GameMode.LOCAL_TWO_PLAYER
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = if (isActiveTurn) 1.5.dp else 1.dp,
            color = if (isActiveTurn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
        )
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isPlayer || matchState.usingCustomEngine || isLocalTwoPlayer) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when {
                                isLocalTwoPlayer -> Icons.Rounded.Group
                                isPlayer -> Icons.Rounded.Person
                                else -> Icons.Rounded.AutoAwesome
                            },
                            contentDescription = if (isPlayer) "اللاعب" else "المحرك",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                } else {
                    Image(
                        painter = painterResource(id = bot.avatarRes),
                        contentDescription = bot.nameAr,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.5.dp, bot.accentColor, RoundedCornerShape(8.dp))
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = when {
                                isLocalTwoPlayer -> if (sideColor == PieceColor.WHITE) "الأبيض (محلي)" else "الأسود (محلي)"
                                isPlayer -> "أنت"
                                matchState.usingCustomEngine -> matchState.customPersonality.nameAr
                                else -> bot.nameAr
                            },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        if (!isLocalTwoPlayer) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = when {
                                        isPlayer -> formatArabicNumber(playerElo)
                                        matchState.usingCustomEngine -> {
                                            if (matchState.customEngineElo <= 0) "أقصى"
                                            else formatArabicNumber(matchState.customEngineElo)
                                        }
                                        matchState.isAdaptiveMatch -> "تكيفي"
                                        else -> formatArabicNumber(bot.elo)
                                    },
                                    fontFamily = MonoFontFamily,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                        }
                        if (capturedPieces.isNotEmpty() || advantage > 0) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(1.dp)
                            ) {
                                capturedPieces.take(6).forEach { p ->
                                    ChessPieceGraphic(piece = p, pieceSize = 13.dp, pieceStyle = pieceStyle)
                                }
                                if (advantage > 0) {
                                    Text(
                                        text = "+${formatArabicNumber(advantage)}",
                                        fontFamily = MonoFontFamily,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(start = 2.dp)
                                    )
                                }
                            }
                        }
                    }

                    Text(
                        text = subtitleAr,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (timeControl != TimeControl.UNLIMITED) {
                    val isLowTime = remainingSeconds in 1..30
                    val clockBg = when {
                        isActiveTurn && isLowTime -> DangerRed
                        isActiveTurn -> MaterialTheme.colorScheme.surfaceVariant
                        else -> MaterialTheme.colorScheme.background
                    }
                    val clockTextColor = when {
                        isActiveTurn && isLowTime -> Color.White
                        isActiveTurn -> MaterialTheme.colorScheme.onSurface
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = clockBg,
                        border = if (isActiveTurn) BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)) else null
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Timer,
                                contentDescription = null,
                                tint = clockTextColor,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = formatClockSeconds(remainingSeconds),
                                fontFamily = MonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = clockTextColor
                            )
                        }
                    }
                }
            }

            if (isThinking) {
                Spacer(Modifier.height(4.dp))
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.5.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

private fun formatClockSeconds(totalSec: Long): String {
    val safe = totalSec.coerceAtLeast(0L)
    val mins = safe / 60
    val secs = safe % 60
    return formatArabicDigits(String.format(Locale.US, "%02d:%02d", mins, secs))
}

@Composable
private fun PostGameAnalysisContent(
    result: GameResult,
    opponentLabel: String,
    opponentColor: Color,
    moves: List<RecordedMove>,
    report: PostGameReport,
    unlockedAchievements: List<AchievementDef>,
    onReviewMoves: () -> Unit,
    onExportPgn: () -> Unit,
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
                    text = "مراجعة المباراة • ${result.labelAr}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "الافتتاح: ${report.openingNameAr}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricBox("دقتك", "${formatArabicNumber(report.playerAccuracy)}٪", MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                MetricBox("دقة $opponentLabel", "${formatArabicNumber(report.opponentAccuracy)}٪", opponentColor, Modifier.weight(1f))
                MetricBox("فقدان", "${formatArabicNumber(report.acpl)} cp", MaterialTheme.colorScheme.tertiary, Modifier.weight(1f))
                MetricBox(
                    "ELO",
                    if (report.eloDelta >= 0) "+${formatArabicNumber(report.eloDelta)}" else formatArabicNumber(report.eloDelta),
                    if (report.eloDelta >= 0) MaterialTheme.colorScheme.primary else MoveBlunderColor,
                    Modifier.weight(0.9f)
                )
            }
        }

        if (moves.size >= 2) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("مخطط الأفضلية عبر المباراة", style = MaterialTheme.typography.titleSmall)
                    EvaluationSwingChart(
                        moves = moves,
                        activeIndex = moves.lastIndex,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        if (unlockedAchievements.isNotEmpty()) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Rounded.EmojiEvents, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                            Text("جوائز جديدة مفتوحة!", style = MaterialTheme.typography.titleSmall)
                        }
                        unlockedAchievements.forEach { ach ->
                            Text("• ${ach.titleAr}: ${ach.descriptionAr} (+${formatArabicNumber(ach.xpBonus)} XP)", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("الدقة حسب مراحل المباراة", style = MaterialTheme.typography.titleSmall)
                    PhaseGaugeRow("الافتتاح", report.openingAccuracy, MaterialTheme.colorScheme.primary)
                    PhaseGaugeRow("وسط اللعب", report.middlegameAccuracy, MaterialTheme.colorScheme.tertiary)
                    PhaseGaugeRow("نهاية اللعب", report.endgameAccuracy, MaterialTheme.colorScheme.secondary)
                }
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(MoveQuality.BRILLIANT, MoveQuality.BEST, MoveQuality.INACCURACY, MoveQuality.MISTAKE, MoveQuality.BLUNDER).forEach { q ->
                    val count = report.qualityCounts[q] ?: 0
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        color = Color(q.colorHex).copy(alpha = 0.14f)
                    ) {
                        Column(modifier = Modifier.padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = formatArabicNumber(count),
                                fontFamily = MonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = Color(q.colorHex),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(text = q.badge, fontSize = 11.sp, color = Color(q.colorHex))
                        }
                    }
                }
            }
        }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f))) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("نقاط القوة", style = MaterialTheme.typography.titleSmall)
                    }
                    report.strengthsAr.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
                }
            }
        }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = MoveMistakeColor.copy(alpha = 0.12f))) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Rounded.WarningAmber, contentDescription = null, tint = MoveInaccuracyColor)
                        Text("نقاط الضعف وتشخيص المشاكل", style = MaterialTheme.typography.titleSmall)
                    }
                    Text(text = report.diagnosisAr, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    report.weaknessesAr.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
                }
            }
        }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f))) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                        Text("خطة التحسين للمباراة القادمة", style = MaterialTheme.typography.titleSmall)
                    }
                    report.actionPlanAr.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
                }
            }
        }

        item {
            HorizontalDivider()
            Spacer(Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onReviewMoves,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                ) { Text("مراجعة", maxLines = 1) }
                OutlinedButton(
                    onClick = onExportPgn,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                ) { Text("تصدير PGN", maxLines = 1) }
                Button(
                    onClick = onPlayAgain,
                    modifier = Modifier
                        .weight(1.1f)
                        .heightIn(min = 48.dp)
                ) { Text("مباراة جديدة", maxLines = 1) }
            }
        }
    }
}

@Composable
private fun MetricBox(title: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
        Column(modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = value, fontFamily = MonoFontFamily, fontWeight = FontWeight.Bold, color = color, style = MaterialTheme.typography.titleSmall, maxLines = 1)
            Text(text = title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
