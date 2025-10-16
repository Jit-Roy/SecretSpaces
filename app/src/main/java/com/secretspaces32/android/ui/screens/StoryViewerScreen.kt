package com.secretspaces32.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
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
        // Story content - Image or Text-only with colored background
        if (currentStory.imageUrl != null) {
            // Image Story - Full screen
            AsyncImage(
                model = currentStory.imageUrl,
                contentDescription = "Story",
                modifier = Modifier
                    .fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        } else {
            // Text-only Story with colored background
            val backgroundColors = listOf(
                Brush.linearGradient(listOf(Color(0xFFFF6B6B), Color(0xFFFF8E53))),
                Brush.linearGradient(listOf(Color(0xFF4E54C8), Color(0xFF8F94FB))),
                Brush.linearGradient(listOf(Color(0xFF11998E), Color(0xFF38EF7D))),
                Brush.linearGradient(listOf(Color(0xFFFA8BFF), Color(0xFF2BD2FF))),
                Brush.linearGradient(listOf(Color(0xFFF093FB), Color(0xFFF5576C))),
                Brush.linearGradient(listOf(Color(0xFF4776E6), Color(0xFF8E54E9)))
            )
            val selectedBackground = backgroundColors[currentStory.id.hashCode().mod(backgroundColors.size)]

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(selectedBackground),
                contentAlignment = Alignment.Center
            ) {
                currentStory.caption?.let { text ->
                    Text(
                        text = text,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(32.dp)
                    )
                }
            }
        }

        // Tap areas for navigation - MOVED BEFORE TOP UI to be behind it
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

        // Top section with progress bars and close button - ON TOP of tap areas
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
                                .background(Color(0xFFFF4D4D).copy(alpha = 0.15f), shape = CircleShape)
                                .border(2.dp, Color(0xFFFF4D4D), CircleShape)
                                .clip(CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Profile",
                                tint = Color(0xFFFF4D4D),
                                modifier = Modifier.size(24.dp)
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

                // Close button with explicit clickable to prevent tap-through
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
                        .clickable {
                            println("DEBUG: Close button clicked")
                            onClose()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // Caption at bottom - Show for IMAGE stories with captions
        if (currentStory.imageUrl != null && !currentStory.caption.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.8f)
                            )
                        )
                    )
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                contentAlignment = Alignment.Center  // Center the text inside the box
            ) {
                Text(
                    text = currentStory.caption,
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Normal,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center  // Center align the text
                )
            }
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
