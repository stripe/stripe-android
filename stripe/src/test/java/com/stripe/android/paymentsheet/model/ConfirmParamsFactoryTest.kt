package com.stripe.android.paymentsheet.model

import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.CardBrand
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.PaymentMethodCreateParamsFixtures
import com.stripe.android.paymentsheet.PaymentSheetFixtures
import org.junit.Test

class ConfirmParamsFactoryTest {
    private val factory = ConfirmParamsFactory(CLIENT_SECRET)

    @Test
    fun `create() with new card when savePaymentMethod is true should create params with setupFutureUsage = OffSession`() {
        assertThat(
            factory.create(
                paymentSelection = PaymentSelection.New.Card(
                    PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
                    CardBrand.Visa,
                    shouldSavePaymentMethod = true
                )
            )
        ).isEqualTo(
            ConfirmPaymentIntentParams.createWithPaymentMethodCreateParams(
                PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
                CLIENT_SECRET,
                setupFutureUsage = ConfirmPaymentIntentParams.SetupFutureUsage.OffSession
            )
        )
    }

    private companion object {
        private const val CLIENT_SECRET = PaymentSheetFixtures.CLIENT_SECRET
    }
}
