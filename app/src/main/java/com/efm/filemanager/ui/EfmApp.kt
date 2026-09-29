package com.efm.filemanager.ui

import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.efm.filemanager.ui.feature.browse.BrowseNavActions
import com.efm.filemanager.ui.feature.browse.BrowseScreen
import com.efm.filemanager.ui.feature.browse.BrowseViewModel
import com.efm.filemanager.ui.feature.duplicates.DuplicatesScreen
import com.efm.filemanager.ui.feature.favorites.FavoritesScreen
import com.efm.filemanager.ui.feature.help.HelpScreen
import com.efm.filemanager.ui.feature.preview.PreviewScreen
import com.efm.filemanager.ui.feature.search.SearchScreen
import com.efm.filemanager.ui.feature.settings.SettingsScreen
import com.efm.filemanager.ui.feature.tags.ManageTagsScreen
import com.efm.filemanager.ui.nav.EfmDestination
import com.efm.filemanager.ui.nav.EfmDrawerContent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun EfmApp() {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    val currentDestination =
        EfmDestination.entries.firstOrNull { it.route == currentRoute } ?: EfmDestination.Browse

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                EfmDrawerContent(
                    currentDestination = currentDestination,
                    onDestinationClick = { destination ->
                        scope.launch { drawerState.close() }
                        navController.navigate(destination.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
        },
    ) {
        NavHost(navController = navController, startDestination = EfmDestination.Browse.route) {
            efmDestinations(navController, scope, drawerState)
        }
    }
}

private fun NavGraphBuilder.efmDestinations(
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
    composable(EfmDestination.Settings.route) {
        SettingsScreen(
            onNavigateBack = { navController.popBackStack() },
            onOpenHelp = { navController.navigate(HELP_ROUTE) },
        )
    }
    composable(HELP_ROUTE) {
        HelpScreen(onNavigateBack = { navController.popBackStack() })
    }
    composable(SEARCH_ROUTE) {
        EfmSearchDestination(navController)
    }
    composable(PREVIEW_ROUTE) {
        PreviewScreen(onNavigateBack = { navController.popBackStack() }, onOpenManageTags = { navController.navigate(MANAGE_TAGS_ROUTE) })
    }
    composable(MANAGE_TAGS_ROUTE) {
        ManageTagsScreen(onNavigateBack = { navController.popBackStack() })
    }
}

@Composable
private fun EfmSearchDestination(navController: NavHostController) {
    // Browse is the graph's start destination, so its back stack entry (and this shared
    // ViewModel instance) outlives navigating here and back -- letting a tapped search result
    // jump straight into Browse's own navigation state instead of duplicating
    // breadcrumb-rebuilding logic in a second ViewModel.
    val browseEntry = remember(navController) { navController.getBackStackEntry(EfmDestination.Browse.route) }
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
