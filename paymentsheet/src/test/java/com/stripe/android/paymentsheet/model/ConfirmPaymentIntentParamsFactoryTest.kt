package com.stripe.android.paymentsheet.model

import com.google.common.truth.Truth
import com.stripe.android.model.CardBrand
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.PaymentMethodCreateParamsFixtures
import com.stripe.android.model.PaymentMethodOptionsParams
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
                    customerRequestedSave = PaymentSelection.CustomerRequestedSave.RequestReuse
                )
            )
        ).isEqualTo(
            ConfirmPaymentIntentParams.createWithPaymentMethodCreateParams(
                paymentMethodCreateParams = PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
                clientSecret = CLIENT_SECRET,
                setupFutureUsage = null,
                paymentMethodOptions = PaymentMethodOptionsParams.Card(
                    setupFutureUsage = ConfirmPaymentIntentParams.SetupFutureUsage.OffSession
                )
            )
        )
    }

    @Test
    fun `create() with new card when savePaymentMethod is true should create params with setupFutureUsage = blank`() {
        Truth.assertThat(
            factory.create(
                paymentSelection = PaymentSelection.New.Card(
                    PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
                    CardBrand.Visa,
                    customerRequestedSave = PaymentSelection.CustomerRequestedSave.RequestNoReuse
                )
            )
        ).isEqualTo(
            ConfirmPaymentIntentParams.createWithPaymentMethodCreateParams(
                paymentMethodCreateParams = PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
                clientSecret = CLIENT_SECRET,
                setupFutureUsage = null,
                paymentMethodOptions = PaymentMethodOptionsParams.Card(
                    setupFutureUsage = ConfirmPaymentIntentParams.SetupFutureUsage.Blank
                )
            )
        )
    }

    @Test
    fun `create() with new card when savePaymentMethod is true should create params with setupFutureUsage = null`() {
        Truth.assertThat(
            factory.create(
                paymentSelection = PaymentSelection.New.Card(
                    PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
                    CardBrand.Visa,
                    customerRequestedSave = PaymentSelection.CustomerRequestedSave.NoRequest
                )
            )
        ).isEqualTo(
            ConfirmPaymentIntentParams.createWithPaymentMethodCreateParams(
                paymentMethodCreateParams = PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
                clientSecret = CLIENT_SECRET,
                setupFutureUsage = null,
                paymentMethodOptions = PaymentMethodOptionsParams.Card()
            )
        )
    }

    private companion object {
        private const val CLIENT_SECRET = com.stripe.android.paymentsheet.PaymentSheetFixtures.CLIENT_SECRET
    }
}
