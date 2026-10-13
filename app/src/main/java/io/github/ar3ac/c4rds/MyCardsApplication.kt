package io.github.ar3ac.c4rds

import android.app.Application
import io.github.ar3ac.c4rds.data.local.AppDatabase
import io.github.ar3ac.c4rds.data.repository.CardRepository

class MyCardsApplication : Application() {

    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy { CardRepository(database.cardDao(), this) }
}
