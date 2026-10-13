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
            val plane = imageProxy.planes[0]
            val buffer = plane.buffer
            val data = ByteArray(buffer.remaining())
            buffer.get(data)

            val width = imageProxy.width
            val height = imageProxy.height
            val rotation = imageProxy.imageInfo.rotationDegrees

            val (rotatedData, finalWidth, finalHeight) = rotateYuv(data, width, height, rotation)

            // Crop al mirino centrale (circa 70% larghezza x 40% altezza)
            val cropWidth = (finalWidth * 0.70f).toInt().coerceIn(1, finalWidth)
            val cropHeight = (finalHeight * 0.40f).toInt().coerceIn(1, finalHeight)
            val cropLeft = ((finalWidth - cropWidth) / 2).coerceIn(0, finalWidth - cropWidth)
            val cropTop = ((finalHeight - cropHeight) / 2).coerceIn(0, finalHeight - cropHeight)

            val source = PlanarYUVLuminanceSource(
                rotatedData,
                finalWidth,
                finalHeight,
                cropLeft,
                cropTop,
                cropWidth,
                cropHeight,
                false
            )

            val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
            val result = reader.decodeWithState(binaryBitmap)

            var rawValue = result.text ?: ""
            var formatStr = mapFormatToString(result.barcodeFormat)

            if (result.barcodeFormat == BarcodeFormat.UPC_A && rawValue.length == 12) {
                rawValue = "0$rawValue"
                formatStr = "EAN_13"
            }

            if (rawValue.isNotBlank()) {
                onSuccess(rawValue, formatStr)
            }
        } catch (_: NotFoundException) {
            // Nessun codice rilevato in questo frame, continua
        } catch (e: Exception) {
            // Ignora errori di parsing temporanei sui singoli frame
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
                onError("Impossibile caricare l'immagine")
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
            onError("Nessun barcode rilevato nell'immagine")
        } catch (e: Exception) {
            onError("Errore durante l'analisi dell'immagine: ${e.message}")
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
