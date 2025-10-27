package com.secretspaces32.android.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.net.Uri
import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
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
import com.yalantis.ucrop.callback.BitmapCropCallback
import androidx.compose.ui.viewinterop.AndroidView
import com.zomato.photofilters.geometry.Point

// AndroidPhotoFilters
import com.zomato.photofilters.imageprocessors.Filter
import com.zomato.photofilters.imageprocessors.subfilters.*

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
    val cropOffsetY: Float = 0f,
    val selectedFilter: String = "Original" // Track selected filter
)

// Load native library for AndroidPhotoFilters
private var isNativeLibraryLoaded = false

private fun loadNativeLibrary() {
    if (!isNativeLibraryLoaded) {
        try {
            System.loadLibrary("NativeImageProcessor")
            isNativeLibraryLoaded = true
        } catch (t: Throwable) { // catch Errors too (e.g., UnsatisfiedLinkError)
            t.printStackTrace()
            isNativeLibraryLoaded = false
        }
    }
}

// Decode a smaller bitmap for preview to keep filtering fast
private fun decodeBitmapForPreview(context: Context, uri: Uri, maxDim: Int = 2048): Bitmap? {
    return try {
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
        val (w, h) = opts.outWidth to opts.outHeight
        if (w <= 0 || h <= 0) return null
        var sampleSize = 1
        var tmpW = w
        var tmpH = h
        while (tmpW > maxDim || tmpH > maxDim) {
            tmpW /= 2
            tmpH /= 2
            sampleSize *= 2
        }
        val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize; inPreferredConfig = Bitmap.Config.ARGB_8888 }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, decodeOpts) }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

// Ensure we always pass a mutable ARGB_8888 bitmap to the native filters
private fun ensureMutableArgb(bitmap: Bitmap): Bitmap {
    if (bitmap.config == Bitmap.Config.ARGB_8888 && bitmap.isMutable) return bitmap
    return bitmap.copy(Bitmap.Config.ARGB_8888, /* mutable = */ true)
}

