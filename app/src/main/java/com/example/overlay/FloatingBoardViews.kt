package com.example.overlay

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View
import androidx.compose.ui.graphics.toArgb
import com.example.chess.BoardTheme
import com.example.chess.ChessBoardState
import com.example.chess.ChessMove
import com.example.chess.ChessPiece
import com.example.chess.PieceColor
import com.example.chess.PieceStyle
import com.example.chess.Square

internal class FloatingBoardCanvasView(context: Context) : View(context) {
    private var boardState: ChessBoardState = ChessBoardState.initial()
    private var boardTheme: BoardTheme = BoardTheme.TOURNAMENT_EMERALD
    private var pieceStyle: PieceStyle = PieceStyle.CLASSIC_STAUNTON
    private var flipped: Boolean = false
    private var selected: Square? = null
    private var legalTargets: Set<Square> = emptySet()
    private var lastMove: ChessMove? = null
    private var showLastMove: Boolean = true
    private var showLegalMoves: Boolean = true
    private var useRings: Boolean = true
    private var legalColorInt: Int = Color.parseColor("#7E65B8")
    private var selectedColorInt: Int = Color.parseColor("#88006A87")
    private var cornerRadiusPx: Float = 0f

    var onSquareTapped: ((Square) -> Unit)? = null

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val selStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val clipPath = Path()
    private val clipRect = RectF()

    fun updateState(
        state: ChessBoardState,
        theme: BoardTheme,
        pieceStyle: PieceStyle,
        flipped: Boolean,
        selected: Square?,
        legalTargets: Set<Square>,
        lastMove: ChessMove?,
        showLastMove: Boolean,
        showLegalMoves: Boolean,
        useRings: Boolean,
        legalColorInt: Int,
        selectedColorInt: Int,
        cornerRadiusPx: Float
    ) {
        this.boardState = state
        this.boardTheme = theme
        this.pieceStyle = pieceStyle
        this.flipped = flipped
        this.selected = selected
        this.legalTargets = legalTargets
        this.lastMove = lastMove
        this.showLastMove = showLastMove
        this.showLegalMoves = showLegalMoves
        this.useRings = useRings
        this.legalColorInt = legalColorInt
        this.selectedColorInt = selectedColorInt
        this.cornerRadiusPx = cornerRadiusPx
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val size = width.coerceAtMost(height).toFloat()
        if (size <= 0f) return
        val cell = size / 8f

        canvas.save()
        if (cornerRadiusPx > 0f) {
            clipPath.reset()
            clipRect.set(0f, 0f, size, size)
            clipPath.addRoundRect(clipRect, cornerRadiusPx, cornerRadiusPx, Path.Direction.CW)
            canvas.clipPath(clipPath)
        }

        val lm = lastMove
        val lightColor = boardTheme.lightSquare.toArgb()
        val darkColor = boardTheme.darkSquare.toArgb()
        val lastMoveColor = boardTheme.lastMoveHighlight.toArgb()

        for (vr in 0..7) {
            val r = if (flipped) 7 - vr else vr
            for (vc in 0..7) {
                val c = if (flipped) 7 - vc else vc
                val sq = Square(r, c)
                val isLight = (r + c) % 2 == 0
                val left = vc * cell
                val top = vr * cell

                paint.color = if (isLight) lightColor else darkColor
                canvas.drawRect(left, top, left + cell, top + cell, paint)

                if (selected == sq) {
                    paint.color = selectedColorInt
                    canvas.drawRect(left, top, left + cell, top + cell, paint)
                    selStrokePaint.color = Color.parseColor("#38BDF8")
                    selStrokePaint.strokeWidth = (cell * 0.06f).coerceAtLeast(1.5f)
                    canvas.drawRect(
                        left + selStrokePaint.strokeWidth * 0.5f,
                        top + selStrokePaint.strokeWidth * 0.5f,
                        left + cell - selStrokePaint.strokeWidth * 0.5f,
                        top + cell - selStrokePaint.strokeWidth * 0.5f,
                        selStrokePaint
                    )
                } else if (showLastMove && lm != null && (lm.from == sq || lm.to == sq)) {
                    paint.color = lastMoveColor
                    canvas.drawRect(left, top, left + cell, top + cell, paint)
                }

                if (vc == 0) {
                    paint.color = if (isLight) darkColor else lightColor
                    paint.textSize = cell * 0.21f
                    paint.textAlign = Paint.Align.LEFT
                    canvas.drawText("${8 - r}", left + cell * 0.07f, top + cell * 0.24f, paint)
                }
                if (vr == 7) {
                    paint.color = if (isLight) darkColor else lightColor
                    paint.textSize = cell * 0.21f
                    paint.textAlign = Paint.Align.RIGHT
                    canvas.drawText("${'a' + c}", left + cell * 0.93f, top + cell * 0.93f, paint)
                }

                val piece = boardState.pieceAt(sq)
                if (piece != null) {
                    drawClassicChessPieceCanvas(
                        canvas = canvas,
                        piece = piece,
                        left = left + cell * 0.06f,
                        top = top + cell * 0.05f,
                        size = cell * 0.88f,
                        style = pieceStyle,
                        context = context
                    )
                }

                if (showLegalMoves && sq in legalTargets) {
                    val cx = left + cell * 0.5f
                    val cy = top + cell * 0.5f
                    if (piece != null) {
                        ringPaint.color = legalColorInt
                        ringPaint.strokeWidth = (cell * 0.095f).coerceAtLeast(2f)
                        canvas.drawCircle(cx, cy, cell * 0.41f, ringPaint)
                    } else if (useRings) {
                        ringPaint.color = legalColorInt
                        ringPaint.strokeWidth = (cell * 0.08f).coerceAtLeast(1.8f)
                        canvas.drawCircle(cx, cy, cell * 0.21f, ringPaint)
                    } else {
                        paint.color = legalColorInt
                        canvas.drawCircle(cx, cy, cell * 0.16f, paint)
                    }
                }
            }
        }
        canvas.restore()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP) {
            val cell = width / 8f
            if (cell > 0f) {
                val vc = (event.x / cell).toInt().coerceIn(0, 7)
                val vr = (event.y / cell).toInt().coerceIn(0, 7)
                val c = if (flipped) 7 - vc else vc
                val r = if (flipped) 7 - vr else vr
                onSquareTapped?.invoke(Square(r, c))
                performClick()
            }
        }
        return true
    }

    override fun performClick(): Boolean = super.performClick()
}

