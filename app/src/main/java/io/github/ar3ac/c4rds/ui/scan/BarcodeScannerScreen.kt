package io.github.ar3ac.c4rds.ui.scan

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import io.github.ar3ac.c4rds.R
import kotlinx.coroutines.delay
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarcodeScannerScreen(
    onBarcodeDetected: (value: String, format: String) -> Unit,
    onManualInputClick: () -> Unit,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val scannerEngine = remember { BarcodeScannerEngine() }
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

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            scannerEngine.scanUri(
                context = context,
                uri = uri,
                onSuccess = { rawValue, formatStr ->
                    onBarcodeDetected(rawValue, formatStr)
                },
                onError = { msg ->
                    android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.scan_barcode_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = { galleryLauncher.launch("image/*") }) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = stringResource(R.string.scan_from_gallery))
                    }
                    TextButton(onClick = onManualInputClick) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.manual_entry))
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
                    scannerEngine = scannerEngine,
                    onBarcodeDetected = onBarcodeDetected
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
                        text = stringResource(R.string.camera_permission_required),
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.camera_permission_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }
                    ) {
                        Text(stringResource(R.string.grant_permission))
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(onClick = onManualInputClick) {
                        Text(stringResource(R.string.manual_entry))
                    }
                }
            }
        }
    }
}

