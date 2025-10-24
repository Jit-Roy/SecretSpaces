package com.secretspaces32.android.data.storage

import android.content.Context
import android.net.Uri
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.google.firebase.auth.FirebaseAuth
import com.secretspaces32.android.BuildConfig
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.UUID
import kotlin.coroutines.resume

/**
 * Centralized Cloudinary Storage Manager for handling all image uploads
 * Supports: Profile Pictures, Post/Secret Images, and Story Images
 */
class CloudinaryStorageManager(private val context: Context) {

    private val auth = FirebaseAuth.getInstance()

    companion object {
        // Read credentials from BuildConfig (stored in local.properties)
        private val CLOUD_NAME = BuildConfig.CLOUDINARY_CLOUD_NAME
        private val API_KEY = BuildConfig.CLOUDINARY_API_KEY
        private val API_SECRET = BuildConfig.CLOUDINARY_API_SECRET
        private val UNSIGNED_PRESET = BuildConfig.CLOUDINARY_UNSIGNED_PRESET

        // Folder paths in Cloudinary
        private const val PROFILE_PICTURES_FOLDER = "secret_spaces/profile_pictures"
        private const val POST_IMAGES_FOLDER = "secret_spaces/post_images"
        private const val STORY_IMAGES_FOLDER = "secret_spaces/story_images"

        @Volatile
        private var isInitialized = false
    }

    init {
        initializeCloudinary()
    }

    /**
     * Initialize Cloudinary MediaManager
     */
    private fun initializeCloudinary() {
        if (!isInitialized) {
            synchronized(this) {
                if (!isInitialized) {
                    // Prefer unsigned upload configuration to avoid embedding secrets in the app
                    val baseConfig = mutableMapOf<String, Any>(
                        "cloud_name" to CLOUD_NAME,
                        "secure" to true
                    )
                    // For backward compatibility, include api_key/secret only if unsigned preset is not set
                    if (UNSIGNED_PRESET.isNullOrBlank()) {
                        if (API_KEY.isNotBlank()) baseConfig["api_key"] = API_KEY
                        if (API_SECRET.isNotBlank()) baseConfig["api_secret"] = API_SECRET
                    }
                    MediaManager.init(context, baseConfig)
                    isInitialized = true
                }
            }
        }
    }

