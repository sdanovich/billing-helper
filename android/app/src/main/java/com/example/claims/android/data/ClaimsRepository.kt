package com.example.claims.android.data

import android.content.ContentResolver
import android.net.Uri
import com.squareup.moshi.Moshi
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

/** Talks to the claims API; assembles the multipart ingest request. */
class ClaimsRepository(
    private val api: ClaimsApi,
    private val resolver: ContentResolver,
    moshi: Moshi
) {
    private val claimAdapter = moshi.adapter(IngestClaim::class.java)

    suspend fun list(
        status: String?, payer: String?, memberId: String?, q: String?,
        page: Int, size: Int, sort: String?
    ): PageResponse<ClaimSummary> = api.list(status, payer, memberId, q, page, size, sort)

    suspend fun detail(id: String): ClaimDetail = api.detail(id)

    suspend fun ingest(claim: IngestClaim, imageUri: Uri?): ClaimDetail {
        val claimPart = MultipartBody.Part.createFormData(
            "claim", null,
            claimAdapter.toJson(claim).toRequestBody("application/json".toMediaType())
        )
        val imagePart = imageUri?.let { uri ->
            val bytes = resolver.openInputStream(uri)?.use { it.readBytes() } ?: ByteArray(0)
            MultipartBody.Part.createFormData(
                "image", "scan.jpg", bytes.toRequestBody("image/jpeg".toMediaType())
            )
        }
        return api.ingest(claimPart, imagePart)
    }
}
