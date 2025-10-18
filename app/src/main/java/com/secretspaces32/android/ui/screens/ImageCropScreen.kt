package com.secretspaces32.android.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
            isLoading = false
        }
    }

    // Crop and save function
    fun cropAndSaveImage() {
        currentBitmap?.let { bitmap ->
            coroutineScope.launch {
                isSaving = true
                val croppedUri = withContext(Dispatchers.IO) {
                    cropBitmapToLandscape(bitmap, imageScale, cropFrameScale, cropFrameOffsetX, cropFrameOffsetY, cacheDir, context)
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

@Composable
fun CropView(
    bitmap: Bitmap,
    imageScale: Float,
    cropFrameScale: Float,
    cropFrameOffsetX: Float,
    cropFrameOffsetY: Float,
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

                                    // Calculate new scale and offset based on dragged edge
                                    when (draggedEdge) {
                                        "right", "bottom-right", "top-right" -> {
                                            // Dragging right edge - increase width
                                            val newWidth = (canvasWidth * initialCropScale) + deltaX
                                            val newScale = (newWidth / canvasWidth).coerceIn(0.3f, 1.0f)
                                            currentCropFrameScale = newScale
                                        }
                                        "left", "bottom-left", "top-left" -> {
                                            // Dragging left edge - increase width and move left
                                            val newWidth = (canvasWidth * initialCropScale) - deltaX
                                            val newScale = (newWidth / canvasWidth).coerceIn(0.3f, 1.0f)

                                            // Adjust position to keep right edge fixed
                                            val widthChange = (newScale - initialCropScale) * canvasWidth
                                            currentCropOffsetX = (initialCropOffsetX - widthChange).coerceIn(
                                                imageLeft,
                                                imageLeft + displayWidth - canvasWidth * newScale
                                            )
                                            currentCropFrameScale = newScale
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

                                        val imageLeft = (canvasWidth - displayWidth) / 2f
                                        val imageTop = (canvasHeight - displayHeight) / 2f
                                        val imageRight = imageLeft + displayWidth
                                        val imageBottom = imageTop + displayHeight

                                        val updatedCropWidth = canvasWidth * currentCropFrameScale
                                        val updatedCropHeight = updatedCropWidth * 9f / 16f

                                        val minCropOffsetX = imageLeft
                                        val maxCropOffsetX = imageRight - updatedCropWidth
                                        val minCropOffsetY = imageTop
                                        val maxCropOffsetY = imageBottom - updatedCropHeight

                                        newCropOffsetX = newCropOffsetX.coerceIn(minCropOffsetX, maxCropOffsetX)
                                        newCropOffsetY = newCropOffsetY.coerceIn(minCropOffsetY, maxCropOffsetY)

                                        currentCropOffsetX = newCropOffsetX
                                        currentCropOffsetY = newCropOffsetY
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

        // Draw the image at fixed centered position
        val imageBitmap = bitmap.asImageBitmap()
        drawImage(
            image = imageBitmap,
            dstOffset = IntOffset(imageLeft.toInt(), imageTop.toInt()),
            dstSize = IntSize(displayWidth.toInt(), displayHeight.toInt())
        )

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
    cacheDir: File?,
    context: Context
): Uri? {
    try {
        // Target dimensions for 16:9 landscape
        val targetWidth = 1920
        val targetHeight = 1080

        val imageWidth = bitmap.width.toFloat()
        val imageHeight = bitmap.height.toFloat()

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

        // Crop the bitmap
        val croppedBitmap = Bitmap.createBitmap(
            bitmap,
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
