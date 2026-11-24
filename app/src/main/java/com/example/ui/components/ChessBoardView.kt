package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chess.BoardMaterial
import com.example.chess.BoardTheme
import com.example.chess.ChessBoardState
import com.example.chess.ChessMove
import com.example.chess.ChessPiece
import com.example.chess.LastMoveHighlightStyle
import com.example.chess.MoveQuality
import com.example.chess.MultiPvLine
import com.example.chess.PieceAnimationStyle
import com.example.chess.PieceColor
import com.example.chess.PieceStyle
import com.example.chess.PieceType
import com.example.chess.Square
import com.example.chess.ThreatArrow
import com.example.ui.theme.MonoFontFamily
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ChessBoardView(
    boardState: ChessBoardState,
    boardTheme: BoardTheme,
    pieceStyle: PieceStyle = PieceStyle.CLASSIC_STAUNTON,
    boardMaterial: BoardMaterial = BoardMaterial.FLAT,
    selectedSquare: Square?,
    legalMovesForSelected: List<ChessMove>,
    lastMove: ChessMove?,
    hintMove: ChessMove?,
    flipped: Boolean,
    interactive: Boolean,
    showLastMove: Boolean = true,
    showLegalMoves: Boolean = true,
    useRingsInsteadOfDots: Boolean = true,
    selectedHighlightColor: Color? = null,
    lastMoveHighlightColor: Color? = null,
    legalMoveIndicatorColor: Color? = null,
    lastMoveHighlightStyle: LastMoveHighlightStyle = LastMoveHighlightStyle.FILL,
    colorLastMoveByQuality: Boolean = false,
    lastMoveQuality: MoveQuality? = null,
    showCoordinates: Boolean = true,
    premoveSquares: Pair<Square, Square>? = null,
    analysisLines: List<MultiPvLine> = emptyList(),
    showEngineArrowsOnBoard: Boolean = true,
    showMultiPvArrowNumbers: Boolean = true,
    arrowStrengthColors: Boolean = true,
    previousBestMoveArrow: ChessMove? = null,
    threats: List<ThreatArrow> = emptyList(),
    pieceAnimationStyle: PieceAnimationStyle = PieceAnimationStyle.DEFAULT,
    enlargeOnDrag: Boolean = true,
    boardScalePercent: Int = 100,
    onSquareClick: (Square) -> Unit,
    modifier: Modifier = Modifier
) {
    val kingInCheckSquare = remember(boardState) {
        if (boardState.isInCheck(boardState.turn)) boardState.findKing(boardState.turn) else null
    }
    val legalTargets = remember(legalMovesForSelected) {
        legalMovesForSelected.map { it.to }.toSet()
    }

    val effectiveSelectedColor = selectedHighlightColor ?: boardTheme.selectedHighlight
    val baseLastMoveColor = if (colorLastMoveByQuality && lastMoveQuality != null) {
        Color(lastMoveQuality.colorHex).copy(alpha = 0.42f)
    } else {
        lastMoveHighlightColor ?: boardTheme.lastMoveHighlight
    }
    val effectiveLegalColor = legalMoveIndicatorColor ?: Color(0xCC0D9488)
    val textMeasurer = rememberTextMeasurer()

    val pulseTransition = rememberInfiniteTransition(label = "piecePulse")
    val pulseScale by pulseTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.09f,
        animationSpec = infiniteRepeatable(
            animation = tween(650),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseVal"
    )
    val scaleFraction = (boardScalePercent.coerceIn(85, 100) / 100f)

    // Chessboard coordinates must always follow standard LTR algebraic orientation
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Box(
            modifier = modifier
                .fillMaxWidth(scaleFraction)
                .aspectRatio(1f)
                .clip(RoundedCornerShape(14.dp))
                .background(boardTheme.boardBorder)
                .border(2.5.dp, boardTheme.boardBorder, RoundedCornerShape(14.dp))
                .padding(3.dp)
                .testTag("chess_board")
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(11.dp))
            ) {
                val squareSize = maxWidth / 8f

                Column(modifier = Modifier.fillMaxSize()) {
                    for (visualRow in 0..7) {
                        val row = if (flipped) 7 - visualRow else visualRow
                        Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            for (visualCol in 0..7) {
                                val col = if (flipped) 7 - visualCol else visualCol
                                val sq = Square(row, col)
                                val isLight = (row + col) % 2 == 0
                                val piece = boardState.pieceAt(sq)
                                val isSelected = selectedSquare == sq
                                val isLastMove = showLastMove &&
                                    lastMoveHighlightStyle != LastMoveHighlightStyle.NONE &&
                                    lastMove != null &&
                                    (lastMove.from == sq || lastMove.to == sq)
                                val isLastMoveDest = showLastMove && lastMove != null && lastMove.to == sq
                                val isPremoveSq = premoveSquares != null &&
                                    (premoveSquares.first == sq || premoveSquares.second == sq)
                                val isLegalTarget = showLegalMoves && (sq in legalTargets)
                                val isCheckSquare = kingInCheckSquare == sq

                                val baseColor = if (isLight) boardTheme.lightSquare else boardTheme.darkSquare
                                val coordColor = if (isLight) boardTheme.darkSquare else boardTheme.lightSquare

                                val overlayColor by animateColorAsState(
                                    targetValue = when {
                                        isSelected -> effectiveSelectedColor
                                        isPremoveSq -> Color(0x88EC4899)
                                        isLastMove && lastMoveHighlightStyle == LastMoveHighlightStyle.FILL -> baseLastMoveColor
                                        else -> Color.Transparent
                                    },
                                    animationSpec = tween(150),
                                    label = "squareHighlight"
                                )

                                val pieceScale by animateFloatAsState(
                                    targetValue = when {
                                        isSelected && pieceAnimationStyle == PieceAnimationStyle.PULSE -> pulseScale
                                        isSelected && enlargeOnDrag -> 1.12f
                                        else -> 1f
                                    },
                                    animationSpec = tween(140),
                                    label = "pieceScale"
                                )
                                val pieceAlpha by animateFloatAsState(
                                    targetValue = if (isSelected && pieceAnimationStyle == PieceAnimationStyle.FADE) 0.78f else 1f,
                                    animationSpec = tween(140),
                                    label = "pieceAlpha"
                                )

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxSize()
                                        .background(baseColor)
                                        .background(overlayColor)
                                        .then(
                                            if (isLastMove && lastMoveHighlightStyle == LastMoveHighlightStyle.BORDER && !isSelected) {
                                                Modifier.border(
                                                    width = 2.5.dp,
                                                    color = baseLastMoveColor.copy(alpha = 0.92f)
                                                )
                                            } else Modifier
                                        )
                                        .then(
                                            if (isSelected && pieceAnimationStyle == PieceAnimationStyle.TRAIL) {
                                                Modifier.border(2.dp, Color(0xCC38BDF8))
                                            } else Modifier
                                        )
                                        .clickable(
                                            enabled = interactive,
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null
                                        ) {
                                            onSquareClick(sq)
                                        }
                                        .testTag("sq_${sq.toAlgebraic()}"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    // Subtle board surface texture (Wood / Ivory / Marble)
                                    if (boardMaterial != BoardMaterial.FLAT) {
                                        BoardTextureOverlay(
                                            material = boardMaterial,
                                            isLight = isLight,
                                            seed = row * 8 + col
                                        )
                                    }

                                    // Red radial glow when king is in check
                                    if (isCheckSquare) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(
                                                    Brush.radialGradient(
                                                        colors = listOf(
                                                            Color(0xCCEF4444),
                                                            Color(0x44EF4444),
                                                            Color.Transparent
                                                        )
                                                    )
                                                )
                                        )
                                    }

                                    // Rank coordinate on leftmost visual column
                                    if (showCoordinates && visualCol == 0) {
                                        Text(
                                            text = "${8 - row}",
                                            fontFamily = MonoFontFamily,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = coordColor.copy(alpha = 0.85f),
                                            modifier = Modifier
                                                .align(Alignment.TopStart)
                                                .padding(start = 2.dp, top = 1.dp)
                                        )
                                    }
                                    // File coordinate on bottommost visual row
                                    if (showCoordinates && visualRow == 7) {
                                        Text(
                                            text = "${'a' + col}",
                                            fontFamily = MonoFontFamily,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = coordColor.copy(alpha = 0.85f),
                                            modifier = Modifier
                                                .align(Alignment.BottomEnd)
                                                .padding(end = 2.dp, bottom = 1.dp)
                                        )
                                    }

                                    // Piece graphic
                                    if (piece != null) {
                                        ChessPieceGraphic(
                                            piece = piece,
                                            pieceStyle = pieceStyle,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(3.dp)
                                                .scale(pieceScale)
                                                .alpha(pieceAlpha)
                                        )
                                    }

                                    // Legal move indicator (Rings or Dots)
                                    if (isLegalTarget) {
                                        if (piece != null) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .padding(3.dp)
                                                    .border(
                                                        width = 3.dp,
                                                        color = effectiveLegalColor,
                                                        shape = CircleShape
                                                    )
                                            )
                                        } else if (useRingsInsteadOfDots) {
                                            Box(
                                                modifier = Modifier
                                                    .size(squareSize * 0.44f)
                                                    .border(
                                                        width = 2.5.dp,
                                                        color = effectiveLegalColor,
                                                        shape = CircleShape
                                                    )
                                            )
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .size(squareSize * 0.30f)
                                                    .clip(CircleShape)
                                                    .background(effectiveLegalColor)
                                            )
                                        }
                                    }

                                    // Move quality badge on target square (like ★ / !! / ??)
                                    if (isLastMoveDest && lastMoveQuality != null && colorLastMoveByQuality) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(1.5.dp)
                                                .size((squareSize * 0.34f).coerceAtLeast(13.dp))
                                                .clip(CircleShape)
                                                .background(Color(lastMoveQuality.colorHex))
                                                .border(1.dp, Color.White, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = lastMoveQuality.badge,
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Black,
                                                color = Color.White,
                                                lineHeight = 9.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Board Arrow Layers: Previous Best Ghost, Threats, MultiPV Engine Lines, and Hint
                val hasArrows = (showEngineArrowsOnBoard && analysisLines.isNotEmpty()) ||
                    previousBestMoveArrow != null ||
                    threats.isNotEmpty() ||
                    hintMove != null

                if (hasArrows) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val cellW = size.width / 8f
                        val cellH = size.height / 8f
                        fun centerOf(sq: Square): Offset {
                            val vc = if (flipped) 7 - sq.col else sq.col
                            val vr = if (flipped) 7 - sq.row else sq.row
                            return Offset((vc + 0.5f) * cellW, (vr + 0.5f) * cellH)
                        }

                        // 1. Previous best move ghost arrow
                        if (previousBestMoveArrow != null) {
                            drawChessArrow(
                                start = centerOf(previousBestMoveArrow.from),
                                end = centerOf(previousBestMoveArrow.to),
                                cellW = cellW,
                                cellH = cellH,
                                color = Color(0x8894A3B8),
                                strokeFactor = 0.12f,
                                dashed = true
                            )
                        }

                        // 2. Threat arrows (red/orange danger indicators)
                        threats.take(4).forEach { threat ->
                            drawChessArrow(
                                start = centerOf(threat.from),
                                end = centerOf(threat.to),
                                cellW = cellW,
                                cellH = cellH,
                                color = Color(0xCCEF4444),
                                strokeFactor = 0.11f
                            )
                        }

                        // 3. MultiPV Engine arrows (Rank 1..5, drawn weakest-first so rank 1 sits on top)
                        if (showEngineArrowsOnBoard && analysisLines.isNotEmpty()) {
                            val bestCp = analysisLines.first().evalWhiteCp
                            val ordered = analysisLines.sortedByDescending { it.rank }
                            ordered.forEach { line ->
                                val arrowColor = if (arrowStrengthColors) {
                                    val delta = abs(bestCp - line.evalWhiteCp)
                                    when {
                                        line.rank == 1 -> Color(0xDD22C55E) // Best emerald green
                                        delta <= 35 -> Color(0xCC38BDF8)   // Strong sky blue
                                        delta <= 95 -> Color(0xBBF59E0B)   // Decent amber
                                        else -> Color(0x9994A3B8)          // Weaker muted slate
                                    }
                                } else {
                                    when (line.rank) {
                                        1 -> Color(0xDD22C55E)
                                        2 -> Color(0xBB38BDF8)
                                        else -> Color(0x8864748B)
                                    }
                                }
                                val strokeScale = when (line.rank) {
                                    1 -> 0.16f
                                    2 -> 0.125f
                                    else -> 0.095f
                                }
                                drawChessArrow(
                                    start = centerOf(line.move.from),
                                    end = centerOf(line.move.to),
                                    cellW = cellW,
                                    cellH = cellH,
                                    color = arrowColor,
                                    strokeFactor = strokeScale,
                                    rankNumber = if (showMultiPvArrowNumbers) line.rank else null,
                                    textMeasurer = textMeasurer
                                )
                            }
                        }

                        // 4. Coach Hint Arrow (highest priority cyan highlight)
                        if (hintMove != null) {
                            drawChessArrow(
                                start = centerOf(hintMove.from),
                                end = centerOf(hintMove.to),
                                cellW = cellW,
                                cellH = cellH,
                                color = Color(0xEE22D3EE),
                                strokeFactor = 0.16f,
                                drawOriginRing = true
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawChessArrow(
    start: Offset,
    end: Offset,
    cellW: Float,
    cellH: Float,
    color: Color,
    strokeFactor: Float = 0.15f,
    dashed: Boolean = false,
    drawOriginRing: Boolean = false,
    rankNumber: Int? = null,
    textMeasurer: TextMeasurer? = null
) {
    val angle = atan2((end.y - start.y).toDouble(), (end.x - start.x).toDouble())
    val arrowEnd = Offset(
        end.x - (cellW * 0.17f) * cos(angle).toFloat(),
        end.y - (cellH * 0.17f) * sin(angle).toFloat()
    )
    drawLine(
        color = color,
        start = start,
        end = arrowEnd,
        strokeWidth = cellW * strokeFactor,
        cap = StrokeCap.Round,
        pathEffect = if (dashed) PathEffect.dashPathEffect(floatArrayOf(cellW * 0.16f, cellW * 0.12f), 0f) else null
    )
    val headLen = cellW * (strokeFactor * 2.15f).coerceAtLeast(0.26f)
    val headAngle = Math.toRadians(28.0)
    val p1 = Offset(
        end.x - headLen * cos(angle - headAngle).toFloat(),
        end.y - headLen * sin(angle - headAngle).toFloat()
    )
    val p2 = Offset(
        end.x - headLen * cos(angle + headAngle).toFloat(),
        end.y - headLen * sin(angle + headAngle).toFloat()
    )
    val headPath = Path().apply {
        moveTo(end.x, end.y)
        lineTo(p1.x, p1.y)
        lineTo(p2.x, p2.y)
        close()
    }
    drawPath(headPath, color)
    if (drawOriginRing) {
        drawCircle(
            color = color,
            radius = cellW * 0.14f,
            center = start,
            style = Stroke(width = cellW * 0.06f)
        )
    }
    if (rankNumber != null && textMeasurer != null) {
        val badgeCenter = Offset(
            end.x - (cellW * 0.24f) * cos(angle).toFloat(),
            end.y - (cellH * 0.24f) * sin(angle).toFloat()
        )
        val badgeR = cellW * 0.15f
        drawCircle(color = Color(0xEE0F172A), radius = badgeR, center = badgeCenter)
        drawCircle(color = color.copy(alpha = 1f), radius = badgeR, center = badgeCenter, style = Stroke(width = 1.6f))
        val layout = textMeasurer.measure(
            text = "$rankNumber",
            style = TextStyle(
                color = Color.White,
                fontSize = (cellW * 0.20f / density).sp,
                fontWeight = FontWeight.Bold
            )
        )
        drawText(
            textLayoutResult = layout,
            topLeft = Offset(
                badgeCenter.x - layout.size.width / 2f,
                badgeCenter.y - layout.size.height / 2f
            )
        )
    }
}

@Composable
private fun BoardTextureOverlay(
    material: BoardMaterial,
    isLight: Boolean,
    seed: Int
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        when (material) {
            BoardMaterial.FLAT -> Unit
            BoardMaterial.WOOD -> {
                val grainColor = if (isLight) Color(0x148B5A2B) else Color(0x1C3E220C)
                for (i in 1..3) {
                    val y = h * (i * 0.25f) + ((seed % 3) - 1) * 2f
                    drawLine(grainColor, Offset(0f, y), Offset(w, y + 2f), strokeWidth = 1.5f)
                }
            }
            BoardMaterial.IVORY -> {
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0x18FFFFFF), Color.Transparent, Color(0x12000000))
                    )
                )
            }
            BoardMaterial.MARBLE -> {
                val veinColor = if (isLight) Color(0x18475569) else Color(0x1FFFFFFF)
                drawLine(veinColor, Offset(0f, h * 0.2f), Offset(w * 0.85f, h), strokeWidth = 1.6f)
                drawLine(veinColor, Offset(w * 0.3f, 0f), Offset(w, h * 0.6f), strokeWidth = 1.2f)
            }
        }
    }
}

