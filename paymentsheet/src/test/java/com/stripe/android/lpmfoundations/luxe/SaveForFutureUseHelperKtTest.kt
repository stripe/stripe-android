package com.stripe.android.lpmfoundations.luxe

import com.google.common.truth.Truth.assertThat
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.SetupIntentFixtures
import com.stripe.android.model.StripeIntent
import kotlin.test.Test

class SaveForFutureUseHelperKtTest {
    @Test
    fun `isSaveForFutureUseValueChangeable returns false for SetupIntents`() {
        val isSaveForFutureUseValueChangeable = isSaveForFutureUseValueChangeable(
            code = PaymentMethod.Type.Card.code,
            metadata = PaymentMethodMetadataFactory.create(
                stripeIntent = SetupIntentFixtures.SI_REQUIRES_PAYMENT_METHOD,
            ),
        )
        assertThat(isSaveForFutureUseValueChangeable).isFalse()
    }

    @Test
    fun `isSaveForFutureUseValueChangeable returns false for PaymentIntents with SFU and null customerConfig`() {
        val isSaveForFutureUseValueChangeable = isSaveForFutureUseValueChangeable(
            code = PaymentMethod.Type.Card.code,
            metadata = PaymentMethodMetadataFactory.create(
                stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                    setupFutureUsage = StripeIntent.Usage.OnSession,
                ),
            ),
        )
        assertThat(isSaveForFutureUseValueChangeable).isFalse()
    }

    @Test
    fun `isSaveForFutureUseValueChangeable returns false for PaymentIntents with SFU and valid customerConfig`() {
        val isSaveForFutureUseValueChangeable = isSaveForFutureUseValueChangeable(
            code = PaymentMethod.Type.Card.code,
            metadata = PaymentMethodMetadataFactory.create(
                stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                    setupFutureUsage = StripeIntent.Usage.OnSession,
                ),
                hasCustomerConfiguration = true,
            ),
        )
        assertThat(isSaveForFutureUseValueChangeable).isFalse()
    }

    @Test
    fun `isSaveForFutureUseValueChangeable returns true for PaymentIntents without SFU and valid customerConfig`() {
        val isSaveForFutureUseValueChangeable = isSaveForFutureUseValueChangeable(
            code = PaymentMethod.Type.Card.code,
            metadata = PaymentMethodMetadataFactory.create(
                stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                    setupFutureUsage = null,
                ),
                hasCustomerConfiguration = true,
            ),
        )
        assertThat(isSaveForFutureUseValueChangeable).isTrue()
    }
}
