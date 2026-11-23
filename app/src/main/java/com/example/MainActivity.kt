package com.example

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ShowChart
import androidx.compose.material.icons.automirrored.rounded.ShowChart
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.chess.GameMode
import com.example.chess.PieceColor
import com.example.chess.StrengthTier
import com.example.notifications.DailyReminderScheduler
import com.example.overlay.FloatingChessService
import com.example.ui.components.BoardCustomizationSheetContent
import com.example.ui.components.EngineEloDialog
import com.example.ui.components.EnginePersonalitySheetContent
import com.example.ui.components.FenBoardSetupSheetContent
import com.example.ui.components.FloatingCustomizationSheetContent
import com.example.ui.components.ImportPgnFenDialog
import com.example.ui.screens.AnalysisScreen
import com.example.ui.screens.ArchiveScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.PlayScreen
import com.example.ui.screens.PuzzlesScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StatsScreen
import com.example.ui.theme.EliteChessTheme
import com.example.viewmodel.ChessViewModel

const val ROUTE_HOME = "home"
const val ROUTE_ARCHIVE = "archive"
const val ROUTE_ANALYSIS = "analysis"
const val ROUTE_SETTINGS = "settings"
const val ROUTE_PLAY = "play"
const val ROUTE_PUZZLES = "puzzles"
const val ROUTE_STATS = "stats"

private data class NavTab(
    val route: String,
    val labelAr: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val tag: String
)

private val NAV_TABS = listOf(
    NavTab(ROUTE_HOME, "الرئيسية", Icons.Rounded.Home, Icons.Outlined.Home, "nav_home"),
    NavTab(ROUTE_ARCHIVE, "الأرشيف", Icons.Rounded.Description, Icons.Outlined.Description, "nav_archive"),
    NavTab(ROUTE_ANALYSIS, "التحليل", Icons.AutoMirrored.Rounded.ShowChart, Icons.AutoMirrored.Outlined.ShowChart, "nav_analysis"),
    NavTab(ROUTE_SETTINGS, "الإعدادات", Icons.Rounded.Settings, Icons.Outlined.Settings, "nav_settings")
)

