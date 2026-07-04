package com.prolocity.patchtracker.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.prolocity.patchtracker.ui.PatchTrackerViewModel
import com.prolocity.patchtracker.ui.patches.PatchEditScreen
import com.prolocity.patchtracker.ui.patches.PatchListScreen
import com.prolocity.patchtracker.ui.patchtypes.PatchTypesScreen
import com.prolocity.patchtracker.ui.players.PlayerEditScreen
import com.prolocity.patchtracker.ui.players.PlayerListScreen
import com.prolocity.patchtracker.ui.sessions.SessionDetailScreen
import com.prolocity.patchtracker.ui.sessions.SessionListScreen
import com.prolocity.patchtracker.ui.sessions.SessionReviewScreen
import com.prolocity.patchtracker.ui.teams.TeamEditScreen
import com.prolocity.patchtracker.ui.teams.TeamListScreen

private data class TopLevelTab(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private val topLevelTabs = listOf(
    TopLevelTab(Routes.PATCHES, "Patches", Icons.Filled.Star),
    TopLevelTab(Routes.PLAYERS, "Players", Icons.Filled.Person),
    TopLevelTab(Routes.TEAMS, "Teams", Icons.Filled.Groups),
    TopLevelTab(Routes.PATCH_TYPES, "Patch Types", Icons.AutoMirrored.Filled.List),
    TopLevelTab(Routes.SESSIONS, "Sessions", Icons.Filled.Event)
)

@Composable
fun PatchTrackerNavHost(viewModel: PatchTrackerViewModel) {
    val navController: NavHostController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination

    Scaffold(
        bottomBar = {
            val isTopLevel = topLevelTabs.any { currentRoute?.hierarchy?.any { dest -> dest.route == it.route } == true }
            if (isTopLevel) {
                NavigationBar {
                    topLevelTabs.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute?.hierarchy?.any { it.route == tab.route } == true,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.PATCHES,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Routes.PATCHES) {
                PatchListScreen(
                    viewModel = viewModel,
                    onAddClick = { navController.navigate(Routes.patchEdit(Routes.NEW_ID)) },
                    onEditClick = { id -> navController.navigate(Routes.patchEdit(id)) }
                )
            }
            composable(
                Routes.PATCH_EDIT_PATTERN,
                arguments = listOf(navArgument("id") { type = NavType.LongType })
            ) { entry ->
                val id = entry.arguments?.getLong("id") ?: Routes.NEW_ID
                PatchEditScreen(
                    viewModel = viewModel,
                    patchAwardId = id,
                    onDone = { navController.popBackStack() },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Routes.PLAYERS) {
                PlayerListScreen(
                    viewModel = viewModel,
                    onAddClick = { navController.navigate(Routes.playerEdit(Routes.NEW_ID)) },
                    onEditClick = { id -> navController.navigate(Routes.playerEdit(id)) }
                )
            }
            composable(
                Routes.PLAYER_EDIT_PATTERN,
                arguments = listOf(navArgument("id") { type = NavType.LongType })
            ) { entry ->
                val id = entry.arguments?.getLong("id") ?: Routes.NEW_ID
                PlayerEditScreen(
                    viewModel = viewModel,
                    playerId = id,
                    onDone = { navController.popBackStack() },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Routes.PATCH_TYPES) {
                PatchTypesScreen(viewModel = viewModel)
            }
            composable(Routes.TEAMS) {
                TeamListScreen(
                    viewModel = viewModel,
                    onAddClick = { navController.navigate(Routes.teamEdit(Routes.NEW_ID)) },
                    onEditClick = { id -> navController.navigate(Routes.teamEdit(id)) }
                )
            }
            composable(
                Routes.TEAM_EDIT_PATTERN,
                arguments = listOf(navArgument("id") { type = NavType.LongType })
            ) { entry ->
                val id = entry.arguments?.getLong("id") ?: Routes.NEW_ID
                TeamEditScreen(
                    viewModel = viewModel,
                    teamId = id,
                    onDone = { navController.popBackStack() },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Routes.SESSIONS) {
                SessionListScreen(
                    viewModel = viewModel,
                    onSessionClick = { id -> navController.navigate(Routes.sessionDetail(id)) },
                    onReviewOpened = { navController.navigate(Routes.SESSION_REVIEW) }
                )
            }
            composable(
                Routes.SESSION_DETAIL_PATTERN,
                arguments = listOf(navArgument("id") { type = NavType.LongType })
            ) { entry ->
                val id = entry.arguments?.getLong("id") ?: Routes.NEW_ID
                SessionDetailScreen(
                    viewModel = viewModel,
                    sessionId = id,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Routes.SESSION_REVIEW) {
                SessionReviewScreen(
                    viewModel = viewModel,
                    onClose = { navController.popBackStack() }
                )
            }
        }
    }
}
