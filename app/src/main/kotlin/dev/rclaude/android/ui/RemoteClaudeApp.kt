package dev.rclaude.android.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dev.rclaude.android.di.AppContainer
import dev.rclaude.android.ui.connect.ConnectScreen
import dev.rclaude.android.ui.connect.ConnectViewModel
import dev.rclaude.android.ui.settings.SettingsScreen
import dev.rclaude.android.ui.settings.SettingsViewModel
import dev.rclaude.android.ui.session.SessionScreen
import dev.rclaude.android.ui.session.SessionViewModel
import dev.rclaude.android.ui.sessions.SessionsScreen
import dev.rclaude.android.ui.sessions.SessionsViewModel

private object Routes {
    const val CONNECT = "connect"
    const val SESSIONS = "sessions"
    const val SETTINGS = "settings"
    const val SESSION = "session/{id}"

    fun session(id: String): String = "session/$id"
}

/** Навигация приложения: подключение → список сессий → сессия. */
@Composable
fun RemoteClaudeApp(container: AppContainer) {
    var startRoute by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        startRoute = if (container.settings.current() != null) Routes.SESSIONS else Routes.CONNECT
    }
    val start = startRoute
    if (start == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = start) {
        composable(Routes.CONNECT) {
            val viewModel: ConnectViewModel = viewModel(
                factory = viewModelFactory {
                    initializer { ConnectViewModel(container.api, container.settings) }
                },
            )
            ConnectScreen(
                state = viewModel.state,
                onLinkChanged = viewModel::onLinkChanged,
                onTokenChanged = viewModel::onTokenChanged,
                onScanned = viewModel::onScanned,
                onCheck = viewModel::check,
                onSave = viewModel::save,
                onSavedHandled = viewModel::onSavedHandled,
                onSaved = {
                    navController.navigate(Routes.SESSIONS) {
                        popUpTo(Routes.CONNECT) { inclusive = true }
                    }
                },
                onBack = if (navController.previousBackStackEntry != null) {
                    { navController.popBackStack() }
                } else {
                    null
                },
            )
        }

        composable(Routes.SESSIONS) {
            val viewModel: SessionsViewModel = viewModel(
                factory = viewModelFactory {
                    initializer { SessionsViewModel(container.api, container.settings) }
                },
            )
            SessionsScreen(
                state = viewModel.state,
                onRefresh = viewModel::refresh,
                onOpen = { session -> navController.navigate(Routes.session(session.id)) },
                onSettings = { navController.navigate(Routes.SETTINGS) },
            )
        }

        composable(Routes.SETTINGS) {
            val viewModel: SettingsViewModel = viewModel(
                factory = viewModelFactory {
                    initializer { SettingsViewModel(container.settings) }
                },
            )
            SettingsScreen(
                state = viewModel.state,
                onPickStyle = viewModel::choose,
                onEditConnection = { navController.navigate(Routes.CONNECT) },
                onBack = { navController.popBackStack() },
            )
        }

        composable(
            route = Routes.SESSION,
            arguments = listOf(navArgument("id") { type = NavType.StringType }),
        ) { entry ->
            val sessionId = entry.arguments?.getString("id").orEmpty()
            val viewModel: SessionViewModel = viewModel(
                key = sessionId,
                factory = viewModelFactory {
                    initializer {
                        SessionViewModel(
                            sessionId = sessionId,
                            api = container.api,
                            settings = container.settings,
                            socketFactory = container.socketFactory,
                        )
                    }
                },
            )
            SessionScreen(
                state = viewModel.state,
                onPrompt = viewModel::sendPrompt,
                onKey = viewModel::sendKey,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