class MainActivity : ComponentActivity() {
    private var incomingExternalFen by mutableStateOf<Pair<String, PieceColor>?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes = window.attributes.apply {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
        applyImmersiveMode()
        DailyReminderScheduler.ensureChannel(this)
        extractOverlayExtras(intent)

        setContent {
            val viewModel: ChessViewModel = viewModel()
            val prefs by viewModel.preferences.collectAsStateWithLifecycle()

            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                EliteChessTheme(themeMode = prefs.themeMode) {
                    EliteChessRoot(
                        viewModel = viewModel,
                        incomingExternalFen = incomingExternalFen,
                        onConsumedExternalFen = { incomingExternalFen = null }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        extractOverlayExtras(intent)
    }

    private fun extractOverlayExtras(intent: Intent?) {
        val fen = intent?.getStringExtra(FloatingChessService.EXTRA_FEN) ?: return
        val colorStr = intent.getStringExtra(FloatingChessService.EXTRA_PLAYER_COLOR)
        val color = runCatching {
            PieceColor.valueOf(colorStr ?: PieceColor.WHITE.name)
        }.getOrDefault(PieceColor.WHITE)
        incomingExternalFen = fen to color
    }

    override fun onResume() {
        super.onResume()
        applyImmersiveMode()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            applyImmersiveMode()
        }
    }

    private fun applyImmersiveMode() {
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EliteChessRoot(
    viewModel: ChessViewModel,
    incomingExternalFen: Pair<String, PieceColor>?,
    onConsumedExternalFen: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: ROUTE_HOME

    val prefs by viewModel.preferences.collectAsStateWithLifecycle()
    val ongoingGame by viewModel.ongoingGame.collectAsStateWithLifecycle()
    val allGames by viewModel.allGames.collectAsStateWithLifecycle()
    val completedGames by viewModel.completedGames.collectAsStateWithLifecycle()
    val ratingHistory by viewModel.ratingHistory.collectAsStateWithLifecycle()
    val puzzleRecords by viewModel.puzzleRecords.collectAsStateWithLifecycle()
    val achievements by viewModel.achievements.collectAsStateWithLifecycle()
    val selectedPeriod by viewModel.selectedPeriod.collectAsStateWithLifecycle()
    val periodicReport by viewModel.periodicReport.collectAsStateWithLifecycle()
    val unassistedStatsOnly by viewModel.unassistedStatsOnly.collectAsStateWithLifecycle()
    val engineStatsSummary by viewModel.engineStatsSummary.collectAsStateWithLifecycle()
    val matchState by viewModel.matchState.collectAsStateWithLifecycle()
    val puzzleState by viewModel.puzzleState.collectAsStateWithLifecycle()
    val stockfishStatus by viewModel.stockfishStatus.collectAsStateWithLifecycle()
    val lastEngineTelemetry by viewModel.lastEngineTelemetry.collectAsStateWithLifecycle()
    val syzygyTables by viewModel.syzygyTables.collectAsStateWithLifecycle()
    val syzygyStatusMessage by viewModel.syzygyStatusMessage.collectAsStateWithLifecycle()

    var showBoardCustomizationSheet by remember { mutableStateOf(false) }
    var showFloatingCustomizationSheet by remember { mutableStateOf(false) }
    var showPersonalitySheet by remember { mutableStateOf(false) }
    var showEngineEloDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var showFenSetupSheet by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    var pendingOverlayPayload by remember { mutableStateOf<Pair<String?, PieceColor?>?>(null) }
    var pendingExportPgn by remember { mutableStateOf<String?>(null) }

    val exportPgnLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/x-chess-pgn")
    ) { uri ->
        val pgnContent = pendingExportPgn
        pendingExportPgn = null
        if (uri != null && pgnContent != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(pgnContent.toByteArray(Charsets.UTF_8))
                }
            }
        }
    }

    val overlayPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        if (FloatingChessService.canDrawOverlay(context)) {
            val payload = pendingOverlayPayload
            FloatingChessService.launch(context, payload?.first, payload?.second)
        }
        pendingOverlayPayload = null
    }

