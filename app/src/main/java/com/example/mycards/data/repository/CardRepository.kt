package com.example.mycards.data.repository

import android.content.Context
import com.example.mycards.data.local.CardDao
import com.example.mycards.data.model.CardEntity
import com.example.mycards.utils.ShortcutHelper
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
        val id = cardDao.insertCard(card)
        syncShortcuts()
        return id
    }

    suspend fun updateCard(card: CardEntity) {
        cardDao.updateCard(card)
        syncShortcuts()
    }

    suspend fun updateCardsOrder(cards: List<CardEntity>) {
        cardDao.updateCards(cards)
        syncShortcuts()
    }

    suspend fun deleteCard(card: CardEntity) {
        cardDao.deleteCard(card)
        syncShortcuts()
    }

    suspend fun deleteAllCards() {
        cardDao.deleteAllCards()
        syncShortcuts()
    }

    suspend fun insertAll(cards: List<CardEntity>) {
        cardDao.insertAll(cards)
        syncShortcuts()
    }

    private suspend fun syncShortcuts() {
        val topCards = cardDao.getTopCards(10)
        ShortcutHelper.updateDynamicShortcuts(context, topCards)
    }
}
