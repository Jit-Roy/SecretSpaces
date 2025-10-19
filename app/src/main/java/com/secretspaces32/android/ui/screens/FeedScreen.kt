package com.secretspaces32.android.ui.screens

import android.location.Location
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Comment
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.secretspaces32.android.data.model.Secret
import com.secretspaces32.android.data.model.User
import com.secretspaces32.android.ui.components.*
import com.secretspaces32.android.ui.theme.*


// Story data class
data class Story(
    val id: String,
    val username: String,
    val profilePicture: String?,
    val hasUnseenStory: Boolean = false,
    val isYourStory: Boolean = false
)

@Composable
fun FeedScreen(
    nearbySecrets: List<Secret>,
    isLoading: Boolean,
    currentUser: User? = null,
    friendsWithStories: List<Story> = emptyList(),
    onSecretClick: (Secret) -> Unit,
    onDropSecretClick: () -> Unit,
    onProfileClick: () -> Unit,
    onLikeClick: (Secret) -> Unit = {},
    onMapClick: (Secret) -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    onMessagesClick: () -> Unit = {},
    onStoryClick: (Story) -> Unit = {},
    onAddStoryClick: () -> Unit = {},
    onUserProfileClick: (String) -> Unit = {}, // Add callback for viewing user profiles
    onImageClick: (List<String>, Int) -> Unit = { _, _ -> }, // Add callback for image viewer
    scrollState: LazyListState? = null // Accept scroll state from parent
) {
    // Use provided scroll state or create a new one (fallback for when not provided)
    val lazyListState = scrollState ?: rememberSaveable(saver = LazyListState.Saver) {
        LazyListState()
    }

    // Build stories list: "Your Story" + friends' stories
    val stories = remember(currentUser, friendsWithStories) {
        buildList {
            // Always add "Your Story" first
            add(
                Story(
                    id = currentUser?.id ?: "your",
                    username = "Your Story",
                    profilePicture = currentUser?.profilePictureUrl,
                    hasUnseenStory = false,
                    isYourStory = true
                )
            )
            // Add friends' stories
            addAll(friendsWithStories)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // Header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = DarkBackground,
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
                    text = "Home",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onNotificationsClick) {
                        Icon(
                            imageVector = Icons.Outlined.Notifications,
                            contentDescription = "Notifications",
                            tint = Color.White
                        )
                    }
                    IconButton(onClick = onMessagesClick) {
                        Icon(
                            imageVector = Icons.Default.Forum,
                            contentDescription = "Messages",
                            tint = Color.White
                        )
                    }
                }
            }
        }

        // Stories Section - Only show if there are stories to display
        if (stories.isNotEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = DarkBackground
            ) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(start = 36.dp, end = 20.dp, top = 12.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(stories) { story ->
                        StoryItem(
                            story = story,
                            onClick = {
                                // Click on profile icon - view stories
                                onStoryClick(story)
                            },
                            onAddClick = if (story.isYourStory) {
                                // Click on + icon - create new story
                                { onAddStoryClick() }
                            } else null
                        )
                    }
                }
            }

            // Divider
            HorizontalDivider(
                color = Color.White.copy(alpha = 0.1f),
                thickness = 0.5.dp
            )
        }

        // Content
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                isLoading -> {
                    LazyColumn(
                        state = lazyListState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 20.dp,
                            end = 20.dp,
                            top = 20.dp,
                            bottom = 96.dp
                        )
                    ) {
                        items(5) {
                            ShimmerFeedCard()
                        }
                    }
                }

                nearbySecrets.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("🔍", style = MaterialTheme.typography.displayMedium)
                            Text(
                                text = "No secrets nearby",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Be the first to drop one!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.6f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = onDropSecretClick,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = TealPrimary
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Drop Secret")
                            }
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        state = lazyListState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 20.dp,
                            end = 20.dp,
                            top = 20.dp,
                            bottom = 96.dp
                        )
                    ) {
                        items(nearbySecrets, key = { it.id }) { secret ->
                            FeedSecretCard(
                                secret = secret,
                                onLikeClick = { onLikeClick(it) },
                                onCommentClick = { onSecretClick(it) },
                                onMapClick = { onMapClick(it) },
                                onCardClick = { onSecretClick(it) },
                                onProfileClick = { userId -> onUserProfileClick(userId) },
                                onImageClick = { imageUrls, initialIndex -> onImageClick(imageUrls, initialIndex) } // Pass image click handler
                            )
                        }

                        // Infinite scroll indicator
                        if (nearbySecrets.size >= 10) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        color = TealPrimary,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StoryItem(
    story: Story,
    onClick: () -> Unit,
    onAddClick: (() -> Unit)? = null
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.width(70.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center
        ) {
            // Story Ring with larger size for Your Story
            Box(
                modifier = Modifier
                    .size(if (story.isYourStory) 64.dp else 48.dp)
                    .border(
                        width = 2.dp,
                        brush = if (story.hasUnseenStory) {
                            Brush.linearGradient(
                                colors = listOf(
                                    TealPrimary,
                                    Color(0xFF8B5CF6),
                                    Color(0xFFEC4899)
                                )
                            )
                        } else {
                            Brush.linearGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.3f),
                                    Color.White.copy(alpha = 0.3f)
                                )
                            )
                        },
                        shape = CircleShape
                    )
            )

            // Profile Picture - clickable to view stories
            Box(
                modifier = Modifier
                    .size(if (story.isYourStory) 60.dp else 44.dp)
                    .background(Color(0xFF1C1C1C), shape = CircleShape)
                    .border(1.dp, Color(0xFFFF4D4D).copy(alpha = 0.3f), CircleShape)
                    .clip(CircleShape)
                    .clickable(onClick = onClick),
                contentAlignment = Alignment.Center
            ) {
                if (story.profilePicture != null) {
                    AsyncImage(
                        model = story.profilePicture,
                        contentDescription = story.username,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = story.username,
                        tint = Color(0xFFFF4D4D),
                        modifier = Modifier.size(if (story.isYourStory) 32.dp else 26.dp)
                    )
                }
            }

            // Add icon for "Your Story" - clickable separately
            if (story.isYourStory && onAddClick != null) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .align(Alignment.BottomEnd)
                        .offset(x = (-2).dp, y = (-2).dp)
                        .clip(CircleShape)
                        .background(TealPrimary)
                        .border(2.dp, DarkBackground, CircleShape)
                        .clickable(onClick = onAddClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Story",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        // Username
        Text(
            text = story.username,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
