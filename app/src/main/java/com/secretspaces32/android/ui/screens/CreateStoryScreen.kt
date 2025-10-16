package com.secretspaces32.android.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.secretspaces32.android.ui.theme.DarkBackground
import com.secretspaces32.android.ui.theme.DarkSurface
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateStoryScreen(
    isLoading: Boolean,
    onCreateStory: (Uri?, String) -> Unit,
    onBack: () -> Unit = {},
    cacheDir: File? = null
) {
    var storyText by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var selectedBackgroundColor by remember { mutableStateOf(0) } // 0-5 for different colors
    val context = androidx.compose.ui.platform.LocalContext.current

    // Available background colors for text stories
    val backgroundColors = listOf(
        Brush.linearGradient(listOf(Color(0xFFFF6B6B), Color(0xFFFF8E53))),
        Brush.linearGradient(listOf(Color(0xFF4E54C8), Color(0xFF8F94FB))),
        Brush.linearGradient(listOf(Color(0xFF11998E), Color(0xFF38EF7D))),
        Brush.linearGradient(listOf(Color(0xFFFA8BFF), Color(0xFF2BD2FF))),
        Brush.linearGradient(listOf(Color(0xFFF093FB), Color(0xFFF5576C))),
        Brush.linearGradient(listOf(Color(0xFF4776E6), Color(0xFF8E54E9)))
    )

    // Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
    }

    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (!success) {
            selectedImageUri = null
        }
    }

    // Camera permission launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            cacheDir?.let { dir ->
                val photoFile = File.createTempFile(
                    "story_photo_${System.currentTimeMillis()}",
                    ".jpg",
                    dir
                )
                val photoUri = androidx.core.content.FileProvider.getUriForFile(
                    context,
                    "com.secretspaces32.android.fileprovider",
                    photoFile
                )
                selectedImageUri = photoUri
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
                        .padding(start = 4.dp, end = 20.dp, top = 16.dp, bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
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
                            text = "Create Story",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    // Post button
                    Button(
                        onClick = {
                            if (selectedImageUri != null || storyText.isNotBlank()) {
                                onCreateStory(selectedImageUri, storyText)
                            }
                        },
                        enabled = (selectedImageUri != null || storyText.isNotBlank()) && !isLoading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFF4D4D),
                            disabledContainerColor = Color.White.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Share", color = Color.White)
                        }
                    }
                }
            }

            // Main Content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Story Preview Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(500.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = DarkSurface
                    )
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        if (selectedImageUri != null) {
                            // Image Story Preview
                            AsyncImage(
                                model = selectedImageUri,
                                contentDescription = "Story image",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )

                            // Text overlay on image (if text is added)
                            if (storyText.isNotBlank()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.3f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = storyText,
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(32.dp)
                                    )
                                }
                            }
                        } else {
                            // Text Story Preview with colored background - now editable
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(backgroundColors[selectedBackgroundColor]),
                                contentAlignment = Alignment.Center
                            ) {
                                BasicTextField(
                                    value = storyText,
                                    onValueChange = { storyText = it },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    textStyle = TextStyle(
                                        fontSize = MaterialTheme.typography.headlineMedium.fontSize,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        textAlign = TextAlign.Center
                                    ),
                                    decorationBox = { innerTextField ->
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            if (storyText.isEmpty()) {
                                                Column(
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Edit,
                                                        contentDescription = null,
                                                        tint = Color.White.copy(alpha = 0.7f),
                                                        modifier = Modifier.size(48.dp)
                                                    )
                                                    Text(
                                                        text = "Type something...",
                                                        style = MaterialTheme.typography.titleMedium,
                                                        color = Color.White.copy(alpha = 0.7f)
                                                    )
                                                }
                                            }
                                            innerTextField()
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                // Text Input Section - Only show caption field when image is selected
                if (selectedImageUri != null) {
                    OutlinedTextField(
                        value = storyText,
                        onValueChange = { storyText = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text(
                                text = "Add a caption...",
                                color = Color.White.copy(alpha = 0.5f)
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFFFF4D4D),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                            cursorColor = Color(0xFFFF4D4D)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        maxLines = 3
                    )
                }

                // Media Options
                Text(
                    text = "Add Media",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Gallery Button
                    Button(
                        onClick = { imagePickerLauncher.launch("image/*") },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DarkSurface
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = "Gallery",
                            tint = Color(0xFFFF4D4D)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Gallery", color = Color.White)
                    }

                    // Camera Button
                    Button(
                        onClick = {
                            cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DarkSurface
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Camera",
                            tint = Color(0xFFFF4D4D)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Camera", color = Color.White)
                    }
                }

                // Remove Image Button (if image is selected)
                if (selectedImageUri != null) {
                    TextButton(
                        onClick = { selectedImageUri = null },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Remove image",
                            tint = Color(0xFFFF4D4D)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Remove Image", color = Color(0xFFFF4D4D))
                    }
                }

                // Background Color Selector (only for text stories)
                if (selectedImageUri == null) {
                    Text(
                        text = "Background Color",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        backgroundColors.forEachIndexed { index, brush ->
                            Box(
                                modifier = Modifier
                                    .size(50.dp)
                                    .background(brush, CircleShape)
                                    .border(
                                        width = if (selectedBackgroundColor == index) 3.dp else 1.dp,
                                        color = if (selectedBackgroundColor == index) Color.White else Color.White.copy(
                                            alpha = 0.3f
                                        ),
                                        shape = CircleShape
                                    )
                                    .clickable { selectedBackgroundColor = index }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}
