package com.mclaut.dieselcalc

import android.content.Context

data class CalcParams(
    val premium:     Double = 135.0,
    val roadUah:     Double = 70000.0,
    val deliveryUah: Double = 60000.0,
    val exciseEur:   Double = 253.8,
    val density:     Double = 0.84,
    val litres:      Double = 30000.0
)

object WidgetPrefs {
    private const val PREFS = "diesel_widget_prefs"

    fun saveParams(
        context:     Context,
        premium:     Double,
        roadUah:     Double,
        deliveryUah: Double,
        exciseEur:   Double,
        density:     Double,
        litres:      Double
    ) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().apply {
            putString("premium",      premium.toString())
            putString("road_uah",     roadUah.toString())
            putString("delivery_uah", deliveryUah.toString())
            putString("excise_eur",   exciseEur.toString())
            putString("density",      density.toString())
            putString("litres",       litres.toString())
            apply()
        }
    }

    fun loadParams(context: Context): CalcParams {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return CalcParams(
            premium     = p.getString("premium",      null)?.toDoubleOrNull() ?: 135.0,
            roadUah     = p.getString("road_uah",     null)?.toDoubleOrNull() ?: 70000.0,
            deliveryUah = p.getString("delivery_uah", null)?.toDoubleOrNull() ?: 60000.0,
            exciseEur   = p.getString("excise_eur",   null)?.toDoubleOrNull() ?: 253.8,
            density     = p.getString("density",      null)?.toDoubleOrNull() ?: 0.84,
            litres      = p.getString("litres",       null)?.toDoubleOrNull() ?: 30000.0
        )
    }
}
