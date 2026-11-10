package com.notas.app.backup

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.Calendar
import java.util.concurrent.TimeUnit

class BackupWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val prefs = BackupPrefs(applicationContext)
        if (!prefs.enabled || !prefs.isConfigured) return Result.success()

        val outcome = BackupManager.sendBackup(applicationContext)
        return when {
            outcome.isSuccess -> Result.success()
            runAttemptCount < 3 -> Result.retry()
            else -> Result.failure()
        }
    }
}

object BackupScheduler {
    private const val UNIQUE_NAME = "anota_daily_backup"

    /**
     * Agenda (ou cancela) o backup diário conforme as configurações.
     * replace = true  -> recalcula o horário (usado ao salvar as configurações)
     * replace = false -> mantém o que já está agendado (usado ao abrir o app)
     */
    fun schedule(context: Context, replace: Boolean) {
        val prefs = BackupPrefs(context)
        val wm = WorkManager.getInstance(context.applicationContext)

        if (!prefs.enabled || !prefs.isConfigured) {
            wm.cancelUniqueWork(UNIQUE_NAME)
            return
        }

        val now = Calendar.getInstance()
        val next = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, prefs.hour)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (!after(now)) add(Calendar.DAY_OF_MONTH, 1)
        }
        val delay = next.timeInMillis - now.timeInMillis

        val request = PeriodicWorkRequestBuilder<BackupWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.MINUTES)
            .build()

        wm.enqueueUniquePeriodicWork(
            UNIQUE_NAME,
            if (replace) ExistingPeriodicWorkPolicy.UPDATE else ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }
}
