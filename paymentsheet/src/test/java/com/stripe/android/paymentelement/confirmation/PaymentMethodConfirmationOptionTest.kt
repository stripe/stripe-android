package com.stripe.android.paymentelement.confirmation

import com.google.common.truth.Truth.assertThat
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFixtures
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodCreateParamsFixtures
import com.stripe.android.model.PaymentMethodExtraParams
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.model.PaymentMethodOptionsParams
import com.stripe.android.paymentelement.PaymentMethodOptionsSetupFutureUsagePreview
import com.stripe.android.paymentsheet.PaymentSheet
import org.junit.Test

@OptIn(PaymentMethodOptionsSetupFutureUsagePreview::class)
class PaymentMethodConfirmationOptionTest {

    @Test
    fun `updatedForDeferredIntent updates new option product usage and payment method options`() {
        val option = PaymentMethodConfirmationOption.New(
            createParams = PaymentMethodCreateParamsFixtures.DEFAULT_CARD.copy(
                productUsage = setOf("existing")
            ),
            optionsParams = PaymentMethodOptionsParams.Card(network = "cartes_bancaires"),
            extraParams = null,
            shouldSave = false,
        )

        val updated = option.updatedForDeferredIntent(PAYMENT_INTENT_CONFIGURATION)

        assertThat(updated.createParams.attribution)
            .containsAtLeast("existing", "deferred-intent", "autopm")
        assertThat(updated.optionsParams).isEqualTo(
            PaymentMethodOptionsParams.Card(
                network = "cartes_bancaires",
                setupFutureUsage = ConfirmPaymentIntentParams.SetupFutureUsage.OffSession,
            )
        )
        assertThat(updated.extraParams).isEqualTo(option.extraParams)
        assertThat(updated.shouldSave).isEqualTo(option.shouldSave)
    }

    @Test
    fun `updatedForDeferredIntent creates payment method options for saved option`() {
        val option = PaymentMethodConfirmationOption.Saved(
            shippingInformation = null,
            paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD,
            optionsParams = null,
        )

        val updated = option.updatedForDeferredIntent(PAYMENT_INTENT_CONFIGURATION)

        assertThat(updated.optionsParams).isEqualTo(
            PaymentMethodOptionsParams.SetupFutureUsage(
                paymentMethodType = PaymentMethod.Type.Card,
                setupFutureUsage = ConfirmPaymentIntentParams.SetupFutureUsage.OffSession,
            )
        )
        assertThat(updated.paymentMethod).isEqualTo(option.paymentMethod)
    }

    @Test
    fun `shouldSaveAsDefault returns false for Saved payment method`() {
        val option = PaymentMethodConfirmationOption.Saved(
            shippingInformation = null,
            paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD,
            optionsParams = null,
        )

        assertThat(option.shouldSaveAsDefault()).isFalse()
    }

    @Test
    fun `shouldSaveAsDefault returns false for New payment method with null extraParams`() {
        val option = PaymentMethodConfirmationOption.New(
            createParams = PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
            optionsParams = null,
            extraParams = null,
            shouldSave = false,
        )

        assertThat(option.shouldSaveAsDefault()).isFalse()
    }

    @Test
    fun `shouldSaveAsDefault returns false for BacsDebit`() {
        val option = PaymentMethodConfirmationOption.New(
            createParams = PaymentMethodCreateParams.create(
                bacsDebit = PaymentMethodCreateParams.BacsDebit(
                    accountNumber = "00012345",
                    sortCode = "10-88-00",
                ),
                billingDetails = PaymentMethodCreateParamsFixtures.BILLING_DETAILS,
            ),
            optionsParams = null,
            extraParams = PaymentMethodExtraParams.BacsDebit(confirmed = true),
            shouldSave = true,
        )

        assertThat(option.shouldSaveAsDefault()).isFalse()
    }

    @Test
    fun `shouldSaveAsDefault returns true for Card with setAsDefault true`() {
        val option = PaymentMethodConfirmationOption.New(
            createParams = PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
            optionsParams = null,
            extraParams = PaymentMethodExtraParams.Card(setAsDefault = true),
            shouldSave = true,
        )

        assertThat(option.shouldSaveAsDefault()).isTrue()
    }

    @Test
    fun `shouldSaveAsDefault returns false for Card with setAsDefault false`() {
        val option = PaymentMethodConfirmationOption.New(
            createParams = PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
            optionsParams = null,
            extraParams = PaymentMethodExtraParams.Card(setAsDefault = false),
            shouldSave = true,
        )

        assertThat(option.shouldSaveAsDefault()).isFalse()
    }

