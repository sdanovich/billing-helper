package com.example.claims.android.ui

import androidx.compose.runtime.Composable
import com.danovich.platform.login.ui.LoginConfig
import com.danovich.platform.login.ui.PlatformLoginScreen
import com.example.claims.android.BuildConfig
import com.example.claims.android.R
import com.example.claims.android.data.AppGraph

/**
 * Wraps the platform-stack login screen with this app's config: branding, social provider ids
 * (blank → button disabled), the app's own AuthApi, and a session sink that persists the result.
 */
@Composable
fun LoginRoute(graph: AppGraph, onAuthed: () -> Unit) {
    val config = LoginConfig(
        appName = "Claims Clerk",
        signInSubtitle = "Sign in to view and scan claims",
        createSubtitle = "Create your clerk account",
        googleIconRes = R.drawable.ic_google,
        githubIconRes = R.drawable.ic_github,
        googleServerClientId = BuildConfig.GOOGLE_SERVER_CLIENT_ID,
        githubClientId = BuildConfig.GITHUB_CLIENT_ID,
        githubRedirectUri = BuildConfig.GITHUB_REDIRECT_URI,
        api = graph.authApi,
        onSession = { graph.session.save(it) }
    )
    PlatformLoginScreen(config) { onAuthed() }
}
