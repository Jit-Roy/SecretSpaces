package com.secretspaces32.android.data.repository

import android.content.Context
import android.net.Uri
import com.secretspaces32.android.data.model.User
import com.secretspaces32.android.data.model.UpdateUserRequest
import com.secretspaces32.android.data.storage.CloudinaryStorageManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class UserRepository(context: Context) {
    private val firestore = FirebaseFirestore.getInstance()
    private val storageManager = CloudinaryStorageManager(context)
    private val auth = FirebaseAuth.getInstance()

    private val usersCollection = firestore.collection("users")
    private val followsCollection = firestore.collection("follows")

    suspend fun createUser(userId: String, email: String, username: String): Result<User> {
        return try {
            val user = User(
                id = userId,
                email = email,
                username = username,
                bio = "",
                followersCount = 0,
                followingCount = 0,
                createdAt = System.currentTimeMillis()
            )

            usersCollection.document(userId).set(user).await()
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUser(userId: String): Result<User> {
        return try {
            val document = usersCollection.document(userId).get().await()
            val user = document.toObject(User::class.java)

            if (user != null) {
                // Get real-time follower and following counts
                val followersCount = getFollowersCount(userId)
                val followingCount = getFollowingCount(userId)

                val updatedUser = user.copy(
                    followersCount = followersCount,
                    followingCount = followingCount
                )

                Result.success(updatedUser)
            } else {
                Result.failure(Exception("User not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateUser(userId: String, request: UpdateUserRequest): Result<Unit> {
        return try {
            val updates = mutableMapOf<String, Any>()
            request.username?.let { updates["username"] = it }
            request.bio?.let { updates["bio"] = it }
            request.profilePictureUrl?.let { updates["profilePictureUrl"] = it }

            usersCollection.document(userId).update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun uploadProfilePicture(imageUri: Uri): Result<String> {
        return storageManager.uploadProfilePicture(imageUri)
    }

    fun getCurrentUserId(): String? = auth.currentUser?.uid

    suspend fun getUsername(): String? {
        val userId = getCurrentUserId() ?: return null
        return try {
            val document = usersCollection.document(userId).get().await()
            document.getString("username")
        } catch (_: Exception) {
            null
        }
    }

    suspend fun getUserProfilePicture(): String? {
        val userId = getCurrentUserId() ?: return null
        return try {
            val document = usersCollection.document(userId).get().await()
            document.getString("profilePictureUrl")
        } catch (_: Exception) {
            null
        }
    }

    // Get the count of users following this user (followers)
    suspend fun getFollowersCount(userId: String): Int {
        return try {
            val snapshot = followsCollection
                .whereEqualTo("followedUserId", userId)
                .get()
                .await()
            snapshot.size()
        } catch (_: Exception) {
            0
        }
    }

    // Get the count of users this user is following (following)
    suspend fun getFollowingCount(userId: String): Int {
        return try {
            val snapshot = followsCollection
                .whereEqualTo("followerUserId", userId)
                .get()
                .await()
            snapshot.size()
        } catch (_: Exception) {
            0
        }
    }

    // Follow a user
    suspend fun followUser(currentUserId: String, targetUserId: String): Result<Unit> {
        return try {
            val followId = "${currentUserId}_${targetUserId}"
            val followData = hashMapOf(
                "followerUserId" to currentUserId,
                "followedUserId" to targetUserId,
                "timestamp" to System.currentTimeMillis()
            )

            followsCollection.document(followId).set(followData).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Unfollow a user
    suspend fun unfollowUser(currentUserId: String, targetUserId: String): Result<Unit> {
        return try {
            val followId = "${currentUserId}_${targetUserId}"
            followsCollection.document(followId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Check if current user is following target user
    suspend fun isFollowing(currentUserId: String, targetUserId: String): Boolean {
        return try {
            val followId = "${currentUserId}_${targetUserId}"
            val document = followsCollection.document(followId).get().await()
            document.exists()
        } catch (_: Exception) {
            false
        }
    }
}
