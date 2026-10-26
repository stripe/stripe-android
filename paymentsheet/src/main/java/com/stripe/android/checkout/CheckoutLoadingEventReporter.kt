package com.stripe.android.checkout

import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.paymentsheet.analytics.LoadingEventReporter
import com.stripe.android.paymentsheet.model.PaymentSelection
import javax.inject.Inject

internal class CheckoutLoadingEventReporter @Inject constructor() : LoadingEventReporter {
    override fun onLoadStarted(initializedViaCompose: Boolean, publishableKey: String) = Unit

    override fun onLoadSucceeded(
        paymentSelection: PaymentSelection?,
        paymentMethodMetadata: PaymentMethodMetadata,
    ) = Unit

    override fun onLoadFailed(error: Throwable, publishableKey: String) = Unit

    override fun onElementsSessionLoadFailed(error: Throwable) = Unit
}
