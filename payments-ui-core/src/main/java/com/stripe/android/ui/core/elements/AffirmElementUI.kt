package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import com.stripe.android.ui.core.R
import com.stripe.android.uicore.stripeColors
import com.stripe.android.uicore.text.EmbeddableImage
import com.stripe.android.uicore.text.Html
import com.stripe.android.R as StripeR

private const val MIN_LUMINANCE_FOR_LIGHT_ICON = 0.5

@Composable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun AffirmElementUI(modifier: Modifier = Modifier) {
    val color = MaterialTheme.stripeColors.component
    val iconRes = if (color.luminance() < MIN_LUMINANCE_FOR_LIGHT_ICON) {
        R.drawable.stripe_ic_affirm_logo_night
    } else {
        R.drawable.stripe_ic_affirm_logo_day
    }
    Html(
        html = stringResource(id = StripeR.string.stripe_affirm_buy_now_pay_later),
        imageLoader = mapOf(
            "affirm" to EmbeddableImage.Drawable(
                iconRes,
                R.string.stripe_paymentsheet_payment_method_affirm
            )
        ),
        color = MaterialTheme.stripeColors.subtitle,
        style = MaterialTheme.typography.h6,
        modifier = modifier,
    )
}
