package com.example.mycards.ui.add

import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.mycards.data.model.CardEntity
import com.example.mycards.data.repository.CardRepository
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
            try {
                val url = "https://autocomplete.clearbit.com/v1/companies/suggest?query=${query.trim().replace(" ", "")}"
                val jsonString = URL(url).readText()
                val jsonArray = JSONArray(jsonString)
                val results = mutableListOf<LogoSearchResult>()
                
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val domain = obj.getString("domain")
                    val nameStr = obj.getString("name")
                    val logoUrl = "https://t2.gstatic.com/faviconV2?client=SOCIAL&type=FAVICON&fallback_opts=TYPE,SIZE,URL&url=http://$domain&size=128"
                    results.add(LogoSearchResult(nameStr, domain, logoUrl))
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
                val bytes = URL(logoUrl).readBytes()
                val filename = "custom_logo_${System.currentTimeMillis()}.png"
                val logosDir = File(context.filesDir, "custom_logos")
                if (!logosDir.exists()) logosDir.mkdirs()
                
                val file = File(logosDir, filename)
                file.writeBytes(bytes)
                
                withContext(Dispatchers.Main) {
                    customLogoPath = file.absolutePath
                }
            } catch (e: Exception) {
                Log.e("AddEditViewModel", "Error downloading custom logo", e)
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
