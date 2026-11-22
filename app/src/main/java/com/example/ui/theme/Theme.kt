package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

private val TigerShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp)
)

@Composable
fun TigerConnectTheme(
    darkTheme: Boolean = true,
    accentPreset: AccentPreset = AccentPreset.TIGER_ORANGE,
    isArabic: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = accentPreset.primary,
            onPrimary = Color(0xFF0B0F19),
            primaryContainer = accentPreset.primary.copy(alpha = 0.22f),
            onPrimaryContainer = accentPreset.secondary,
            secondary = accentPreset.secondary,
            onSecondary = Color(0xFF0B0F19),
            secondaryContainer = ObsidianSurfaceVariant,
            onSecondaryContainer = ObsidianTextPrimary,
            tertiary = EmeraldSuccess,
            onTertiary = Color(0xFF0B0F19),
            background = ObsidianBg,
            onBackground = ObsidianTextPrimary,
            surface = ObsidianSurface,
            onSurface = ObsidianTextPrimary,
            surfaceVariant = ObsidianSurfaceVariant,
            onSurfaceVariant = ObsidianTextSecondary,
            outline = ObsidianBorder,
            error = CrimsonDanger,
            onError = Color.White
        )
    } else {
        lightColorScheme(
            primary = accentPreset.primary,
            onPrimary = Color.White,
            primaryContainer = accentPreset.primary.copy(alpha = 0.14f),
            onPrimaryContainer = accentPreset.primary,
            secondary = accentPreset.secondary,
            onSecondary = Color(0xFF0F172A),
            secondaryContainer = IvorySurfaceVariant,
            onSecondaryContainer = IvoryTextPrimary,
            tertiary = EmeraldSuccess,
            onTertiary = Color.White,
            background = IvoryBg,
            onBackground = IvoryTextPrimary,
            surface = IvorySurface,
            onSurface = IvoryTextPrimary,
            surfaceVariant = IvorySurfaceVariant,
            onSurfaceVariant = IvoryTextSecondary,
            outline = IvoryBorder,
            error = CrimsonDanger,
            onError = Color.White
        )
    }

    val layoutDirection = if (isArabic) LayoutDirection.Rtl else LayoutDirection.Ltr

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = buildTigerTypography(isArabic),
            shapes = TigerShapes,
            content = content
        )
    }
}
