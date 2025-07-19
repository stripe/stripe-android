package com.stripe.android.link.ui.wallet

import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.DisplayablePaymentDetails
import com.stripe.payments.model.R
import org.junit.Test
import com.stripe.android.paymentsheet.R as PaymentsSheetR

class DefaultPaymentUITest {

    @Test
    fun `toDefaultPaymentUI when feature flag disabled should return null`() {
        val paymentDetails = DisplayablePaymentDetails(
            defaultCardBrand = "visa",
            last4 = "4242",
            defaultPaymentType = "CARD",
            numberOfSavedPaymentDetails = 1L
        )

        val result = paymentDetails.toDefaultPaymentUI(enableDefaultValuesInECE = false)

        assertThat(result).isNull()
    }

    @Test
    fun `toDefaultPaymentUI with BANK_ACCOUNT type should return bank icon`() {
        val paymentDetails = DisplayablePaymentDetails(
            defaultCardBrand = "visa",
            last4 = "4242",
            defaultPaymentType = "BANK_ACCOUNT",
            numberOfSavedPaymentDetails = 1L
        )

        val result = paymentDetails.toDefaultPaymentUI(enableDefaultValuesInECE = true)

        assertThat(result).isNotNull()
        assertThat(result!!.paymentIconRes).isEqualTo(PaymentsSheetR.drawable.stripe_link_bank_outlined)
        assertThat(result.last4).isEqualTo("4242")
    }

    @Test
    fun `toDefaultPaymentUI when missing defaultCardBrand should return Unknown brand icon`() {
        val paymentDetails = DisplayablePaymentDetails(
            defaultCardBrand = null,
            last4 = "4242",
            defaultPaymentType = "CARD",
            numberOfSavedPaymentDetails = 1L
        )

        val result = paymentDetails.toDefaultPaymentUI(enableDefaultValuesInECE = true)

        assertThat(result).isNotNull()
        assertThat(result!!.paymentIconRes).isEqualTo(R.drawable.stripe_ic_unknown_brand_unpadded)
        assertThat(result.last4).isEqualTo("4242")
    }

    @Test
    fun `toDefaultPaymentUI when missing last4 should return null`() {
        val paymentDetails = DisplayablePaymentDetails(
            defaultCardBrand = "visa",
            last4 = null,
            defaultPaymentType = "CARD",
            numberOfSavedPaymentDetails = 1L
        )

        val result = paymentDetails.toDefaultPaymentUI(enableDefaultValuesInECE = true)

        assertThat(result).isNull()
    }

    @Test
    fun `toDefaultPaymentUI with valid CARD data should return DefaultPaymentUI`() {
        val paymentDetails = DisplayablePaymentDetails(
            defaultCardBrand = "visa",
            last4 = "4242",
            defaultPaymentType = "CARD",
            numberOfSavedPaymentDetails = 1L
        )

        val result = paymentDetails.toDefaultPaymentUI(enableDefaultValuesInECE = true)

        assertThat(result).isNotNull()
        assertThat(result!!.paymentIconRes).isEqualTo(R.drawable.stripe_ic_visa_unpadded)
        assertThat(result.last4).isEqualTo("4242")
    }

    @Test
    fun `toDefaultPaymentUI with mastercard should return correct icon`() {
        val paymentDetails = DisplayablePaymentDetails(
            defaultCardBrand = "mastercard",
            last4 = "5555",
            defaultPaymentType = "CARD",
            numberOfSavedPaymentDetails = 2L
        )

        val result = paymentDetails.toDefaultPaymentUI(enableDefaultValuesInECE = true)

        assertThat(result).isNotNull()
        assertThat(result!!.paymentIconRes).isEqualTo(R.drawable.stripe_ic_mastercard_unpadded)
        assertThat(result.last4).isEqualTo("5555")
    }

    @Test
    fun `toDefaultPaymentUI with amex should return correct icon`() {
        val paymentDetails = DisplayablePaymentDetails(
            defaultCardBrand = "amex",
            last4 = "0005",
            defaultPaymentType = "CARD",
            numberOfSavedPaymentDetails = 1L
        )

        val result = paymentDetails.toDefaultPaymentUI(enableDefaultValuesInECE = true)

        assertThat(result).isNotNull()
        assertThat(result!!.paymentIconRes).isEqualTo(R.drawable.stripe_ic_amex_unpadded)
        assertThat(result.last4).isEqualTo("0005")
    }
}
