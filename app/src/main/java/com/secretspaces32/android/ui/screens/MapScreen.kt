package com.secretspaces32.android.ui.screens

import android.Manifest
import android.location.Location
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.isGranted
import com.google.gson.JsonPrimitive
import com.secretspaces32.android.BuildConfig
import com.secretspaces32.android.data.model.Secret
import com.secretspaces32.android.ui.components.*
import com.secretspaces32.android.ui.theme.*
import com.secretspaces32.android.utils.LocationHelper
import kotlinx.coroutines.delay
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView
import org.maplibre.android.plugins.annotation.SymbolManager
import org.maplibre.android.plugins.annotation.SymbolOptions

// Define 3 states for the bottom sheet
enum class SheetState { COLLAPSED, HALF_EXPANDED, FULLY_EXPANDED }

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MapScreen(
    currentLocation: Location?,
    nearbySecrets: List<Secret>,
    onSecretClick: (Secret) -> Unit,
    onSheetStateChange: (String) -> Unit = {},
    onLocationPermissionGranted: () -> Unit = {}
) {
    var showSecretPreview by remember { mutableStateOf<Secret?>(null) }
    var focusedSecret by remember { mutableStateOf<Secret?>(null) }

    // Location permissions state
    val locationPermissions = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )

    // Update location when permission is granted
    LaunchedEffect(locationPermissions.allPermissionsGranted) {
        if (locationPermissions.allPermissionsGranted && currentLocation == null) {
            onLocationPermissionGranted()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Map View - Only show if permissions are granted
        if (locationPermissions.allPermissionsGranted) {
            // Show map if we have location OR show loading if location is being fetched
            if (currentLocation != null) {
                MapViewComposable(
                    currentLocation = currentLocation,
                    secrets = nearbySecrets,
                    onMarkerClick = { showSecretPreview = it },
                    focusedSecret = focusedSecret,
                    onFocusHandled = { focusedSecret = null }
                )
            } else {
                // Permissions granted but location still loading
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.foundation.Image(
                        painter = painterResource(id = com.secretspaces32.android.R.drawable.map_placeholder),
                        contentDescription = "Map Placeholder",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f))
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(
                            color = TealPrimary,
                            modifier = Modifier.size(42.dp)
                        )
                        Text(
                            text = "Getting your location...",
                            color = Color.White,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        } else {
            // Show map placeholder with location permission request
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                // Map placeholder image
                androidx.compose.foundation.Image(
                    painter = painterResource(id = com.secretspaces32.android.R.drawable.map_placeholder),
                    contentDescription = "Map Placeholder",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Dark overlay for better text visibility
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f))
                )

                // Permission request UI
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(32.dp)
                ) {
                    // Map icon
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Location",
                        tint = TealPrimary,
                        modifier = Modifier.size(80.dp)
                    )

                    Text(
                        text = "Enable Location",
                        color = Color.White,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "To view nearby secrets on the map, we need access to your location.",
                        color = Color.White.copy(alpha = 0.9f),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Permission request button
                    Button(
                        onClick = {
                            locationPermissions.launchMultiplePermissionRequest()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TealPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .height(56.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MyLocation,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Allow Location Access",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Show different message if permission was denied
                    if (locationPermissions.permissions.any { !it.status.isGranted } && !locationPermissions.allPermissionsGranted) {
                        Text(
                            text = "Location permission is required to show the map. Please grant permission in the dialog above.",
                            color = Color.White.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )
                    }
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
fun MapViewComposable(
    currentLocation: Location,
    secrets: List<Secret>,
    onMarkerClick: (Secret) -> Unit,
    focusedSecret: Secret? = null,
    onFocusHandled: () -> Unit = {}
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val lifecycle = lifecycleOwner.lifecycle

    var mapView by remember { mutableStateOf<MapView?>(null) }
    var symbolManager by remember { mutableStateOf<SymbolManager?>(null) }
    var isMapReady by remember { mutableStateOf(false) }
    var mapLibreMapInstance by remember { mutableStateOf<org.maplibre.android.maps.MapLibreMap?>(null) }

    // Handle focusing on a specific secret when requested
    LaunchedEffect(focusedSecret) {
        focusedSecret?.let { secret ->
            mapLibreMapInstance?.let { map ->
                try {
                    val secretPosition = LatLng(secret.latitude, secret.longitude)
                    map.animateCamera(
                        CameraUpdateFactory.newLatLngZoom(secretPosition, 16.0),
                        1500
                    )
                    onFocusHandled()
                } catch (e: Exception) {
                    onFocusHandled()
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                symbolManager?.onDestroy()
                mapView?.onPause()
                mapView?.onStop()
                mapView?.onDestroy()
            } catch (_: Exception) {
            }
            symbolManager = null
            mapView = null
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(DarkBackground)) {
        AndroidView(
            factory = { ctx ->
                try {

                    // Initialize MapLibre BEFORE creating MapView
                    try {
                        MapLibre.getInstance(ctx)
                    } catch (_: Exception) {
                        // MapLibre not initialized, initialize it now
                        MapLibre.getInstance(ctx)
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

                        // Setup map directly in factory using post to ensure view is attached
                        this.post {
                            try {
                                this.getMapAsync { mapLibreMap ->
                                    // Store the map instance for camera control
                                    mapLibreMapInstance = mapLibreMap
                                    try {
                                        val styleUrl = "https://api.maptiler.com/maps/streets-v2/style.json?key=${BuildConfig.MAPTILER_API_KEY}"

                                        mapLibreMap.setStyle(styleUrl) { style ->
                                            try {
                                                // Set initial camera position to user's location
                                                mapLibreMap.moveCamera(
                                                    CameraUpdateFactory.newLatLngZoom(
                                                        LatLng(currentLocation.latitude, currentLocation.longitude),
                                                        14.0
                                                    )
                                                )

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
                                                        .withLatLng(LatLng(currentLocation.latitude, currentLocation.longitude))
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
                                            } catch (e: Exception) {
                                                isMapReady = true
                                            }
                                        }
                                    } catch (e: Exception) {
                                        isMapReady = true
                                    }
                                }
                            } catch (e: Exception) {
                                isMapReady = true
                            }
                        }
                    }
                } catch (e: Exception) {
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
        delay(15000)
        if (!isMapReady) {
            isMapReady = true
        }
    }

    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    try {
                        mapView?.onPause()
                    } catch (_: Exception) {
                    }
                }
                Lifecycle.Event.ON_RESUME -> {
                    try {
                        mapView?.onResume()
                    } catch (_: Exception) {
                    }
                }
                Lifecycle.Event.ON_STOP -> {
                    try {
                        mapView?.onStop()
                    } catch (_: Exception) {
                    }
                }
                Lifecycle.Event.ON_START -> {
                    try {
                        mapView?.onStart()
                    } catch (_: Exception) {
                    }
                }
                Lifecycle.Event.ON_DESTROY -> {
                    try {
                        symbolManager?.onDestroy()
                        symbolManager = null
                        mapView?.onDestroy()
                        mapView = null
                    } catch (_: Exception) {
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