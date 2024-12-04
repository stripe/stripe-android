package com.stripe.android.paymentsheet.ui

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
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.stripe.android.uicore.StripeThemeDefaults
import com.stripe.android.uicore.getOnSuccessBackgroundColor
import com.stripe.android.uicore.getSuccessBackgroundColor
import java.util.Locale

@Composable
internal fun PromoBadge(
    text: String,
    modifier: Modifier = Modifier,
    tinyMode: Boolean = false,
) {
    // TODO(tillh-stripe): Revisit how we want the badge text to scale in tiny mode
    FixedTextSize(fixed = tinyMode) {
        val backgroundColor = Color(
            color = StripeThemeDefaults.primaryButtonStyle.getSuccessBackgroundColor(LocalContext.current),
        )

        val foregroundColor = Color(
            color = StripeThemeDefaults.primaryButtonStyle.getOnSuccessBackgroundColor(LocalContext.current),
        )

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
                text = formatPromoText(text),
                color = foregroundColor,
                style = MaterialTheme.typography.caption.copy(
                    fontSize = StripeThemeDefaults.typography.xSmallFontSize,
                ),
            )
        }
    }
}

@Composable
private fun formatPromoText(text: String): String {
    val context = LocalContext.current
    val currentLocale: Locale = if (SDK_INT >= N) {
        context.resources.configuration.locales[0]
    } else {
        @Suppress("DEPRECATION")
        context.resources.configuration.locale
    }

    val isEnglish = currentLocale.language == Locale.ENGLISH.language
    return if (isEnglish) "Get $text" else text
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

private fun Density.copy(
    fontScale: Float = 1f,
): Density {
    return Density(
        density = density,
        fontScale = fontScale,
    )
}
