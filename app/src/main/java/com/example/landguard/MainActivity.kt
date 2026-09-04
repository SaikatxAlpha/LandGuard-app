package com.example.landguard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.landguard.ui.alerts.AlertHistoryScreen
import com.example.landguard.ui.auth.OnboardingScreen
import com.example.landguard.ui.home.HomeScreen
import com.example.landguard.ui.theme.LandGuardTheme
import dagger.hilt.android.AndroidEntryPoint

object Routes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val MAP = "map"
    const val ALERT_HISTORY = "alert_history"
    const val REPORT = "report"
    const val PROFILE = "profile"
    const val ALERT_DETAIL = "alert_detail/{alertId}"
    fun alertDetail(id: String) = "alert_detail/$id"
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LandGuardTheme {
                LandGuardApp()
            }
        }
    }
}

@Composable
fun LandGuardApp(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Routes.ONBOARDING) {

        composable(Routes.ONBOARDING) {
            OnboardingScreen(
                onVerified = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.HOME) {
            HomeScreen(
                onOpenAlert = { id -> navController.navigate(Routes.alertDetail(id)) },
                onOpenMap = { navController.navigate(Routes.MAP) },
                onOpenAlertHistory = { navController.navigate(Routes.ALERT_HISTORY) },
                onOpenReport = { navController.navigate(Routes.REPORT) },
                onOpenProfile = { navController.navigate(Routes.PROFILE) }
            )
        }

        composable(Routes.ALERT_HISTORY) {
            AlertHistoryScreen(
                onOpenAlert = { id -> navController.navigate(Routes.alertDetail(id)) }
            )
        }

        composable(Routes.MAP) { PlaceholderScreen("Risk Map") }
        composable(Routes.REPORT) { PlaceholderScreen("Report an Observation") }
        composable(Routes.PROFILE) { PlaceholderScreen("Profile & Settings") }
        composable(Routes.ALERT_DETAIL) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("alertId") ?: ""
            PlaceholderScreen("Alert Detail: $id")
        }
    }
}

@Composable
private fun PlaceholderScreen(title: String) {
    Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
        Text(text = "$title — coming soon", modifier = Modifier.padding(padding).padding(16.dp = androidx.compose.ui.unit.dp))
    }
}