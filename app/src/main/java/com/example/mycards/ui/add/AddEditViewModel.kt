package com.example.mycards.ui.add

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.mycards.data.model.CardEntity
import com.example.mycards.data.repository.CardRepository
import kotlinx.coroutines.launch

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

    var isEditMode by mutableStateOf(false)
    var isSaved by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)

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
                    favorite = favorite
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
                    favorite = favorite
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
