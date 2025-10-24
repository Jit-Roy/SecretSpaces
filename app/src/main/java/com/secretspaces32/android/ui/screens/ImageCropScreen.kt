package com.secretspaces32.android.ui.screens

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.secretspaces32.android.ui.theme.DarkBackground
import com.yalantis.ucrop.UCrop
import com.yalantis.ucrop.UCropActivity
import java.io.File
import androidx.core.graphics.toColorInt

@Composable
fun ImageCropScreen(
    selectedImages: List<Uri>,
    onImagesCropped: (List<Uri>) -> Unit,
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    var currentImageIndex by remember { mutableIntStateOf(0) }
    var croppedImages by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var isProcessing by remember { mutableStateOf(false) }
    var hasLaunched by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Create launcher callback function
    fun launchNextImage(launcher: androidx.activity.result.ActivityResultLauncher<Intent>) {
        println("DEBUG: ImageCropScreen - launchNextImage called for index $currentImageIndex")
        if (currentImageIndex < selectedImages.size) {
            val sourceUri = selectedImages[currentImageIndex]
            val destinationFileName = "cropped_${System.currentTimeMillis()}_$currentImageIndex.jpg"
            val destinationUri = Uri.fromFile(File(context.cacheDir, destinationFileName))

            println("DEBUG: ImageCropScreen - Creating UCrop intent for $sourceUri")

            val uCropIntent = UCrop.of(sourceUri, destinationUri)
                .withAspectRatio(1f, 1f) // Changed to square aspect ratio for Instagram-like posts
                .withMaxResultSize(2000, 2000)
                .withOptions(UCrop.Options().apply {
                    setCompressionQuality(90)
                    setHideBottomControls(false)
                    setFreeStyleCropEnabled(true) // Allow free crop
                    setToolbarColor("#1C1C1E".toColorInt())
                    setStatusBarColor("#1C1C1E".toColorInt())
                    setActiveControlsWidgetColor("#FF0000".toColorInt()) // Red accent color
                    setToolbarWidgetColor(android.graphics.Color.WHITE)
                    setRootViewBackgroundColor(android.graphics.Color.BLACK)
                    setLogoColor("#FF0000".toColorInt())
                    setShowCropFrame(true)
                    setShowCropGrid(true)
                    setCropGridStrokeWidth(2)
                    setCropFrameStrokeWidth(4)
                    setAllowedGestures(UCropActivity.ALL, UCropActivity.ALL, UCropActivity.ALL)
                    setToolbarTitle("Edit Image ${currentImageIndex + 1}/${selectedImages.size}")
                })
                .getIntent(context)
                .apply {
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                }

            try {
                println("DEBUG: ImageCropScreen - Launching UCrop activity")
                launcher.launch(uCropIntent)
            } catch (e: Exception) {
                println("ERROR: ImageCropScreen - Failed to launch UCrop: ${e.message}")
                e.printStackTrace()
                errorMessage = "Failed to open crop editor: ${e.message}"
                // Fallback: skip cropping and continue with original images
                isProcessing = false
                onImagesCropped(selectedImages)
            }
        }
    }

    // Define the launcher
    val uCropLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        println("DEBUG: ImageCropScreen - UCrop result received with code: ${result.resultCode}")
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val resultUri = UCrop.getOutput(result.data!!)
            println("DEBUG: ImageCropScreen - Cropped URI: $resultUri")
            if (resultUri != null) {
                croppedImages = croppedImages + resultUri

                // Move to next image or finish
                if (currentImageIndex < selectedImages.size - 1) {
                    println("DEBUG: ImageCropScreen - Moving to next image")
                    // Advance the index; the subsequent LaunchedEffect will trigger the next launch
                    currentImageIndex++
                } else {
                    println("DEBUG: ImageCropScreen - All images cropped, total: ${croppedImages.size}")
                    // All images cropped
                    isProcessing = false
                    onImagesCropped(croppedImages)
                }
            } else {
                println("ERROR: ImageCropScreen - Result URI is null")
                // No URI returned despite OK result
                isProcessing = false
                onBack()
            }
        } else if (result.resultCode == Activity.RESULT_CANCELED) {
            println("DEBUG: ImageCropScreen - User cancelled cropping")
            // User cancelled, go back
            isProcessing = false
            onBack()
        } else {
            // Error occurred
            val cropError = result.data?.let { UCrop.getError(it) }
            println("ERROR: ImageCropScreen - Crop error: ${cropError?.message}")
            cropError?.printStackTrace()
            errorMessage = "Crop failed: ${cropError?.message}"
            isProcessing = false
            onBack()
        }
    }

    // Launch uCrop for the first image when selection arrives
    LaunchedEffect(selectedImages) {
        println("DEBUG: ImageCropScreen - LaunchedEffect triggered with ${selectedImages.size} images")
        if (selectedImages.isNotEmpty() && !hasLaunched) {
            println("DEBUG: ImageCropScreen - Starting crop process")
            hasLaunched = true
            isProcessing = true
            currentImageIndex = 0
            // Add a small delay to ensure UI is ready
            kotlinx.coroutines.delay(300)
            launchNextImage(uCropLauncher)
        }
    }

    // Launch subsequent images when index advances
    LaunchedEffect(currentImageIndex) {
        if (isProcessing && currentImageIndex > 0 && currentImageIndex < selectedImages.size) {
            println("DEBUG: ImageCropScreen - Launching next image at index $currentImageIndex")
            // Add a small delay between crops
            kotlinx.coroutines.delay(300)
            launchNextImage(uCropLauncher)
        }
    }

    // Show a loading screen while transitioning
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            if (errorMessage != null) {
                Text(
                    text = "Error",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    color = Color.Red
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = errorMessage!!,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onBack,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Go Back")
                }
            } else {
                CircularProgressIndicator(color = Color.Red)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = if (isProcessing) {
                        "Preparing crop editor..."
                    } else {
                        "Loading..."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.7f)
                )
                if (selectedImages.isNotEmpty() && currentImageIndex < selectedImages.size) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Image ${currentImageIndex + 1} of ${selectedImages.size}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}
