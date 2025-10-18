package com.secretspaces32.android.ui.screens

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.secretspaces32.android.data.model.User
import com.secretspaces32.android.ui.theme.DarkBackground
import com.secretspaces32.android.ui.theme.DarkSurface
import java.io.File

data class GalleryImage(
    val uri: Uri,
    val id: Long
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropSecretScreen(
    isLoading: Boolean,
    onNext: (List<Uri>) -> Unit,
    onBack: () -> Unit = {},
    cacheDir: File? = null,
    currentUser: User? = null
) {
    val context = LocalContext.current
    var selectedImageUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var galleryImages by remember { mutableStateOf<List<GalleryImage>>(emptyList()) }
    var isMultiSelectMode by remember { mutableStateOf(false) }
    var hasGalleryPermission by remember { mutableStateOf(false) }

    // Gallery permission launcher
    val galleryPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasGalleryPermission = isGranted
        if (isGranted) {
            galleryImages = loadGalleryImages(context)
        }
    }

    // Check and request permission on launch
    LaunchedEffect(Unit) {
        val permission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            android.Manifest.permission.READ_MEDIA_IMAGES
        } else {
            android.Manifest.permission.READ_EXTERNAL_STORAGE
        }

        hasGalleryPermission = androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            permission
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (hasGalleryPermission) {
            galleryImages = loadGalleryImages(context)
        } else {
            galleryPermissionLauncher.launch(permission)
        }
    }

    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            // Image was captured, refresh gallery
            galleryImages = loadGalleryImages(context)
        }
    }

    // Camera permission launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Create temp file for camera image
            cacheDir?.let { dir ->
                val photoFile = File.createTempFile(
                    "secret_photo_${System.currentTimeMillis()}",
                    ".jpg",
                    dir
                )
                val photoUri = androidx.core.content.FileProvider.getUriForFile(
                    context,
                    "com.secretspaces32.android.fileprovider",
                    photoFile
                )
                cameraLauncher.launch(photoUri)
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
                    // Back arrow and New post text
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
                            text = "New post",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    // Next button
                    TextButton(
                        onClick = {
                            if (selectedImageUris.isNotEmpty()) {
                                onNext(selectedImageUris)
                            }
                        },
                        enabled = selectedImageUris.isNotEmpty() && !isLoading
                    ) {
                        Text(
                            text = "Next",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedImageUris.isNotEmpty()) Color(0xFF4D9FFF) else Color.White.copy(alpha = 0.5f)
                        )
                    }
                }
            }

            // Main Content - Image Preview
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.6f)
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                if (selectedImageUris.isNotEmpty()) {
                    AsyncImage(
                        model = selectedImageUris.first(),
                        contentDescription = "Selected image preview",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )

                    // Image counter if multiple selected
                    if (selectedImageUris.size > 1) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(16.dp),
                            shape = RoundedCornerShape(20.dp),
                            color = Color.Black.copy(alpha = 0.6f)
                        ) {
                            Text(
                                text = "1/${selectedImageUris.size}",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                color = Color.White,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else {
                    Text(
                        text = "Select photos",
                        color = Color.White.copy(alpha = 0.5f),
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            }

            // Bottom Gallery Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.4f)
                    .background(DarkBackground)
            ) {
                // Recents header with SELECT MULTIPLE button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Recents",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Dropdown",
                            tint = Color.White,
                            modifier = Modifier
                                .size(20.dp)
                                .padding(start = 4.dp)
                        )
                    }

                    Surface(
                        onClick = {
                            isMultiSelectMode = !isMultiSelectMode
                            if (!isMultiSelectMode) {
                                // Keep only first selected image when exiting multi-select
                                selectedImageUris = selectedImageUris.take(1)
                            }
                        },
                        shape = RoundedCornerShape(20.dp),
                        color = if (isMultiSelectMode) Color(0xFF4D9FFF) else Color.Transparent,
                        border = if (!isMultiSelectMode) androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)) else null
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "SELECT MULTIPLE",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                // Gallery Grid
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    // Camera item (first item)
                    item {
                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .background(Color(0xFF1C1C1E))
                                .clickable {
                                    cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Take Photo",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    // Gallery images
                    items(galleryImages) { image ->
                        val isSelected = selectedImageUris.contains(image.uri)
                        val selectionNumber = if (isSelected) selectedImageUris.indexOf(image.uri) + 1 else 0

                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clickable {
                                    if (isMultiSelectMode) {
                                        selectedImageUris = if (isSelected) {
                                            selectedImageUris.filter { it != image.uri }
                                        } else {
                                            selectedImageUris + image.uri
                                        }
                                    } else {
                                        selectedImageUris = listOf(image.uri)
                                    }
                                }
                        ) {
                            AsyncImage(
                                model = image.uri,
                                contentDescription = "Gallery image",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )

                            // Selection overlay
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color(0xFF4D9FFF).copy(alpha = 0.3f))
                                )

                                // Selection number
                                if (isMultiSelectMode) {
                                    Surface(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(4.dp)
                                            .size(24.dp),
                                        shape = CircleShape,
                                        color = Color(0xFF4D9FFF)
                                    ) {
                                        Box(
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = selectionNumber.toString(),
                                                color = Color.White,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Selected",
                                        tint = Color(0xFF4D9FFF),
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(4.dp)
                                            .size(24.dp)
                                    )
                                }
                            } else if (isMultiSelectMode) {
                                // Show empty circle in multi-select mode
                                Surface(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(4.dp)
                                        .size(24.dp),
                                    shape = CircleShape,
                                    color = Color.Transparent,
                                    border = androidx.compose.foundation.BorderStroke(2.dp, Color.White)
                                ) {}
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun loadGalleryImages(context: Context): List<GalleryImage> {
    val images = mutableListOf<GalleryImage>()
    val projection = arrayOf(
        MediaStore.Images.Media._ID,
        MediaStore.Images.Media.DATE_ADDED
    )

    val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

    context.contentResolver.query(
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        projection,
        null,
        null,
        sortOrder
    )?.use { cursor ->
        val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)

        while (cursor.moveToNext() && images.size < 100) { // Limit to 100 recent images
            val id = cursor.getLong(idColumn)
            val contentUri = ContentUris.withAppendedId(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                id
            )
            images.add(GalleryImage(contentUri, id))
        }
    }

    return images
}
