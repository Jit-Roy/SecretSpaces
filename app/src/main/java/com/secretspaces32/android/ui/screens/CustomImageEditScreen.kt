package com.secretspaces32.android.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.net.Uri
import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.RotateLeft
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

// uCrop
import com.yalantis.ucrop.view.UCropView
import com.yalantis.ucrop.view.OverlayView
import com.yalantis.ucrop.callback.BitmapCropCallback
import androidx.compose.ui.viewinterop.AndroidView

enum class EditMode {
    CROP, FILTER, ADJUST, STICKER, MORE
}

data class ImageEditState(
    val rotation: Float = 0f,
    val scale: Float = 1f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val brightness: Float = 0f,
    val contrast: Float = 1f,
    val saturation: Float = 1f,
    val flipHorizontal: Boolean = false,
    val flipVertical: Boolean = false,
    val showGrid: Boolean = true,
    val cropScale: Float = 1f,
    val cropOffsetX: Float = 0f,
    val cropOffsetY: Float = 0f
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomImageEditScreen(
    selectedImages: List<Uri>,
    onImagesCropped: (List<Uri>) -> Unit,
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var currentImageIndex by remember { mutableIntStateOf(0) }
    var editedImages by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var editMode by remember { mutableStateOf(EditMode.CROP) }
    var editState by remember { mutableStateOf(ImageEditState()) }
    var isProcessing by remember { mutableStateOf(false) }

    // Keep per-image cropped source produced by uCrop (embedded)
    var croppedImageUris by remember { mutableStateOf<Map<Int, Uri>>(emptyMap()) }

    // UCrop view reference for current screen
    var uCropViewRef by remember { mutableStateOf<UCropView?>(null) }

    // Use cropped uri if available for current index
    val currentImageUri: Uri? = run {
        val original = selectedImages.getOrNull(currentImageIndex)
        croppedImageUris[currentImageIndex] ?: original
    }

    // Flip current image by writing a mirrored temporary file and return its Uri
    suspend fun flipCurrentImageHorizontal(source: Uri): Uri? = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(source)?.use { input ->
                val original = BitmapFactory.decodeStream(input)
                val m = Matrix().apply { postScale(-1f, 1f, original.width / 2f, original.height / 2f) }
                val flipped = Bitmap.createBitmap(original, 0, 0, original.width, original.height, m, true)
                val outFile = File(context.cacheDir, "flipped_${System.currentTimeMillis()}_${currentImageIndex}.jpg")
                FileOutputStream(outFile).use { flipped.compress(Bitmap.CompressFormat.JPEG, 95, it) }
                Uri.fromFile(outFile)
            }
        } catch (t: Throwable) {
            t.printStackTrace()
            null
        }
    }

    fun configureUCropView(view: UCropView, imageUri: Uri) {
        val crop = view.cropImageView
        val overlay = view.overlayView
        // Configure gestures and visuals similar to uCrop default
        crop.setScaleEnabled(true)
        crop.setRotateEnabled(true)
        crop.setMaxResultImageSizeX(4096)
        crop.setMaxResultImageSizeY(4096)
        crop.setImageToWrapCropBounds(true)
        // Start with 16:9 landscape box, allow freestyle resize
        overlay.setShowCropGrid(true)
        overlay.setShowCropFrame(true)
        overlay.setFreestyleCropMode(OverlayView.FREESTYLE_CROP_MODE_ENABLE)
        crop.setTargetAspectRatio(16f / 9f)

        // Prepare output destination per load
        val out = File(view.context.cacheDir, "ucrop_embed_${System.currentTimeMillis()}.jpg")
        val outUri = Uri.fromFile(out)
        try {
            crop.setImageUri(imageUri, outUri)
        } catch (t: Throwable) {
            t.printStackTrace()
        }
    }

    suspend fun cropWithUCropViewIfPresent(currentImageUri: Uri?): Uri? = withContext(Dispatchers.Main) {
        val view = uCropViewRef ?: return@withContext null
        val cropView = view.cropImageView
        val out = File(view.context.cacheDir, "ucrop_inplace_${System.currentTimeMillis()}_${currentImageIndex}.jpg")
        val outUri = Uri.fromFile(out)
        // Ensure output uri is set (input same as current)
        try {
            if (currentImageUri != null) cropView.setImageUri(currentImageUri, outUri)
        } catch (t: Throwable) {
            t.printStackTrace()
        }
        kotlinx.coroutines.suspendCancellableCoroutine<Uri?> { cont ->
            cropView.cropAndSaveImage(
                Bitmap.CompressFormat.JPEG,
                90,
                object : BitmapCropCallback {
                    override fun onBitmapCropped(resultUri: Uri, offsetX: Int, offsetY: Int, imageWidth: Int, imageHeight: Int) {
                        cont.resume(resultUri, onCancellation = {})
                    }
                    override fun onCropFailure(t: Throwable) {
                        t.printStackTrace()
                        cont.resume(null, onCancellation = {})
                    }
                }
            )
        }
    }

    suspend fun saveEditedImage(): Uri? {
        return withContext(Dispatchers.IO) {
            try {
                // First, perform crop using embedded UCropView if available
                val croppedFirstUri = cropWithUCropViewIfPresent(currentImageUri) ?: currentImageUri
                val inputStream = context.contentResolver.openInputStream(croppedFirstUri!!)
                var bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()

                // Apply transformations (flip + color); rotation is handled by UCrop
                val matrix = Matrix().apply {
                    if (editState.flipHorizontal) {
                        postScale(-1f, 1f, bitmap.width / 2f, bitmap.height / 2f)
                    }
                    if (editState.flipVertical) {
                        postScale(1f, -1f, bitmap.width / 2f, bitmap.height / 2f)
                    }
                    // Do not apply editState.rotation here to avoid double-rotation
                }

                bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)

                val colorMatrix = ColorMatrix().apply {
                    val brightness = editState.brightness * 255
                    postConcat(ColorMatrix(floatArrayOf(
                        1f, 0f, 0f, 0f, brightness,
                        0f, 1f, 0f, 0f, brightness,
                        0f, 0f, 1f, 0f, brightness,
                        0f, 0f, 0f, 1f, 0f
                    )))
                    postConcat(ColorMatrix().apply { setSaturation(editState.saturation) })
                    val c = editState.contrast
                    postConcat(ColorMatrix(floatArrayOf(
                        c, 0f, 0f, 0f, 128 * (1 - c),
                        0f, c, 0f, 0f, 128 * (1 - c),
                        0f, 0f, c, 0f, 128 * (1 - c),
                        0f, 0f, 0f, 1f, 0f
                    )))
                }

                val paint = android.graphics.Paint().apply { colorFilter = ColorMatrixColorFilter(colorMatrix) }
                val resultBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config ?: Bitmap.Config.ARGB_8888)
                val canvas = android.graphics.Canvas(resultBitmap)
                canvas.drawBitmap(bitmap, 0f, 0f, paint)

                val file = File(context.cacheDir, "edited_${System.currentTimeMillis()}_$currentImageIndex.jpg")
                FileOutputStream(file).use { out ->
                    resultBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                }

                Uri.fromFile(file)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    fun handleNext() {
        scope.launch {
            isProcessing = true
            // Trigger a crop to persist current crop box before saving adjustments
            val maybeCropped = cropWithUCropViewIfPresent(currentImageUri)
            if (maybeCropped != null) {
                croppedImageUris = croppedImageUris + (currentImageIndex to maybeCropped)
            }
            val editedUri = saveEditedImage()
            if (editedUri != null) {
                editedImages = editedImages + editedUri
                if (currentImageIndex < selectedImages.size - 1) {
                    currentImageIndex++
                    editState = ImageEditState()
                } else {
                    onImagesCropped(editedImages)
                }
            }
            isProcessing = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.Black.copy(alpha = 0.8f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Page indicator
                        if (selectedImages.size > 1) {
                            Box(
                                modifier = Modifier
                                    .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "${currentImageIndex + 1}/${selectedImages.size}",
                                    color = Color.White,
                                    fontSize = 14.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                        }

                        TextButton(
                            onClick = { handleNext() },
                            enabled = !isProcessing
                        ) {
                            if (isProcessing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    text = if (currentImageIndex < selectedImages.size - 1) "Next" else "Done",
                                    color = Color.Red,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }
                }
            }

            // Image Editor Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                if (currentImageUri != null) {
                    if (editMode == EditMode.CROP) {
                        AndroidView<UCropView>(
                            modifier = Modifier.fillMaxSize(),
                            factory = { ctx: Context ->
                                UCropView(ctx, null).also { view ->
                                    uCropViewRef = view
                                    configureUCropView(view, currentImageUri)
                                }
                            },
                            update = { view: UCropView ->
                                configureUCropView(view, currentImageUri)
                            }
                        )
                    } else {
                        ImageEditCanvas(
                            imageUri = currentImageUri,
                            editState = editState,
                            onEditStateChange = { editState = it }
                        )
                    }
                }
            }

            // Edit Mode Selector
            EditModeSelector(selectedMode = editMode, onModeSelected = { editMode = it })

            // Controls
            when (editMode) {
                EditMode.CROP -> CropControls(
                    editState = editState,
                    onEditStateChange = { editState = it },
                    onRotate = { degrees ->
                        uCropViewRef?.cropImageView?.postRotate(degrees)
                        uCropViewRef?.cropImageView?.setImageToWrapCropBounds(true)
                    },
                    onFlipH = {
                        val src = currentImageUri ?: return@CropControls
                        scope.launch {
                            isProcessing = true
                            val flippedUri = flipCurrentImageHorizontal(src)
                            flippedUri?.let { uri ->
                                val updated = croppedImageUris.toMutableMap()
                                updated[currentImageIndex] = uri
                                croppedImageUris = updated.toMap()
                                uCropViewRef?.let { configureUCropView(it, uri) }
                            }
                            isProcessing = false
                        }
                    }
                )
                EditMode.FILTER -> FilterControls(editState, onEditStateChange = { editState = it })
                EditMode.ADJUST -> AdjustControls(editState, onEditStateChange = { editState = it })
                EditMode.STICKER -> StickerControls()
                EditMode.MORE -> MoreControls()
            }
        }
    }
}

