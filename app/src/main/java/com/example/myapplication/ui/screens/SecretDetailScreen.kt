package com.example.myapplication.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.myapplication.data.model.Comment
import com.example.myapplication.data.model.Like
import com.example.myapplication.data.model.Secret
import com.example.myapplication.utils.LocationHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecretDetailScreen(
    secret: Secret,
    comments: List<Comment>,
    likes: List<Like>,
    isLikedByCurrentUser: Boolean,
    onLikeClick: () -> Unit,
    onCommentSubmit: (String) -> Unit,
    onBack: () -> Unit,
    isLoading: Boolean = false
) {
    var commentText by remember { mutableStateOf("") }
    var showLikes by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Secret Details") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Secret content
            item {
                SecretContent(secret = secret)
            }

            // Like and comment buttons
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onLikeClick,
                        enabled = !isLoading,
                        colors = if (isLikedByCurrentUser) {
                            ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            ButtonDefaults.outlinedButtonColors()
                        }
                    ) {
                        Icon(
                            if (isLikedByCurrentUser) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Like"
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("${secret.likeCount}")
                    }

                    OutlinedButton(
                        onClick = { /* Scroll to comments */ }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Comment, contentDescription = "Comments")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("${secret.commentCount}")
                    }

                    TextButton(onClick = { showLikes = true }) {
                        Text("View Likes")
                    }
                }
            }

            // Comment input
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = commentText,
                        onValueChange = { commentText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Add a comment...") },
                        maxLines = 3
                    )

                    IconButton(
                        onClick = {
                            if (commentText.isNotBlank()) {
                                onCommentSubmit(commentText)
                                commentText = ""
                            }
                        },
                        enabled = !isLoading && commentText.isNotBlank()
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
                    }
                }
            }

            // Comments section
            item {
                Text(
                    text = "Comments (${comments.size})",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            if (comments.isEmpty()) {
                item {
                    Text(
                        text = "No comments yet. Be the first to comment!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                }
            } else {
                items(comments) { comment ->
                    CommentItem(comment = comment)
                }
            }
        }
    }

    // Likes dialog
    if (showLikes) {
        AlertDialog(
            onDismissRequest = { showLikes = false },
            title = { Text("Likes (${likes.size})") },
            text = {
                LazyColumn {
                    if (likes.isEmpty()) {
                        item {
                            Text("No likes yet")
                        }
                    } else {
                        items(likes) { like ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Favorite,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(like.username)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLikes = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun SecretContent(secret: Secret) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // User info
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = secret.userProfilePicture ?: "https://via.placeholder.com/40",
                    contentDescription = "Profile picture",
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = secret.username,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = LocationHelper.formatTimestamp(secret.timestamp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Secret text
            Text(
                text = secret.text,
                style = MaterialTheme.typography.bodyLarge
            )

            // Image if available
            secret.imageUrl?.let { imageUrl ->
                Spacer(modifier = Modifier.height(12.dp))
                AsyncImage(
                    model = imageUrl,
                    contentDescription = "Secret image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Location
            secret.distance?.let { distance ->
                Text(
                    text = "📍 ${LocationHelper.formatDistance(distance)} away",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun CommentItem(comment: Comment) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        AsyncImage(
            model = comment.userProfilePicture ?: "https://via.placeholder.com/32",
            contentDescription = "Profile picture",
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = comment.username,
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = LocationHelper.formatTimestamp(comment.timestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = comment.text,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
