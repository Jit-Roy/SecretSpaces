package com.example.myapplication.data.model

data class User(
    val id: String = "",
    val email: String = "",
    val username: String = "",
    val profilePictureUrl: String? = null,
    val bio: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

data class UpdateUserRequest(
    val username: String? = null,
    val bio: String? = null,
    val profilePictureUrl: String? = null
)

