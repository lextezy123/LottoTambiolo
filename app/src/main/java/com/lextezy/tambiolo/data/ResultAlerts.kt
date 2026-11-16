package com.lextezy.tambiolo.data

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.lextezy.tambiolo.MainActivity
import com.lextezy.tambiolo.R
import com.lextezy.tambiolo.TambioloApplication
import com.lextezy.tambiolo.ui.i18n.stringsFor
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/**
 * Result alerts. About once an hour, while alerts are on, the phone checks the
 * results files and shows one notification listing any draws it hasn't seen yet.
 * Everything happens on the phone: no account, no push server.
 */
object ResultAlerts {
    private const val CHANNEL_ID = "results"
    private const val WORK_NAME = "results-check"
    private const val NOTIFICATION_ID = 1001

    fun canNotify(context: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun schedule(context: Context) {
        val request = PeriodicWorkRequestBuilder<ResultsCheckWorker>(1, TimeUnit.HOURS)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }

    fun show(context: Context, text: AlertText, channelName: String) {
        if (!canNotify(context) || text.lines.isEmpty()) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_DEFAULT)
        )
        val open = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pending = PendingIntent.getActivity(
            context, 0, open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        // Results usually land between 10 PM and early morning. Never make a sound while people sleep.
        val hour = manilaNow().hour
        val quiet = hour >= 22 || hour < 7
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_tambiolo)
            .setContentTitle(text.title)
            .setContentText(text.lines.first())
            .setStyle(NotificationCompat.BigTextStyle().bigText(text.lines.joinToString("\n")))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setSilent(quiet)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            // Permission was taken away between the check and the call; nothing to do.
        }
    }
}

class ResultsCheckWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext as? TambioloApplication ?: return Result.success()
        val prefs = app.prefs
        if (!prefs.alertsEnabled.first()) return Result.success()

        val lotto = try {
            app.results.refresh().draws
        } catch (e: Exception) {
            return Result.retry()
        }
        val digits = runCatching { app.results.refreshDigits().draws }.getOrDefault(emptyList())

        val today = manilaNow().toLocalDate()
        val seen = prefs.seenDraws.first()
        if (seen != null) {
            val fresh = findNewResults(lotto, digits, seen, prefs.alertGames.first(), today)
            if (!fresh.isEmpty) {
                val s = stringsFor(prefs.language.first())
                ResultAlerts.show(applicationContext, alertText(fresh, prefs.combos.first(), s), s.alertChannel)
            }
        }
        // First run just records what exists, so turning alerts on never announces old draws.
        prefs.setSeenDraws(recentKeys(lotto, digits, today))
        return Result.success()
    }
}
