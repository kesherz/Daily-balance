package com.example.dailycandle

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.dailycandle.data.DataTransfer
import com.example.dailycandle.ui.BalanceApp
import com.example.dailycandle.ui.BalanceViewModel

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as BalanceApplication
        val factory = viewModelFactory {
            initializer {
                BalanceViewModel(app.repository, app.preferences, DataTransfer(contentResolver, app.repository), createSavedStateHandle())
            }
        }
        val model = ViewModelProvider(this, factory)[BalanceViewModel::class.java]
        setContent { BalanceApp(model) }
    }
}
