package com.stripe.android.checkout

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.utils.DurationProvider
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.utils.FakeDurationProvider
import org.junit.Test

internal class CheckoutLoadingEventReporterTest {

    @Test
    fun `onLoadSucceeded starts Checkout duration`() {
        val durationProvider = FakeDurationProvider()
        val loadingEventReporter = CheckoutLoadingEventReporter(durationProvider)

        loadingEventReporter.onLoadSucceeded(
            paymentSelection = null,
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(),
        )

        assertThat(
            durationProvider.has(
                FakeDurationProvider.Call.Start(
                    key = DurationProvider.Key.Checkout,
                    reset = true,
                )
            )
        ).isTrue()
    }
}
