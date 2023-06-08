package com.stripe.android.paymentsheet

import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.SetupIntentFixtures
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentsheet.PaymentSheet.IntentConfiguration
import com.stripe.android.testing.PaymentIntentFactory
import org.junit.Assert.assertThrows
import kotlin.test.Test

@OptIn(ExperimentalPaymentSheetDecouplingApi::class)
internal class DeferredIntentValidatorTest {

    @Test
    fun `Fails if PaymentIntent is validated against IntentConfiguration in setup mode`() {
        val paymentIntent = PaymentIntentFactory.create()
        val intentConfiguration = makeIntentConfigurationForSetup()

        val failure = assertThrows(IllegalArgumentException::class.java) {
            DeferredIntentValidator.validate(
                stripeIntent = paymentIntent,
                intentConfiguration = intentConfiguration,
                isFlowController = false,
            )
        }

        assertThat(failure).isInstanceOf(IllegalArgumentException::class.java)

        assertThat(failure).hasMessageThat().isEqualTo(
            "You returned a PaymentIntent client secret " +
                "but used a PaymentSheet.IntentConfiguration in setup mode."
        )
    }

    @Test
    fun `Fails if PaymentIntent has different amount than IntentConfiguration`() {
        val paymentIntent = PaymentIntentFactory.create()
        val intentConfiguration = makeIntentConfigurationForPayment(amount = 4321L)

        val failure = assertThrows(IllegalArgumentException::class.java) {
            DeferredIntentValidator.validate(
                stripeIntent = paymentIntent,
                intentConfiguration = intentConfiguration,
                isFlowController = false,
            )
        }

        assertThat(failure).isInstanceOf(IllegalArgumentException::class.java)

        assertThat(failure).hasMessageThat().isEqualTo(
            "Your PaymentIntent amount (1000) does not match " +
                "the PaymentSheet.IntentConfiguration amount (4321)."
        )
    }

    @Test
    fun `Fails if PaymentIntent has different currency than IntentConfiguration`() {
        val paymentIntent = PaymentIntentFactory.create()
        val intentConfiguration = makeIntentConfigurationForPayment(currency = "eur")

        val failure = assertThrows(IllegalArgumentException::class.java) {
            DeferredIntentValidator.validate(
                stripeIntent = paymentIntent,
                intentConfiguration = intentConfiguration,
                isFlowController = false,
            )
        }

        assertThat(failure).isInstanceOf(IllegalArgumentException::class.java)

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

        val failure = assertThrows(IllegalArgumentException::class.java) {
            DeferredIntentValidator.validate(
                stripeIntent = paymentIntent,
                intentConfiguration = intentConfiguration,
                isFlowController = false,
            )
        }

        assertThat(failure).isInstanceOf(IllegalArgumentException::class.java)

        assertThat(failure).hasMessageThat().isEqualTo(
            "Your PaymentIntent setupFutureUsage (on_session) does not match " +
                "the PaymentSheet.IntentConfiguration setupFutureUsage (off_session)."
        )
    }

    @Test
    fun `Fails if PaymentIntent has different captureMethod than IntentConfiguration`() {
        val paymentIntent = PaymentIntentFactory.create()
        val intentConfiguration = makeIntentConfigurationForPayment(
            captureMethod = IntentConfiguration.CaptureMethod.Manual,
        )

        val failure = assertThrows(IllegalArgumentException::class.java) {
            DeferredIntentValidator.validate(
                stripeIntent = paymentIntent,
                intentConfiguration = intentConfiguration,
                isFlowController = false,
            )
        }

        assertThat(failure).isInstanceOf(IllegalArgumentException::class.java)

        assertThat(failure).hasMessageThat().isEqualTo(
            "Your PaymentIntent captureMethod (Automatic) does not match " +
                "the PaymentSheet.IntentConfiguration captureMethod (Manual)."
        )
    }

    @Test
    fun `Fails if PaymentIntent has manual capture method outside of FlowController`() {
        val paymentIntent = PaymentIntentFactory.create()
        val intentConfiguration = makeIntentConfigurationForPayment(
            captureMethod = IntentConfiguration.CaptureMethod.Manual,
        )

        val failure = assertThrows(IllegalArgumentException::class.java) {
            DeferredIntentValidator.validate(
                stripeIntent = paymentIntent,
                intentConfiguration = intentConfiguration,
                isFlowController = true,
            )
        }

        assertThat(failure).isInstanceOf(IllegalArgumentException::class.java)

        assertThat(failure).hasMessageThat().isEqualTo(
            "Your PaymentIntent captureMethod (Automatic) does not match " +
                "the PaymentSheet.IntentConfiguration captureMethod (Manual)."
        )
    }

    @Test
    fun `Succeeds if PaymentIntent and IntentConfiguration match`() {
        val paymentIntent = PaymentIntentFactory.create()
        val intentConfiguration = makeIntentConfigurationForPayment()

        val result = DeferredIntentValidator.validate(
            stripeIntent = paymentIntent,
            intentConfiguration = intentConfiguration,
            isFlowController = true,
        )

        assertThat(result).isEqualTo(paymentIntent)
    }

    @Test
    fun `Fails if SetupIntent is validated against IntentConfiguration in payment mode`() {
        val paymentIntent = SetupIntentFixtures.SI_SUCCEEDED
        val intentConfiguration = makeIntentConfigurationForPayment()

        val failure = assertThrows(IllegalArgumentException::class.java) {
            DeferredIntentValidator.validate(
                stripeIntent = paymentIntent,
                intentConfiguration = intentConfiguration,
                isFlowController = false,
            )
        }

        assertThat(failure).isInstanceOf(IllegalArgumentException::class.java)

        assertThat(failure).hasMessageThat().isEqualTo(
            "You returned a SetupIntent client secret " +
                "but used a PaymentSheet.IntentConfiguration in payment mode."
        )
    }

    @Test
    fun `Fails if SetupIntent has different usage than IntentConfiguration`() {
        val paymentIntent = SetupIntentFixtures.SI_SUCCEEDED
        val intentConfiguration = makeIntentConfigurationForSetup(
            usage = IntentConfiguration.SetupFutureUse.OnSession,
        )

        val failure = assertThrows(IllegalArgumentException::class.java) {
            DeferredIntentValidator.validate(
                stripeIntent = paymentIntent,
                intentConfiguration = intentConfiguration,
                isFlowController = false,
            )
        }

        assertThat(failure).isInstanceOf(IllegalArgumentException::class.java)

        assertThat(failure).hasMessageThat().isEqualTo(
            "Your SetupIntent usage (off_session) does not match " +
                "the PaymentSheet.IntentConfiguration usage (off_session)."
        )
    }

    @Test
    fun `Succeeds if SetupIntent and IntentConfiguration match`() {
        val paymentIntent = SetupIntentFixtures.SI_SUCCEEDED
        val intentConfiguration = makeIntentConfigurationForSetup()

        val result = DeferredIntentValidator.validate(
            stripeIntent = paymentIntent,
            intentConfiguration = intentConfiguration,
            isFlowController = true,
        )

        assertThat(result).isEqualTo(paymentIntent)
    }

    private fun makeIntentConfigurationForPayment(
        amount: Long = 1000L,
        currency: String = "usd",
        setupFutureUse: IntentConfiguration.SetupFutureUse? = null,
        captureMethod: IntentConfiguration.CaptureMethod = IntentConfiguration.CaptureMethod.Automatic,
    ): IntentConfiguration {
        return IntentConfiguration(
            mode = IntentConfiguration.Mode.Payment(
                amount = amount,
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