@Composable
fun PromotionPickerDialog(
    color: PieceColor,
    pieceStyle: PieceStyle = PieceStyle.CLASSIC_STAUNTON,
    onSelect: (PieceType) -> Unit,
    onDismiss: () -> Unit
) {
    val options = listOf(PieceType.QUEEN, PieceType.ROOK, PieceType.BISHOP, PieceType.KNIGHT)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "اختر قطعة الترقية",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                options.forEach { type ->
                    Card(
                        onClick = { onSelect(type) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("promo_${type.fenChar}"),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            ChessPieceGraphic(
                                piece = ChessPiece(type, color),
                                pieceSize = 40.dp,
                                pieceStyle = pieceStyle
                            )
                            Text(
                                text = type.nameAr,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {}
    )
}

@Composable
fun MiniBoardThumbnail(
    boardState: ChessBoardState,
    boardTheme: BoardTheme,
    pieceStyle: PieceStyle,
    size: Dp = 80.dp,
    modifier: Modifier = Modifier
) {
    val pieceSize = size / 9.2f
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Column(
            modifier = modifier
                .size(size)
                .clip(RoundedCornerShape(10.dp))
                .border(1.5.dp, boardTheme.boardBorder, RoundedCornerShape(10.dp))
        ) {
            for (r in 0..7) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    for (c in 0..7) {
                        val isLight = (r + c) % 2 == 0
                        val piece = boardState.pieceAt(Square(r, c))
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                                .background(if (isLight) boardTheme.lightSquare else boardTheme.darkSquare),
                            contentAlignment = Alignment.Center
                        ) {
                            if (piece != null) {
                                ChessPieceGraphic(
                                    piece = piece,
                                    pieceSize = pieceSize,
                                    pieceStyle = pieceStyle
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

