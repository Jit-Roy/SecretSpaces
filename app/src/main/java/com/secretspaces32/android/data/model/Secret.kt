package com.secretspaces32.android.data.model

data class Secret(
    val id: String = "",
    val text: String = "",
    val imageUrl: String? = null,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val timestamp: Long = System.currentTimeMillis(),
    val userId: String = "",
    val username: String = "",
    val userProfilePicture: String? = null,
    val isAnonymous: Boolean = false,
    val likeCount: Int = 0,
    val commentCount: Int = 0,
    val distance: Double? = null, // Distance from user in meters
    val isLikedByCurrentUser: Boolean = false
)

data class CreateSecretRequest(
    val text: String,
    val imageBase64: String?,
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long,
    val userId: String,
    val username: String,
    val userProfilePicture: String?,
    val isAnonymous: Boolean = false
)

data class SecretsResponse(
    val secrets: List<Secret>
)
