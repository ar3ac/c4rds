package com.example.mycards.data.backup

import android.content.Context
import android.net.Uri
import com.example.mycards.data.model.CardEntity
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
    @SerializedName("owner") val owner: String? = ""
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
                BackupCardDto(
                    name = card.name,
                    barcodeValue = card.barcodeValue,
                    barcodeFormat = card.barcodeFormat,
                    favorite = card.favorite,
                    owner = card.owner
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
                throw IllegalArgumentException("Versione backup (${backupData.version}) non supportata. Aggiorna l'app c4rds.")
            }

            backupData.cards.map { dto ->
                CardEntity(
                    name = dto.name.ifBlank { "Senza Nome" },
                    barcodeValue = dto.barcodeValue,
                    barcodeFormat = dto.barcodeFormat,
                    favorite = dto.favorite,
                    owner = dto.owner ?: ""
                )
            }
        }
    }

    fun shareDatabase(context: Context, cards: List<CardEntity>) {
        try {
            val dtos = cards.map { card ->
                BackupCardDto(
                    name = card.name,
                    barcodeValue = card.barcodeValue,
                    barcodeFormat = card.barcodeFormat,
                    favorite = card.favorite,
                    owner = card.owner
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
                putExtra(android.content.Intent.EXTRA_SUBJECT, "Tessere c4rds")
                putExtra(android.content.Intent.EXTRA_TEXT, "Ecco tutte le mie tessere! Apri questo file con l'app c4rds per importarle.")
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(android.content.Intent.createChooser(shareIntent, "Condividi Portafoglio"))
        } catch (e: Exception) {
            e.printStackTrace()
            android.widget.Toast.makeText(context, "Errore durante la condivisione", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
}
