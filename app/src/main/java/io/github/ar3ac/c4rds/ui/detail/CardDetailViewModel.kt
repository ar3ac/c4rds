package io.github.ar3ac.c4rds.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.ar3ac.c4rds.data.model.CardEntity
import io.github.ar3ac.c4rds.data.repository.CardRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CardDetailViewModel(
    private val repository: CardRepository,
    private val cardId: Long
) : ViewModel() {

    val card: StateFlow<CardEntity?> = repository.getCardByIdFlow(cardId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    fun toggleFavorite(onLimitReached: () -> Unit) {
        val currentCard = card.value ?: return
        viewModelScope.launch {
            if (!currentCard.favorite) {
                val currentFavorites = repository.getFavoritesCount()
                if (currentFavorites >= 3) {
                    onLimitReached()
                    return@launch
                }
            }
            repository.updateCard(currentCard.copy(favorite = !currentCard.favorite))
        }
    }

    fun deleteCard(onDeleted: () -> Unit) {
        val currentCard = card.value ?: return
        viewModelScope.launch {
            repository.deleteCard(currentCard)
            onDeleted()
        }
    }
}

class CardDetailViewModelFactory(
    private val repository: CardRepository,
    private val cardId: Long
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CardDetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CardDetailViewModel(repository, cardId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
