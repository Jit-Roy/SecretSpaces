package com.secretspaces32.android.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.secretspaces32.android.data.model.User

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    user: User?,
    onSignOut: () -> Unit,
    onUpdateProfile: (username: String, bio: String, imageUri: Uri?) -> Unit,
    onBackClick: () -> Unit,
    isLoading: Boolean = false
) {
    var username by remember { mutableStateOf(user?.username ?: "") }
    var bio by remember { mutableStateOf(user?.bio ?: "") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
    }

    LaunchedEffect(user) {
        username = user?.username ?: ""
        bio = user?.bio ?: ""
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0C0C0C))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            // ---- TOP BAR ----
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF0C0C0C),
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }

                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )

                    // Placeholder for symmetry
                    Spacer(modifier = Modifier.width(48.dp))
                }
            }

            // Scrollable content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {

                // ---- PROFILE PICTURE ----
                Box(
                    modifier = Modifier.size(120.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFFFF4D4D).copy(alpha = 0.15f), shape = CircleShape)
                            .border(2.dp, Color(0xFFFF4D4D), CircleShape)
                            .clip(CircleShape)
                    ) {
                        if (selectedImageUri != null || user?.profilePictureUrl != null) {
                            AsyncImage(
                                model = selectedImageUri ?: user?.profilePictureUrl,
                                contentDescription = "Profile picture",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Profile",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp),
                                tint = Color(0xFFFF4D4D)
                            )
                        }
                    }

                    IconButton(
                        onClick = { imagePickerLauncher.launch("image/*") },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(36.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFFF4D4D)
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Edit photo",
                                tint = Color.White,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                }

                // ---- EDIT PROFILE SECTION ----
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFFFF4D4D).copy(alpha = 0.3f)),
                    color = Color(0xFF121212)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Edit Profile",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )

                        OutlinedTextField(
                            value = username,
                            onValueChange = { username = it },
                            label = { Text("Username", color = Color.White.copy(alpha = 0.8f)) },
                            leadingIcon = { Icon(Icons.Default.Person, null, tint = Color(0xFFFF4D4D)) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFFFF4D4D),
                                unfocusedBorderColor = Color(0xFFFF4D4D).copy(alpha = 0.3f),
                                focusedLabelColor = Color(0xFFFF4D4D),
                                unfocusedLabelColor = Color.White.copy(alpha = 0.7f),
                                cursorColor = Color.White,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedPlaceholderColor = Color.White.copy(alpha = 0.5f),
                                unfocusedPlaceholderColor = Color.White.copy(alpha = 0.5f)
                            )
                        )

                        OutlinedTextField(
                            value = bio,
                            onValueChange = { bio = it },
                            label = { Text("Bio", color = Color.White.copy(alpha = 0.8f)) },
                            placeholder = { Text("Tell something about yourself", color = Color.White.copy(alpha = 0.5f)) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 100.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFFFF4D4D),
                                unfocusedBorderColor = Color(0xFFFF4D4D).copy(alpha = 0.3f),
                                focusedLabelColor = Color(0xFFFF4D4D),
                                unfocusedLabelColor = Color.White.copy(alpha = 0.7f),
                                cursorColor = Color.White,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedPlaceholderColor = Color.White.copy(alpha = 0.5f),
                                unfocusedPlaceholderColor = Color.White.copy(alpha = 0.5f)
                            )
                        )

                        Button(
                            onClick = {
                                onUpdateProfile(username, bio, selectedImageUri)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            enabled = username.isNotBlank() && !isLoading,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFF4D4D)
                            )
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("Save Changes", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // ---- LOGOUT SECTION ----
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFFFF4D4D).copy(alpha = 0.3f)),
                    color = Color(0xFF121212)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Account",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )

                        Button(
                            onClick = { showLogoutDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFF4D4D).copy(alpha = 0.2f)
                            )
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Logout,
                                contentDescription = "Logout",
                                tint = Color(0xFFFF4D4D)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Log Out", color = Color(0xFFFF4D4D), fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(80.dp)) // Space for navigation bar
            }
        }

        // Logout Confirmation Dialog
        if (showLogoutDialog) {
            AlertDialog(
                onDismissRequest = { showLogoutDialog = false },
                title = {
                    Text(
                        text = "Log Out",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        text = "Are you sure you want to log out?",
                        color = Color.White.copy(alpha = 0.8f)
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showLogoutDialog = false
                            onSignOut()
                        }
                    ) {
                        Text("Log Out", color = Color(0xFFFF4D4D), fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showLogoutDialog = false }) {
                        Text("Cancel", color = Color.White.copy(alpha = 0.7f))
                    }
                },
                containerColor = Color(0xFF1C1C1C)
            )
        }
    }
}
