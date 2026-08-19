package com.example.mycards

import android.app.Application
import com.example.mycards.data.local.AppDatabase
import com.example.mycards.data.repository.CardRepository

class MyCardsApplication : Application() {

    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy { CardRepository(database.cardDao(), this) }
}
