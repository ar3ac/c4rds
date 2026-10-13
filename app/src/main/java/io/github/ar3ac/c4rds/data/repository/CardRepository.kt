package io.github.ar3ac.c4rds.data.repository

import android.content.Context
import io.github.ar3ac.c4rds.data.local.CardDao
import io.github.ar3ac.c4rds.data.model.CardEntity
import io.github.ar3ac.c4rds.utils.ShortcutHelper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach

class CardRepository(
    private val cardDao: CardDao,
    private val context: Context
) {

    val allCards: Flow<List<CardEntity>> = cardDao.getAllCards().onEach { list ->
        ShortcutHelper.updateDynamicShortcuts(context, list)
    }

    val favoriteCards: Flow<List<CardEntity>> = cardDao.getFavoriteCards()

    suspend fun getFavoritesCount(): Int {
        return cardDao.getFavoritesCount()
    }

    suspend fun getCardById(id: Long): CardEntity? {
        return cardDao.getCardById(id)
    }

    fun getCardByIdFlow(id: Long): Flow<CardEntity?> {
        return cardDao.getCardByIdFlow(id)
    }

    suspend fun insertCard(card: CardEntity): Long {
        return cardDao.insertCard(card)
    }

    suspend fun updateCard(card: CardEntity) {
        cardDao.updateCard(card)
    }

    suspend fun updateCardsOrder(cards: List<CardEntity>) {
        cardDao.updateCards(cards)
    }

    suspend fun deleteCard(card: CardEntity) {
        cardDao.deleteCard(card)
    }

    suspend fun deleteAllCards() {
        cardDao.deleteAllCards()
    }

    suspend fun insertAll(cards: List<CardEntity>) {
        cardDao.insertAll(cards)
    }
}
