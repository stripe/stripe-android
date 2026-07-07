package com.stripe.android.paymentelement.confirmation.gpay

import com.google.common.truth.Truth.assertThat
import com.stripe.android.common.model.CommonConfigurationFactory
import com.stripe.android.paymentsheet.PaymentSheet
import org.junit.Test

class GooglePayIsEmailRequiredProviderTest {

    @Test
    fun `returns true when email collection is always`() {
        val result = isEmailRequired(
            email = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
            defaultBillingDetails = PaymentSheet.BillingDetails(email = "present@example.com"),
        )

        assertThat(result).isTrue()
    }

    @Test
    fun `returns true when billing details are missing and email collection is automatic`() {
        val result = isEmailRequired(
            email = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Automatic,
        )

        assertThat(result).isTrue()
    }

    @Test
    fun `returns true when billing details email is missing and email collection is automatic`() {
        val result = isEmailRequired(
            email = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Automatic,
            defaultBillingDetails = PaymentSheet.BillingDetails(),
        )

        assertThat(result).isTrue()
    }

    @Test
    fun `returns false when billing details email is present and email collection is automatic`() {
        val result = isEmailRequired(
            email = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Automatic,
            defaultBillingDetails = PaymentSheet.BillingDetails(email = "present@example.com"),
        )

        assertThat(result).isFalse()
    }

    @Test
    fun `returns false when billing details email is missing and email collection is never`() {
        val result = isEmailRequired(
            email = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Never,
            defaultBillingDetails = PaymentSheet.BillingDetails(),
        )

        assertThat(result).isFalse()
    }

    @Test
    fun `returns false when billing details email is present and email collection is never`() {
        val result = isEmailRequired(
            email = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Never,
            defaultBillingDetails = PaymentSheet.BillingDetails(email = "present@example.com"),
        )

        assertThat(result).isFalse()
    }

    private fun isEmailRequired(
        email: PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode,
        defaultBillingDetails: PaymentSheet.BillingDetails? = null,
    ): Boolean {
        return GooglePayIsEmailRequiredProvider.get(
            configuration = configuration(
                email = email,
                defaultBillingDetails = defaultBillingDetails,
            ),
        )
    }

    private fun configuration(
        email: PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode,
        defaultBillingDetails: PaymentSheet.BillingDetails? = null,
    ) = CommonConfigurationFactory.create(
        defaultBillingDetails = defaultBillingDetails,
        billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
            email = email,
        ),
    )
}
