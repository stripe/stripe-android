package com.stripe.android.ui.core.elements

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withLink

@Composable
internal fun PaymentMethodMessageHeaderUI(paymentMethodMessage: PaymentMethodMessageHeaderElement) {
    val promotion = paymentMethodMessage.messagePromotion
    val message = buildAnnotatedString {
        append(promotion.message)
        append(". ")
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
