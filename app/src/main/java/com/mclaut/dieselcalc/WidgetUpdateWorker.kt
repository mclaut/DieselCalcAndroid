package com.mclaut.dieselcalc

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
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
        // Спочатку — Diesel-розрахунок (як раніше).
        val icePrice = fetchInvestingPrice() ?: return Result.retry()
        val usdMed   = fetchUSDMizhbank()    ?: return Result.retry()
        val eurMed   = fetchEURNBU()         ?: return Result.retry()

        val p      = WidgetPrefs.loadParams(applicationContext)
        val litres = max(p.litres, 1.0)

        val pricePer1000L     = (icePrice + p.premium) * usdMed * p.density
        val exciseUAH         = p.exciseEur * eurMed
        val transportPer1000L = (p.roadUah / litres) * 1000.0
        val totalBeforeVAT    = pricePer1000L + exciseUAH + transportPer1000L
        val vatPer1000L       = totalBeforeVAT * 0.20
        val finalPer1000L     = totalBeforeVAT + vatPer1000L
        val borderPerLitre    = finalPer1000L / 1000.0
        val deliveryTotal     = borderPerLitre + (p.deliveryUah / litres)

        val border   = formatNum(borderPerLitre, 2)
        val delivery = formatNum(deliveryTotal,  2)
        val iceStr   = formatNum(icePrice, 0)
        val time     = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        val date     = SimpleDateFormat("dd.MM", Locale.getDefault()).format(Date())

        // Оновлення Diesel-віджета.
        updateAllOfType(DieselWidget(), DieselWidget::class.java) { prefs ->
            prefs[KEY_BORDER]       = border
            prefs[KEY_DELIVERY]     = delivery
            prefs[KEY_ICE_PRICE]    = iceStr
            prefs[KEY_UPDATED]      = time
            prefs[KEY_UPDATED_DATE] = date
        }

        // === Currency widgets ===
        // Тягнемо повні списки одним заходом і розкидаємо по джерелах.
        val usdRates = fetchAllRates("USD")
        val eurRates = fetchAllRates("EUR")
        val cross    = fetchEurUsdCross()

        // Для кожного джерела — оновлюємо preferences відповідного віджета.
        for (source in CurrencySource.values()) {
            val u = usdRates.firstOrNull { it.source == source.key }
            val e = eurRates.firstOrNull { it.source == source.key }
            val widget: GlanceAppWidget = when (source) {
                CurrencySource.NBU        -> NBUWidget()
                CurrencySource.INTERBANK  -> InterbankWidget()
                CurrencySource.PRIVATCARD -> PrivatCardWidget()
                CurrencySource.PRIVATCASH -> PrivatCashWidget()
            }
            val cls: Class<out GlanceAppWidget> = when (source) {
                CurrencySource.NBU        -> NBUWidget::class.java
                CurrencySource.INTERBANK  -> InterbankWidget::class.java
                CurrencySource.PRIVATCARD -> PrivatCardWidget::class.java
                CurrencySource.PRIVATCASH -> PrivatCashWidget::class.java
            }

            updateAllOfType(widget, cls) { prefs ->
                u?.let {
                    if (it.bid != null) prefs[ratePref(source.key, "USD", "bid")] = it.bid
                    if (it.ask != null) prefs[ratePref(source.key, "USD", "ask")] = it.ask
                    prefs[ratePref(source.key, "USD", "rate")] = it.rate
                }
                e?.let {
                    if (it.bid != null) prefs[ratePref(source.key, "EUR", "bid")] = it.bid
                    if (it.ask != null) prefs[ratePref(source.key, "EUR", "ask")] = it.ask
                    prefs[ratePref(source.key, "EUR", "rate")] = it.rate
                }
                cross?.let { prefs[crossPref(source.key)] = it }
                prefs[timePref(source.key)] = time
                prefs[datePref(source.key)] = date
            }
        }

        return Result.success()
    }

    /** Оновлює preferences для всіх інстансів даного widget-класу. */
    private suspend fun updateAllOfType(
        widget: GlanceAppWidget,
        cls: Class<out GlanceAppWidget>,
        mutate: (androidx.datastore.preferences.core.MutablePreferences) -> Unit
    ) {
        val ids = withContext(Dispatchers.Main) {
            GlanceAppWidgetManager(applicationContext).getGlanceIds(cls)
        }
        for (id in ids) {
            updateAppWidgetState(applicationContext, PreferencesGlanceStateDefinition, id) { prefs ->
                prefs.toMutablePreferences().apply { mutate(this) }
            }
            widget.update(applicationContext, id)
        }
    }

    companion object {
        private const val WORK_TAG = "DieselWidgetUpdate"

        fun enqueue(context: Context, immediate: Boolean = false) {
            val wm = WorkManager.getInstance(context)

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val periodic = PeriodicWorkRequestBuilder<WidgetUpdateWorker>(30, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .addTag(WORK_TAG)
                .build()
            wm.enqueueUniquePeriodicWork(WORK_TAG, ExistingPeriodicWorkPolicy.UPDATE, periodic)

            if (immediate) {
                val oneTime = OneTimeWorkRequestBuilder<WidgetUpdateWorker>()
                    .setConstraints(constraints)
                    .build()
                wm.enqueue(oneTime)
            }
        }
    }
}
