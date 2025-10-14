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
import com.secretspaces32.android.ui.components.FeedSecretCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    user: User?,
    mySecrets: List<Secret> = emptyList(),
    onSignOut: () -> Unit,
    onUpdateProfile: (username: String, bio: String, imageUri: Uri?) -> Unit,
    onMySecretsClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onLikeClick: (Secret) -> Unit = {},
    onCommentClick: (Secret) -> Unit = {},
    onMapClick: (Secret) -> Unit = {},
    onSecretClick: (Secret) -> Unit = {},
    isLoading: Boolean = false
) {
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

                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = Color(0xFFFF4D4D)
                        )
                    }
                }
            }

            // Scrollable content with all posts
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                // Profile header section
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
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
                            if (user?.profilePictureUrl != null) {
                                AsyncImage(
                                    model = user.profilePictureUrl,
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
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // ---- USER INFO ----
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = user?.username ?: "User Name",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        if (!user?.bio.isNullOrBlank()) {
                            Text(
                                text = user?.bio ?: "",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.7f)
                            )
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
                        StatItem("Posts", mySecrets.size.toString())
                        StatItem("Followers", user?.followersCount?.toString() ?: "0")
                        StatItem("Following", user?.followingCount?.toString() ?: "0")
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Section title for posts
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "My Posts",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // ---- MY POSTS SECTION ----
                if (mySecrets.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No posts yet",
                            color = Color.White.copy(alpha = 0.5f),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        mySecrets.forEach { secret ->
                            FeedSecretCard(
                                secret = secret,
                                onLikeClick = onLikeClick,
                                onCommentClick = onCommentClick,
                                onMapClick = onMapClick,
                                onCardClick = onSecretClick
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(100.dp)) // Space for navigation bar
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
