package com.labelscanner.app

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat

/**
 * The single Compose screen for Label Scanner.
 * Bare-bones UI: status, camera/gallery buttons, image preview, results.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LabelScannerScreen(activity: MainActivity) {
    val status by activity.uiStatus
    val bitmap by activity.capturedBitmap
    val modelOutput by activity.modelOutput
    val flaggedResults by activity.flaggedResults
    val isProcessing by activity.isProcessing

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Label Scanner") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Status indicator
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Text(
                    text = status,
                    modifier = Modifier.padding(16.dp),
                    fontSize = 14.sp
                )
            }

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        val hasPermission = ContextCompat.checkSelfPermission(
                            activity, Manifest.permission.CAMERA
                        ) == PackageManager.PERMISSION_GRANTED

                        if (hasPermission) {
                            activity.takePictureLauncher.launch(null)
                        } else {
                            activity.cameraPermissionLauncher.launch(
                                Manifest.permission.CAMERA
                            )
                        }
                    },
                    enabled = !isProcessing,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("📷 Take Photo")
                }

                Button(
                    onClick = {
                        activity.pickImageLauncher.launch("image/*")
                    },
                    enabled = !isProcessing,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("🖼️ Gallery")
                }
            }

            // Loading indicator
            if (isProcessing) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            // Image preview
            bitmap?.let { bmp ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = "Captured food label",
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 250.dp),
                        contentScale = ContentScale.Fit
                    )
                }
            }

            // Flagged ingredients (if any)
            if (flaggedResults.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFFF3E0) // Light orange warning
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "⚠️ Flagged Ingredients",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color(0xFFE65100)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        flaggedResults.forEach { (ingredient, note) ->
                            Text(
                                text = "• $ingredient",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = Color(0xFFBF360C)
                            )
                            Text(
                                text = "  $note",
                                fontSize = 13.sp,
                                color = Color(0xFF4E342E),
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                    }
                }
            }

            // Raw model output
            if (modelOutput.isNotEmpty()) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Model Output (raw)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = modelOutput,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            // Spacer at bottom for scroll comfort
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
