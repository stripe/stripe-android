package com.stripe.android.paymentsheet.ui

import android.graphics.Typeface
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.core.content.res.ResourcesCompat
import com.stripe.android.uicore.StripeThemeDefaults
import com.stripe.android.uicore.stripeTypography

@Composable
internal fun ErrorMessage(
    error: String,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val typography = MaterialTheme.stripeTypography

    val typeface = remember(typography) {
        typography.fontFamily?.let {
            ResourcesCompat.getFont(context, it)
        } ?: Typeface.DEFAULT
    }

    val fontSize = remember(typography) {
        with(density) {
            val sizeInPx = StripeThemeDefaults.typography.smallFontSize.value
            (sizeInPx.dp * typography.fontSizeMultiplier).toSp()
        }
    }

    Text(
        text = error,
        fontSize = fontSize,
        color = MaterialTheme.colors.error,
        fontFamily = FontFamily(typeface),
        modifier = modifier.semantics {
            this.liveRegion = LiveRegionMode.Assertive
        },
    )
}
