package io.github.ar3ac.c4rds.ui.settings

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.ar3ac.c4rds.data.backup.BackupManager
import io.github.ar3ac.c4rds.data.model.CardEntity
import io.github.ar3ac.c4rds.data.repository.CardRepository
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
                statusMessage = context.getString(io.github.ar3ac.c4rds.R.string.backup_export_success)
                isError = false
            }.onFailure { ex ->
                statusMessage = context.getString(io.github.ar3ac.c4rds.R.string.backup_export_error, ex.localizedMessage ?: "")
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
                    statusMessage = context.getString(io.github.ar3ac.c4rds.R.string.backup_empty_file)
                    isError = true
                } else {
                    pendingImportCards = cards
                    showImportDialog = true
                }
            }.onFailure { ex ->
                statusMessage = context.getString(io.github.ar3ac.c4rds.R.string.backup_import_error, ex.localizedMessage ?: "")
                isError = true
            }
        }
    }

    fun confirmImportReplace(context: Context) {
        val cards = pendingImportCards ?: return
        viewModelScope.launch {
            io.github.ar3ac.c4rds.utils.ShortcutHelper.clearAllShortcuts(context)
            repository.deleteAllCards()
            repository.insertAll(cards)
            statusMessage = context.getString(io.github.ar3ac.c4rds.R.string.backup_restore_replace_success, cards.size)
            isError = false
            pendingImportCards = null
            showImportDialog = false
        }
    }

    fun confirmImportMerge(context: Context) {
        val cards = pendingImportCards ?: return
        viewModelScope.launch {
            repository.insertAll(cards)
            statusMessage = context.getString(io.github.ar3ac.c4rds.R.string.backup_restore_merge_success, cards.size)
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
