package com.efm.filemanager.ui

import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.PermanentDrawerSheet
import androidx.compose.material3.PermanentNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalConfiguration
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.efm.filemanager.domain.model.FileCategory
import com.efm.filemanager.domain.model.GlobalFilesSort
import com.efm.filemanager.ui.feature.advisor.StorageAdvisorScreen
import com.efm.filemanager.ui.feature.audit.AuditScreen
import com.efm.filemanager.ui.feature.browse.BrowseNavActions
import com.efm.filemanager.ui.feature.browse.BrowseScreen
import com.efm.filemanager.ui.feature.browse.BrowseViewModel
import com.efm.filemanager.ui.feature.dashboard.DashboardNavActions
import com.efm.filemanager.ui.feature.dashboard.DashboardScreen
import com.efm.filemanager.ui.feature.duplicates.DuplicatesScreen
import com.efm.filemanager.ui.feature.favorites.FavoritesScreen
import com.efm.filemanager.ui.feature.globalfiles.GLOBAL_FILES_ALL_CATEGORIES
import com.efm.filemanager.ui.feature.globalfiles.GLOBAL_FILES_CATEGORY_ARG
import com.efm.filemanager.ui.feature.globalfiles.GLOBAL_FILES_SORT_ARG
import com.efm.filemanager.ui.feature.globalfiles.GlobalFileListScreen
import com.efm.filemanager.ui.feature.help.HelpScreen
import com.efm.filemanager.ui.feature.logs.LogsScreen
import com.efm.filemanager.ui.feature.preview.PreviewScreen
import com.efm.filemanager.ui.feature.search.SearchScreen
import com.efm.filemanager.ui.feature.settings.SettingsNavActions
import com.efm.filemanager.ui.feature.settings.SettingsScreen
import com.efm.filemanager.ui.feature.statistics.StatisticsNavActions
import com.efm.filemanager.ui.feature.statistics.StatisticsScreen
import com.efm.filemanager.ui.feature.tags.ManageTagsScreen
import com.efm.filemanager.ui.feature.vault.VaultScreen
import com.efm.filemanager.ui.feature.whatsnew.WhatsNewDialog
import com.efm.filemanager.ui.feature.whatsnew.WhatsNewViewModel
import com.efm.filemanager.ui.nav.EfmDestination
import com.efm.filemanager.ui.nav.EfmDrawerContent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun EfmApp(
    initialDestinationRoute: String? = null,
    whatsNewViewModel: WhatsNewViewModel = hiltViewModel(),
) {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    val currentDestination =
        EfmDestination.entries.firstOrNull { it.route == currentRoute } ?: EfmDestination.Browse
    val whatsNewEntries by whatsNewViewModel.entriesToShow.collectAsStateWithLifecycle()

    // Set only when launched from Phase 17's daily-insights notification -- a one-shot jump
    // straight to that screen instead of landing on Browse like every other cold start.
    LaunchedEffect(initialDestinationRoute) {
        if (initialDestinationRoute != null) navController.navigate(initialDestinationRoute)
    }

    val onDrawerDestinationClick: (EfmDestination) -> Unit = { destination ->
        scope.launch { drawerState.close() }
        navController.navigate(destination.route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    // Phase 14's adaptive layout: a tablet/unfolded-foldable-width screen gets the drawer
    // permanently visible (no hamburger needed), same as every other Material 3 app at this
    // width class; a phone-width screen keeps today's modal drawer unchanged.
    if (isExpandedWidthScreen()) {
        EfmExpandedLayout(navController, scope, drawerState, currentDestination, onDrawerDestinationClick)
    } else {
        EfmCompactLayout(navController, scope, drawerState, currentDestination, onDrawerDestinationClick)
    }

    if (whatsNewEntries.isNotEmpty()) {
        WhatsNewDialog(entries = whatsNewEntries, onDismiss = whatsNewViewModel::dismiss)
    }
}

/** The default, phone-width layout -- unchanged from before Phase 14's adaptive-layout work. */
@Composable
private fun EfmCompactLayout(
    navController: NavHostController,
    scope: CoroutineScope,
    drawerState: DrawerState,
    currentDestination: EfmDestination,
    onDestinationClick: (EfmDestination) -> Unit,
) {
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                EfmDrawerContent(currentDestination = currentDestination, onDestinationClick = onDestinationClick)
            }
        },
    ) {
        NavHost(navController = navController, startDestination = EfmDestination.Browse.route) {
            efmDestinations(navController, scope, drawerState)
        }
    }
}

