package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.KiteDatabase
import com.example.data.KiteRepository
import com.example.ui.KiteApp
import com.example.ui.theme.KiteTheme
import com.example.ui.viewmodel.KiteViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = KiteDatabase.getDatabase(applicationContext)
        val repository = KiteRepository(database.kiteDao())

        setContent {
            val kiteViewModel: KiteViewModel = viewModel(
                factory = KiteViewModel.Factory(repository)
            )
            val themeMode by kiteViewModel.currentThemeMode.collectAsStateWithLifecycle()

            KiteTheme(themeMode = themeMode) {
                KiteApp(viewModel = kiteViewModel)
            }
        }
    }
}
