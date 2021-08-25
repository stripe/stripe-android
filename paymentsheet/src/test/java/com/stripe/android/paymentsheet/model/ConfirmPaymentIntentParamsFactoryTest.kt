package com.stripe.android.paymentsheet.model

import com.google.common.truth.Truth
import com.stripe.android.model.CardBrand
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.PaymentMethodCreateParamsFixtures
import org.junit.Test

class ConfirmPaymentIntentParamsFactoryTest {
    private val factory = ConfirmPaymentIntentParamsFactory(
        PaymentIntentClientSecret(CLIENT_SECRET)
    )

    @Test
    fun `create() with new card when savePaymentMethod is true should create params with setupFutureUsage = OffSession`() {
        Truth.assertThat(
            factory.create(
                paymentSelection = PaymentSelection.New.Card(
                    PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
                    CardBrand.Visa,
                    shouldSavePaymentMethod = true
                )
            )
        ).isEqualTo(
            ConfirmPaymentIntentParams.createWithPaymentMethodCreateParams(
                paymentMethodCreateParams = PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
                clientSecret = CLIENT_SECRET,
                setupFutureUsage = ConfirmPaymentIntentParams.SetupFutureUsage.OffSession
            )
        )
    }

    private companion object {
        private const val CLIENT_SECRET = com.stripe.android.paymentsheet.PaymentSheetFixtures.CLIENT_SECRET
    }
}
