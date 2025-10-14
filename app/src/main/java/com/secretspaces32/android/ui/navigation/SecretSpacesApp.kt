package com.secretspaces32.android.ui.navigation

import android.Manifest
import android.widget.Toast
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.secretspaces32.android.viewmodel.MainViewModel
import com.secretspaces32.android.ui.screens.*
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.CustomCredential
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import kotlinx.coroutines.launch

enum class Screen {
    Auth,
    Feed,
    Map,
    DropSecret,
    Profile,
    MySecrets,
    SecretDetail
}

@OptIn(ExperimentalPermissionsApi::class)
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
    var selectedScreen by remember { mutableStateOf(Screen.Feed) }

    // Persist the sheet state at navigation level so it survives screen changes
    var mapSheetState by rememberSaveable { mutableStateOf("COLLAPSED") }

    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    // Location permissions - removed automatic request
    // Permissions will be requested from MapScreen when user navigates there

    // Initialize with default location when authenticated
    LaunchedEffect(uiState.isAuthenticated) {
        if (uiState.isAuthenticated) {
            try {
                println("DEBUG: App authenticated, initializing with default location")
                // Call updateLocation - it will use default location if no permission
                viewModel.updateLocation()
            } catch (e: Exception) {
                println("DEBUG: Error in authentication flow: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    // Show error messages
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            try {
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                viewModel.clearError()
            } catch (e: Exception) {
                println("DEBUG: Error showing toast: ${e.message}")
            }
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
            onGoogleSignIn = {
                coroutineScope.launch {
                    try {
                        println("DEBUG: Starting Google Sign-In process")

                        // Configure Google ID option with your Web Client ID from Firebase Console
                        val googleIdOption = GetGoogleIdOption.Builder()
                            .setFilterByAuthorizedAccounts(false)
                            .setServerClientId("170496527031-ul59lq2gqm76re4an5p2lftol7g3hjfl.apps.googleusercontent.com")
                            .build()

                        println("DEBUG: GoogleIdOption built, creating request")

                        val request = GetCredentialRequest.Builder()
                            .addCredentialOption(googleIdOption)
                            .build()

                        println("DEBUG: Requesting credentials from CredentialManager")

                        val result = credentialManager.getCredential(
                            request = request,
                            context = context
                        )

                        println("DEBUG: Credential received, type: ${result.credential.type}")

                        when (val credential = result.credential) {
                            is CustomCredential -> {
                                if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                                    try {
                                        // Use GoogleIdTokenCredential.createFrom to parse the custom credential
                                        val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                                        val idToken = googleIdTokenCredential.idToken
                                        println("DEBUG: Google ID Token extracted from CustomCredential, signing in with Firebase")
                                        viewModel.signInWithGoogle(idToken)
                                    } catch (e: GoogleIdTokenParsingException) {
                                        println("DEBUG: Error parsing Google ID Token: ${e.message}")
                                        Toast.makeText(
                                            context,
                                            "Error parsing Google credentials",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                } else {
                                    println("DEBUG: Unexpected custom credential type: ${credential.type}")
                                    Toast.makeText(
                                        context,
                                        "Unexpected credential type: ${credential.type}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                            else -> {
                                println("DEBUG: Credential is not CustomCredential: ${credential.javaClass.name}")
                                Toast.makeText(
                                    context,
                                    "Unexpected credential format",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    } catch (e: Exception) {
                        println("DEBUG: Google Sign-In error: ${e.javaClass.simpleName}")
                        println("DEBUG: Error message: ${e.message}")
                        e.printStackTrace()

                        val errorMessage = when {
                            e.message?.contains("16") == true -> "Google Sign-In not configured. Enable it in Firebase Console."
                            e.message?.contains("developer console", ignoreCase = true) == true ->
                                "Please add SHA-1 & SHA-256 to Firebase Console"
                            e.message?.contains("cancelled", ignoreCase = true) == true -> "Sign-in cancelled"
                            e.message?.contains("No credentials", ignoreCase = true) == true -> "No Google accounts found"
                            else -> "Google Sign-In failed: ${e.message}"
                        }

                        Toast.makeText(
                            context,
                            errorMessage,
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            },
            isLoading = uiState.isLoading,
            isEmailAuthLoading = uiState.isEmailAuthLoading,
            isGoogleAuthLoading = uiState.isGoogleAuthLoading
        )
        return
    }

    // Main app navigation
    when (selectedScreen) {
        Screen.Feed, Screen.Map, Screen.Profile -> {
            MainScreenContainer(
                currentLocation = uiState.currentLocation,
                nearbySecrets = uiState.secrets,
                currentUser = uiState.currentUser,
                mySecrets = uiState.mySecrets,
                isLoading = uiState.isLoading,
                onSecretClick = { secret ->
                    viewModel.selectSecret(secret)
                    selectedScreen = Screen.SecretDetail
                },
                onDropSecretClick = {
                    selectedScreen = Screen.DropSecret
                },
                onSignOut = {
                    viewModel.signOut()
                    selectedScreen = Screen.Feed
                },
                onUpdateProfile = { username, bio, imageUri ->
                    viewModel.updateProfile(username, bio, imageUri)
                },
                onLikeClick = { secret ->
                    viewModel.toggleLike(secret)
                },
                onLoadMySecrets = {
                    viewModel.loadMySecrets()
                },
                onLocationPermissionGranted = {
                    viewModel.updateLocation()
                },
                onPostSecret = { text, imageUri, isAnonymous, mood, category, hashtags ->
                    viewModel.createSecret(text, imageUri, isAnonymous, mood, category, hashtags)
                },
                cacheDir = context.cacheDir
            )
        }

        Screen.DropSecret -> {
            // This case is now handled within MainScreenContainer
            // Redirect to Feed
            selectedScreen = Screen.Feed
        }

        Screen.Profile -> {
            // Load user's secrets when Profile screen is opened
            LaunchedEffect(Unit) {
                viewModel.loadMySecrets()
            }

            ProfileScreen(
                user = uiState.currentUser,
                mySecrets = uiState.mySecrets,
                onSignOut = {
                    viewModel.signOut()
                    selectedScreen = Screen.Feed
                },
                onUpdateProfile = { username, bio, imageUri ->
                    viewModel.updateProfile(username, bio, imageUri)
                },
                onMySecretsClick = {
                    selectedScreen = Screen.MySecrets
                },
                onBackClick = {
                    selectedScreen = Screen.Feed
                },
                isLoading = uiState.isLoading
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

        Screen.SecretDetail -> {
            uiState.selectedSecret?.let { secret ->
                SecretDetailScreen(
                    secret = secret,
                    comments = uiState.selectedSecretComments,
                    likes = uiState.selectedSecretLikes,
                    isLikedByCurrentUser = secret.isLikedByCurrentUser,
                    onLikeClick = {
                        viewModel.toggleLike(secret)
                    },
                    onCommentSubmit = { commentText ->
                        viewModel.addComment(secret.id, commentText)
                    },
                    onBack = {
                        selectedScreen = Screen.Feed
                    },
                    isLoading = uiState.isLoading
                )
            }
        }
        Screen.Auth -> {
            // Should not reach here as we handle auth above
        }
    }
}
