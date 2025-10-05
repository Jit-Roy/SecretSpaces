package com.secretspaces32.android.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.secretspaces32.android.data.model.Secret
import com.secretspaces32.android.utils.LocationHelper

@Composable
fun MySecretsScreen(
    secrets: List<Secret>,
    isLoading: Boolean,
    onSecretClick: (Secret) -> Unit = {}
) {
    Box(modifier = Modifier.fillMaxSize()) {
        if (isLoading && secrets.isEmpty()) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center)
            )
        } else if (secrets.isEmpty()) {
            Text(
                text = "You haven't dropped any secrets yet.\nGo to a location and drop your first secret!",
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(32.dp),
                style = MaterialTheme.typography.bodyLarge
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(secrets) { secret ->
                    MySecretCard(secret = secret, onClick = { onSecretClick(secret) })
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MySecretCard(secret: Secret, onClick: () -> Unit = {}) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header with timestamp and location
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = LocationHelper.formatTimestamp(secret.timestamp),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
                Text(
                    text = "📍 %.4f, %.4f".format(secret.latitude, secret.longitude),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

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

            // Engagement stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "❤️ ${secret.likeCount}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "💬 ${secret.commentCount}",
                    style = MaterialTheme.typography.bodyMedium
                )
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
}