    fun requestOrLaunchFloatingOverlay(fen: String? = null, color: PieceColor? = null) {
        if (FloatingChessService.canDrawOverlay(context)) {
            FloatingChessService.launch(context, fen, color)
        } else {
            pendingOverlayPayload = fen to color
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")
            )
            overlayPermissionLauncher.launch(intent)
        }
    }

    fun exportMatchPgnFile() {
        pendingExportPgn = viewModel.exportCurrentGamePgn()
        exportPgnLauncher.launch("Ruqaa.pgn")
    }

    fun navigateTo(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    LaunchedEffect(incomingExternalFen) {
        val payload = incomingExternalFen ?: return@LaunchedEffect
        viewModel.startCustomEngineMatch(
            playerColor = payload.second,
            startFen = payload.first,
            edited = true
        )
        navigateTo(ROUTE_PLAY)
        onConsumedExternalFen()
    }

    // Auto-show floating overlay when minimizing app while a game is ongoing
    DisposableEffect(lifecycleOwner, prefs.floatingAutoShow, matchState.gameResult) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP &&
                prefs.floatingAutoShow &&
                matchState.gameResult.isOngoing &&
                FloatingChessService.canDrawOverlay(context)
            ) {
                val activity = context as? ComponentActivity
                if (activity?.isChangingConfigurations != true) {
                    FloatingChessService.launch(
                        context = context,
                        fen = matchState.boardState.toFen(),
                        playerColor = matchState.playerColor
                    )
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val hideBottomNav = currentRoute == ROUTE_PLAY

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            containerColor = MaterialTheme.colorScheme.background
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                Row(modifier = Modifier.fillMaxSize()) {
                    if (useRail) {
                        NavigationRail(modifier = Modifier.fillMaxHeight()) {
                            NAV_TABS.forEach { tab ->
                                val selected = currentRoute == tab.route
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = { navigateTo(tab.route) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = tab.labelAr
                                        )
                                    },
                                    label = { Text(tab.labelAr) },
                                    modifier = Modifier.testTag(tab.tag)
                                )
                            }
                        }
                    }

                    NavHost(
                        navController = navController,
                        startDestination = ROUTE_HOME,
                        modifier = Modifier.weight(1f)
                    ) {
                        composable(ROUTE_HOME) {
                            HomeScreen(
                                prefs = prefs,
                                ongoingGame = ongoingGame,
                                onStartCustomMatch = { color, elo, personality, humanStyle, timeControl ->
                                    viewModel.startCustomEngineMatch(
                                        playerColor = color,
                                        targetElo = elo,
                                        personality = personality,
                                        humanStyle = humanStyle,
                                        timeControl = timeControl
                                    )
                                    navigateTo(ROUTE_PLAY)
                                },
                                onStartBotMatch = { bot, color, adaptive, timeControl ->
                                    viewModel.startNewMatch(bot, color, adaptive, timeControl)
                                    navigateTo(ROUTE_PLAY)
                                },
                                onStartLocalMatch = { timeControl ->
                                    viewModel.startLocalTwoPlayerMatch(timeControl = timeControl)
                                    navigateTo(ROUTE_PLAY)
                                },
                                onResumeGame = { saved ->
                                    viewModel.resumeSavedGame(saved)
                                    navigateTo(ROUTE_PLAY)
                                },
                                onActivateFloatingWindow = {
                                    requestOrLaunchFloatingOverlay(
                                        fen = ongoingGame?.currentFen ?: matchState.boardState.toFen(),
                                        color = matchState.playerColor
                                    )
                                },
                                onOpenImportDialog = { showImportDialog = true },
                                onOpenDailyPuzzle = {
                                    viewModel.loadDailyPuzzle()
                                    navigateTo(ROUTE_PUZZLES)
                                },
                                onOpenStats = { navigateTo(ROUTE_STATS) },
                                onOpenAboutLicenses = { showAboutDialog = true }
                            )
                        }

                        composable(ROUTE_ARCHIVE) {
                            ArchiveScreen(
                                allGames = allGames,
                                prefs = prefs,
                                onOpenStats = { navigateTo(ROUTE_STATS) },
                                onOpenGame = { saved ->
                                    viewModel.resumeSavedGame(saved)
                                    navigateTo(ROUTE_PLAY)
                                },
                                onDeleteGame = { id -> viewModel.discardOngoingGame(id) }
                            )
                        }

                        composable(ROUTE_ANALYSIS) {
                            AnalysisScreen(
                                matchState = matchState,
                                onOpenFenSetup = { showFenSetupSheet = true },
                                onAnalyzeCurrentMatch = {
                                    if (matchState.recordedMoves.isNotEmpty() && !matchState.isReviewingPastMove) {
                                        viewModel.selectReviewMove(matchState.recordedMoves.lastIndex)
                                    }
                                    navigateTo(ROUTE_PLAY)
                                },
                                onAnalyzeMultiPvPosition = {
                                    navigateTo(ROUTE_PLAY)
                                    viewModel.analyzeDisplayedPositionMultiPv()
                                },
                                onOpenImportDialog = { showImportDialog = true },
                                onOpenPuzzles = { navigateTo(ROUTE_PUZZLES) },
                                onOpenSavedArchive = { navigateTo(ROUTE_ARCHIVE) }
                            )
                        }

                        composable(ROUTE_SETTINGS) {
                            SettingsScreen(
                                prefs = prefs,
                                stockfishStatus = stockfishStatus,
                                syzygyTables = syzygyTables,
                                syzygyStatusMessage = syzygyStatusMessage,
                                onSelectThemeMode = { viewModel.setThemeMode(it) },
                                onSelectBoardTheme = { viewModel.setBoardTheme(it) },
                                onOpenBoardCustomization = { showBoardCustomizationSheet = true },
                                onSelectStrengthTier = { viewModel.setStrengthTier(it) },
                                onOpenEngineEloDialog = { showEngineEloDialog = true },
                                onToggleHumanStyle = { viewModel.setHumanStyleEnabled(it) },
                                onOpenPersonalitySheet = { showPersonalitySheet = true },
                                onSelectEngineBackend = { viewModel.setEngineBackendPreference(it) },
                                onSelectStockfishMoveTime = { viewModel.setStockfishMoveTimeMs(it) },
                                onRefreshStockfish = { viewModel.refreshStockfishProbe() },
                                onToggleEvalBar = { viewModel.setShowEvalBar(it) },
                                onToggleFloatingAutoShow = { viewModel.setFloatingAutoShow(it) },
                                onOpenFloatingNow = {
                                    requestOrLaunchFloatingOverlay(
                                        fen = matchState.boardState.toFen(),
                                        color = matchState.playerColor
                                    )
                                },
                                onOpenFloatingCustomization = { showFloatingCustomizationSheet = true },
                                onToggleSound = { viewModel.setSoundEnabled(it) },
                                onSelectSoundProfile = { viewModel.setSoundProfile(it) },
                                onChangeSoundVolume = { viewModel.setSoundVolume(it) },
                                onToggleHaptics = { viewModel.setHapticsEnabled(it) },
                                onSelectVibrationStyle = { viewModel.setVibrationStyle(it) },
                                onToggleAutoReview = { viewModel.setAutoReviewAfterGame(it) },
                                onUpdateReminders = { enabled, hr, min ->
                                    viewModel.updateReminderSettings(enabled, hr, min)
                                },
                                onTestNotification = { viewModel.sendTestNotification() },
                                onImportSyzygyFiles = { uris -> viewModel.importSyzygyFiles(uris) },
                                onDeleteSyzygyTable = { name -> viewModel.deleteSyzygyTable(name) },
                                onClearSyzygyTables = { viewModel.clearSyzygyTables() },
                                onOpenAboutLicenses = { showAboutDialog = true }
                            )
                        }

                        composable(ROUTE_PLAY) {
                            PlayScreen(
                                matchState = matchState,
                                prefs = prefs,
                                onNavigateBack = { navController.popBackStack() },
                                onSquareClick = { viewModel.onMatchSquareClick(it) },
                                onSelectPromotion = { viewModel.selectPromotion(it) },
                                onDismissPromotion = { viewModel.dismissPromotion() },
                                onRequestHint = { viewModel.requestHint() },
                                onUndo = { viewModel.undoLastTurn() },
                                onFlipBoard = { viewModel.toggleBoardFlip() },
                                onResign = { viewModel.resignMatch() },
                                onClaimDraw = { viewModel.claimDraw() },
                                onSelectReviewMove = { viewModel.selectReviewMove(it) },
                                onOpenMoveLedger = { viewModel.openMoveLedgerSheet() },
                                onDismissMoveLedger = { viewModel.dismissMoveLedgerSheet() },
                                onOpenPostGameSheet = { viewModel.openPostGameSheet() },
                                onDismissPostGameSheet = { viewModel.dismissPostGameSheet() },
                                onStartOrResumeFullReview = { viewModel.startOrResumeFullReview() },
                                onPauseFullReview = { viewModel.pauseFullReview() },
                                onAnalyzeMultiPv = { viewModel.analyzeDisplayedPositionMultiPv() },
                                onExportPgn = { exportMatchPgnFile() },
                                onStartRematch = {
                                    when (matchState.gameMode) {
                                        GameMode.LOCAL_TWO_PLAYER -> viewModel.startLocalTwoPlayerMatch(
                                            timeControl = matchState.timeControl,
                                            startFen = matchState.customStartFen,
                                            edited = matchState.editedPosition
                                        )
                                        GameMode.VS_ENGINE -> viewModel.startCustomEngineMatch(
                                            playerColor = matchState.playerColor,
                                            targetElo = matchState.customEngineElo,
                                            personality = matchState.customPersonality,
                                            humanStyle = matchState.humanStyle,
                                            timeControl = matchState.timeControl,
                                            startFen = matchState.customStartFen,
                                            edited = matchState.editedPosition
                                        )
                                        GameMode.VS_BOT_PERSONA -> viewModel.startNewMatch(
                                            bot = matchState.bot,
                                            playerColor = matchState.playerColor,
                                            adaptiveMode = prefs.adaptiveMode,
                                            timeControl = matchState.timeControl
                                        )
                                    }
                                },
                                onToggleEvalBar = { viewModel.setShowEvalBar(it) },
                                onOpenInFloatingWindow = { fen, color ->
                                    requestOrLaunchFloatingOverlay(fen, color)
                                },
                                engineTelemetry = lastEngineTelemetry
                            )
                        }

                        composable(ROUTE_PUZZLES) {
                            PuzzlesScreen(
                                puzzleState = puzzleState,
                                prefs = prefs,
                                solvedRecords = puzzleRecords,
                                onSquareClick = { viewModel.onPuzzleSquareClick(it) },
                                onShowHint = { viewModel.showPuzzleHint() },
                                onResetPuzzle = { viewModel.loadPuzzle(puzzleState.puzzle, puzzleState.isDaily) },
                                onSelectPuzzle = { puzzle, isDaily -> viewModel.loadPuzzle(puzzle, isDaily) }
                            )
                        }

                        composable(ROUTE_STATS) {
                            StatsScreen(
                                prefs = prefs,
                                selectedPeriod = selectedPeriod,
                                periodicReport = periodicReport,
                                ratingHistory = ratingHistory,
                                completedGames = completedGames,
                                achievements = achievements,
                                unassistedOnly = unassistedStatsOnly,
                                engineStatsSummary = engineStatsSummary,
                                onSelectPeriod = { viewModel.selectReportPeriod(it) },
                                onToggleUnassistedOnly = { viewModel.setUnassistedStatsOnly(it) },
                                onReviewSavedGame = { saved ->
                                    viewModel.resumeSavedGame(saved)
                                    navigateTo(ROUTE_PLAY)
                                }
                            )
                        }
                    }
                }

                // Floating Pill Bottom Navigation Bar
                if (!useRail && !hideBottomNav) {
                    FloatingBottomNavPill(
                        currentRoute = currentRoute,
                        onSelectRoute = { navigateTo(it) },
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }
            }
        }
    }

    // 1. Board & Piece Customization Modal Bottom Sheet
    if (showBoardCustomizationSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showBoardCustomizationSheet = false },
            sheetState = sheetState
        ) {
            BoardCustomizationSheetContent(
                prefs = prefs,
                onSave = { style, material, selHex, lastHex, legalHex, showLast, showLegal, useRings ->
                    viewModel.saveBoardAppearance(style, material, selHex, lastHex, legalHex, showLast, showLegal, useRings)
                    showBoardCustomizationSheet = false
                },
                onDismiss = { showBoardCustomizationSheet = false }
            )
        }
    }

    // 2. Floating Window Customization Modal Bottom Sheet
    if (showFloatingCustomizationSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showFloatingCustomizationSheet = false },
            sheetState = sheetState
        ) {
            FloatingCustomizationSheetContent(
                prefs = prefs,
                onSave = { widthDp, frameShape, opacityPercent, bubbleSize ->
                    viewModel.saveFloatingCustomization(widthDp, frameShape, opacityPercent, bubbleSize)
                    showFloatingCustomizationSheet = false
                },
                onDismiss = { showFloatingCustomizationSheet = false }
            )
        }
    }

    // 3. Engine Personality Selection Sheet (13 Personalities + Intensity & Risk)
    if (showPersonalitySheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showPersonalitySheet = false },
            sheetState = sheetState
        ) {
            EnginePersonalitySheetContent(
                selectedPersonality = prefs.enginePersonality,
                intensity = prefs.personalityIntensity,
                risk = prefs.personalityRisk,
                onSelectPersonality = { viewModel.setEnginePersonality(it) },
                onIntensityChange = { viewModel.setPersonalityIntensity(it) },
                onSelectRisk = { viewModel.setPersonalityRisk(it) },
                onDismiss = { showPersonalitySheet = false }
            )
        }
    }

    // 4. Custom Engine ELO Dialog (5 Tiers + Unlimited + 200..3000)
    if (showEngineEloDialog) {
        EngineEloDialog(
            initialElo = prefs.effectiveEngineElo,
            initialTier = prefs.effectiveStrengthTier,
            onConfirmPreset = { tier -> viewModel.setStrengthTier(tier) },
            onConfirmCustomElo = { elo -> viewModel.setCustomEngineElo(elo) },
            onDismiss = { showEngineEloDialog = false }
        )
    }

    // 5. Import PGN or FEN Dialog
    if (showImportDialog) {
        ImportPgnFenDialog(
            onImported = { parsed ->
                viewModel.importParsedGame(parsed)
                navigateTo(ROUTE_PLAY)
            },
            onDismiss = { showImportDialog = false }
        )
    }

    // 6. Interactive FEN Board Setup Sheet
    if (showFenSetupSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showFenSetupSheet = false },
            sheetState = sheetState
        ) {
            FenBoardSetupSheetContent(
                boardTheme = prefs.boardTheme,
                pieceStyle = prefs.pieceStyle,
                onStartFromFen = { fen, color, twoPlayer ->
                    if (twoPlayer) {
                        viewModel.startLocalTwoPlayerMatch(startFen = fen, edited = true)
                    } else {
                        viewModel.startCustomEngineMatch(
                            playerColor = color,
                            startFen = fen,
                            edited = true
                        )
                    }
                    navigateTo(ROUTE_PLAY)
                },
                onDismiss = { showFenSetupSheet = false }
            )
        }
    }

    // 7. About Ruqaa & Open Source Licenses Dialog
    if (showAboutDialog) {
        AboutRuqaaDialog(onDismiss = { showAboutDialog = false })
    }
}

