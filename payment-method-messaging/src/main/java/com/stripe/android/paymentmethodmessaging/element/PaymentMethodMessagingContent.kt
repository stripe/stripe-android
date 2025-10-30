@file:OptIn(PaymentMethodMessagingElementPreview::class)

package com.stripe.android.paymentmethodmessaging.element

import androidx.compose.runtime.Composable
import com.stripe.android.model.PaymentMethodMessage
import com.stripe.android.model.PaymentMethodMessageMultiPartner
import com.stripe.android.model.PaymentMethodMessageSinglePartner

internal sealed class PaymentMethodMessagingContent {

    @Composable
    abstract fun Content(appearance: PaymentMethodMessagingElement.Appearance.State)

    class SinglePartner(
        private val message: PaymentMethodMessageSinglePartner
    ) : PaymentMethodMessagingContent() {
        @Composable
        override fun Content(appearance: PaymentMethodMessagingElement.Appearance.State) {
            // NO-OP
        }
    }

    class MultiPartner(
        private val message: PaymentMethodMessageMultiPartner
    ) : PaymentMethodMessagingContent() {
        @Composable
        override fun Content(appearance: PaymentMethodMessagingElement.Appearance.State) {
            // NO-OP
        }
    }

    object NoContent : PaymentMethodMessagingContent() {
        @Composable
        override fun Content(appearance: PaymentMethodMessagingElement.Appearance.State) {
            // NO-OP
        }
    }

    companion object {
        fun get(message: PaymentMethodMessage): PaymentMethodMessagingContent {
            val singlePartnerMessage = message.singlePartner
            val multiPartnerMessage = message.multiPartner
            return if (singlePartnerMessage != null) {
                SinglePartner(singlePartnerMessage)
            } else if (multiPartnerMessage != null) {
                MultiPartner(multiPartnerMessage)
            } else {
                NoContent
            }
        }
    }
}
