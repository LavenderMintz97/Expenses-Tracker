package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import androidx.room.Room
import com.example.data.ExpenseDatabase
import com.example.data.ExpenseRepository
import com.example.ui.AppContent
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private lateinit var database: ExpenseDatabase
    private lateinit var repository: ExpenseRepository
    private lateinit var viewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Room Database and Repository
        database = Room.databaseBuilder(
            applicationContext,
            ExpenseDatabase::class.java,
            "expense_tracker_db"
        ).fallbackToDestructiveMigration().build()

        repository = ExpenseRepository(database.expenseDao())

        // Create MainViewModel
        val factory = MainViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[MainViewModel::class.java]

        // Handle initial voice intent extra if launched from widget
        if (intent?.getBooleanExtra("EXTRA_START_VOICE", false) == true) {
            viewModel.setVoiceInputSheetVisible(true)
        }

        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                AppContent(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra("EXTRA_START_VOICE", false)) {
            viewModel.setVoiceInputSheetVisible(true)
        }
    }
}
