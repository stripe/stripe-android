package com.stripe.android.ui.core.elements

import android.content.Intent
import android.net.Uri
import androidx.annotation.RestrictTo
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import com.stripe.android.ui.core.R
import com.stripe.android.ui.core.elements.AfterpayClearpayHeaderElement.Companion.isClearpay
import com.stripe.android.uicore.shouldUseDarkDynamicColor
import com.stripe.android.uicore.stripeColors
import com.stripe.android.uicore.text.EmbeddableImage
import com.stripe.android.uicore.text.Html

@Composable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun AfterpayClearpayElementUI(
    enabled: Boolean,
    element: AfterpayClearpayHeaderElement,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val messageFormatString = element.getLabel(context.resources)
        .replace("<img/>", "<img src=\"afterpay\"/>")

    Html(
        html = messageFormatString,
        enabled = enabled,
        imageLoader = mapOf(
            "afterpay" to EmbeddableImage.Drawable(
                if (isClearpay()) {
                    R.drawable.stripe_ic_clearpay_logo
                } else {
                    R.drawable.stripe_ic_afterpay_logo
                },
                if (isClearpay()) {
                    R.string.stripe_paymentsheet_payment_method_clearpay
                } else {
                    R.string.stripe_paymentsheet_payment_method_afterpay
                },
                colorFilter = if (MaterialTheme.colors.surface.shouldUseDarkDynamicColor()) {
                    null
                } else {
                    ColorFilter.tint(Color.White)
                }
            )
        ),
        modifier = modifier,
        color = MaterialTheme.stripeColors.subtitle,
        style = MaterialTheme.typography.h6,
        urlSpanStyle = SpanStyle(),
        imageAlign = PlaceholderVerticalAlign.Bottom,
        onClick = {
            val openURL = Intent(Intent.ACTION_VIEW, Uri.parse(element.infoUrl))
            context.startActivity(openURL)
        }
    )
}
