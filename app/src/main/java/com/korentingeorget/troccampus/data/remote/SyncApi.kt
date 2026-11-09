package com.korentingeorget.troccampus.data.remote

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.POST

@Serializable
data class AnnonceDto(
    val id: Long,
    val title: String,
    val type: String,
    val price: Int,
    val campus: String,
    val contact: String,
    val description: String,
    val createdAt: Long,
    val lat: Double? = null,
    val lng: Double? = null,
    val hasPhoto: Boolean = false,
)

@Serializable
data class EchoResponse(
    val url: String = "",
    val json: AnnonceDto? = null,
    val status: String? = null,
    val synced: Boolean? = null,
)

interface SyncApi {
    @POST("api/sync")
    suspend fun pushToCoolify(@Body body: AnnonceDto): EchoResponse

    @POST("post")
    suspend fun pushToHttpbin(@Body body: AnnonceDto): EchoResponse
}
