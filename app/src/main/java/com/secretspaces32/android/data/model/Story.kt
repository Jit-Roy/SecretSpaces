package com.secretspaces32.android.data.model

data class Story(
    val id: String = "",
    val userId: String = "",
    val username: String = "",
    val userProfilePicture: String? = null,
    val imageUrl: String? = null,
    val caption: String? = null,
    val timestamp: Long = 0,
    val expiresAt: Long = 0, // 24 hours from creation
    val viewCount: Int = 0,
    val isActive: Boolean = true
)

data class StoryView(
    val id: String = "",
    val storyId: String = "",
    val userId: String = "",
    val username: String = "",
    val timestamp: Long = 0
)
