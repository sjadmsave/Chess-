package com.example.ui.components

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chess.BoardTheme
import com.example.chess.ChessBoardState
import com.example.chess.ChessMove
import com.example.chess.ChessPiece
import com.example.chess.PieceColor
import com.example.chess.PieceStyle
import com.example.chess.PieceType
import com.example.chess.Square
import com.example.ui.theme.MonoFontFamily
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ChessBoardView(
    boardState: ChessBoardState,
    boardTheme: BoardTheme,
    pieceStyle: PieceStyle = PieceStyle.CLASSIC_STAUNTON,
    selectedSquare: Square?,
    legalMovesForSelected: List<ChessMove>,
    lastMove: ChessMove?,
    hintMove: ChessMove?,
    flipped: Boolean,
    interactive: Boolean,
    onSquareClick: (Square) -> Unit,
    modifier: Modifier = Modifier
) {
    val kingInCheckSquare = remember(boardState) {
        if (boardState.isInCheck(boardState.turn)) boardState.findKing(boardState.turn) else null
    }
    val legalTargets = remember(legalMovesForSelected) {
        legalMovesForSelected.map { it.to }.toSet()
    }

    // Chessboard coordinates must always follow standard LTR algebraic orientation
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Box(
            modifier = modifier
                .aspectRatio(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(boardTheme.boardBorder)
                .border(3.dp, boardTheme.boardBorder, RoundedCornerShape(16.dp))
                .padding(4.dp)
                .testTag("chess_board")
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp))
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
                                val isLastMove = lastMove != null && (lastMove.from == sq || lastMove.to == sq)
                                val isLegalTarget = sq in legalTargets
                                val isCheckSquare = kingInCheckSquare == sq

                                val baseColor = if (isLight) boardTheme.lightSquare else boardTheme.darkSquare
                                val coordColor = if (isLight) boardTheme.darkSquare else boardTheme.lightSquare

                                val overlayColor by animateColorAsState(
                                    targetValue = when {
                                        isSelected -> boardTheme.selectedHighlight
                                        isLastMove -> boardTheme.lastMoveHighlight
                                        else -> Color.Transparent
                                    },
                                    animationSpec = tween(160),
                                    label = "squareHighlight"
                                )

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxSize()
                                        .background(baseColor)
                                        .background(overlayColor)
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
                                    if (visualCol == 0) {
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
                                    if (visualRow == 7) {
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
                                        )
                                    }

                                    // Legal move indicator
                                    if (isLegalTarget) {
                                        if (piece != null) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .padding(3.dp)
                                                    .border(
                                                        width = 3.dp,
                                                        color = Color(0xBB10B981),
                                                        shape = CircleShape
                                                    )
                                            )
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .size(squareSize * 0.30f)
                                                    .clip(CircleShape)
                                                    .background(Color(0x9910B981))
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Coach Hint Arrow overlay
                if (hintMove != null) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val cellW = size.width / 8f
                        val cellH = size.height / 8f
                        fun centerOf(sq: Square): Offset {
                            val vc = if (flipped) 7 - sq.col else sq.col
                            val vr = if (flipped) 7 - sq.row else sq.row
                            return Offset((vc + 0.5f) * cellW, (vr + 0.5f) * cellH)
                        }
                        val start = centerOf(hintMove.from)
                        val end = centerOf(hintMove.to)
                        val angle = atan2((end.y - start.y).toDouble(), (end.x - start.x).toDouble())
                        val arrowEnd = Offset(
                            end.x - (cellW * 0.16f) * cos(angle).toFloat(),
                            end.y - (cellH * 0.16f) * sin(angle).toFloat()
                        )
                        val hintColor = Color(0xDD22D3EE)

                        drawLine(
                            color = hintColor,
                            start = start,
                            end = arrowEnd,
                            strokeWidth = cellW * 0.16f,
                            cap = StrokeCap.Round
                        )
                        val headLen = cellW * 0.34f
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
                        drawPath(headPath, hintColor)
                        drawCircle(
                            color = hintColor,
                            radius = cellW * 0.14f,
                            center = start,
                            style = Stroke(width = cellW * 0.06f)
                        )
                    }
                }
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
