package com.secretspaces32.android.ui.navigation

import android.Manifest
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.secretspaces32.android.viewmodel.MainViewModel
import com.secretspaces32.android.ui.screens.*
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState

enum class Screen {
    Auth,
    Map,
    Feed,
    DropSecret,
    Profile,
    MySecrets,
    SecretDetail
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun SecretSpacesApp() {
    val context = LocalContext.current
    val viewModel: MainViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return MainViewModel(context) as T
            }
        }
    )

    val uiState by viewModel.uiState.collectAsState()
    var selectedScreen by remember { mutableStateOf(Screen.Map) }

    // Location permissions
    val locationPermissions = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )

    // Request location permissions when authenticated
    LaunchedEffect(uiState.isAuthenticated) {
        if (uiState.isAuthenticated && !locationPermissions.allPermissionsGranted) {
            locationPermissions.launchMultiplePermissionRequest()
        }
    }

    // Update location when permissions are granted
    LaunchedEffect(locationPermissions.allPermissionsGranted) {
        if (locationPermissions.allPermissionsGranted && uiState.isAuthenticated) {
            viewModel.updateLocation()
        }
    }

    // Show error messages
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            viewModel.clearError()
        }
    }

    // Show authentication screen if not authenticated
    if (!uiState.isAuthenticated) {
        AuthScreen(
            onSignIn = { email, password ->
                viewModel.signIn(email, password)
            },
            onSignUp = { email, password, username ->
                viewModel.signUp(email, password, username)
            },
            onGoogleSignIn = { idToken ->
                viewModel.signInWithGoogle(idToken)
            },
            isLoading = uiState.isLoading
        )
        return
    }

    // Show secret detail screen
    if (selectedScreen == Screen.SecretDetail && uiState.selectedSecret != null) {
        SecretDetailScreen(
            secret = uiState.selectedSecret!!,
            comments = uiState.selectedSecretComments,
            likes = uiState.selectedSecretLikes,
            isLikedByCurrentUser = uiState.selectedSecret!!.isLikedByCurrentUser,
            onLikeClick = {
                viewModel.toggleLike(uiState.selectedSecret!!)
            },
            onCommentSubmit = { text ->
                viewModel.addComment(uiState.selectedSecret!!.id, text)
            },
            onBack = {
                viewModel.selectSecret(null)
                selectedScreen = Screen.Feed
            },
            isLoading = uiState.isLoading
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(when (selectedScreen) {
                        Screen.Map -> "Secret Spaces"
                        Screen.Feed -> "Feed"
                        Screen.DropSecret -> "Drop Secret"
                        Screen.Profile -> "Profile"
                        Screen.MySecrets -> "My Secrets"
                        else -> "Secret Spaces"
                    })
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Place, contentDescription = "Map") },
                    label = { Text("Map") },
                    selected = selectedScreen == Screen.Map,
                    onClick = { selectedScreen = Screen.Map }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Menu, contentDescription = "Feed") },
                    label = { Text("Feed") },
                    selected = selectedScreen == Screen.Feed,
                    onClick = { selectedScreen = Screen.Feed }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Add, contentDescription = "Drop Secret") },
                    label = { Text("Drop") },
                    selected = selectedScreen == Screen.DropSecret,
                    onClick = { selectedScreen = Screen.DropSecret }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = "My Secrets") },
                    label = { Text("Mine") },
                    selected = selectedScreen == Screen.MySecrets,
                    onClick = {
                        selectedScreen = Screen.MySecrets
                        viewModel.loadMySecrets()
                    }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                    label = { Text("Profile") },
                    selected = selectedScreen == Screen.Profile,
                    onClick = { selectedScreen = Screen.Profile }
                )
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            if (!locationPermissions.allPermissionsGranted) {
                // Show permission request screen
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Location permission is required",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { locationPermissions.launchMultiplePermissionRequest() }) {
                        Text("Grant Permission")
                    }
                }
            } else {
                when (selectedScreen) {
                    Screen.Map -> {
                        MapScreen(
                            secrets = uiState.secrets,
                            currentLatitude = uiState.currentLocation?.latitude,
                            currentLongitude = uiState.currentLocation?.longitude,
                            onSecretClick = { secret ->
                                viewModel.selectSecret(secret)
                                selectedScreen = Screen.SecretDetail
                            },
                            selectedSecret = uiState.selectedSecret,
                            onLocationPermissionGranted = {
                                viewModel.updateLocation()
                            }
                        )
                    }
                    Screen.Feed -> {
                        FeedScreen(
                            secrets = uiState.secrets,
                            isLoading = uiState.isLoading,
                            onSecretClick = { secret ->
                                viewModel.selectSecret(secret)
                                selectedScreen = Screen.SecretDetail
                            },
                            onLikeClick = { secret ->
                                viewModel.toggleLike(secret)
                            }
                        )
                    }
                    Screen.DropSecret -> {
                        DropSecretScreen(
                            isLoading = uiState.isLoading,
                            onPostSecret = { text, imageUri, isAnonymous ->
                                viewModel.createSecret(text, imageUri, isAnonymous)
                                Toast.makeText(
                                    context,
                                    "Secret posted successfully!",
                                    Toast.LENGTH_SHORT
                                ).show()
                                selectedScreen = Screen.Map
                            }
                        )
                    }
                    Screen.MySecrets -> {
                        MySecretsScreen(
                            secrets = uiState.mySecrets,
                            isLoading = uiState.isLoading,
                            onSecretClick = { secret ->
                                viewModel.selectSecret(secret)
                                selectedScreen = Screen.SecretDetail
                            }
                        )
                    }
                    Screen.Profile -> {
                        ProfileScreen(
                            user = uiState.currentUser,
                            onSignOut = {
                                viewModel.signOut()
                                selectedScreen = Screen.Map
                            },
                            onUpdateProfile = { username, bio, imageUri ->
                                viewModel.updateProfile(username, bio, imageUri)
                            },
                            isLoading = uiState.isLoading
                        )
                    }
                    else -> {}
                }
            }
        }
    }
}
