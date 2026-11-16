package io.github.ar3ac.c4rds.ui.scan

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.camera.core.ImageProxy
import com.google.zxing.*
import com.google.zxing.common.GlobalHistogramBinarizer
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

    // Preallocated byte buffers reused across frames to prevent GC pressure and frame drops
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

        try {
            val yPlane = imageProxy.planes[0]
            val yBuffer = yPlane.buffer
            val rowStride = yPlane.rowStride
            val width = imageProxy.width
            val height = imageProxy.height
            val rotation = imageProxy.imageInfo.rotationDegrees

            val requiredSize = width * height
            var data = yuvBuffer
            if (data == null || data.size != requiredSize) {
                data = ByteArray(requiredSize)
                yuvBuffer = data
            }

            // Copy Y-plane bytes efficiently considering rowStride padding
            if (rowStride == width) {
                yBuffer.get(data, 0, requiredSize)
            } else {
                for (row in 0 until height) {
                    yBuffer.position(row * rowStride)
                    yBuffer.get(data, row * width, width)
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

            // Primary source: full-frame display orientation (preserves quiet zones & margins)
            val primarySource = PlanarYUVLuminanceSource(
                finalData,
                finalWidth,
                finalHeight,
                0,
                0,
                finalWidth,
                finalHeight,
                false
            )

            var result = decodeLuminanceSource(primarySource)

            // Secondary source: if not detected and rotation != 0, test unrotated raw sensor frame.
            // Raw sensor frame is orthogonal (90° offset), so it instantly detects barcodes oriented vertically!
            if (result == null && rotation != 0) {
                val secondarySource = PlanarYUVLuminanceSource(
                    data,
                    width,
                    height,
                    0,
                    0,
                    width,
                    height,
                    false
                )
                result = decodeLuminanceSource(secondarySource)
            }

            if (result != null) {
                var rawValue = result.text ?: ""
                var formatStr = mapFormatToString(result.barcodeFormat)

                // Fix UPC-A -> EAN-13 (leading 0)
                if (result.barcodeFormat == BarcodeFormat.UPC_A && rawValue.length == 12) {
                    rawValue = "0$rawValue"
                    formatStr = "EAN_13"
                }

                if (rawValue.isNotBlank()) {
                    onSuccess(rawValue, formatStr)
                }
            }
        } catch (_: Exception) {
            // Ignore transient decoding exceptions per frame
        } finally {
            onComplete()
        }
    }

    internal fun decodeLuminanceSource(source: LuminanceSource): Result? {
        // 1. GlobalHistogramBinarizer: By far the fastest & most reliable for 1D barcodes under real lighting
        try {
            reader.reset()
            return reader.decodeWithState(BinaryBitmap(GlobalHistogramBinarizer(source)))
        } catch (_: NotFoundException) {}

        // 2. HybridBinarizer: Optimized for 2D barcodes (QR, DataMatrix, PDF417)
        try {
            reader.reset()
            return reader.decodeWithState(BinaryBitmap(HybridBinarizer(source)))
        } catch (_: NotFoundException) {}

        // 3. Inverted GlobalHistogramBinarizer: Light-on-dark / inverted barcodes (dark mode cards)
        try {
            reader.reset()
            return reader.decodeWithState(BinaryBitmap(GlobalHistogramBinarizer(source.invert())))
        } catch (_: NotFoundException) {}

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
            var result = decodeLuminanceSource(source)

            // If not found in original orientation, try rotated 90 degrees
            if (result == null) {
                val rotatedPixels = IntArray(width * height)
                for (y in 0 until height) {
                    for (x in 0 until width) {
                        rotatedPixels[x * height + (height - y - 1)] = pixels[x + y * width]
                    }
                }
                val rotatedSource = RGBLuminanceSource(height, width, rotatedPixels)
                result = decodeLuminanceSource(rotatedSource)
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
        } catch (_: NotFoundException) {
            onError(context.getString(io.github.ar3ac.c4rds.R.string.scan_no_barcode_found))
        } catch (e: Exception) {
            onError(context.getString(io.github.ar3ac.c4rds.R.string.scan_no_barcode_found))
        } finally {
            reader.reset()
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
