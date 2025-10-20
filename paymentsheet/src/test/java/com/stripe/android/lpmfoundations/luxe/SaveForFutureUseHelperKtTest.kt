package com.stripe.android.lpmfoundations.luxe

import com.google.common.truth.Truth.assertThat
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodSaveConsentBehavior
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.SetupIntentFixtures
import com.stripe.android.model.StripeIntent
import kotlin.test.Test

class SaveForFutureUseHelperKtTest {
    @Test
    fun `isSaveForFutureUseValueChangeable returns false for SI if behavior is Legacy`() {
        val isSaveForFutureUseValueChangeable = isSaveForFutureUseValueChangeable(
            code = PaymentMethod.Type.Card.code,
            intent = SetupIntentFixtures.SI_REQUIRES_PAYMENT_METHOD,
            paymentMethodSaveConsentBehavior = PaymentMethodSaveConsentBehavior.Legacy,
            hasCustomerConfiguration = true,
        )

        assertThat(isSaveForFutureUseValueChangeable).isFalse()
    }

    @Test
    fun `isSaveForFutureUseValueChangeable returns false for PI with SFU and no customer if behavior is Legacy`() {
        val isSaveForFutureUseValueChangeable = isSaveForFutureUseValueChangeable(
            code = PaymentMethod.Type.Card.code,
            intent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                setupFutureUsage = StripeIntent.Usage.OnSession,
            ),
            paymentMethodSaveConsentBehavior = PaymentMethodSaveConsentBehavior.Legacy,
            hasCustomerConfiguration = false,
        )

        assertThat(isSaveForFutureUseValueChangeable).isFalse()
    }

    @Test
    fun `isSaveForFutureUseValueChangeable returns false for PI with SFU and has customer if behavior is Legacy`() {
        val isSaveForFutureUseValueChangeable = isSaveForFutureUseValueChangeable(
            code = PaymentMethod.Type.Card.code,
            intent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                setupFutureUsage = StripeIntent.Usage.OnSession,
            ),
            paymentMethodSaveConsentBehavior = PaymentMethodSaveConsentBehavior.Legacy,
            hasCustomerConfiguration = true,
        )

        assertThat(isSaveForFutureUseValueChangeable).isFalse()
    }

    @Test
    fun `isSaveForFutureUseValueChangeable returns true for PI without SFU and has customer if behavior is Legacy`() {
        val isSaveForFutureUseValueChangeable = isSaveForFutureUseValueChangeable(
            code = PaymentMethod.Type.Card.code,
            intent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                setupFutureUsage = null,
            ),
            paymentMethodSaveConsentBehavior = PaymentMethodSaveConsentBehavior.Legacy,
            hasCustomerConfiguration = true,
        )

        assertThat(isSaveForFutureUseValueChangeable).isTrue()
    }

    @Test
    fun `isSaveForFutureUseValueChangeable returns false for PI without SFU & LPM SFU set for legacy behavior`() {
        val isSaveForFutureUseValueChangeable = isSaveForFutureUseValueChangeable(
            code = PaymentMethod.Type.Card.code,
            intent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                setupFutureUsage = null,
                paymentMethodOptionsJsonString = """
                    {
                        "card": {
                            "setup_future_usage": "off_session"
                        }
                    }
                """.trimIndent()
            ),
            paymentMethodSaveConsentBehavior = PaymentMethodSaveConsentBehavior.Legacy,
            hasCustomerConfiguration = true,
        )

        assertThat(isSaveForFutureUseValueChangeable).isFalse()
    }

    @Test
    fun `isSaveForFutureUseValueChangeable returns true for PI without SFU & LPM SFU as none for legacy behavior`() {
        val isSaveForFutureUseValueChangeable = isSaveForFutureUseValueChangeable(
            code = PaymentMethod.Type.Card.code,
            intent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                setupFutureUsage = null,
                paymentMethodOptionsJsonString = """
                    {
                        "card": {
                            "setup_future_usage": "none"
                        }
                    }
                """.trimIndent()
            ),
            paymentMethodSaveConsentBehavior = PaymentMethodSaveConsentBehavior.Legacy,
            hasCustomerConfiguration = true,
        )

        assertThat(isSaveForFutureUseValueChangeable).isTrue()
    }

    @Test
    fun `isSaveForFutureUseValueChangeable returns true if consent behavior is Enabled and has customer`() {
        val isSaveForFutureUseValueChangeable = isSaveForFutureUseValueChangeable(
            code = PaymentMethod.Type.Card.code,
            intent = SetupIntentFixtures.SI_REQUIRES_PAYMENT_METHOD,
            hasCustomerConfiguration = true,
            paymentMethodSaveConsentBehavior = PaymentMethodSaveConsentBehavior.Enabled,
        )

        assertThat(isSaveForFutureUseValueChangeable).isTrue()
    }

    @Test
    fun `isSaveForFutureUseValueChangeable returns false if consent behavior is Enabled and no customer`() {
        val isSaveForFutureUseValueChangeable = isSaveForFutureUseValueChangeable(
            code = PaymentMethod.Type.Card.code,
            intent = SetupIntentFixtures.SI_REQUIRES_PAYMENT_METHOD,
            hasCustomerConfiguration = false,
            paymentMethodSaveConsentBehavior = PaymentMethodSaveConsentBehavior.Enabled,
        )

        assertThat(isSaveForFutureUseValueChangeable).isFalse()
    }

    @Test
    fun `isSaveForFutureUseValueChangeable returns false if consent behavior is Disabled`() {
        val isSaveForFutureUseValueChangeable = isSaveForFutureUseValueChangeable(
            code = PaymentMethod.Type.Card.code,
            intent = SetupIntentFixtures.SI_REQUIRES_PAYMENT_METHOD,
            hasCustomerConfiguration = true,
            paymentMethodSaveConsentBehavior = PaymentMethodSaveConsentBehavior.Disabled(
                overrideAllowRedisplay = null,
            ),
        )

        assertThat(isSaveForFutureUseValueChangeable).isFalse()
    }

    @Test
    fun `isSaveForFutureUseValueChangeable with metadata works as expected`() {
        val isSaveForFutureUseValueChangeable = isSaveForFutureUseValueChangeable(
            code = PaymentMethod.Type.Card.code,
            metadata = PaymentMethodMetadataFactory.create(
                stripeIntent = SetupIntentFixtures.SI_REQUIRES_PAYMENT_METHOD,
                hasCustomerConfiguration = true,
                paymentMethodSaveConsentBehavior = PaymentMethodSaveConsentBehavior.Enabled,
            )
        )

        assertThat(isSaveForFutureUseValueChangeable).isTrue()
    }
}
