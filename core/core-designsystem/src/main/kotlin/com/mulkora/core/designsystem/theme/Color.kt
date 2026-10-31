package com.mulkora.core.designsystem.theme

import androidx.compose.ui.graphics.Color

// --- Mülkora Marka Renkleri ---
// CLAUDE.md Bölüm 28: Tasarım Dili

// Ana renk — Güven ve güvenliği temsil eden mavi-yeşil tonu
val MulkoraGreen = Color(0xFF1A7A4A)
val MulkoraGreenLight = Color(0xFF4CAF80)
val MulkoraGreenDark = Color(0xFF0D5533)

// İkincil renk — Isı ve güven için amber
val MulkoraAmber = Color(0xFFF59E0B)
val MulkoraAmberLight = Color(0xFFFCD34D)
val MulkoraAmberDark = Color(0xFFB45309)

// Nötr renkler
val MulkoraNeutral50  = Color(0xFFF9FAFB)
val MulkoraNeutral100 = Color(0xFFF3F4F6)
val MulkoraNeutral200 = Color(0xFFE5E7EB)
val MulkoraNeutral300 = Color(0xFFD1D5DB)
val MulkoraNeutral400 = Color(0xFF9CA3AF)
val MulkoraNeutral500 = Color(0xFF6B7280)
val MulkoraNeutral600 = Color(0xFF4B5563)
val MulkoraNeutral700 = Color(0xFF374151)
val MulkoraNeutral800 = Color(0xFF1F2937)
val MulkoraNeutral900 = Color(0xFF111827)

// Durum renkleri
val MulkoraSuccess = Color(0xFF22C55E)
val MulkoraWarning = Color(0xFFF59E0B)
val MulkoraError   = Color(0xFFEF4444)
val MulkoraInfo    = Color(0xFF3B82F6)

// --- Açık Tema Renkleri (Material 3) ---
val LightPrimary            = MulkoraGreen
val LightOnPrimary          = Color.White
val LightPrimaryContainer   = Color(0xFFBBF7D0)
val LightOnPrimaryContainer = Color(0xFF003319)

val LightSecondary            = MulkoraAmber
val LightOnSecondary          = Color.White
val LightSecondaryContainer   = Color(0xFFFEF3C7)
val LightOnSecondaryContainer = Color(0xFF451A03)

val LightBackground      = MulkoraNeutral50
val LightOnBackground    = MulkoraNeutral900
val LightSurface         = Color.White
val LightOnSurface       = MulkoraNeutral900
val LightSurfaceVariant  = MulkoraNeutral100
val LightOnSurfaceVariant = MulkoraNeutral600
val LightOutline         = MulkoraNeutral300

val LightError   = MulkoraError
val LightOnError = Color.White

// --- Koyu Tema Renkleri (Material 3) ---
val DarkPrimary            = MulkoraGreenLight
val DarkOnPrimary          = Color(0xFF003319)
val DarkPrimaryContainer   = MulkoraGreenDark
val DarkOnPrimaryContainer = Color(0xFFBBF7D0)

val DarkSecondary            = MulkoraAmberLight
val DarkOnSecondary          = Color(0xFF451A03)
val DarkSecondaryContainer   = MulkoraAmberDark
val DarkOnSecondaryContainer = Color(0xFFFEF3C7)

val DarkBackground      = MulkoraNeutral900
val DarkOnBackground    = MulkoraNeutral100
val DarkSurface         = MulkoraNeutral800
val DarkOnSurface       = MulkoraNeutral100
val DarkSurfaceVariant  = MulkoraNeutral700
val DarkOnSurfaceVariant = MulkoraNeutral300
val DarkOutline         = MulkoraNeutral600

val DarkError   = Color(0xFFFF6B6B)
val DarkOnError = Color(0xFF4A0000)

// --- Özel rol renkleri ---
val TenantBadgeColor   = MulkoraGreen       // Kiracı rozeti
val LandlordBadgeColor = MulkoraAmber       // Ev sahibi rozeti
val BothBadgeColor     = Color(0xFF6366F1) // Her iki rol

// Gönderi türü renkleri (ikon arka planı için)
val PostTypeQuestion   = Color(0xFF3B82F6) // Soru — Mavi
val PostTypeExperience = MulkoraGreen       // Deneyim — Yeşil
val PostTypeProblem    = MulkoraError       // Sorun — Kırmızı
val PostTypeTip        = MulkoraAmber       // Tavsiye — Amber
val PostTypeInfo       = Color(0xFF8B5CF6) // Bilgilendirme — Mor
val PostTypeDebate     = Color(0xFF06B6D4) // Tartışma — Cyan