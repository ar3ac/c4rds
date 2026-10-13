package io.github.ar3ac.c4rds.data.backup

import android.content.Context
import android.net.Uri
import io.github.ar3ac.c4rds.data.model.CardEntity
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter

data class BackupCardDto(
    @SerializedName("name") val name: String,
    @SerializedName("barcodeValue") val barcodeValue: String,
    @SerializedName("barcodeFormat") val barcodeFormat: String,
    @SerializedName("favorite") val favorite: Boolean = false,
    @SerializedName("owner") val owner: String? = "",
    @SerializedName("customLogoBase64") val customLogoBase64: String? = null
)

data class BackupDataDto(
    @SerializedName("version") val version: Int = 1,
    @SerializedName("cards") val cards: List<BackupCardDto>
)

object BackupManager {

    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    suspend fun exportToJson(context: Context, uri: Uri, cards: List<CardEntity>): Result<Unit> {
        return runCatching {
            val dtos = cards.map { card ->
                val base64Logo = card.customLogoPath?.let { path ->
                    val file = java.io.File(path)
                    if (file.exists()) {
                        android.util.Base64.encodeToString(file.readBytes(), android.util.Base64.NO_WRAP)
                    } else null
                }

                BackupCardDto(
                    name = card.name,
                    barcodeValue = card.barcodeValue,
                    barcodeFormat = card.barcodeFormat,
                    favorite = card.favorite,
                    owner = card.owner,
                    customLogoBase64 = base64Logo
                )
            }
            val backupData = BackupDataDto(version = 1, cards = dtos)
            val jsonString = gson.toJson(backupData)

            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                OutputStreamWriter(outputStream, Charsets.UTF_8).use { writer ->
                    writer.write(jsonString)
                }
            } ?: throw IllegalStateException("Impossibile aprire lo stream di scrittura per l'URI fornito.")
        }
    }

    suspend fun importFromJson(context: Context, uri: Uri): Result<List<CardEntity>> {
        return runCatching {
            val jsonString = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
                    reader.readText()
                }
            } ?: throw IllegalStateException("Impossibile leggere il file dal percorso selezionato.")

            val backupData = gson.fromJson(jsonString, BackupDataDto::class.java)
                ?: throw IllegalArgumentException("File JSON non valido o vuoto.")

            if (backupData.version > 1) {
                throw IllegalArgumentException("Versione backup (${backupData.version}) non supportata. Aggiorna l'app C4rds.")
            }

            backupData.cards.map { dto ->
                var customLogoPath: String? = null
                if (!dto.customLogoBase64.isNullOrBlank()) {
                    try {
                        val bytes = android.util.Base64.decode(dto.customLogoBase64, android.util.Base64.NO_WRAP)
                        val filename = "custom_logo_${System.currentTimeMillis()}_${java.util.UUID.randomUUID().toString().take(5)}.png"
                        val logosDir = java.io.File(context.filesDir, "custom_logos")
                        if (!logosDir.exists()) logosDir.mkdirs()
                        val file = java.io.File(logosDir, filename)
                        file.writeBytes(bytes)
                        customLogoPath = file.absolutePath
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                CardEntity(
                    name = dto.name.ifBlank { "Senza Nome" },
                    barcodeValue = dto.barcodeValue,
                    barcodeFormat = dto.barcodeFormat,
                    favorite = dto.favorite,
                    owner = dto.owner ?: "",
                    customLogoPath = customLogoPath
                )
            }
        }
    }

    fun shareDatabase(context: Context, cards: List<CardEntity>) {
        try {
            val dtos = cards.map { card ->
                val base64Logo = card.customLogoPath?.let { path ->
                    val file = java.io.File(path)
                    if (file.exists()) {
                        android.util.Base64.encodeToString(file.readBytes(), android.util.Base64.NO_WRAP)
                    } else null
                }

                BackupCardDto(
                    name = card.name,
                    barcodeValue = card.barcodeValue,
                    barcodeFormat = card.barcodeFormat,
                    favorite = card.favorite,
                    owner = card.owner,
                    customLogoBase64 = base64Logo
                )
            }
            val backupData = BackupDataDto(version = 1, cards = dtos)
            val jsonString = gson.toJson(backupData)

            val exportDir = java.io.File(context.cacheDir, "exports")
            if (!exportDir.exists()) {
                exportDir.mkdirs()
            }

            val exportFile = java.io.File(exportDir, "portafoglio_famiglia.c4rds")
            exportFile.writeText(jsonString, Charsets.UTF_8)

            val uri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                exportFile
            )

            val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = "*/*"
                putExtra(android.content.Intent.EXTRA_STREAM, uri)
                putExtra(android.content.Intent.EXTRA_SUBJECT, "Tessere C4rds")
                putExtra(android.content.Intent.EXTRA_TEXT, "Ecco tutte le mie tessere! Apri questo file con l'app C4rds per importarle.")
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(android.content.Intent.createChooser(shareIntent, "Condividi Portafoglio"))
        } catch (e: Exception) {
            e.printStackTrace()
            android.widget.Toast.makeText(context, "Errore durante la condivisione", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
}
