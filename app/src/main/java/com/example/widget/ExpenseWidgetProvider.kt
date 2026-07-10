package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.room.Room
import com.example.MainActivity
import com.example.R
import com.example.data.ExpenseDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ExpenseWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // Build database instance
        val db = Room.databaseBuilder(
            context.applicationContext,
            ExpenseDatabase::class.java,
            "expense_tracker_db"
        ).fallbackToDestructiveMigration().build()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Fetch all transactions to compute balance and monthly spending
                val transactions = db.expenseDao().getAllTransactions().first()
                
                var balance = 5420.0 // Default starting mock balance
                var monthlySpend = 0.0

                // Sum items based on type
                // INCOME -> +amount
                // EXPENSE -> -amount
                // INVESTMENT -> -amount
                // FAMILY_SHARING -> -amount
                for (tx in transactions) {
                    when (tx.type) {
                        "INCOME" -> balance += tx.amount
                        "EXPENSE" -> {
                            balance -= tx.amount
                            monthlySpend += tx.amount
                        }
                        "INVESTMENT" -> balance -= tx.amount
                        "FAMILY_SHARING" -> {
                            balance -= tx.amount
                            monthlySpend += tx.amount
                        }
                    }
                }

                val formattedBalance = "$${String.format("%,.2f", balance)}"
                val formattedSpend = "Spending: $${String.format("%,.2f", monthlySpend)}"

                for (appWidgetId in appWidgetIds) {
                    val views = RemoteViews(context.packageName, R.layout.expense_widget)
                    
                    views.setTextViewText(R.id.widget_balance, formattedBalance)
                    views.setTextViewText(R.id.widget_monthly_spend, formattedSpend)

                    // Intent to launch MainActivity with voice deep link enabled
                    val clickIntent = Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        putExtra("EXTRA_START_VOICE", true)
                    }
                    
                    val pendingIntent = PendingIntent.getActivity(
                        context,
                        appWidgetId,
                        clickIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )

                    // Apply click action to the voice button AND the widget background for high-response feel
                    views.setOnClickPendingIntent(R.id.btn_widget_speak, pendingIntent)
                    views.setOnClickPendingIntent(R.id.widget_title, pendingIntent)
                    views.setOnClickPendingIntent(R.id.widget_balance, pendingIntent)

                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                db.close()
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        // Handle custom updates or actions
        if (intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisWidget = ComponentName(context, ExpenseWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
            onUpdate(context, appWidgetManager, appWidgetIds)
        }
    }
}
