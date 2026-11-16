package io.github.ar3ac.c4rds.ui.scan

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.camera.core.ImageProxy
import com.google.zxing.*
import com.google.zxing.common.GlobalHistogramBinarizer
import com.google.zxing.common.HybridBinarizer

class BarcodeScannerEngine {

    companion object {
        private const val TAG = "BarcodeScannerEngine"
    }

    // Fast reader for real-time video frames (30 FPS, no TRY_HARDER to prevent frame drops)
    private val videoReader = MultiFormatReader().apply {
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
                BarcodeFormat.DATA_MATRIX
            )
        )
        setHints(hints)
    }

    // Thorough reader for static gallery images (uses TRY_HARDER)
    private val stillReader = MultiFormatReader().apply {
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

    // Preallocated byte buffers reused across frames to prevent GC pauses
    private var yuvBuffer: ByteArray? = null
    private var rotatedBuffer: ByteArray? = null

    @Synchronized
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

        var detectedResult: Result? = null

        try {
            val yPlane = imageProxy.planes[0]
            val yBuffer = yPlane.buffer
            val rowStride = yPlane.rowStride
            val width = imageProxy.width
            val height = imageProxy.height
            val rotation = imageProxy.imageInfo.rotationDegrees

            // Always rewind buffer to ensure correct position
            yBuffer.rewind()

            val requiredSize = width * height
            var data = yuvBuffer
            if (data == null || data.size != requiredSize) {
                data = ByteArray(requiredSize)
                yuvBuffer = data
            }

            // Copy Y-plane bytes taking rowStride padding into account
            if (rowStride == width) {
                yBuffer.get(data, 0, requiredSize)
            } else {
                var offset = 0
                for (row in 0 until height) {
                    yBuffer.position(row * rowStride)
                    yBuffer.get(data, offset, width)
                    offset += width
                }
            }

            var finalData = data
            var finalWidth = width
            var finalHeight = height

            if (rotation != 0) {
                var rotated = rotatedBuffer
                if (rotated == null || rotated.size != requiredSize) {
                    rotated = ByteArray(requiredSize)
                    rotatedBuffer = rotated
                }
                rotateYuv(data, rotated, width, height, rotation)
                finalData = rotated
                if (rotation == 90 || rotation == 270) {
                    finalWidth = height
                    finalHeight = width
                }
            }

            // 1. FAST PASS: Viewfinder central region (100% width, center 60% height in display orientation)
            // Scanning 60% height takes only ~20ms, ensuring silky smooth 30 FPS analysis
            val cropHeight = (finalHeight * 0.60f).toInt().coerceIn(1, finalHeight)
            val cropTop = ((finalHeight - cropHeight) / 2).coerceIn(0, finalHeight - cropHeight)
            val croppedSource = PlanarYUVLuminanceSource(
                finalData,
                finalWidth,
                finalHeight,
                0,
                cropTop,
                finalWidth,
                cropHeight,
                false
            )
            detectedResult = decodeLuminance(videoReader, croppedSource)

            // 2. FALLBACK PASS: Full-frame in display orientation (if barcode is outside central 60%)
            if (detectedResult == null) {
                val fullSource = PlanarYUVLuminanceSource(
                    finalData,
                    finalWidth,
                    finalHeight,
                    0,
                    0,
                    finalWidth,
                    finalHeight,
                    false
                )
                detectedResult = decodeLuminance(videoReader, fullSource)
            }

            // 3. ORTHOGONAL PASS: Unrotated sensor frame (90° offset detects vertically held barcodes)
            if (detectedResult == null && rotation != 0) {
                val sensorSource = PlanarYUVLuminanceSource(
                    data,
                    width,
                    height,
                    0,
                    0,
                    width,
                    height,
                    false
                )
                detectedResult = decodeLuminance(videoReader, sensorSource)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Frame analysis exception: ${e.message}")
        } finally {
            onComplete()
        }

        // Deliver result outside internal frame decoding block
        if (detectedResult != null) {
            var rawValue = detectedResult.text ?: ""
            var formatStr = mapFormatToString(detectedResult.barcodeFormat)

            // Fix UPC-A to EAN-13 (leading 0)
            if (detectedResult.barcodeFormat == BarcodeFormat.UPC_A && rawValue.length == 12) {
                rawValue = "0$rawValue"
                formatStr = "EAN_13"
            }

            if (rawValue.isNotBlank()) {
                Log.d(TAG, "Barcode successfully detected: $rawValue ($formatStr)")
                onSuccess(rawValue, formatStr)
            }
        }
    }

    internal fun decodeLuminanceSource(source: LuminanceSource): Result? {
        return decodeLuminance(videoReader, source)
    }

    internal fun decodeLuminance(reader: MultiFormatReader, source: LuminanceSource): Result? {
        // 1. GlobalHistogramBinarizer: Primary and fastest binarizer for 1D retail barcodes
        try {
            reader.reset()
            return reader.decodeWithState(BinaryBitmap(GlobalHistogramBinarizer(source)))
        } catch (_: ReaderException) {
        } catch (_: Exception) {}

        // 2. HybridBinarizer: Optimized for 2D matrix codes (QR Code, DataMatrix)
        try {
            reader.reset()
            return reader.decodeWithState(BinaryBitmap(HybridBinarizer(source)))
        } catch (_: ReaderException) {
        } catch (_: Exception) {}

        // 3. Inverted GlobalHistogramBinarizer: Light-on-dark / inverted barcodes (dark mode cards)
        try {
            reader.reset()
            return reader.decodeWithState(BinaryBitmap(GlobalHistogramBinarizer(source.invert())))
        } catch (_: ReaderException) {
        } catch (_: Exception) {}

        return null
    }

    @Synchronized
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
            var result = decodeLuminance(stillReader, source)

            // If not found, try rotated 90 degrees
            if (result == null) {
                val rotatedPixels = IntArray(width * height)
                for (y in 0 until height) {
                    for (x in 0 until width) {
                        rotatedPixels[x * height + (height - y - 1)] = pixels[x + y * width]
                    }
                }
                val rotatedSource = RGBLuminanceSource(height, width, rotatedPixels)
                result = decodeLuminance(stillReader, rotatedSource)
            }

            if (result != null) {
                var rawValue = result.text ?: ""
                var formatStr = mapFormatToString(result.barcodeFormat)

                if (result.barcodeFormat == BarcodeFormat.UPC_A && rawValue.length == 12) {
                    rawValue = "0$rawValue"
                    formatStr = "EAN_13"
                }

                onSuccess(rawValue, formatStr)
            } else {
                onError(context.getString(io.github.ar3ac.c4rds.R.string.scan_no_barcode_found))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error scanning URI", e)
            onError(context.getString(io.github.ar3ac.c4rds.R.string.scan_no_barcode_found))
        } finally {
            stillReader.reset()
        }
    }

    private fun rotateYuv(
        src: ByteArray,
        dst: ByteArray,
        width: Int,
        height: Int,
        rotation: Int
    ) {
        when (rotation) {
            90 -> {
                for (y in 0 until height) {
                    for (x in 0 until width) {
                        dst[x * height + (height - y - 1)] = src[x + y * width]
                    }
                }
            }
            180 -> {
                val lastIdx = src.size - 1
                for (i in src.indices) {
                    dst[lastIdx - i] = src[i]
                }
            }
            270 -> {
                for (y in 0 until height) {
                    for (x in 0 until width) {
                        dst[(width - x - 1) * height + y] = src[x + y * width]
                    }
                }
            }
            else -> {
                System.arraycopy(src, 0, dst, 0, src.size)
            }
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
