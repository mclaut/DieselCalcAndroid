package com.mclaut.dieselcalc

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.layout.ContentScale

/**
 * Створює bitmap із текстом, повернутим на -90° (рендерим горизонтально, потім
 * повертаємо матрицею). Виходить вертикальний рядок, що читається знизу-вгору
 * з правого боку — як iOS `rotationEffect(.degrees(-90))`.
 *
 * Чому через Matrix, а не Canvas.rotate: rotation на canvas нестабільно
 * взаємодіє з anti-aliasing і Glance ImageProvider — текст інколи рендерився
 * горизонтально. Matrix.postRotate на готовий bitmap гарантує повернений
 * результат.
 *
 * Чому `density` а не `scaledDensity`: scaledDensity множить на font-scale
 * системи, тому при налаштованому крупному шрифті у користувача bitmap
 * вибухав до 200dp. Беремо raw density — фіксований pixel-розмір.
 */
private fun createRotatedTextBitmap(
    text: String,
    fontSizePx: Float,
    colorArgb: Int,
    bold: Boolean
): Bitmap {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = fontSizePx
        color    = colorArgb
        typeface = if (bold) Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                   else      Typeface.DEFAULT
    }
    val textWidth  = paint.measureText(text).toInt().coerceAtLeast(1)
    val fm         = paint.fontMetrics
    val textHeight = (fm.descent - fm.ascent).toInt().coerceAtLeast(1)

    // 1) Рендерим горизонтально на bitmap (textWidth × textHeight).
    val horizontal = Bitmap.createBitmap(textWidth, textHeight, Bitmap.Config.ARGB_8888)
    val canvas     = Canvas(horizontal)
    canvas.drawText(text, 0f, -fm.ascent, paint)

    // 2) Повертаємо весь bitmap на -90° — отримуємо bitmap textHeight × textWidth.
    val matrix = Matrix().apply { postRotate(-90f) }
    return Bitmap.createBitmap(horizontal, 0, 0, textWidth, textHeight, matrix, true)
}

/**
 * Composable: text повернутий на -90° у Glance Image.
 *
 * Розміри підбираються так, щоб результат був вузький по ширині (~10-14dp)
 * і високий по вертикалі (~50-90dp залежно від кількості символів). Image
 * без жорсткої size — натуральний 1:1 pixel mapping.
 */
@Composable
fun RotatedVerticalText(
    text: String,
    color: Color,
    fontSizeSp: Float = 8f,
    bold: Boolean = true
) {
    if (text.isEmpty()) return
    val ctx     = LocalContext.current
    // density (НЕ scaledDensity) — щоб font-scale у Settings не роздував bitmap.
    val density = ctx.resources.displayMetrics.density
    val bitmap  = createRotatedTextBitmap(
        text       = text,
        fontSizePx = fontSizeSp * density,
        colorArgb  = color.toArgb(),
        bold       = bold
    )
    Image(
        provider           = ImageProvider(bitmap),
        contentDescription = null,
        modifier           = GlanceModifier,
        contentScale       = ContentScale.Fit
    )
}
