package com.stripe.android.paymentsheet.paymentdatacollection.ach

import com.google.common.truth.Truth.assertThat
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodSaveConsentBehavior
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.PaymentSheet.BillingDetails
import com.stripe.android.paymentsheet.PaymentSheet.BillingDetailsCollectionConfiguration
import com.stripe.android.paymentsheet.paymentdatacollection.FormArguments
import com.stripe.android.ui.core.cbc.CardBrandChoiceEligibility
import org.junit.Test

class BankFormFieldsStateTest {

    @Test
    fun `Collects name in Instant Debits if requested`() {
        val formArgs = createFormArguments(
            billingDetailsCollectionConfiguration = BillingDetailsCollectionConfiguration(
                name = BillingDetailsCollectionConfiguration.CollectionMode.Always,
            ),
        )

        val formFieldsState = BankFormFieldsState(
            formArgs = formArgs,
            instantDebits = true,
        )

        assertThat(formFieldsState.showNameField).isTrue()
    }

    @Test
    fun `Hides name field in Instant Debits by default`() {
        val formArgs = createFormArguments(
            billingDetailsCollectionConfiguration = BillingDetailsCollectionConfiguration(),
        )

        val formFieldsState = BankFormFieldsState(
            formArgs = formArgs,
            instantDebits = true,
        )

        assertThat(formFieldsState.showNameField).isFalse()
    }

    @Test
    fun `Collects email in Instant Debits by default`() {
        val formArgs = createFormArguments(
            billingDetailsCollectionConfiguration = BillingDetailsCollectionConfiguration(),
        )

        val formFieldsState = BankFormFieldsState(
            formArgs = formArgs,
            instantDebits = true,
        )

        assertThat(formFieldsState.showEmailField).isTrue()
    }

    @Test
    fun `Collects email in Instant Debits even if requested to hide because of missing default email`() {
        val formArgs = createFormArguments(
            billingDetailsCollectionConfiguration = BillingDetailsCollectionConfiguration(
                email = BillingDetailsCollectionConfiguration.CollectionMode.Never,
                attachDefaultsToPaymentMethod = true,
            ),
            billingDetails = BillingDetails(
                email = "",
            )
        )

        val formFieldsState = BankFormFieldsState(
            formArgs = formArgs,
            instantDebits = true,
        )

        assertThat(formFieldsState.showEmailField).isTrue()
    }

    @Test
    fun `Collects name in US Bank Account even if requested to hide because of missing default name`() {
        val formArgs = createFormArguments(
            billingDetailsCollectionConfiguration = BillingDetailsCollectionConfiguration(
                name = BillingDetailsCollectionConfiguration.CollectionMode.Never,
                attachDefaultsToPaymentMethod = true,
            ),
            billingDetails = BillingDetails(
                name = "",
            )
        )

        val formFieldsState = BankFormFieldsState(
            formArgs = formArgs,
            instantDebits = false,
        )

        assertThat(formFieldsState.showNameField).isTrue()
    }

    @Test
    fun `Collects email in US Bank Account even if requested to hide because of missing default email`() {
        val formArgs = createFormArguments(
            billingDetailsCollectionConfiguration = BillingDetailsCollectionConfiguration(
                email = BillingDetailsCollectionConfiguration.CollectionMode.Never,
                attachDefaultsToPaymentMethod = true,
            ),
            billingDetails = BillingDetails(
                email = "",
            )
        )

        val formFieldsState = BankFormFieldsState(
            formArgs = formArgs,
            instantDebits = false,
        )

        assertThat(formFieldsState.showEmailField).isTrue()
    }

    private fun createFormArguments(
        billingDetailsCollectionConfiguration: BillingDetailsCollectionConfiguration,
        billingDetails: BillingDetails? = null,
    ): FormArguments {
        return FormArguments(
            paymentMethodCode = PaymentMethod.Type.USBankAccount.code,
            merchantName = "Test Merchant",
            amount = null,
            billingDetails = billingDetails,
            billingDetailsCollectionConfiguration = billingDetailsCollectionConfiguration,
            cbcEligibility = CardBrandChoiceEligibility.Ineligible,
            hasIntentToSetup = false,
            paymentMethodSaveConsentBehavior = PaymentMethodSaveConsentBehavior.Legacy,
        )
    }
}
