package com.stripe.android.lpmfoundations

import com.google.common.truth.Truth.assertThat
import com.stripe.android.common.model.PaymentMethodRemovePermission
import com.stripe.android.lpmfoundations.paymentmethod.CustomerMetadata
import com.stripe.android.lpmfoundations.paymentmethod.IntegrationMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodSaveConsentBehavior
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.SetupIntentFixtures
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentsheet.model.CheckoutSessionClientSecret
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponseFactory
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
    fun `isSaveForFutureUseValueChangeable returns false for PI without SFU & LPM SFU as none for legacy behavior`() {
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

        assertThat(isSaveForFutureUseValueChangeable).isFalse()
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
                saveConsent = PaymentMethodSaveConsentBehavior.Enabled,
            )
        )

        assertThat(isSaveForFutureUseValueChangeable).isTrue()
    }

    @Test
    fun `isSaveForFutureUseValueChangeable returns false for checkout session with LPM SFU as none`() {
        val isSaveForFutureUseValueChangeable = isSaveForFutureUseValueChangeable(
            code = PaymentMethod.Type.SepaDebit.code,
            metadata = createCheckoutSessionMetadata(sepaDebitSetupFutureUsage = "none"),
        )

        assertThat(isSaveForFutureUseValueChangeable).isFalse()
    }

    @Test
    fun `isSaveForFutureUseValueChangeable returns true for checkout session without LPM SFU`() {
        val isSaveForFutureUseValueChangeable = isSaveForFutureUseValueChangeable(
            code = PaymentMethod.Type.SepaDebit.code,
            metadata = createCheckoutSessionMetadata(sepaDebitSetupFutureUsage = null),
        )

        assertThat(isSaveForFutureUseValueChangeable).isTrue()
    }

    @Test
    fun `isSaveForFutureUseValueChangeable returns true for checkout session with LPM SFU as none for other LPM`() {
        val isSaveForFutureUseValueChangeable = isSaveForFutureUseValueChangeable(
            code = PaymentMethod.Type.Card.code,
            metadata = createCheckoutSessionMetadata(sepaDebitSetupFutureUsage = "none"),
        )

        assertThat(isSaveForFutureUseValueChangeable).isTrue()
    }

    @Test
    fun `isSaveForFutureUseValueChangeable returns false for PI with SFU & LPM SFU as none for legacy behavior`() {
        val isSaveForFutureUseValueChangeable = isSaveForFutureUseValueChangeable(
            code = PaymentMethod.Type.SepaDebit.code,
            intent = paymentIntent(sepaDebitSetupFutureUsage = "none").copy(
                setupFutureUsage = StripeIntent.Usage.OffSession,
            ),
            paymentMethodSaveConsentBehavior = PaymentMethodSaveConsentBehavior.Legacy,
            hasCustomerConfiguration = true,
        )

        assertThat(isSaveForFutureUseValueChangeable).isFalse()
    }

    @Test
    fun `isSaveForFutureUseValueChangeable returns false for customer session with LPM SFU as none`() {
        val isSaveForFutureUseValueChangeable = isSaveForFutureUseValueChangeable(
            code = PaymentMethod.Type.SepaDebit.code,
            metadata = PaymentMethodMetadataFactory.create(
                stripeIntent = paymentIntent(sepaDebitSetupFutureUsage = "none"),
                hasCustomerConfiguration = true,
                customerSessionClientSecret = "cuss_123",
                saveConsent = PaymentMethodSaveConsentBehavior.Enabled,
            ),
        )

        assertThat(isSaveForFutureUseValueChangeable).isFalse()
    }

    private fun createCheckoutSessionMetadata(
        sepaDebitSetupFutureUsage: String?,
    ): PaymentMethodMetadata {
        return PaymentMethodMetadataFactory.create(
            stripeIntent = paymentIntent(sepaDebitSetupFutureUsage),
            integrationMetadata = IntegrationMetadata.CheckoutSession(
                instancesKey = "test_instances_key",
                checkoutSessionResponse = CheckoutSessionResponseFactory.create(),
                collectedEmail = null,
            ),
        ).copy(
            customerMetadata = CustomerMetadata.CheckoutSession(
                clientSecret = CheckoutSessionClientSecret("cs_test_abc123_secret_test"),
                customerId = "cus_123",
                removePaymentMethod = PaymentMethodRemovePermission.Full,
                saveConsent = PaymentMethodSaveConsentBehavior.Enabled,
            ),
        )
    }

    private fun paymentIntent(sepaDebitSetupFutureUsage: String?) =
        PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
            paymentMethodTypes = listOf("card", "sepa_debit"),
            setupFutureUsage = null,
            paymentMethodOptionsJsonString = sepaDebitSetupFutureUsage?.let {
                """{"sepa_debit": {"setup_future_usage": "$it"}}"""
            },
        )
}
