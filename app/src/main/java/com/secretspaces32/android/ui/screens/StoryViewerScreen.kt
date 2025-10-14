package com.secretspaces32.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.secretspaces32.android.data.model.Story
import kotlinx.coroutines.delay

@Composable
fun StoryViewerScreen(
    stories: List<Story>,
    currentIndex: Int,
    onNextStory: () -> Unit,
    onPreviousStory: () -> Unit,
    onClose: () -> Unit
) {
    println("DEBUG: StoryViewerScreen - stories count: ${stories.size}")

    if (stories.isEmpty()) {
        println("DEBUG: No stories to display, closing viewer")
        LaunchedEffect(Unit) {
            onClose()
        }
        return
    }

    val currentStory = stories.getOrNull(currentIndex) ?: stories.first()
    println("DEBUG: Displaying story: ${currentStory.id}")
    var progress by remember(currentIndex) { mutableStateOf(0f) }

    // Auto-advance story after 5 seconds
    LaunchedEffect(currentIndex) {
        progress = 0f
        val duration = 5000f // 5 seconds
        val steps = 50
        val stepDuration = duration / steps

        repeat(steps) { step ->
            delay(stepDuration.toLong())
            progress = (step + 1) / steps.toFloat()
        }

        // Auto advance to next story
        if (currentIndex < stories.size - 1) {
            onNextStory()
        } else {
            onClose()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Story Image - Full screen
        AsyncImage(
            model = currentStory.imageUrl,
            contentDescription = "Story",
            modifier = Modifier
                .fillMaxSize()
                .clickable {
                    // Tap right side to go next, left side to go previous
                },
            contentScale = ContentScale.Fit
        )

        // Gradient overlay for better text visibility
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.7f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Top section with progress bars and close button
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(16.dp)
        ) {
            // Progress bars
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                stories.forEachIndexed { index, _ ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(3.dp)
                            .background(
                                color = when {
                                    index < currentIndex -> Color.White
                                    index == currentIndex -> Color.White.copy(alpha = progress)
                                    else -> Color.White.copy(alpha = 0.3f)
                                },
                                shape = CircleShape
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // User info and close button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Profile picture
                    if (currentStory.userProfilePicture != null) {
                        AsyncImage(
                            model = currentStory.userProfilePicture,
                            contentDescription = "Profile",
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color.Gray),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = currentStory.username.firstOrNull()?.toString()?.uppercase() ?: "?",
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Username and time
                    Column {
                        Text(
                            text = currentStory.username,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = getTimeAgo(currentStory.timestamp),
                            color = Color.White.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                // Close button
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White
                    )
                }
            }
        }

        // Caption at bottom
        currentStory.caption?.let { caption ->
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.7f)
                            )
                        )
                    )
                    .padding(16.dp)
            ) {
                Text(
                    text = caption,
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        // Tap areas for navigation
        Row(modifier = Modifier.fillMaxSize()) {
            // Left side - previous story
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable {
                        if (currentIndex > 0) {
                            onPreviousStory()
                        } else {
                            onClose()
                        }
                    }
            )

            // Right side - next story
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable {
                        if (currentIndex < stories.size - 1) {
                            onNextStory()
                        } else {
                            onClose()
                        }
                    }
            )
        }
    }
}

private fun getTimeAgo(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 60_000 -> "Just now"
        diff < 3600_000 -> "${diff / 60_000}m ago"
        diff < 86400_000 -> "${diff / 3600_000}h ago"
        else -> "${diff / 86400_000}d ago"
    }
}
