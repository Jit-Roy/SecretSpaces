package com.secretspaces32.android.viewmodel

import android.content.Context
import android.location.Location
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.secretspaces32.android.data.model.Comment
import com.secretspaces32.android.data.model.Like
import com.secretspaces32.android.data.model.Secret
import com.secretspaces32.android.data.model.User
import com.secretspaces32.android.data.model.UpdateUserRequest
import com.secretspaces32.android.data.repository.AuthRepository
import com.secretspaces32.android.data.repository.SecretRepository
import com.secretspaces32.android.data.repository.StoryRepository
import com.secretspaces32.android.data.repository.UserRepository
import com.secretspaces32.android.utils.LocationHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AppUiState(
    val isAuthenticated: Boolean = false,
    val currentUser: User? = null,
    val currentLocation: Location? = null,
    val secrets: List<Secret> = emptyList(),
    val mySecrets: List<Secret> = emptyList(),
    val selectedSecret: Secret? = null,
    val selectedSecretComments: List<Comment> = emptyList(),
    val selectedSecretLikes: List<Like> = emptyList(),
    val isLoading: Boolean = false,
    val isEmailAuthLoading: Boolean = false,
    val isGoogleAuthLoading: Boolean = false,
    val errorMessage: String? = null,
    val myStories: List<com.secretspaces32.android.data.model.Story> = emptyList(),
    val selectedUserStories: List<com.secretspaces32.android.data.model.Story> = emptyList(),
    val currentStoryIndex: Int = 0
)

