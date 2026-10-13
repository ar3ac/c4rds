package io.github.ar3ac.c4rds.ui.add

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.ar3ac.c4rds.data.model.CardEntity
import io.github.ar3ac.c4rds.data.repository.CardRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File
import java.net.URL

data class LogoSearchResult(val name: String, val domain: String, val logoUrl: String)

class AddEditViewModel(
    private val repository: CardRepository,
    private val cardId: Long = -1L,
    initialValue: String = "",
    initialFormat: String = "",
    initialName: String = "",
    initialOwner: String = ""
) : ViewModel() {

    var name by mutableStateOf(if (initialName.isNotBlank()) initialName else "")
    var owner by mutableStateOf(if (initialOwner.isNotBlank()) initialOwner else "")
    var barcodeValue by mutableStateOf(initialValue)
    var barcodeFormat by mutableStateOf(
        if (initialFormat.isNotBlank()) initialFormat else "CODE_128"
    )
    var favorite by mutableStateOf(false)
    var customLogoPath by mutableStateOf<String?>(null)

    var isEditMode by mutableStateOf(false)
    var isSaved by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)

    // Per la ricerca web del logo
    var isSearchingLogo by mutableStateOf(false)
    var logoSearchResults by mutableStateOf<List<LogoSearchResult>>(emptyList())

    // Flag per tracciare se l'utente ha modificato manualmente il formato via menu a tendina
    private var isUserCustomFormat = initialFormat.isNotBlank()

    init {
        if (cardId > 0) {
            isEditMode = true
            viewModelScope.launch {
                val existing = repository.getCardById(cardId)
                if (existing != null) {
                    name = existing.name
                    owner = existing.owner
                    barcodeValue = existing.barcodeValue
                    barcodeFormat = existing.barcodeFormat
                    favorite = existing.favorite
                    customLogoPath = existing.customLogoPath
                    isUserCustomFormat = true
                }
            }
        }
    }

    fun onBarcodeValueChanged(newValue: String) {
        barcodeValue = newValue
        errorMessage = null

        // Auto-rilevamento intelligente: rimuovi qualsiasi tipo di spazio vuoto (interno, iniziale, finale)
        if (!isUserCustomFormat) {
            val cleanValue = newValue.replace("\\s+".toRegex(), "")
            barcodeFormat = when {
                cleanValue.length == 13 && cleanValue.all { it.isDigit() } -> "EAN_13"
                cleanValue.length == 8 && cleanValue.all { it.isDigit() } -> "EAN_8"
                cleanValue.length == 12 && cleanValue.all { it.isDigit() } -> "UPC_A"
                else -> "CODE_128"
            }
        }
    }

    fun onFormatSelected(format: String) {
        barcodeFormat = format
        isUserCustomFormat = true
    }
    
    fun searchWebLogos(query: String) {
        if (query.isBlank()) return
        isSearchingLogo = true
        viewModelScope.launch(Dispatchers.IO) {
            val results = mutableListOf<LogoSearchResult>()
            try {
                // 1. Seeklogo Search (High Quality)
                try {
                    val seeklogoUrl = "https://seeklogo.com/search?q=${java.net.URLEncoder.encode(query.trim(), "UTF-8")}"
                    val connection = URL(seeklogoUrl).openConnection() as java.net.HttpURLConnection
                    connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                    connection.connectTimeout = 5000
                    connection.readTimeout = 5000
                    val html = connection.inputStream.bufferedReader().use { it.readText() }
                    
                    val regex = "src=\"(https://images\\.seeklogo\\.com/[^\"]+)\"[^>]*alt=\"([^\"]+)\"".toRegex(RegexOption.IGNORE_CASE)
                    val matches = regex.findAll(html)
                    matches.take(8).forEach { match ->
                        val logoUrl = match.groupValues[1]
                        val nameStr = match.groupValues[2].replace(" Logo PNG Vector", "").replace(" Logo PNG", "")
                        // Avoid duplicates if any
                        if (results.none { it.logoUrl == logoUrl }) {
                            results.add(LogoSearchResult(nameStr, "Seeklogo", logoUrl))
                        }
                    }
                } catch (e: Exception) {
                    Log.e("AddEditViewModel", "Error fetching from seeklogo", e)
                }

                // 2. Clearbit Autocomplete -> Google Favicon (Fallback)
                try {
                    val url = "https://autocomplete.clearbit.com/v1/companies/suggest?query=${query.trim().replace(" ", "")}"
                    val jsonString = URL(url).readText()
                    val jsonArray = JSONArray(jsonString)
                    
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        val domain = obj.getString("domain")
                        val nameStr = obj.getString("name")
                        val logoUrl = "https://t2.gstatic.com/faviconV2?client=SOCIAL&type=FAVICON&fallback_opts=TYPE,SIZE,URL&url=http://$domain&size=128"
                        results.add(LogoSearchResult(nameStr, domain, logoUrl))
                    }
                } catch (e: Exception) {
                    Log.e("AddEditViewModel", "Error fetching from clearbit", e)
                }
                
                withContext(Dispatchers.Main) {
                    logoSearchResults = results
                    isSearchingLogo = false
                }
            } catch (e: Exception) {
                Log.e("AddEditViewModel", "Error searching logos", e)
                withContext(Dispatchers.Main) {
                    isSearchingLogo = false
                }
            }
        }
    }

    fun downloadAndSetCustomLogo(context: Context, logoUrl: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val connection = URL(logoUrl).openConnection() as java.net.HttpURLConnection
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                val bytes = connection.inputStream.readBytes()
                
                // Ritaglia la trasparenza in eccesso
                var bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                if (bitmap != null) {
                    bitmap = cropTransparentBorders(bitmap)
                    val filename = "custom_logo_${System.currentTimeMillis()}.png"
                    val logosDir = File(context.filesDir, "custom_logos")
                    if (!logosDir.exists()) logosDir.mkdirs()
                    
                    val file = File(logosDir, filename)
                    file.outputStream().use { out ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                    }
                    
                    withContext(Dispatchers.Main) {
                        customLogoPath = file.absolutePath
                    }
                }
            } catch (e: Exception) {
                Log.e("AddEditViewModel", "Error downloading custom logo", e)
            }
        }
    }

    private fun cropTransparentBorders(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        var minX = width
        var minY = height
        var maxX = -1
        var maxY = -1

        for (y in 0 until height) {
            for (x in 0 until width) {
                val alpha = (pixels[y * width + x] ushr 24) and 0xff
                if (alpha > 10) { // Tolleranza per pixel quasi trasparenti
                    if (x < minX) minX = x
                    if (x > maxX) maxX = x
                    if (y < minY) minY = y
                    if (y > maxY) maxY = y
                }
            }
        }

        if (maxX < minX || maxY < minY) {
            return bitmap // L'immagine è vuota o completamente trasparente
        }

        // Aggiungi un piccolissimo padding (es. 4 pixel) per non tagliare troppo a filo
        val padding = 4
        minX = maxOf(0, minX - padding)
        minY = maxOf(0, minY - padding)
        maxX = minOf(width - 1, maxX + padding)
        maxY = minOf(height - 1, maxY + padding)

        val newWidth = maxX - minX + 1
        val newHeight = maxY - minY + 1
        
        // Se il ritaglio è inutile (immagine già a filo), evitalo
        if (newWidth == width && newHeight == height) return bitmap

        return Bitmap.createBitmap(bitmap, minX, minY, newWidth, newHeight)
    }

    fun copyUriAndSetCustomLogo(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                if (inputStream != null) {
                    val bytes = inputStream.readBytes()
                    inputStream.close()
                    val filename = "custom_logo_${System.currentTimeMillis()}.png"
                    val logosDir = File(context.filesDir, "custom_logos")
                    if (!logosDir.exists()) logosDir.mkdirs()
                    
                    val file = File(logosDir, filename)
                    file.writeBytes(bytes)
                    
                    withContext(Dispatchers.Main) {
                        customLogoPath = file.absolutePath
                    }
                }
            } catch (e: Exception) {
                Log.e("AddEditViewModel", "Error copying custom logo from gallery", e)
            }
        }
    }

    fun saveCard(onSuccess: (Long) -> Unit) {
        if (name.isBlank()) {
            errorMessage = "Inserisci un nome per la tessera"
            return
        }
        if (barcodeValue.isBlank()) {
            errorMessage = "Inserisci il valore del codice a barre"
            return
        }

        // Pulisci il valore del barcode rimuovendo spazi inutili (specialmente per formati numerici)
        var finalValue = barcodeValue.trim()
        val numericFormats = listOf("EAN_13", "EAN_8", "UPC_A", "UPC_E", "ITF")
        if (barcodeFormat in numericFormats || finalValue.replace("\\s+".toRegex(), "").all { it.isDigit() }) {
            finalValue = finalValue.replace("\\s+".toRegex(), "")
        }

        val cleanedBarcodeValue = finalValue

        viewModelScope.launch {
            if (isEditMode) {
                val cardToUpdate = CardEntity(
                    id = cardId,
                    name = name.trim(),
                    owner = owner.trim(),
                    barcodeValue = cleanedBarcodeValue,
                    barcodeFormat = barcodeFormat,
                    favorite = favorite,
                    customLogoPath = customLogoPath
                )
                repository.updateCard(cardToUpdate)
                isSaved = true
                onSuccess(cardId)
            } else {
                val newCard = CardEntity(
                    name = name.trim(),
                    owner = owner.trim(),
                    barcodeValue = cleanedBarcodeValue,
                    barcodeFormat = barcodeFormat,
                    favorite = favorite,
                    customLogoPath = customLogoPath
                )
                val newId = repository.insertCard(newCard)
                isSaved = true
                onSuccess(newId)
            }
        }
    }
}

class AddEditViewModelFactory(
    private val repository: CardRepository,
    private val cardId: Long,
    private val initialValue: String,
    private val initialFormat: String,
    private val initialName: String,
    private val initialOwner: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AddEditViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AddEditViewModel(repository, cardId, initialValue, initialFormat, initialName, initialOwner) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
