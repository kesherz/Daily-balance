package com.example.dailycandle

import android.app.Application
import androidx.room.Room
import com.example.dailycandle.data.BalanceDatabase
import com.example.dailycandle.data.BalanceRepository
import com.example.dailycandle.data.LegacySource
import com.example.dailycandle.data.UserPreferences
import com.example.dailycandle.domain.LegacySnapshot

class BalanceApplication : Application() {
    private val database by lazy {
        Room.databaseBuilder(this, BalanceDatabase::class.java, "daily_balance.db").build()
    }
    val repository by lazy {
        BalanceRepository(database, LegacySource {
            // Never clear or rewrite the original preferences, including malformed slots.
            val old = getSharedPreferences("data", MODE_PRIVATE).all
            LegacySnapshot(old["values"] as? String ?: "", old["dates"] as? String ?: "")
        })
    }
    val preferences by lazy { UserPreferences(this) }
}
