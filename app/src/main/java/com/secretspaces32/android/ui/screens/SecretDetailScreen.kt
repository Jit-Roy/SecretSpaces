package com.secretspaces32.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
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
import com.secretspaces32.android.data.model.Comment
import com.secretspaces32.android.data.model.Like
import com.secretspaces32.android.data.model.Secret
import com.secretspaces32.android.utils.LocationHelper

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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0C0C0C))
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(start = 36.dp, end = 20.dp, top = 16.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color(0xFFFF4D4D)
                    )
                }

                Text(
                    text = "Secret Details",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                // Placeholder for symmetry
                Box(modifier = Modifier.size(48.dp))
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 16.dp,
                    bottom = 96.dp
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Secret Content Card
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF121212),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            Color(0xFFFF4D4D).copy(alpha = 0.3f)
                        )
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
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF1C1C1C))
                                        .border(1.dp, Color(0xFFFF4D4D).copy(alpha = 0.3f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (secret.userProfilePicture != null) {
                                        AsyncImage(
                                            model = secret.userProfilePicture,
                                            contentDescription = "Profile",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = "Profile",
                                            tint = Color(0xFFFF4D4D),
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = secret.username,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = LocationHelper.formatTimestamp(secret.timestamp),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.6f)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Secret text
                            Text(
                                text = secret.text,
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.White
                            )

                            // Image if available
                            secret.imageUrl?.let { imageUrl ->
                                Spacer(modifier = Modifier.height(12.dp))
                                AsyncImage(
                                    model = imageUrl,
                                    contentDescription = "Secret image",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 400.dp)
                                        .clip(RoundedCornerShape(12.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Location
                            secret.distance?.let { distance ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = Color(0xFFFF4D4D),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = LocationHelper.formatDistance(distance) + " away",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFFFF4D4D)
                                    )
                                }
                            }
                        }
                    }
                }

                // Like and Comment Actions
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Like Button
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            color = if (isLikedByCurrentUser)
                                Color(0xFFFF4D4D)
                            else
                                Color(0xFF1C1C1C),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isLikedByCurrentUser)
                                    Color(0xFFFF4D4D)
                                else
                                    Color(0xFFFF4D4D).copy(alpha = 0.3f)
                            ),
                            onClick = onLikeClick
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isLikedByCurrentUser)
                                        Icons.Default.Favorite
                                    else
                                        Icons.Default.FavoriteBorder,
                                    contentDescription = "Like",
                                    tint = if (isLikedByCurrentUser)
                                        Color.White
                                    else
                                        Color(0xFFFF4D4D),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${secret.likeCount}",
                                    color = if (isLikedByCurrentUser)
                                        Color.White
                                    else
                                        Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Comment Count Button
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF1C1C1C),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                Color(0xFFFF4D4D).copy(alpha = 0.3f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Comment,
                                    contentDescription = "Comments",
                                    tint = Color(0xFFFF4D4D),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${secret.commentCount}",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // View Likes Button
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF1C1C1C),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                Color(0xFFFF4D4D).copy(alpha = 0.3f)
                            ),
                            onClick = { showLikes = true }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Visibility,
                                    contentDescription = "View Likes",
                                    tint = Color(0xFFFF4D4D),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                // Comments Header
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Comments",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "(${comments.size})",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                }

                // Comments List
                if (comments.isEmpty()) {
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF1C1C1C),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                Color(0xFFFF4D4D).copy(alpha = 0.15f)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Comment,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.3f),
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "No comments yet",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = Color.White.copy(alpha = 0.6f)
                                )
                                Text(
                                    text = "Be the first to comment!",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.4f)
                                )
                            }
                        }
                    }
                } else {
                    items(comments) { comment ->
                        CommentItem(comment = comment)
                    }
                }

                // Bottom spacing
                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }

            // Comment Input (Fixed at bottom)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF121212),
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    OutlinedTextField(
                        value = commentText,
                        onValueChange = { commentText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = {
                            Text(
                                "Add a comment...",
                                color = Color.White.copy(alpha = 0.5f)
                            )
                        },
                        maxLines = 3,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFFF4D4D),
                            unfocusedBorderColor = Color(0xFFFF4D4D).copy(alpha = 0.3f),
                            cursorColor = Color(0xFFFF4D4D),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    Surface(
                        shape = CircleShape,
                        color = if (commentText.isNotBlank())
                            Color(0xFFFF4D4D)
                        else
                            Color(0xFF1C1C1C),
                        onClick = {
                            if (commentText.isNotBlank() && !isLoading) {
                                onCommentSubmit(commentText)
                                commentText = ""
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = if (commentText.isNotBlank())
                                Color.White
                            else
                                Color.White.copy(alpha = 0.3f),
                            modifier = Modifier
                                .padding(12.dp)
                                .size(24.dp)
                        )
                    }
                }
            }
        }
    }

    // Likes Dialog
    if (showLikes) {
        AlertDialog(
            onDismissRequest = { showLikes = false },
            containerColor = Color(0xFF121212),
            title = {
                Text(
                    "Likes (${likes.size})",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                if (likes.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.FavoriteBorder,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.3f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "No likes yet",
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 400.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
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
                                    tint = Color(0xFFFF4D4D),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    like.username,
                                    color = Color.White,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showLikes = false }
                ) {
                    Text("Close", color = Color(0xFFFF4D4D))
                }
            }
        )
    }
}

@Composable
fun CommentItem(comment: Comment) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF1C1C1C),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            Color(0xFFFF4D4D).copy(alpha = 0.15f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF0C0C0C))
                    .border(1.dp, Color(0xFFFF4D4D).copy(alpha = 0.3f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (comment.userProfilePicture != null) {
                    AsyncImage(
                        model = comment.userProfilePicture,
                        contentDescription = "Profile",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Profile",
                        tint = Color(0xFFFF4D4D),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = comment.username,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = LocationHelper.formatTimestamp(comment.timestamp),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = comment.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        }
    }
}
