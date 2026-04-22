package com.mclaut.dieselcalc

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.work.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.max

class WidgetUpdateWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val icePrice = fetchInvestingPrice() ?: return Result.retry()
        val usdUah   = fetchUSDMizhbank()    ?: return Result.retry()
        val eurUah   = fetchEURNBU()         ?: return Result.retry()

        val p      = WidgetPrefs.loadParams(applicationContext)
        val litres = max(p.litres, 1.0)

        val pricePer1000L    = (icePrice + p.premium) * usdUah * p.density
        val exciseUAH        = p.exciseEur * eurUah
        val transportPer1000L = (p.roadUah / litres) * 1000.0
        val totalBeforeVAT   = pricePer1000L + exciseUAH + transportPer1000L
        val vatPer1000L      = totalBeforeVAT * 0.20
        val finalPer1000L    = totalBeforeVAT + vatPer1000L
        val borderPerLitre   = finalPer1000L / 1000.0
        val deliveryTotal    = borderPerLitre + (p.deliveryUah / litres)

        val border   = formatNum(borderPerLitre, 2)
        val delivery = formatNum(deliveryTotal,  2)
        val time     = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

        val glanceIds = withContext(Dispatchers.Main) {
            GlanceAppWidgetManager(applicationContext).getGlanceIds(DieselWidget::class.java)
        }

        for (id in glanceIds) {
            updateAppWidgetState(applicationContext, PreferencesGlanceStateDefinition, id) { prefs ->
                prefs.toMutablePreferences().apply {
                    this[KEY_BORDER]   = border
                    this[KEY_DELIVERY] = delivery
                    this[KEY_UPDATED]  = time
                }
            }
            DieselWidget().update(applicationContext, id)
        }

        return Result.success()
    }

    companion object {
        private const val WORK_TAG = "DieselWidgetUpdate"

        fun enqueue(context: Context, immediate: Boolean = false) {
            val wm = WorkManager.getInstance(context)

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            // Periodic: кожні 30 хвилин
            val periodic = PeriodicWorkRequestBuilder<WidgetUpdateWorker>(30, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .addTag(WORK_TAG)
                .build()
            wm.enqueueUniquePeriodicWork(WORK_TAG, ExistingPeriodicWorkPolicy.UPDATE, periodic)

            // Негайний запуск якщо потрібно
            if (immediate) {
                val oneTime = OneTimeWorkRequestBuilder<WidgetUpdateWorker>()
                    .setConstraints(constraints)
                    .build()
                wm.enqueue(oneTime)
            }
        }
    }
}
