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
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
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

        // McLaut brand-палітра.
        val bg             = if (isDark) Color(0xFF000000) else Color(0xFFF2F2F2)
        val orangeMain     = Brand.Orange
        val orangeDim      = Brand.Orange.copy(alpha = 0.80f)
        val brandIndigo    = Brand.darkAccent(isDark)
        val brandIndigoDim = brandIndigo.copy(alpha = 0.80f)
        val iceMain        = if (isDark) Color(0xD9FFFFFF) else Color(0xBF000000)
        val dim            = if (isDark) Color(0x99FFFFFF) else Color(0x99000000)

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(bg)
                .padding(start = 6.dp, top = 6.dp, bottom = 6.dp, end = 4.dp)
        ) {
            Row(
                modifier          = GlanceModifier.fillMaxSize(),
                verticalAlignment = Alignment.Vertical.CenterVertically
            ) {
                // Ліва колонка — Вигрузка / Кордон / ICE (компактно під 70dp)
                Column(modifier = GlanceModifier.defaultWeight()) {
                    Text(
                        "Вигрузка",
                        style = TextStyle(color = ColorProvider(orangeDim), fontSize = 9.sp)
                    )
                    Text(
                        delivery,
                        style = TextStyle(
                            color      = ColorProvider(orangeMain),
                            fontSize   = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(GlanceModifier.height(2.dp))

                    Text(
                        "Кордон",
                        style = TextStyle(color = ColorProvider(brandIndigoDim), fontSize = 9.sp)
                    )
                    Text(
                        border,
                        style = TextStyle(
                            color      = ColorProvider(brandIndigo),
                            fontSize   = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(GlanceModifier.height(2.dp))

                    Text(
                        "ICE \$$ice",
                        style = TextStyle(
                            color      = ColorProvider(iceMain),
                            fontSize   = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }

                // Права колонка — час + дата ВЕРТИКАЛЬНО (повернуто -90°), як в iOS.
                val whenText = buildString {
                    if (time.isNotEmpty()) append(time)
                    if (time.isNotEmpty() && date.isNotEmpty()) append("·")
                    if (date.isNotEmpty()) append(date)
                }
                if (whenText.isNotEmpty()) {
                    Spacer(GlanceModifier.width(2.dp))
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier         = GlanceModifier.width(12.dp).fillMaxSize()
                    ) {
                        RotatedVerticalText(
                            text       = whenText,
                            color      = dim,
                            fontSizeSp = 9f,
                            bold       = true
                        )
                    }
                }
            }
        }
    }
}
