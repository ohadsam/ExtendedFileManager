package com.efm.filemanager.ui

import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.efm.filemanager.ui.feature.browse.BrowseScreen
import com.efm.filemanager.ui.feature.help.HelpScreen
import com.efm.filemanager.ui.feature.settings.SettingsScreen
import com.efm.filemanager.ui.nav.EfmDestination
import com.efm.filemanager.ui.nav.EfmDrawerContent
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
            composable(EfmDestination.Browse.route) {
                BrowseScreen(onOpenDrawer = { scope.launch { drawerState.open() } })
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
        }
    }
}

private const val HELP_ROUTE = "help"
