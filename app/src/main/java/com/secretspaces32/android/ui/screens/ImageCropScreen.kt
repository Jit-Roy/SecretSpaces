package com.secretspaces32.android.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.abs
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.secretspaces32.android.ui.theme.DarkBackground
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageCropScreen(
    selectedImages: List<Uri>,
    onImagesCropped: (List<Uri>) -> Unit,
    onBack: () -> Unit = {},
    cacheDir: File? = null
) {
    val context = LocalContext.current
    var currentImageIndex by rememberSaveable { mutableIntStateOf(0) }
    var croppedImages by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var currentBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }

    // Image scale state - preserve during interruptions
    var imageScale by rememberSaveable { mutableFloatStateOf(1f) }
    var cropFrameScale by rememberSaveable { mutableFloatStateOf(0.9f) }
    var cropFrameOffsetX by rememberSaveable { mutableFloatStateOf(0f) }
    var cropFrameOffsetY by rememberSaveable { mutableFloatStateOf(0f) }
    var rotationAngle by rememberSaveable { mutableFloatStateOf(0f) }
    var isFlippedHorizontally by rememberSaveable { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    // Load current image
    LaunchedEffect(currentImageIndex) {
        if (currentImageIndex < selectedImages.size) {
            isLoading = true
            currentBitmap = loadBitmap(context, selectedImages[currentImageIndex])
            // Reset transformation
            imageScale = 1f
            cropFrameScale = 0.9f
            cropFrameOffsetX = 0f
            cropFrameOffsetY = 0f
            rotationAngle = 0f
            isFlippedHorizontally = false
            isLoading = false
        }
    }

    // Crop and save function
    fun cropAndSaveImage() {
        currentBitmap?.let { bitmap ->
            coroutineScope.launch {
                isSaving = true
                val croppedUri = withContext(Dispatchers.IO) {
                    cropBitmapToLandscape(bitmap, imageScale, cropFrameScale, cropFrameOffsetX, cropFrameOffsetY, rotationAngle, isFlippedHorizontally, cacheDir, context)
                }

                croppedUri?.let {
                    croppedImages = croppedImages + it

                    // Move to next image or finish
                    if (currentImageIndex < selectedImages.size - 1) {
                        currentImageIndex++
                    } else {
                        // All images cropped, proceed to description
                        onImagesCropped(croppedImages)
                    }
                }
                isSaving = false
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top Bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = DarkBackground,
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(start = 4.dp, end = 16.dp, top = 16.dp, bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = "Crop to Landscape",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    // Progress indicator
                    Text(
                        text = "${currentImageIndex + 1}/${selectedImages.size}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }

            if (isLoading) {
                // Loading indicator
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFF4D9FFF))
                }
            } else {
                // Crop editor
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(Color.Black)
                ) {
                    currentBitmap?.let { bitmap ->
                        CropView(
                            bitmap = bitmap,
                            imageScale = imageScale,
                            cropFrameScale = cropFrameScale,
                            cropFrameOffsetX = cropFrameOffsetX,
                            cropFrameOffsetY = cropFrameOffsetY,
                            rotationAngle = rotationAngle,
                            isFlippedHorizontally = isFlippedHorizontally,
                            onTransform = { newImageScale, newCropScale, newOffsetX, newOffsetY ->
                                imageScale = newImageScale
                                cropFrameScale = newCropScale
                                cropFrameOffsetX = newOffsetX
                                cropFrameOffsetY = newOffsetY
                            }
                        )
                    }
                }

                // Instructions
                Text(
                    text = "Drag corners/edges to resize • Drag center to move • Pinch inside to zoom image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.7f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                // Rotation and Flip Controls
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Rotate Left Button
                    Button(
                        onClick = {
                            rotationAngle = (rotationAngle - 90f) % 360f
                            // Reset crop frame to center when rotating
                            cropFrameOffsetX = 0f
                            cropFrameOffsetY = 0f
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF333333)
                        )
                    ) {
                        Text("↶ Rotate")
                    }
                    
                    // Flip Horizontal Button
                    Button(
                        onClick = {
                            isFlippedHorizontally = !isFlippedHorizontally
                            // Reset crop frame to center when flipping
                            cropFrameOffsetX = 0f
                            cropFrameOffsetY = 0f
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF333333)
                        )
                    ) {
                        Text("⇄ Flip")
                    }
                    
                    // Rotate Right Button
                    Button(
                        onClick = {
                            rotationAngle = (rotationAngle + 90f) % 360f
                            // Reset crop frame to center when rotating
                            cropFrameOffsetX = 0f
                            cropFrameOffsetY = 0f
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF333333)
                        )
                    ) {
                        Text("Rotate ↷")
                    }
                }

                // Bottom button
                Button(
                    onClick = { cropAndSaveImage() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4D9FFF)
                    ),
                    enabled = !isSaving
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = if (currentImageIndex < selectedImages.size - 1) "Next" else "Done",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

