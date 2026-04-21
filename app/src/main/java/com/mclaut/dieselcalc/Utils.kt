package com.mclaut.dieselcalc

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import java.text.NumberFormat
import java.util.Locale

fun formatNum(value: Double, decimals: Int = 0): String {
    val nf = NumberFormat.getNumberInstance(Locale("uk", "UA"))
    nf.minimumFractionDigits = decimals
    nf.maximumFractionDigits = decimals
    return nf.format(value)
}

fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("DieselCalc", text))
}
