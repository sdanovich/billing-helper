package com.example.claims.android.data

import android.content.Context
import coil.ImageLoader
import com.danovich.platform.auth.BearerAuthInterceptor
import com.danovich.platform.auth.TokenProvider
import com.danovich.platform.auth.TokenRefreshInterceptor
import com.danovich.platform.login.ui.AuthApi
import com.danovich.platform.login.ui.RefreshRequest
import com.example.claims.android.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

/**
 * Manual dependency graph. Two OkHttp clients on purpose:
 *  - [authClient]: plain, used by [authApi] and the token refresh (no interceptors → no recursion).
 *  - data client: carries the bearer and refreshes on 401. Interceptor order is the one the
 *    platform-auth skill mandates — refresh OUTER, bearer INNER — so a retried request after a
 *    401 still picks up the freshly minted token.
 */
class AppGraph(context: Context) {

    private val baseUrl = BuildConfig.BASE_URL
    private val refreshPath = "/api/auth/refresh"

    private val moshi: Moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

    val session = SessionStore(context)

    private val logging = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
    }

    private val authClient = OkHttpClient.Builder().addInterceptor(logging).build()
    val authApi: AuthApi = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(authClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(AuthApi::class.java)

    /** Per-user token refresh: trade the stored refresh token for a new JWT (and a rotated refresh). */
    private val tokenProvider = TokenProvider {
        val rt = session.refreshToken ?: return@TokenProvider null
        try {
            val resp = runBlocking { authApi.refresh(RefreshRequest(rt)) }
            session.save(resp)
            resp.token
        } catch (e: Exception) {
            null
        }
    }

    private val dataClient = OkHttpClient.Builder()
        .addInterceptor(TokenRefreshInterceptor(tokenProvider, refreshPath)) // OUTER
        .addInterceptor(BearerAuthInterceptor(refreshPath))                  // INNER
        .addInterceptor(logging)                                            // logs the final request
        .build()

    private val claimsApi: ClaimsApi = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(dataClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(ClaimsApi::class.java)

    /** Coil loader sharing the data client so image requests carry the bearer header. */
    val imageLoader: ImageLoader = ImageLoader.Builder(context).okHttpClient(dataClient).build()

    val repository = ClaimsRepository(claimsApi, context.contentResolver, moshi)

    fun imageUrl(claimId: String): String = "$baseUrl/api/claims/$claimId/image"
}
