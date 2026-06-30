package com.example.claims.android

import android.content.Intent
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

    /** Forward the GitHub OAuth redirect (claimsapp://oauth?code=...) to the login screen. */
    private fun handleOAuthRedirect(intent: Intent?) {
        val data = intent?.data ?: return
        if (data.scheme == "claimsapp" && data.host == "oauth") {
            data.getQueryParameter("code")?.let { OauthRelay.deliver(it) }
        }
    }
}
