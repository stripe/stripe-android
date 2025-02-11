package com.stripe.android.paymentsheet.ui

import android.content.Context
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.N
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.stripe.android.paymentsheet.R
import com.stripe.android.uicore.StripeThemeDefaults
import com.stripe.android.uicore.getOnSuccessBackgroundColor
import com.stripe.android.uicore.getSuccessBackgroundColor
import com.stripe.android.uicore.stripeColors
import java.util.Locale

@Composable
internal fun PromoBadge(
    text: String,
    modifier: Modifier = Modifier,
    eligible: Boolean = true,
    tinyMode: Boolean = false,
) {
    // TODO(tillh-stripe): Revisit how we want the badge text to scale in tiny mode
    FixedTextSize(fixed = tinyMode) {
        val backgroundColor = if (eligible) {
            Color(
                color = StripeThemeDefaults.primaryButtonStyle.getSuccessBackgroundColor(LocalContext.current),
            )
        } else {
            MaterialTheme.stripeColors.componentBorder
        }

        val foregroundColor = if (eligible) {
            Color(
                color = StripeThemeDefaults.primaryButtonStyle.getOnSuccessBackgroundColor(LocalContext.current),
            )
        } else {
            MaterialTheme.stripeColors.onComponent
        }

        val shape = MaterialTheme.shapes.medium

        Box(
            modifier = modifier
                .background(backgroundColor, shape)
                .padding(
                    horizontal = if (tinyMode) 4.dp else 6.dp,
                    vertical = if (tinyMode) 0.dp else 4.dp,
                )
        ) {
            Text(
                text = formatPromoText(text, eligible),
                color = foregroundColor,
                style = MaterialTheme.typography.caption.copy(
                    fontSize = StripeThemeDefaults.typography.xSmallFontSize,
                ),
            )
        }
    }
}

@Composable
private fun formatPromoText(
    text: String,
    eligible: Boolean,
): String {
    return if (eligible) {
        val context = LocalContext.current

        if (context.isEnglishLanguage) {
            "Get $text"
        } else {
            text
        }
    } else {
        stringResource(R.string.stripe_paymentsheet_bank_payment_promo_ineligible, text)
    }
}

@Composable
private fun FixedTextSize(
    fixed: Boolean,
    content: @Composable () -> Unit,
) {
    if (fixed) {
        val density = LocalDensity.current.copy(fontScale = 1f)
        CompositionLocalProvider(LocalDensity provides density) {
            content()
        }
    } else {
        content()
    }
}

private val Context.isEnglishLanguage: Boolean
    get() {
        val currentLocale: Locale = if (SDK_INT >= N) {
            resources.configuration.locales[0]
        } else {
            @Suppress("DEPRECATION")
            resources.configuration.locale
        }

        return currentLocale.language == Locale.ENGLISH.language
    }

private fun Density.copy(
    fontScale: Float = 1f,
): Density {
    return Density(
        density = density,
        fontScale = fontScale,
    )
}
