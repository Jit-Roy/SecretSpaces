package com.secretspaces32.android.ui.screens

import android.location.Location
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.gson.JsonPrimitive
import com.secretspaces32.android.BuildConfig
import com.secretspaces32.android.data.model.Secret
import com.secretspaces32.android.ui.components.*
import com.secretspaces32.android.ui.theme.*
import com.secretspaces32.android.utils.LocationHelper
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView
import org.maplibre.android.plugins.annotation.SymbolManager
import org.maplibre.android.plugins.annotation.SymbolOptions

@Composable
fun MapScreen(
    currentLocation: Location?,
    nearbySecrets: List<Secret>,
    isLoading: Boolean,
    onSecretClick: (Secret) -> Unit,
    onDropSecretClick: () -> Unit,
    onProfileClick: () -> Unit,
    onFeedClick: () -> Unit
) {
    var showSecretPreview by remember { mutableStateOf<Secret?>(null) }
    var bottomSheetOffset by remember { mutableFloatStateOf(0f) }
    val maxDragDistance = 300f

    Box(modifier = Modifier.fillMaxSize()) {
        // Map background
        if (currentLocation != null) {
            MapViewComposable(
                currentLocation = currentLocation,
                secrets = nearbySecrets,
                onMarkerClick = { showSecretPreview = it }
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(DarkBackground),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(
                        color = TealPrimary,
                        modifier = Modifier.size(42.dp)
                    )
                    Text(
                        text = "Loading map...",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "Fetching your location",
                        color = Color.White.copy(alpha = 0.6f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.End
        ) {
            IconButton(
                onClick = onProfileClick,
                modifier = Modifier
                    .size(42.dp)
                    .shadow(10.dp, CircleShape)
                    .background(DarkSurface.copy(alpha = 0.9f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = "Profile",
                    tint = TealPrimary,
                    modifier = Modifier.size(26.dp)
                )
            }
        }

        // Bottom sheet (Nearby secrets)
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .offset(y = bottomSheetOffset.dp)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragEnd = {
                            if (bottomSheetOffset < -maxDragDistance) onFeedClick()
                            bottomSheetOffset = 0f
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            val newOffset = bottomSheetOffset + dragAmount.y
                            bottomSheetOffset = newOffset.coerceIn(-maxDragDistance, 0f)
                        }
                    )
                }
                .background(
                    color = DarkSurface.copy(alpha = 0.98f),
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                )
                .padding(top = 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(4.dp)
                    .background(Color.White.copy(alpha = 0.3f), RoundedCornerShape(2.dp))
                    .align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📍 Nearby Secrets",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = TealPrimary.copy(alpha = 0.25f)
                ) {
                    Text(
                        text = "${nearbySecrets.size}",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = TealPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            when {
                isLoading -> {
                    LazyColumn(
                        modifier = Modifier.height(240.dp),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(2) { ShimmerLoadingCard() }
                    }
                }

                nearbySecrets.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("🔍", style = MaterialTheme.typography.displayMedium)
                            Text(
                                text = "No secrets nearby",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White
                            )
                            Text(
                                text = "Be the first to drop one!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.6f)
                            )
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.height(240.dp),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(nearbySecrets.take(3), key = { it.id }) { secret ->
                            CompactSecretCard(secret = secret) { onSecretClick(secret) }
                        }

                        if (nearbySecrets.size > 3) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onFeedClick() }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "See ${nearbySecrets.size - 3} more secrets",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = TealPrimary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowUp,
                                            contentDescription = null,
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

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp, end = 20.dp),
                contentAlignment = Alignment.BottomEnd
            ) {
                FloatingActionButton(
                    onClick = onDropSecretClick,
                    modifier = Modifier
                        .size(64.dp)
                        .shadow(14.dp, CircleShape),
                    containerColor = TealPrimary,
                    contentColor = Color.White,
                    shape = CircleShape
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Drop Secret",
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }

        // Secret Preview Modal
        AnimatedVisibility(
            visible = showSecretPreview != null,
            enter = fadeIn() + scaleIn(initialScale = 0.8f),
            exit = fadeOut() + scaleOut(targetScale = 0.8f)
        ) {
            showSecretPreview?.let { secret ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.6f))
                        .clickable { showSecretPreview = null },
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(0.9f),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        elevation = CardDefaults.cardElevation(10.dp)
                    ) {
                        Column(modifier = Modifier.padding(24.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "🔮 Secret",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = TealPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                                IconButton(
                                    onClick = { showSecretPreview = null },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Close",
                                        tint = Color.White.copy(alpha = 0.7f)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = secret.text,
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.White,
                                lineHeight = 24.sp
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                secret.distance?.let { distance ->
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.LocationOn,
                                            contentDescription = null,
                                            tint = AquaGreen,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = LocationHelper.formatDistance(distance),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = AquaGreen
                                        )
                                    }
                                }

                                Button(
                                    onClick = {
                                        showSecretPreview = null
                                        onSecretClick(secret)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("View Details")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CompactSecretCard(secret: Secret, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = DarkBackground.copy(alpha = 0.7f)
        ),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = secret.text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White,
                    maxLines = 2
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    secret.distance?.let { distance ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = AquaGreen,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = LocationHelper.formatDistance(distance),
                                style = MaterialTheme.typography.labelMedium,
                                color = AquaGreen
                            )
                        }
                    }
                    Text(
                        text = LocationHelper.formatTimestamp(secret.timestamp),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = TealPrimary.copy(alpha = 0.6f),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun MapViewComposable(
    currentLocation: Location,
    secrets: List<Secret>,
    onMarkerClick: (Secret) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val lifecycle = lifecycleOwner.lifecycle

    var mapView by remember { mutableStateOf<MapView?>(null) }
    var symbolManager by remember { mutableStateOf<SymbolManager?>(null) }
    var isMapReady by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            try {
                symbolManager?.onDestroy()
                mapView?.onPause()
                mapView?.onStop()
                mapView?.onDestroy()
            } catch (e: Exception) {
                println("Cleanup error: ${e.message}")
                e.printStackTrace()
            }
            symbolManager = null
            mapView = null
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(DarkBackground)) {
        AndroidView(
            factory = { ctx ->
                try {
                    println("Creating MapView...")

                    // Initialize MapLibre BEFORE creating MapView
                    try {
                        MapLibre.getInstance(ctx)
                        println("MapLibre already initialized")
                    } catch (e: Exception) {
                        // MapLibre not initialized, initialize it now
                        println("Initializing MapLibre...")
                        MapLibre.getInstance(ctx)
                        println("MapLibre initialized successfully")
                    }

                    MapView(ctx).apply {
                        this.id = android.view.View.generateViewId()
                        setBackgroundColor(android.graphics.Color.BLACK)

                        // Store reference immediately
                        mapView = this

                        // Initialize lifecycle immediately in factory
                        this.onCreate(null)
                        this.onStart()
                        this.onResume()

                        println("MapView created and lifecycle initialized")

                        // Setup map directly in factory using post to ensure view is attached
                        this.post {
                            println("Post called - setting up map...")
                            try {
                                this.getMapAsync { mapLibreMap ->
                                    println("✅ getMapAsync callback triggered!")
                                    try {
                                        println("Loading style...")
                                        val styleUrl = "https://api.maptiler.com/maps/streets-v2/style.json?key=${BuildConfig.MAPTILER_API_KEY}"

                                        mapLibreMap.setStyle(styleUrl) { style ->
                                            println("✅ Style callback triggered!")
                                            try {
                                                println("Adding markers...")
                                                val position = LatLng(currentLocation.latitude, currentLocation.longitude)
                                                mapLibreMap.moveCamera(CameraUpdateFactory.newLatLngZoom(position, 14.0))

                                                // Clean up old symbol manager
                                                symbolManager?.onDestroy()

                                                // Create symbol manager
                                                val newSymbolManager = SymbolManager(this, mapLibreMap, style)
                                                newSymbolManager.iconAllowOverlap = true
                                                newSymbolManager.textAllowOverlap = true
                                                symbolManager = newSymbolManager

                                                // Current location marker
                                                newSymbolManager.create(
                                                    SymbolOptions()
                                                        .withLatLng(position)
                                                        .withTextField("📍")
                                                        .withTextSize(18f)
                                                        .withTextColor("rgb(0, 217, 208)")
                                                )

                                                // Secret markers
                                                secrets.forEach { secret ->
                                                    newSymbolManager.create(
                                                        SymbolOptions()
                                                            .withLatLng(LatLng(secret.latitude, secret.longitude))
                                                            .withTextField("🔮")
                                                            .withTextSize(18f)
                                                            .withTextColor("rgb(123, 97, 255)")
                                                            .withData(JsonPrimitive(secret.id))
                                                    )
                                                }

                                                newSymbolManager.addClickListener { symbol ->
                                                    symbol.data?.let { data ->
                                                        secrets.find { it.id == data.asString }?.let(onMarkerClick)
                                                    }
                                                    true
                                                }

                                                isMapReady = true
                                                println("✅ Map setup complete!")
                                            } catch (e: Exception) {
                                                println("❌ Error in style callback: ${e.message}")
                                                e.printStackTrace()
                                                isMapReady = true
                                            }
                                        }
                                    } catch (e: Exception) {
                                        println("❌ Error setting style: ${e.message}")
                                        e.printStackTrace()
                                        isMapReady = true
                                    }
                                }
                            } catch (e: Exception) {
                                println("❌ Error in getMapAsync: ${e.message}")
                                e.printStackTrace()
                                isMapReady = true
                            }
                        }
                    }
                } catch (e: Exception) {
                    println("MapView creation error: ${e.message}")
                    e.printStackTrace()
                    // Return a fallback view if map creation fails
                    android.widget.FrameLayout(ctx).apply {
                        setBackgroundColor(android.graphics.Color.BLACK)
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Show loading indicator while map initializes
        if (!isMapReady) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(color = TealPrimary)
                    Text(
                        text = "Initializing map...",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }

    // Timeout fallback
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(15000)
        if (!isMapReady) {
            println("⚠️ Map loading timeout after 15 seconds - forcing show")
            isMapReady = true
        }
    }

    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            println("Lifecycle event: $event")
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    try {
                        mapView?.onPause()
                    } catch (e: Exception) {
                        println("onPause error: ${e.message}")
                        e.printStackTrace()
                    }
                }
                Lifecycle.Event.ON_RESUME -> {
                    try {
                        mapView?.onResume()
                    } catch (e: Exception) {
                        println("onResume error: ${e.message}")
                        e.printStackTrace()
                    }
                }
                Lifecycle.Event.ON_STOP -> {
                    try {
                        mapView?.onStop()
                    } catch (e: Exception) {
                        println("onStop error: ${e.message}")
                        e.printStackTrace()
                    }
                }
                Lifecycle.Event.ON_START -> {
                    try {
                        mapView?.onStart()
                    } catch (e: Exception) {
                        println("onStart error: ${e.message}")
                        e.printStackTrace()
                    }
                }
                Lifecycle.Event.ON_DESTROY -> {
                    try {
                        println("Calling onDestroy on MapView")
                        symbolManager?.onDestroy()
                        symbolManager = null
                        mapView?.onDestroy()
                        mapView = null
                    } catch (e: Exception) {
                        println("onDestroy error: ${e.message}")
                        e.printStackTrace()
                    }
                }
                else -> {}
            }
        }

        lifecycle.addObserver(observer)

        onDispose {
            lifecycle.removeObserver(observer)
        }
    }
}
