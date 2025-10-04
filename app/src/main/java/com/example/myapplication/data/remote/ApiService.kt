package com.example.myapplication.data.remote

import com.example.myapplication.data.model.CreateSecretRequest
import com.example.myapplication.data.model.Secret
import com.example.myapplication.data.model.SecretsResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface ApiService {

    @POST("/api/secrets")
    suspend fun createSecret(
        @Body request: CreateSecretRequest
    ): Response<Secret>

    @GET("/api/secrets")
    suspend fun getNearbySecrets(
        @Query("lat") latitude: Double,
        @Query("lon") longitude: Double,
        @Query("radius") radius: Double = 5000.0 // Default 5km
    ): Response<SecretsResponse>
}

