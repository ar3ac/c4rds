package com.example.mycards.ui.scan

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarcodeScannerScreen(
    onBarcodeDetected: (value: String, format: String) -> Unit,
    onManualInputClick: () -> Unit,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scansiona Barcode") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Indietro")
                    }
                },
                actions = {
                    TextButton(onClick = onManualInputClick) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Manuale")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (hasCameraPermission) {
                CameraPreviewWithAnalyzer(
                    onBarcodeDetected = onBarcodeDetected
                )

                // Overlay mirino per scansione
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val boxWidth = size.width * 0.8f
                    val boxHeight = size.height * 0.35f
                    val left = (size.width - boxWidth) / 2
                    val top = (size.height - boxHeight) / 2

                    // Sfondo semitrasparente
                    drawRect(
                        color = Color.Black.copy(alpha = 0.5f)
                    )

                    // Finestra trasparente al centro
                    drawRoundRect(
                        color = Color.Transparent,
                        topLeft = Offset(left, top),
                        size = Size(boxWidth, boxHeight),
                        cornerRadius = CornerRadius(16.dp.toPx()),
                        blendMode = BlendMode.Clear
                    )

                    // Bordo mirino verde/azzurro
                    drawRoundRect(
                        color = Color(0xFF00E676),
                        topLeft = Offset(left, top),
                        size = Size(boxWidth, boxHeight),
                        cornerRadius = CornerRadius(16.dp.toPx()),
                        style = Stroke(width = 3.dp.toPx())
                    )
                }

                Text(
                    text = "Inquadra il codice a barre o QR",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 48.dp)
                        .background(
                            Color.Black.copy(alpha = 0.7f),
                            shape = MaterialTheme.shapes.medium
                        )
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Permesso Fotocamera Richiesto",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "L'app necessita dell'accesso alla fotocamera esclusivamente per leggere i codici a barre delle tue tessere.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }
                    ) {
                        Text("Concedi Permesso")
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(onClick = onManualInputClick) {
                        Text("Inserisci Manualmente")
                    }
                }
            }
        }
    }
}

