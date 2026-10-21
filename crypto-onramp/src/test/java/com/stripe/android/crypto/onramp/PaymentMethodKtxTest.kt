package com.stripe.android.crypto.onramp

import com.google.common.truth.Truth.assertThat
import com.stripe.android.crypto.onramp.model.OnrampBankAccountDetails
import com.stripe.android.crypto.onramp.model.OnrampCardDetails
import com.stripe.android.crypto.onramp.model.OnrampCreateCryptoPaymentTokenResult
import com.stripe.android.crypto.onramp.model.onrampCardDetails
import com.stripe.android.crypto.onramp.model.platformPayKycInfo
import com.stripe.android.model.Address
import com.stripe.android.model.CardBrand
import com.stripe.android.model.CardFunding
import com.stripe.android.model.PaymentMethod
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PaymentMethodKtxTest {
    @Test
    fun testGooglePayKycInfoReturnsNullWhenBillingDetailsMissing() {
        val paymentMethod = createPaymentMethod(billingDetails = null)

        assertThat(paymentMethod.platformPayKycInfo()).isNull()
    }

    @Test
    fun testGooglePayKycInfoReturnsKycInfoForBillingNameOnly() {
        val paymentMethod = createPaymentMethod(
            billingDetails = PaymentMethod.BillingDetails(
                name = "John Smith"
            )
        )

        val kycInfo = requireNotNull(paymentMethod.platformPayKycInfo())

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

        val kycInfo = requireNotNull(paymentMethod.platformPayKycInfo())

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

        val kycInfo = requireNotNull(paymentMethod.platformPayKycInfo())

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

        assertThat(paymentMethod.platformPayKycInfo()).isNull()
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

        assertThat(paymentMethod.platformPayKycInfo()).isNull()
    }

    @Test
    fun testGooglePayKycInfoReturnsKycInfoWhenCityIsPresent() {
        val paymentMethod = createPaymentMethod(
            billingDetails = PaymentMethod.BillingDetails(
                address = Address(city = "Brooklyn")
            )
        )

        assertThat(paymentMethod.platformPayKycInfo()?.address?.city).isEqualTo("Brooklyn")
    }

    @Test
    fun testGooglePayKycInfoReturnsKycInfoWhenCountryIsPresent() {
        val paymentMethod = createPaymentMethod(
            billingDetails = PaymentMethod.BillingDetails(
                address = Address(country = "US")
            )
        )

        assertThat(paymentMethod.platformPayKycInfo()?.address?.country).isEqualTo("US")
    }

    @Test
    fun testGooglePayKycInfoReturnsKycInfoWhenLine1IsPresent() {
        val paymentMethod = createPaymentMethod(
            billingDetails = PaymentMethod.BillingDetails(
                address = Address(line1 = "123 Fake Street")
            )
        )

        assertThat(paymentMethod.platformPayKycInfo()?.address?.line1).isEqualTo("123 Fake Street")
    }

    @Test
    fun testGooglePayKycInfoReturnsKycInfoWhenLine2IsPresent() {
        val paymentMethod = createPaymentMethod(
            billingDetails = PaymentMethod.BillingDetails(
                address = Address(line2 = "Apt 2")
            )
        )

        assertThat(paymentMethod.platformPayKycInfo()?.address?.line2).isEqualTo("Apt 2")
    }

    @Test
    fun testGooglePayKycInfoReturnsKycInfoWhenPostalCodeIsPresent() {
        val paymentMethod = createPaymentMethod(
            billingDetails = PaymentMethod.BillingDetails(
                address = Address(postalCode = "11201")
            )
        )

        assertThat(paymentMethod.platformPayKycInfo()?.address?.postalCode).isEqualTo("11201")
    }

    @Test
    fun testGooglePayKycInfoReturnsKycInfoWhenStateIsPresent() {
        val paymentMethod = createPaymentMethod(
            billingDetails = PaymentMethod.BillingDetails(
                address = Address(state = "New York")
            )
        )

        assertThat(paymentMethod.platformPayKycInfo()?.address?.state).isEqualTo("New York")
    }

    @Test
    fun testGooglePayKycInfoParsesCompositeName() {
        val paymentMethod = createPaymentMethod(
            billingDetails = PaymentMethod.BillingDetails(
                name = "  Jane Mary Doe  "
            )
        )

        val kycInfo = requireNotNull(paymentMethod.platformPayKycInfo())

        assertThat(kycInfo.firstName).isEqualTo("Jane Mary")
        assertThat(kycInfo.lastName).isEqualTo("Doe")
    }

    @Test
    fun `email alone produces contact information`() {
        val result = createPaymentMethod(
            PaymentMethod.BillingDetails(email = "  user@example.com  ")
        ).platformPayKycInfo()

        assertThat(result?.email).isEqualTo("user@example.com")
        assertThat(result?.firstName).isNull()
        assertThat(result?.phone).isNull()
    }

    @Test
    fun `international phone alone produces normalized contact information`() {
        val result = createPaymentMethod(
            PaymentMethod.BillingDetails(phone = "+1 (212) 555-1234")
        ).platformPayKycInfo()

        assertThat(result?.phone).isEqualTo("+12125551234")
        assertThat(result?.rawPhone).isEqualTo("+1 (212) 555-1234")
    }

    @Test
    fun `national phone uses billing country`() {
        val result = createPaymentMethod(
            PaymentMethod.BillingDetails(phone = " (212) 555-1234 ", address = Address(country = "US"))
        ).platformPayKycInfo()

        assertThat(result?.phone).isEqualTo("+12125551234")
        assertThat(result?.rawPhone).isEqualTo(" (212) 555-1234 ")
    }

    @Test
    fun `UK phone strips national trunk prefix`() {
        val result = createPaymentMethod(
            PaymentMethod.BillingDetails(phone = "020 7946 0018", address = Address(country = "gb"))
        ).platformPayKycInfo()

        assertThat(result?.phone).isEqualTo("+442079460018")
        assertThat(result?.rawPhone).isEqualTo("020 7946 0018")
    }

    @Test
    fun `international phone is not prefixed with billing country`() {
        val result = createPaymentMethod(
            PaymentMethod.BillingDetails(phone = "+44 20 7946 0018", address = Address(country = "US"))
        ).platformPayKycInfo()

        assertThat(result?.phone).isEqualTo("+442079460018")
    }

    @Test
    fun `national phone without country keeps only original string`() {
        val result = createPaymentMethod(
            PaymentMethod.BillingDetails(phone = "(212) 555-1234")
        ).platformPayKycInfo()

        assertThat(result).isNotNull()
        assertThat(result?.phone).isNull()
        assertThat(result?.rawPhone).isEqualTo("(212) 555-1234")
    }

    @Test
    fun `invalid phone keeps only original string`() {
        val result = createPaymentMethod(
            PaymentMethod.BillingDetails(phone = "123", address = Address(country = "US"))
        ).platformPayKycInfo()

        assertThat(result?.phone).isNull()
        assertThat(result?.rawPhone).isEqualTo("123")
    }

    @Test
    fun `unknown country does not guess a national phone prefix`() {
        val result = createPaymentMethod(
            PaymentMethod.BillingDetails(phone = "2125551234", address = Address(country = "ZZ"))
        ).platformPayKycInfo()

        assertThat(result?.phone).isNull()
        assertThat(result?.rawPhone).isEqualTo("2125551234")
    }

    @Test
    fun `blank contact values do not produce KYC information`() {
        val result = createPaymentMethod(
            PaymentMethod.BillingDetails(email = "  ", phone = "  ")
        ).platformPayKycInfo()

        assertThat(result).isNull()
    }

    @Test
    fun `debit card summary includes brand and last four`() {
        val details = cardPaymentMethod(funding = "debit").onrampCardDetails()

        assertThat(details?.funding).isEqualTo(CardFunding.Debit)
        assertThat(details?.brand).isEqualTo(CardBrand.Visa)
        assertThat(details?.last4).isEqualTo("4242")
    }

    @Test
    fun `credit card summary maps funding`() {
        assertThat(cardPaymentMethod(funding = "credit").onrampCardDetails()?.funding)
            .isEqualTo(CardFunding.Credit)
    }

    @Test
    fun `prepaid card summary maps funding`() {
        assertThat(cardPaymentMethod(funding = "prepaid").onrampCardDetails()?.funding)
            .isEqualTo(CardFunding.Prepaid)
    }

    @Test
    fun `missing funding maps to unknown`() {
        assertThat(cardPaymentMethod(funding = null).onrampCardDetails()?.funding)
            .isEqualTo(CardFunding.Unknown)
    }

    @Test
    fun `unrecognized funding maps to unknown`() {
        assertThat(cardPaymentMethod(funding = "unrecognized").onrampCardDetails()?.funding)
            .isEqualTo(CardFunding.Unknown)
    }

    @Test
    fun `funding mapping is case insensitive`() {
        assertThat(cardPaymentMethod(funding = "DEBIT").onrampCardDetails()?.funding)
            .isEqualTo(CardFunding.Debit)
    }

    @Test
    fun `missing card produces no summary`() {
        assertThat(PaymentMethod.Builder().setId("test-payment-method").build().onrampCardDetails()).isNull()
    }

    @Test
    fun `missing last four and unknown brand are preserved`() {
        val paymentMethod = PaymentMethod.Builder()
            .setId("test-payment-method")
            .setCard(PaymentMethod.Card())
            .build()

        val details = requireNotNull(paymentMethod.onrampCardDetails())

        assertThat(details.funding).isEqualTo(CardFunding.Unknown)
        assertThat(details.brand).isEqualTo(CardBrand.Unknown)
        assertThat(details.last4).isNull()
    }

    @Test
    fun `token-only result constructor remains available`() {
        val result = OnrampCreateCryptoPaymentTokenResult.Completed("test-token")

        assertThat(result.cryptoPaymentToken).isEqualTo("test-token")
        assertThat(result.cardDetails).isNull()
        val constructor = OnrampCreateCryptoPaymentTokenResult.Completed::class.java
            .getConstructor(String::class.java)
        assertThat(constructor.newInstance("test-token").cryptoPaymentToken).isEqualTo("test-token")
    }

    @Test
    fun `card result constructor remains available`() {
        val card = cardPaymentMethod("debit").onrampCardDetails()
        val constructor = OnrampCreateCryptoPaymentTokenResult.Completed::class.java
            .getConstructor(String::class.java, OnrampCardDetails::class.java)

        val result = constructor.newInstance("test-token", card)

        assertThat(result.cardDetails).isSameInstanceAs(card)
        assertThat(result.bankAccountDetails).isNull()
    }

    @Test
    fun `token result rejects simultaneous card and bank summaries`() {
        assertThrows(IllegalArgumentException::class.java) {
            OnrampCreateCryptoPaymentTokenResult.Completed(
                "test-token",
                cardPaymentMethod("debit").onrampCardDetails(),
                OnrampBankAccountDetails("6789"),
            )
        }
    }

    private fun cardPaymentMethod(funding: String?): PaymentMethod = PaymentMethod.Builder()
        .setId("test-payment-method")
        .setCard(PaymentMethod.Card(funding = funding, brand = CardBrand.Visa, last4 = "4242"))
        .build()

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