@SuppressLint("ClickableViewAccessibility")
@Composable
fun CameraPreviewWithAnalyzer(
    scannerEngine: BarcodeScannerEngine,
    onBarcodeDetected: (value: String, format: String) -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    var isDetected by remember { mutableStateOf(false) }
    var activeCamera by remember { mutableStateOf<Camera?>(null) }
    var hasFlashUnit by remember { mutableStateOf(false) }
    var isTorchOn by remember { mutableStateOf(false) }
    var currentZoom by remember { mutableFloatStateOf(1.0f) }
    var maxZoomRatio by remember { mutableFloatStateOf(1.0f) }
    var focusTarget by remember { mutableStateOf<Offset?>(null) }

    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    // Laser scan line vertical animation
    val infiniteTransition = rememberInfiniteTransition(label = "scanner_laser")
    val laserProgress by infiniteTransition.animateFloat(
        initialValue = 0.05f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_y"
    )

    // Clear focus ring after 1.2s
    LaunchedEffect(focusTarget) {
        if (focusTarget != null) {
            delay(1200)
            focusTarget = null
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                if (isTorchOn) {
                    activeCamera?.cameraControl?.enableTorch(false)
                }
            } catch (_: Exception) {}
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

                val scaleDetector = ScaleGestureDetector(ctx, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                    override fun onScale(detector: ScaleGestureDetector): Boolean {
                        activeCamera?.let { cam ->
                            val current = cam.cameraInfo.zoomState.value?.zoomRatio ?: 1.0f
                            val max = cam.cameraInfo.zoomState.value?.maxZoomRatio ?: 3.0f
                            val target = (current * detector.scaleFactor).coerceIn(1.0f, max)
                            cam.cameraControl.setZoomRatio(target)
                        }
                        return true
                    }
                })

                val gestureDetector = GestureDetector(ctx, object : GestureDetector.SimpleOnGestureListener() {
                    override fun onSingleTapUp(e: MotionEvent): Boolean {
                        activeCamera?.let { cam ->
                            val factory = previewView.meteringPointFactory
                            val point = factory.createPoint(e.x, e.y)
                            val action = FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE)
                                .setAutoCancelDuration(3, TimeUnit.SECONDS)
                                .build()
                            cam.cameraControl.startFocusAndMetering(action)
                            focusTarget = Offset(e.x, e.y)
                        }
                        return true
                    }
                })

                previewView.setOnTouchListener { _, event ->
                    scaleDetector.onTouchEvent(event)
                    gestureDetector.onTouchEvent(event)
                    true
                }

                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()

                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

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
                        scannerEngine.scanImageProxy(
                            imageProxy = imageProxy,
                            isAlreadyDetected = isDetected,
                            onSuccess = { rawValue, formatStr ->
                                if (!isDetected) {
                                    isDetected = true
                                    onBarcodeDetected(rawValue, formatStr)
                                }
                            },
                            onComplete = {
                                imageProxy.close()
                            }
                        )
                    }

                    try {
                        cameraProvider.unbindAll()
                        val camera = cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            imageAnalysis
                        )
                        activeCamera = camera
                        hasFlashUnit = camera.cameraInfo.hasFlashUnit()

                        camera.cameraInfo.zoomState.observe(lifecycleOwner) { zoomState ->
                            if (zoomState != null) {
                                currentZoom = zoomState.zoomRatio
                                maxZoomRatio = zoomState.maxZoomRatio
                            }
                        }

                        // Trigger auto-focus and auto-exposure metering on center initially
                        previewView.post {
                            try {
                                val centerX = previewView.width / 2f
                                val centerY = previewView.height / 2f
                                val factory = previewView.meteringPointFactory
                                val point = factory.createPoint(centerX, centerY)
                                val action = FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE)
                                    .setAutoCancelDuration(4, TimeUnit.SECONDS)
                                    .build()
                                camera.cameraControl.startFocusAndMetering(action)
                            } catch (_: Exception) {}
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }, ContextCompat.getMainExecutor(ctx))

                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        // Viewfinder overlay & laser
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            val rectWidth = canvasWidth * 0.88f
            val rectHeight = (rectWidth * 0.55f).coerceAtMost(canvasHeight * 0.45f)
            val rectLeft = (canvasWidth - rectWidth) / 2f
            val rectTop = (canvasHeight - rectHeight) / 2f

            val targetRect = Rect(
                left = rectLeft,
                top = rectTop,
                right = rectLeft + rectWidth,
                bottom = rectTop + rectHeight
            )

            // Dark semi-transparent scrim around viewfinder box
            val scrimPath = Path().apply {
                addRect(Rect(0f, 0f, canvasWidth, canvasHeight))
                addRoundRect(
                    RoundRect(
                        rect = targetRect,
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx(), 16.dp.toPx())
                    )
                )
                fillType = PathFillType.EvenOdd
            }
            drawPath(
                path = scrimPath,
                color = Color.Black.copy(alpha = 0.55f)
            )

            // Animated horizontal laser scanning line
            val laserY = rectTop + rectHeight * laserProgress
            drawLine(
                color = Color.Red.copy(alpha = 0.85f),
                start = Offset(rectLeft + 16.dp.toPx(), laserY),
                end = Offset(rectLeft + rectWidth - 16.dp.toPx(), laserY),
                strokeWidth = 2.5.dp.toPx()
            )

            // Viewfinder corner brackets
            val cornerLength = 28.dp.toPx()
            val strokeWidth = 3.5.dp.toPx()
            val cornerColor = Color.White

            // Top-Left
            drawLine(cornerColor, Offset(rectLeft, rectTop), Offset(rectLeft + cornerLength, rectTop), strokeWidth)
            drawLine(cornerColor, Offset(rectLeft, rectTop), Offset(rectLeft, rectTop + cornerLength), strokeWidth)
            // Top-Right
            drawLine(cornerColor, Offset(rectLeft + rectWidth, rectTop), Offset(rectLeft + rectWidth - cornerLength, rectTop), strokeWidth)
            drawLine(cornerColor, Offset(rectLeft + rectWidth, rectTop), Offset(rectLeft + rectWidth, rectTop + cornerLength), strokeWidth)
            // Bottom-Left
            drawLine(cornerColor, Offset(rectLeft, rectTop + rectHeight), Offset(rectLeft + cornerLength, rectTop + rectHeight), strokeWidth)
            drawLine(cornerColor, Offset(rectLeft, rectTop + rectHeight), Offset(rectLeft, rectTop + rectHeight - cornerLength), strokeWidth)
            // Bottom-Right
            drawLine(cornerColor, Offset(rectLeft + rectWidth, rectTop + rectHeight), Offset(rectLeft + rectWidth - cornerLength, rectTop + rectHeight), strokeWidth)
            drawLine(cornerColor, Offset(rectLeft + rectWidth, rectTop + rectHeight), Offset(rectLeft + rectWidth, rectTop + rectHeight - cornerLength), strokeWidth)

            // Tap-to-focus indicator ring
            focusTarget?.let { target ->
                drawCircle(
                    color = Color.White.copy(alpha = 0.8f),
                    radius = 32.dp.toPx(),
                    center = target,
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }

        // Floating controls (Flashlight toggle and Zoom toggle)
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 96.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Flashlight button (if device has flash)
            if (hasFlashUnit) {
                IconButton(
                    onClick = {
                        activeCamera?.let { cam ->
                            val nextTorch = !isTorchOn
                            try {
                                cam.cameraControl.enableTorch(nextTorch)
                                isTorchOn = nextTorch
                            } catch (_: Exception) {}
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            if (isTorchOn) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.6f),
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        imageVector = if (isTorchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                        contentDescription = stringResource(R.string.scan_torch),
                        tint = if (isTorchOn) MaterialTheme.colorScheme.onPrimary else Color.White
                    )
                }
            }

            // Zoom 1x / 2x toggle button
            Button(
                onClick = {
                    activeCamera?.let { cam ->
                        val targetZoom = if (currentZoom > 1.4f) 1.0f else 2.0f.coerceAtMost(maxZoomRatio)
                        try {
                            cam.cameraControl.setZoomRatio(targetZoom)
                        } catch (_: Exception) {}
                    }
                },
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Black.copy(alpha = 0.6f),
                    contentColor = Color.White
                ),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                modifier = Modifier.height(48.dp)
            ) {
                Text(
                    text = if (currentZoom > 1.4f) "2.0x" else "1.0x",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }

        // Viewfinder hint text
        Text(
            text = stringResource(R.string.scan_viewfinder_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 36.dp)
                .background(
                    Color.Black.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}
