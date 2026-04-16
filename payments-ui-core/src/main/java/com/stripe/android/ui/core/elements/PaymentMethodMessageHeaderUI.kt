package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withLink
import com.stripe.android.model.PaymentMethodMessagePromotion

@Composable
internal fun PaymentMethodMessageHeaderUI(element: PaymentMethodMessageHeaderElement) {
    PaymentMethodMessagePromotionText(element.promotion)
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Composable
fun PaymentMethodMessagePromotionText(promotion: PaymentMethodMessagePromotion) {
    val message = buildAnnotatedString {
        append(promotion.message.maybeAddPeriod())
        withLink(
            LinkAnnotation.Url(
                url = promotion.learnMore.url
            )
        ) {
            append(promotion.learnMore.message)
        }
    }
    Text(
        text = message
    )
}

private fun String.maybeAddPeriod(): String {
    return if (endsWith('.')) this else "$this. "
}
