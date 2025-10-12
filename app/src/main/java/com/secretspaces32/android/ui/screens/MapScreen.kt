package com.secretspaces32.android.ui.screens

import android.location.Location
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
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
import kotlinx.coroutines.delay
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView
import org.maplibre.android.plugins.annotation.SymbolManager
import org.maplibre.android.plugins.annotation.SymbolOptions

// Define 3 states for the bottom sheet
enum class SheetState { COLLAPSED, HALF_EXPANDED, FULLY_EXPANDED }

// Custom Saver for SheetState enum
private val SheetStateSaver = Saver<SheetState, String>(
    save = { it.name },
    restore = { SheetState.valueOf(it) }
)

@Composable
fun MapScreen(
    currentLocation: Location?,
    nearbySecrets: List<Secret>,
    isLoading: Boolean,
    onSecretClick: (Secret) -> Unit,
    onDropSecretClick: () -> Unit,
    onProfileClick: () -> Unit,
    @Suppress("UNUSED_PARAMETER") onFeedClick: () -> Unit,
    initialSheetState: String = "COLLAPSED",
    onSheetStateChange: (String) -> Unit = {}
) {
    var showSecretPreview by remember { mutableStateOf<Secret?>(null) }
    var sheetState by remember(initialSheetState) {
        mutableStateOf(
            when(initialSheetState) {
                "FULLY_EXPANDED" -> SheetState.FULLY_EXPANDED
                "HALF_EXPANDED" -> SheetState.HALF_EXPANDED
                else -> SheetState.COLLAPSED
            }
        )
    }

    // Notify parent when sheet state changes
    LaunchedEffect(sheetState) {
        onSheetStateChange(sheetState.name)
    }

    var isRefreshing by remember { mutableStateOf(false) }
    val lazyListState = rememberLazyListState()
    var focusedSecret by remember { mutableStateOf<Secret?>(null) }

    val density = androidx.compose.ui.platform.LocalDensity.current
    val windowInfo = LocalWindowInfo.current
    val screenHeightPx = windowInfo.containerSize.height.toFloat()

    // Define the three state heights
    val collapsedHeight = with(density) { 100.dp.toPx() }
    val halfExpandedHeight = screenHeightPx * 0.5f
    val fullyExpandedHeight = screenHeightPx - with(density) { 60.dp.toPx() }

    val targetOffset = when (sheetState) {
        SheetState.FULLY_EXPANDED -> screenHeightPx - fullyExpandedHeight
        SheetState.HALF_EXPANDED -> screenHeightPx - halfExpandedHeight
        SheetState.COLLAPSED -> screenHeightPx - collapsedHeight
    }

    val animatedOffset by androidx.compose.animation.core.animateFloatAsState(
        targetValue = targetOffset,
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
            stiffness = androidx.compose.animation.core.Spring.StiffnessLow
        ),
        label = "sheetOffset"
    )

    var dragAmount by remember { mutableFloatStateOf(0f) }

    // Handle pull to refresh
    val canScrollUp = lazyListState.canScrollBackward
    LaunchedEffect(canScrollUp, isRefreshing) {
        if (isRefreshing && !canScrollUp) {
            delay(1500)
            isRefreshing = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Map background
        if (currentLocation != null) {
            MapViewComposable(
                currentLocation = currentLocation,
                secrets = nearbySecrets,
                onMarkerClick = { showSecretPreview = it },
                focusedSecret = focusedSecret,
                onFocusHandled = { focusedSecret = null }
            )
        } else {
            // Show map placeholder while loading location
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
                        .background(Color.Black.copy(alpha = 0.4f))
                )

                // Loading indicator
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
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Fetching your location",
                        color = Color.White.copy(alpha = 0.8f),
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
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Title
            Text(
                text = "Secret Spaces",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier
                    .shadow(8.dp)
            )
            
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

        // Bottom sheet (Feed)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .offset { androidx.compose.ui.unit.IntOffset(0, (animatedOffset + dragAmount).toInt()) }
                .background(
                    color = DarkBackground,
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                )
                .padding(top = 8.dp)
        ) {
            // Draggable header area (handle + title) wrapped in a single Box
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(sheetState) {
                        detectDragGestures(
                            onDragEnd = {
                                // Determine which state to snap to based on drag amount
                                sheetState = when {
                                    // Dragging down
                                    dragAmount > 0 -> {
                                        when (sheetState) {
                                            SheetState.FULLY_EXPANDED -> {
                                                if (dragAmount > 100) SheetState.HALF_EXPANDED else SheetState.FULLY_EXPANDED
                                            }
                                            SheetState.HALF_EXPANDED -> {
                                                if (dragAmount > 100) SheetState.COLLAPSED else SheetState.HALF_EXPANDED
                                            }
                                            SheetState.COLLAPSED -> SheetState.COLLAPSED
                                        }
                                    }
                                    // Dragging up
                                    dragAmount < 0 -> {
                                        when (sheetState) {
                                            SheetState.COLLAPSED -> {
                                                if (dragAmount < -100) SheetState.HALF_EXPANDED else SheetState.COLLAPSED
                                            }
                                            SheetState.HALF_EXPANDED -> {
                                                if (dragAmount < -100) SheetState.FULLY_EXPANDED else SheetState.HALF_EXPANDED
                                            }
                                            SheetState.FULLY_EXPANDED -> SheetState.FULLY_EXPANDED
                                        }
                                    }
                                    else -> sheetState
                                }
                                dragAmount = 0f
                            },
                            onDrag = { change, dragDelta ->
                                change.consume()
                                dragAmount += dragDelta.y
                                // Limit drag to prevent going beyond bounds
                                val minOffset = screenHeightPx - fullyExpandedHeight
                                val maxOffset = screenHeightPx - collapsedHeight
                                val totalOffset = animatedOffset + dragAmount
                                // Clamp the total offset
                                if (totalOffset < minOffset) {
                                    dragAmount = minOffset - animatedOffset
                                } else if (totalOffset > maxOffset) {
                                    dragAmount = maxOffset - animatedOffset
                                }
                            }
                        )
                    }
            ) {
                // Drag handle
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(4.dp)
                            .background(Color.White.copy(alpha = 0.3f), RoundedCornerShape(2.dp))
                            .clickable {
                                // Cycle through states on tap
                                sheetState = when (sheetState) {
                                    SheetState.COLLAPSED -> SheetState.HALF_EXPANDED
                                    SheetState.HALF_EXPANDED -> SheetState.FULLY_EXPANDED
                                    SheetState.FULLY_EXPANDED -> SheetState.COLLAPSED
                                }
                            }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Header with title and count
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🌟 Secret Feed",
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
            }

            // Feed content
            if (sheetState != SheetState.COLLAPSED) {
                Spacer(modifier = Modifier.height(16.dp))

                Box(modifier = Modifier.fillMaxSize()) {
                    when {
                        isLoading -> {
                            LazyColumn(
                                state = lazyListState,
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(if (sheetState == SheetState.FULLY_EXPANDED) 5 else 2) {
                                    ShimmerFeedCard()
                                }
                            }
                        }

                        nearbySecrets.isEmpty() -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text("🔍", style = MaterialTheme.typography.displayMedium)
                                    Text(
                                        text = "No secrets nearby",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Be the first to drop one!",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White.copy(alpha = 0.6f)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(
                                        onClick = onDropSecretClick,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = TealPrimary
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Drop Secret")
                                    }
                                }
                            }
                        }

                        else -> {
                            Box(modifier = Modifier.fillMaxSize()) {
                                LazyColumn(
                                    state = lazyListState,
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(
                                        start = 20.dp,
                                        end = 20.dp,
                                        top = 8.dp,
                                        bottom = if (sheetState == SheetState.FULLY_EXPANDED) 100.dp else 20.dp
                                    ),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    items(nearbySecrets, key = { it.id }) { secret ->
                                        FeedSecretCard(
                                            secret = secret,
                                            onLikeClick = { /* TODO: Handle like */ },
                                            onCommentClick = { onSecretClick(it) },
                                            onMapClick = {
                                                // Collapse sheet and focus on map location
                                                focusedSecret = it
                                                sheetState = SheetState.COLLAPSED
                                            },
                                            onCardClick = { onSecretClick(it) }
                                        )
                                    }

                                    // Infinite scroll indicator
                                    if (nearbySecrets.size >= 10) {
                                        item {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 16.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                CircularProgressIndicator(
                                                    color = TealPrimary,
                                                    modifier = Modifier.size(32.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                // Pull to refresh indicator
                                androidx.compose.animation.AnimatedVisibility(
                                    visible = isRefreshing,
                                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 16.dp)
                                ) {
                                    CircularProgressIndicator(
                                        color = TealPrimary,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // FAB fixed at bottom right corner - only visible when fully expanded
        AnimatedVisibility(
            visible = sheetState == SheetState.FULLY_EXPANDED,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 24.dp, end = 20.dp)
        ) {
            FloatingActionButton(
                onClick = onDropSecretClick,
                modifier = Modifier
                    .size(64.dp)
                    .shadow(14.dp, CircleShape),
                containerColor = Color(0xFFB71C1C),
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
                    println("Error focusing on secret: ${e.message}")
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
                println("Cleanup error")
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
                    } catch (_: Exception) {
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
                                    // Store the map instance for camera control
                                    mapLibreMapInstance = mapLibreMap
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
        delay(15000)
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
                    } catch (_: Exception) {
                        println("onPause error")
                    }
                }
                Lifecycle.Event.ON_RESUME -> {
                    try {
                        mapView?.onResume()
                    } catch (_: Exception) {
                        println("onResume error")
                    }
                }
                Lifecycle.Event.ON_STOP -> {
                    try {
                        mapView?.onStop()
                    } catch (_: Exception) {
                        println("onStop error")
                    }
                }
                Lifecycle.Event.ON_START -> {
                    try {
                        mapView?.onStart()
                    } catch (_: Exception) {
                        println("onStart error")
                    }
                }
                Lifecycle.Event.ON_DESTROY -> {
                    try {
                        println("Calling onDestroy on MapView")
                        symbolManager?.onDestroy()
                        symbolManager = null
                        mapView?.onDestroy()
                        mapView = null
                    } catch (_: Exception) {
                        println("onDestroy error")
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
