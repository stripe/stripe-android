package com.stripe.android.paymentsheet

import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.SetupIntentFixtures
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentsheet.PaymentSheet.IntentConfiguration
import com.stripe.android.testing.PaymentIntentFactory
import kotlin.test.Test
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
    fun `Fails if PaymentIntent has different setupFutureUse than IntentConfiguration`() {
        val paymentIntent = PaymentIntentFactory.create(
            setupFutureUsage = StripeIntent.Usage.OnSession,
        )
        val intentConfiguration = makeIntentConfigurationForPayment(
            setupFutureUse = IntentConfiguration.SetupFutureUse.OffSession,
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
                "the PaymentSheet.IntentConfiguration setupFutureUsage (off_session)."
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
    fun `Fails if SetupIntent has different usage than IntentConfiguration`() {
        val setupIntent = SetupIntentFixtures.SI_SUCCEEDED
        val intentConfiguration = makeIntentConfigurationForSetup(
            usage = IntentConfiguration.SetupFutureUse.OnSession,
        )

        val failure = assertFailsWith<IllegalArgumentException> {
            DeferredIntentValidator.validate(
                stripeIntent = setupIntent,
                intentConfiguration = intentConfiguration,
                allowsManualConfirmation = false,
            )
        }

        assertThat(failure).hasMessageThat().isEqualTo(
            "Your SetupIntent usage (off_session) does not match " +
                "the PaymentSheet.IntentConfiguration usage (off_session)."
        )
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
