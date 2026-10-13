package io.github.ar3ac.c4rds.ui.scan

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.camera.core.ImageProxy
import com.google.zxing.*
import com.google.zxing.common.HybridBinarizer

class BarcodeScannerEngine {
    private val reader = MultiFormatReader().apply {
        val hints = mapOf<DecodeHintType, Any>(
            DecodeHintType.POSSIBLE_FORMATS to listOf(
                BarcodeFormat.EAN_13,
                BarcodeFormat.EAN_8,
                BarcodeFormat.CODE_128,
                BarcodeFormat.CODE_39,
                BarcodeFormat.UPC_A,
                BarcodeFormat.UPC_E,
                BarcodeFormat.QR_CODE,
                BarcodeFormat.ITF,
                BarcodeFormat.PDF_417,
                BarcodeFormat.CODABAR,
                BarcodeFormat.DATA_MATRIX
            ),
            DecodeHintType.TRY_HARDER to java.lang.Boolean.TRUE
        )
        setHints(hints)
    }

    fun scanImageProxy(
        imageProxy: ImageProxy,
        isAlreadyDetected: Boolean,
        onSuccess: (value: String, format: String) -> Unit,
        onComplete: () -> Unit
    ) {
        if (isAlreadyDetected) {
            onComplete()
            return
        }

        try {
            val yPlane = imageProxy.planes[0]
            val yBuffer = yPlane.buffer
            val rowStride = yPlane.rowStride
            val width = imageProxy.width
            val height = imageProxy.height
            val rotation = imageProxy.imageInfo.rotationDegrees

            // Estrae i byte Y considerando l'eventuale padding del rowStride
            val data = ByteArray(width * height)
            if (rowStride == width) {
                yBuffer.get(data)
            } else {
                for (row in 0 until height) {
                    yBuffer.position(row * rowStride)
                    yBuffer.get(data, row * width, width)
                }
            }

            val (rotatedData, finalWidth, finalHeight) = rotateYuv(data, width, height, rotation)

            // 1. Prova prima con il crop mirato centrale (75% larghezza x 40% altezza)
            val cropWidth = (finalWidth * 0.75f).toInt().coerceIn(1, finalWidth)
            val cropHeight = (finalHeight * 0.40f).toInt().coerceIn(1, finalHeight)
            val cropLeft = ((finalWidth - cropWidth) / 2).coerceIn(0, finalWidth - cropWidth)
            val cropTop = ((finalHeight - cropHeight) / 2).coerceIn(0, finalHeight - cropHeight)

            val croppedSource = PlanarYUVLuminanceSource(
                rotatedData,
                finalWidth,
                finalHeight,
                cropLeft,
                cropTop,
                cropWidth,
                cropHeight,
                false
            )

            var result: Result? = null
            try {
                result = reader.decodeWithState(BinaryBitmap(HybridBinarizer(croppedSource)))
            } catch (_: NotFoundException) {
                // Se non trovato nel box centrale ristretto, fallback sull'intero frame ruotato
                try {
                    reader.reset()
                    val fullSource = PlanarYUVLuminanceSource(
                        rotatedData,
                        finalWidth,
                        finalHeight,
                        0,
                        0,
                        finalWidth,
                        finalHeight,
                        false
                    )
                    result = reader.decodeWithState(BinaryBitmap(HybridBinarizer(fullSource)))
                } catch (_: NotFoundException) {
                    // Nessun barcode rilevato in questo frame
                }
            }

            if (result != null) {
                var rawValue = result.text ?: ""
                var formatStr = mapFormatToString(result.barcodeFormat)

                // Fix UPC-A -> EAN-13
                if (result.barcodeFormat == BarcodeFormat.UPC_A && rawValue.length == 12) {
                    rawValue = "0$rawValue"
                    formatStr = "EAN_13"
                }

                if (rawValue.isNotBlank()) {
                    onSuccess(rawValue, formatStr)
                }
            }
        } catch (e: Exception) {
            // Ignora eccezioni temporanee di parsing frame
        } finally {
            reader.reset()
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
            val inputStream = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (bitmap == null) {
                onError(context.getString(io.github.ar3ac.c4rds.R.string.scan_no_barcode_found))
                return
            }

            val width = bitmap.width
            val height = bitmap.height
            val pixels = IntArray(width * height)
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

            val source = RGBLuminanceSource(width, height, pixels)
            val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
            val result = reader.decodeWithState(binaryBitmap)

            var rawValue = result.text ?: ""
            var formatStr = mapFormatToString(result.barcodeFormat)

            if (result.barcodeFormat == BarcodeFormat.UPC_A && rawValue.length == 12) {
                rawValue = "0$rawValue"
                formatStr = "EAN_13"
            }

            onSuccess(rawValue, formatStr)
        } catch (_: NotFoundException) {
            onError(context.getString(io.github.ar3ac.c4rds.R.string.scan_no_barcode_found))
        } catch (e: Exception) {
            onError(context.getString(io.github.ar3ac.c4rds.R.string.scan_no_barcode_found))
        } finally {
            reader.reset()
        }
    }

    private fun rotateYuv(
        data: ByteArray,
        width: Int,
        height: Int,
        rotation: Int
    ): Triple<ByteArray, Int, Int> {
        return when (rotation) {
            90 -> {
                val rotated = ByteArray(data.size)
                for (y in 0 until height) {
                    for (x in 0 until width) {
                        rotated[x * height + (height - y - 1)] = data[x + y * width]
                    }
                }
                Triple(rotated, height, width)
            }
            180 -> {
                val rotated = ByteArray(data.size)
                for (i in data.indices) {
                    rotated[data.size - 1 - i] = data[i]
                }
                Triple(rotated, width, height)
            }
            270 -> {
                val rotated = ByteArray(data.size)
                for (y in 0 until height) {
                    for (x in 0 until width) {
                        rotated[(width - x - 1) * height + y] = data[x + y * width]
                    }
                }
                Triple(rotated, height, width)
            }
            else -> Triple(data, width, height)
        }
    }

    private fun mapFormatToString(format: BarcodeFormat): String {
        return when (format) {
            BarcodeFormat.EAN_13 -> "EAN_13"
            BarcodeFormat.EAN_8 -> "EAN_8"
            BarcodeFormat.CODE_128 -> "CODE_128"
            BarcodeFormat.CODE_39 -> "CODE_39"
            BarcodeFormat.UPC_A -> "UPC_A"
            BarcodeFormat.UPC_E -> "UPC_E"
            BarcodeFormat.QR_CODE -> "QR_CODE"
            BarcodeFormat.ITF -> "ITF"
            BarcodeFormat.PDF_417 -> "PDF_417"
            BarcodeFormat.CODABAR -> "CODABAR"
            BarcodeFormat.DATA_MATRIX -> "DATA_MATRIX"
            else -> "CODE_128"
        }
    }
}