class MainViewModel(
    private val context: Context
) : ViewModel() {

    private val authRepository = AuthRepository()
    private val userRepository = UserRepository(context)
    private val secretRepository = SecretRepository(context)
    private val storyRepository = StoryRepository(context)
    private val locationHelper = LocationHelper(context)

    private val _uiState = MutableStateFlow(AppUiState())
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    init {
        checkAuthState()
    }

    private fun checkAuthState() {
        viewModelScope.launch {
            authRepository.currentUser.collect { firebaseUser ->
                if (firebaseUser != null) {
                    loadCurrentUser(firebaseUser.uid)
                } else {
                    _uiState.value = _uiState.value.copy(
                        isAuthenticated = false,
                        currentUser = null
                    )
                }
            }
        }
    }

    private suspend fun loadCurrentUser(userId: String) {
        val result = userRepository.getUser(userId)
        result.onSuccess { user ->
            _uiState.value = _uiState.value.copy(
                isAuthenticated = true,
                currentUser = user
            )
        }.onFailure {
            _uiState.value = _uiState.value.copy(
                isAuthenticated = true,
                currentUser = null,
                errorMessage = "Failed to load user profile"
            )
        }
    }

    fun signUp(email: String, password: String, username: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isEmailAuthLoading = true)

            val authResult = authRepository.signUp(email, password, username)
            authResult.onSuccess { firebaseUser ->
                val userResult = userRepository.createUser(
                    userId = firebaseUser.uid,
                    email = email,
                    username = username
                )

                userResult.onSuccess { user ->
                    _uiState.value = _uiState.value.copy(
                        isAuthenticated = true,
                        currentUser = user,
                        isEmailAuthLoading = false
                    )
                }.onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isEmailAuthLoading = false,
                        errorMessage = "Failed to create user profile: ${e.message}"
                    )
                }
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    isEmailAuthLoading = false,
                    errorMessage = "Sign up failed: ${e.message}"
                )
            }
        }
    }

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isEmailAuthLoading = true)

            val result = authRepository.signIn(email, password)
            result.onSuccess {
                _uiState.value = _uiState.value.copy(isEmailAuthLoading = false)
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    isEmailAuthLoading = false,
                    errorMessage = "Sign in failed: ${e.message}"
                )
            }
        }
    }

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isGoogleAuthLoading = true)

            val authResult = authRepository.signInWithGoogle(idToken)
            authResult.onSuccess { firebaseUser ->
                // Check if user exists in Firestore
                val userResult = userRepository.getUser(firebaseUser.uid)

                userResult.onSuccess { user ->
                    // User already exists
                    _uiState.value = _uiState.value.copy(
                        isAuthenticated = true,
                        currentUser = user,
                        isGoogleAuthLoading = false
                    )
                }.onFailure {
                    // Create new user profile
                    val createResult = userRepository.createUser(
                        userId = firebaseUser.uid,
                        email = firebaseUser.email ?: "",
                        username = firebaseUser.displayName ?: "User"
                    )

                    createResult.onSuccess { user ->
                        _uiState.value = _uiState.value.copy(
                            isAuthenticated = true,
                            currentUser = user,
                            isGoogleAuthLoading = false
                        )
                    }.onFailure { e ->
                        _uiState.value = _uiState.value.copy(
                            isGoogleAuthLoading = false,
                            errorMessage = "Failed to create user profile: ${e.message}"
                        )
                    }
                }
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    isGoogleAuthLoading = false,
                    errorMessage = "Google sign in failed: ${e.message}"
                )
            }
        }
    }

    fun signOut() {
        authRepository.signOut()
        _uiState.value = AppUiState()
    }

    fun updateProfile(username: String, bio: String, imageUri: Uri?) {
        viewModelScope.launch {
            val userId = _uiState.value.currentUser?.id ?: return@launch
            _uiState.value = _uiState.value.copy(isLoading = true)

            try {
                var profilePictureUrl: String? = null

                if (imageUri != null) {
                    val uploadResult = userRepository.uploadProfilePicture(imageUri)
                    uploadResult.onSuccess { url ->
                        profilePictureUrl = url
                    }
                }

                val updateRequest = UpdateUserRequest(
                    username = username,
                    bio = bio,
                    profilePictureUrl = profilePictureUrl
                )

                val result = userRepository.updateUser(userId, updateRequest)
                result.onSuccess {
                    // Update profile picture in all existing secrets and comments
                    if (profilePictureUrl != null) {
                        secretRepository.updateUserProfilePictureInPosts(userId, profilePictureUrl)
                    }

                    loadCurrentUser(userId)
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }.onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Failed to update profile: ${e.message}"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Error: ${e.message}"
                )
            }
        }
    }

    fun updateLocation() {
        viewModelScope.launch {
            try {
                println("DEBUG: Starting location update...")

                if (!locationHelper.hasLocationPermission()) {
                    println("DEBUG: Location permission not granted - skipping location update")
                    // Don't set any location - let the UI handle permission request
                    return@launch
                }

                if (!locationHelper.isLocationEnabled()) {
                    println("DEBUG: Location services not enabled - skipping location update")
                    // Don't set any location - let the UI handle this
                    return@launch
                }

                println("DEBUG: Fetching location...")
                val location = locationHelper.getCurrentLocation()

                if (location == null) {
                    println("DEBUG: Location is null - could not fetch location")
                    return@launch
                }

                println("DEBUG: ✅ Location obtained - Lat: ${location.latitude}, Lng: ${location.longitude}")
                _uiState.value = _uiState.value.copy(currentLocation = location)

                // Fetch nearby secrets with the obtained location
                fetchNearbySecrets(location.latitude, location.longitude)

            } catch (e: Exception) {
                println("DEBUG: ❌ Location error - ${e.message}")
                e.printStackTrace()
            }
        }
    }

    fun fetchNearbySecrets(latitude: Double, longitude: Double) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val result = secretRepository.getNearbySecrets(latitude, longitude)

            result.onSuccess { secrets ->
                val secretsWithDistance = secrets.map { secret ->
                    val distance = LocationHelper.calculateDistance(
                        latitude, longitude,
                        secret.latitude, secret.longitude
                    )
                    secret.copy(distance = distance)
                }

                _uiState.value = _uiState.value.copy(
                    secrets = secretsWithDistance,
                    isLoading = false
                )
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to fetch secrets: ${e.message}"
                )
            }
        }
    }

    fun createSecret(text: String, imageUri: Uri?, isAnonymous: Boolean, mood: String? = null, category: String? = null, hashtags: String? = null) {
        viewModelScope.launch {
            val location = _uiState.value.currentLocation
            val user = _uiState.value.currentUser

            if (location == null) {
                _uiState.value = _uiState.value.copy(errorMessage = "Location not available")
                return@launch
            }

            if (user == null) {
                _uiState.value = _uiState.value.copy(errorMessage = "User not authenticated")
                return@launch
            }

            _uiState.value = _uiState.value.copy(isLoading = true)

            val result = secretRepository.createSecret(
                text = text,
                imageUri = imageUri,
                latitude = location.latitude,
                longitude = location.longitude,
                username = user.username,
                userProfilePicture = user.profilePictureUrl,
                isAnonymous = isAnonymous,
                mood = mood,
                category = category,
                hashtags = hashtags
            )

            result.onSuccess {
                _uiState.value = _uiState.value.copy(isLoading = false)
                fetchNearbySecrets(location.latitude, location.longitude)
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to post secret: ${e.message}"
                )
            }
        }
    }

    fun createStory(imageUri: Uri?, caption: String?) {
        viewModelScope.launch {
            val user = _uiState.value.currentUser

            if (user == null) {
                _uiState.value = _uiState.value.copy(errorMessage = "User not authenticated")
                return@launch
            }

            if (imageUri == null) {
                _uiState.value = _uiState.value.copy(errorMessage = "Story requires an image")
                return@launch
            }

            _uiState.value = _uiState.value.copy(isLoading = true)

            val result = storyRepository.createStory(
                imageUri = imageUri,
                caption = caption,
                username = user.username,
                userProfilePicture = user.profilePictureUrl
            )

            result.onSuccess {
                _uiState.value = _uiState.value.copy(isLoading = false)
                // Reload user's stories after creating a new one
                loadMyStories()
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to post story: ${e.message}"
                )
            }
        }
    }

    fun loadMyStories() {
        viewModelScope.launch {
            val userId = _uiState.value.currentUser?.id ?: return@launch

            val result = storyRepository.getUserStories(userId)
            result.onSuccess { stories ->
                _uiState.value = _uiState.value.copy(myStories = stories)
            }.onFailure { e ->
                println("DEBUG: Failed to load stories: ${e.message}")
            }
        }
    }

    fun loadUserStories(userId: String) {
        viewModelScope.launch {
            println("DEBUG: loadUserStories called for userId: $userId")
            val result = storyRepository.getUserStories(userId)
            result.onSuccess { stories ->
                println("DEBUG: Successfully loaded ${stories.size} stories")
                stories.forEach { story ->
                    println("DEBUG: Story - id: ${story.id}, imageUrl: ${story.imageUrl}")
                }
                _uiState.value = _uiState.value.copy(
                    selectedUserStories = stories,
                    currentStoryIndex = 0
                )
                println("DEBUG: UI state updated with stories")
            }.onFailure { e ->
                println("DEBUG: Failed to load stories: ${e.message}")
                e.printStackTrace()
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Failed to load stories: ${e.message}"
                )
            }
        }
    }

    fun nextStory() {
        val currentIndex = _uiState.value.currentStoryIndex
        val storiesCount = _uiState.value.selectedUserStories.size
        if (currentIndex < storiesCount - 1) {
            _uiState.value = _uiState.value.copy(currentStoryIndex = currentIndex + 1)
        }
    }

    fun previousStory() {
        val currentIndex = _uiState.value.currentStoryIndex
        if (currentIndex > 0) {
            _uiState.value = _uiState.value.copy(currentStoryIndex = currentIndex - 1)
        }
    }

    fun clearSelectedStories() {
        _uiState.value = _uiState.value.copy(
            selectedUserStories = emptyList(),
            currentStoryIndex = 0
        )
    }

    fun toggleLike(secret: Secret) {
        viewModelScope.launch {
            val user = _uiState.value.currentUser ?: return@launch

            val result = secretRepository.toggleLike(secret.id, user.username)
            result.onSuccess { isLiked ->
                // Update the secret in the list
                val updatedSecrets = _uiState.value.secrets.map { s ->
                    if (s.id == secret.id) {
                        s.copy(
                            isLikedByCurrentUser = isLiked,
                            likeCount = if (isLiked) s.likeCount + 1 else maxOf(0, s.likeCount - 1)
                        )
                    } else s
                }
                _uiState.value = _uiState.value.copy(secrets = updatedSecrets)

                // Update selected secret if it's the same
                if (_uiState.value.selectedSecret?.id == secret.id) {
                    loadSecretDetails(secret.id)
                }
            }
        }
    }

    fun selectSecret(secret: Secret?) {
        _uiState.value = _uiState.value.copy(selectedSecret = secret)
        secret?.let { loadSecretDetails(it.id) }
    }

    fun loadSecretDetails(secretId: String) {
        viewModelScope.launch {
            val commentsResult = secretRepository.getComments(secretId)
            val likesResult = secretRepository.getLikes(secretId)

            commentsResult.onSuccess { comments ->
                _uiState.value = _uiState.value.copy(selectedSecretComments = comments)
            }

            likesResult.onSuccess { likes ->
                _uiState.value = _uiState.value.copy(selectedSecretLikes = likes)
            }
        }
    }

    fun addComment(secretId: String, text: String) {
        viewModelScope.launch {
            val user = _uiState.value.currentUser ?: return@launch

            println("DEBUG: Adding comment for secret $secretId")

            val result = secretRepository.addComment(
                secretId = secretId,
                text = text,
                username = user.username,
                userProfilePicture = user.profilePictureUrl
            )

            result.onSuccess { newComment ->
                println("DEBUG: Comment added successfully: ${newComment.text}")

                // Add small delay to ensure Firebase propagates the comment
                kotlinx.coroutines.delay(800)

                println("DEBUG: Reloading secret details...")
                // Reload comments and likes
                val commentsResult = secretRepository.getComments(secretId)
                val likesResult = secretRepository.getLikes(secretId)

                commentsResult.onSuccess { comments ->
                    println("DEBUG: Loaded ${comments.size} comments")
                    _uiState.value = _uiState.value.copy(selectedSecretComments = comments)
                }

                likesResult.onSuccess { likes ->
                    _uiState.value = _uiState.value.copy(selectedSecretLikes = likes)
                }

                // Update comment count in the secrets list
                val updatedSecrets = _uiState.value.secrets.map { s ->
                    if (s.id == secretId) {
                        s.copy(commentCount = s.commentCount + 1)
                    } else s
                }

                // Update the selected secret with new comment count
                val updatedSelectedSecret = _uiState.value.selectedSecret?.let { secret ->
                    if (secret.id == secretId) {
                        secret.copy(commentCount = secret.commentCount + 1)
                    } else secret
                }

                _uiState.value = _uiState.value.copy(
                    secrets = updatedSecrets,
                    selectedSecret = updatedSelectedSecret
                )

                println("DEBUG: UI state updated with ${_uiState.value.selectedSecretComments.size} comments")
            }.onFailure { e ->
                println("DEBUG: Failed to add comment: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Failed to add comment: ${e.message}"
                )
            }
        }
    }

    fun loadMySecrets() {
        viewModelScope.launch {
            val userId = _uiState.value.currentUser?.id ?: return@launch
            _uiState.value = _uiState.value.copy(isLoading = true)

            val result = secretRepository.getUserSecrets(userId)
            result.onSuccess { secrets ->
                _uiState.value = _uiState.value.copy(
                    mySecrets = secrets,
                    isLoading = false
                )
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to load your secrets: ${e.message}"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}
