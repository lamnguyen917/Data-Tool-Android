package com.elp.datatool.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val Base = TextStyle(fontFamily = FontFamily.Default, letterSpacing = 0.sp)

// Type scale matching the wireframes (20 / 16 / 14 / 13 / 12 / 11 / 10 sp)
val Typography = Typography(
    headlineSmall = Base.copy(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.Medium),
    titleLarge = Base.copy(fontSize = 20.sp, lineHeight = 25.sp, fontWeight = FontWeight.Medium),
    titleMedium = Base.copy(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.Medium),
    titleSmall = Base.copy(fontSize = 14.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium),
    bodyLarge = Base.copy(fontSize = 15.sp, lineHeight = 22.sp),
    bodyMedium = Base.copy(fontSize = 13.sp, lineHeight = 18.sp),
    bodySmall = Base.copy(fontSize = 12.sp, lineHeight = 17.sp),
    labelLarge = Base.copy(fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium),
    labelMedium = Base.copy(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium),
    labelSmall = Base.copy(fontSize = 11.sp, lineHeight = 14.sp),
)

/** Small upper-case caption shown above screen titles ("DATA TOOL"). */
val EyebrowTextStyle = Base.copy(
    fontSize = 11.sp,
    lineHeight = 14.sp,
    fontWeight = FontWeight.Medium,
    letterSpacing = 1.3.sp,
)

/** Smallest metadata text used inside result cards. */
val CaptionTextStyle = Base.copy(fontSize = 10.sp, lineHeight = 13.sp)
