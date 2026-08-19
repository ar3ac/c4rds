package com.example.mycards.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.mycards.data.model.CardEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CardDao {

    @Query("SELECT * FROM cards ORDER BY displayOrder ASC, favorite DESC, name ASC")
    fun getAllCards(): Flow<List<CardEntity>>

    @Update
    suspend fun updateCards(cards: List<CardEntity>)

    @Query("SELECT * FROM cards WHERE favorite = 1 ORDER BY name ASC")
    fun getFavoriteCards(): Flow<List<CardEntity>>

    @Query("SELECT COUNT(*) FROM cards WHERE favorite = 1")
    suspend fun getFavoritesCount(): Int

    @Query("SELECT * FROM cards WHERE id = :id LIMIT 1")
    suspend fun getCardById(id: Long): CardEntity?

    @Query("SELECT * FROM cards WHERE id = :id LIMIT 1")
    fun getCardByIdFlow(id: Long): Flow<CardEntity?>

    @Query("SELECT * FROM cards WHERE favorite = 1 ORDER BY name ASC LIMIT :limit")
    suspend fun getTopFavorites(limit: Int): List<CardEntity>

    @Query("SELECT * FROM cards ORDER BY favorite DESC, name ASC LIMIT :limit")
    suspend fun getTopCards(limit: Int): List<CardEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCard(card: CardEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(cards: List<CardEntity>)

    @Update
    suspend fun updateCard(card: CardEntity)

    @Delete
    suspend fun deleteCard(card: CardEntity)

    @Query("DELETE FROM cards")
    suspend fun deleteAllCards()
}
