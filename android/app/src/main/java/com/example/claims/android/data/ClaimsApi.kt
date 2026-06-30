package com.example.claims.android.data

import okhttp3.MultipartBody
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

/** The claims API (token-protected). Auth endpoints live in platform-login-ui's AuthApi. */
interface ClaimsApi {

    @GET("/api/claims")
    suspend fun list(
        @Query("status") status: String?,
        @Query("payer") payer: String?,
        @Query("member_id") memberId: String?,
        @Query("q") q: String?,
        @Query("page") page: Int,
        @Query("size") size: Int,
        @Query("sort") sort: String?
    ): PageResponse<ClaimSummary>

    @GET("/api/claims/{id}")
    suspend fun detail(@Path("id") id: String): ClaimDetail

    @Multipart
    @POST("/api/claims")
    suspend fun ingest(
        @Part claim: MultipartBody.Part,
        @Part image: MultipartBody.Part?
    ): ClaimDetail
}
