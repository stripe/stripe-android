package com.stripe.android.paymentelement.embedded

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethodCreateParamsFixtures
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.ExperimentalEmbeddedPaymentElementApi
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.testing.PaymentMethodFactory
import org.junit.Test
import com.stripe.android.ui.core.R as StripeUiCoreR

@OptIn(ExperimentalEmbeddedPaymentElementApi::class)
internal class DefaultEmbeddedSelectionChooserTest {
    @Test
    fun `Uses new payment selection if there's no existing one`() = runScenario {
        val selection = chooser.choose(
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(isGooglePayReady = true),
            paymentMethods = null,
            previousSelection = null,
            newSelection = PaymentSelection.GooglePay,
            previousConfiguration = null,
            newConfiguration = EmbeddedPaymentElement.Configuration.Builder("Example, Inc.").build(),
        )
        assertThat(selection).isEqualTo(PaymentSelection.GooglePay)
    }

    @Test
    fun `Can use existing payment selection if it's still supported`() = runScenario {
        val previousSelection = PaymentSelection.New.GenericPaymentMethod(
            label = "Sofort".resolvableString,
            iconResource = StripeUiCoreR.drawable.stripe_ic_paymentsheet_pm_klarna,
            lightThemeIconUrl = null,
            darkThemeIconUrl = null,
            paymentMethodCreateParams = PaymentMethodCreateParamsFixtures.SOFORT,
            customerRequestedSave = PaymentSelection.CustomerRequestedSave.NoRequest,
        )

        val paymentMethod = PaymentMethodFixtures.createCard()
        val newSelection = PaymentSelection.Saved(paymentMethod)
        val selection = chooser.choose(
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                    paymentMethodTypes = listOf("card", "sofort")
                )
            ),
            paymentMethods = PaymentMethodFixtures.createCards(3) + paymentMethod,
            previousSelection = previousSelection,
            newSelection = newSelection,
            previousConfiguration = null,
            newConfiguration = EmbeddedPaymentElement.Configuration.Builder("Example, Inc.").build(),
        )
        assertThat(selection).isEqualTo(previousSelection)
    }

    @Test
    fun `Can use existing saved payment selection if it's still supported`() = runScenario {
        val paymentMethod = PaymentMethodFixtures.createCard()
        val previousSelection = PaymentSelection.Saved(paymentMethod)

        val selection = chooser.choose(
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(isGooglePayReady = true),
            paymentMethods = PaymentMethodFixtures.createCards(3) + paymentMethod,
            previousSelection = previousSelection,
            newSelection = PaymentSelection.GooglePay,
            previousConfiguration = null,
            newConfiguration = EmbeddedPaymentElement.Configuration.Builder("Example, Inc.").build(),
        )
        assertThat(selection).isEqualTo(previousSelection)
    }

    @Test
    fun `Can't use existing saved payment method if it's no longer allowed`() = runScenario {
        val previousSelection = PaymentSelection.Saved(PaymentMethodFixtures.SEPA_DEBIT_PAYMENT_METHOD)

        val selection = chooser.choose(
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(),
            paymentMethods = PaymentMethodFixtures.createCards(3) + PaymentMethodFixtures.SEPA_DEBIT_PAYMENT_METHOD,
            previousSelection = previousSelection,
            newSelection = null,
            previousConfiguration = null,
            newConfiguration = EmbeddedPaymentElement.Configuration.Builder("Example, Inc.").build(),
        )
        assertThat(selection).isNull()
    }

    @Test
    fun `Can't use existing saved payment method if it's no longer available`() = runScenario {
        val previousSelection = PaymentSelection.Saved(PaymentMethodFactory.cashAppPay())

        val selection = chooser.choose(
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                    paymentMethodTypes = listOf("card", "cashapp")
                )
            ),
            paymentMethods = PaymentMethodFixtures.createCards(3),
            previousSelection = previousSelection,
            newSelection = null,
            previousConfiguration = null,
            newConfiguration = EmbeddedPaymentElement.Configuration.Builder("Example, Inc.").build(),
        )
        assertThat(selection).isNull()
    }

    @Test
    fun `Can use external payment method if it's supported`() = runScenario {
        val previousSelection =
            PaymentMethodFixtures.createExternalPaymentMethod(PaymentMethodFixtures.PAYPAL_EXTERNAL_PAYMENT_METHOD_SPEC)

        val selection = chooser.choose(
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                externalPaymentMethodSpecs = listOf(PaymentMethodFixtures.PAYPAL_EXTERNAL_PAYMENT_METHOD_SPEC),
                isGooglePayReady = true,
            ),
            paymentMethods = PaymentMethodFixtures.createCards(3),
            previousSelection = previousSelection,
            newSelection = PaymentSelection.GooglePay,
            previousConfiguration = null,
            newConfiguration = EmbeddedPaymentElement.Configuration.Builder("Example, Inc.").build(),
        )
        assertThat(selection).isEqualTo(previousSelection)
    }

    @Test
    fun `Can't use external payment method if it's not returned by backend`() = runScenario {
        val previousSelection =
            PaymentMethodFixtures.createExternalPaymentMethod(PaymentMethodFixtures.PAYPAL_EXTERNAL_PAYMENT_METHOD_SPEC)

        val selection = chooser.choose(
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                externalPaymentMethodSpecs = listOf(),
            ),
            paymentMethods = PaymentMethodFixtures.createCards(3),
            previousSelection = previousSelection,
            newSelection = null,
            previousConfiguration = null,
            newConfiguration = EmbeddedPaymentElement.Configuration.Builder("Example, Inc.").build(),
        )
        assertThat(selection).isNull()
    }

    @Test
    fun `PaymentSelection is preserved when config changes are not volatile`() = runScenario {
        val previousSelection = PaymentSelection.GooglePay
        val paymentMethod = PaymentMethodFixtures.createCard()
        val newSelection = PaymentSelection.Saved(paymentMethod)

        val selection = chooser.choose(
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(isGooglePayReady = true),
            paymentMethods = PaymentMethodFixtures.createCards(3) + paymentMethod,
            previousSelection = previousSelection,
            newSelection = newSelection,
            previousConfiguration = EmbeddedPaymentElement.Configuration.Builder("Example, Inc.")
                .embeddedViewDisplaysMandateText(false).build(),
            newConfiguration = EmbeddedPaymentElement.Configuration.Builder("Example, Inc.").build(),
        )
        assertThat(selection).isEqualTo(previousSelection)
    }

    @Test
    fun `PaymentSelection is not preserved when config changes are volatile`() = runScenario {
        val previousSelection = PaymentSelection.GooglePay
        val paymentMethod = PaymentMethodFixtures.createCard()
        val newSelection = PaymentSelection.Saved(paymentMethod)

        val selection = chooser.choose(
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(isGooglePayReady = true),
            paymentMethods = PaymentMethodFixtures.createCards(3) + paymentMethod,
            previousSelection = previousSelection,
            newSelection = newSelection,
            previousConfiguration = EmbeddedPaymentElement.Configuration.Builder("Example, Inc.")
                .defaultBillingDetails(PaymentSheet.BillingDetails(email = "jaynewstrom@example.com")).build(),
            newConfiguration = EmbeddedPaymentElement.Configuration.Builder("Example, Inc.").build(),
        )
        assertThat(selection).isEqualTo(newSelection)
    }

    private fun runScenario(
        block: Scenario.() -> Unit,
    ) {
        Scenario(DefaultEmbeddedSelectionChooser()).block()
    }

    private class Scenario(
        val chooser: EmbeddedSelectionChooser,
    )
}
