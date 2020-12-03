package com.stripe.android.paymentsheet.model

import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.PaymentMethodCreateParamsFixtures
import com.stripe.android.paymentsheet.PaymentSheetFixtures
import org.junit.Test

class ConfirmParamsFactoryTest {
    private val factory = ConfirmParamsFactory()

    @Test
    fun `create() when savePaymentMethod is true should create params with setupFutureUsage = OnSession`() {
        assertThat(
            factory.create(
                clientSecret = CLIENT_SECRET,
                paymentSelection = PaymentSelection.New(
                    PaymentMethodCreateParamsFixtures.DEFAULT_CARD
                ),
                shouldSavePaymentMethod = true
            )
        ).isEqualTo(
            ConfirmPaymentIntentParams.createWithPaymentMethodCreateParams(
                PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
                CLIENT_SECRET,
                setupFutureUsage = ConfirmPaymentIntentParams.SetupFutureUsage.OnSession
            )
        )
    }

    private companion object {
        private const val CLIENT_SECRET = PaymentSheetFixtures.CLIENT_SECRET
    }
}
