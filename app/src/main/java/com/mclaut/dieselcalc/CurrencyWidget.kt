package com.mclaut.dieselcalc

import android.content.Context
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import java.util.Locale

// === Currency widget — 4 джерела (НБУ / Міжбанк / Privat Card / Privat Cash). ===
// 1×1 (70dp) square compact layout. Dіта+час повернуті на -90° (як в iOS)
// у вузькій правій колонці.

enum class CurrencySource(val key: String, val displayName: String, val short: String, val hasBidAsk: Boolean) {
    NBU       ("nbu",              "НБУ",              "НБУ",  hasBidAsk = false),
    INTERBANK ("minfin_interbank", "Міжбанк",          "Між",  hasBidAsk = true),
    PRIVATCARD("privat24_card",    "Privat24 Карта",   "Карта", hasBidAsk = true),
    PRIVATCASH("privat24_cash",    "Privat24 Готівка", "Готів", hasBidAsk = true)
}

// Ключі: "currency_<source>_<currency>_<field>". Напр. currency_nbu_usd_rate.
fun ratePref(source: String, ccy: String, field: String) =
    doublePreferencesKey("currency_${source}_${ccy.lowercase()}_$field")

fun crossPref(source: String) = doublePreferencesKey("currency_${source}_cross")
fun timePref (source: String) = stringPreferencesKey("currency_${source}_time")
fun datePref (source: String) = stringPreferencesKey("currency_${source}_date")

abstract class CurrencyWidget(private val source: CurrencySource) : GlanceAppWidget() {

    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // Гарантуємо що дані завантажені перед першим рендером усіх 4
        // currency-віджетів. TTL у DataSync дедуплікує одночасні виклики.
        WidgetDataSync.refresh(context)
        provideContent { Content() }
    }

    @Composable
    private fun Content() {
        val prefs = currentState<Preferences>()
        val ctx   = LocalContext.current
        val isDark = (ctx.resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

        // McLaut brand-палітра + читабельний фон (не pure-black щоб не
        // зливалося з темними шпалерами).
        val bg         = if (isDark) Color(0xFF1C1C1E) else Color(0xFFF2F2F7)
        val usdColor   = Brand.Orange
        val eurColor   = Brand.darkAccent(isDark)
        val crossColor = if (isDark) Color(0xD9FFFFFF) else Color(0xBF000000)
        val header     = if (isDark) Color(0xCCFFFFFF) else Color(0xCC000000)
        val dim        = if (isDark) Color(0x99FFFFFF) else Color(0x99000000)

        val usdBid  = prefs[ratePref(source.key, "USD", "bid")]
        val usdAsk  = prefs[ratePref(source.key, "USD", "ask")]
        val usdRate = prefs[ratePref(source.key, "USD", "rate")]
        val eurBid  = prefs[ratePref(source.key, "EUR", "bid")]
        val eurAsk  = prefs[ratePref(source.key, "EUR", "ask")]
        val eurRate = prefs[ratePref(source.key, "EUR", "rate")]
        val cross   = prefs[crossPref(source.key)]
        val time    = prefs[timePref(source.key)] ?: ""
        val date    = prefs[datePref(source.key)] ?: ""

        // Wide 2×1 layout (~155×70dp). Зверху джерело, нижче три рівнозначні
        // колонки з курсами: USD / EUR / €/$. Bid/ask стискаємо у "X/Y" формат
        // щоб помістилось у тонку колонку.
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .cornerRadius(16.dp)
                .background(bg)
                .padding(start = 10.dp, top = 4.dp, bottom = 4.dp, end = 4.dp)
        ) {
            Row(
                modifier          = GlanceModifier.fillMaxSize(),
                verticalAlignment = Alignment.Vertical.CenterVertically
            ) {
                Column(modifier = GlanceModifier.defaultWeight()) {
                    // Верхній рядок: джерело + крос-курс справа на тому ж рядку
                    Row(
                        modifier          = GlanceModifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Vertical.CenterVertically
                    ) {
                        Text(
                            source.displayName,
                            style = TextStyle(
                                color      = ColorProvider(header),
                                fontSize   = 11.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            maxLines = 1,
                            modifier = GlanceModifier.defaultWeight()
                        )
                        if (cross != null) {
                            Text(
                                "€/\$ " + String.format(Locale.US, "%.4f", cross),
                                style = TextStyle(
                                    color      = ColorProvider(crossColor),
                                    fontSize   = 11.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                maxLines = 1
                            )
                        }
                    }
                    Spacer(GlanceModifier.height(3.dp))
                    // 2 колонки валют (USD + EUR) — більше місця для bid/ask
                    Row(modifier = GlanceModifier.fillMaxWidth()) {
                        WideRateColumn("USD", usdColor, source.hasBidAsk, usdBid, usdAsk, usdRate)
                        WideRateColumn("EUR", eurColor, source.hasBidAsk, eurBid, eurAsk, eurRate)
                    }
                }

                // Права rotated дата по всій висоті
                val whenText = buildString {
                    if (time.isNotEmpty()) append(time)
                    if (time.isNotEmpty() && date.isNotEmpty()) append("·")
                    if (date.isNotEmpty()) append(date)
                }
                if (whenText.isNotEmpty()) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier         = GlanceModifier
                            .width(12.dp)
                            .fillMaxHeight()
                    ) {
                        RotatedVerticalText(
                            text       = whenText,
                            color      = dim,
                            fontSizeSp = 8f,
                            bold       = true
                        )
                    }
                }
            }
        }
    }

    /**
     * Колонка курсу для wide 2-column layout: лейбл валюти + значення.
     * Тепер тільки 2 колонки (USD + EUR), крос виносимо в верхній рядок
     * до джерела — це дає по 50dp ширини на колонку замість 35dp,
     * тому bid/ask можна показувати на 2 рядках бóльшим шрифтом.
     *
     * Single rate (НБУ) — одне число 18sp.
     * Bid/ask (Privat/Міжбанк) — два числа стеком, ↓bid + ↑ask по 16sp.
     */
    @Composable
    private fun androidx.glance.layout.RowScope.WideRateColumn(
        label: String,
        color: Color,
        hasBidAsk: Boolean,
        bid: Double?, ask: Double?, rate: Double?
    ) {
        Column(modifier = GlanceModifier.defaultWeight()) {
            Text(
                label,
                style = TextStyle(
                    color    = ColorProvider(color.copy(alpha = 0.75f)),
                    fontSize = 10.sp
                )
            )
            if (hasBidAsk && bid != null && ask != null) {
                Text(
                    "↓" + String.format(Locale("uk", "UA"), "%.2f", bid),
                    style = TextStyle(
                        color      = ColorProvider(color),
                        fontSize   = 16.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 1
                )
                Text(
                    "↑" + String.format(Locale("uk", "UA"), "%.2f", ask),
                    style = TextStyle(
                        color      = ColorProvider(color),
                        fontSize   = 16.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 1
                )
            } else {
                Text(
                    rate?.let { String.format(Locale("uk", "UA"), "%.2f", it) } ?: "—",
                    style = TextStyle(
                        color      = ColorProvider(color),
                        fontSize   = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }
}

// === 4 конкретні віджети ===

class NBUWidget        : CurrencyWidget(CurrencySource.NBU)
class InterbankWidget  : CurrencyWidget(CurrencySource.INTERBANK)
class PrivatCardWidget : CurrencyWidget(CurrencySource.PRIVATCARD)
class PrivatCashWidget : CurrencyWidget(CurrencySource.PRIVATCASH)
