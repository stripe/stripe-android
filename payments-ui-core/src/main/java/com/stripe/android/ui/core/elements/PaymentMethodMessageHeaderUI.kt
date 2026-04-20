package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.tooling.preview.Preview
import com.stripe.android.model.PaymentMethodMessageLearnMore
import com.stripe.android.model.PaymentMethodMessagePromotion

@Composable
internal fun PaymentMethodMessageHeaderUI(
    element: PaymentMethodMessageHeaderElement,
    modifier: Modifier
) {
    PaymentMethodMessagePromotionText(element.promotion, modifier)
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Composable
fun PaymentMethodMessagePromotionText(
    promotion: PaymentMethodMessagePromotion,
    modifier: Modifier = Modifier
) {
    val message = buildAnnotatedString {
        append(promotion.message.maybeAddPeriod())
        withLink(
            LinkAnnotation.Url(
                url = promotion.learnMore.url,
                styles = TextLinkStyles(
                    style = SpanStyle(
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colors.primary,
                        textDecoration = TextDecoration.Underline
                    )
                ),
            )
        ) {
            append(promotion.learnMore.message)
        }
    }
    Text(
        text = message,
        style = MaterialTheme.typography.body1,
        modifier = modifier
    )
}

private fun String.maybeAddPeriod(): String {
    return if (endsWith('.')) this else "$this. "
}
