package com.raksha.video.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.raksha.video.auth.AuthScreen
import com.raksha.video.call.CallScreen
import com.raksha.video.call.CreateCallScreen
import com.raksha.video.call.JoinCallScreen
import com.raksha.video.core.navigation.Routes
import com.raksha.video.data.AppPreferences
import com.raksha.video.home.HomeScreen
import com.raksha.video.permissions.PermissionsScreen
import com.raksha.video.settings.SettingsScreen
import com.raksha.video.safetycenter.SafetyCenterScreen

@Composable
fun RakshaNavHost(startDestination: String, preferences: AppPreferences, navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = startDestination) {
        composable(Routes.Auth) { AuthScreen(onAuthenticated = { navController.navigate(Routes.Permissions) { popUpTo(Routes.Auth) { inclusive = true } } }, preferences = preferences) }
        composable(Routes.Permissions) { PermissionsScreen(onContinue = { navController.navigate(Routes.Home) { popUpTo(Routes.Permissions) { inclusive = true } } }, preferences = preferences) }
        composable(Routes.Home) { HomeScreen(onSettings = { navController.navigate(Routes.Settings) }, onNewCall = { navController.navigate(Routes.CreateCall) }, onJoinCall = { navController.navigate(Routes.JoinCall) }, onSafetyCenter = { navController.navigate(Routes.SafetyCenter) }) }
        composable(Routes.Settings) { SettingsScreen(preferences = preferences, onBack = { navController.popBackStack() }, onLoggedOut = { navController.navigate(Routes.Auth) { popUpTo(0) } }) }
        composable(Routes.SafetyCenter) { SafetyCenterScreen(onBack = { navController.popBackStack() }) }
        composable(Routes.CreateCall) { CreateCallScreen(onCallCreated = { id -> navController.navigate(Routes.call(id, true)) }, onJoinExisting = { navController.navigate(Routes.JoinCall) }, onBack = { navController.popBackStack() }) }
        composable(Routes.JoinCall) { JoinCallScreen(onJoin = { id -> navController.navigate(Routes.call(id, false)) }, onBack = { navController.popBackStack() }) }
        composable(Routes.Call) { entry ->
            val id = entry.arguments?.getString("callId") ?: return@composable
            val caller = entry.arguments?.getString("caller")?.toBoolean() ?: false
            CallScreen(callId = id, caller = caller, onEnded = { navController.popBackStack(Routes.Home, false) })
        }
    }
}
