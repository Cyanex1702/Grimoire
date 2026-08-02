package com.cyanbudget.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.cyanbudget.app.MainActivity
import com.cyanbudget.app.R
import com.cyanbudget.app.data.FinanceDatabase
import com.cyanbudget.app.model.FinanceInsights
import com.cyanbudget.app.model.asMoney
import kotlin.math.roundToInt

class BudgetWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { update(context, manager, it) }
    }

    companion object {
        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, BudgetWidgetProvider::class.java)
            manager.getAppWidgetIds(component).forEach { update(context, manager, it) }
        }

        private fun update(context: Context, manager: AppWidgetManager, id: Int) {
            val summary = FinanceInsights.summary(FinanceDatabase.get(context).transactions())
            val preferences = context.getSharedPreferences("widget", Context.MODE_PRIVATE)
            val budget = preferences.getLong("budget", 300_000L)
            val privacy = preferences.getBoolean("privacy", false)
            val currency = preferences.getString("currency", "USD") ?: "USD"
            val views = RemoteViews(context.packageName, R.layout.widget_budget)
            views.setTextViewText(R.id.widget_balance, summary.balanceCents.asMoney(currency, privacy))
            views.setTextViewText(R.id.widget_spending, "Spent ${summary.monthExpenseCents.asMoney(currency, privacy)} this month")
            views.setProgressBar(R.id.widget_progress, 100, ((summary.monthExpenseCents.toFloat() / budget) * 100).roundToInt().coerceIn(0, 100), false)
            views.setOnClickPendingIntent(R.id.widget_add, activityIntent(context, "add"))
            views.setOnClickPendingIntent(R.id.widget_voice, activityIntent(context, "voice"))
            views.setOnClickPendingIntent(R.id.widget_balance, activityIntent(context, "home"))
            manager.updateAppWidget(id, views)
        }

        private fun activityIntent(context: Context, destination: String): PendingIntent {
            val intent = Intent(context, MainActivity::class.java).apply {
                putExtra("destination", destination)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            return PendingIntent.getActivity(context, destination.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }
    }
}
