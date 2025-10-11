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
import com.secretspaces32.android.data.repository.FirebaseSecretRepository
import com.secretspaces32.android.data.repository.FirebaseUserRepository
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
    val errorMessage: String? = null
)

class MainViewModel(
    private val context: Context
) : ViewModel() {

    private val authRepository = AuthRepository()
    private val userRepository = FirebaseUserRepository()
    private val secretRepository = FirebaseSecretRepository()
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
            _uiState.value = _uiState.value.copy(isLoading = true)

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
                        isLoading = false
                    )
                }.onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Failed to create user profile: ${e.message}"
                    )
                }
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Sign up failed: ${e.message}"
                )
            }
        }
    }

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val result = authRepository.signIn(email, password)
            result.onSuccess {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Sign in failed: ${e.message}"
                )
            }
        }
    }

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val authResult = authRepository.signInWithGoogle(idToken)
            authResult.onSuccess { firebaseUser ->
                // Check if user exists in Firestore
                val userResult = userRepository.getUser(firebaseUser.uid)

                userResult.onSuccess { user ->
                    // User already exists
                    _uiState.value = _uiState.value.copy(
                        isAuthenticated = true,
                        currentUser = user,
                        isLoading = false
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
                            isLoading = false
                        )
                    }.onFailure { e ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = "Failed to create user profile: ${e.message}"
                        )
                    }
                }
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
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
                    val uploadResult = userRepository.uploadProfilePicture(userId, imageUri)
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
                    println("DEBUG: Location permission not granted - using default location")
                    // Use a default location (e.g., San Francisco)
                    val defaultLocation = Location("default").apply {
                        latitude = 37.7749
                        longitude = -122.4194
                    }
                    _uiState.value = _uiState.value.copy(currentLocation = defaultLocation)
                    fetchNearbySecrets(defaultLocation.latitude, defaultLocation.longitude)
                    return@launch
                }

                if (!locationHelper.isLocationEnabled()) {
                    println("DEBUG: Location services not enabled - using default location")
                    // Use a default location
                    val defaultLocation = Location("default").apply {
                        latitude = 37.7749
                        longitude = -122.4194
                    }
                    _uiState.value = _uiState.value.copy(currentLocation = defaultLocation)
                    fetchNearbySecrets(defaultLocation.latitude, defaultLocation.longitude)
                    return@launch
                }

                println("DEBUG: Fetching location...")
                val location = locationHelper.getCurrentLocation()

                if (location == null) {
                    println("DEBUG: Location is null - using default location")
                    // Use a default location
                    val defaultLocation = Location("default").apply {
                        latitude = 37.7749
                        longitude = -122.4194
                    }
                    _uiState.value = _uiState.value.copy(currentLocation = defaultLocation)
                    fetchNearbySecrets(defaultLocation.latitude, defaultLocation.longitude)
                    return@launch
                }

                println("DEBUG: ✅ Location obtained - Lat: ${location.latitude}, Lng: ${location.longitude}")
                _uiState.value = _uiState.value.copy(currentLocation = location)

                // Fetch nearby secrets with the obtained location
                fetchNearbySecrets(location.latitude, location.longitude)

            } catch (e: Exception) {
                println("DEBUG: ❌ Location error - ${e.message}, using default location")
                e.printStackTrace()
                // Use default location on error
                val defaultLocation = Location("default").apply {
                    latitude = 37.7749
                    longitude = -122.4194
                }
                _uiState.value = _uiState.value.copy(currentLocation = defaultLocation)
                fetchNearbySecrets(defaultLocation.latitude, defaultLocation.longitude)
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

    fun createSecret(text: String, imageUri: Uri?, isAnonymous: Boolean) {
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
                isAnonymous = isAnonymous
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

            val result = secretRepository.addComment(
                secretId = secretId,
                text = text,
                username = user.username,
                userProfilePicture = user.profilePictureUrl
            )

            result.onSuccess {
                loadSecretDetails(secretId)

                // Update comment count in the list
                val updatedSecrets = _uiState.value.secrets.map { s ->
                    if (s.id == secretId) {
                        s.copy(commentCount = s.commentCount + 1)
                    } else s
                }
                _uiState.value = _uiState.value.copy(secrets = updatedSecrets)
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
