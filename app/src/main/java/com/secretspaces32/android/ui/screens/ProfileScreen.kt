package com.secretspaces32.android.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.secretspaces32.android.data.model.User
import com.secretspaces32.android.data.model.Secret
import com.secretspaces32.android.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    user: User?,
    mySecrets: List<Secret> = emptyList(),
    onSignOut: () -> Unit,
    onUpdateProfile: (username: String, bio: String, imageUri: Uri?) -> Unit,
    onMySecretsClick: () -> Unit = {},
    onBackClick: () -> Unit = {},
    isLoading: Boolean = false
) {
    var isEditMode by remember { mutableStateOf(false) }
    var username by remember { mutableStateOf(user?.username ?: "") }
    var bio by remember { mutableStateOf(user?.bio ?: "") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

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
                        .padding(start = 36.dp, end = 20.dp, top = 16.dp, bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Profile",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )

                    IconButton(onClick = onSignOut) {
                        Icon(
                            imageVector = Icons.Default.Logout,
                            contentDescription = "Sign out",
                            tint = Color(0xFFFF4D4D)
                        )
                    }
                }
            }

            // Scrollable content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Spacer(modifier = Modifier.height(16.dp))

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

                    if (isEditMode) {
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
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ---- USER INFO ----
                AnimatedContent(
                    targetState = isEditMode,
                    transitionSpec = {
                        fadeIn() + expandVertically() togetherWith fadeOut() + shrinkVertically()
                    },
                    label = "userInfoAnimation"
                ) { editMode ->
                    if (editMode) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
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

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        isEditMode = false
                                        username = user?.username ?: ""
                                        bio = user?.bio ?: ""
                                        selectedImageUri = null
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Cancel")
                                }

                                Button(
                                    onClick = {
                                        onUpdateProfile(username, bio, selectedImageUri)
                                        isEditMode = false
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    enabled = username.isNotBlank(),
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
                                        Text("Save")
                                    }
                                }
                            }
                        }
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = user?.username ?: "User Name",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )

                            if (!bio.isNullOrBlank()) {
                                Text(
                                    text = bio,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = { isEditMode = true },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF4D4D))
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Edit Profile")
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // ---- STATS SECTION ----
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFFFF4D4D).copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatItem("Posts", "42")
                    StatItem("Followers", "200")
                    StatItem("Following", "180")
                }

                Spacer(modifier = Modifier.height(24.dp))

                // ---- SECRETS SECTION ----
                SectionBox(title = "My Secrets") {
                    if (mySecrets.isEmpty()) {
                        Text(
                            text = "No secrets yet.",
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            mySecrets.take(3).forEach {
                                SecretCard(it.text)
                            }
                            TextButton(onClick = onMySecretsClick) {
                                Text("View All", color = Color(0xFFFF4D4D))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

// --- Helper Composables ---

@Composable
fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.6f)
        )
    }
}

@Composable
fun SectionBox(title: String, content: @Composable ColumnScope.() -> Unit) {
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
                text = title,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
            content()
        }
    }
}

@Composable
fun SecretCard(text: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF1C1C1C),
        border = BorderStroke(1.dp, Color(0xFFFF4D4D).copy(alpha = 0.15f))
    ) {
        Text(
            text = text,
            color = Color.White,
            modifier = Modifier.padding(16.dp)
        )
    }
}
