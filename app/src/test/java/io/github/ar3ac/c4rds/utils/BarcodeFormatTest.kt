package io.github.ar3ac.c4rds.utils

import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import org.junit.Assert.*
import org.junit.Test

class BarcodeFormatTest {

    @Test
    fun testMapFormatNameToZxing() {
        assertEquals(BarcodeFormat.EAN_13, BarcodeGenerator.mapFormatNameToZxing("EAN_13"))
        assertEquals(BarcodeFormat.EAN_13, BarcodeGenerator.mapFormatNameToZxing("ean_13"))
        assertEquals(BarcodeFormat.EAN_13, BarcodeGenerator.mapFormatNameToZxing("EAN13"))
        assertEquals(BarcodeFormat.CODE_128, BarcodeGenerator.mapFormatNameToZxing("CODE_128"))
        assertEquals(BarcodeFormat.QR_CODE, BarcodeGenerator.mapFormatNameToZxing("QR_CODE"))
        assertEquals(BarcodeFormat.QR_CODE, BarcodeGenerator.mapFormatNameToZxing("QR"))
        assertEquals(BarcodeFormat.UPC_A, BarcodeGenerator.mapFormatNameToZxing("UPC_A"))
        assertEquals(BarcodeFormat.DATA_MATRIX, BarcodeGenerator.mapFormatNameToZxing("DATA_MATRIX"))
    }

    @Test
    fun testSupportedFormatsList() {
        val list = BarcodeGenerator.supportedFormats
        assertTrue(list.contains("EAN_13"))
        assertTrue(list.contains("CODE_128"))
        assertTrue(list.contains("QR_CODE"))
        assertTrue(list.contains("UPC_A"))
        assertTrue(list.contains("DATA_MATRIX"))
        assertEquals(11, list.size)
    }

    @Test
    fun testZXingMatrixGenerationPureJvm() {
        val writer = MultiFormatWriter()
        // Test Code 128 bit matrix generation
        val matrix128 = writer.encode("123456789", BarcodeFormat.CODE_128, 200, 100)
        assertNotNull(matrix128)
        assertEquals(200, matrix128.width)
        assertEquals(100, matrix128.height)

        // Test QR Code bit matrix generation
        val matrixQr = writer.encode("https://c4rds.app", BarcodeFormat.QR_CODE, 100, 100)
        assertNotNull(matrixQr)
        assertEquals(100, matrixQr.width)
        assertEquals(100, matrixQr.height)
    }

    @Test
    fun testUpcAToEan13NormalizationRule() {
        val upcA12Digits = "012345678905"
        val normalizedEan13 = if (upcA12Digits.length == 12) "0$upcA12Digits" else upcA12Digits
        assertEquals(13, normalizedEan13.length)
        assertTrue(normalizedEan13.startsWith("0"))
    }

    @Test
    fun testCode128QuietZoneMargin() {
        val writer = MultiFormatWriter()
        val hints = java.util.EnumMap<com.google.zxing.EncodeHintType, Any>(com.google.zxing.EncodeHintType::class.java).apply {
            put(com.google.zxing.EncodeHintType.MARGIN, 10)
        }
        // Test with Esselunga style Code 128 barcode
        val matrix = writer.encode("0401234567890", BarcodeFormat.CODE_128, 400, 100, hints)
        assertNotNull(matrix)
        // With quiet zone margin, the first and last columns must be white
        assertFalse("First pixel column should be white (quiet zone)", matrix.get(0, 50))
        assertFalse("Last pixel column should be white (quiet zone)", matrix.get(matrix.width - 1, 50))
    }
}