@Composable
fun ImageEditCanvas(
    imageUri: Uri,
    editState: ImageEditState,
    onEditStateChange: (ImageEditState) -> Unit
) {
    var scale by remember { mutableFloatStateOf(editState.scale) }
    var offsetX by remember { mutableFloatStateOf(editState.offsetX) }
    var offsetY by remember { mutableFloatStateOf(editState.offsetY) }
    var rotation by remember { mutableFloatStateOf(editState.rotation) }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Image layer with transform gestures
        Image(
            painter = rememberAsyncImagePainter(imageUri),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, rotationDelta ->
                        scale = (scale * zoom).coerceIn(0.3f, 4f)
                        offsetX += pan.x
                        offsetY += pan.y
                        rotation += rotationDelta

                        onEditStateChange(
                            editState.copy(
                                scale = scale,
                                offsetX = offsetX,
                                offsetY = offsetY,
                                rotation = rotation
                            )
                        )
                    }
                }
                .graphicsLayer(
                    scaleX = scale * if (editState.flipHorizontal) -1f else 1f,
                    scaleY = scale * if (editState.flipVertical) -1f else 1f,
                    rotationZ = rotation,
                    translationX = offsetX,
                    translationY = offsetY
                ),
            contentScale = ContentScale.Fit,
            colorFilter = ColorFilter.colorMatrix(
                androidx.compose.ui.graphics.ColorMatrix().apply {
                    val brightness = editState.brightness * 255
                    val contrastMatrix = androidx.compose.ui.graphics.ColorMatrix(floatArrayOf(
                        editState.contrast, 0f, 0f, 0f, brightness,
                        0f, editState.contrast, 0f, 0f, brightness,
                        0f, 0f, editState.contrast, 0f, brightness,
                        0f, 0f, 0f, 1f, 0f
                    ))
                    val saturationMatrix = androidx.compose.ui.graphics.ColorMatrix().apply {
                        setToSaturation(editState.saturation)
                    }
                    this.timesAssign(contrastMatrix)
                    this.timesAssign(saturationMatrix)
                }
            )
        )
    }
}

