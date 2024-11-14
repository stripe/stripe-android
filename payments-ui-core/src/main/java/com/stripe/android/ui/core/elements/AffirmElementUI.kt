package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.stripe.android.ui.core.R
import com.stripe.android.uicore.stripeColors
import com.stripe.android.uicore.text.EmbeddableImage
import com.stripe.android.uicore.text.Html
import com.stripe.android.R as StripeR

@Composable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun AffirmElementUI(modifier: Modifier = Modifier) {
    Html(
        html = stringResource(id = StripeR.string.stripe_affirm_buy_now_pay_later),
        imageLoader = mapOf(
            "affirm" to EmbeddableImage.Drawable(
                R.drawable.stripe_ic_affirm_logo,
                R.string.stripe_paymentsheet_payment_method_affirm
            )
        ),
        color = MaterialTheme.stripeColors.subtitle,
        style = MaterialTheme.typography.h6,
        modifier = modifier,
    )
}
