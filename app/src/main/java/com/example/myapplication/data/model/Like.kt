package com.example.myapplication.data.model

data class Like(
    val id: String = "",
    val secretId: String = "",
    val userId: String = "",
    val username: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

