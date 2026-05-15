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
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.*
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import java.util.Locale

// === Currency widget — 4 джерела (НБУ / Міжбанк / Privat Card / Privat Cash). ===
// Кожне джерело має свій GlanceAppWidget + Receiver + xml-config. Дані для всіх
// тягне один WidgetUpdateWorker і складає в Glance Preferences з префіксом джерела.

enum class CurrencySource(val key: String, val displayName: String, val hasBidAsk: Boolean) {
    NBU       ("nbu",             "НБУ",              hasBidAsk = false),
    INTERBANK ("minfin_interbank","Міжбанк",          hasBidAsk = true),
    PRIVATCARD("privat24_card",   "Privat24 Карта",   hasBidAsk = true),
    PRIVATCASH("privat24_cash",   "Privat24 Готівка", hasBidAsk = true)
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
        provideContent { Content() }
    }

    @Composable
    private fun Content() {
        val prefs = currentState<Preferences>()
        val ctx   = LocalContext.current
        val isDark = (ctx.resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

        // McLaut brand-палітра: USD = помаранчевий, EUR = темно-індиго,
        // крос-курс — нейтральний (не з брендової двоколірки, щоб не змішувати).
        val bg         = if (isDark) Color(0xFF000000) else Color(0xFFF2F2F2)
        val usdColor   = Brand.Orange
        val eurColor   = Brand.darkAccent(isDark)
        val crossColor = if (isDark) Color(0xD9FFFFFF) else Color(0xBF000000)
        val header     = if (isDark) Color(0xB3FFFFFF) else Color(0xB3000000)
        val dim        = if (isDark) Color(0x73FFFFFF) else Color(0x80000000)

        val usdBid = prefs[ratePref(source.key, "USD", "bid")]
        val usdAsk = prefs[ratePref(source.key, "USD", "ask")]
        val usdRate = prefs[ratePref(source.key, "USD", "rate")]
        val eurBid = prefs[ratePref(source.key, "EUR", "bid")]
        val eurAsk = prefs[ratePref(source.key, "EUR", "ask")]
        val eurRate = prefs[ratePref(source.key, "EUR", "rate")]
        val cross  = prefs[crossPref(source.key)]
        val time   = prefs[timePref(source.key)] ?: ""
        val date   = prefs[datePref(source.key)] ?: ""

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(bg)
                .padding(start = 12.dp, top = 8.dp, bottom = 8.dp, end = 8.dp)
        ) {
            Row(
                modifier          = GlanceModifier.fillMaxSize(),
                verticalAlignment = Alignment.Vertical.CenterVertically
            ) {
                // Ліва колонка — джерело + USD + EUR + крос
                Column(modifier = GlanceModifier.defaultWeight()) {
                    Text(
                        source.displayName,
                        style = TextStyle(
                            color      = ColorProvider(header),
                            fontSize   = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(GlanceModifier.height(4.dp))

                    // USD
                    RateRow("USD", usdColor, source.hasBidAsk, usdBid, usdAsk, usdRate)
                    Spacer(GlanceModifier.height(4.dp))

                    // EUR
                    RateRow("EUR", eurColor, source.hasBidAsk, eurBid, eurAsk, eurRate)

                    if (cross != null) {
                        Spacer(GlanceModifier.height(4.dp))
                        Row(verticalAlignment = Alignment.Vertical.CenterVertically) {
                            Text(
                                "€/$ ",
                                style = TextStyle(
                                    color    = ColorProvider(crossColor.copy(alpha = 0.75f)),
                                    fontSize = 12.sp
                                )
                            )
                            Text(
                                String.format(Locale.US, "%.4f", cross),
                                style = TextStyle(
                                    color      = ColorProvider(crossColor),
                                    fontSize   = 19.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }

                // Права колонка — час + дата вертикально, прижаті до краю
                Column(
                    horizontalAlignment = Alignment.Horizontal.End,
                    verticalAlignment   = Alignment.Vertical.CenterVertically,
                    modifier            = GlanceModifier.padding(start = 4.dp)
                ) {
                    if (time.isNotEmpty()) {
                        Text(
                            time,
                            style = TextStyle(
                                color      = ColorProvider(dim),
                                fontSize   = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                    if (date.isNotEmpty()) {
                        Text(
                            date,
                            style = TextStyle(color = ColorProvider(dim), fontSize = 10.sp)
                        )
                    }
                }
            }
        }
    }

    /**
     * Рядок курсу. Якщо джерело hasBidAsk — показуємо bid/ask двома рядками з ↓/↑.
     * Інакше — single rate в один рядок з лейблом.
     */
    @Composable
    private fun RateRow(
        label: String,
        color: Color,
        hasBidAsk: Boolean,
        bid: Double?, ask: Double?, rate: Double?
    ) {
        if (hasBidAsk && bid != null && ask != null) {
            Column {
                Text(
                    label,
                    style = TextStyle(
                        color    = ColorProvider(color.copy(alpha = 0.75f)),
                        fontSize = 12.sp
                    )
                )
                Row(verticalAlignment = Alignment.Vertical.CenterVertically) {
                    Text("↓ ", style = TextStyle(color = ColorProvider(color.copy(alpha = 0.55f)), fontSize = 11.sp))
                    Text(
                        String.format(Locale("uk", "UA"), "%.2f", bid),
                        style = TextStyle(color = ColorProvider(color), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    )
                }
                Row(verticalAlignment = Alignment.Vertical.CenterVertically) {
                    Text("↑ ", style = TextStyle(color = ColorProvider(color.copy(alpha = 0.55f)), fontSize = 11.sp))
                    Text(
                        String.format(Locale("uk", "UA"), "%.2f", ask),
                        style = TextStyle(color = ColorProvider(color), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    )
                }
            }
        } else {
            // Single rate (НБУ або fallback) — компактний рядок "USD  43,97"
            Row(verticalAlignment = Alignment.Vertical.CenterVertically) {
                Text(
                    "$label ",
                    style = TextStyle(
                        color    = ColorProvider(color.copy(alpha = 0.75f)),
                        fontSize = 12.sp
                    )
                )
                Text(
                    rate?.let { String.format(Locale("uk", "UA"), "%.2f", it) } ?: "—",
                    style = TextStyle(
                        color      = ColorProvider(color),
                        fontSize   = 22.sp,
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
