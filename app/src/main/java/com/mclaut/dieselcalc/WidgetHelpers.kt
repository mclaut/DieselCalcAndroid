package com.mclaut.dieselcalc

import android.graphics.Bitmap
import android.graphics.Canvas
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
 * Малює `text` під кутом -90° (як iOS `rotationEffect(.degrees(-90))`):
 * текст читається знизу-вгору з правого боку віджета. Glance не підтримує
 * `Modifier.rotate`, тому рендеримо в Bitmap і показуємо через `Image`.
 *
 * Розмір bitmap'у обчислюється точно під текст: вузький по ширині (висота
 * рядка), високий по вертикалі (довжина рядка) — щоб вписувався у вузьку
 * праву колонку віджета.
 */
private fun createRotatedTextBitmap(
    text: String,
    fontSizePx: Float,
    colorArgb: Int,
    bold: Boolean,
    paddingPx: Int = 2
): Bitmap {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = fontSizePx
        color    = colorArgb
        typeface = if (bold) Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                   else      Typeface.DEFAULT
    }
    val textWidth  = paint.measureText(text)
    val fm         = paint.fontMetrics
    val textHeight = (fm.descent - fm.ascent)

    val bmpWidth  = (textHeight + paddingPx * 2).toInt().coerceAtLeast(1)
    val bmpHeight = (textWidth  + paddingPx * 2).toInt().coerceAtLeast(1)
    val bmp       = Bitmap.createBitmap(bmpWidth, bmpHeight, Bitmap.Config.ARGB_8888)
    val canvas    = Canvas(bmp)

    // Поворот на -90°: текст, що рендериться горизонтально, стає вертикальним
    // і читається знизу вгору з правого боку (як iOS rotationEffect(-90)).
    canvas.save()
    canvas.translate(paddingPx.toFloat(), bmpHeight.toFloat() - paddingPx)
    canvas.rotate(-90f)
    canvas.drawText(text, 0f, -fm.ascent, paint)
    canvas.restore()

    return bmp
}

/**
 * Composable-обгортка: показує `text` повернутим на -90° через `Image`.
 * Шрифт ~9sp за замовч., bold для читабельності у маленькій правій колонці.
 */
@Composable
fun RotatedVerticalText(
    text: String,
    color: Color,
    fontSizeSp: Float = 9f,
    bold: Boolean = true
) {
    if (text.isEmpty()) return
    val ctx     = LocalContext.current
    val density = ctx.resources.displayMetrics.scaledDensity
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
