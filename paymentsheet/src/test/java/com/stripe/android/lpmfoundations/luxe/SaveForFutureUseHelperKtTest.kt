package com.stripe.android.lpmfoundations.luxe

import com.google.common.truth.Truth.assertThat
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.SetupIntentFixtures
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentsheet.PaymentSheet
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
class SaveForFutureUseHelperKtTest {
    @Test
    fun `isSaveForFutureUseValueChangeable returns false for SetupIntents`() {
        val isSaveForFutureUseValueChangeable = isSaveForFutureUseValueChangeable(
            code = PaymentMethod.Type.Card.code,
            metadata = PaymentMethodMetadataFactory.create(
                stripeIntent = SetupIntentFixtures.SI_REQUIRES_PAYMENT_METHOD,
            ),
            customerConfiguration = null,
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
            customerConfiguration = null,
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
            ),
            customerConfiguration = PaymentSheet.CustomerConfiguration("123", "123"),
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
            ),
            customerConfiguration = PaymentSheet.CustomerConfiguration("123", "123"),
        )
        assertThat(isSaveForFutureUseValueChangeable).isTrue()
    }
}
