package com.mclaut.dieselcalc

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider

/**
 * Вертикальний текст: кожен символ на власному рядку.
 *
 * Glance не підтримує `rotationEffect`, тому імітуємо вертикальне читання
 * top→bottom стеком односимвольних `Text`. Час "14:30" виглядає як:
 *   1
 *   4
 *   :
 *   3
 *   0
 *
 * Використовується для правої колонки віджета "часом оновлення".
 */
@Composable
fun VerticalText(
    text: String,
    color: ColorProvider,
    fontSize: TextUnit = 9.sp,
    bold: Boolean = false
) {
    Column(horizontalAlignment = Alignment.Horizontal.End) {
        text.forEach { ch ->
            Text(
                ch.toString(),
                style = TextStyle(
                    color      = color,
                    fontSize   = fontSize,
                    fontWeight = if (bold) FontWeight.Medium else FontWeight.Normal
                )
            )
        }
    }
}
