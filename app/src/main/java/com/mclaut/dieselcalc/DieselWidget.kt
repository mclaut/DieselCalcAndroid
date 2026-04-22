package com.mclaut.dieselcalc

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.*
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider

val KEY_BORDER   = stringPreferencesKey("price_border")
val KEY_DELIVERY = stringPreferencesKey("price_delivery")
val KEY_UPDATED  = stringPreferencesKey("updated_at")

class DieselWidget : GlanceAppWidget() {

    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent { Content() }
    }

    @Composable
    private fun Content() {
        val prefs    = currentState<Preferences>()
        val delivery = prefs[KEY_DELIVERY] ?: "—"
        val border   = prefs[KEY_BORDER]   ?: "—"
        val updated  = prefs[KEY_UPDATED]  ?: "ще не оновлено"

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(Color(0xFF0D1F0DL))
                .padding(12.dp)
        ) {
            Column(
                modifier          = GlanceModifier.fillMaxSize(),
                verticalAlignment = Alignment.Vertical.CenterVertically
            ) {
                androidx.glance.text.Text(
                    "⛽ DieselCalc",
                    style = TextStyle(
                        color      = ColorProvider(Color(0xFF4CAF50L)),
                        fontSize   = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(GlanceModifier.height(8.dp))

                androidx.glance.text.Text(
                    "Вигрузка",
                    style = TextStyle(color = ColorProvider(Color(0xFFF47920L)), fontSize = 11.sp)
                )
                androidx.glance.text.Text(
                    "$delivery грн/л",
                    style = TextStyle(
                        color      = ColorProvider(Color(0xFFF47920L)),
                        fontSize   = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(GlanceModifier.height(8.dp))

                androidx.glance.text.Text(
                    "Кордон",
                    style = TextStyle(color = ColorProvider(Color(0xFF2196F3L)), fontSize = 11.sp)
                )
                androidx.glance.text.Text(
                    "$border грн/л",
                    style = TextStyle(
                        color      = ColorProvider(Color(0xFF2196F3L)),
                        fontSize   = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(GlanceModifier.height(8.dp))

                androidx.glance.text.Text(
                    "↻ $updated",
                    style = TextStyle(
                        color    = ColorProvider(Color(0xFF888888L)),
                        fontSize = 9.sp
                    )
                )
            }
        }
    }
}
