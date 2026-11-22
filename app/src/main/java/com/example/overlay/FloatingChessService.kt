package com.example.overlay

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import androidx.compose.ui.graphics.toArgb
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.audio.ChessSound
import com.example.audio.SoundManager
import com.example.chess.BoardTheme
import com.example.chess.ChessBoardState
import com.example.chess.ChessEngine
import com.example.chess.ChessMove
import com.example.chess.ChessPiece
import com.example.chess.EnginePersonality
import com.example.chess.GameResult
import com.example.chess.PieceColor
import com.example.chess.PieceType
import com.example.chess.Square
import com.example.chess.StockfishUciEngine
import com.example.data.UserPreferences
import com.example.data.UserPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs

class FloatingChessService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var prefsRepo: UserPreferencesRepository
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val soundManager = SoundManager()

    private var rootView: View? = null
    private var layoutParams: WindowManager.LayoutParams? = null
    private var prefs: UserPreferences = UserPreferences()

    private enum class OverlayMode {
        BOARD, MENU, WIZARD_STEP1, WIZARD_STEP2, BUBBLE
    }

    private var mode = OverlayMode.BOARD
    private var boardState = ChessBoardState.initial()
    private val moveStack = mutableListOf<Pair<ChessBoardState, ChessMove>>()
    private var playerColor = PieceColor.WHITE
    private var flipped = false
    private var selectedSquare: Square? = null
    private var legalForSelected: List<ChessMove> = emptyList()
    private var lastMove: ChessMove? = null
    private var isThinking = false

    // In-overlay Wizard config
    private var wizardColor = PieceColor.WHITE
    private var wizardElo = 1200
    private var wizardPersonality = EnginePersonality.OPPORTUNITY_HUNTER

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        prefsRepo = UserPreferencesRepository(applicationContext)
        startForegroundNotification()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        if (!canDrawOverlay(this)) {
            stopSelf()
            return START_NOT_STICKY
        }

        val fenArg = intent?.getStringExtra(EXTRA_FEN)
        val colorArg = intent?.getStringExtra(EXTRA_PLAYER_COLOR)

        serviceScope.launch {
            prefs = prefsRepo.preferencesFlow.first()
            soundManager.enabled = prefs.soundEnabled
            wizardElo = prefs.customEngineElo
            wizardPersonality = prefs.enginePersonality

            if (!fenArg.isNullOrBlank()) {
                boardState = ChessBoardState.fromFen(fenArg)
                moveStack.clear()
                lastMove = null
                selectedSquare = null
                legalForSelected = emptyList()
            }
            if (!colorArg.isNullOrBlank()) {
                playerColor = runCatching { PieceColor.valueOf(colorArg) }.getOrDefault(PieceColor.WHITE)
                flipped = playerColor == PieceColor.BLACK
            }

            if (rootView == null) {
                attachOverlayWindow()
            } else {
                rebuildContent()
            }

            if (boardState.turn != playerColor && boardState.evaluateGameResult() == GameResult.ONGOING) {
                triggerEngineReply()
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        StockfishUciEngine.closeSession()
        removeOverlayWindow()
        serviceScope.cancel()
    }

    private fun startForegroundNotification() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "النافذة العائمة للشطرنج",
                NotificationManager.IMPORTANCE_LOW
            )
            nm.createNotificationChannel(channel)
        }
        val openAppIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, FloatingChessService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("أرينا الشطرنج • نافذة عائمة نشطة")
            .setContentText("اضغط للعودة إلى التطبيق أو لإغلاق النافذة العائمة")
            .setContentIntent(openAppIntent)
            .addAction(0, "إغلاق النافذة", stopIntent)
            .setOngoing(true)
            .build()
        startForeground(NOTIF_ID, notification)
    }

    private fun dp(value: Int): Int = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        value.toFloat(),
        resources.displayMetrics
    ).toInt()

    private fun attachOverlayWindow() {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = dp(24)
            y = dp(120)
        }

        val container = FrameLayout(this)
        rootView = container
        layoutParams = params
        rebuildContent()
        windowManager.addView(container, params)
    }

    private fun removeOverlayWindow() {
        rootView?.let {
            runCatching { windowManager.removeView(it) }
        }
        rootView = null
    }

    private fun rebuildContent() {
        val container = rootView as? FrameLayout ?: return
        container.removeAllViews()

        if (mode == OverlayMode.BUBBLE) {
            val bubblePx = dp(prefs.floatingBubbleSize.sizeDp)
            val bubble = FrameLayout(this).apply {
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(Color.parseColor("#0F6C7A"))
                    setStroke(dp(2), Color.WHITE)
                }
                elevation = dp(8).toFloat()
            }
            val iconText = TextView(this).apply {
                text = "♞"
                setTextColor(Color.WHITE)
                textSize = (prefs.floatingBubbleSize.sizeDp * 0.44f)
                gravity = Gravity.CENTER
            }
            bubble.addView(
                iconText,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
            )
            attachDragAndClick(bubble, onClick = {
                mode = OverlayMode.BOARD
                rebuildContent()
            })
            container.addView(bubble, FrameLayout.LayoutParams(bubblePx, bubblePx))
            return
        }

        val widthPx = dp(prefs.floatingWidthDp)
        val alphaInt = ((prefs.floatingOpacityPercent.coerceIn(35, 100) / 100f) * 255).toInt()
        val cardBg = Color.argb(alphaInt, 244, 246, 249)
        val cornerPx = dp(prefs.floatingFrameShape.cornerDp).toFloat()

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(cardBg)
                cornerRadius = cornerPx
                setStroke(dp(1), Color.parseColor("#CBD5E1"))
            }
            setPadding(dp(6), dp(6), dp(6), dp(6))
            elevation = dp(10).toFloat()
        }

        when (mode) {
            OverlayMode.BOARD -> buildBoardOverlayUi(card, widthPx)
            OverlayMode.MENU -> buildMenuOverlayUi(card, widthPx)
            OverlayMode.WIZARD_STEP1 -> buildWizardStep1Ui(card, widthPx)
            OverlayMode.WIZARD_STEP2 -> buildWizardStep2Ui(card, widthPx)
            OverlayMode.BUBBLE -> Unit
        }

        container.addView(
            card,
            FrameLayout.LayoutParams(widthPx, FrameLayout.LayoutParams.WRAP_CONTENT)
        )
    }

    private fun buildTopBar(card: LinearLayout) {
        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, dp(5))
        }

        fun iconBtn(symbol: String, enabled: Boolean = true, onClick: () -> Unit): TextView {
            return TextView(this).apply {
                text = symbol
                textSize = 14f
                gravity = Gravity.CENTER
                setTextColor(if (enabled) Color.parseColor("#0F172A") else Color.parseColor("#94A3B8"))
                background = GradientDrawable().apply {
                    setColor(Color.parseColor("#FFFFFF"))
                    cornerRadius = dp(10).toFloat()
                    setStroke(dp(1), Color.parseColor("#CBD5E1"))
                }
                setPadding(dp(8), dp(4), dp(8), dp(4))
                if (enabled) setOnClickListener { onClick() }
            }
        }

        val menuBtn = iconBtn("•••") {
            mode = OverlayMode.MENU
            rebuildContent()
        }
        val editBtn = iconBtn("✎") {
            wizardColor = playerColor
            mode = OverlayMode.WIZARD_STEP1
            rebuildContent()
        }
        val undoBtn = iconBtn("↩", enabled = moveStack.isNotEmpty() && !isThinking) {
            undoOverlayMoves()
        }
        val flipBtn = iconBtn("⇅") {
            flipped = !flipped
            rebuildContent()
        }
        val dragHandle = TextView(this).apply {
            text = "⋮⋮"
            textSize = 14f
            gravity = Gravity.CENTER
            setTextColor(Color.parseColor("#0F6C7A"))
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#FFFFFF"))
                cornerRadius = dp(10).toFloat()
                setStroke(dp(1), Color.parseColor("#CBD5E1"))
            }
            setPadding(dp(8), dp(4), dp(8), dp(4))
        }
        attachDragAndClick(dragHandle, onClick = null)

        val buttons = listOf(menuBtn, editBtn, undoBtn, flipBtn, dragHandle)
        buttons.forEachIndexed { idx, v ->
            val lp = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                if (idx < buttons.lastIndex) marginEnd = dp(4)
            }
            bar.addView(v, lp)
        }
        card.addView(bar)
    }

    private fun buildBoardOverlayUi(card: LinearLayout, cardWidthPx: Int) {
        buildTopBar(card)
        val boardPx = cardWidthPx - dp(12)
        val boardView = FloatingBoardCanvasView(this).apply {
            updateState(
                state = boardState,
                theme = prefs.boardTheme,
                flipped = flipped,
                selected = selectedSquare,
                legalTargets = legalForSelected.map { it.to }.toSet(),
                lastMove = lastMove,
                useRings = prefs.useRingsInsteadOfDots,
                legalColorInt = prefs.legalMoveColorHex.toInt(),
                selectedColorInt = prefs.selectedColorHex.toInt()
            )
            onSquareTapped = { sq ->
                handleOverlaySquareTap(sq)
            }
        }
        card.addView(boardView, LinearLayout.LayoutParams(boardPx, boardPx))
    }

    private fun buildMenuOverlayUi(card: LinearLayout, cardWidthPx: Int) {
        buildTopBar(card)

        val statusRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8), dp(4), dp(8), dp(6))
        }
        val statusText = TextView(this).apply {
            val res = boardState.evaluateGameResult()
            text = when {
                res != GameResult.ONGOING -> res.labelAr
                isThinking -> "المحرك يفكر..."
                boardState.turn == playerColor -> "دورك"
                else -> "دور المحرك"
            }
            textSize = 13f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#0F172A"))
            gravity = Gravity.END
        }
        statusRow.addView(statusText, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        card.addView(statusRow)

        val scroll = ScrollView(this)
        val list = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        fun menuItem(title: String, icon: String, isDanger: Boolean = false, enabled: Boolean = true, onClick: () -> Unit) {
            val btn = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                background = GradientDrawable().apply {
                    setColor(Color.WHITE)
                    cornerRadius = dp(14).toFloat()
                    setStroke(dp(1), Color.parseColor("#E2E8F0"))
                }
                setPadding(dp(10), dp(10), dp(10), dp(10))
                if (enabled) setOnClickListener { onClick() }
            }
            val iconTv = TextView(this).apply {
                text = icon
                textSize = 14f
                setTextColor(if (isDanger) Color.parseColor("#B91C1C") else Color.parseColor("#0F6C7A"))
                setPadding(0, 0, dp(8), 0)
            }
            val titleTv = TextView(this).apply {
                text = title
                textSize = 13f
                setTypeface(null, Typeface.BOLD)
                setTextColor(
                    when {
                        !enabled -> Color.parseColor("#94A3B8")
                        isDanger -> Color.parseColor("#B91C1C")
                        else -> Color.parseColor("#0F172A")
                    }
                )
                gravity = Gravity.END
            }
            btn.addView(iconTv)
            btn.addView(titleTv, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = dp(6)
            }
            list.addView(btn, lp)
        }

        menuItem("تراجع عن النقلة", "↩", enabled = moveStack.isNotEmpty() && !isThinking) {
            undoOverlayMoves()
            mode = OverlayMode.BOARD
            rebuildContent()
        }
        menuItem("تعديل الوضعية / مباراة جديدة", "✎") {
            mode = OverlayMode.WIZARD_STEP1
            rebuildContent()
        }
        menuItem("قوة المحرك ($wizardElo)", "⚙") {
            mode = OverlayMode.WIZARD_STEP2
            rebuildContent()
        }
        menuItem("تصغير إلى فقاعة", "—") {
            mode = OverlayMode.BUBBLE
            rebuildContent()
        }
        menuItem("فتح أرينا الشطرنج", "↗") {
            val openIntent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(EXTRA_FEN, boardState.toFen())
                putExtra(EXTRA_PLAYER_COLOR, playerColor.name)
            }
            startActivity(openIntent)
            stopSelf()
        }
        menuItem("إغلاق النافذة", "✕", isDanger = true) {
            stopSelf()
        }

        scroll.addView(list)
        card.addView(scroll, LinearLayout.LayoutParams(cardWidthPx - dp(12), dp(220)))
    }

    private fun buildWizardHeader(card: LinearLayout) {
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(4), dp(2), dp(4), dp(8))
        }
        val closeBtn = TextView(this).apply {
            text = "✕"
            textSize = 14f
            gravity = Gravity.CENTER
            setTextColor(Color.parseColor("#0F172A"))
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.WHITE)
            }
            setPadding(dp(8), dp(4), dp(8), dp(4))
            setOnClickListener {
                mode = OverlayMode.BOARD
                rebuildContent()
            }
        }
        val title = TextView(this).apply {
            text = "مباراة عائمة"
            textSize = 13f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#0F172A"))
            gravity = Gravity.CENTER
        }
        val drag = TextView(this).apply {
            text = "⋮⋮"
            textSize = 14f
            gravity = Gravity.CENTER
            setTextColor(Color.parseColor("#0F6C7A"))
            setPadding(dp(6), dp(4), dp(6), dp(4))
        }
        attachDragAndClick(drag, onClick = null)

        header.addView(closeBtn)
        header.addView(title, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        header.addView(drag)
        card.addView(header)
    }

    private fun buildWizardStep1Ui(card: LinearLayout, cardWidthPx: Int) {
        buildWizardHeader(card)
        val stepTv = TextView(this).apply {
            text = "الخطوة 1 من 2"
            textSize = 11f
            setTextColor(Color.parseColor("#0F6C7A"))
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.END
        }
        val titleTv = TextView(this).apply {
            text = "اختر لونك"
            textSize = 18f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#0F172A"))
            gravity = Gravity.END
            setPadding(0, dp(2), 0, dp(2))
        }
        val subTv = TextView(this).apply {
            text = "تبدأ المباراة من النافذة العائمة."
            textSize = 12f
            setTextColor(Color.parseColor("#475569"))
            gravity = Gravity.END
            setPadding(0, 0, 0, dp(10))
        }
        card.addView(stepTv)
        card.addView(titleTv)
        card.addView(subTv)

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        fun colorChoice(label: String, icon: String, selected: Boolean, onClick: () -> Unit): Button {
            return Button(this).apply {
                text = "$label  $icon"
                isAllCaps = false
                setTextColor(if (selected) Color.WHITE else Color.parseColor("#0F172A"))
                background = GradientDrawable().apply {
                    setColor(if (selected) Color.parseColor("#0F6C7A") else Color.WHITE)
                    cornerRadius = dp(14).toFloat()
                    setStroke(dp(1), Color.parseColor("#CBD5E1"))
                }
                setOnClickListener { onClick() }
            }
        }
        val blackBtn = colorChoice("أسود", "♚", wizardColor == PieceColor.BLACK) {
            wizardColor = PieceColor.BLACK
            mode = OverlayMode.WIZARD_STEP2
            rebuildContent()
        }
        val whiteBtn = colorChoice("أبيض", "♔", wizardColor == PieceColor.WHITE) {
            wizardColor = PieceColor.WHITE
            mode = OverlayMode.WIZARD_STEP2
            rebuildContent()
        }
        row.addView(blackBtn, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = dp(6) })
        row.addView(whiteBtn, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        card.addView(row, LinearLayout.LayoutParams(cardWidthPx - dp(12), LinearLayout.LayoutParams.WRAP_CONTENT))
    }

    private fun buildWizardStep2Ui(card: LinearLayout, cardWidthPx: Int) {
        buildWizardHeader(card)
        val stepTv = TextView(this).apply {
            text = "الخطوة 2 من 2"
            textSize = 11f
            setTextColor(Color.parseColor("#0F6C7A"))
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.END
        }
        val titleTv = TextView(this).apply {
            text = "اضبط الخصم"
            textSize = 17f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#0F172A"))
            gravity = Gravity.END
        }
        val sideNoteTv = TextView(this).apply {
            text = "ستُلعب المباراة ب${if (wizardColor == PieceColor.WHITE) "الأبيض" else "الأسود"}"
            textSize = 11f
            setTextColor(Color.parseColor("#475569"))
            gravity = Gravity.END
            setPadding(0, 0, 0, dp(6))
        }
        card.addView(stepTv)
        card.addView(titleTv)
        card.addView(sideNoteTv)

        val personalityBtn = Button(this).apply {
            text = wizardPersonality.nameAr
            isAllCaps = false
            setTextColor(Color.parseColor("#0F172A"))
            background = GradientDrawable().apply {
                setColor(Color.WHITE)
                cornerRadius = dp(14).toFloat()
            }
            setOnClickListener {
                val all = EnginePersonality.entries
                val nextIdx = (all.indexOf(wizardPersonality) + 1) % all.size
                wizardPersonality = all[nextIdx]
                rebuildContent()
            }
        }
        card.addView(personalityBtn, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            bottomMargin = dp(8)
        })

        val eloTv = TextView(this).apply {
            text = "إيلو مستهدف • نحو $wizardElo إيلو"
            textSize = 12f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#0F6C7A"))
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(4))
        }
        card.addView(eloTv)

        val seek = SeekBar(this).apply {
            max = 2800 // maps 200..3000
            progress = (wizardElo - 200).coerceIn(0, 2800)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar?, p: Int, fromUser: Boolean) {
                    wizardElo = ((p + 200) / 50) * 50
                    eloTv.text = "إيلو مستهدف • نحو $wizardElo إيلو"
                }
                override fun onStartTrackingTouch(sb: SeekBar?) {}
                override fun onStopTrackingTouch(sb: SeekBar?) {}
            })
        }
        card.addView(seek)

        val stepRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(4), 0, dp(8))
        }
        val plusBtn = Button(this).apply {
            text = "+"
            setTextColor(Color.parseColor("#0F172A"))
            background = GradientDrawable().apply { setColor(Color.WHITE); cornerRadius = dp(12).toFloat() }
            setOnClickListener {
                wizardElo = (wizardElo + 100).coerceAtMost(3000)
                seek.progress = wizardElo - 200
                eloTv.text = "إيلو مستهدف • نحو $wizardElo إيلو"
            }
        }
        val rangeTv = TextView(this).apply {
            text = "200 — 3000"
            textSize = 11f
            setTextColor(Color.parseColor("#475569"))
            gravity = Gravity.CENTER
        }
        val minusBtn = Button(this).apply {
            text = "−"
            setTextColor(Color.parseColor("#0F172A"))
            background = GradientDrawable().apply { setColor(Color.WHITE); cornerRadius = dp(12).toFloat() }
            setOnClickListener {
                wizardElo = (wizardElo - 100).coerceAtLeast(200)
                seek.progress = wizardElo - 200
                eloTv.text = "إيلو مستهدف • نحو $wizardElo إيلو"
            }
        }
        stepRow.addView(plusBtn, LinearLayout.LayoutParams(dp(44), dp(38)))
        stepRow.addView(rangeTv, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        stepRow.addView(minusBtn, LinearLayout.LayoutParams(dp(44), dp(38)))
        card.addView(stepRow)

        val actionRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        val startBtn = Button(this).apply {
            text = "ابدأ المباراة"
            isAllCaps = false
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#0F6C7A"))
                cornerRadius = dp(14).toFloat()
            }
            setOnClickListener {
                playerColor = wizardColor
                flipped = playerColor == PieceColor.BLACK
                boardState = ChessBoardState.initial()
                moveStack.clear()
                selectedSquare = null
                legalForSelected = emptyList()
                lastMove = null
                mode = OverlayMode.BOARD
                serviceScope.launch {
                    prefsRepo.setCustomEngineElo(wizardElo)
                    prefsRepo.setEnginePersonality(wizardPersonality)
                }
                rebuildContent()
                if (playerColor == PieceColor.BLACK) {
                    triggerEngineReply()
                }
            }
        }
        val backBtn = Button(this).apply {
            text = "رجوع"
            isAllCaps = false
            setTextColor(Color.parseColor("#0F172A"))
            background = GradientDrawable().apply {
                setColor(Color.WHITE)
                cornerRadius = dp(14).toFloat()
            }
            setOnClickListener {
                mode = OverlayMode.WIZARD_STEP1
                rebuildContent()
            }
        }
        actionRow.addView(startBtn, LinearLayout.LayoutParams(0, dp(40), 1f).apply { marginEnd = dp(6) })
        actionRow.addView(backBtn, LinearLayout.LayoutParams(0, dp(40), 1f))
        card.addView(actionRow)
    }

    private fun handleOverlaySquareTap(sq: Square) {
        if (isThinking || boardState.turn != playerColor) return
        if (boardState.evaluateGameResult() != GameResult.ONGOING) return

        if (selectedSquare != null) {
            val matching = legalForSelected.filter { it.to == sq }
            if (matching.isNotEmpty()) {
                val chosen = matching.find { it.promotion == PieceType.QUEEN } ?: matching.first()
                moveStack.add(boardState to chosen)
                boardState = boardState.copyWithMove(chosen)
                lastMove = chosen
                selectedSquare = null
                legalForSelected = emptyList()
                soundManager.play(if (chosen.capturedPiece != null) ChessSound.CAPTURE else ChessSound.MOVE)
                rebuildContent()

                if (boardState.evaluateGameResult() == GameResult.ONGOING) {
                    triggerEngineReply()
                }
                return
            }
        }

        val p = boardState.pieceAt(sq)
        if (p != null && p.color == playerColor) {
            selectedSquare = sq
            legalForSelected = boardState.generateLegalMoves().filter { it.from == sq }
        } else {
            selectedSquare = null
            legalForSelected = emptyList()
        }
        rebuildContent()
    }

    private fun triggerEngineReply() {
        isThinking = true
        serviceScope.launch {
            delay(250)
            val result = StockfishUciEngine.computeMove(
                context = applicationContext,
                state = boardState,
                backendPreference = prefs.engineBackendPreference,
                targetElo = wizardElo,
                personality = wizardPersonality,
                humanStyle = prefs.humanStyleEnabled,
                moveTimeMs = prefs.stockfishMoveTimeMs,
                threads = prefs.stockfishThreads
            )
            val move = result.bestMove
            if (move != null) {
                moveStack.add(boardState to move)
                boardState = boardState.copyWithMove(move)
                lastMove = move
                soundManager.play(if (move.capturedPiece != null) ChessSound.CAPTURE else ChessSound.MOVE)
            }
            isThinking = false
            rebuildContent()
        }
    }

    private fun undoOverlayMoves() {
        if (moveStack.isEmpty() || isThinking) return
        val dropCount = if (boardState.turn == playerColor && moveStack.size >= 2) 2 else 1
        repeat(dropCount.coerceAtMost(moveStack.size)) {
            val prev = moveStack.removeAt(moveStack.lastIndex)
            boardState = prev.first
        }
        lastMove = moveStack.lastOrNull()?.second
        selectedSquare = null
        legalForSelected = emptyList()
        rebuildContent()
    }

    private fun attachDragAndClick(view: View, onClick: (() -> Unit)?) {
        var initialX = 0
        var initialY = 0
        var touchStartX = 0f
        var touchStartY = 0f
        view.setOnTouchListener { _, event ->
            val params = layoutParams ?: return@setOnTouchListener false
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    touchStartX = event.rawX
                    touchStartY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX + (event.rawX - touchStartX).toInt()
                    params.y = initialY + (event.rawY - touchStartY).toInt()
                    rootView?.let { windowManager.updateViewLayout(it, params) }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val dx = abs(event.rawX - touchStartX)
                    val dy = abs(event.rawY - touchStartY)
                    if (dx < 12f && dy < 12f && onClick != null) {
                        onClick()
                    }
                    true
                }
                else -> false
            }
        }
    }

    companion object {
        const val ACTION_STOP = "com.example.overlay.STOP"
        const val EXTRA_FEN = "extra_fen"
        const val EXTRA_PLAYER_COLOR = "extra_player_color"
        private const val CHANNEL_ID = "floating_chess_overlay_channel"
        private const val NOTIF_ID = 4102

        fun canDrawOverlay(context: Context): Boolean =
            Settings.canDrawOverlays(context)

        fun launch(context: Context, fen: String? = null, playerColor: PieceColor? = null) {
            val intent = Intent(context, FloatingChessService::class.java).apply {
                if (fen != null) putExtra(EXTRA_FEN, fen)
                if (playerColor != null) putExtra(EXTRA_PLAYER_COLOR, playerColor.name)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }
}

private class FloatingBoardCanvasView(context: Context) : View(context) {
    private var boardState: ChessBoardState = ChessBoardState.initial()
    private var boardTheme: BoardTheme = BoardTheme.TOURNAMENT_EMERALD
    private var flipped: Boolean = false
    private var selected: Square? = null
    private var legalTargets: Set<Square> = emptySet()
    private var lastMove: ChessMove? = null
    private var useRings: Boolean = true
    private var legalColorInt: Int = Color.parseColor("#0D9488")
    private var selectedColorInt: Int = Color.parseColor("#AA0D9488")

    var onSquareTapped: ((Square) -> Unit)? = null

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }
    private val strokeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        style = Paint.Style.STROKE
    }

    fun updateState(
        state: ChessBoardState,
        theme: BoardTheme,
        flipped: Boolean,
        selected: Square?,
        legalTargets: Set<Square>,
        lastMove: ChessMove?,
        useRings: Boolean,
        legalColorInt: Int,
        selectedColorInt: Int
    ) {
        this.boardState = state
        this.boardTheme = theme
        this.flipped = flipped
        this.selected = selected
        this.legalTargets = legalTargets
        this.lastMove = lastMove
        this.useRings = useRings
        this.legalColorInt = legalColorInt
        this.selectedColorInt = selectedColorInt
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val size = width.coerceAtMost(height).toFloat()
        if (size <= 0f) return
        val cell = size / 8f

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
                } else if (lm != null && (lm.from == sq || lm.to == sq)) {
                    paint.color = lastMoveColor
                    canvas.drawRect(left, top, left + cell, top + cell, paint)
                }

                // Coordinates
                if (vc == 0) {
                    paint.color = if (isLight) darkColor else lightColor
                    paint.textSize = cell * 0.22f
                    paint.textAlign = Paint.Align.LEFT
                    canvas.drawText("${8 - r}", left + cell * 0.06f, top + cell * 0.25f, paint)
                }
                if (vr == 7) {
                    paint.color = if (isLight) darkColor else lightColor
                    paint.textSize = cell * 0.22f
                    paint.textAlign = Paint.Align.RIGHT
                    canvas.drawText("${'a' + c}", left + cell * 0.94f, top + cell * 0.94f, paint)
                }

                val piece = boardState.pieceAt(sq)
                if (piece != null) {
                    val glyph = unicodePiece(piece)
                    val cx = left + cell * 0.5f
                    val cy = top + cell * 0.74f
                    val pSize = cell * 0.76f
                    strokeTextPaint.textSize = pSize
                    strokeTextPaint.strokeWidth = cell * 0.06f
                    strokeTextPaint.color = if (piece.color == PieceColor.WHITE) Color.parseColor("#1E293B") else Color.WHITE
                    canvas.drawText(glyph, cx, cy, strokeTextPaint)

                    textPaint.textSize = pSize
                    textPaint.color = if (piece.color == PieceColor.WHITE) Color.WHITE else Color.parseColor("#0F172A")
                    canvas.drawText(glyph, cx, cy, textPaint)
                }

                if (sq in legalTargets) {
                    val cx = left + cell * 0.5f
                    val cy = top + cell * 0.5f
                    if (piece != null) {
                        ringPaint.color = legalColorInt
                        ringPaint.strokeWidth = cell * 0.10f
                        canvas.drawCircle(cx, cy, cell * 0.42f, ringPaint)
                    } else if (useRings) {
                        ringPaint.color = legalColorInt
                        ringPaint.strokeWidth = cell * 0.08f
                        canvas.drawCircle(cx, cy, cell * 0.22f, ringPaint)
                    } else {
                        paint.color = legalColorInt
                        canvas.drawCircle(cx, cy, cell * 0.16f, paint)
                    }
                }
            }
        }
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

    private fun unicodePiece(p: ChessPiece): String = when (p.type) {
        PieceType.KING -> "♚"
        PieceType.QUEEN -> "♛"
        PieceType.ROOK -> "♜"
        PieceType.BISHOP -> "♝"
        PieceType.KNIGHT -> "♞"
        PieceType.PAWN -> "♟"
    }
}
