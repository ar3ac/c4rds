package io.github.ar3ac.c4rds.ui.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.ar3ac.c4rds.data.model.CardEntity
import io.github.ar3ac.c4rds.data.repository.CardRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(
    private val repository: CardRepository
) : ViewModel() {

    var searchQuery by mutableStateOf("")

    val cards: StateFlow<List<CardEntity>> = repository.allCards
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun toggleFavorite(card: CardEntity, onLimitReached: () -> Unit) {
        viewModelScope.launch {
            if (!card.favorite) {
                val currentFavorites = cards.value.count { it.favorite }
                if (currentFavorites >= 3) {
                    onLimitReached()
                    return@launch
                }
            }
            repository.updateCard(card.copy(favorite = !card.favorite))
        }
    }

    fun deleteCard(card: CardEntity) {
        viewModelScope.launch {
            repository.deleteCard(card)
        }
    }

    fun updateCardsOrder(cards: List<CardEntity>) {
        viewModelScope.launch {
            val updatedCards = cards.mapIndexed { index, card ->
                card.copy(displayOrder = index)
            }
            repository.updateCardsOrder(updatedCards)
        }
    }
}

class HomeViewModelFactory(
    private val repository: CardRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