@Composable
private fun AboutRuqaaDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("حول رُقعة والتراخيص", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "تطبيق رُقعة للشطرنج • الإصدار 3.2.3",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "يعمل التطبيق بالكامل دون اتصال بالإنترنت (Offline ١٠٠٪) ودون جمع أي بيانات شخصية أو تتبع. تبقى المباريات وجداول النهايات محفوظة محليًا على جهازك فقط.",
                    style = MaterialTheme.typography.bodySmall
                )
                HorizontalDivider()
                Text(
                    text = "محرك Stockfish 19 (ترخيص GPLv3)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "يعتمد التحليل العميق واللعب المخصّص على محرك Stockfish الحر مفتوح المصدر بموجب رخصة GNU GPL v3. مساهمو Stockfish (AUTHORS): Tord Romstad, Marco Costalba, Joona Kiiski, Gary Linscott وفريق المطوّرين والمختبرين عبر Fishtest.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                HorizontalDivider()
                Text(
                    text = "جداول Syzygy لنهايات اللعب",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "يدعم التطبيق استيراد جداول Syzygy (.rtbw و .rtbz) حتى ٦ قطع عبر مستكشف الملفات المحلي (SAF) لقراءتها محليًا.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                HorizontalDivider()
                Text(
                    text = "خط IBM Plex Sans Arabic (رخصة SIL OFL 1.1)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "الخط العربي المدمج في الواجهة: IBM Plex Sans Arabic بأوزان 400 و 600 وفق ترخيص SIL Open Font License 1.1.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("إغلاق", fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
private fun FloatingBottomNavPill(
    currentRoute: String,
    onSelectRoute: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp)),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 3.dp,
            shadowElevation = 10.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                NAV_TABS.forEach { tab ->
                    val selected = currentRoute == tab.route ||
                        (tab.route == ROUTE_ARCHIVE && currentRoute == ROUTE_STATS) ||
                        (tab.route == ROUTE_ANALYSIS && currentRoute == ROUTE_PUZZLES)

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(20.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onSelectRoute(tab.route) }
                            .padding(vertical = 2.dp)
                            .testTag(tab.tag),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    if (selected) MaterialTheme.colorScheme.primaryContainer
                                    else Color.Transparent
                                )
                                .padding(horizontal = 16.dp, vertical = 5.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                contentDescription = tab.labelAr,
                                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Text(
                            text = tab.labelAr,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
