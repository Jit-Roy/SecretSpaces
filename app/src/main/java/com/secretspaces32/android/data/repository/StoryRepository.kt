package com.secretspaces32.android.data.repository

import android.content.Context
import android.net.Uri
import com.secretspaces32.android.data.model.Story
import com.secretspaces32.android.data.model.StoryView
import com.secretspaces32.android.data.storage.CloudinaryStorageManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import java.util.UUID

class StoryRepository(context: Context) {
    private val firestore = FirebaseFirestore.getInstance()
    private val storageManager = CloudinaryStorageManager(context)
    private val auth = FirebaseAuth.getInstance()

    private val storiesCollection = firestore.collection("stories")
    private val storyViewsCollection = firestore.collection("story_views")

    companion object {
        private const val STORY_DURATION_MS = 24 * 60 * 60 * 1000L // 24 hours
    }

    /**
     * Create a new story with image
     */
    suspend fun createStory(
        imageUri: Uri,
        caption: String?,
        username: String,
        userProfilePicture: String?
    ): Result<Story> {
        return try {
            val userId = auth.currentUser?.uid 
                ?: return Result.failure(Exception("Not authenticated"))

            // Upload story image using centralized storage manager
            val imageUrl = storageManager.uploadStoryImage(imageUri).getOrNull()
                ?: return Result.failure(Exception("Failed to upload story image"))

            val storyId = UUID.randomUUID().toString()
            val timestamp = System.currentTimeMillis()
            
            val story = Story(
                id = storyId,
                userId = userId,
                username = username,
                userProfilePicture = userProfilePicture,
                imageUrl = imageUrl,
                caption = caption,
                timestamp = timestamp,
                expiresAt = timestamp + STORY_DURATION_MS,
                viewCount = 0,
                isActive = true
            )

            storiesCollection.document(storyId).set(story).await()
            Result.success(story)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get all active stories from users the current user follows
     */
    suspend fun getActiveStories(followingUserIds: List<String>): Result<Map<String, List<Story>>> {
        return try {
            val currentTime = System.currentTimeMillis()
            
            if (followingUserIds.isEmpty()) {
                return Result.success(emptyMap())
            }

            // Fetch stories from followed users
            val snapshot = storiesCollection
                .whereIn("userId", followingUserIds.take(10)) // Firestore has a limit of 10 items in whereIn
                .whereGreaterThan("expiresAt", currentTime)
                .whereEqualTo("isActive", true)
                .orderBy("expiresAt")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()

            val stories = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Story::class.java)
            }

            // Group stories by user
            val storiesByUser = stories.groupBy { it.userId }

            Result.success(storiesByUser)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get stories for a specific user
     */
    suspend fun getUserStories(userId: String): Result<List<Story>> {
        return try {
            val currentTime = System.currentTimeMillis()

            // Simplified query - only filter by userId to avoid index requirement
            val snapshot = storiesCollection
                .whereEqualTo("userId", userId)
                .get()
                .await()

            // Filter in code instead of in the query
            val stories = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Story::class.java)
            }.filter { story ->
                // Filter out expired and inactive stories
                story.expiresAt > currentTime && story.isActive
            }.sortedByDescending { it.timestamp }

            println("DEBUG: StoryRepository - Found ${stories.size} active stories for user $userId")
            Result.success(stories)
        } catch (e: Exception) {
            println("DEBUG: StoryRepository - Error loading stories: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Get a single story by ID
     */
    suspend fun getStory(storyId: String): Result<Story> {
        return try {
            val document = storiesCollection.document(storyId).get().await()
            val story = document.toObject(Story::class.java)

            if (story != null) {
                Result.success(story)
            } else {
                Result.failure(Exception("Story not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Mark a story as viewed by current user
     */
    suspend fun viewStory(storyId: String, username: String): Result<Unit> {
        return try {
            val userId = auth.currentUser?.uid
                ?: return Result.failure(Exception("Not authenticated"))

            val viewId = "${userId}_${storyId}"
            val existingView = storyViewsCollection.document(viewId).get().await()

            if (!existingView.exists()) {
                // Create new view record
                val storyView = StoryView(
                    id = viewId,
                    storyId = storyId,
                    userId = userId,
                    username = username,
                    timestamp = System.currentTimeMillis()
                )
                storyViewsCollection.document(viewId).set(storyView).await()

                // Increment view count
                storiesCollection.document(storyId).get().await()
                    .toObject(Story::class.java)?.let { story ->
                        storiesCollection.document(storyId)
                            .update("viewCount", story.viewCount + 1)
                            .await()
                    }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get views for a specific story
     */
    suspend fun getStoryViews(storyId: String): Result<List<StoryView>> {
        return try {
            val snapshot = storyViewsCollection
                .whereEqualTo("storyId", storyId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()

            val views = snapshot.documents.mapNotNull { doc ->
                doc.toObject(StoryView::class.java)
            }

            Result.success(views)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Delete a story (soft delete by marking as inactive)
     */
    suspend fun deleteStory(storyId: String): Result<Unit> {
        return try {
            val userId = auth.currentUser?.uid
                ?: return Result.failure(Exception("Not authenticated"))

            // Verify ownership
            val story = storiesCollection.document(storyId).get().await()
                .toObject(Story::class.java)
                ?: return Result.failure(Exception("Story not found"))

            if (story.userId != userId) {
                return Result.failure(Exception("Not authorized to delete this story"))
            }

            // Mark as inactive
            storiesCollection.document(storyId)
                .update("isActive", false)
                .await()

            // Optionally delete the image from storage
            if (story.imageUrl.isNotEmpty()) {
                storageManager.deleteImage(story.imageUrl)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Clean up expired stories (should be called periodically)
     */
    suspend fun cleanupExpiredStories(): Result<Int> {
        return try {
            val currentTime = System.currentTimeMillis()

            val snapshot = storiesCollection
                .whereLessThan("expiresAt", currentTime)
                .whereEqualTo("isActive", true)
                .get()
                .await()

            var deletedCount = 0
            for (document in snapshot.documents) {
                document.reference.update("isActive", false).await()
                deletedCount++

                // Optionally delete images from storage
                document.toObject(Story::class.java)?.let { story ->
                    if (story.imageUrl.isNotEmpty()) {
                        storageManager.deleteImage(story.imageUrl)
                    }
                }
            }

            Result.success(deletedCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Check if user has viewed a story
     */
    suspend fun hasViewedStory(storyId: String): Boolean {
        return try {
            val userId = auth.currentUser?.uid ?: return false
            val viewId = "${userId}_${storyId}"
            val document = storyViewsCollection.document(viewId).get().await()
            document.exists()
        } catch (_: Exception) {
            false
        }
    }
}
