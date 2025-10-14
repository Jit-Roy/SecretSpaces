package com.secretspaces32.android.data.model

data class User(
    val id: String = "",
    val email: String = "",
    val username: String = "",
    val profilePictureUrl: String? = null,
    val bio: String = "",
    val followersCount: Int = 0,
    val followingCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

data class UpdateUserRequest(
    val username: String? = null,
    val bio: String? = null,
    val profilePictureUrl: String? = null
)
