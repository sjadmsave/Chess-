package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.FormatListBulleted
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.FirstPage
import androidx.compose.material.icons.rounded.LastPage
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chess.MoveQuality
import com.example.chess.PieceColor
import com.example.chess.RecordedMove
import com.example.ui.theme.MonoFontFamily

data class TurnEntry(
    val turnNumber: Int,
    val whiteMove: RecordedMove?,
    val whiteIndex: Int?,
    val blackMove: RecordedMove?,
    val blackIndex: Int?
)

fun groupMovesIntoTurns(moves: List<RecordedMove>): List<TurnEntry> {
    if (moves.isEmpty()) return emptyList()
    val result = mutableListOf<TurnEntry>()
    var i = 0
    while (i < moves.size) {
        val current = moves[i]
        if (current.color == PieceColor.WHITE) {
            val nextIsBlack = i + 1 < moves.size && moves[i + 1].color == PieceColor.BLACK
            result.add(
                TurnEntry(
                    turnNumber = current.moveNumber,
                    whiteMove = current,
                    whiteIndex = i,
                    blackMove = if (nextIsBlack) moves[i + 1] else null,
                    blackIndex = if (nextIsBlack) i + 1 else null
                )
            )
            i += if (nextIsBlack) 2 else 1
        } else {
            result.add(
                TurnEntry(
                    turnNumber = current.moveNumber,
                    whiteMove = null,
                    whiteIndex = null,
                    blackMove = current,
                    blackIndex = i
                )
            )
            i += 1
        }
    }
    return result
}

fun formatEvalPawns(evalWhiteCp: Int): String = when {
    evalWhiteCp >= 90000 -> "+M"
    evalWhiteCp <= -90000 -> "-M"
    else -> {
        val p = evalWhiteCp / 100f
        val sign = if (p > 0.05f) "+" else ""
        String.format(java.util.Locale.US, "%s%.1f", sign, p)
    }
}

/**
 * Compact Chess.com-style horizontal algebraic move strip with opening title and ledger toggle.
 */
