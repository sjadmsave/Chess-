package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.notifications.DailyReminderScheduler
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.PlayScreen
import com.example.ui.screens.PuzzlesScreen
import com.example.ui.screens.SettingsSheetContent
import com.example.ui.screens.StatsScreen
import com.example.ui.theme.EliteChessTheme
import com.example.viewmodel.ChessViewModel

const val ROUTE_HOME = "home"
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
    NavTab(ROUTE_PLAY, "اللوحة", Icons.Rounded.SportsEsports, Icons.Outlined.SportsEsports, "nav_play"),
    NavTab(ROUTE_PUZZLES, "الألغاز", Icons.Rounded.Extension, Icons.Outlined.Extension, "nav_puzzles"),
    NavTab(ROUTE_STATS, "الأداء", Icons.Rounded.BarChart, Icons.Outlined.BarChart, "nav_stats")
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        DailyReminderScheduler.ensureChannel(this)

        setContent {
            val viewModel: ChessViewModel = viewModel()
            val prefs by viewModel.preferences.collectAsStateWithLifecycle()

            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                EliteChessTheme(themeMode = prefs.themeMode) {
                    EliteChessRoot(viewModel = viewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EliteChessRoot(viewModel: ChessViewModel) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: ROUTE_HOME

    val prefs by viewModel.preferences.collectAsStateWithLifecycle()
    val ongoingGame by viewModel.ongoingGame.collectAsStateWithLifecycle()
    val completedGames by viewModel.completedGames.collectAsStateWithLifecycle()
    val ratingHistory by viewModel.ratingHistory.collectAsStateWithLifecycle()
    val puzzleRecords by viewModel.puzzleRecords.collectAsStateWithLifecycle()
    val achievements by viewModel.achievements.collectAsStateWithLifecycle()
    val selectedPeriod by viewModel.selectedPeriod.collectAsStateWithLifecycle()
    val periodicReport by viewModel.periodicReport.collectAsStateWithLifecycle()
    val matchState by viewModel.matchState.collectAsStateWithLifecycle()
    val puzzleState by viewModel.puzzleState.collectAsStateWithLifecycle()

    var showSettings by remember { mutableStateOf(false) }

    fun navigateTo(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            bottomBar = {
                if (!useRail) {
                    NavigationBar {
                        NAV_TABS.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationBarItem(
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
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail) {
                    NavigationRail(
                        modifier = Modifier.fillMaxHeight(),
                        header = {
                            IconButton(onClick = { showSettings = true }) {
                                Icon(Icons.Rounded.Settings, contentDescription = "الإعدادات")
                            }
                        }
                    ) {
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
                            onToggleAdaptiveMode = { viewModel.setAdaptiveMode(it) },
                            onStartMatch = { bot, color, adaptive ->
                                viewModel.startNewMatch(bot, color, adaptive)
                                navigateTo(ROUTE_PLAY)
                            },
                            onResumeGame = { saved ->
                                viewModel.resumeSavedGame(saved)
                                navigateTo(ROUTE_PLAY)
                            },
                            onDiscardOngoingGame = { id ->
                                viewModel.discardOngoingGame(id)
                            },
                            onOpenDailyPuzzle = {
                                viewModel.loadDailyPuzzle()
                                navigateTo(ROUTE_PUZZLES)
                            },
                            onOpenSettings = { showSettings = true }
                        )
                    }

                    composable(ROUTE_PLAY) {
                        PlayScreen(
                            matchState = matchState,
                            prefs = prefs,
                            onSquareClick = { viewModel.onMatchSquareClick(it) },
                            onSelectPromotion = { viewModel.selectPromotion(it) },
                            onDismissPromotion = { viewModel.dismissPromotion() },
                            onRequestHint = { viewModel.requestHint() },
                            onUndo = { viewModel.undoLastTurn() },
                            onFlipBoard = { viewModel.toggleBoardFlip() },
                            onResign = { viewModel.resignMatch() },
                            onSelectReviewMove = { viewModel.selectReviewMove(it) },
                            onOpenPostGameSheet = { viewModel.openPostGameSheet() },
                            onDismissPostGameSheet = { viewModel.dismissPostGameSheet() },
                            onStartRematch = {
                                viewModel.startNewMatch(
                                    bot = matchState.bot,
                                    playerColor = matchState.playerColor,
                                    adaptiveMode = prefs.adaptiveMode
                                )
                            }
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
                            onSelectPeriod = { viewModel.selectReportPeriod(it) },
                            onReviewSavedGame = { saved ->
                                viewModel.resumeSavedGame(saved)
                                navigateTo(ROUTE_PLAY)
                            }
                        )
                    }
                }
            }
        }
    }

    if (showSettings) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showSettings = false },
            sheetState = sheetState
        ) {
            SettingsSheetContent(
                prefs = prefs,
                onSelectThemeMode = { viewModel.setThemeMode(it) },
                onSelectBoardTheme = { viewModel.setBoardTheme(it) },
                onSelectPieceStyle = { viewModel.setPieceStyle(it) },
                onToggleSound = { viewModel.setSoundEnabled(it) },
                onToggleHaptics = { viewModel.setHapticsEnabled(it) },
                onToggleEvalBar = { viewModel.setShowEvalBar(it) },
                onUpdateReminders = { enabled, hr, min -> viewModel.updateReminderSettings(enabled, hr, min) },
                onTestNotification = { viewModel.sendTestNotification() }
            )
        }
    }
}
