package com.example.claims.android.data

import android.content.Context
import com.danovich.platform.auth.TokenStore
import com.danovich.platform.login.ui.AuthResponse

/**
 * Persists the long-lived refresh token (+ identity) so the clerk stays signed in across cold
 * starts — the foundation for refresh-token-backed PIN quick-unlock. The short-lived JWT itself
 * stays only in memory ({@link TokenStore}); it's re-minted from the refresh token on demand.
 */
class SessionStore(context: Context) {

    private val prefs = context.getSharedPreferences("claims_session", Context.MODE_PRIVATE)

    val refreshToken: String? get() = prefs.getString(KEY_REFRESH, null)
    val email: String? get() = prefs.getString(KEY_EMAIL, null)
    val userId: String? get() = prefs.getString(KEY_USER, null)

    fun isLoggedIn(): Boolean = refreshToken != null

    /** Store a fresh session: JWT in memory, refresh token + identity persisted. */
    fun save(resp: AuthResponse) {
        TokenStore.set(resp.token)
        prefs.edit()
            .putString(KEY_REFRESH, resp.refreshToken)
            .putString(KEY_EMAIL, resp.email)
            .putString(KEY_USER, resp.userId)
            .apply()
    }

    fun logout() {
        TokenStore.clear()
        prefs.edit().clear().apply()
    }

    private companion object {
        const val KEY_REFRESH = "refresh"
        const val KEY_EMAIL = "email"
        const val KEY_USER = "userId"
    }
}