// Helper function to compute display dimensions
private fun computeDisplayDims(
    canvasWidth: Float,
    canvasHeight: Float,
    imgW: Float,
    imgH: Float,
    scale: Float
): Pair<Float, Float> {
    val imageAspect = imgW / imgH
    val canvasAspect = canvasWidth / canvasHeight
    return if (imageAspect > canvasAspect) {
        val dw = canvasWidth * scale
        val dh = dw / imageAspect
        dw to dh
    } else {
        val dh = canvasHeight * scale
        val dw = dh * imageAspect
        dw to dh
    }
}

// Helper function to get rectangle corners
private fun rectCorners(rect: Rect): List<Offset> {
    return listOf(
        Offset(rect.left, rect.top),
        Offset(rect.right, rect.top),
        Offset(rect.right, rect.bottom),
        Offset(rect.left, rect.bottom)
    )
}

// Helper: Check if a point is inside a convex polygon using cross product
private fun isPointInPolygon(point: Offset, polygon: List<Offset>): Boolean {
    if (polygon.size < 3) return false
    
    var sign = 0
    for (i in polygon.indices) {
        val p1 = polygon[i]
        val p2 = polygon[(i + 1) % polygon.size]
        
        val crossProduct = (p2.x - p1.x) * (point.y - p1.y) - (p2.y - p1.y) * (point.x - p1.x)
        
        if (i == 0) {
            sign = if (crossProduct >= 0) 1 else -1
        } else {
            val currentSign = if (crossProduct >= 0) 1 else -1
            if (currentSign != sign) return false
        }
    }
    return true
}

// Check if a rectangle is inside the rotated image
private fun isRectInsideRotatedImage(
    rect: Rect,
    canvasWidth: Float,
    canvasHeight: Float,
    imgW: Float,
    imgH: Float,
    scale: Float,
    rotation: Float,
    isFlippedHorizontally: Boolean
): Boolean {
    val (dw0, dh0) = computeDisplayDims(canvasWidth, canvasHeight, imgW, imgH, scale)
    var hw = dw0 / 2f
    var hh = dh0 / 2f
    // Shrink a bit to avoid floating-point edge leaks
    val margin = 4.0f
    hw = (hw - margin).coerceAtLeast(0f)
    hh = (hh - margin).coerceAtLeast(0f)
    val cx = canvasWidth / 2f
    val cy = canvasHeight / 2f
    
    // Compute the rotated image polygon corners in canvas space
    val rad = (rotation % 360f) * (PI.toFloat() / 180f)
    val c = cos(rad)
    val s = sin(rad)
    
    // Image corners in local space (before any transformation)
    val imgCorners = arrayOf(
        Offset(-hw, -hh),
        Offset(hw, -hh),
        Offset(hw, hh),
        Offset(-hw, hh)
    )
    
    // Transform image corners to canvas space
    // The transformation in Canvas applies: first scale/flip, then rotate
    // So we need to apply transformations in the same order
    val transformedImgCorners = imgCorners.map { p ->
        var x = p.x
        val y = p.y
        // Apply horizontal flip first (this is applied before rotation in the canvas)
        if (isFlippedHorizontally) {
            x = -x
        }
        // Then apply rotation around center
        val rotX = x * c - y * s + cx
        val rotY = x * s + y * c + cy
        Offset(rotX, rotY)
    }
    
    // Check if all crop rect corners are inside the rotated image polygon
    val cropCorners = rectCorners(rect)
    for (p in cropCorners) {
        if (!isPointInPolygon(p, transformedImgCorners)) {
            return false
        }
    }
    return true
}

