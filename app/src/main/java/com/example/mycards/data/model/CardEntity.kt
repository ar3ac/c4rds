package com.example.mycards.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cards")
data class CardEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val barcodeValue: String,
    val barcodeFormat: String,
    val favorite: Boolean = false,
    @ColumnInfo(defaultValue = "0")
    val displayOrder: Int = 0,
    @ColumnInfo(defaultValue = "")
    val owner: String = ""
)
