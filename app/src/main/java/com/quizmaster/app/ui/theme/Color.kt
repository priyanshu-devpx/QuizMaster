package com.quizmaster.app.ui.theme

import androidx.compose.ui.graphics.Color

// =========================================================
// QUIZMASTER — MODERN INDIGO & SLATE PALETTE (InspoAI Spec)
// =========================================================

// -------------------------
// BRAND ACCENTS
// -------------------------

val QuizPrimary = Color(0xFF6366F1)      // Light mode Electric Indigo (#6366F1)
val QuizPrimaryDark = Color(0xFF818CF8) // Dark mode Electric Indigo (#818CF8)

val QuizSecondary = Color(0xFF06B6D4)     // Light mode Vibrant Teal (#06B6D4)
val QuizSecondaryDark = Color(0xFF22D3EE) // Dark mode Vibrant Teal (#22D3EE)

val QuizTertiary = Color(0xFF06B6D4)
val QuizTertiaryDark = Color(0xFF22D3EE)

// =========================================================
// LIGHT THEME (Slate & Indigo)
// =========================================================

val LightBackground = Color(0xFFF8FAFC)        // #F8FAFC Cool Slate / Off-white
val LightSurface = Color(0xFFFFFFFF)           // #FFFFFF Cards / Question container
val LightSurfaceVariant = Color(0xFFF1F5F9)

val LightTextPrimary = Color(0xFF0F172A)       // #0F172A Deep Slate / Near-Black
val LightTextSecondary = Color(0xFF475569)     // #475569 Medium Slate Gray
val LightTextMuted = Color(0xFF64748B)

val LightOutline = Color(0xFFCBD5E1)
val LightOutlineVariant = Color(0xFFE2E8F0)    // #E2E8F0 Subtle 1px border

// Primary containers
val LightPrimaryContainer = Color(0xFFE0E7FF)
val LightOnPrimaryContainer = Color(0xFF1E1B4B)

// Secondary containers
val LightSecondaryContainer = Color(0xFFCFFAFE)
val LightOnSecondaryContainer = Color(0xFF164E63)

// Tertiary containers
val LightTertiaryContainer = Color(0xFFCFFAFE)
val LightOnTertiaryContainer = Color(0xFF164E63)

// =========================================================
// DARK THEME (Midnight Blue & Lighter Slate)
// =========================================================

val DarkBackground = Color(0xFF0F172A)       // #0F172A Deep Slate / Midnight Blue
val DarkSurface = Color(0xFF1E293B)          // #1E293B Lighter slate surface (Cards)
val DarkSurfaceVariant = Color(0xFF334155)

val DarkTextPrimary = Color(0xFFF1F5F9)      // #F1F5F9 Off-white / Ice
val DarkTextSecondary = Color(0xFF94A3B8)    // #94A3B8 Muted Slate
val DarkTextMuted = Color(0xFF94A3B8)

val DarkOutline = Color(0xFF334155)          // #334155 Border in dark mode
val DarkOutlineVariant = Color(0xFF475569)

// Primary & Dark Text on Primary Button in Dark Mode (#0F172A)
val DarkOnPrimary = Color(0xFF0F172A)        // Dark Text on #818CF8 primary buttons in dark mode
val DarkPrimaryContainer = Color(0xFF312E81)
val DarkOnPrimaryContainer = Color(0xFFE0E7FF)

// Secondary
val DarkOnSecondary = Color(0xFF0F172A)
val DarkSecondaryContainer = Color(0xFF1E3A8A)
val DarkOnSecondaryContainer = Color(0xFFDBEAFE)

// Tertiary
val DarkOnTertiary = Color(0xFF083344)
val DarkTertiaryContainer = Color(0xFF164E63)
val DarkOnTertiaryContainer = Color(0xFFCFFAFE)

// =========================================================
// SEMANTIC QUIZ FEEDBACK STATUS COLORS
// =========================================================

// SUCCESS (Correct Answer / Passed State) — Emerald
val QuizSuccessLight = Color(0xFF10B981)
val QuizSuccessDark = Color(0xFF10B981)

val QuizSuccessContainerLight = Color(0xFFDCFCE7)
val QuizSuccessContainerDark = Color(0xFF052E16)

// WARNING (Timer Low / Attention Needed) — Amber
val QuizWarningLight = Color(0xFFF59E0B)
val QuizWarningDark = Color(0xFFF59E0B)

val QuizWarningContainerLight = Color(0xFFFEF3C7)
val QuizWarningContainerDark = Color(0xFF422006)

// ERROR (Wrong Answer / Failed State) — Coral Red
val QuizErrorLight = Color(0xFFEF4444)
val QuizErrorDark = Color(0xFFEF4444)

val DarkOnErrorContainer = Color(0xFFFEE2E2)
val LightOnErrorContainer = Color(0xFF7F1D1D)
val QuizErrorContainerLight = Color(0xFFFEE2E2)
val QuizErrorContainerDark = Color(0xFF450A0A)

// =========================================================
// BASIC COLORS
// =========================================================

val White = Color(0xFFFFFFFF)
val Black = Color(0xFF000000)
val DarkOnError = Color(0xFF450A0A)