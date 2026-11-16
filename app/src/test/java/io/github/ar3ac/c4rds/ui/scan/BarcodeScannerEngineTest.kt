package io.github.ar3ac.c4rds.ui.scan

import com.google.zxing.*
import com.google.zxing.common.BitMatrix
import org.junit.Assert.*
import org.junit.Test
import java.util.EnumMap

class BarcodeScannerEngineTest {

    private val engine = BarcodeScannerEngine()

    private fun generateLuminanceSource(
        value: String,
        format: BarcodeFormat,
        width: Int,
        height: Int,
        margin: Int = 10,
        inverted: Boolean = false
    ): PlanarYUVLuminanceSource {
        val hints = EnumMap<EncodeHintType, Any>(EncodeHintType::class.java).apply {
            put(EncodeHintType.MARGIN, margin)
            put(EncodeHintType.CHARACTER_SET, "UTF-8")
        }
        val matrix = MultiFormatWriter().encode(value, format, width, height, hints)
        val data = ByteArray(width * height)
        for (y in 0 until height) {
            for (x in 0 until width) {
                val isBlack = matrix.get(x, y)
                val pixelVal = if (inverted) {
                    if (isBlack) 255.toByte() else 0.toByte()
                } else {
                    if (isBlack) 0.toByte() else 255.toByte()
                }
                data[y * width + x] = pixelVal
            }
        }
        return PlanarYUVLuminanceSource(data, width, height, 0, 0, width, height, false)
    }

    @Test
    fun testDecodeEan13() {
        val code = "8001234567897"
        val source = generateLuminanceSource(code, BarcodeFormat.EAN_13, 600, 300)
        val result = engine.decodeLuminanceSource(source)
        assertNotNull("EAN-13 should decode", result)
        assertEquals(code, result?.text)
        assertEquals(BarcodeFormat.EAN_13, result?.barcodeFormat)
    }

    @Test
    fun testDecodeEan8() {
        val code = "12345670"
        val source = generateLuminanceSource(code, BarcodeFormat.EAN_8, 400, 200)
        val result = engine.decodeLuminanceSource(source)
        assertNotNull("EAN-8 should decode", result)
        assertEquals(code, result?.text)
        assertEquals(BarcodeFormat.EAN_8, result?.barcodeFormat)
    }

    @Test
    fun testDecodeCode128() {
        val code = "CARD-1234-ABCD"
        val source = generateLuminanceSource(code, BarcodeFormat.CODE_128, 600, 200)
        val result = engine.decodeLuminanceSource(source)
        assertNotNull("CODE_128 should decode", result)
        assertEquals(code, result?.text)
        assertEquals(BarcodeFormat.CODE_128, result?.barcodeFormat)
    }

    @Test
    fun testDecodeCode39() {
        val code = "MYCARD99"
        val source = generateLuminanceSource(code, BarcodeFormat.CODE_39, 700, 200)
        val result = engine.decodeLuminanceSource(source)
        assertNotNull("CODE_39 should decode", result)
        assertEquals(code, result?.text)
        assertEquals(BarcodeFormat.CODE_39, result?.barcodeFormat)
    }

    @Test
    fun testDecodeQrCode() {
        val text = "https://github.com/ar3ac/c4rds"
        val source = generateLuminanceSource(text, BarcodeFormat.QR_CODE, 400, 400, margin = 2)
        val result = engine.decodeLuminanceSource(source)
        assertNotNull("QR Code should decode", result)
        assertEquals(text, result?.text)
        assertEquals(BarcodeFormat.QR_CODE, result?.barcodeFormat)
    }

    @Test
    fun testDecodeInvertedBarcode() {
        val code = "8001234567897"
        val source = generateLuminanceSource(code, BarcodeFormat.EAN_13, 600, 300, inverted = true)
        val result = engine.decodeLuminanceSource(source)
        assertNotNull("Inverted barcode should decode with invert fallback", result)
        assertEquals(code, result?.text)
    }
}
