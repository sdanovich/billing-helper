package com.example.claims.android

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.danovich.platform.login.ui.OauthRelay
import com.example.claims.android.ui.AppNav
import com.example.claims.android.ui.theme.ClaimsTheme

class MainActivity : ComponentActivity() {

    private val graph by lazy { (application as ClaimsApp).graph }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleOAuthRedirect(intent)
        setContent {
            ClaimsTheme {
                AppNav(graph)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleOAuthRedirect(intent)
    }

    /** Forward the GitHub OAuth redirect (<scheme>://oauth?code=...) to the login screen.
     *  The scheme/host come from the configured redirect URI so this works regardless of
     *  which GitHub OAuth app (and callback) the build is wired to. */
    private fun handleOAuthRedirect(intent: Intent?) {
        val data = intent?.data ?: return
        val redirect = Uri.parse(BuildConfig.GITHUB_REDIRECT_URI)
        if (data.scheme == redirect.scheme && data.host == redirect.host) {
            data.getQueryParameter("code")?.let { OauthRelay.deliver(it) }
        }
    }
}
