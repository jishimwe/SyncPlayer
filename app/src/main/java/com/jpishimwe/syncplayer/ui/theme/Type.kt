package com.jpishimwe.syncplayer.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import com.jpishimwe.syncplayer.R

val provider: GoogleFont.Provider =
    GoogleFont.Provider(
        providerAuthority = "com.google.android.gms.fonts",
        providerPackage = "com.google.android.gms",
        certificates = R.array.com_google_android_gms_fonts_certs,
    )

val bodyFontFamily: FontFamily =
    FontFamily(
        Font(
            googleFont = GoogleFont("Nunito Sans"),
            fontProvider = provider,
        ),
    )

val displayFontFamily: FontFamily =
    FontFamily(
        Font(
            googleFont = GoogleFont("Nunito Sans"),
            fontProvider = provider,
        ),
    )

val fontFamily: FontFamily =
    FontFamily(
        Font(
            googleFont = GoogleFont("Nunito Sans"),
            fontProvider = provider,
            weight = FontWeight.Normal,
        ),
        Font(
            googleFont = GoogleFont("Nunito Sans"),
            fontProvider = provider,
            weight = FontWeight.Medium,
        ),
        Font(
            googleFont = GoogleFont("Nunito Sans"),
            fontProvider = provider,
            weight = FontWeight.SemiBold,
        ),
        Font(
            googleFont = GoogleFont("Nunito Sans"),
            fontProvider = provider,
            weight = FontWeight.Bold,
        ),
        Font(
            googleFont = GoogleFont("Nunito Sans"),
            fontProvider = provider,
            weight = FontWeight.ExtraBold,
        ),
    )

// Default Material 3 typography values
val baseline = Typography()

val AppTypography =
    Typography(
        displayLarge =
            baseline.displayLarge.copy(
                fontFamily = fontFamily,
                fontWeight = FontWeight.ExtraBold,
            ),
        displayMedium = baseline.displayMedium.copy(fontFamily = fontFamily),
        displaySmall =
            baseline.displaySmall.copy(
                fontFamily = fontFamily,
                fontWeight = FontWeight.ExtraBold,
            ),
        headlineLarge =
            baseline.headlineLarge.copy(
                fontFamily = fontFamily,
                fontWeight = FontWeight.ExtraBold,
            ),
        headlineMedium = baseline.headlineMedium.copy(fontFamily = fontFamily),
        headlineSmall = baseline.headlineSmall.copy(fontFamily = fontFamily),
        titleLarge =
            baseline.titleLarge.copy(
                fontFamily = fontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
            ),
        titleMedium =
            baseline.titleMedium.copy(
                fontFamily = fontFamily,
                fontWeight = FontWeight.SemiBold,
            ),
        titleSmall = baseline.titleSmall.copy(fontFamily = fontFamily),
        bodyLarge = baseline.bodyLarge.copy(fontFamily = fontFamily),
        bodyMedium = baseline.bodyMedium.copy(fontFamily = fontFamily),
        bodySmall = baseline.bodySmall.copy(fontFamily = fontFamily),
        labelLarge = baseline.labelLarge.copy(fontFamily = fontFamily),
        labelMedium = baseline.labelMedium.copy(fontFamily = fontFamily),
        labelSmall =
            baseline.labelSmall.copy(
                fontFamily = fontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 12.sp,
            ),
    )
