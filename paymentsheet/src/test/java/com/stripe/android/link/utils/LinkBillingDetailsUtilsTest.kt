package com.stripe.android.link.utils

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.model.CountryCode
import com.stripe.android.link.TestFactory
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.model.ConsumerSession
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode
import com.stripe.android.paymentsheet.PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode
import org.junit.Test

class LinkBillingDetailsUtilsTest {

    private val testEmail = "test@example.com"
    private val testPhone = "+1234567890"
    private val testUnredactedPhone = "234567890"
    private val testUnredactedCountryCode = "US"
    private val testName = "John Doe"

    private val linkAccount = LinkAccount(
        ConsumerSession(
            emailAddress = testEmail,
            clientSecret = "secret",
            verificationSessions = emptyList(),
            redactedPhoneNumber = testPhone,
            redactedFormattedPhoneNumber = testPhone,
            unredactedPhoneNumber = testUnredactedPhone,
            phoneNumberCountry = testUnredactedCountryCode
        )
    )

    private val defaultBillingDetails = PaymentSheet.BillingDetails(
        name = testName,
        email = "merchant@example.com",
        phone = "+0987654321",
        address = PaymentSheet.Address(
            line1 = "123 Main St",
            city = "San Francisco",
            state = "CA",
            postalCode = "94105",
            country = "US"
        )
    )

    @Test
    fun `effectiveBillingDetails with no collection requirements preserves default values`() {
        val configuration = TestFactory.LINK_CONFIGURATION.copy(
            billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                name = CollectionMode.Never,
                email = CollectionMode.Never,
                phone = CollectionMode.Never,
                address = AddressCollectionMode.Never
            ),
            defaultBillingDetails = defaultBillingDetails
        )

        val result = effectiveBillingDetails(configuration, linkAccount)