@Composable
fun EditModeSelector(
    selectedMode: EditMode,
    onModeSelected: (EditMode) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.9f))
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        EditModeButton(
            icon = Icons.Default.CropRotate,
            label = "Crop",
            isSelected = selectedMode == EditMode.CROP,
            onClick = { onModeSelected(EditMode.CROP) }
        )
        EditModeButton(
            icon = Icons.Default.FilterVintage,
            label = "Filter",
            isSelected = selectedMode == EditMode.FILTER,
            onClick = { onModeSelected(EditMode.FILTER) }
        )
        EditModeButton(
            icon = Icons.Default.Tune,
            label = "Adjust",
            isSelected = selectedMode == EditMode.ADJUST,
            onClick = { onModeSelected(EditMode.ADJUST) }
        )
        EditModeButton(
            icon = Icons.Default.EmojiEmotions,
            label = "Sticker",
            isSelected = selectedMode == EditMode.STICKER,
            onClick = { onModeSelected(EditMode.STICKER) }
        )
        EditModeButton(
            icon = Icons.Default.MoreHoriz,
            label = "More",
            isSelected = selectedMode == EditMode.MORE,
            onClick = { onModeSelected(EditMode.MORE) }
        )
    }
}

@Composable
fun EditModeButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) Color(0xFFFFD700) else Color.White.copy(alpha = 0.6f),
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = if (isSelected) Color(0xFFFFD700) else Color.White.copy(alpha = 0.6f),
            fontSize = 11.sp
        )
    }
}

