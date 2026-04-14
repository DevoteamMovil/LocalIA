package com.arcadiapps.localIA.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.arcadiapps.localIA.ui.chat.ChatScreen
import com.arcadiapps.localIA.ui.home.HomeScreen
import com.arcadiapps.localIA.ui.models.ModelsScreen
import com.arcadiapps.localIA.ui.settings.SettingsScreen

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Models : Screen("models")
    object Settings : Screen("settings")
    object Chat : Screen("chat/{sessionId}") {
        fun createRoute(sessionId: String) = "chat/$sessionId"
    }
}

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Screen.Home.route) {
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToModels = { navController.navigate(Screen.Models.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToChat = { sessionId ->
                    navController.navigate(Screen.Chat.createRoute(sessionId))
                }
            )
        }
        composable(Screen.Models.route) {
            ModelsScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.Settings.route) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
        composable(
            route = Screen.Chat.route,
            arguments = listOf(navArgument("sessionId") { type = NavType.StringType })
        ) { backStack ->
            val sessionId = backStack.arguments?.getString("sessionId") ?: return@composable
            ChatScreen(
                sessionId = sessionId,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
