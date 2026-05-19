package com.mclaut.dieselcalc

import android.content.Context
import androidx.work.*
import java.util.concurrent.TimeUnit

/**
 * Періодичний фоновий refresh даних усіх віджетів — кожні 30 хв.
 * Перший запуск (одразу після додавання) робиться без network-constraint
 * щоб не чекати на WorkManager network-check.
 *
 * Реальна логіка fetch+save вся у WidgetDataSync (єдина точка для віджетів
 * і worker'а одночасно).
 */
class WidgetUpdateWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val ok = WidgetDataSync.refresh(applicationContext, force = true)
        return if (ok) Result.success() else Result.retry()
    }

    companion object {
        private const val WORK_TAG = "DieselWidgetUpdate"

        fun enqueue(context: Context, immediate: Boolean = false) {
            val wm = WorkManager.getInstance(context)

            // Періодика — з network-constraint (не намагаємось fetch без мережі)
            val periodic = PeriodicWorkRequestBuilder<WidgetUpdateWorker>(30, TimeUnit.MINUTES)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .addTag(WORK_TAG)
                .build()
            wm.enqueueUniquePeriodicWork(WORK_TAG, ExistingPeriodicWorkPolicy.KEEP, periodic)

            // Негайний запуск БЕЗ constraint — щоб дані з'явились моментально
            // на щойно доданому віджеті, навіть якщо WorkManager думає що мережа
            // "metered" або "in-progress". Якщо мережі реально нема — worker
            // поверне retry, WorkManager сам ретрайне коли з'явиться.
            if (immediate) {
                val oneTime = OneTimeWorkRequestBuilder<WidgetUpdateWorker>()
                    .build()
                wm.enqueue(oneTime)
            }
        }
    }
}
