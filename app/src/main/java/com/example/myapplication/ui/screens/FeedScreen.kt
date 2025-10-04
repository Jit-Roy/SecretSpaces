package com.example.myapplication.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.myapplication.data.model.Secret
import com.example.myapplication.utils.LocationHelper

@Composable
fun FeedScreen(
    secrets: List<Secret>,
    isLoading: Boolean,
    onSecretClick: (Secret) -> Unit = {},
    onLikeClick: (Secret) -> Unit = {}
) {
    Box(modifier = Modifier.fillMaxSize()) {
        if (isLoading && secrets.isEmpty()) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center)
            )
        } else if (secrets.isEmpty()) {
            Text(
                text = "No secrets nearby.\nBe the first to drop one!",
                modifier = Modifier.align(Alignment.Center),
                style = MaterialTheme.typography.bodyLarge
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(secrets.sortedBy { it.distance }) { secret ->
                    SecretCard(
                        secret = secret,
                        onClick = { onSecretClick(secret) },
                        onLikeClick = { onLikeClick(secret) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecretCard(
    secret: Secret,
    onClick: () -> Unit = {},
    onLikeClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        onClick = onClick
    ) {
        SecretDetailContent(
            secret = secret,
            onLikeClick = onLikeClick
        )
    }
}

@Composable
fun SecretDetailContent(
    secret: Secret,
    onLikeClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // User header
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
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = secret.username,
                    style = MaterialTheme.typography.titleMedium
                )
                secret.distance?.let { distance ->
                    Text(
                        text = LocationHelper.formatDistance(distance),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Text(
                text = LocationHelper.formatTimestamp(secret.timestamp),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

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
                    .heightIn(max = 300.dp),
                contentScale = ContentScale.Crop
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Engagement buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.clickable { onLikeClick() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (secret.isLikedByCurrentUser) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Like",
                    tint = if (secret.isLikedByCurrentUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${secret.likeCount}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Comment,
                    contentDescription = "Comments",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${secret.commentCount}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            if (secret.isAnonymous) {
                Text(
                    text = "🕶️ Anonymous",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }
    }
}
