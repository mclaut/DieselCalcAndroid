package com.mclaut.dieselcalc

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

// 4 receivers — по одному на віджет джерела. Усі викликають той самий
// WidgetUpdateWorker, який тягне дані для всіх джерел і розкидає їх по
// відповідних Glance Preferences.

class NBUWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = NBUWidget()

    override fun onUpdate(context: Context, m: AppWidgetManager, ids: IntArray) {
        super.onUpdate(context, m, ids)
        WidgetUpdateWorker.enqueue(context, immediate = true)
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        WidgetUpdateWorker.enqueue(context, immediate = true)
    }
}

class InterbankWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = InterbankWidget()

    override fun onUpdate(context: Context, m: AppWidgetManager, ids: IntArray) {
        super.onUpdate(context, m, ids)
        WidgetUpdateWorker.enqueue(context, immediate = true)
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        WidgetUpdateWorker.enqueue(context, immediate = true)
    }
}

class PrivatCardWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = PrivatCardWidget()

    override fun onUpdate(context: Context, m: AppWidgetManager, ids: IntArray) {
        super.onUpdate(context, m, ids)
        WidgetUpdateWorker.enqueue(context, immediate = true)
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        WidgetUpdateWorker.enqueue(context, immediate = true)
    }
}

class PrivatCashWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = PrivatCashWidget()

    override fun onUpdate(context: Context, m: AppWidgetManager, ids: IntArray) {
        super.onUpdate(context, m, ids)
        WidgetUpdateWorker.enqueue(context, immediate = true)
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        WidgetUpdateWorker.enqueue(context, immediate = true)
    }
}
