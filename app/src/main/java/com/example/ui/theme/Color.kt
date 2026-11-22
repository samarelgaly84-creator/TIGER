package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Core Brand Palette — Tiger Obsidian Luxury
val TigerOrange = Color(0xFFF97316)
val TigerOrangeLight = Color(0xFFFB923C)
val TigerOrangeDark = Color(0xFFEA580C)
val TigerGold = Color(0xFFFBBF24)
val TigerAmberGlow = Color(0xFFF59E0B)

// Obsidian Dark Neutrals
val ObsidianBg = Color(0xFF090D16)
val ObsidianSurface = Color(0xFF111827)
val ObsidianSurfaceVariant = Color(0xFF1E293B)
val ObsidianBorder = Color(0xFF334155)
val ObsidianTextPrimary = Color(0xFFF8FAFC)
val ObsidianTextSecondary = Color(0xFF94A3B8)

// Warm Ivory Light Neutrals
val IvoryBg = Color(0xFFFFFAF5)
val IvorySurface = Color(0xFFFFFFFF)
val IvorySurfaceVariant = Color(0xFFFFF1E6)
val IvoryBorder = Color(0xFFE2E8F0)
val IvoryTextPrimary = Color(0xFF0F172A)
val IvoryTextSecondary = Color(0xFF475569)

// Semantic Status Colors
val EmeraldSuccess = Color(0xFF10B981)
val CrimsonDanger = Color(0xFFEF4444)
val CobaltInfo = Color(0xFF3B82F6)
val VioletLive = Color(0xFF8B5CF6)

enum class AccentPreset(
    val id: String,
    val labelEn: String,
    val labelAr: String,
    val primary: Color,
    val secondary: Color
) {
    TIGER_ORANGE("orange", "Tiger Orange", "برتقالي تايجر", Color(0xFFF97316), Color(0xFFFBBF24)),
    ROYAL_GOLD("gold", "Royal Gold", "ذهبي ملكي", Color(0xFFEAB308), Color(0xFFF97316)),
    CYBER_EMERALD("emerald", "Cyber Emerald", "زمردي رقمي", Color(0xFF10B981), Color(0xFF34D399)),
    ELECTRIC_COBALT("cobalt", "Electric Cobalt", "أزرق كوبالت", Color(0xFF3B82F6), Color(0xFF60A5FA)),
    CRIMSON_PULSE("crimson", "Crimson Pulse", "قرمزي نابض", Color(0xFFEF4444), Color(0xFFF97316));

    companion object {
        fun fromId(id: String): AccentPreset = entries.firstOrNull { it.id == id } ?: TIGER_ORANGE
    }
}
