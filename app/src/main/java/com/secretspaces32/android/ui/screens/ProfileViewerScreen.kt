package com.secretspaces32.android.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.secretspaces32.android.data.model.Secret
import com.secretspaces32.android.ui.components.FeedSecretCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileViewerScreen(
    user: User?,
    currentUserId: String? = null,
    userSecrets: List<Secret> = emptyList(),
    isFollowing: Boolean = false,
    isFriend: Boolean = false,
    friendRequestSent: Boolean = false,
    onBack: () -> Unit,
    onFollowClick: () -> Unit,
    onSendFriendRequest: () -> Unit,
    onLikeClick: (Secret) -> Unit = {},
    onCommentClick: (Secret) -> Unit = {},
    onMapClick: (Secret) -> Unit = {},
    onSecretClick: (Secret) -> Unit = {},
    isLoading: Boolean = false
) {
    // Check if viewing own profile
    val isOwnProfile = currentUserId != null && currentUserId == user?.id

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
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }

                    Text(
                        text = "Profile",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )

                    // Placeholder for alignment
                    Box(modifier = Modifier.size(48.dp))
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
                            Spacer(modifier = Modifier.height(4.dp))
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
                        ProfileStatItem("Posts", userSecrets.size.toString())
                        ProfileStatItem("Followers", user?.followersCount?.toString() ?: "0")
                        ProfileStatItem("Following", user?.followingCount?.toString() ?: "0")
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // ---- ACTION BUTTONS ---- (only show for other users' profiles)
                    if (!isOwnProfile) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Follow/Unfollow Button
                            Button(
                                onClick = onFollowClick,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isFollowing) Color(0xFF333333) else Color(0xFFFF4D4D),
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(12.dp),
                                enabled = !isLoading
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        imageVector = if (isFollowing) Icons.Default.PersonRemove else Icons.Default.PersonAdd,
                                        contentDescription = if (isFollowing) "Unfollow" else "Follow",
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isFollowing) "Following" else "Follow",
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // Friend Request Button
                            Button(
                                onClick = onSendFriendRequest,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = when {
                                        isFriend -> Color(0xFF4CAF50)
                                        friendRequestSent -> Color(0xFF666666)
                                        else -> Color(0xFFFF4D4D)
                                    },
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(12.dp),
                                enabled = !isLoading && !friendRequestSent
                            ) {
                                Icon(
                                    imageVector = when {
                                        isFriend -> Icons.Default.Check
                                        friendRequestSent -> Icons.Default.HourglassEmpty
                                        else -> Icons.Default.PersonAddAlt1
                                    },
                                    contentDescription = "Friend Request",
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = when {
                                        isFriend -> "Friends"
                                        friendRequestSent -> "Pending"
                                        else -> "Add Friend"
                                    },
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    // Section title for posts
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Posts",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${userSecrets.size} posts",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                }

                // ---- USER POSTS SECTION ----
                if (userSecrets.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PostAdd,
                                contentDescription = "No posts",
                                modifier = Modifier.size(48.dp),
                                tint = Color.White.copy(alpha = 0.3f)
                            )
                            Text(
                                text = "No posts yet",
                                color = Color.White.copy(alpha = 0.5f),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        userSecrets.forEach { secret ->
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
private fun ProfileStatItem(label: String, value: String) {
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
