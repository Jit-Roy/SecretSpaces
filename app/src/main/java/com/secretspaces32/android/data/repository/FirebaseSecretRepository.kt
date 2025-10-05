package com.secretspaces32.android.data.repository

import android.net.Uri
import com.secretspaces32.android.data.model.Comment
import com.secretspaces32.android.data.model.Like
import com.secretspaces32.android.data.model.Secret
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.util.UUID

class FirebaseSecretRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val secretsCollection = firestore.collection("secrets")
    private val likesCollection = firestore.collection("likes")
    private val commentsCollection = firestore.collection("comments")

    suspend fun createSecret(
        text: String,
        imageUri: Uri?,
        latitude: Double,
        longitude: Double,
        username: String,
        userProfilePicture: String?,
        isAnonymous: Boolean
    ): Result<Secret> {
        return try {
            val userId = auth.currentUser?.uid ?: return Result.failure(Exception("Not authenticated"))

            // Upload image if provided
            val imageUrl = imageUri?.let { uploadSecretImage(it) }?.getOrNull()

            val secretId = UUID.randomUUID().toString()
            val secret = Secret(
                id = secretId,
                text = text,
                imageUrl = imageUrl,
                latitude = latitude,
                longitude = longitude,
                timestamp = System.currentTimeMillis(),
                userId = userId,
                username = if (isAnonymous) "Anonymous" else username,
                userProfilePicture = if (isAnonymous) null else userProfilePicture,
                isAnonymous = isAnonymous,
                likeCount = 0,
                commentCount = 0
            )

            secretsCollection.document(secretId).set(secret).await()
            Result.success(secret)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getNearbySecrets(
        latitude: Double,
        longitude: Double,
        radiusInKm: Double = 5.0
    ): Result<List<Secret>> {
        return try {
            val currentUserId = auth.currentUser?.uid ?: ""

            // Fetch all secrets (in production, use geohash for efficient queries)
            val snapshot = secretsCollection
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(100)
                .get()
                .await()

            val secrets = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Secret::class.java)
            }.filter { secret ->
                // Filter by distance
                val distance = calculateDistance(
                    latitude, longitude,
                    secret.latitude, secret.longitude
                )
                distance <= radiusInKm * 1000 // Convert km to meters
            }.map { secret ->
                // Check if current user liked this secret
                val isLiked = checkIfUserLikedSecret(secret.id, currentUserId)
                secret.copy(isLikedByCurrentUser = isLiked)
            }

            Result.success(secrets)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserSecrets(userId: String): Result<List<Secret>> {
        return try {
            val snapshot = secretsCollection
                .whereEqualTo("userId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()

            val secrets = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Secret::class.java)
            }

            Result.success(secrets)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun toggleLike(secretId: String, username: String): Result<Boolean> {
        return try {
            val userId = auth.currentUser?.uid ?: return Result.failure(Exception("Not authenticated"))

            val likeId = "${userId}_$secretId"
            val likeDoc = likesCollection.document(likeId)
            val likeSnapshot = likeDoc.get().await()

            val isLiked = if (likeSnapshot.exists()) {
                // Unlike
                likeDoc.delete().await()

                // Decrement like count
                secretsCollection.document(secretId).get().await()
                    .toObject(Secret::class.java)?.let { secret ->
                        secretsCollection.document(secretId)
                            .update("likeCount", maxOf(0, secret.likeCount - 1))
                            .await()
                    }
                false
            } else {
                // Like
                val like = Like(
                    id = likeId,
                    secretId = secretId,
                    userId = userId,
                    username = username,
                    timestamp = System.currentTimeMillis()
                )
                likeDoc.set(like).await()

                // Increment like count
                secretsCollection.document(secretId).get().await()
                    .toObject(Secret::class.java)?.let { secret ->
                        secretsCollection.document(secretId)
                            .update("likeCount", secret.likeCount + 1)
                            .await()
                    }
                true
            }

            Result.success(isLiked)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addComment(secretId: String, text: String, username: String, userProfilePicture: String?): Result<Comment> {
        return try {
            val userId = auth.currentUser?.uid ?: return Result.failure(Exception("Not authenticated"))

            val commentId = UUID.randomUUID().toString()
            val comment = Comment(
                id = commentId,
                secretId = secretId,
                userId = userId,
                username = username,
                userProfilePicture = userProfilePicture,
                text = text,
                timestamp = System.currentTimeMillis()
            )

            commentsCollection.document(commentId).set(comment).await()

            // Increment comment count
            secretsCollection.document(secretId).get().await()
                .toObject(Secret::class.java)?.let { secret ->
                    secretsCollection.document(secretId)
                        .update("commentCount", secret.commentCount + 1)
                        .await()
                }

            Result.success(comment)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getComments(secretId: String): Result<List<Comment>> {
        return try {
            val snapshot = commentsCollection
                .whereEqualTo("secretId", secretId)
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .get()
                .await()

            val comments = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Comment::class.java)
            }

            Result.success(comments)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getLikes(secretId: String): Result<List<Like>> {
        return try {
            val snapshot = likesCollection
                .whereEqualTo("secretId", secretId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()

            val likes = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Like::class.java)
            }

            Result.success(likes)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun uploadSecretImage(imageUri: Uri): Result<String> {
        return try {
            val filename = "secret_${UUID.randomUUID()}.jpg"
            val ref = storage.reference.child("secret_images/$filename")

            ref.putFile(imageUri).await()
            val downloadUrl = ref.downloadUrl.await()

            Result.success(downloadUrl.toString())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun checkIfUserLikedSecret(secretId: String, userId: String): Boolean {
        return try {
            val likeId = "${userId}_$secretId"
            val snapshot = likesCollection.document(likeId).get().await()
            snapshot.exists()
        } catch (e: Exception) {
            false
        }
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6371000.0 // meters
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)

        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return earthRadius * c
    }
}