internal class FloatingEditorBoardView(context: Context) : View(context) {
    private var board: Array<ChessPiece?> = arrayOfNulls(64)
    private var theme: BoardTheme = BoardTheme.TOURNAMENT_EMERALD
    private var pieceStyle: PieceStyle = PieceStyle.CLASSIC_STAUNTON
    private var cornerRadiusPx: Float = 0f
    var onSquareTapped: ((Square) -> Unit)? = null

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val clipPath = Path()
    private val clipRect = RectF()

    fun updateBoard(
        arr: Array<ChessPiece?>,
        boardTheme: BoardTheme,
        pieceStyle: PieceStyle = PieceStyle.CLASSIC_STAUNTON,
        cornerRadiusPx: Float = 0f
    ) {
        this.board = arr
        this.theme = boardTheme
        this.pieceStyle = pieceStyle
        this.cornerRadiusPx = cornerRadiusPx
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val size = width.coerceAtMost(height).toFloat()
        if (size <= 0f) return
        val cell = size / 8f
        val lightColor = theme.lightSquare.toArgb()
        val darkColor = theme.darkSquare.toArgb()

        canvas.save()
        if (cornerRadiusPx > 0f) {
            clipPath.reset()
            clipRect.set(0f, 0f, size, size)
            clipPath.addRoundRect(clipRect, cornerRadiusPx, cornerRadiusPx, Path.Direction.CW)
            canvas.clipPath(clipPath)
        }

        for (r in 0..7) {
            for (c in 0..7) {
                val left = c * cell
                val top = r * cell
                paint.color = if ((r + c) % 2 == 0) lightColor else darkColor
                canvas.drawRect(left, top, left + cell, top + cell, paint)
                val piece = board[r * 8 + c]
                if (piece != null) {
                    drawClassicChessPieceCanvas(
                        canvas = canvas,
                        piece = piece,
                        left = left + cell * 0.06f,
                        top = top + cell * 0.05f,
                        size = cell * 0.88f,
                        style = pieceStyle,
                        context = context
                    )
                }
            }
        }
        canvas.restore()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP) {
            val cell = width / 8f
            if (cell > 0f) {
                val c = (event.x / cell).toInt().coerceIn(0, 7)
                val r = (event.y / cell).toInt().coerceIn(0, 7)
                onSquareTapped?.invoke(Square(r, c))
                performClick()
            }
        }
        return true
    }

    override fun performClick(): Boolean = super.performClick()
}