@Composable
fun CompactMoveHistoryStrip(
    moves: List<RecordedMove>,
    openingNameAr: String,
    activeReviewIndex: Int?,
    playerColor: PieceColor,
    onSelectMove: (Int?) -> Unit,
    onOpenFullLedger: () -> Unit,
    modifier: Modifier = Modifier
) {
    val turns = remember(moves) { groupMovesIntoTurns(moves) }
    val rowState = rememberLazyListState()

    LaunchedEffect(turns.size, activeReviewIndex) {
        if (turns.isNotEmpty() && activeReviewIndex == null) {
            rowState.animateScrollToItem(turns.lastIndex)
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("compact_move_history_strip"),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Button to open full algebraic notation ledger
            Surface(
                onClick = onOpenFullLedger,
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.testTag("open_move_ledger_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.FormatListBulleted,
                        contentDescription = "سجل النقلات الكامل",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "${moves.size}",
                        fontFamily = MonoFontFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            if (moves.isEmpty()) {
                Text(
                    text = "بانتظار النقلة الأولى • $openingNameAr",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            } else {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    LazyRow(
                        state = rowState,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("move_history_row"),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        items(turns, key = { it.turnNumber }) { turn ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text(
                                    text = "${turn.turnNumber}.",
                                    fontFamily = MonoFontFamily,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (turn.whiteMove != null && turn.whiteIndex != null) {
                                    AlgebraicMoveChip(
                                        move = turn.whiteMove,
                                        isSelected = activeReviewIndex == turn.whiteIndex ||
                                            (activeReviewIndex == null && turn.whiteIndex == moves.lastIndex),
                                        showQuality = turn.whiteMove.color == playerColor,
                                        onClick = {
                                            onSelectMove(if (activeReviewIndex == turn.whiteIndex) null else turn.whiteIndex)
                                        }
                                    )
                                }
                                if (turn.blackMove != null && turn.blackIndex != null) {
                                    AlgebraicMoveChip(
                                        move = turn.blackMove,
                                        isSelected = activeReviewIndex == turn.blackIndex ||
                                            (activeReviewIndex == null && turn.blackIndex == moves.lastIndex),
                                        showQuality = turn.blackMove.color == playerColor,
                                        onClick = {
                                            onSelectMove(if (activeReviewIndex == turn.blackIndex) null else turn.blackIndex)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AlgebraicMoveChip(
    move: RecordedMove,
    isSelected: Boolean,
    showQuality: Boolean,
    onClick: () -> Unit
) {
    val qColor = Color(move.quality.colorHex)
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f) else Color.Transparent,
        border = if (isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = move.san,
                fontFamily = MonoFontFamily,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (showQuality && move.quality != MoveQuality.GOOD) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(qColor)
                )
            }
        }
    }
}

/**
 * Full Chess.com-style Turn-by-Turn Algebraic Notation Ledger + Evaluation Swing Graph + Playback Controls.
 */
@Composable
fun MoveHistoryLedgerPanel(
    moves: List<RecordedMove>,
    openingNameAr: String,
    activeReviewIndex: Int?,
    playerColor: PieceColor,
    onSelectMove: (Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    val turns = remember(moves) { groupMovesIntoTurns(moves) }
    val listState = rememberLazyListState()

    val currentIdx = activeReviewIndex ?: (moves.lastIndex.takeIf { it >= 0 })

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .testTag("full_move_history_ledger"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("سجل النقلات الجبري الكامل", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = openingNameAr,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = "${moves.size} نقلة",
                fontFamily = MonoFontFamily,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Chess.com-style Evaluation Swing Chart
        if (moves.size >= 2) {
            EvaluationSwingChart(
                moves = moves,
                activeIndex = currentIdx,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Table Header
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Surface(
                shape = RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "#",
                        fontFamily = MonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(32.dp)
                    )
                    Text(
                        text = "White",
                        fontFamily = MonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "Black",
                        fontFamily = MonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "Eval",
                        fontFamily = MonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(46.dp)
                    )
                }
            }

            if (turns.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "لم تُلعب أي نقلات بعد",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp),
                    contentPadding = PaddingValues(bottom = 8.dp)
                ) {
                    items(turns, key = { it.turnNumber }) { turn ->
                        val rowBg = if (turn.turnNumber % 2 == 0) {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        } else {
                            Color.Transparent
                        }
                        val lastEvalCp = turn.blackMove?.evalWhiteCp ?: turn.whiteMove?.evalWhiteCp ?: 0
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(rowBg)
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${turn.turnNumber}.",
                                fontFamily = MonoFontFamily,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.width(32.dp)
                            )
                            LedgerMoveCell(
                                move = turn.whiteMove,
                                selected = turn.whiteIndex != null && turn.whiteIndex == currentIdx,
                                showBadge = turn.whiteMove?.color == playerColor,
                                onClick = { turn.whiteIndex?.let { onSelectMove(it) } },
                                modifier = Modifier.weight(1f)
                            )
                            LedgerMoveCell(
                                move = turn.blackMove,
                                selected = turn.blackIndex != null && turn.blackIndex == currentIdx,
                                showBadge = turn.blackMove?.color == playerColor,
                                onClick = { turn.blackIndex?.let { onSelectMove(it) } },
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = formatEvalPawns(lastEvalCp),
                                fontFamily = MonoFontFamily,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (lastEvalCp >= 0) MaterialTheme.colorScheme.primary else Color(0xFFFB7185),
                                modifier = Modifier.width(46.dp)
                            )
                        }
                    }
                }
            }
        }

        // Playback Controls: First, Prev, Next, Live
        if (moves.isNotEmpty()) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { onSelectMove(0) },
                        enabled = currentIdx != 0,
                        modifier = Modifier.testTag("move_first_btn")
                    ) {
                        Icon(Icons.Rounded.FirstPage, contentDescription = "أول نقلة")
                    }
                    IconButton(
                        onClick = {
                            val idx = (currentIdx ?: moves.lastIndex) - 1
                            if (idx >= 0) onSelectMove(idx)
                        },
                        enabled = (currentIdx ?: 0) > 0,
                        modifier = Modifier.testTag("move_prev_btn")
                    ) {
                        Icon(Icons.Rounded.ChevronLeft, contentDescription = "النقلة السابقة")
                    }
                    IconButton(
                        onClick = {
                            val idx = (currentIdx ?: 0) + 1
                            if (idx < moves.lastIndex) onSelectMove(idx) else onSelectMove(null)
                        },
                        enabled = activeReviewIndex != null,
                        modifier = Modifier.testTag("move_next_btn")
                    ) {
                        Icon(Icons.Rounded.ChevronRight, contentDescription = "النقلة التالية")
                    }
                    IconButton(
                        onClick = { onSelectMove(null) },
                        enabled = activeReviewIndex != null,
                        modifier = Modifier.testTag("move_live_btn")
                    ) {
                        Icon(Icons.Rounded.LastPage, contentDescription = "الوضع الحالي")
                    }
                }
            }
        }
    }
}

@Composable
private fun LedgerMoveCell(
    move: RecordedMove?,
    selected: Boolean,
    showBadge: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (move == null) {
        Spacer(modifier = modifier)
        return
    }
    val qColor = Color(move.quality.colorHex)
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.24f) else Color.Transparent,
        modifier = modifier
            .padding(end = 6.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = move.san,
                fontFamily = MonoFontFamily,
                fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (showBadge) {
                Text(
                    text = move.quality.badge,
                    fontFamily = MonoFontFamily,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = qColor
                )
            }
        }
    }
}

@Composable
fun EvaluationSwingChart(
    moves: List<RecordedMove>,
    activeIndex: Int?,
    modifier: Modifier = Modifier
) {
    val lineColor = MaterialTheme.colorScheme.primary
    val surfaceBg = MaterialTheme.colorScheme.surfaceVariant
    Canvas(
        modifier = modifier
            .height(64.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(surfaceBg)
    ) {
        val w = size.width
        val h = size.height
        val midY = h / 2f

        // Center equality line
        drawLine(
            color = Color(0x5594A3B8),
            start = Offset(0f, midY),
            end = Offset(w, midY),
            strokeWidth = 1.5f
        )

        if (moves.isEmpty()) return@Canvas
        val stepX = if (moves.size == 1) w else w / (moves.size - 1).toFloat()

        val points = moves.mapIndexed { idx, m ->
            val clamped = m.evalWhiteCp.coerceIn(-700, 700)
            val norm = clamped / 700f // +1 = top (white), -1 = bottom (black)
            val y = midY - (norm * (h * 0.42f))
            Offset(idx * stepX, y)
        }

        val areaAbove = Path().apply {
            moveTo(0f, midY)
            points.forEach { lineTo(it.x, it.y) }
            lineTo(points.last().x, midY)
            close()
        }
        drawPath(areaAbove, color = Color(0x44F8FAFC))

        val linePath = Path().apply {
            moveTo(points.first().x, points.first().y)
            for (i in 1 until points.size) lineTo(points[i].x, points[i].y)
        }
        drawPath(linePath, color = lineColor, style = Stroke(width = 3.5f, cap = StrokeCap.Round))

        if (activeIndex != null && activeIndex in points.indices) {
            val pt = points[activeIndex]
            drawCircle(color = lineColor, radius = 6f, center = pt)
            drawCircle(color = Color.White, radius = 2.5f, center = pt)
        }
    }
}
