package dev.homelab.presseapp

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import dev.homelab.presseapp.data.SecureCredentialStore
import dev.homelab.presseapp.data.Source
import dev.homelab.presseapp.ui.screens.CredentialsScreen
import dev.homelab.presseapp.ui.screens.HomeScreen
import dev.homelab.presseapp.ui.screens.ReaderScreen
import kotlinx.serialization.Serializable

@Serializable
object CredentialsRoute

@Serializable
object HomeRoute

@Serializable
data class ReaderRoute(val sourceName: String)

@Composable
fun AppNavHost(credentialStore: SecureCredentialStore) {
    val navController: NavHostController = rememberNavController()
    val startDestination = if (credentialStore.hasCredentials()) HomeRoute else CredentialsRoute

    NavHost(navController = navController, startDestination = startDestination) {
        composable<CredentialsRoute> {
            CredentialsScreen(
                credentialStore = credentialStore,
                onSaved = {
                    navController.navigate(HomeRoute) {
                        popUpTo(CredentialsRoute) { inclusive = true }
                    }
                },
            )
        }
        composable<HomeRoute> {
            HomeScreen(
                onSourceSelected = { source -> navController.navigate(ReaderRoute(source.name)) },
                onEditCredentials = {
                    navController.navigate(CredentialsRoute)
                },
            )
        }
        composable<ReaderRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<ReaderRoute>()
            val source = Source.valueOf(route.sourceName)
            ReaderScreen(
                source = source,
                credentialStore = credentialStore,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