@Composable
fun CropControls(
    editState: ImageEditState,
    onEditStateChange: (ImageEditState) -> Unit,
    onRotate: (Float) -> Unit,
    onFlipH: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.9f))
            .padding(16.dp)
    ) {
        Text(
            text = "Straighten",
            color = Color.White,
            fontSize = 14.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Slider(
            value = editState.rotation,
            onValueChange = { newValue ->
                val delta = newValue - editState.rotation
                onRotate(delta)
                onEditStateChange(editState.copy(rotation = newValue))
            },
            valueRange = -180f..180f,
            colors = SliderDefaults.colors(thumbColor = Color(0xFFFFD700), activeTrackColor = Color(0xFFFFD700))
        )

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { onRotate(-90f) },
                modifier = Modifier.size(48.dp).background(Color.White.copy(alpha = 0.15f), CircleShape)
            ) {
                Icon(imageVector = Icons.AutoMirrored.Filled.RotateLeft, contentDescription = "Rotate -90", tint = Color.White)
            }
            IconButton(
                onClick = onFlipH,
                modifier = Modifier.size(48.dp).background(Color.White.copy(alpha = 0.15f), CircleShape)
            ) {
                Icon(imageVector = Icons.Default.Flip, contentDescription = "Flip H", tint = Color.White)
            }
        }
    }
}

@Composable
fun FilterControls(
    editState: ImageEditState,
    onEditStateChange: (ImageEditState) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.9f))
            .padding(16.dp)
    ) {
        Text(
            text = "Filters",
            color = Color.White,
            fontSize = 14.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            FilterPreset("Original", onClick = {
                onEditStateChange(editState.copy(brightness = 0f, contrast = 1f, saturation = 1f))
            })
            FilterPreset("Bright", onClick = {
                onEditStateChange(editState.copy(brightness = 0.2f, contrast = 1.1f, saturation = 1.2f))
            })
            FilterPreset("Dark", onClick = {
                onEditStateChange(editState.copy(brightness = -0.2f, contrast = 1.2f, saturation = 0.8f))
            })
            FilterPreset("B&W", onClick = {
                onEditStateChange(editState.copy(saturation = 0f, contrast = 1.3f))
            })
        }
    }
}

@Composable
fun FilterPreset(label: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .background(Color.Gray, RoundedCornerShape(8.dp))
                .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = Color.White,
            fontSize = 11.sp
        )
    }
}

@Composable
fun AdjustControls(
    editState: ImageEditState,
    onEditStateChange: (ImageEditState) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.9f))
            .padding(16.dp)
    ) {
        // Brightness
        Text(
            text = "Brightness",
            color = Color.White,
            fontSize = 14.sp
        )
        Slider(
            value = editState.brightness,
            onValueChange = { onEditStateChange(editState.copy(brightness = it)) },
            valueRange = -0.5f..0.5f,
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFFFFD700),
                activeTrackColor = Color(0xFFFFD700)
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Contrast
        Text(
            text = "Contrast",
            color = Color.White,
            fontSize = 14.sp
        )
        Slider(
            value = editState.contrast,
            onValueChange = { onEditStateChange(editState.copy(contrast = it)) },
            valueRange = 0.5f..2f,
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFFFFD700),
                activeTrackColor = Color(0xFFFFD700)
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Saturation
        Text(
            text = "Saturation",
            color = Color.White,
            fontSize = 14.sp
        )
        Slider(
            value = editState.saturation,
            onValueChange = { onEditStateChange(editState.copy(saturation = it)) },
            valueRange = 0f..2f,
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFFFFD700),
                activeTrackColor = Color(0xFFFFD700)
            )
        )
    }
}

@Composable
fun StickerControls() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.9f))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Stickers coming soon...",
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 14.sp
        )
    }
}

@Composable
fun MoreControls() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.9f))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "More options coming soon...",
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 14.sp
        )
    }
}
