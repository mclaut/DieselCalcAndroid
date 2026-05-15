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
import androidx.glance.layout.*
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider

// Diesel widget — три рядки даних (Вигрузка / Кордон / ICE Gasoil),
// дата + час оновлення вертикально в правій колонці, adaptive theme.

val KEY_BORDER    = stringPreferencesKey("price_border")
val KEY_DELIVERY  = stringPreferencesKey("price_delivery")
val KEY_ICE_PRICE = stringPreferencesKey("price_ice")
val KEY_UPDATED   = stringPreferencesKey("updated_at")
val KEY_UPDATED_DATE = stringPreferencesKey("updated_date")

class DieselWidget : GlanceAppWidget() {

    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent { Content() }
    }

    @Composable
    private fun Content() {
        val prefs    = currentState<Preferences>()
        val delivery = prefs[KEY_DELIVERY]  ?: "—"
        val border   = prefs[KEY_BORDER]    ?: "—"
        val ice      = prefs[KEY_ICE_PRICE] ?: "—"
        val time     = prefs[KEY_UPDATED]   ?: ""
        val date     = prefs[KEY_UPDATED_DATE] ?: ""

        val ctx      = LocalContext.current
        val isDark   = (ctx.resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

        // Адаптивна палітра: темна як було, світла як системні віджети.
        val bg          = if (isDark) Color(0xFF000000) else Color(0xFFF2F2F2)
        val orangeMain  = if (isDark) Color(0xFFFFA500) else Color(0xFFD97000)
        val orangeDim   = if (isDark) Color(0xCCFFA500) else Color(0xCCD97000)
        val blueMain    = if (isDark) Color(0xFF73C7FF) else Color(0xFF006BD8)
        val blueDim     = if (isDark) Color(0xCC73C7FF) else Color(0xCC006BD8)
        val iceMain     = if (isDark) Color(0xD9FFFFFF) else Color(0xBF000000)
        val dim         = if (isDark) Color(0x73FFFFFF) else Color(0x80000000)

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(bg)
                .padding(start = 12.dp, top = 10.dp, bottom = 10.dp, end = 8.dp)
        ) {
            Row(
                modifier          = GlanceModifier.fillMaxSize(),
                verticalAlignment = Alignment.Vertical.CenterVertically
            ) {
                // Ліва колонка — три блоки цін
                Column(modifier = GlanceModifier.defaultWeight()) {
                    // Вигрузка
                    Text(
                        "Вигрузка",
                        style = TextStyle(color = ColorProvider(orangeDim), fontSize = 12.sp)
                    )
                    Text(
                        delivery,
                        style = TextStyle(
                            color      = ColorProvider(orangeMain),
                            fontSize   = 26.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )

                    Spacer(GlanceModifier.height(8.dp))

                    // Кордон
                    Text(
                        "Кордон",
                        style = TextStyle(color = ColorProvider(blueDim), fontSize = 12.sp)
                    )
                    Text(
                        border,
                        style = TextStyle(
                            color      = ColorProvider(blueMain),
                            fontSize   = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )

                    Spacer(GlanceModifier.height(8.dp))

                    // ICE Gasoil
                    Text(
                        "ICE Gasoil",
                        style = TextStyle(color = ColorProvider(dim), fontSize = 11.sp)
                    )
                    Text(
                        "$$ice",
                        style = TextStyle(
                            color      = ColorProvider(iceMain),
                            fontSize   = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                // Права колонка — час і дата вертикально, прижаті до правого краю
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
}