        assertThat(result.name).isEqualTo(testName)
        assertThat(result.email).isEqualTo("merchant@example.com")
        assertThat(result.phone).isEqualTo("+0987654321")
        assertThat(result.address).isEqualTo(defaultBillingDetails.address)
    }

    @Test
    fun `effectiveBillingDetails supplements email when required and missing`() {
        val configurationWithoutEmail = TestFactory.LINK_CONFIGURATION.copy(
            billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                email = CollectionMode.Always
            ),
            defaultBillingDetails = defaultBillingDetails.copy(email = null)
        )

        val result = effectiveBillingDetails(configurationWithoutEmail, linkAccount)

        assertThat(result.email).isEqualTo(testEmail)
    }

    @Test
    fun `effectiveBillingDetails preserves default email when required and present`() {
        val configuration = TestFactory.LINK_CONFIGURATION.copy(
            billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                email = CollectionMode.Always
            ),
            defaultBillingDetails = defaultBillingDetails
        )

        val result = effectiveBillingDetails(configuration, linkAccount)

        assertThat(result.email).isEqualTo("merchant@example.com")
    }

    @Test
    fun `effectiveBillingDetails supplements phone when required and missing`() {
        val configurationWithoutPhone = TestFactory.LINK_CONFIGURATION.copy(
            billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                phone = CollectionMode.Always
            ),
            defaultBillingDetails = defaultBillingDetails.copy(phone = null)
        )

        val result = effectiveBillingDetails(configurationWithoutPhone, linkAccount)

        assertThat(result.phone).isEqualTo(testPhone)
    }

    @Test
    fun `effectiveBillingDetails preserves default phone when required and present`() {
        val configuration = TestFactory.LINK_CONFIGURATION.copy(
            billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                phone = CollectionMode.Always
            ),
            defaultBillingDetails = defaultBillingDetails
        )

        val result = effectiveBillingDetails(configuration, linkAccount)

        assertThat(result.phone).isEqualTo("+0987654321")
    }

    @Test
    fun `effectiveBillingDetails does not supplement when not required`() {
        val configuration = TestFactory.LINK_CONFIGURATION.copy(
            billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                email = CollectionMode.Never,
                phone = CollectionMode.Never
            ),
            defaultBillingDetails = PaymentSheet.BillingDetails()
        )

        val result = effectiveBillingDetails(configuration, linkAccount)

        assertThat(result.email).isNull()
        assertThat(result.phone).isNull()
    }

    @Test
    fun `ConsumerPaymentDetails non-card payment details always supports requirements`() {
        val bankAccount = TestFactory.CONSUMER_PAYMENT_DETAILS_BANK_ACCOUNT
        val configuration = PaymentSheet.BillingDetailsCollectionConfiguration(
            address = AddressCollectionMode.Full,
            phone = CollectionMode.Always,
            name = CollectionMode.Always
        )

        val result = bankAccount.supports(configuration, linkAccount)

        assertThat(result).isTrue()
    }

    @Test
    fun `ConsumerPaymentDetails Card supports when address is complete`() {
        val card = TestFactory.CONSUMER_PAYMENT_DETAILS_CARD.copy(
            billingAddress = ConsumerPaymentDetails.BillingAddress(
                name = "John Doe",
                line1 = "123 Main St",
                locality = "San Francisco",
                postalCode = "94105",
                countryCode = CountryCode.US,
                line2 = null,
                administrativeArea = "CA"
            )
        )
        val configuration = PaymentSheet.BillingDetailsCollectionConfiguration(
            address = AddressCollectionMode.Full
        )

        val result = card.supports(configuration, linkAccount)

        assertThat(result).isTrue()
    }

    @Test
    fun `ConsumerPaymentDetails Card does not support when address is incomplete`() {
        val card = TestFactory.CONSUMER_PAYMENT_DETAILS_CARD.copy(
            billingAddress = ConsumerPaymentDetails.BillingAddress(
                name = "John Doe",
                line1 = null, // Missing required field
                locality = "San Francisco",
                postalCode = "94105",
                countryCode = CountryCode.US,
                line2 = null,
                administrativeArea = "CA"
            )
        )
        val configuration = PaymentSheet.BillingDetailsCollectionConfiguration(
            address = AddressCollectionMode.Full
        )

        val result = card.supports(configuration, linkAccount)

        assertThat(result).isFalse()
    }

    @Test
    fun `ConsumerPaymentDetails Card does not support when phone required but missing from Link account`() {
        val card = TestFactory.CONSUMER_PAYMENT_DETAILS_CARD
        val configuration = PaymentSheet.BillingDetailsCollectionConfiguration(
            phone = CollectionMode.Always
        )
        val linkAccountWithoutPhone = LinkAccount(
            ConsumerSession(
                emailAddress = testEmail,
                clientSecret = "secret",
                verificationSessions = emptyList(),
                redactedPhoneNumber = "+1********00",
                redactedFormattedPhoneNumber = "(***) *** **00",
                unredactedPhoneNumber = null
            )
        )

        val result = card.supports(configuration, linkAccountWithoutPhone)

        assertThat(result).isFalse()
    }

    @Test
    fun `ConsumerPaymentDetails Card does not support when name required but missing`() {
        val card = TestFactory.CONSUMER_PAYMENT_DETAILS_CARD.copy(
            billingAddress = TestFactory.CONSUMER_PAYMENT_DETAILS_CARD.billingAddress?.copy(
                name = null
            )
        )
        val configuration = PaymentSheet.BillingDetailsCollectionConfiguration(
            name = CollectionMode.Always
        )

        val result = card.supports(configuration, linkAccount)

        assertThat(result).isFalse()
    }

    @Test
    fun `withEffectiveBillingDetails returns same card when linkAccount is null`() {
        val card = TestFactory.CONSUMER_PAYMENT_DETAILS_CARD
        val configuration = TestFactory.LINK_CONFIGURATION

        val result = card.withEffectiveBillingDetails(configuration, null)

        assertThat(result).isEqualTo(card)
    }

    @Test
    fun `withEffectiveBillingDetails enhances card with effective billing details if same country and postcode`() {
        val card = TestFactory.CONSUMER_PAYMENT_DETAILS_CARD.copy(
            billingAddress = ConsumerPaymentDetails.BillingAddress(
                name = null,
                line1 = "Original Line 1",
                locality = null,
                postalCode = "12345",
                countryCode = CountryCode.US,
                line2 = null,
                administrativeArea = null
            ),
            billingEmailAddress = null
        )

        val configuration = TestFactory.LINK_CONFIGURATION.copy(
            billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                email = CollectionMode.Always
            ),
            defaultBillingDetails = PaymentSheet.BillingDetails(
                name = "Default Name",
                email = "default@example.com",
                address = PaymentSheet.Address(
                    line1 = "Default Line 1",
                    line2 = "Default Line 2",
                    city = "Default City",
                    state = "CA",
                    postalCode = "12345",
                    country = "US"
                )
            )
        )

        val result = card.withEffectiveBillingDetails(configuration, linkAccount)

        // Should use effective address when addresses are compatible (same country and postal code)
        assertThat(result.billingAddress?.name).isEqualTo("Default Name")
        assertThat(result.billingAddress?.line1).isEqualTo("Default Line 1")
        assertThat(result.billingAddress?.line2).isEqualTo("Default Line 2")
        assertThat(result.billingAddress?.locality).isEqualTo("Default City")
        assertThat(result.billingAddress?.administrativeArea).isEqualTo("CA")
        assertThat(result.billingAddress?.postalCode).isEqualTo("12345")
        assertThat(result.billingAddress?.countryCode).isEqualTo(CountryCode.US)
        // Should supplement email from effective billing details
        assertThat(result.billingEmailAddress).isEqualTo("default@example.com")
    }

    @Test
    fun `withEffectiveBillingDetails uses fallback when no current address exists`() {
        val card = TestFactory.CONSUMER_PAYMENT_DETAILS_CARD.copy(
            billingAddress = null,
            billingEmailAddress = null
        )

        val configuration = TestFactory.LINK_CONFIGURATION.copy(
            billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                email = CollectionMode.Always
            ),
            defaultBillingDetails = PaymentSheet.BillingDetails(
                name = "Default Name",
                email = "default@example.com",
                address = PaymentSheet.Address(
                    line1 = "Default Line 1",
                    city = "Default City",
                    state = "CA",
                    postalCode = "54321",
                    country = "US"
                )
            )
        )

        val result = card.withEffectiveBillingDetails(configuration, linkAccount)

        // Should use effective billing address when no current address exists
        assertThat(result.billingAddress?.name).isEqualTo("Default Name")
        assertThat(result.billingAddress?.line1).isEqualTo("Default Line 1")
        assertThat(result.billingAddress?.locality).isEqualTo("Default City")
        assertThat(result.billingAddress?.countryCode).isEqualTo(CountryCode.US)
        // Should supplement email from default billing details
        assertThat(result.billingEmailAddress).isEqualTo("default@example.com")
    }

    @Test
    fun `withEffectiveBillingDetails preserves original address when different country or postcode`() {
        val card = TestFactory.CONSUMER_PAYMENT_DETAILS_CARD.copy(
            billingAddress = ConsumerPaymentDetails.BillingAddress(
                name = "Original Name",
                line1 = "Original Line 1",
                locality = "Original City",
                postalCode = "11111",
                countryCode = CountryCode.create("CA"), // Different country
                line2 = null,
                administrativeArea = "ON"
            ),
            billingEmailAddress = "original@example.com"
        )

        val configuration = TestFactory.LINK_CONFIGURATION.copy(
            billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                email = CollectionMode.Always
            ),
            defaultBillingDetails = PaymentSheet.BillingDetails(
                name = "Default Name",
                email = "default@example.com",
                address = PaymentSheet.Address(
                    line1 = "Default Line 1",
                    city = "Default City",
                    state = "CA",
                    postalCode = "22222", // Different postcode
                    country = "US" // Different country
                )
            )
        )

        val result = card.withEffectiveBillingDetails(configuration, linkAccount)

        // Should preserve original address when addresses are incompatible
        assertThat(result.billingAddress?.name).isEqualTo("Original Name")
        assertThat(result.billingAddress?.line1).isEqualTo("Original Line 1")
        assertThat(result.billingAddress?.locality).isEqualTo("Original City")
        assertThat(result.billingAddress?.postalCode).isEqualTo("11111")
        assertThat(result.billingAddress?.countryCode).isEqualTo(CountryCode.create("CA"))
        // Should still supplement email from effective billing details
        assertThat(result.billingEmailAddress).isEqualTo("default@example.com")
    }
}
