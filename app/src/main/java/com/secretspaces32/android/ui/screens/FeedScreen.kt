package com.secretspaces32.android.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.secretspaces32.android.data.model.Secret
import com.secretspaces32.android.ui.components.*
import com.secretspaces32.android.ui.theme.*
import com.secretspaces32.android.utils.LocationHelper

enum class FeedFilter {
    RECENT, POPULAR, NEARBY
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    secrets: List<Secret>,
    isLoading: Boolean,
    onSecretClick: (Secret) -> Unit,
    onBackClick: () -> Unit
) {
    var selectedFilter by remember { mutableStateOf(FeedFilter.RECENT) }

    val filteredSecrets = remember(secrets, selectedFilter) {
        when (selectedFilter) {
            FeedFilter.RECENT -> secrets.sortedByDescending { it.timestamp }
            FeedFilter.POPULAR -> secrets.sortedByDescending { it.likeCount + it.commentCount }
            FeedFilter.NEARBY -> secrets.sortedBy { it.distance ?: Double.MAX_VALUE }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Animated gradient background
        AnimatedGradientBackground(modifier = Modifier.fillMaxSize())

        Column(modifier = Modifier.fillMaxSize()) {
            // Top Bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = DarkSurface.copy(alpha = 0.95f),
                tonalElevation = 8.dp
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(16.dp),
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
                            text = "Feed",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        // Placeholder for symmetry
                        Spacer(modifier = Modifier.width(48.dp))
                    }

                    // Filter Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        FilterChip(
                            selected = selectedFilter == FeedFilter.RECENT,
                            onClick = { selectedFilter = FeedFilter.RECENT },
                            label = { Text("🕒 Recent") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = TealPrimary,
                                selectedLabelColor = Color.White,
                                containerColor = DarkBackground,
                                labelColor = Color.White.copy(alpha = 0.7f)
                            )
                        )

                        FilterChip(
                            selected = selectedFilter == FeedFilter.POPULAR,
                            onClick = { selectedFilter = FeedFilter.POPULAR },
                            label = { Text("🔥 Popular") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CoralPink,
                                selectedLabelColor = Color.White,
                                containerColor = DarkBackground,
                                labelColor = Color.White.copy(alpha = 0.7f)
                            )
                        )

                        FilterChip(
                            selected = selectedFilter == FeedFilter.NEARBY,
                            onClick = { selectedFilter = FeedFilter.NEARBY },
                            label = { Text("📍 Nearby") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SoftBlue,
                                selectedLabelColor = Color.White,
                                containerColor = DarkBackground,
                                labelColor = Color.White.copy(alpha = 0.7f)
                            )
                        )
                    }
                }
            }

            // Secrets Feed
            if (isLoading && secrets.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(48.dp),
                            color = TealPrimary,
                            strokeWidth = 4.dp
                        )
                        Text(
                            text = "Loading secrets...",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                    }
                }
            } else if (filteredSecrets.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    GlassmorphicCard {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "🤫",
                                style = MaterialTheme.typography.displayLarge
                            )
                            Text(
                                text = "No secrets found",
                                style = MaterialTheme.typography.headlineSmall,
                                color = Color.White
                            )
                            Text(
                                text = "Check back later or drop your own!",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(
                        items = filteredSecrets,
                        key = { it.id }
                    ) { secret ->
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn() + slideInVertically(),
                            exit = fadeOut() + slideOutVertically()
                        ) {
                            FeedSecretCard(
                                secret = secret,
                                onClick = { onSecretClick(secret) }
                            )
                        }
                    }

                    // Bottom padding
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedSecretCard(secret: Secret, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(24.dp),
                ambientColor = TealPrimary.copy(alpha = 0.2f),
                spotColor = AquaGreen.copy(alpha = 0.2f)
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = DarkSurface.copy(alpha = 0.95f)
        ),
        onClick = onClick
    ) {
        Box {
            // Gradient accent on top
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(TealPrimary, AquaGreen, SoftTeal)
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header with user info and timestamp
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (secret.isAnonymous) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.5f),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Anonymous",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        } else {
                            Text(
                                text = secret.username,
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White
                            )
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        secret.distance?.let { distance ->
                            Text(
                                text = LocationHelper.formatDistance(distance),
                                style = MaterialTheme.typography.labelSmall,
                                color = AquaGreen
                            )
                        }

                        Text(
                            text = "•",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.5f)
                        )

                        Text(
                            text = LocationHelper.formatTimestamp(secret.timestamp),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Secret text
                Text(
                    text = secret.text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White
                )

                // Image if available
                secret.imageUrl?.let { imageUrl ->
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp)
                            .shadow(
                                elevation = 8.dp,
                                shape = RoundedCornerShape(16.dp)
                            ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        AsyncImage(
                            model = imageUrl,
                            contentDescription = "Secret image",
                            modifier = Modifier.fillMaxWidth(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                GradientDivider()

                Spacer(modifier = Modifier.height(16.dp))

                // Engagement stats
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    Surface(
                        color = CoralPink.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = "Likes",
                                tint = CoralPink,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "${secret.likeCount}",
                                style = MaterialTheme.typography.labelLarge,
                                color = CoralPink
                            )
                        }
                    }

                    Surface(
                        color = SoftBlue.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChatBubble,
                                contentDescription = "Comments",
                                tint = SoftBlue,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "${secret.commentCount}",
                                style = MaterialTheme.typography.labelLarge,
                                color = SoftBlue
                            )
                        }
                    }

                    Surface(
                        color = TealPrimary.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Visibility,
                                contentDescription = "Views",
                                tint = TealPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