    @Test
    fun `shouldSaveAsDefault returns false for Card with setAsDefault null`() {
        val option = PaymentMethodConfirmationOption.New(
            createParams = PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
            optionsParams = null,
            extraParams = PaymentMethodExtraParams.Card(setAsDefault = null),
            shouldSave = true,
        )

        assertThat(option.shouldSaveAsDefault()).isFalse()
    }

    @Test
    fun `shouldSaveAsDefault returns true for USBankAccount with setAsDefault true`() {
        val option = PaymentMethodConfirmationOption.New(
            createParams = PaymentMethodCreateParamsFixtures.US_BANK_ACCOUNT,
            optionsParams = null,
            extraParams = PaymentMethodExtraParams.USBankAccount(setAsDefault = true),
            shouldSave = true,
        )

        assertThat(option.shouldSaveAsDefault()).isTrue()
    }

    @Test
    fun `shouldSaveAsDefault returns false for USBankAccount with setAsDefault false`() {
        val option = PaymentMethodConfirmationOption.New(
            createParams = PaymentMethodCreateParamsFixtures.US_BANK_ACCOUNT,
            optionsParams = null,
            extraParams = PaymentMethodExtraParams.USBankAccount(setAsDefault = false),
            shouldSave = true,
        )

        assertThat(option.shouldSaveAsDefault()).isFalse()
    }

    @Test
    fun `shouldSaveAsDefault returns true for Link with setAsDefault true`() {
        val option = PaymentMethodConfirmationOption.New(
            createParams = PaymentMethodCreateParams.createLink(
                paymentDetailsId = "payment_details_id",
                consumerSessionClientSecret = "consumer_secret",
                clientAttributionMetadata = PaymentMethodMetadataFixtures.CLIENT_ATTRIBUTION_METADATA,
            ),
            optionsParams = null,
            extraParams = PaymentMethodExtraParams.Link(setAsDefault = true),
            shouldSave = true,
        )

        assertThat(option.shouldSaveAsDefault()).isTrue()
    }

    @Test
    fun `shouldSaveAsDefault returns false for Link with setAsDefault false`() {
        val option = PaymentMethodConfirmationOption.New(
            createParams = PaymentMethodCreateParams.createLink(
                paymentDetailsId = "payment_details_id",
                consumerSessionClientSecret = "consumer_secret",
                clientAttributionMetadata = PaymentMethodMetadataFixtures.CLIENT_ATTRIBUTION_METADATA,
            ),
            optionsParams = null,
            extraParams = PaymentMethodExtraParams.Link(setAsDefault = false),
            shouldSave = true,
        )

        assertThat(option.shouldSaveAsDefault()).isFalse()
    }

    @Test
    fun `shouldSaveAsDefault returns true for SepaDebit with setAsDefault true`() {
        val option = PaymentMethodConfirmationOption.New(
            createParams = PaymentMethodCreateParamsFixtures.DEFAULT_SEPA_DEBIT,
            optionsParams = null,
            extraParams = PaymentMethodExtraParams.SepaDebit(setAsDefault = true),
            shouldSave = true,
        )

        assertThat(option.shouldSaveAsDefault()).isTrue()
    }

    @Test
    fun `shouldSaveAsDefault returns false for SepaDebit with setAsDefault false`() {
        val option = PaymentMethodConfirmationOption.New(
            createParams = PaymentMethodCreateParamsFixtures.DEFAULT_SEPA_DEBIT,
            optionsParams = null,
            extraParams = PaymentMethodExtraParams.SepaDebit(setAsDefault = false),
            shouldSave = true,
        )

        assertThat(option.shouldSaveAsDefault()).isFalse()
    }

    private companion object {
        val PAYMENT_INTENT_CONFIGURATION = PaymentSheet.IntentConfiguration(
            mode = PaymentSheet.IntentConfiguration.Mode.Payment(
                amount = 1_000,
                currency = "usd",
                paymentMethodOptions = PaymentSheet.IntentConfiguration.Mode.Payment.PaymentMethodOptions(
                    setupFutureUsageValues = mapOf(
                        PaymentMethod.Type.Card to PaymentSheet.IntentConfiguration.SetupFutureUse.OffSession
                    )
                )
            )
        )
    }
}
