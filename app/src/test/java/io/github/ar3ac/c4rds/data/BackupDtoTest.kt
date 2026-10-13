package io.github.ar3ac.c4rds.data

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import io.github.ar3ac.c4rds.data.backup.BackupCardDto
import io.github.ar3ac.c4rds.data.backup.BackupDataDto
import org.junit.Assert.*
import org.junit.Test

class BackupDtoTest {

    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    @Test
    fun testSerializationAndDeserialization() {
        val originalCards = listOf(
            BackupCardDto(
                name = "Esselunga Fidaty",
                barcodeValue = "0400012345678",
                barcodeFormat = "CODE_128",
                favorite = true,
                owner = "Family",
                customLogoBase64 = null
            ),
            BackupCardDto(
                name = "Coop Card",
                barcodeValue = "2000000123456",
                barcodeFormat = "EAN_13",
                favorite = false,
                owner = "",
                customLogoBase64 = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg=="
            )
        )
        val backupData = BackupDataDto(version = 1, cards = originalCards)

        val jsonString = gson.toJson(backupData)
        assertNotNull(jsonString)
        assertTrue(jsonString.contains("Esselunga Fidaty"))
        assertTrue(jsonString.contains("0400012345678"))

        val deserialized = gson.fromJson(jsonString, BackupDataDto::class.java)
        assertNotNull(deserialized)
        assertEquals(1, deserialized.version)
        assertEquals(2, deserialized.cards.size)

        val firstCard = deserialized.cards[0]
        assertEquals("Esselunga Fidaty", firstCard.name)
        assertEquals("0400012345678", firstCard.barcodeValue)
        assertEquals("CODE_128", firstCard.barcodeFormat)
        assertTrue(firstCard.favorite)
        assertEquals("Family", firstCard.owner)
        assertNull(firstCard.customLogoBase64)

        val secondCard = deserialized.cards[1]
        assertEquals("Coop Card", secondCard.name)
        assertEquals("2000000123456", secondCard.barcodeValue)
        assertEquals("EAN_13", secondCard.barcodeFormat)
        assertFalse(secondCard.favorite)
        assertNotNull(secondCard.customLogoBase64)
    }

    @Test
    fun testLegacyJsonWithoutOwnerOrLogo() {
        val legacyJson = """
            {
                "version": 1,
                "cards": [
                    {
                        "name": "Conad Insieme",
                        "barcodeValue": "123456789012",
                        "barcodeFormat": "EAN_13",
                        "favorite": false
                    }
                ]
            }
        """.trimIndent()

        val parsed = gson.fromJson(legacyJson, BackupDataDto::class.java)
        assertNotNull(parsed)
        assertEquals(1, parsed.cards.size)
        val card = parsed.cards.first()
        assertEquals("Conad Insieme", card.name)
        assertEquals("123456789012", card.barcodeValue)
        assertEquals("EAN_13", card.barcodeFormat)
        assertNull(card.customLogoBase64)
    }
}

