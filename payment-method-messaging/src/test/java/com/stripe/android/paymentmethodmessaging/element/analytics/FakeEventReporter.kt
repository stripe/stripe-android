@file:OptIn(PaymentMethodMessagingElementPreview::class)

package com.stripe.android.paymentmethodmessaging.element.analytics

import com.stripe.android.paymentmethodmessaging.element.PaymentMethodMessagingContent
import com.stripe.android.paymentmethodmessaging.element.PaymentMethodMessagingElement
import com.stripe.android.paymentmethodmessaging.element.PaymentMethodMessagingElementPreview

internal class FakeEventReporter : PaymentMethodMessagingEventReporter {
    override fun onInit() {
        // NO-OP
    }

    override fun onLoadStarted(configuration: PaymentMethodMessagingElement.Configuration.State) {
        // NO-OP
    }

    override fun onLoadSucceeded(
        paymentMethods: List<String>,
        content: PaymentMethodMessagingContent
    ) {
        // NO-OP
    }

    override fun onLoadFailed(error: Throwable) {
        // NO-OP
    }

    override fun onElementDisplayed(appearance: PaymentMethodMessagingElement.Appearance.State) {
        // NO-OP
    }

    override fun onElementTapped() {
        // NO-OP
    }
}