@Composable
fun CropView(
    bitmap: Bitmap,
    imageScale: Float,
    cropFrameScale: Float,
    cropFrameOffsetX: Float,
    cropFrameOffsetY: Float,
    rotationAngle: Float = 0f,
    isFlippedHorizontally: Boolean = false,
    onTransform: (Float, Float, Float, Float) -> Unit
) {
    var currentImageScale by remember { mutableFloatStateOf(imageScale) }
    var currentCropFrameScale by remember { mutableFloatStateOf(cropFrameScale) }
    var currentCropOffsetX by remember { mutableFloatStateOf(cropFrameOffsetX) }
    var currentCropOffsetY by remember { mutableFloatStateOf(cropFrameOffsetY) }
    var isDraggingEdge by remember { mutableStateOf(false) }
    var draggedEdge by remember { mutableStateOf<String?>(null) }
    var initialTouchPoint by remember { mutableStateOf(Offset.Zero) }
    var initialCropScale by remember { mutableFloatStateOf(0f) }
    var initialCropOffsetX by remember { mutableFloatStateOf(0f) }
    var initialCropOffsetY by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(imageScale, cropFrameScale, cropFrameOffsetX, cropFrameOffsetY) {
        currentImageScale = imageScale
        currentCropFrameScale = cropFrameScale
        currentCropOffsetX = cropFrameOffsetX
        currentCropOffsetY = cropFrameOffsetY
    }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val canvasWidth = size.width
                        val canvasHeight = size.height

                        val cropWidth = canvasWidth * currentCropFrameScale
                        val cropHeight = cropWidth * 9f / 16f

                        val cropLeft = if (currentCropOffsetX == 0f && currentCropOffsetY == 0f) {
                            (canvasWidth - cropWidth) / 2f
                        } else {
                            currentCropOffsetX
                        }

                        val cropTop = if (currentCropOffsetX == 0f && currentCropOffsetY == 0f) {
                            (canvasHeight - cropHeight) / 2f
                        } else {
                            currentCropOffsetY
                        }

                        val cropRight = cropLeft + cropWidth
                        val cropBottom = cropTop + cropHeight

                        val touchZone = 50f // Touch zone size for edges and corners

                        when (event.type) {
                            androidx.compose.ui.input.pointer.PointerEventType.Press -> {
                                val touch = event.changes.first().position
                                initialTouchPoint = touch

                                // Check if touch is on corner or edge
                                val onLeftEdge = touch.x >= cropLeft - touchZone && touch.x <= cropLeft + touchZone
                                val onRightEdge = touch.x >= cropRight - touchZone && touch.x <= cropRight + touchZone
                                val onTopEdge = touch.y >= cropTop - touchZone && touch.y <= cropTop + touchZone
                                val onBottomEdge = touch.y >= cropBottom - touchZone && touch.y <= cropBottom + touchZone

                                if (onLeftEdge || onRightEdge || onTopEdge || onBottomEdge) {
                                    isDraggingEdge = true
                                    initialCropScale = currentCropFrameScale
                                    initialCropOffsetX = currentCropOffsetX
                                    initialCropOffsetY = currentCropOffsetY

                                    // Determine which edge/corner
                                    draggedEdge = when {
                                        onLeftEdge && onTopEdge -> "top-left"
                                        onRightEdge && onTopEdge -> "top-right"
                                        onLeftEdge && onBottomEdge -> "bottom-left"
                                        onRightEdge && onBottomEdge -> "bottom-right"
                                        onLeftEdge -> "left"
                                        onRightEdge -> "right"
                                        onTopEdge -> "top"
                                        onBottomEdge -> "bottom"
                                        else -> null
                                    }
                                } else {
                                    isDraggingEdge = false
                                    draggedEdge = null
                                }
                            }
                            androidx.compose.ui.input.pointer.PointerEventType.Move -> {
                                if (isDraggingEdge && draggedEdge != null) {
                                    val touch = event.changes.first().position
                                    val deltaX = touch.x - initialTouchPoint.x
                                    val deltaY = touch.y - initialTouchPoint.y

                                    val imageWidth = bitmap.width.toFloat()
                                    val imageHeight = bitmap.height.toFloat()
                                    val imageAspect = imageWidth / imageHeight

                                    // Calculate image display dimensions
                                    val displayWidth: Float
                                    val displayHeight: Float

                                    if (imageAspect > (canvasWidth / canvasHeight)) {
                                        displayWidth = canvasWidth * currentImageScale
                                        displayHeight = displayWidth / imageAspect
                                    } else {
                                        displayHeight = canvasHeight * currentImageScale
                                        displayWidth = displayHeight * imageAspect
                                    }

                                    val imageLeft = (canvasWidth - displayWidth) / 2f
                                    val imageTop = (canvasHeight - displayHeight) / 2f
                                    val imageRight = imageLeft + displayWidth
                                    val imageBottom = imageTop + displayHeight

                                    // Dynamic scale constraints so the crop stays inside the displayed image
                                    val maxScaleByWidth = displayWidth / canvasWidth
                                    val maxScaleByHeight = (displayHeight * 16f / 9f) / canvasWidth
                                    val maxAllowedScale = minOf(maxScaleByWidth, maxScaleByHeight)
                                    val minAllowedScale = 0.2f // allow a smaller box than before

                                    val initialWidth = canvasWidth * initialCropScale
                                    val initialHeight = initialWidth * 9f / 16f
                                    val initialRight = initialCropOffsetX + if (initialCropOffsetX == 0f && initialCropOffsetY == 0f) {
                                        // If not positioned yet, treat as centered
                                        (canvasWidth - initialWidth) / 2f + initialWidth
                                    } else {
                                        initialWidth
                                    }

                                    // Helper: clamp offsets to keep frame inside rotated image
                                    fun clampOffsets(width: Float, height: Float, proposedLeft: Float, proposedTop: Float): Pair<Float, Float> {
                                        // For rotated images, use boundary checking
                                        if (rotationAngle % 360f != 0f || isFlippedHorizontally) {
                                            val testRect = Rect(proposedLeft, proposedTop, proposedLeft + width, proposedTop + height)
                                            if (isRectInsideRotatedImage(testRect, canvasWidth, canvasHeight, imageWidth, imageHeight, currentImageScale, rotationAngle, isFlippedHorizontally)) {
                                                return proposedLeft to proposedTop
                                            }
                                            // If the proposed position is invalid, try to find a valid position by adjusting slightly
                                            // For simplicity, just return the current position to prevent invalid moves
                                            return currentCropOffsetX to currentCropOffsetY
                                        } else {
                                            // For non-rotated images, use simple bounding box
                                            val minX = imageLeft
                                            val maxX = imageRight - width
                                            val minY = imageTop
                                            val maxY = imageBottom - height
                                            return proposedLeft.coerceIn(minX, maxX) to proposedTop.coerceIn(minY, maxY)
                                        }
                                    }

                                    fun applyResize(newWidth: Float, anchorLeft: Boolean, anchorTop: Boolean) {
                                        var width = newWidth.coerceAtLeast(1f)
                                        var newScale = (width / canvasWidth).coerceIn(minAllowedScale, maxAllowedScale)
                                        width = canvasWidth * newScale
                                        val height = width * 9f / 16f

                                        // Determine new left/top based on anchors
                                        var left = if (anchorLeft) currentCropOffsetX else (currentCropOffsetX + cropWidth) - width
                                        var top = if (anchorTop) currentCropOffsetY else (currentCropOffsetY + cropHeight) - height

                                        // If offsets are zero (initial centered state), compute centered pos first
                                        if (currentCropOffsetX == 0f && currentCropOffsetY == 0f) {
                                            val centeredLeft = (canvasWidth - cropWidth) / 2f
                                            val centeredTop = (canvasHeight - cropHeight) / 2f
                                            left = if (anchorLeft) centeredLeft else (centeredLeft + cropWidth) - width
                                            top = if (anchorTop) centeredTop else (centeredTop + cropHeight) - height
                                        }

                                        val (clampedLeft, clampedTop) = clampOffsets(width, height, left, top)

                                        // Only apply if the new position is valid
                                        val testRect = Rect(clampedLeft, clampedTop, clampedLeft + width, clampedTop + height)
                                        if (rotationAngle % 360f != 0f || isFlippedHorizontally) {
                                            if (isRectInsideRotatedImage(testRect, canvasWidth, canvasHeight, imageWidth, imageHeight, currentImageScale, rotationAngle, isFlippedHorizontally)) {
                                                currentCropFrameScale = newScale
                                                currentCropOffsetX = clampedLeft
                                                currentCropOffsetY = clampedTop
                                            }
                                        } else {
                                            currentCropFrameScale = newScale
                                            currentCropOffsetX = clampedLeft
                                            currentCropOffsetY = clampedTop
                                        }
                                    }

                                    when (draggedEdge) {
                                        // Horizontal edges
                                        "right" -> {
                                            val newWidth = (canvasWidth * initialCropScale) + deltaX
                                            applyResize(newWidth = newWidth, anchorLeft = true, anchorTop = true)
                                        }
                                        "left" -> {
                                            val newWidth = (canvasWidth * initialCropScale) - deltaX
                                            applyResize(newWidth = newWidth, anchorLeft = false, anchorTop = true)
                                        }
                                        // Vertical edges
                                        "bottom" -> {
                                            val newHeight = (initialHeight) + deltaY
                                            val newWidth = newHeight * 16f / 9f
                                            applyResize(newWidth = newWidth, anchorLeft = true, anchorTop = true)
                                        }
                                        "top" -> {
                                            val newHeight = (initialHeight) - deltaY
                                            val newWidth = newHeight * 16f / 9f
                                            applyResize(newWidth = newWidth, anchorLeft = true, anchorTop = false)
                                        }
                                        // Corners
                                        "bottom-right" -> {
                                            val widthFromX = (canvasWidth * initialCropScale) + deltaX
                                            val widthFromY = ((initialHeight + deltaY) * 16f / 9f)
                                            val newWidth = if (kotlin.math.abs(widthFromX - initialWidth) >= kotlin.math.abs(widthFromY - initialWidth)) widthFromX else widthFromY
                                            applyResize(newWidth = newWidth, anchorLeft = true, anchorTop = true)
                                        }
                                        "bottom-left" -> {
                                            val widthFromX = (canvasWidth * initialCropScale) - deltaX
                                            val widthFromY = ((initialHeight + deltaY) * 16f / 9f)
                                            val newWidth = if (kotlin.math.abs(widthFromX - initialWidth) >= kotlin.math.abs(widthFromY - initialWidth)) widthFromX else widthFromY
                                            applyResize(newWidth = newWidth, anchorLeft = false, anchorTop = true)
                                        }
                                        "top-right" -> {
                                            val widthFromX = (canvasWidth * initialCropScale) + deltaX
                                            val widthFromY = ((initialHeight - deltaY) * 16f / 9f)
                                            val newWidth = if (kotlin.math.abs(widthFromX - initialWidth) >= kotlin.math.abs(widthFromY - initialWidth)) widthFromX else widthFromY
                                            applyResize(newWidth = newWidth, anchorLeft = true, anchorTop = false)
                                        }
                                        "top-left" -> {
                                            val widthFromX = (canvasWidth * initialCropScale) - deltaX
                                            val widthFromY = ((initialHeight - deltaY) * 16f / 9f)
                                            val newWidth = if (kotlin.math.abs(widthFromX - initialWidth) >= kotlin.math.abs(widthFromY - initialWidth)) widthFromX else widthFromY
                                            applyResize(newWidth = newWidth, anchorLeft = false, anchorTop = false)
                                        }
                                    }

                                    onTransform(currentImageScale, currentCropFrameScale, currentCropOffsetX, currentCropOffsetY)
                                    event.changes.forEach { it.consume() }
                                } else if (!isDraggingEdge) {
                                    // Move the crop frame
                                    val touch = event.changes.first().position
                                    val deltaX = touch.x - initialTouchPoint.x
                                    val deltaY = touch.y - initialTouchPoint.y

                                    // Check if inside crop frame
                                    val isInsideCropFrame = initialTouchPoint.x >= cropLeft &&
                                                           initialTouchPoint.x <= cropRight &&
                                                           initialTouchPoint.y >= cropTop &&
                                                           initialTouchPoint.y <= cropBottom

                                    if (isInsideCropFrame) {
                                        var newCropOffsetX = currentCropOffsetX + deltaX
                                        var newCropOffsetY = currentCropOffsetY + deltaY

                                        val imageWidth = bitmap.width.toFloat()
                                        val imageHeight = bitmap.height.toFloat()
                                        val imageAspect = imageWidth / imageHeight

                                        val displayWidth: Float
                                        val displayHeight: Float

                                        if (imageAspect > (canvasWidth / canvasHeight)) {
                                            displayWidth = canvasWidth * currentImageScale
                                            displayHeight = displayWidth / imageAspect
                                        } else {
                                            displayHeight = canvasHeight * currentImageScale
                                            displayWidth = displayHeight * imageAspect
                                        }

                                        val updatedCropWidth = canvasWidth * currentCropFrameScale
                                        val updatedCropHeight = updatedCropWidth * 9f / 16f

                                        // Use boundary checking for rotated images
                                        if (rotationAngle % 360f != 0f || isFlippedHorizontally) {
                                            val testRect = Rect(newCropOffsetX, newCropOffsetY, newCropOffsetX + updatedCropWidth, newCropOffsetY + updatedCropHeight)
                                            if (isRectInsideRotatedImage(testRect, canvasWidth, canvasHeight, imageWidth, imageHeight, currentImageScale, rotationAngle, isFlippedHorizontally)) {
                                                currentCropOffsetX = newCropOffsetX
                                                currentCropOffsetY = newCropOffsetY
                                            }
                                        } else {
                                            // For non-rotated images, use simple bounding box
                                            val imageLeft = (canvasWidth - displayWidth) / 2f
                                            val imageTop = (canvasHeight - displayHeight) / 2f
                                            val imageRight = imageLeft + displayWidth
                                            val imageBottom = imageTop + displayHeight

                                            val minCropOffsetX = imageLeft
                                            val maxCropOffsetX = imageRight - updatedCropWidth
                                            val minCropOffsetY = imageTop
                                            val maxCropOffsetY = imageBottom - updatedCropHeight

                                            newCropOffsetX = newCropOffsetX.coerceIn(minCropOffsetX, maxCropOffsetX)
                                            newCropOffsetY = newCropOffsetY.coerceIn(minCropOffsetY, maxCropOffsetY)

                                            currentCropOffsetX = newCropOffsetX
                                            currentCropOffsetY = newCropOffsetY
                                        }
                                        
                                        initialTouchPoint = touch
                                        onTransform(currentImageScale, currentCropFrameScale, currentCropOffsetX, currentCropOffsetY)
                                    }
                                }
                            }
                            androidx.compose.ui.input.pointer.PointerEventType.Release -> {
                                isDraggingEdge = false
                                draggedEdge = null
                            }
                            else -> {}
                        }
                    }
                }
            }
            .pointerInput(Unit) {
                detectTransformGestures { centroid, _, zoom, _ ->
                    if (zoom != 1f) {
                        val canvasWidth = size.width
                        val canvasHeight = size.height
                        val cropWidth = canvasWidth * currentCropFrameScale
                        val cropHeight = cropWidth * 9f / 16f

                        val cropLeft = if (currentCropOffsetX == 0f && currentCropOffsetY == 0f) {
                            (canvasWidth - cropWidth) / 2f
                        } else {
                            currentCropOffsetX
                        }

                        val cropTop = if (currentCropOffsetX == 0f && currentCropOffsetY == 0f) {
                            (canvasHeight - cropHeight) / 2f
                        } else {
                            currentCropOffsetY
                        }

                        val isInsideCropFrame = centroid.x >= cropLeft &&
                                               centroid.x <= cropLeft + cropWidth &&
                                               centroid.y >= cropTop &&
                                               centroid.y <= cropTop + cropHeight

                        if (isInsideCropFrame) {
                            // Scale the image
                            currentImageScale = (currentImageScale * zoom).coerceIn(1f, 5f)
                            onTransform(currentImageScale, currentCropFrameScale, currentCropOffsetX, currentCropOffsetY)
                        }
                    }
                }
            }
    ) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        val imageWidth = bitmap.width.toFloat()
        val imageHeight = bitmap.height.toFloat()
        val imageAspect = imageWidth / imageHeight

        // Calculate image display dimensions (centered and scaled)
        val displayWidth: Float
        val displayHeight: Float

        if (imageAspect > (canvasWidth / canvasHeight)) {
            displayWidth = canvasWidth * currentImageScale
            displayHeight = displayWidth / imageAspect
        } else {
            displayHeight = canvasHeight * currentImageScale
            displayWidth = displayHeight * imageAspect
        }

        // Center the image (fixed position)
        val imageLeft = (canvasWidth - displayWidth) / 2f
        val imageTop = (canvasHeight - displayHeight) / 2f

        // Draw the image at fixed centered position with rotation and flip
        val imageBitmap = bitmap.asImageBitmap()
        val centerX = canvasWidth / 2f
        val centerY = canvasHeight / 2f
        
        // Apply transformations (rotation and flip) around the center
        rotate(
            degrees = rotationAngle,
            pivot = Offset(centerX, centerY)
        ) {
            scale(
                scaleX = if (isFlippedHorizontally) -1f else 1f,
                scaleY = 1f,
                pivot = Offset(centerX, centerY)
            ) {
                drawImage(
                    image = imageBitmap,
                    dstOffset = IntOffset(imageLeft.toInt(), imageTop.toInt()),
                    dstSize = IntSize(displayWidth.toInt(), displayHeight.toInt())
                )
            }
        }

        // Calculate 16:9 crop frame (movable and scalable)
        val cropWidth = canvasWidth * currentCropFrameScale
        val cropHeight = cropWidth * 9f / 16f

        // Initialize crop frame to center if offsets are zero
        val cropLeft = if (currentCropOffsetX == 0f && currentCropOffsetY == 0f) {
            (canvasWidth - cropWidth) / 2f
        } else {
            currentCropOffsetX
        }

        val cropTop = if (currentCropOffsetX == 0f && currentCropOffsetY == 0f) {
            (canvasHeight - cropHeight) / 2f
        } else {
            currentCropOffsetY
        }

        val cropRect = Rect(
            cropLeft,
            cropTop,
            cropLeft + cropWidth,
            cropTop + cropHeight
        )

        // Draw semi-transparent dark overlay on top
        drawRect(
            color = Color.Black.copy(alpha = 0.5f),
            topLeft = Offset(0f, 0f),
            size = Size(canvasWidth, cropRect.top)
        )
        // Draw semi-transparent dark overlay on bottom
        drawRect(
            color = Color.Black.copy(alpha = 0.5f),
            topLeft = Offset(0f, cropRect.bottom),
            size = Size(canvasWidth, canvasHeight - cropRect.bottom)
        )
        // Draw semi-transparent dark overlay on left
        drawRect(
            color = Color.Black.copy(alpha = 0.5f),
            topLeft = Offset(0f, cropRect.top),
            size = Size(cropRect.left, cropRect.height)
        )
        // Draw semi-transparent dark overlay on right
        drawRect(
            color = Color.Black.copy(alpha = 0.5f),
            topLeft = Offset(cropRect.right, cropRect.top),
            size = Size(canvasWidth - cropRect.right, cropRect.height)
        )

        // Draw crop frame border
        drawRect(
            color = Color.White,
            topLeft = Offset(cropRect.left, cropRect.top),
            size = Size(cropRect.width, cropRect.height),
            style = Stroke(width = 3f)
        )

        // Draw corner handles
        val handleSize = 40f
        val handleColor = Color.White
        val handleStroke = 4f

        // Top-left corner
        drawLine(
            color = handleColor,
            start = Offset(cropRect.left, cropRect.top),
            end = Offset(cropRect.left + handleSize, cropRect.top),
            strokeWidth = handleStroke
        )
        drawLine(
            color = handleColor,
            start = Offset(cropRect.left, cropRect.top),
            end = Offset(cropRect.left, cropRect.top + handleSize),
            strokeWidth = handleStroke
        )

        // Top-right corner
        drawLine(
            color = handleColor,
            start = Offset(cropRect.right - handleSize, cropRect.top),
            end = Offset(cropRect.right, cropRect.top),
            strokeWidth = handleStroke
        )
        drawLine(
            color = handleColor,
            start = Offset(cropRect.right, cropRect.top),
            end = Offset(cropRect.right, cropRect.top + handleSize),
            strokeWidth = handleStroke
        )

        // Bottom-left corner
        drawLine(
            color = handleColor,
            start = Offset(cropRect.left, cropRect.bottom - handleSize),
            end = Offset(cropRect.left, cropRect.bottom),
            strokeWidth = handleStroke
        )
        drawLine(
            color = handleColor,
            start = Offset(cropRect.left, cropRect.bottom),
            end = Offset(cropRect.left + handleSize, cropRect.bottom),
            strokeWidth = handleStroke
        )

        // Bottom-right corner
        drawLine(
            color = handleColor,
            start = Offset(cropRect.right, cropRect.bottom - handleSize),
            end = Offset(cropRect.right, cropRect.bottom),
            strokeWidth = handleStroke
        )
        drawLine(
            color = handleColor,
            start = Offset(cropRect.right - handleSize, cropRect.bottom),
            end = Offset(cropRect.right, cropRect.bottom),
            strokeWidth = handleStroke
        )

        // Draw grid lines
        val gridColor = Color.White.copy(alpha = 0.5f)
        // Vertical lines
        drawLine(
            color = gridColor,
            start = Offset(cropRect.left + cropRect.width / 3f, cropRect.top),
            end = Offset(cropRect.left + cropRect.width / 3f, cropRect.bottom),
            strokeWidth = 1f
        )
        drawLine(
            color = gridColor,
            start = Offset(cropRect.left + cropRect.width * 2f / 3f, cropRect.top),
            end = Offset(cropRect.left + cropRect.width * 2f / 3f, cropRect.bottom),
            strokeWidth = 1f
        )
        // Horizontal lines
        drawLine(
            color = gridColor,
            start = Offset(cropRect.left, cropRect.top + cropRect.height / 3f),
            end = Offset(cropRect.right, cropRect.top + cropRect.height / 3f),
            strokeWidth = 1f
        )
        drawLine(
            color = gridColor,
            start = Offset(cropRect.left, cropRect.top + cropRect.height * 2f / 3f),
            end = Offset(cropRect.right, cropRect.top + cropRect.height * 2f / 3f),
            strokeWidth = 1f
        )
    }
}

