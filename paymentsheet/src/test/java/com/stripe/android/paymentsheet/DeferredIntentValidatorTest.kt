package com.stripe.android.paymentsheet

import com.google.common.truth.Truth.assertThat
import com.stripe.android.isInstanceOf
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.SetupIntentFixtures
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentsheet.PaymentSheet.IntentConfiguration
import com.stripe.android.testing.PaymentIntentFactory
import com.stripe.android.testing.PaymentMethodFactory
import com.stripe.android.testing.PaymentMethodFactory.update
import kotlin.test.Test
import kotlin.test.assertFails
import kotlin.test.assertFailsWith

internal class DeferredIntentValidatorTest {

    @Test
    fun `Fails if PaymentIntent is validated against IntentConfiguration in setup mode`() {
        val paymentIntent = PaymentIntentFactory.create()
        val intentConfiguration = makeIntentConfigurationForSetup()

        val failure = assertFailsWith<IllegalArgumentException> {
            DeferredIntentValidator.validate(
                stripeIntent = paymentIntent,
                intentConfiguration = intentConfiguration,
                allowsManualConfirmation = false,
            )
        }

        assertThat(failure).hasMessageThat().isEqualTo(
            "You returned a PaymentIntent client secret " +
                "but used a PaymentSheet.IntentConfiguration in setup mode."
        )
    }

    @Test
    fun `Fails if PaymentIntent has different currency than IntentConfiguration`() {
        val paymentIntent = PaymentIntentFactory.create()
        val intentConfiguration = makeIntentConfigurationForPayment(currency = "eur")

        val failure = assertFailsWith<IllegalArgumentException> {
            DeferredIntentValidator.validate(
                stripeIntent = paymentIntent,
                intentConfiguration = intentConfiguration,
                allowsManualConfirmation = false,
            )
        }

        assertThat(failure).hasMessageThat().isEqualTo(
            "Your PaymentIntent currency (usd) does not match " +
                "the PaymentSheet.IntentConfiguration currency (eur)."
        )
    }

    @Test
    fun `Succeeds if PaymentIntent has different setupFutureUse than IntentConfiguration`() {
        val paymentIntent = PaymentIntentFactory.create(
            setupFutureUsage = StripeIntent.Usage.OnSession,
        )
        val intentConfiguration = makeIntentConfigurationForPayment(
            setupFutureUse = IntentConfiguration.SetupFutureUse.OffSession,
        )

        assertThat(
            DeferredIntentValidator.validate(
                stripeIntent = paymentIntent,
                intentConfiguration = intentConfiguration,
                allowsManualConfirmation = false,
            )
        ).isEqualTo(paymentIntent)
    }

    @Test
    fun `Fails if PaymentIntent has setupFutureUse and IntentConfiguration was not set`() {
        val paymentIntent = PaymentIntentFactory.create(
            setupFutureUsage = StripeIntent.Usage.OnSession,
        )
        val intentConfiguration = makeIntentConfigurationForPayment(
            setupFutureUse = null,
        )

        val failure = assertFailsWith<IllegalArgumentException> {
            DeferredIntentValidator.validate(
                stripeIntent = paymentIntent,
                intentConfiguration = intentConfiguration,
                allowsManualConfirmation = false,
            )
        }

        assertThat(failure).hasMessageThat().isEqualTo(
            "Your PaymentIntent setupFutureUsage (on_session) does not match " +
                "the PaymentSheet.IntentConfiguration setupFutureUsage (null)."
        )
    }

    @Test
    fun `Succeeds if PaymentIntent has different captureMethod than IntentConfiguration`() {
        val paymentIntent = PaymentIntentFactory.create()
        val intentConfiguration = makeIntentConfigurationForPayment(
            captureMethod = IntentConfiguration.CaptureMethod.Manual,
        )

        val result = DeferredIntentValidator.validate(
            stripeIntent = paymentIntent,
            intentConfiguration = intentConfiguration,
            allowsManualConfirmation = false,
        )

        assertThat(result).isEqualTo(paymentIntent)
    }

    @Test
    fun `Fails if PaymentIntent has manual confirmation method outside of FlowController`() {
        val paymentIntent = PaymentIntentFactory.create(
            confirmationMethod = PaymentIntent.ConfirmationMethod.Manual,
        )

        val intentConfiguration = makeIntentConfigurationForPayment()

        val failure = assertFailsWith<IllegalArgumentException> {
            DeferredIntentValidator.validate(
                stripeIntent = paymentIntent,
                intentConfiguration = intentConfiguration,
                allowsManualConfirmation = false,
            )
        }

        assertThat(failure).hasMessageThat().isEqualTo(
            "Your PaymentIntent confirmationMethod (Manual) can only " +
                "be used with PaymentSheet.FlowController."
        )
    }

