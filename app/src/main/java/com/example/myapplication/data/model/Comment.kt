package com.example.myapplication.data.model

data class Comment(
    val id: String = "",
    val secretId: String = "",
    val userId: String = "",
    val username: String = "",
    val userProfilePicture: String? = null,
    val text: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

