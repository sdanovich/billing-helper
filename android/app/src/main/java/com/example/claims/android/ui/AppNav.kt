package com.example.claims.android.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.claims.android.data.AppGraph

@Composable
fun AppNav(graph: AppGraph) {
    val nav = rememberNavController()
    val start = if (graph.session.isLoggedIn()) "list" else "login"

    NavHost(navController = nav, startDestination = start) {
        composable("login") {
            LoginRoute(graph) {
                nav.navigate("list") { popUpTo("login") { inclusive = true } }
            }
        }
        composable("list") {
            ClaimsListScreen(
                graph = graph,
                onOpen = { id -> nav.navigate("detail/$id") },
                onScan = { nav.navigate("scan") },
                onLogout = {
                    graph.session.logout()
                    nav.navigate("login") { popUpTo(0) { inclusive = true } }
                }
            )
        }
        composable(
            "detail/{id}",
            arguments = listOf(navArgument("id") { type = NavType.StringType })
        ) { entry ->
            ClaimDetailScreen(
                graph = graph,
                claimId = entry.arguments?.getString("id").orEmpty(),
                onBack = { nav.popBackStack() }
            )
        }
        composable("scan") {
            ScanScreen(
                graph = graph,
                onDone = { nav.popBackStack() },
                onCancel = { nav.popBackStack() }
            )
        }
    }
}