    @Test
    fun `Succeeds if PaymentIntent has manual confirmation method inside of FlowController`() {
        val paymentIntent = PaymentIntentFactory.create(
            confirmationMethod = PaymentIntent.ConfirmationMethod.Manual,
        )

        val intentConfiguration = makeIntentConfigurationForPayment()

        val result = DeferredIntentValidator.validate(
            stripeIntent = paymentIntent,
            intentConfiguration = intentConfiguration,
            allowsManualConfirmation = true,
        )

        assertThat(result).isEqualTo(paymentIntent)
    }

    @Test
    fun `Succeeds if PaymentIntent and IntentConfiguration match`() {
        val paymentIntent = PaymentIntentFactory.create()
        val intentConfiguration = makeIntentConfigurationForPayment()

        val result = DeferredIntentValidator.validate(
            stripeIntent = paymentIntent,
            intentConfiguration = intentConfiguration,
            allowsManualConfirmation = true,
        )

        assertThat(result).isEqualTo(paymentIntent)
    }

    @Test
    fun `Succeeds if PaymentIntent and IntentConfiguration have same currency but in different case`() {
        val paymentIntent = PaymentIntentFactory.create()
        val intentConfiguration = makeIntentConfigurationForPayment(
            currency = "USD", // uppercase to test case insensitivity
        )

        val result = DeferredIntentValidator.validate(
            stripeIntent = paymentIntent,
            intentConfiguration = intentConfiguration,
            allowsManualConfirmation = true,
        )

        assertThat(result).isEqualTo(paymentIntent)
    }

    @Test
    fun `Fails if SetupIntent is validated against IntentConfiguration in payment mode`() {
        val setupIntent = SetupIntentFixtures.SI_SUCCEEDED
        val intentConfiguration = makeIntentConfigurationForPayment()

        val failure = assertFailsWith<IllegalArgumentException> {
            DeferredIntentValidator.validate(
                stripeIntent = setupIntent,
                intentConfiguration = intentConfiguration,
                allowsManualConfirmation = false,
            )
        }

        assertThat(failure).hasMessageThat().isEqualTo(
            "You returned a SetupIntent client secret " +
                "but used a PaymentSheet.IntentConfiguration in payment mode."
        )
    }

    @Test
    fun `Succeeds if SetupIntent has different usage than IntentConfiguration`() {
        val setupIntent = SetupIntentFixtures.SI_SUCCEEDED.copy(
            usage = StripeIntent.Usage.OnSession,
        )
        val intentConfiguration = makeIntentConfigurationForSetup(
            usage = IntentConfiguration.SetupFutureUse.OffSession,
        )

        assertThat(
            DeferredIntentValidator.validate(
                stripeIntent = setupIntent,
                intentConfiguration = intentConfiguration,
                allowsManualConfirmation = false,
            )
        ).isEqualTo(setupIntent)
    }

    @Test
    fun `Succeeds if SetupIntent and IntentConfiguration match`() {
        val setupIntent = SetupIntentFixtures.SI_SUCCEEDED
        val intentConfiguration = makeIntentConfigurationForSetup()

        val result = DeferredIntentValidator.validate(
            stripeIntent = setupIntent,
            intentConfiguration = intentConfiguration,
            allowsManualConfirmation = true,
        )

        assertThat(result).isEqualTo(setupIntent)
    }

    @Test
    fun `PM validation succeeds if no PM attached to intent`() {
        val providedCard = PaymentMethodFactory.card(id = "pm_1")

        val result = DeferredIntentValidator.validatePaymentMethod(
            intent = PaymentIntentFactory.create(paymentMethod = null),
            paymentMethod = providedCard,
        )

        assertThat(result).isNotNull()
    }

    @Test
    fun `PM validation fails if PMs are different types`() {
        val providedCard = PaymentMethodFactory.card(random = true)
        val attachedUsBankAccount = PaymentMethodFactory.usBankAccount()

        val exception = assertFails {
            DeferredIntentValidator.validatePaymentMethod(
                intent = PaymentIntentFactory.create(attachedUsBankAccount),
                paymentMethod = providedCard,
            )
        }

        assertThat(exception).isInstanceOf<java.lang.IllegalArgumentException>()
        assertThat(exception.message).isEqualTo(
            "Your payment method (${attachedUsBankAccount.id}) attached to the intent does not " +
                "match the provided payment method (${providedCard.id})!"
        )
    }

    @Test
    fun `Card validation succeeds when fingerprints are the same`() = sameFingerprintTest { id, fingerprint ->
        PaymentMethodFactory.card(id = id)
            .update(
                last4 = "4242",
                addCbcNetworks = false,
                fingerprint = fingerprint
            )
    }

    @Test
    fun `Card validation fails when IDs & fingerprints are different`() =
        differentFingerprintTest { id, fingerprint ->
            PaymentMethodFactory.card(id = id)
                .update(
                    last4 = "4242",
                    addCbcNetworks = false,
                    fingerprint = fingerprint,
                )
        }

