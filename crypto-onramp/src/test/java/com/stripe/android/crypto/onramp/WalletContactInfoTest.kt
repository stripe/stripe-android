package com.stripe.android.crypto.onramp

import com.google.common.truth.Truth.assertThat
import com.stripe.android.crypto.onramp.model.walletContactInfo
import com.stripe.android.model.Address
import com.stripe.android.model.PaymentMethod
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WalletContactInfoTest {
    @Test
    fun `email alone produces contact information`() {
        val result = createPaymentMethod(
            PaymentMethod.BillingDetails(email = "  user@example.com  ")
        ).walletContactInfo()

        assertThat(result?.email).isEqualTo("user@example.com")
        assertThat(result?.phone).isNull()
    }

    @Test
    fun `international phone alone produces normalized contact information`() {
        val result = createPaymentMethod(
            PaymentMethod.BillingDetails(phone = "+1 (212) 555-1234")
        ).walletContactInfo()

        assertThat(result?.phone).isEqualTo("+12125551234")
        assertThat(result?.rawPhone).isEqualTo("+1 (212) 555-1234")
    }

    @Test
    fun `national phone uses billing country`() {
        val result = createPaymentMethod(
            PaymentMethod.BillingDetails(phone = " (212) 555-1234 ", address = Address(country = "US"))
        ).walletContactInfo()

        assertThat(result?.phone).isEqualTo("+12125551234")
        assertThat(result?.rawPhone).isEqualTo(" (212) 555-1234 ")
    }

    @Test
    fun `UK phone strips national trunk prefix`() {
        val result = createPaymentMethod(
            PaymentMethod.BillingDetails(phone = "020 7946 0018", address = Address(country = "gb"))
        ).walletContactInfo()

        assertThat(result?.phone).isEqualTo("+442079460018")
        assertThat(result?.rawPhone).isEqualTo("020 7946 0018")
    }

    @Test
    fun `international phone is not prefixed with billing country`() {
        val result = createPaymentMethod(
            PaymentMethod.BillingDetails(phone = "+44 20 7946 0018", address = Address(country = "US"))
        ).walletContactInfo()

        assertThat(result?.phone).isEqualTo("+442079460018")
    }

    @Test
    fun `national phone without country keeps only original string`() {
        val result = createPaymentMethod(
            PaymentMethod.BillingDetails(phone = "(212) 555-1234")
        ).walletContactInfo()

        assertThat(result).isNotNull()
        assertThat(result?.phone).isNull()
        assertThat(result?.rawPhone).isEqualTo("(212) 555-1234")
    }

    @Test
    fun `invalid phone keeps only original string`() {
        val result = createPaymentMethod(
            PaymentMethod.BillingDetails(phone = "123", address = Address(country = "US"))
        ).walletContactInfo()

        assertThat(result?.phone).isNull()
        assertThat(result?.rawPhone).isEqualTo("123")
    }

    @Test
    fun `unknown country does not guess a national phone prefix`() {
        val result = createPaymentMethod(
            PaymentMethod.BillingDetails(phone = "2125551234", address = Address(country = "ZZ"))
        ).walletContactInfo()

        assertThat(result?.phone).isNull()
        assertThat(result?.rawPhone).isEqualTo("2125551234")
    }

    @Test
    fun `blank contact values do not produce contact information`() {
        val result = createPaymentMethod(
            PaymentMethod.BillingDetails(email = "  ", phone = "  ")
        ).walletContactInfo()

        assertThat(result).isNull()
    }

    private fun createPaymentMethod(
        billingDetails: PaymentMethod.BillingDetails? = PaymentMethod.BillingDetails(),
    ): PaymentMethod {
        return PaymentMethod(
            id = "pm_123",
            created = 1550757934255L,
            liveMode = false,
            type = PaymentMethod.Type.Card,
            billingDetails = billingDetails,
            customerId = "cus_123",
            code = "card"
        )
    }
}
