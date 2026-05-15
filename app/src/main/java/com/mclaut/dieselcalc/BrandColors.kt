package com.mclaut.dieselcalc

import androidx.compose.ui.graphics.Color

/**
 * McLaut корпоративна палітра.
 *
 * Має збігатися з:
 *   - EVchargersAndroid `ui/theme/Color.kt`
 *   - EVchargers iOS `Sources/EVchargers/Core/AppColors.swift`
 *   - `res/values/colors.xml`
 *
 * Брендова двоколірка: помаранчевий + темно-індиго. Помаранчевий читається
 * однаково добре і на чорному, і на світлому фоні. Індиго `#1D1766` нестерпно
 * темний для dark-режиму — для нього беремо світлий тонал `#B9B0FF` (90-й
 * tone стандартного Material indigo-tonal-palette).
 */
object Brand {
    // Primary brand
    val Orange     = Color(0xFFF08521)   // McLautOrange — у обох темах
    // Secondary brand — два варіанти за темою
    val Dark       = Color(0xFF1D1766)   // McLautDark — light theme accent
    val DarkOnDark = Color(0xFFB9B0FF)   // tonal-90 indigo — dark theme accent

    /** Брендовий індиго, адаптований під фон. */
    fun darkAccent(isDark: Boolean): Color = if (isDark) DarkOnDark else Dark
}
