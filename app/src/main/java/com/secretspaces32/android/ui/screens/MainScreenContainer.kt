package com.secretspaces32.android.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
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
    onPostSecret: ((String, android.net.Uri?, Boolean, String?, String?, String?) -> Unit)? = null,
    cacheDir: java.io.File? = null
) {
    var currentDestination by remember { mutableStateOf(NavDestination.HOME) }
    var focusedSecret by remember { mutableStateOf<Secret?>(null) }

    // Load user secrets when profile is accessed
    LaunchedEffect(currentDestination) {
        if (currentDestination == NavDestination.PROFILE) {
            onLoadMySecrets()
        }
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
                    }
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
                    onPostSecret = { text, imageUri, isAnonymous, mood, category, hashtags ->
                        onPostSecret?.invoke(text, imageUri, isAnonymous, mood, category, hashtags)
                        currentDestination = NavDestination.HOME
                    },
                    onBack = {
                        currentDestination = NavDestination.HOME
                    },
                    cacheDir = cacheDir,
                    currentUser = currentUser
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
                    onBackClick = {
                        currentDestination = NavDestination.HOME
                    },
                    isLoading = isLoading
                )
            }
        }

        // Bottom Navigation Bar - Always visible
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
                    NavDestination.TRENDS -> {
                        currentDestination = NavDestination.TRENDS
                    }
                    NavDestination.PROFILE -> {
                        currentDestination = NavDestination.PROFILE
                    }
                }
            },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}
