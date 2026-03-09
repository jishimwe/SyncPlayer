package com.jpishimwe.syncplayer.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import com.jpishimwe.syncplayer.R

val provider: GoogleFont.Provider =
    GoogleFont.Provider(
        providerAuthority = "com.google.android.gms.font",
        providerPackage = "com.google.android.gms",
        certificates = R.array.com_google_android_gms_fonts_certs,
    )

@OptIn(ExperimentalTextApi::class)
val fontFamily: FontFamily =
    FontFamily(
        Font(
            R.font.nunito_sans,
            weight = FontWeight.Thin,
            variationSettings =
                FontVariation.Settings(
                    FontVariation.weight(100),
                ),
        ),
        Font(
            R.font.nunito_sans,
            weight = FontWeight.ExtraLight,
            variationSettings =
                FontVariation.Settings(
                    FontVariation.weight(200),
                ),
        ),
        Font(
            R.font.nunito_sans,
            weight = FontWeight.Light,
            variationSettings =
                FontVariation.Settings(
                    FontVariation.weight(300),
                ),
        ),
        Font(
            R.font.nunito_sans,
            weight = FontWeight.Normal,
            variationSettings =
                FontVariation.Settings(
                    FontVariation.weight(400),
                ),
        ),
        Font(
            R.font.nunito_sans,
            weight = FontWeight.Medium,
            variationSettings =
                FontVariation.Settings(
                    FontVariation.weight(500),
                ),
        ),
        Font(
            R.font.nunito_sans,
            weight = FontWeight.SemiBold,
            variationSettings =
                FontVariation.Settings(
                    FontVariation.weight(600),
                ),
        ),
        Font(
            R.font.nunito_sans,
            weight = FontWeight.Bold,
            variationSettings =
                FontVariation.Settings(
                    FontVariation.weight(700),
                ),
        ),
        Font(
            R.font.nunito_sans,
            weight = FontWeight.ExtraBold,
            variationSettings =
                FontVariation.Settings(
                    FontVariation.weight(800),
                ),
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
                fontWeight = FontWeight.Thin,
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
