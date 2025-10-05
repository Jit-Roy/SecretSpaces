package com.secretspaces32.android.data.repository

import android.net.Uri
import com.secretspaces32.android.data.model.User
import com.secretspaces32.android.data.model.UpdateUserRequest
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.util.UUID

class FirebaseUserRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val usersCollection = firestore.collection("users")

    suspend fun createUser(userId: String, email: String, username: String): Result<User> {
        return try {
            val user = User(
                id = userId,
                email = email,
                username = username,
                bio = "",
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
                Result.success(user)
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

    suspend fun uploadProfilePicture(userId: String, imageUri: Uri): Result<String> {
        return try {
            val filename = "profile_${userId}_${UUID.randomUUID()}.jpg"
            val ref = storage.reference.child("profile_pictures/$filename")

            ref.putFile(imageUri).await()
            val downloadUrl = ref.downloadUrl.await()

            Result.success(downloadUrl.toString())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getCurrentUserId(): String? = auth.currentUser?.uid

    suspend fun getUsername(): String? {
        val userId = getCurrentUserId() ?: return null
        return try {
            val document = usersCollection.document(userId).get().await()
            document.getString("username")
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getUserProfilePicture(): String? {
        val userId = getCurrentUserId() ?: return null
        return try {
            val document = usersCollection.document(userId).get().await()
            document.getString("profilePictureUrl")
        } catch (e: Exception) {
            null
        }
    }
}
