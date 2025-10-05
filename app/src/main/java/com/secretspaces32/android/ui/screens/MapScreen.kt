package com.secretspaces32.android.ui.screens

import android.graphics.drawable.BitmapDrawable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.secretspaces32.android.R
import com.secretspaces32.android.data.model.Secret
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    secrets: List<Secret>,
    currentLatitude: Double?,
    currentLongitude: Double?,
    onSecretClick: (Secret) -> Unit,
    selectedSecret: Secret?
) {
    val context = LocalContext.current
    var mapView by remember { mutableStateOf<MapView?>(null) }

    LaunchedEffect(Unit) {
        Configuration.getInstance().userAgentValue = context.packageName
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                MapView(ctx).apply {
                    mapView = this
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)

                    controller.setZoom(15.0)

                    // Set initial position
                    if (currentLatitude != null && currentLongitude != null) {
                        controller.setCenter(GeoPoint(currentLatitude, currentLongitude))
                    }
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { map ->
                map.overlays.clear()

                // Add current location marker
                if (currentLatitude != null && currentLongitude != null) {
                    val userMarker = Marker(map).apply {
                        position = GeoPoint(currentLatitude, currentLongitude)
                        title = "You are here"
                        snippet = "Your current location"
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    }
                    map.overlays.add(userMarker)

                    // Center map on user location
                    map.controller.animateTo(GeoPoint(currentLatitude, currentLongitude))
                }

                // Add secret markers
                secrets.forEach { secret ->
                    val marker = Marker(map).apply {
                        position = GeoPoint(secret.latitude, secret.longitude)
                        title = "Secret"
                        snippet = secret.text.take(50)
                        setOnMarkerClickListener { _, _ ->
                            onSecretClick(secret)
                            true
                        }
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    }
                    map.overlays.add(marker)
                }

                map.invalidate()
            }
        )

        // Show selected secret details
        selectedSecret?.let { secret ->
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                SecretDetailContent(secret = secret)
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            mapView?.onDetach()
        }
    }
}
