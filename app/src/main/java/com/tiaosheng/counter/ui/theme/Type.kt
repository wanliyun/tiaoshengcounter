package com.tiaosheng.counter.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val CounterDisplay = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontWeight = FontWeight.Bold,
    fontSize = 72.sp,
    lineHeight = 80.sp,
    letterSpacing = (-2).sp
)

val HeadlineStat = TextStyle(
    fontWeight = FontWeight.SemiBold,
    fontSize = 28.sp,
    lineHeight = 32.sp
)

val BodyLabel = TextStyle(
    fontWeight = FontWeight.Normal,
    fontSize = 14.sp,
    lineHeight = 20.sp
)

val CaptionHint = TextStyle(
    fontWeight = FontWeight.Normal,
    fontSize = 12.sp,
    lineHeight = 16.sp
)

val TiaoshengTypography = Typography(
    displayLarge = CounterDisplay,
    headlineMedium = HeadlineStat,
    bodyMedium = BodyLabel,
    labelSmall = CaptionHint
)
