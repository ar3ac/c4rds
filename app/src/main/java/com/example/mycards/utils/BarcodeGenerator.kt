package com.example.mycards.utils

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import java.util.EnumMap

object BarcodeGenerator {

    fun generateBarcode(
        value: String,
        formatName: String,
        width: Int = 1000,
        height: Int = 500
    ): Bitmap? {
        if (value.isBlank()) return null

        val zxingFormat = mapFormatNameToZxing(formatName) ?: BarcodeFormat.CODE_128

        val actualHeight = if (zxingFormat == BarcodeFormat.QR_CODE || zxingFormat == BarcodeFormat.DATA_MATRIX) {
            width
        } else {
            height
        }

        return try {
            val hints = EnumMap<EncodeHintType, Any>(EncodeHintType::class.java).apply {
                put(EncodeHintType.MARGIN, 2)
                put(EncodeHintType.CHARACTER_SET, "UTF-8")
            }

            val writer = MultiFormatWriter()
            val bitMatrix: BitMatrix = writer.encode(value, zxingFormat, width, actualHeight, hints)

            val matrixWidth = bitMatrix.width
            val matrixHeight = bitMatrix.height
            val pixels = IntArray(matrixWidth * matrixHeight)

            for (y in 0 until matrixHeight) {
                val offset = y * matrixWidth
                for (x in 0 until matrixWidth) {
                    pixels[offset + x] = if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE
                }
            }

            val bitmap = Bitmap.createBitmap(matrixWidth, matrixHeight, Bitmap.Config.ARGB_8888)
            bitmap.setPixels(pixels, 0, matrixWidth, 0, 0, matrixWidth, matrixHeight)
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun mapFormatNameToZxing(formatName: String): BarcodeFormat? {
        return try {
            when (formatName.uppercase()) {
                "EAN_13", "EAN13" -> BarcodeFormat.EAN_13
                "EAN_8", "EAN8" -> BarcodeFormat.EAN_8
                "CODE_128", "CODE128" -> BarcodeFormat.CODE_128
                "CODE_39", "CODE39" -> BarcodeFormat.CODE_39
                "UPC_A", "UPCA" -> BarcodeFormat.UPC_A
                "UPC_E", "UPCE" -> BarcodeFormat.UPC_E
                "QR_CODE", "QR", "QRCODE" -> BarcodeFormat.QR_CODE
                "ITF" -> BarcodeFormat.ITF
                "PDF_417", "PDF417" -> BarcodeFormat.PDF_417
                "CODABAR" -> BarcodeFormat.CODABAR
                "DATA_MATRIX" -> BarcodeFormat.DATA_MATRIX
                else -> try { BarcodeFormat.valueOf(formatName) } catch (e: Exception) { null }
            }
        } catch (e: Exception) {
            BarcodeFormat.CODE_128
        }
    }

    val supportedFormats = listOf(
        "EAN_13",
        "EAN_8",
        "CODE_128",
        "CODE_39",
        "UPC_A",
        "UPC_E",
        "QR_CODE",
        "ITF",
        "PDF_417",
        "CODABAR",
        "DATA_MATRIX"
    )
}