private suspend fun loadBitmap(context: Context, uri: Uri): Bitmap? {
    return withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream?.close()

            // Calculate sample size to reduce memory
            val maxSize = 2048
            var sampleSize = 1
            while (options.outWidth / sampleSize > maxSize || options.outHeight / sampleSize > maxSize) {
                sampleSize *= 2
            }

            val finalOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
            }

            val finalInputStream = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(finalInputStream, null, finalOptions)
            finalInputStream?.close()
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

private fun cropBitmapToLandscape(
    bitmap: Bitmap,
    imageScale: Float,
    cropFrameScale: Float,
    cropFrameOffsetX: Float,
    cropFrameOffsetY: Float,
    rotationAngle: Float,
    isFlippedHorizontally: Boolean,
    cacheDir: File?,
    context: Context
): Uri? {
    try {
        // Target dimensions for 16:9 landscape
        val targetWidth = 1920
        val targetHeight = 1080

        // First, apply rotation and flip to the original bitmap
        val transformedBitmap = if (rotationAngle % 360f != 0f || isFlippedHorizontally) {
            val matrix = Matrix()
            // Apply flip
            if (isFlippedHorizontally) {
                matrix.postScale(-1f, 1f, bitmap.width / 2f, bitmap.height / 2f)
            }
            // Apply rotation
            if (rotationAngle % 360f != 0f) {
                matrix.postRotate(rotationAngle, bitmap.width / 2f, bitmap.height / 2f)
            }
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } else {
            bitmap
        }

        val imageWidth = transformedBitmap.width.toFloat()
        val imageHeight = transformedBitmap.height.toFloat()

        // We need to calculate what the canvas dimensions would be
        // For simplicity, assume a standard canvas aspect ratio
        // In reality, you'd pass the actual canvas dimensions
        val canvasWidth = 1080f // Typical mobile width
        val canvasHeight = 1920f // Typical mobile height

        val imageAspect = imageWidth / imageHeight

        // Calculate image display dimensions (same as in CropView)
        val displayWidth: Float
        val displayHeight: Float

        if (imageAspect > (canvasWidth / canvasHeight)) {
            displayWidth = canvasWidth * imageScale
            displayHeight = displayWidth / imageAspect
        } else {
            displayHeight = canvasHeight * imageScale
            displayWidth = displayHeight * imageAspect
        }

        // Image is centered on canvas
        val imageLeft = (canvasWidth - displayWidth) / 2f
        val imageTop = (canvasHeight - displayHeight) / 2f

        // Crop frame dimensions
        val cropFrameWidth = canvasWidth * cropFrameScale
        val cropFrameHeight = cropFrameWidth * 9f / 16f

        // Crop frame position (centered if no offset)
        val cropLeft = if (cropFrameOffsetX == 0f && cropFrameOffsetY == 0f) {
            (canvasWidth - cropFrameWidth) / 2f
        } else {
            cropFrameOffsetX
        }

        val cropTop = if (cropFrameOffsetX == 0f && cropFrameOffsetY == 0f) {
            (canvasHeight - cropFrameHeight) / 2f
        } else {
            cropFrameOffsetY
        }

        // Convert crop frame coordinates to bitmap coordinates
        val bitmapCropLeft = ((cropLeft - imageLeft) / displayWidth * imageWidth).coerceIn(0f, imageWidth)
        val bitmapCropTop = ((cropTop - imageTop) / displayHeight * imageHeight).coerceIn(0f, imageHeight)
        val bitmapCropWidth = (cropFrameWidth / displayWidth * imageWidth).coerceAtMost(imageWidth - bitmapCropLeft)
        val bitmapCropHeight = (cropFrameHeight / displayHeight * imageHeight).coerceAtMost(imageHeight - bitmapCropTop)

        // Crop the transformed bitmap
        val croppedBitmap = Bitmap.createBitmap(
            transformedBitmap,
            bitmapCropLeft.toInt(),
            bitmapCropTop.toInt(),
            bitmapCropWidth.toInt(),
            bitmapCropHeight.toInt()
        )

        // Scale to target dimensions
        val finalBitmap = Bitmap.createScaledBitmap(
            croppedBitmap,
            targetWidth,
            targetHeight,
            true
        )

        // Save to file
        val file = File(cacheDir, "cropped_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { out ->
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }

        // Clean up bitmaps
        if (transformedBitmap != bitmap) {
            transformedBitmap.recycle()
        }
        croppedBitmap.recycle()
        finalBitmap.recycle()

        return androidx.core.content.FileProvider.getUriForFile(
            context,
            "com.secretspaces32.android.fileprovider",
            file
        )
    } catch (e: Exception) {
        e.printStackTrace()
        return null
    }
}