/**
 * Tablet/unfolded-foldable width: the same drawer content, always visible, no open/close gesture
 * needed. Every screen's own `onOpenDrawer` callback still fires harmlessly into [drawerState]
 * (nothing renders a [ModalNavigationDrawer] here to observe it) rather than needing every
 * screen's top bar to know which layout it's in.
 */
@Composable
private fun EfmExpandedLayout(
    navController: NavHostController,
    scope: CoroutineScope,
    drawerState: DrawerState,
    currentDestination: EfmDestination,
    onDestinationClick: (EfmDestination) -> Unit,
) {
    PermanentNavigationDrawer(
        drawerContent = {
            PermanentDrawerSheet {
                EfmDrawerContent(currentDestination = currentDestination, onDestinationClick = onDestinationClick)
            }
        },
    ) {
        NavHost(navController = navController, startDestination = EfmDestination.Browse.route) {
            efmDestinations(navController, scope, drawerState)
        }
    }
}

/** Material's own "Expanded" width-class breakpoint (tablets, unfolded foldables). */
internal const val EXPANDED_WIDTH_BREAKPOINT_DP = 840

internal fun isExpandedWidth(screenWidthDp: Int): Boolean = screenWidthDp >= EXPANDED_WIDTH_BREAKPOINT_DP

@Composable
private fun isExpandedWidthScreen(): Boolean = isExpandedWidth(LocalConfiguration.current.screenWidthDp)

private fun NavGraphBuilder.efmDestinations(
    navController: NavHostController,
    scope: CoroutineScope,
    drawerState: DrawerState,
) {
    efmDrawerDestinations(navController, scope, drawerState)
    efmDashboardDestination(navController, scope, drawerState)
    efmModalDestinations(navController)
}

private fun NavGraphBuilder.efmDrawerDestinations(
    navController: NavHostController,
    scope: CoroutineScope,
    drawerState: DrawerState,
) {
    composable(EfmDestination.Browse.route) {
        BrowseScreen(
            navActions =
                BrowseNavActions(
                    onOpenDrawer = { scope.launch { drawerState.open() } },
                    onOpenSearch = { navController.navigate(SEARCH_ROUTE) },
                    onOpenPreview = { navController.navigate(PREVIEW_ROUTE) },
                    onOpenManageTags = { navController.navigate(MANAGE_TAGS_ROUTE) },
                ),
        )
    }
    composable(EfmDestination.Duplicates.route) {
        DuplicatesScreen(
            onOpenDrawer = { scope.launch { drawerState.open() } },
            onOpenPreview = { navController.navigate(PREVIEW_ROUTE) },
            onOpenManageTags = { navController.navigate(MANAGE_TAGS_ROUTE) },
        )
    }
    composable(EfmDestination.Favorites.route) {
        FavoritesScreen(
            onOpenDrawer = { scope.launch { drawerState.open() } },
            onOpenPreview = { navController.navigate(PREVIEW_ROUTE) },
            onOpenManageTags = { navController.navigate(MANAGE_TAGS_ROUTE) },
        )
    }
    composable(EfmDestination.Insights.route) {
        StorageAdvisorScreen(
            onOpenDrawer = { scope.launch { drawerState.open() } },
            onOpenPreview = { navController.navigate(PREVIEW_ROUTE) },
        )
    }
    composable(EfmDestination.Statistics.route) {
        StatisticsScreen(
            onOpenDrawer = { scope.launch { drawerState.open() } },
            navActions = statisticsNavActions(navController),
        )
    }
    composable(EfmDestination.Vault.route) {
        VaultScreen(onOpenDrawer = { scope.launch { drawerState.open() } })
    }
    composable(EfmDestination.Logs.route) {
        LogsScreen(onOpenDrawer = { scope.launch { drawerState.open() } })
    }
    composable(EfmDestination.Audit.route) {
        AuditScreen(onOpenDrawer = { scope.launch { drawerState.open() } })
    }
    composable(EfmDestination.Settings.route) {
        SettingsScreen(
            onNavigateBack = { navController.popBackStack() },
            navActions = settingsNavActions(navController),
        )
    }
}

// A separate NavGraphBuilder function (same reason statisticsNavActions()/settingsNavActions()
// are their own functions): efmDrawerDestinations() was already close to detekt's LongMethod
// threshold, and Phase 19's own screen is logically distinct from every other drawer entry
// above it anyway (it reuses several of their routes rather than owning new ones).
private fun NavGraphBuilder.efmDashboardDestination(
    navController: NavHostController,
    scope: CoroutineScope,
    drawerState: DrawerState,
) {
    composable(EfmDestination.Dashboard.route) {
        DashboardScreen(
            onOpenDrawer = { scope.launch { drawerState.open() } },
            navActions = dashboardNavActions(navController),
        )
    }
}

