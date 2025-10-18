package com.secretspaces32.android.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.secretspaces32.android.data.model.Secret
import com.secretspaces32.android.data.model.User
import com.secretspaces32.android.ui.components.*
import android.location.Location

@Composable
fun MainScreenContainer(
    currentLocation: Location?,
    nearbySecrets: List<Secret>,
    currentUser: User?,
    mySecrets: List<Secret>,
    isLoading: Boolean,
    onSecretClick: (Secret) -> Unit,
    onDropSecretClick: () -> Unit,
    onSignOut: () -> Unit,
    onUpdateProfile: (String, String, android.net.Uri?) -> Unit,
    onLikeClick: (Secret) -> Unit = {},
    onLoadMySecrets: () -> Unit = {},
    onLocationPermissionGranted: () -> Unit = {},
    onPostSecret: ((String, android.net.Uri?, Boolean, String?, String?, String?, String) -> Unit)? = null,
    cacheDir: java.io.File? = null,
    myStories: List<com.secretspaces32.android.data.model.Story> = emptyList(),
    onViewMyStory: () -> Unit = {},
    onLoadMyStories: () -> Unit = {},
    onUserProfileClick: (String) -> Unit = {},
    onCreateStory: (android.net.Uri?, String) -> Unit = { _, _ -> },
    feedScrollState: LazyListState? = null // Accept from parent
) {
    var currentDestination by rememberSaveable { mutableStateOf(NavDestination.HOME) }
    var focusedSecret by remember { mutableStateOf<Secret?>(null) }
    var selectedImages by rememberSaveable { mutableStateOf<List<android.net.Uri>>(emptyList()) }

    // Use provided scroll state or create fallback (but parent should always provide it now)
    val actualFeedScrollState = feedScrollState ?: rememberSaveable(saver = LazyListState.Saver) {
        LazyListState()
    }

    // Load user secrets when profile is accessed
    LaunchedEffect(currentDestination) {
        if (currentDestination == NavDestination.PROFILE) {
            onLoadMySecrets()
        }
    }

    // Load user stories when home is displayed
    LaunchedEffect(Unit) {
        onLoadMyStories()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Content based on current destination
        when (currentDestination) {
            NavDestination.HOME -> {
                FeedScreen(
                    nearbySecrets = nearbySecrets,
                    isLoading = isLoading,
                    currentUser = currentUser,
                    onSecretClick = onSecretClick,
                    onDropSecretClick = { currentDestination = NavDestination.CREATE },
                    onProfileClick = { currentDestination = NavDestination.PROFILE },
                    onLikeClick = onLikeClick,
                    onMapClick = { secret ->
                        focusedSecret = secret
                        currentDestination = NavDestination.MAP
                    },
                    onAddStoryClick = {
                        // Navigate to create story screen
                        currentDestination = NavDestination.CREATE_STORY
                    },
                    onStoryClick = { story ->
                        println("DEBUG: onStoryClick called for story: ${story.username}, isYourStory: ${story.isYourStory}")
                        if (story.isYourStory) {
                            // Always try to view your stories
                            // The onViewMyStory will load them and navigate
                            println("DEBUG: Calling onViewMyStory")
                            onViewMyStory()
                        }
                    },
                    onUserProfileClick = { userId ->
                        // Navigate to the user's profile
                        onUserProfileClick(userId)
                    },
                    scrollState = actualFeedScrollState // Pass the scroll state to FeedScreen
                )
            }

            NavDestination.MAP -> {
                MapScreen(
                    currentLocation = currentLocation,
                    nearbySecrets = nearbySecrets,
                    onSecretClick = onSecretClick,
                    onLocationPermissionGranted = onLocationPermissionGranted
                )
            }

            NavDestination.CREATE -> {
                DropSecretScreen(
                    isLoading = isLoading,
                    onNext = { imageUris ->
                        selectedImages = imageUris
                        currentDestination = NavDestination.CROP_IMAGES
                    },
                    onBack = {
                        currentDestination = NavDestination.HOME
                    },
                    cacheDir = cacheDir,
                    currentUser = currentUser
                )
            }

            NavDestination.CROP_IMAGES -> {
                ImageCropScreen(
                    selectedImages = selectedImages,
                    onImagesCropped = { croppedUris ->
                        selectedImages = croppedUris
                        currentDestination = NavDestination.ADD_DESCRIPTION
                    },
                    onBack = {
                        currentDestination = NavDestination.CREATE
                    },
                    cacheDir = cacheDir
                )
            }

            NavDestination.ADD_DESCRIPTION -> {
                AddDescriptionScreen(
                    selectedImages = selectedImages,
                    isLoading = isLoading,
                    onPostSecret = { text, imageUris ->
                        // Post the first image with the text (keeping backward compatibility)
                        // You may want to update your backend to support multiple images
                        onPostSecret?.invoke(text, imageUris.firstOrNull(), false, null, null, null, "Secret")
                        selectedImages = emptyList()
                        currentDestination = NavDestination.HOME
                    },
                    onBack = {
                        currentDestination = NavDestination.CROP_IMAGES
                    },
                    currentUser = currentUser
                )
            }

            NavDestination.CREATE_STORY -> {
                CreateStoryScreen(
                    isLoading = isLoading,
                    onCreateStory = { imageUri, text ->
                        onCreateStory(imageUri, text)
                        currentDestination = NavDestination.HOME
                    },
                    onBack = {
                        currentDestination = NavDestination.HOME
                    },
                    cacheDir = cacheDir
                )
            }

            NavDestination.TRENDS -> {
                TrendsScreen(
                    onProfileClick = { currentDestination = NavDestination.PROFILE }
                )
            }

            NavDestination.PROFILE -> {
                ProfileScreen(
                    user = currentUser,
                    mySecrets = mySecrets,
                    onSignOut = onSignOut,
                    onUpdateProfile = onUpdateProfile,
                    onMySecretsClick = {
                        // TODO: Navigate to MySecrets if needed
                    },
                    onSettingsClick = {
                        currentDestination = NavDestination.SETTINGS
                    },
                    onLikeClick = onLikeClick,
                    onCommentClick = { secret ->
                        // Handle comment click
                    },
                    onMapClick = { secret ->
                        focusedSecret = secret
                        currentDestination = NavDestination.MAP
                    },
                    onSecretClick = onSecretClick,
                    isLoading = isLoading
                )
            }

            NavDestination.SETTINGS -> {
                SettingsScreen(
                    user = currentUser,
                    onSignOut = onSignOut,
                    onUpdateProfile = onUpdateProfile,
                    onBackClick = {
                        currentDestination = NavDestination.PROFILE
                    },
                    isLoading = isLoading
                )
            }
        }

        // Bottom Navigation Bar - Always visible except on Settings, Create, Crop Images, Add Description, and Create Story screens
        if (currentDestination != NavDestination.SETTINGS &&
            currentDestination != NavDestination.CREATE &&
            currentDestination != NavDestination.CROP_IMAGES &&
            currentDestination != NavDestination.ADD_DESCRIPTION &&
            currentDestination != NavDestination.CREATE_STORY) {
            BottomNavigationBar(
                currentDestination = currentDestination,
                onNavigate = { destination ->
                    when (destination) {
                        NavDestination.HOME -> {
                            currentDestination = NavDestination.HOME
                        }
                        NavDestination.MAP -> {
                            currentDestination = NavDestination.MAP
                        }
                        NavDestination.CREATE -> {
                            currentDestination = NavDestination.CREATE
                        }
                        NavDestination.CREATE_STORY -> {
                            currentDestination = NavDestination.CREATE_STORY
                        }
                        NavDestination.TRENDS -> {
                            currentDestination = NavDestination.TRENDS
                        }
                        NavDestination.PROFILE -> {
                            currentDestination = NavDestination.PROFILE
                        }
                        NavDestination.SETTINGS -> {
                            currentDestination = NavDestination.SETTINGS
                        }
                        NavDestination.ADD_DESCRIPTION, NavDestination.CROP_IMAGES -> {
                            // Don't allow direct navigation to ADD_DESCRIPTION or CROP_IMAGES
                        }
                    }
                },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}
