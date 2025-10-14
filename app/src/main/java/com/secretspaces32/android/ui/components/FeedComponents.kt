package com.secretspaces32.android.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.secretspaces32.android.data.model.Secret
import com.secretspaces32.android.ui.theme.*
import com.secretspaces32.android.utils.LocationHelper

@Composable
fun FeedSecretCard(
    secret: Secret,
    onLikeClick: (Secret) -> Unit,
    onCommentClick: (Secret) -> Unit,
    onMapClick: (Secret) -> Unit,
    onCardClick: (Secret) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    val maxLines = if (isExpanded) Int.MAX_VALUE else 4

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Transparent)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Header: Profile info + distance/time
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Profile Picture or Anonymous Avatar
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1C1C1C))
                        .border(1.dp, Color(0xFFFF4D4D).copy(alpha = 0.3f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (secret.isAnonymous || secret.userProfilePicture.isNullOrEmpty()) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Profile",
                            tint = Color(0xFFFF4D4D),
                            modifier = Modifier.size(26.dp)
                        )
                    } else {
                        AsyncImage(
                            model = secret.userProfilePicture,
                            contentDescription = "Profile",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                // Username + metadata
                Column {
                    Text(
                        text = if (secret.isAnonymous) "Anonymous" else secret.username.ifEmpty { "Secret Keeper" },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        secret.distance?.let { distance ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = AquaGreen,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = LocationHelper.formatDistance(distance),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AquaGreen
                                )
                            }
                        }
                        Text(
                            text = "·",
                            color = Color.White.copy(alpha = 0.4f),
                            fontSize = 10.sp
                        )
                        Text(
                            text = LocationHelper.formatTimestamp(secret.timestamp),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            // Category Tag
            secret.category?.let { category ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF6B46C1).copy(alpha = 0.3f),
                    modifier = Modifier.border(
                        1.dp,
                        Color(0xFF9333EA).copy(alpha = 0.5f),
                        RoundedCornerShape(12.dp)
                    )
                ) {
                    Text(
                        text = category,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFE9D5FF),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 10.sp
                    )
                }
            }
        }

        // Mood and Hashtags Row
        if (!secret.mood.isNullOrEmpty() || !secret.hashtags.isNullOrEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Mood emoji
                secret.mood?.let { mood ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFFF4D4D).copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = mood,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            fontSize = 16.sp
                        )
                    }
                }

                // Hashtags
                secret.hashtags?.takeIf { it.isNotEmpty() }?.let { hashtags ->
                    Text(
                        text = hashtags,
                        style = MaterialTheme.typography.bodySmall,
                        color = TealPrimary,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Body: Secret text
        Column {
            Text(
                text = secret.text,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White,
                lineHeight = 24.sp,
                maxLines = maxLines,
                overflow = TextOverflow.Ellipsis
            )

            // "Read more" button if text is long
            if (secret.text.length > 150 && !isExpanded) {
                Text(
                    text = "Read more...",
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .clickable { isExpanded = true },
                    style = MaterialTheme.typography.bodySmall,
                    color = TealPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            }
            if (isExpanded && secret.text.length > 150) {
                Text(
                    text = "Show less",
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .clickable { isExpanded = false },
                    style = MaterialTheme.typography.bodySmall,
                    color = TealPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Optional: Image if present
        secret.imageUrl?.let { imageUrl ->
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(16.dp))
            ) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = "Secret image",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                // Gradient overlay for better visibility
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.3f)
                                )
                            )
                        )
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Footer: Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Like button
            FeedActionButton(
                icon = if (secret.isLikedByCurrentUser) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                label = if (secret.likeCount > 0) "${secret.likeCount}" else "Like",
                tint = if (secret.isLikedByCurrentUser) Color(0xFFFF4081) else Color.White.copy(alpha = 0.7f),
                onClick = { onLikeClick(secret) }
            )

            // Comment button
            FeedActionButton(
                icon = Icons.Default.ChatBubbleOutline,
                label = if (secret.commentCount > 0) "${secret.commentCount}" else "Comment",
                tint = Color.White.copy(alpha = 0.7f),
                onClick = { onCommentClick(secret) }
            )

            // Map button
            FeedActionButton(
                icon = Icons.Default.Place,
                label = "Map",
                tint = AquaGreen.copy(alpha = 0.9f),
                onClick = { onMapClick(secret) }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Thin horizontal divider to separate posts
        HorizontalDivider(
            color = Color.White.copy(alpha = 0.15f),
            thickness = 0.5.dp
        )
    }
}

@Composable
fun FeedActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = tint,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun ShimmerFeedCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Transparent)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(shimmerBrush())
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(100.dp)
                        .height(14.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(shimmerBrush())
                )
                Box(
                    modifier = Modifier
                        .width(140.dp)
                        .height(12.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(shimmerBrush())
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        repeat(3) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(shimmerBrush())
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            repeat(3) {
                Box(
                    modifier = Modifier
                        .width(70.dp)
                        .height(16.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(shimmerBrush())
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Divider for shimmer card too
        HorizontalDivider(
            color = Color.White.copy(alpha = 0.15f),
            thickness = 0.5.dp
        )
    }
}

@Composable
fun shimmerBrush(): Brush {
    val shimmerColors = listOf(
        Color.White.copy(alpha = 0.05f),
        Color.White.copy(alpha = 0.15f),
        Color.White.copy(alpha = 0.05f)
    )

    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1200,
                easing = androidx.compose.animation.core.FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )

    return Brush.linearGradient(
        colors = shimmerColors,
        start = androidx.compose.ui.geometry.Offset(translateAnim - 1000f, translateAnim - 1000f),
        end = androidx.compose.ui.geometry.Offset(translateAnim, translateAnim)
    )
}
