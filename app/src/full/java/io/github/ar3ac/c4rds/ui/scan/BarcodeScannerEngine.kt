package io.github.ar3ac.c4rds.ui.scan

import android.content.Context
import android.net.Uri
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

class BarcodeScannerEngine {
    private val barcodeScannerOptions = BarcodeScannerOptions.Builder()
        .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
        .build()

    private val barcodeScanner = BarcodeScanning.getClient(barcodeScannerOptions)

    fun scanImageProxy(
        imageProxy: ImageProxy,
        isAlreadyDetected: Boolean,
        onSuccess: (value: String, format: String) -> Unit,
        onComplete: () -> Unit
    ) {
        @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
        val mediaImage = imageProxy.image
        if (mediaImage != null && !isAlreadyDetected) {
            val image = InputImage.fromMediaImage(
                mediaImage,
                imageProxy.imageInfo.rotationDegrees
            )

            barcodeScanner.process(image)
                .addOnSuccessListener { barcodes ->
                    for (barcode in barcodes) {
                        val rawValue = barcode.rawValue

                        var isInsideTarget = true
                        val boundingBox = barcode.boundingBox
                        if (boundingBox != null) {
                            val rotation = imageProxy.imageInfo.rotationDegrees
                            val isPortrait = rotation == 90 || rotation == 270
                            val actualWidth = if (isPortrait) image.height else image.width
                            val actualHeight = if (isPortrait) image.width else image.height

                            val centerX = boundingBox.centerX()
                            val centerY = boundingBox.centerY()

                            isInsideTarget = (centerX > actualWidth * 0.3f && centerX < actualWidth * 0.7f) &&
                                    (centerY > actualHeight * 0.35f && centerY < actualHeight * 0.65f)
                        }

                        if (rawValue != null && rawValue.trim().isNotEmpty() && isInsideTarget) {
                            var formatStr = mapFormatToString(barcode.format)
                            var finalRawValue = rawValue
                            if (barcode.format == Barcode.FORMAT_UPC_A && finalRawValue.length == 12) {
                                finalRawValue = "0$finalRawValue"
                                formatStr = "EAN_13"
                            }
                            onSuccess(finalRawValue, formatStr)
                            break
                        }
                    }
                }
                .addOnCompleteListener {
                    onComplete()
                }
        } else {
            onComplete()
        }
    }

    fun scanUri(
        context: Context,
        uri: Uri,
        onSuccess: (value: String, format: String) -> Unit,
        onError: (message: String) -> Unit
    ) {
        try {
            val inputImage = InputImage.fromFilePath(context, uri)
            barcodeScanner.process(inputImage)
                .addOnSuccessListener { barcodes ->
                    if (barcodes.isNotEmpty()) {
                        val barcode = barcodes.first()
                        var rawValue = barcode.rawValue ?: ""
                        var formatStr = mapFormatToString(barcode.format)

                        if (barcode.format == Barcode.FORMAT_UPC_A && rawValue.length == 12) {
                            rawValue = "0$rawValue"
                            formatStr = "EAN_13"
                        }

                        onSuccess(rawValue, formatStr)
                    } else {
                        onError("Nessun barcode rilevato nell'immagine")
                    }
                }
                .addOnFailureListener {
                    onError("Errore durante l'analisi dell'immagine")
                }
        } catch (e: Exception) {
            onError("Errore durante l'apertura dell'immagine: ${e.message}")
        }
    }

    private fun mapFormatToString(format: Int): String {
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
}
