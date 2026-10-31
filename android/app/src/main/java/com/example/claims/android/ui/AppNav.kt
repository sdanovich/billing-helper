package com.example.claims.android.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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

    // Refresh token rejected by the server (e.g. expired after 30 days idle): the session is
    // already cleared, so route back to login instead of leaving screens showing raw 401s.
    val expired by graph.session.expired.collectAsState()
    LaunchedEffect(expired) {
        if (expired && nav.currentDestination?.route != "login") {
            nav.navigate("login") { popUpTo(0) { inclusive = true } }
        }
    }

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