@Composable
fun CameraPreviewWithAnalyzer(
    onBarcodeDetected: (value: String, format: String) -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    var isDetected by remember { mutableStateOf(false) }

    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }

                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()

                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    val barcodeScannerOptions = BarcodeScannerOptions.Builder()
                        .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
                        .build()

                    val barcodeScanner = BarcodeScanning.getClient(barcodeScannerOptions)

                    val resolutionSelector = androidx.camera.core.resolutionselector.ResolutionSelector.Builder()
                        .setResolutionStrategy(
                            androidx.camera.core.resolutionselector.ResolutionStrategy(
                                android.util.Size(1280, 720),
                                androidx.camera.core.resolutionselector.ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                            )
                        ).build()

                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .setResolutionSelector(resolutionSelector)
                        .build()

                    imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                        @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
                        val mediaImage = imageProxy.image
                        if (mediaImage != null && !isDetected) {
                            val image = InputImage.fromMediaImage(
                                mediaImage,
                                imageProxy.imageInfo.rotationDegrees
                            )

                            barcodeScanner.process(image)
                                .addOnSuccessListener { barcodes ->
                                    for (barcode in barcodes) {
                                        val rawValue = barcode.rawValue
                                        if (rawValue != null && rawValue.trim().isNotEmpty() && !isDetected) {
                                            isDetected = true
                                            val formatStr = mapMlKitFormatToString(barcode.format)
                                            android.util.Log.d("BarcodeScanner", "Scanned: $rawValue ($formatStr)")
                                            onBarcodeDetected(rawValue, formatStr)
                                            break
                                        }
                                    }
                                }
                                .addOnCompleteListener {
                                    imageProxy.close()
                                }
                        } else {
                            imageProxy.close()
                        }
                    }

                    try {
                        cameraProvider.unbindAll()
                        val camera = cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            imageAnalysis
                        )
                        
                        // Applica un leggero zoom per facilitare la scansione dei barcode 1D
                        val zoomState = camera.cameraInfo.zoomState.value
                        if (zoomState != null) {
                            val maxZoom = zoomState.maxZoomRatio
                            val targetZoom = 1.5f
                            if (maxZoom >= targetZoom) {
                                camera.cameraControl.setZoomRatio(targetZoom)
                            } else if (maxZoom > 1f) {
                                camera.cameraControl.setZoomRatio(maxZoom)
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }, androidx.core.content.ContextCompat.getMainExecutor(ctx))

                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        // Overlay per lo scanner
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            // Calcola il rettangolo target (proporzioni per barcode 1D)
            val rectWidth = canvasWidth * 0.85f
            val rectHeight = rectWidth * 0.4f
            val rectLeft = (canvasWidth - rectWidth) / 2f
            val rectTop = (canvasHeight - rectHeight) / 2f

            val targetRect = androidx.compose.ui.geometry.Rect(
                left = rectLeft,
                top = rectTop,
                right = rectLeft + rectWidth,
                bottom = rectTop + rectHeight
            )

            // Sfondo oscurato attorno al rettangolo
            val path = androidx.compose.ui.graphics.Path().apply {
                addRect(androidx.compose.ui.geometry.Rect(0f, 0f, canvasWidth, canvasHeight))
                addRoundRect(
                    androidx.compose.ui.geometry.RoundRect(
                        rect = targetRect,
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx(), 16.dp.toPx())
                    )
                )
                fillType = androidx.compose.ui.graphics.PathFillType.EvenOdd
            }
            drawPath(
                path = path,
                color = Color.Black.copy(alpha = 0.5f)
            )

            // Linea rossa laser animata fissa al centro
            drawLine(
                color = Color.Red,
                start = androidx.compose.ui.geometry.Offset(rectLeft + 16.dp.toPx(), canvasHeight / 2),
                end = androidx.compose.ui.geometry.Offset(rectLeft + rectWidth - 16.dp.toPx(), canvasHeight / 2),
                strokeWidth = 3.dp.toPx()
            )
            
            // Cornici agli angoli
            val cornerLength = 32.dp.toPx()
            val strokeWidth = 4.dp.toPx()
            val cornerColor = Color.White
            
            // Top-Left
            drawLine(cornerColor, androidx.compose.ui.geometry.Offset(rectLeft, rectTop), androidx.compose.ui.geometry.Offset(rectLeft + cornerLength, rectTop), strokeWidth)
            drawLine(cornerColor, androidx.compose.ui.geometry.Offset(rectLeft, rectTop), androidx.compose.ui.geometry.Offset(rectLeft, rectTop + cornerLength), strokeWidth)
            // Top-Right
            drawLine(cornerColor, androidx.compose.ui.geometry.Offset(rectLeft + rectWidth, rectTop), androidx.compose.ui.geometry.Offset(rectLeft + rectWidth - cornerLength, rectTop), strokeWidth)
            drawLine(cornerColor, androidx.compose.ui.geometry.Offset(rectLeft + rectWidth, rectTop), androidx.compose.ui.geometry.Offset(rectLeft + rectWidth, rectTop + cornerLength), strokeWidth)
            // Bottom-Left
            drawLine(cornerColor, androidx.compose.ui.geometry.Offset(rectLeft, rectTop + rectHeight), androidx.compose.ui.geometry.Offset(rectLeft + cornerLength, rectTop + rectHeight), strokeWidth)
            drawLine(cornerColor, androidx.compose.ui.geometry.Offset(rectLeft, rectTop + rectHeight), androidx.compose.ui.geometry.Offset(rectLeft, rectTop + rectHeight - cornerLength), strokeWidth)
            // Bottom-Right
            drawLine(cornerColor, androidx.compose.ui.geometry.Offset(rectLeft + rectWidth, rectTop + rectHeight), androidx.compose.ui.geometry.Offset(rectLeft + rectWidth - cornerLength, rectTop + rectHeight), strokeWidth)
            drawLine(cornerColor, androidx.compose.ui.geometry.Offset(rectLeft + rectWidth, rectTop + rectHeight), androidx.compose.ui.geometry.Offset(rectLeft + rectWidth, rectTop + rectHeight - cornerLength), strokeWidth)
        }
    }
}

private fun mapMlKitFormatToString(format: Int): String {
    return when (format) {
        Barcode.FORMAT_EAN_13 -> "EAN_13"
        Barcode.FORMAT_EAN_8 -> "EAN_8"
        Barcode.FORMAT_CODE_128 -> "CODE_128"
        Barcode.FORMAT_CODE_39 -> "CODE_39"
        Barcode.FORMAT_UPC_A -> "UPC_A"
        Barcode.FORMAT_UPC_E -> "UPC_E"
        Barcode.FORMAT_QR_CODE -> "QR_CODE"
        Barcode.FORMAT_ITF -> "ITF"
        Barcode.FORMAT_PDF417 -> "PDF_417"
        Barcode.FORMAT_CODABAR -> "CODABAR"
        Barcode.FORMAT_DATA_MATRIX -> "DATA_MATRIX"
        else -> "CODE_128"
    }
}
