package com.secretspaces32.android.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.annotations.MarkerOptions
import com.mapbox.mapboxsdk.annotations.IconFactory
import com.secretspaces32.android.BuildConfig
import com.secretspaces32.android.R
import com.secretspaces32.android.data.model.Secret
import com.secretspaces32.android.ui.components.*
import com.secretspaces32.android.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun MapScreen(
    secrets: List<Secret>,
    currentLatitude: Double?,
    currentLongitude: Double?,
    onSecretClick: (Secret) -> Unit,
    selectedSecret: Secret?,
    onLocationPermissionGranted: () -> Unit = {}
) {
    val context = LocalContext.current
    var mapView by remember { mutableStateOf<MapView?>(null) }
    var hasInitializedPosition by remember { mutableStateOf(false) }
    var showLocationLoading by remember { mutableStateOf(true) }

    // Location permissions state
    val locationPermissions = rememberMultiplePermissionsState(
        permissions = listOf(
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )

    // Hide loading indicator after 5 seconds regardless of location status
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(5000)
        showLocationLoading = false
    }

    // Hide loading when location is obtained
    LaunchedEffect(currentLatitude, currentLongitude) {
        if (currentLatitude != null && currentLongitude != null) {
            showLocationLoading = false
        }
    }

    // Trigger location update when permission is granted
    LaunchedEffect(locationPermissions.allPermissionsGranted) {
        if (locationPermissions.allPermissionsGranted) {
            onLocationPermissionGranted()
            showLocationLoading = true
            kotlinx.coroutines.delay(5000)
            showLocationLoading = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                // Initialize Mapbox/MapLibre instance
                Mapbox.getInstance(ctx)

                MapView(ctx).apply {
                    mapView = this
                    onCreate(null)

                    // Set initial camera position - use actual location if available, otherwise default
                    val initialLat = currentLatitude ?: 20.5937
                    val initialLng = currentLongitude ?: 78.9629

                    getMapAsync { mapboxMap ->
                        // Build MapTiler style URL with API key
                        val styleUrl = "https://api.maptiler.com/maps/streets-v2/style.json?key=${BuildConfig.MAPTILER_API_KEY}"

                        mapboxMap.setStyle(Style.Builder().fromUri(styleUrl)) { style ->
                            // Set camera position
                            val cameraPosition = CameraPosition.Builder()
                                .target(LatLng(initialLat, initialLng))
                                .zoom(if (currentLatitude != null) 15.0 else 5.0)
                                .build()
                            mapboxMap.cameraPosition = cameraPosition

                            // Add current location marker if available
                            if (currentLatitude != null && currentLongitude != null) {
                                try {
                                    val iconFactory = IconFactory.getInstance(ctx)
                                    val icon = iconFactory.fromResource(R.drawable.ic_launcher_foreground)

                                    mapboxMap.addMarker(
                                        MarkerOptions()
                                            .position(LatLng(currentLatitude, currentLongitude))
                                            .title("You are here")
                                            .icon(icon)
                                    )
                                } catch (e: Exception) {
                                    // Fallback: add marker without custom icon
                                    mapboxMap.addMarker(
                                        MarkerOptions()
                                            .position(LatLng(currentLatitude, currentLongitude))
                                            .title("📍 You are here")
                                    )
                                }
                            }

                            // Add markers for secrets
                            secrets.forEach { secret ->
                                mapboxMap.addMarker(
                                    MarkerOptions()
                                        .position(LatLng(secret.latitude, secret.longitude))
                                        .title("🤫 ${secret.text.take(30)}...")
                                )
                            }

                            // Add marker click listener
                            mapboxMap.setOnMarkerClickListener { marker ->
                                // Check if it's a secret marker
                                val clickedSecret = secrets.find { secret ->
                                    marker.position.latitude == secret.latitude &&
                                            marker.position.longitude == secret.longitude
                                }
                                clickedSecret?.let { onSecretClick(it) }
                                true
                            }
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { map ->
                // Update camera position when location becomes available
                if (currentLatitude != null && currentLongitude != null && !hasInitializedPosition) {
                    map.getMapAsync { mapboxMap ->
                        mapboxMap.animateCamera(
                            CameraUpdateFactory.newLatLngZoom(
                                LatLng(currentLatitude, currentLongitude),
                                15.0
                            ),
                            2000 // 2 second animation
                        )
                        hasInitializedPosition = true

                        // Add/update current location marker
                        mapboxMap.clear()

                        // Add current location marker
                        try {
                            val iconFactory = IconFactory.getInstance(context)
                            val icon = iconFactory.fromResource(R.drawable.ic_launcher_foreground)

                            mapboxMap.addMarker(
                                MarkerOptions()
                                    .position(LatLng(currentLatitude, currentLongitude))
                                    .title("You are here")
                                    .icon(icon)
                            )
                        } catch (e: Exception) {
                            mapboxMap.addMarker(
                                MarkerOptions()
                                    .position(LatLng(currentLatitude, currentLongitude))
                                    .title("📍 You are here")
                            )
                        }

                        // Re-add secret markers
                        secrets.forEach { secret ->
                            mapboxMap.addMarker(
                                MarkerOptions()
                                    .position(LatLng(secret.latitude, secret.longitude))
                                    .title("🤫 ${secret.text.take(30)}...")
                            )
                        }
                    }
                }

                // Update markers when secrets change
                if (hasInitializedPosition) {
                    map.getMapAsync { mapboxMap ->
                        mapboxMap.clear()

                        // Re-add current location marker
                        if (currentLatitude != null && currentLongitude != null) {
                            try {
                                val iconFactory = IconFactory.getInstance(context)
                                val icon = iconFactory.fromResource(R.drawable.ic_launcher_foreground)

                                mapboxMap.addMarker(
                                    MarkerOptions()
                                        .position(LatLng(currentLatitude, currentLongitude))
                                        .title("You are here")
                                        .icon(icon)
                                )
                            } catch (e: Exception) {
                                mapboxMap.addMarker(
                                    MarkerOptions()
                                        .position(LatLng(currentLatitude, currentLongitude))
                                        .title("📍 You are here")
                                )
                            }
                        }

                        // Add secret markers
                        secrets.forEach { secret ->
                            mapboxMap.addMarker(
                                MarkerOptions()
                                    .position(LatLng(secret.latitude, secret.longitude))
                                    .title("🤫 ${secret.text.take(30)}...")
                            )
                        }
                    }
                }
            }
        )

        // Premium location status indicator
        AnimatedVisibility(
            visible = currentLatitude == null || currentLongitude == null,
            modifier = Modifier.align(Alignment.TopCenter),
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut() + slideOutVertically()
        ) {
            if (showLocationLoading && locationPermissions.allPermissionsGranted) {
                // Loading card
                GlassmorphicCard(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PulsingDot(color = MintGreen)
                        Text(
                            "Getting your location...",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            } else if (!locationPermissions.allPermissionsGranted) {
                // Permission request card with premium styling
                GlassmorphicCard(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(0.9f)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .background(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            DeepPurple.copy(alpha = 0.3f),
                                            Color.Transparent
                                        )
                                    ),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MyLocation,
                                contentDescription = "Location",
                                modifier = Modifier.size(48.dp),
                                tint = DeepPurple
                            )
                        }

                        Text(
                            "Location Access Required",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Text(
                            "To discover and post secrets nearby, we need access to your location.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        PremiumButton(
                            text = "Grant Location Access",
                            onClick = {
                                locationPermissions.launchMultiplePermissionRequest()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        )

                        if (locationPermissions.shouldShowRationale) {
                            TextButton(
                                onClick = {
                                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                        data = Uri.fromParts("package", context.packageName, null)
                                    }
                                    context.startActivity(intent)
                                }
                            ) {
                                Text(
                                    "Open Settings",
                                    color = ElectricBlue
                                )
                            }
                        }
                    }
                }
            } else {
                // Location unavailable card
                GlassmorphicCard(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(0.9f)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "📍 Location Unavailable",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "Please enable GPS/Location in your device settings",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        TextButton(
                            onClick = {
                                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                                context.startActivity(intent)
                            }
                        ) {
                            Text(
                                "Open Location Settings",
                                color = ElectricBlue
                            )
                        }
                    }
                }
            }
        }

        // Show selected secret details with premium card
        AnimatedVisibility(
            visible = selectedSecret != null,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it })
        ) {
            selectedSecret?.let { secret ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .shadow(
                            elevation = 24.dp,
                            shape = RoundedCornerShape(24.dp),
                            ambientColor = DeepPurple.copy(alpha = 0.5f),
                            spotColor = ElectricBlue.copy(alpha = 0.5f)
                        ),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Box {
                        // Gradient accent
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .background(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(DeepPurple, ElectricBlue, CoralPink)
                                    )
                                )
                        )

                        SecretCardContent(secret = secret)
                    }
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            mapView?.onDestroy()
        }
    }
}
