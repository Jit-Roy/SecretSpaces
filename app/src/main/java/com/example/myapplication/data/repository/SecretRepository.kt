package com.example.myapplication.data.repository

// DEPRECATED: This file is replaced by FirebaseSecretRepository.kt
// Please use FirebaseSecretRepository for Firebase integration
// This file can be safely deleted

import com.example.myapplication.data.model.CreateSecretRequest
import com.example.myapplication.data.model.Secret
import com.example.myapplication.data.remote.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Deprecated("Use FirebaseSecretRepository instead")
class SecretRepository {

    private val apiService = RetrofitClient.apiService

    suspend fun createSecret(request: CreateSecretRequest): Result<Secret> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.createSecret(request)
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!)
                } else {
                    Result.failure(Exception("Failed to create secret: ${response.message()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun getNearbySecrets(
        latitude: Double,
        longitude: Double,
        radius: Double = 5000.0
    ): Result<List<Secret>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getNearbySecrets(latitude, longitude, radius)
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!.secrets)
                } else {
                    Result.failure(Exception("Failed to fetch secrets: ${response.message()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