// Extracted so efmDashboardDestination() stays trivial.
private fun dashboardNavActions(navController: NavHostController): DashboardNavActions =
    DashboardNavActions(
        onOpenStatistics = { navController.navigate(EfmDestination.Statistics.route) },
        onOpenDuplicates = { navController.navigate(EfmDestination.Duplicates.route) },
        onOpenInsights = { navController.navigate(EfmDestination.Insights.route) },
        onOpenFavorites = { navController.navigate(EfmDestination.Favorites.route) },
        onOpenSearch = { navController.navigate(SEARCH_ROUTE) },
    )

// Both extracted so efmDrawerDestinations() stays under detekt's LongMethod threshold.
private fun statisticsNavActions(navController: NavHostController): StatisticsNavActions =
    StatisticsNavActions(
        onOpenDuplicates = { navController.navigate(EfmDestination.Duplicates.route) },
        onOpenInsights = { navController.navigate(EfmDestination.Insights.route) },
        onOpenAudit = { navController.navigate(EfmDestination.Audit.route) },
        onOpenGlobalFiles = { category, sort -> navController.navigate(globalFilesRoute(category, sort)) },
    )

private fun globalFilesRoute(
    category: FileCategory?,
    sort: GlobalFilesSort,
): String = "$GLOBAL_FILES_ROUTE/${category?.name ?: GLOBAL_FILES_ALL_CATEGORIES}/${sort.name}"

private fun settingsNavActions(navController: NavHostController): SettingsNavActions =
    SettingsNavActions(
        onOpenLogs = { navController.navigate(EfmDestination.Logs.route) },
        onOpenAudit = { navController.navigate(EfmDestination.Audit.route) },
        onOpenHelp = { navController.navigate(HELP_ROUTE) },
    )

private fun NavGraphBuilder.efmModalDestinations(navController: NavHostController) {
    composable(HELP_ROUTE) {
        HelpScreen(onNavigateBack = { navController.popBackStack() })
    }
    composable(SEARCH_ROUTE) { entry ->
        EfmSearchDestination(navController, entry)
    }
    composable(PREVIEW_ROUTE) {
        PreviewScreen(onNavigateBack = { navController.popBackStack() }, onOpenManageTags = { navController.navigate(MANAGE_TAGS_ROUTE) })
    }
    composable(MANAGE_TAGS_ROUTE) {
        ManageTagsScreen(onNavigateBack = { navController.popBackStack() })
    }
    composable(
        "$GLOBAL_FILES_ROUTE/{$GLOBAL_FILES_CATEGORY_ARG}/{$GLOBAL_FILES_SORT_ARG}",
        arguments =
            listOf(
                navArgument(GLOBAL_FILES_CATEGORY_ARG) { type = NavType.StringType },
                navArgument(GLOBAL_FILES_SORT_ARG) { type = NavType.StringType },
            ),
    ) {
        GlobalFileListScreen(
            onNavigateBack = { navController.popBackStack() },
            onOpenPreview = { navController.navigate(PREVIEW_ROUTE) },
        )
    }
}

@Composable
private fun EfmSearchDestination(
    navController: NavHostController,
    entry: NavBackStackEntry,
) {
    // Browse is the graph's start destination, so its back stack entry (and this shared
    // ViewModel instance) outlives navigating here and back -- letting a tapped search result
    // jump straight into Browse's own navigation state instead of duplicating
    // breadcrumb-rebuilding logic in a second ViewModel.
    val browseEntry = remember(entry) { navController.getBackStackEntry(EfmDestination.Browse.route) }
    val browseViewModel: BrowseViewModel = hiltViewModel(browseEntry)
    SearchScreen(
        onNavigateBack = { navController.popBackStack() },
        onOpenLocation = { entry ->
            browseViewModel.navigateToLocation(entry)
            navController.popBackStack()
        },
        onOpenPreview = { navController.navigate(PREVIEW_ROUTE) },
        onOpenManageTags = { navController.navigate(MANAGE_TAGS_ROUTE) },
    )
}

private const val HELP_ROUTE = "help"
private const val SEARCH_ROUTE = "search"
private const val PREVIEW_ROUTE = "preview"
private const val MANAGE_TAGS_ROUTE = "manage_tags"
private const val GLOBAL_FILES_ROUTE = "global_files"
