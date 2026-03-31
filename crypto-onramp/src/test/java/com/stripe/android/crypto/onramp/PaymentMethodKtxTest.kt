package com.stripe.android.crypto.onramp

import com.google.common.truth.Truth.assertThat
import com.stripe.android.crypto.onramp.model.googlePayKycInfo
import com.stripe.android.model.Address
import com.stripe.android.model.PaymentMethod
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PaymentMethodKtxTest {
    @Test
    fun testGooglePayKycInfoReturnsNullWhenBillingDetailsMissing() {
        val paymentMethod = createPaymentMethod(billingDetails = null)

        assertThat(paymentMethod.googlePayKycInfo()).isNull()
    }

    @Test
    fun testGooglePayKycInfoReturnsKycInfoForBillingNameOnly() {
        val paymentMethod = createPaymentMethod(
            billingDetails = PaymentMethod.BillingDetails(
                name = "John Smith"
            )
        )

        val kycInfo = requireNotNull(paymentMethod.googlePayKycInfo())

        assertThat(kycInfo.firstName).isEqualTo("John")
        assertThat(kycInfo.lastName).isEqualTo("Smith")
        assertThat(kycInfo.address).isNull()
    }

    @Test
    fun testGooglePayKycInfoReturnsKycInfoForBillingAddressOnly() {
        val paymentMethod = createPaymentMethod(
            billingDetails = PaymentMethod.BillingDetails(
                address = Address(
                    line1 = "123 Main St\nApt 2",
                    city = "New York",
                    state = "NY",
                    postalCode = "10001",
                    country = "US"
                )
            )
        )

        val kycInfo = requireNotNull(paymentMethod.googlePayKycInfo())

        assertThat(kycInfo.firstName).isNull()
        assertThat(kycInfo.lastName).isNull()
        assertThat(kycInfo.address?.line1).isEqualTo("123 Main St\nApt 2")
        assertThat(kycInfo.address?.city).isEqualTo("New York")
        assertThat(kycInfo.address?.state).isEqualTo("NY")
        assertThat(kycInfo.address?.postalCode).isEqualTo("10001")
        assertThat(kycInfo.address?.country).isEqualTo("US")
    }

    @Test
    fun testGooglePayKycInfoReturnsKycInfoForPartialBillingName() {
        val paymentMethod = createPaymentMethod(
            billingDetails = PaymentMethod.BillingDetails(
                name = "John"
            )
        )

        val kycInfo = requireNotNull(paymentMethod.googlePayKycInfo())

        assertThat(kycInfo.firstName).isEqualTo("John")
        assertThat(kycInfo.lastName).isNull()
        assertThat(kycInfo.address).isNull()
    }

    @Test
    fun testGooglePayKycInfoReturnsNullWhenBillingDetailsHaveNoUsableFields() {
        val paymentMethod = createPaymentMethod(
            billingDetails = PaymentMethod.BillingDetails(
                name = "",
                address = Address()
            )
        )

        assertThat(paymentMethod.googlePayKycInfo()).isNull()
    }

    @Test
    fun testGooglePayKycInfoReturnsNullWhenBillingDetailsHaveWhitespaceFields() {
        val paymentMethod = createPaymentMethod(
            billingDetails = PaymentMethod.BillingDetails(
                name = " ",
                address = Address(
                    city = " ",
                    country = " ",
                    line1 = " ",
                    line2 = " ",
                    postalCode = " ",
                    state = " "
                )
            )
        )

        assertThat(paymentMethod.googlePayKycInfo()).isNull()
    }

    @Test
    fun testGooglePayKycInfoReturnsKycInfoWhenCityIsPresent() {
        val paymentMethod = createPaymentMethod(
            billingDetails = PaymentMethod.BillingDetails(
                address = Address(city = "Brooklyn")
            )
        )

        assertThat(paymentMethod.googlePayKycInfo()?.address?.city).isEqualTo("Brooklyn")
    }

    @Test
    fun testGooglePayKycInfoReturnsKycInfoWhenCountryIsPresent() {
        val paymentMethod = createPaymentMethod(
            billingDetails = PaymentMethod.BillingDetails(
                address = Address(country = "US")
            )
        )

        assertThat(paymentMethod.googlePayKycInfo()?.address?.country).isEqualTo("US")
    }

    @Test
    fun testGooglePayKycInfoReturnsKycInfoWhenLine1IsPresent() {
        val paymentMethod = createPaymentMethod(
            billingDetails = PaymentMethod.BillingDetails(
                address = Address(line1 = "123 Fake Street")
            )
        )

        assertThat(paymentMethod.googlePayKycInfo()?.address?.line1).isEqualTo("123 Fake Street")
    }

    @Test
    fun testGooglePayKycInfoReturnsKycInfoWhenLine2IsPresent() {
        val paymentMethod = createPaymentMethod(
            billingDetails = PaymentMethod.BillingDetails(
                address = Address(line2 = "Apt 2")
            )
        )

        assertThat(paymentMethod.googlePayKycInfo()?.address?.line2).isEqualTo("Apt 2")
    }

    @Test
    fun testGooglePayKycInfoReturnsKycInfoWhenPostalCodeIsPresent() {
        val paymentMethod = createPaymentMethod(
            billingDetails = PaymentMethod.BillingDetails(
                address = Address(postalCode = "11201")
            )
        )

        assertThat(paymentMethod.googlePayKycInfo()?.address?.postalCode).isEqualTo("11201")
    }

    @Test
    fun testGooglePayKycInfoReturnsKycInfoWhenStateIsPresent() {
        val paymentMethod = createPaymentMethod(
            billingDetails = PaymentMethod.BillingDetails(
                address = Address(state = "New York")
            )
        )

        assertThat(paymentMethod.googlePayKycInfo()?.address?.state).isEqualTo("New York")
    }

    @Test
    fun testGooglePayKycInfoParsesCompositeName() {
        val paymentMethod = createPaymentMethod(
            billingDetails = PaymentMethod.BillingDetails(
                name = "  Jane Mary Doe  "
            )
        )

        val kycInfo = requireNotNull(paymentMethod.googlePayKycInfo())

        assertThat(kycInfo.firstName).isEqualTo("Jane")
        assertThat(kycInfo.lastName).isEqualTo("Mary Doe")
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
