package com.stripe.android.common.taptoadd

import androidx.compose.material.darkColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.ui.PrimaryButtonColors
import com.stripe.android.paymentsheet.ui.PrimaryButtonTheme
import com.stripe.android.uicore.StripeColors
import com.stripe.android.uicore.StripeTheme

@Composable
fun TapToAddTheme(
    content: @Composable () -> Unit,
) {
    StripeTheme(
        colors = TapToAddThemeDefaults.colors,

    ) {
        PrimaryButtonTheme(
            colors = TapToAddThemeDefaults.primaryButtonColors,
        ) {
            content()
        }
    }
}

private object TapToAddThemeDefaults {
    val colors = StripeColors(
        component = Color.Black,
        componentBorder = Color(0xFF48484A),
        componentDivider = Color(0xFF48484A),
        onComponent = Color.White,
        subtitle = Color(0x99FFFFFF),
        textCursor = Color.White,
        placeholderText = Color(0x61FFFFFF),
        appBarIcon = Color.White,
        materialColors = darkColors(
            primary = Color(0xFF272729),
            surface = Color.Black,
            onSurface = Color.White,
            error = Color.Red,
        ),
    )

    val primaryButtonColors: PrimaryButtonColors
        @Composable
        get() = PrimaryButtonColors(
            background = Color(0xFF272729),
            onBackground = Color.White,
            successBackground = colorResource(
                id = R.color.stripe_paymentsheet_googlepay_primary_button_background_color
            ),
            onSuccessBackground = colorResource(
                id = R.color.stripe_paymentsheet_googlepay_primary_button_tint_color
            )
        )
}