    @Test
    fun `US Bank account validation succeeds when fingerprints are the same`() =
        sameFingerprintTest { id, fingerprint ->
            PaymentMethodFactory.usBankAccount().run {
                copy(id = id, usBankAccount = usBankAccount?.copy(fingerprint = fingerprint))
            }
        }

    @Test
    fun `US Bank account validation fails when IDs & fingerprints are different`() =
        differentFingerprintTest { id, fingerprint ->
            PaymentMethodFactory.usBankAccount().run {
                copy(id = id, usBankAccount = usBankAccount?.copy(fingerprint = fingerprint))
            }
        }

    @Test
    fun `Au Becs Debit account validation succeeds when fingerprints are the same`() =
        sameFingerprintTest { id, fingerprint ->
            PaymentMethodFactory.auBecsDebit().run {
                copy(id = id, auBecsDebit = auBecsDebit?.copy(fingerprint = fingerprint))
            }
        }

    @Test
    fun `Au Becs Debit account validation fails when IDs & fingerprints are different`() =
        differentFingerprintTest { id, fingerprint ->
            PaymentMethodFactory.auBecsDebit().run {
                copy(id = id, auBecsDebit = auBecsDebit?.copy(fingerprint = fingerprint))
            }
        }

    @Test
    fun `Bacs Debit account validation succeeds when fingerprints are the same`() =
        sameFingerprintTest { id, fingerprint ->
            PaymentMethodFactory.bacs().run {
                copy(id = id, bacsDebit = bacsDebit?.copy(fingerprint = fingerprint))
            }
        }

    @Test
    fun `Bacs Debit account validation fails when IDs & fingerprints are different`() =
        differentFingerprintTest { id, fingerprint ->
            PaymentMethodFactory.bacs().run {
                copy(id = id, bacsDebit = bacsDebit?.copy(fingerprint = fingerprint))
            }
        }

    @Test
    fun `Sepa Debit account validation succeeds when fingerprints are the same`() =
        sameFingerprintTest { id, fingerprint ->
            PaymentMethodFactory.sepaDebit().run {
                copy(id = id, sepaDebit = sepaDebit?.copy(fingerprint = fingerprint))
            }
        }

    @Test
    fun `Sepa Debit account validation fails when IDs & fingerprints are different`() =
        differentFingerprintTest { id, fingerprint ->
            PaymentMethodFactory.sepaDebit().run {
                copy(id = id, sepaDebit = sepaDebit?.copy(fingerprint = fingerprint))
            }
        }

    private fun sameFingerprintTest(
        createPaymentMethod: (id: String, fingerprint: String) -> PaymentMethod,
    ) {
        val attachedPaymentMethod = createPaymentMethod("pm_1", "fingerprint")
        val providedPaymentMethod = createPaymentMethod("pm_2", "fingerprint")

        val result = DeferredIntentValidator.validatePaymentMethod(
            intent = PaymentIntentFactory.create(attachedPaymentMethod),
            paymentMethod = providedPaymentMethod,
        )

        assertThat(result).isNotNull()
    }

    private fun differentFingerprintTest(
        createPaymentMethod: (id: String, fingerprint: String) -> PaymentMethod,
    ) {
        val attachedPaymentMethod = createPaymentMethod("pm_1", "fingerprint1")
        val providedPaymentMethod = createPaymentMethod("pm_2", "fingerprint2")

        val exception = assertFails {
            DeferredIntentValidator.validatePaymentMethod(
                intent = PaymentIntentFactory.create(attachedPaymentMethod),
                paymentMethod = providedPaymentMethod,
            )
        }

        assertThat(exception).isInstanceOf<java.lang.IllegalArgumentException>()
        assertThat(exception.message).isEqualTo(
            "Your payment method (${attachedPaymentMethod.id}) attached to the intent does not " +
                "match the provided payment method (${providedPaymentMethod.id})!"
        )
    }

    private fun makeIntentConfigurationForPayment(
        currency: String = "usd",
        setupFutureUse: IntentConfiguration.SetupFutureUse? = null,
        captureMethod: IntentConfiguration.CaptureMethod = IntentConfiguration.CaptureMethod.Automatic,
    ): IntentConfiguration {
        return IntentConfiguration(
            mode = IntentConfiguration.Mode.Payment(
                amount = 1_000,
                currency = currency,
                setupFutureUse = setupFutureUse,
                captureMethod = captureMethod,
            ),
        )
    }

    private fun makeIntentConfigurationForSetup(
        usage: IntentConfiguration.SetupFutureUse = IntentConfiguration.SetupFutureUse.OffSession,
    ): IntentConfiguration {
        return IntentConfiguration(
            mode = IntentConfiguration.Mode.Setup(
                setupFutureUse = usage,
            ),
        )
    }
}