// Apply AndroidPhotoFilters based on filter name
fun applyPhotoFilter(context: Context, bitmap: Bitmap, filterName: String): Bitmap {
    // Ensure native library is loaded
    loadNativeLibrary()

    val filter = when (filterName) {
        "Original" -> return bitmap
        "Bright" -> Filter().apply {
            addSubFilter(BrightnessSubFilter(30))
            addSubFilter(ContrastSubFilter(1.1f))
            addSubFilter(SaturationSubFilter(0.2f))
        }
        "Dark" -> Filter().apply {
            addSubFilter(BrightnessSubFilter(-30))
            addSubFilter(ContrastSubFilter(1.2f))
            addSubFilter(VignetteSubFilter(context, 100))
        }
        "B&W" -> Filter().apply {
            addSubFilter(SaturationSubFilter(0.0f))
            addSubFilter(ContrastSubFilter(1.3f))
        }
        "Warm" -> Filter().apply {
            addSubFilter(ColorOverlaySubFilter(200, 1.0f, 0.6f, 0.2f))
            addSubFilter(BrightnessSubFilter(10))
        }
        "Cool" -> Filter().apply {
            addSubFilter(ColorOverlaySubFilter(150, 0.2f, 0.5f, 1.0f))
            addSubFilter(ContrastSubFilter(1.1f))
        }
        "Vintage" -> Filter().apply {
            addSubFilter(ColorOverlaySubFilter(100, 1.0f, 0.8f, 0.5f))
            addSubFilter(ContrastSubFilter(1.2f))
            addSubFilter(VignetteSubFilter(context, 150))
        }
        "Sepia" -> Filter().apply {
            val red = arrayOf(
                Point(0f, 10f),
                Point(100f, 130f),
                Point(255f, 255f)
            )
            val green = arrayOf(
                Point(0f, 0f),
                Point(100f, 110f),
                Point(255f, 240f)
            )
            val blue = arrayOf(
                Point(0f, 0f),
                Point(100f, 80f),
                Point(255f, 210f)
            )
            addSubFilter(ToneCurveSubFilter(null, red, green, blue))
        }
        "Vivid" -> Filter().apply {
            addSubFilter(ContrastSubFilter(1.3f))
            addSubFilter(SaturationSubFilter(0.5f))
            addSubFilter(BrightnessSubFilter(5))
        }
        "Nashville" -> Filter().apply {
            addSubFilter(ColorOverlaySubFilter(100, 1.0f, 0.78f, 0.58f))
            addSubFilter(ContrastSubFilter(1.2f))
            addSubFilter(BrightnessSubFilter(5))
        }
        "Retro" -> Filter().apply {
            val rgb = arrayOf(
                Point(0f, 0f),
                Point(80f, 70f),
                Point(180f, 200f),
                Point(255f, 255f)
            )
            addSubFilter(ToneCurveSubFilter(rgb, null, null, null))
            addSubFilter(VignetteSubFilter(context, 120))
        }
        else -> return bitmap
    }

    return try {
        // Always work on a copy to avoid mutating the input bitmap
        val working = bitmap.copy(Bitmap.Config.ARGB_8888, /* mutable = */ true)
        filter.processFilter(working)
    } catch (e: Exception) {
        e.printStackTrace()
        bitmap
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageEditor(
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

    // Store filtered bitmap for preview
    var filteredBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // Small base preview for filter thumbnails
    var filterStripBaseBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // Use cropped uri if available for current index
    val currentImageUri: Uri? = run {
        val original = selectedImages.getOrNull(currentImageIndex)
        croppedImageUris[currentImageIndex] ?: original
    }

    // Decode a small preview for the filter strip when image changes
    LaunchedEffect(currentImageUri) {
        filterStripBaseBitmap = withContext(Dispatchers.IO) {
            currentImageUri?.let { decodeBitmapForPreview(context, it, maxDim = 320) }
        }
    }

    // Apply filter when selected filter changes
    LaunchedEffect(editState.selectedFilter, currentImageUri) {
        if (editState.selectedFilter != "Original" && currentImageUri != null) {
            val out: Bitmap? = withContext(Dispatchers.IO) {
                try {
                    val preview = decodeBitmapForPreview(context, currentImageUri)
                    preview?.let { applyPhotoFilter(context, it, editState.selectedFilter) }
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }
            filteredBitmap = out
        } else {
            filteredBitmap = null
        }
    }

    // Flip current image by writing a mirrored temporary file and return its Uri
    suspend fun flipCurrentImageHorizontal(source: Uri): Uri? = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(source)?.use { input ->
                val original = BitmapFactory.decodeStream(input)
                val m = Matrix().apply { postScale(-1f, 1f, original.width / 2f, original.height / 2f) }
                @Suppress("DEPRECATION")
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
        crop.isScaleEnabled = true
        crop.isRotateEnabled = true
        crop.setMaxResultImageSizeX(4096)
        crop.setMaxResultImageSizeY(4096)
        crop.setImageToWrapCropBounds(true)
        // Lock aspect ratio to 16:9 - crop box is resizable but maintains the ratio
        overlay.setShowCropGrid(true)
        overlay.setShowCropFrame(true)
        // DON'T set freestyleCropMode at all - this allows resizing with locked aspect ratio
        // Setting DISABLE would lock the frame completely
        // Setting ENABLE would allow free aspect ratio changes
        // Not setting it allows resize while maintaining the target aspect ratio
        overlay.setDimmedColor(Color.Black.copy(alpha = 0.6f).toArgb())
        // Set target aspect ratio to 16:9 - this locks the ratio during resize
        crop.targetAspectRatio = 16f / 9f

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
        @Suppress("DEPRECATION")
        kotlinx.coroutines.suspendCancellableCoroutine { cont ->
            cropView.cropAndSaveImage(
                Bitmap.CompressFormat.JPEG,
                90,
                object : BitmapCropCallback {
                    override fun onBitmapCropped(resultUri: Uri, offsetX: Int, offsetY: Int, imageWidth: Int, imageHeight: Int) {
                        cont.resume(resultUri) { }
                    }
                    override fun onCropFailure(t: Throwable) {
                        t.printStackTrace()
                        cont.resume(null) { }
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

                // Apply AndroidPhotoFilters based on selected filter
                bitmap = applyPhotoFilter(context, bitmap, editState.selectedFilter)

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

                @Suppress("DEPRECATION")
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

                @Suppress("DEPRECATION")
                val paint = android.graphics.Paint().apply { colorFilter = ColorMatrixColorFilter(colorMatrix) }
                val resultBitmap = androidx.core.graphics.createBitmap(bitmap.width, bitmap.height, bitmap.config ?: Bitmap.Config.ARGB_8888)
                @Suppress("DEPRECATION")
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
                        AndroidView(
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
                            onEditStateChange = { editState = it },
                            filteredBitmap = filteredBitmap
                        )
                    }
                }
            }

            // Controls section (above the mode selector buttons)
            when (editMode) {
                EditMode.CROP -> {
                    // Crop-specific controls (rotation/flip buttons and slider)
                    CropControlsSection(
                        editState = editState,
                        onEditStateChange = { editState = it },
                        onRotate = {
                            uCropViewRef?.cropImageView?.postRotate(-90f)
                            uCropViewRef?.cropImageView?.setImageToWrapCropBounds(true)
                        },
                        onRotateFine = { degrees ->
                            uCropViewRef?.cropImageView?.postRotate(degrees)
                            uCropViewRef?.cropImageView?.setImageToWrapCropBounds(true)
                        },
                        onFlipH = {
                            val src = currentImageUri
                            if (src != null) {
                                scope.launch {
                                    val flippedUri = flipCurrentImageHorizontal(src)
                                    flippedUri?.let { uri ->
                                        val updated = croppedImageUris.toMutableMap()
                                        updated[currentImageIndex] = uri
                                        croppedImageUris = updated.toMap()
                                        uCropViewRef?.let { configureUCropView(it, uri) }
                                    }
                                }
                            }
                        }
                    )
                }
                EditMode.FILTER -> FilterControls(baseBitmap = filterStripBaseBitmap, editState = editState, onEditStateChange = { editState = it })
                EditMode.ADJUST -> AdjustControls(editState, onEditStateChange = { editState = it })
                EditMode.STICKER -> StickerControls()
                EditMode.MORE -> MoreControls()
            }

            // Mode selector buttons (always at the bottom)
            EditModeSelector(
                selectedMode = editMode,
                onModeSelected = { editMode = it }
            )
        }
    }
}

@Composable
fun ImageEditCanvas(
    imageUri: Uri,
    editState: ImageEditState,
    onEditStateChange: (ImageEditState) -> Unit,
    filteredBitmap: Bitmap? = null
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
            painter = if (filteredBitmap != null) {
                BitmapPainter(filteredBitmap.asImageBitmap())
            } else {
                rememberAsyncImagePainter(imageUri)
            },
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
    // BOTTOM: Mode selector buttons (always at the same position)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.9f))
            .navigationBarsPadding()
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
            icon = Icons.Default.FilterList,
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
fun CropControlsSection(
    editState: ImageEditState,
    onEditStateChange: (ImageEditState) -> Unit,
    onRotate: () -> Unit,
    onRotateFine: (Float) -> Unit,
    onFlipH: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.9f))
            .padding(vertical = 12.dp)
    ) {
        // TOP: Rotation and Flip buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onRotate,
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

        // MIDDLE: Horizontal scrollbar for fine-tuning rotation
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            RotationSliderWithLines(
                value = editState.rotation,
                onValueChange = { newValue ->
                    val delta = newValue - editState.rotation
                    onRotateFine(delta)
                    onEditStateChange(editState.copy(rotation = newValue))
                }
            )
        }
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
fun RotationSliderWithLines(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val lineSpacing = 4.dp

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp),
        contentAlignment = Alignment.Center
    ) {
        // Scrollable lines background
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, _, _ ->
                        dragOffset += pan.x
                        // Map drag to rotation value (-180 to 180)
                        val sensitivity = 0.5f
                        val newValue = (value - pan.x * sensitivity).coerceIn(-180f, 180f)
                        onValueChange(newValue)
                    }
                }
        ) {
            val centerX = size.width / 2f
            val lineSpacingPx = lineSpacing.toPx()

            // Calculate offset based on rotation value
            val pixelsPerDegree = lineSpacingPx
            val scrollOffset = value * pixelsPerDegree

            // Draw vertical lines
            for (i in -200..200) {
                val xPos = centerX + (i * lineSpacingPx) - scrollOffset

                if (xPos >= 0 && xPos <= size.width) {
                    val degree = i

                    // Determine line height based on position
                    val lineHeight = when {
                        degree % 45 == 0 -> size.height * 0.7f // Major marks
                        degree % 15 == 0 -> size.height * 0.5f // Medium marks
                        degree % 5 == 0 -> size.height * 0.35f // Minor marks
                        else -> size.height * 0.25f // Smallest marks
                    }

                    // Determine color - center line is highlighted
                    val distanceFromCenter = kotlin.math.abs(xPos - centerX)
                    val alpha = if (distanceFromCenter < 3f) 1f else 0.4f
                    val color = if (distanceFromCenter < 3f) {
                        Color(0xFFFFD700) // Gold for center
                    } else {
                        Color.White.copy(alpha = alpha)
                    }

                    val yStart = (size.height - lineHeight) / 2f
                    val yEnd = yStart + lineHeight

                    drawLine(
                        color = color,
                        start = Offset(xPos, yStart),
                        end = Offset(xPos, yEnd),
                        strokeWidth = if (distanceFromCenter < 3f) 3f else 1.5f
                    )
                }
            }

            // Draw center indicator line (fixed position)
            drawLine(
                color = Color(0xFFFFD700),
                start = Offset(centerX, 0f),
                end = Offset(centerX, size.height),
                strokeWidth = 3f
            )
        }

        // Degree value display
        Text(
            text = "${value.toInt()}°",
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 4.dp)
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
fun FilterPreset(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    thumbnail: Bitmap? = null,
    isLoading: Boolean = false
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .background(Color.Gray, RoundedCornerShape(8.dp))
                .border(
                    width = if (isSelected) 3.dp else 1.dp,
                    color = if (isSelected) Color(0xFFFFD700) else Color.White.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(8.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                }
                thumbnail != null -> {
                    Image(
                        painter = BitmapPainter(thumbnail.asImageBitmap()),
                        contentDescription = label,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                else -> {
                    // empty base, keeps layout stable
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = if (isSelected) Color(0xFFFFD700) else Color.White,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
fun FilterControls(
    baseBitmap: Bitmap?,
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

        // Scrollable horizontal list of filters
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Original
            val origThumb = baseBitmap
            FilterPreset(
                label = "Original",
                isSelected = editState.selectedFilter == "Original",
                onClick = {
                    onEditStateChange(
                        editState.copy(
                            selectedFilter = "Original",
                            brightness = 0f,
                            contrast = 1f,
                            saturation = 1f
                        )
                    )
                },
                thumbnail = origThumb,
                isLoading = baseBitmap == null
            )

            // Bright
            val brightThumb by rememberFilterThumbnail(baseBitmap, "Bright")
            FilterPreset(
                label = "Bright",
                isSelected = editState.selectedFilter == "Bright",
                onClick = { onEditStateChange(editState.copy(selectedFilter = "Bright")) },
                thumbnail = brightThumb,
                isLoading = baseBitmap == null || (brightThumb == null)
            )

            // Dark
            val darkThumb by rememberFilterThumbnail(baseBitmap, "Dark")
            FilterPreset(
                label = "Dark",
                isSelected = editState.selectedFilter == "Dark",
                onClick = { onEditStateChange(editState.copy(selectedFilter = "Dark")) },
                thumbnail = darkThumb,
                isLoading = baseBitmap == null || (darkThumb == null)
            )

            // B&W
            val bwThumb by rememberFilterThumbnail(baseBitmap, "B&W")
            FilterPreset(
                label = "B&W",
                isSelected = editState.selectedFilter == "B&W",
                onClick = { onEditStateChange(editState.copy(selectedFilter = "B&W")) },
                thumbnail = bwThumb,
                isLoading = baseBitmap == null || (bwThumb == null)
            )

            // Warm
            val warmThumb by rememberFilterThumbnail(baseBitmap, "Warm")
            FilterPreset(
                label = "Warm",
                isSelected = editState.selectedFilter == "Warm",
                onClick = { onEditStateChange(editState.copy(selectedFilter = "Warm")) },
                thumbnail = warmThumb,
                isLoading = baseBitmap == null || (warmThumb == null)
            )

            // Cool
            val coolThumb by rememberFilterThumbnail(baseBitmap, "Cool")
            FilterPreset(
                label = "Cool",
                isSelected = editState.selectedFilter == "Cool",
                onClick = { onEditStateChange(editState.copy(selectedFilter = "Cool")) },
                thumbnail = coolThumb,
                isLoading = baseBitmap == null || (coolThumb == null)
            )

            // Vintage
            val vintageThumb by rememberFilterThumbnail(baseBitmap, "Vintage")
            FilterPreset(
                label = "Vintage",
                isSelected = editState.selectedFilter == "Vintage",
                onClick = { onEditStateChange(editState.copy(selectedFilter = "Vintage")) },
                thumbnail = vintageThumb,
                isLoading = baseBitmap == null || (vintageThumb == null)
            )

            // Sepia
            val sepiaThumb by rememberFilterThumbnail(baseBitmap, "Sepia")
            FilterPreset(
                label = "Sepia",
                isSelected = editState.selectedFilter == "Sepia",
                onClick = { onEditStateChange(editState.copy(selectedFilter = "Sepia")) },
                thumbnail = sepiaThumb,
                isLoading = baseBitmap == null || (sepiaThumb == null)
            )

            // Vivid
            val vividThumb by rememberFilterThumbnail(baseBitmap, "Vivid")
            FilterPreset(
                label = "Vivid",
                isSelected = editState.selectedFilter == "Vivid",
                onClick = { onEditStateChange(editState.copy(selectedFilter = "Vivid")) },
                thumbnail = vividThumb,
                isLoading = baseBitmap == null || (vividThumb == null)
            )

            // Nashville
            val nashThumb by rememberFilterThumbnail(baseBitmap, "Nashville")
            FilterPreset(
                label = "Nashville",
                isSelected = editState.selectedFilter == "Nashville",
                onClick = { onEditStateChange(editState.copy(selectedFilter = "Nashville")) },
                thumbnail = nashThumb,
                isLoading = baseBitmap == null || (nashThumb == null)
            )

            // Retro
            val retroThumb by rememberFilterThumbnail(baseBitmap, "Retro")
            FilterPreset(
                label = "Retro",
                isSelected = editState.selectedFilter == "Retro",
                onClick = { onEditStateChange(editState.copy(selectedFilter = "Retro")) },
                thumbnail = retroThumb,
                isLoading = baseBitmap == null || (retroThumb == null)
            )
        }
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

@Composable
private fun rememberFilterThumbnail(base: Bitmap?, filterName: String): State<Bitmap?> {
    val context = LocalContext.current
    val thumbState = remember(base, filterName) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(base, filterName) {
        if (base == null) {
            thumbState.value = null
        } else {
            val bmp = withContext(Dispatchers.IO) {
                try {
                    if (filterName == "Original") base else applyPhotoFilter(context, base, filterName)
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }
            thumbState.value = bmp
        }
    }
    return thumbState
}
