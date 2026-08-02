package com.cyanbudget.app.work

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.cyanbudget.app.R
import com.cyanbudget.app.data.FinanceDatabase
import com.cyanbudget.app.model.FinanceInsights
import com.cyanbudget.app.model.asMoney
import com.cyanbudget.app.widget.BudgetWidgetProvider

class FinanceReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        BudgetWidgetProvider.updateAll(applicationContext)
        if (inputData.getBoolean("show_summary", false)) {
            val manager = applicationContext.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(NotificationChannel("summaries", "Financial summaries", NotificationManager.IMPORTANCE_DEFAULT))
            val summary = FinanceInsights.summary(FinanceDatabase.get(applicationContext).transactions())
            manager.notify(1001, NotificationCompat.Builder(applicationContext, "summaries")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Your weekly money check-in")
                .setContentText("You spent ${summary.weekCents.asMoney("USD")} in the last 7 days.")
                .setAutoCancel(true).build())
        }
        return Result.success()
    }
}
