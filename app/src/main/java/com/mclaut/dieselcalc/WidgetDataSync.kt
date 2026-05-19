package com.mclaut.dieselcalc

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max

/**
 * Єдина точка для оновлення даних усіх 5 віджетів.
 *
 * Викликається ДВОМА шляхами:
 * 1) **Кожен виджет у `provideGlance`** — гарантує що при першому рендері
 *    (відразу після додавання на екран) дані вже завантажені, а не "—".
 * 2) **WidgetUpdateWorker** — фоновий періодичний refresh кожні 30 хв.
 *
 * Mutex+TTL гарантує що 5 одночасних викликів від різних віджетів роблять
 * ОДИН HTTP-запит, інші чекають на mutex і використовують свіжий кеш.
 */
object WidgetDataSync {
    private val mutex = Mutex()
    @Volatile private var lastFetchMs = 0L
    private const val CACHE_TTL_MS = 5 * 60 * 1000L  // 5 хв

    /**
     * Перевіряє свіжість і за потреби тягне нові дані. `force=true` ігнорує TTL
     * (використовує worker для періодики).
     *
     * @return true якщо хоч щось вдалось оновити (або кеш свіжий), false при
     *         повному фейлі мережі.
     */
    suspend fun refresh(context: Context, force: Boolean = false): Boolean {
        return mutex.withLock {
            if (!force && System.currentTimeMillis() - lastFetchMs < CACHE_TTL_MS) {
                return@withLock true   // дані свіжі, нічого робити
            }
            val ok = fetchAndStore(context)
            if (ok) lastFetchMs = System.currentTimeMillis()
            ok
        }
    }

    private suspend fun fetchAndStore(context: Context): Boolean {
        val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        val date = SimpleDateFormat("dd.MM", Locale.getDefault()).format(Date())
        var anySuccess = false

        // 1) Currency widgets — повний список курсів USD/EUR + крос
        val usdRates = fetchAllRates("USD")
        val eurRates = fetchAllRates("EUR")
        val cross    = fetchEurUsdCross()

        if (usdRates.isNotEmpty() || eurRates.isNotEmpty()) {
            anySuccess = true
            for (source in CurrencySource.values()) {
                val u = usdRates.firstOrNull { it.source == source.key }
                val e = eurRates.firstOrNull { it.source == source.key }
                val (widget, cls) = currencyWidgetFor(source)

                updateAllOfType(context, widget, cls) { prefs ->
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
        }

        // 2) Diesel розрахунок — незалежно
        val icePrice = fetchInvestingPrice()
        val usdMed   = fetchUSDMizhbank()
        val eurMed   = fetchEURNBU()

        if (icePrice != null && usdMed != null && eurMed != null) {
            anySuccess = true
            val p      = WidgetPrefs.loadParams(context)
            val litres = max(p.litres, 1.0)

            val pricePer1000L     = (icePrice + p.premium) * usdMed * p.density
            val exciseUAH         = p.exciseEur * eurMed
            val transportPer1000L = (p.roadUah / litres) * 1000.0
            val totalBeforeVAT    = pricePer1000L + exciseUAH + transportPer1000L
            val vatPer1000L       = totalBeforeVAT * 0.20
            val finalPer1000L     = totalBeforeVAT + vatPer1000L
            val borderPerLitre    = finalPer1000L / 1000.0
            val deliveryTotal     = borderPerLitre + (p.deliveryUah / litres)

            updateAllOfType(context, DieselWidget(), DieselWidget::class.java) { prefs ->
                prefs[KEY_BORDER]       = formatNum(borderPerLitre, 2)
                prefs[KEY_DELIVERY]     = formatNum(deliveryTotal,  2)
                prefs[KEY_ICE_PRICE]    = formatNum(icePrice, 0)
                prefs[KEY_UPDATED]      = time
                prefs[KEY_UPDATED_DATE] = date
            }
        }
        return anySuccess
    }

    private fun currencyWidgetFor(source: CurrencySource): Pair<GlanceAppWidget, Class<out GlanceAppWidget>> =
        when (source) {
            CurrencySource.NBU        -> NBUWidget()        to NBUWidget::class.java
            CurrencySource.INTERBANK  -> InterbankWidget()  to InterbankWidget::class.java
            CurrencySource.PRIVATCARD -> PrivatCardWidget() to PrivatCardWidget::class.java
            CurrencySource.PRIVATCASH -> PrivatCashWidget() to PrivatCashWidget::class.java
        }

    private suspend fun updateAllOfType(
        context: Context,
        widget: GlanceAppWidget,
        cls: Class<out GlanceAppWidget>,
        mutate: (androidx.datastore.preferences.core.MutablePreferences) -> Unit
    ) {
        val ids = withContext(Dispatchers.Main) {
            GlanceAppWidgetManager(context).getGlanceIds(cls)
        }
        for (id in ids) {
            updateAppWidgetState(context, PreferencesGlanceStateDefinition, id) { prefs ->
                prefs.toMutablePreferences().apply { mutate(this) }
            }
            widget.update(context, id)
        }
    }
}
