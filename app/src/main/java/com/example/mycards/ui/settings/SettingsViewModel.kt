package com.example.mycards.ui.settings

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.mycards.data.backup.BackupManager
import com.example.mycards.data.model.CardEntity
import com.example.mycards.data.repository.CardRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val repository: CardRepository
) : ViewModel() {

    var statusMessage by mutableStateOf<String?>(null)
    var isError by mutableStateOf(false)

    var pendingImportCards by mutableStateOf<List<CardEntity>?>(null)
    var showImportDialog by mutableStateOf(false)

    fun exportBackup(context: Context, uri: Uri) {
        viewModelScope.launch {
            val allCards = repository.allCards.first()
            val result = BackupManager.exportToJson(context, uri, allCards)
            result.onSuccess {
                statusMessage = "Backup esportato con successo!"
                isError = false
            }.onFailure { ex ->
                statusMessage = "Errore durante l'esportazione: ${ex.localizedMessage}"
                isError = true
            }
        }
    }

    fun shareDatabase(context: Context) {
        viewModelScope.launch {
            val allCards = repository.allCards.first()
            BackupManager.shareDatabase(context, allCards)
        }
    }

    fun prepareImport(context: Context, uri: Uri) {
        viewModelScope.launch {
            val result = BackupManager.importFromJson(context, uri)
            result.onSuccess { cards ->
                if (cards.isEmpty()) {
                    statusMessage = "Il file selezionato non contiene tessere."
                    isError = true
                } else {
                    pendingImportCards = cards
                    showImportDialog = true
                }
            }.onFailure { ex ->
                statusMessage = "Errore importazione: ${ex.localizedMessage}"
                isError = true
            }
        }
    }

    fun confirmImportReplace(context: Context) {
        val cards = pendingImportCards ?: return
        viewModelScope.launch {
            com.example.mycards.utils.ShortcutHelper.clearAllShortcuts(context)
            repository.deleteAllCards()
            repository.insertAll(cards)
            statusMessage = "Ripristinate ${cards.size} tessere (database sostituito)."
            isError = false
            pendingImportCards = null
            showImportDialog = false
        }
    }

    fun confirmImportMerge() {
        val cards = pendingImportCards ?: return
        viewModelScope.launch {
            repository.insertAll(cards)
            statusMessage = "Aggiunte ${cards.size} tessere al database esistente."
            isError = false
            pendingImportCards = null
            showImportDialog = false
        }
    }
}

class SettingsViewModelFactory(
    private val repository: CardRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