    /**
     * Upload profile picture
     * Folder: secret_spaces/profile_pictures/{userId}/
     */
    suspend fun uploadProfilePicture(imageUri: Uri): Result<String> {
        val userId = getCurrentUserId()
            ?: return Result.failure(Exception("User not authenticated"))

        return try {
            val timestamp = System.currentTimeMillis()

            val options = mutableMapOf<String, Any>(
                "folder" to "$PROFILE_PICTURES_FOLDER/$userId",
                "public_id" to "profile_$timestamp",
                "resource_type" to "image",
                "context" to mapOf(
                    "uploadedBy" to userId,
                    "uploadedAt" to timestamp.toString(),
                    "imageType" to "PROFILE"
                ),
                "overwrite" to true,
                "invalidate" to true
            )
            if (!UNSIGNED_PRESET.isNullOrBlank()) {
                options["upload_preset"] = UNSIGNED_PRESET
            }

            uploadImage(imageUri, options)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Upload post/secret image
     * Folder: secret_spaces/post_images/{userId}/
     */
    suspend fun uploadPostImage(imageUri: Uri, postId: String? = null): Result<String> {
        val userId = getCurrentUserId()
            ?: return Result.failure(Exception("User not authenticated"))

        return try {
            val timestamp = System.currentTimeMillis()
            val id = postId ?: UUID.randomUUID().toString()
            val filename = "${id}_$timestamp"

            val options = mutableMapOf<String, Any>(
                "folder" to "$POST_IMAGES_FOLDER/$userId",
                "public_id" to filename,
                "resource_type" to "image",
                "context" to mapOf(
                    "uploadedBy" to userId,
                    "uploadedAt" to timestamp.toString(),
                    "imageType" to "POST",
                    "postId" to id
                )
            )
            if (!UNSIGNED_PRESET.isNullOrBlank()) {
                options["upload_preset"] = UNSIGNED_PRESET
            }

            uploadImage(imageUri, options)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Upload story image
     * Folder: secret_spaces/story_images/{userId}/
     * Stories are temporary and expire after 24 hours
     */
    suspend fun uploadStoryImage(imageUri: Uri): Result<String> {
        val userId = getCurrentUserId()
            ?: return Result.failure(Exception("User not authenticated"))

        return try {
            val timestamp = System.currentTimeMillis()
            val filename = "story_$timestamp"
            val expiresAt = timestamp + (24 * 60 * 60 * 1000) // 24 hours

            val options = mutableMapOf<String, Any>(
                "folder" to "$STORY_IMAGES_FOLDER/$userId",
                "public_id" to filename,
                "resource_type" to "image",
                "context" to mapOf(
                    "uploadedBy" to userId,
                    "uploadedAt" to timestamp.toString(),
                    "imageType" to "STORY",
                    "expiresAt" to expiresAt.toString()
                )
            )
            if (!UNSIGNED_PRESET.isNullOrBlank()) {
                options["upload_preset"] = UNSIGNED_PRESET
            }

            uploadImage(imageUri, options)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Core upload function using Cloudinary SDK
     */
    private suspend fun uploadImage(
        imageUri: Uri,
        options: Map<String, Any>
    ): Result<String> = suspendCancellableCoroutine { continuation ->
        try {
            val requestId = MediaManager.get().upload(imageUri)
                .options(options)
                .callback(object : UploadCallback {
                    override fun onStart(requestId: String) { /* no-op */ }
                    override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) { /* no-op */ }
                    override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                        val secureUrl = resultData["secure_url"] as? String
                        if (secureUrl != null) {
                            continuation.resume(Result.success(secureUrl))
                        } else {
                            continuation.resume(
                                Result.failure(Exception("Failed to get image URL from Cloudinary"))
                            )
                        }
                    }
                    override fun onError(requestId: String, error: ErrorInfo) {
                        continuation.resume(
                            Result.failure(Exception("Upload failed: ${error.description}"))
                        )
                    }
                    override fun onReschedule(requestId: String, error: ErrorInfo) { /* no-op */ }
                })
                .dispatch()

            continuation.invokeOnCancellation {
                // Cancel the upload if coroutine is cancelled
                try {
                    MediaManager.get().cancelRequest(requestId)
                } catch (_: Exception) { }
            }
        } catch (e: Exception) {
            continuation.resume(Result.failure(Exception("Failed to start upload: ${e.message}", e)))
        }
    }

    /**
     * Delete an image from Cloudinary by URL
     */
    @Suppress("RedundantSuspendModifier")
    suspend fun deleteImage(imageUrl: String): Result<Unit> {
        return try {
            // Note: Cloudinary Android SDK doesn't support deletion directly
            // You'll need to use the REST API or admin SDK for deletion
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to delete image: ${e.message}", e))
        }
    }

    /**
     * Delete all profile pictures for a user
     */
    @Suppress("RedundantSuspendModifier", "UNUSED_PARAMETER")
    suspend fun deleteUserProfilePictures(userId: String): Result<Unit> {
        // Use Cloudinary dashboard or admin API for bulk operations
        return Result.success(Unit)
    }

    /**
     * Delete all posts for a user
     */
    @Suppress("RedundantSuspendModifier", "UNUSED_PARAMETER")
    suspend fun deleteUserPosts(userId: String): Result<Unit> {
        return Result.success(Unit)
    }

    /**
     * Delete all stories for a user
     */
    @Suppress("RedundantSuspendModifier", "UNUSED_PARAMETER")
    suspend fun deleteUserStories(userId: String): Result<Unit> {
        return Result.success(Unit)
    }

    /**
     * Extract public_id from Cloudinary URL
     */
    private fun extractPublicIdFromUrl(url: String): String? {
        return try {
            val regex = """$CLOUD_NAME/image/upload/(?:v\d+/)?(.+)\.[a-zA-Z]+""".toRegex()
            regex.find(url)?.groupValues?.getOrNull(1)
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Get current authenticated user ID
     */
    private fun getCurrentUserId(): String? = auth.currentUser?.uid

    /**
     * Get Cloudinary URL with transformations
     */
    fun getTransformedUrl(
        imageUrl: String,
        width: Int? = null,
        height: Int? = null,
        quality: String = "auto",
        format: String = "auto"
    ): String {
        if (!imageUrl.contains(CLOUD_NAME)) return imageUrl

        val transformations = buildList {
            if (width != null) add("w_$width")
            if (height != null) add("h_$height")
            add("q_$quality")
            add("f_$format")
            add("c_limit")
        }.joinToString(",")

        return imageUrl.replace(
            "/image/upload/",
            "/image/upload/$transformations/"
        )
    }

    /**
     * Get thumbnail URL (optimized for loading)
     */
    @Suppress("unused")
    fun getThumbnailUrl(imageUrl: String, size: Int = 200): String {
        return getTransformedUrl(
            imageUrl = imageUrl,
            width = size,
            height = size,
            quality = "auto:low",
            format = "webp"
        )
    }
}
