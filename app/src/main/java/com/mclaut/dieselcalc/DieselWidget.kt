package com.mclaut.dieselcalc

import android.content.Context
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
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
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider

// Diesel widget — 1×1 (70dp) square, компактний layout: Вигрузка / Кордон
// (значущі цифри great-bold) + ICE Gasoil дрібним. Дата+час повернуті на -90°
// у вузькій правій колонці (як iOS rotationEffect(-90)).

val KEY_BORDER       = stringPreferencesKey("price_border")
val KEY_DELIVERY     = stringPreferencesKey("price_delivery")
val KEY_ICE_PRICE    = stringPreferencesKey("price_ice")
val KEY_UPDATED      = stringPreferencesKey("updated_at")
val KEY_UPDATED_DATE = stringPreferencesKey("updated_date")

class DieselWidget : GlanceAppWidget() {

    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // Гарантуємо що дані завантажені перед першим рендером віджета
        // (якщо WorkManager не встиг або був заблокований constraint'ами).
        // TTL у DataSync дедуплікує паралельні виклики від 5 widget'ів.
        WidgetDataSync.refresh(context)
        provideContent { Content() }
    }

    @Composable
    private fun Content() {
        val prefs    = currentState<Preferences>()
        val delivery = prefs[KEY_DELIVERY]     ?: "—"
        val border   = prefs[KEY_BORDER]       ?: "—"
        val ice      = prefs[KEY_ICE_PRICE]    ?: "—"
        val time     = prefs[KEY_UPDATED]      ?: ""
        val date     = prefs[KEY_UPDATED_DATE] ?: ""

        val ctx     = LocalContext.current
        val isDark  = (ctx.resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

        // McLaut brand-палітра + читабельний фон. Pure #000 зливається з
        // темними шпалерами користувача — беремо чуть світліший Material
        // dark surface (#1C1C1E), а для світлої теми Apple-style #F2F2F7.
        val bg             = if (isDark) Color(0xFF1C1C1E) else Color(0xFFF2F2F7)
        val orangeMain     = Brand.Orange
        val orangeDim      = Brand.Orange.copy(alpha = 0.80f)
        val brandIndigo    = Brand.darkAccent(isDark)
        val brandIndigoDim = brandIndigo.copy(alpha = 0.80f)
        val iceMain        = if (isDark) Color(0xD9FFFFFF) else Color(0xBF000000)
        val dim            = if (isDark) Color(0x99FFFFFF) else Color(0x99000000)

        // Wide 2×1 layout: ~155×70dp. 3 секції горизонтально (Вигрузка /
        // Кордон / ICE), rotated дата справа по всій висоті.
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .cornerRadius(16.dp)
                .background(bg)
                .padding(start = 10.dp, top = 6.dp, bottom = 6.dp, end = 4.dp)
        ) {
            Row(
                modifier          = GlanceModifier.fillMaxSize(),
                verticalAlignment = Alignment.Vertical.CenterVertically
            ) {
                // Колонка 1 — Вигрузка
                Column(modifier = GlanceModifier.defaultWeight()) {
                    Text(
                        "Вигрузка",
                        style = TextStyle(color = ColorProvider(orangeDim), fontSize = 10.sp)
                    )
                    Text(
                        delivery,
                        style = TextStyle(
                            color      = ColorProvider(orangeMain),
                            fontSize   = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                // Колонка 2 — Кордон
                Column(modifier = GlanceModifier.defaultWeight()) {
                    Text(
                        "Кордон",
                        style = TextStyle(color = ColorProvider(brandIndigoDim), fontSize = 10.sp)
                    )
                    Text(
                        border,
                        style = TextStyle(
                            color      = ColorProvider(brandIndigo),
                            fontSize   = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                // Колонка 3 — ICE Gasoil
                Column(modifier = GlanceModifier.defaultWeight()) {
                    Text(
                        "ICE",
                        style = TextStyle(color = ColorProvider(iceMain.copy(alpha = 0.7f)), fontSize = 9.sp)
                    )
                    Text(
                        "\$$ice",
                        style = TextStyle(
                            color      = ColorProvider(iceMain),
                            fontSize   = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
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
}
