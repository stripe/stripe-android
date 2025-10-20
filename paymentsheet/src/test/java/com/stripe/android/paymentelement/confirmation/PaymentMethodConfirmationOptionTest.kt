package com.stripe.android.paymentelement.confirmation

import com.google.common.truth.Truth.assertThat
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFixtures
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodCreateParamsFixtures
import com.stripe.android.model.PaymentMethodExtraParams
import com.stripe.android.model.PaymentMethodFixtures
import org.junit.Test

class PaymentMethodConfirmationOptionTest {

    @Test
    fun `shouldSaveAsDefault returns false for Saved payment method`() {
        val option = PaymentMethodConfirmationOption.Saved(
            paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD,
            optionsParams = null,
            passiveCaptchaParams = null,
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
            passiveCaptchaParams = null,
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
            passiveCaptchaParams = null,
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
            passiveCaptchaParams = null,
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
            passiveCaptchaParams = null,
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
            passiveCaptchaParams = null,
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
            passiveCaptchaParams = null,
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
            passiveCaptchaParams = null,
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
            passiveCaptchaParams = null,
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
            passiveCaptchaParams = null,
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
            passiveCaptchaParams = null,
        )

        assertThat(option.shouldSaveAsDefault()).isFalse()
    }
}
