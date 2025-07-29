package com.stripe.android.paymentsheet

import android.content.res.ColorStateList
import android.graphics.Color
import com.stripe.android.common.model.CommonConfiguration
import com.stripe.android.common.model.asCommonConfiguration
import com.stripe.android.elements.BillingDetails
import com.stripe.android.elements.CustomerConfiguration
import com.stripe.android.elements.CustomerSessionApiPreview
import com.stripe.android.paymentsheet.addresselement.AddressDetails
import org.junit.Test
import kotlin.test.assertFailsWith

class PaymentSheetConfigurationKtxTest {
    @Test
    fun `'validate' should fail when ephemeral key secret is blank`() {
        val configWithBlankEphemeralKeySecret = configuration.newBuilder()
            .customer(
                CustomerConfiguration(
                    id = "cus_1",
                    ephemeralKeySecret = "   "
                )
            )
            .build()
            .asCommonConfiguration()

        assertFailsWith(
            IllegalArgumentException::class,
            message = "When a CustomerConfiguration is passed to PaymentSheet, " +
                "the ephemeralKeySecret cannot be an empty string."
        ) {
            configWithBlankEphemeralKeySecret.validate(isLiveMode = false)
        }
    }

    private fun getConfig(eKey: String): CommonConfiguration {
        return configuration.newBuilder()
            .customer(
                CustomerConfiguration(
                    id = "cus_1",
                    ephemeralKeySecret = eKey
                ),
            ).build().asCommonConfiguration()
    }

    @Test
    fun `'validate' should succeed when ephemeral key secret is of correct format`() {
        getConfig("ek_askljdlkasfhgasdfjls").validate(isLiveMode = false)
        getConfig("ek_test_iiuwfhdaiuhasdvkcjn32n").validate(isLiveMode = false)
    }

    @Test
    fun `'validate' should fail when ephemeral key secret is of wrong format`() {
        fun assertFailsWithEphemeralKeySecret(ephemeralKeySecret: String) {
            assertFailsWith(
                IllegalArgumentException::class,
                message = "`ephemeralKeySecret` format does not match expected client secret formatting"
            ) {
                getConfig(ephemeralKeySecret).validate(isLiveMode = false)
            }
        }

        assertFailsWithEphemeralKeySecret("eph_askjdfhajkshdfjkashdjkfhsakjdhfkjashfd")
        assertFailsWithEphemeralKeySecret("eph_test_askjdfhajkshdfjkashdjkfhsakjdhfkjashfd")
        assertFailsWithEphemeralKeySecret("sk_askjdfhajkshdfjkashdjkfhsakjdhfkjashfd")
        assertFailsWithEphemeralKeySecret("ek_")
        assertFailsWithEphemeralKeySecret("ek")
        assertFailsWithEphemeralKeySecret("eeek_aldkfjalskdjflkasjbvdkjds")
    }

    @OptIn(CustomerSessionApiPreview::class)
    @Test
    fun `'validate' should fail when customer client secret key is secret is blank`() {
        val configWithBlankCustomerSessionClientSecret = configuration.newBuilder()
            .customer(
                CustomerConfiguration.createWithCustomerSession(
                    id = "cus_1",
                    clientSecret = "   "
                ),
            ).build().asCommonConfiguration()

        assertFailsWith(
            IllegalArgumentException::class,
            message = "When a CustomerConfiguration is passed to PaymentSheet, " +
                "the customerSessionClientSecret cannot be an empty string."
        ) {
            configWithBlankCustomerSessionClientSecret.validate(isLiveMode = false)
        }
    }

    @OptIn(CustomerSessionApiPreview::class)
    @Test
    fun `'validate' should fail when provided argument has an ephemeral key secret format`() {
        val configWithEphemeralKeySecretAsCustomerSessionClientSecret = configuration.newBuilder()
            .customer(
                CustomerConfiguration.createWithCustomerSession(
                    id = "cus_1",
                    clientSecret = "ek_12345"
                ),
            ).build().asCommonConfiguration()

        assertFailsWith(
            IllegalArgumentException::class,
            message = "Argument looks like an Ephemeral Key secret, but expecting a CustomerSession client " +
                "secret. See CustomerSession API: https://docs.stripe.com/api/customer_sessions/create"
        ) {
            configWithEphemeralKeySecretAsCustomerSessionClientSecret.validate(isLiveMode = false)
        }
    }

    @OptIn(CustomerSessionApiPreview::class)
    @Test
    fun `'validate' should fail when provided argument is not a recognized customer session client secret format`() {
        val configWithInvalidCustomerSessionClientSecret = configuration.newBuilder()
            .customer(
                CustomerConfiguration.createWithCustomerSession(
                    id = "cus_1",
                    clientSecret = "total_12345"
                ),
            ).build().asCommonConfiguration()

        assertFailsWith(
            IllegalArgumentException::class,
            message = "Argument does not look like a CustomerSession client secret. " +
                "See CustomerSession API: https://docs.stripe.com/api/customer_sessions/create"
        ) {
            configWithInvalidCustomerSessionClientSecret.validate(isLiveMode = false)
        }
    }

    @Test
    fun `'validate' should succeed when external payment methods have correct prefix`() {
        val configWithValidExternalPaymentMethods = configuration.newBuilder()
            .externalPaymentMethods(listOf("external_paypal", "external_fawry"))
            .build()
            .asCommonConfiguration()

        // Should not throw
        configWithValidExternalPaymentMethods.validate(isLiveMode = false)
    }

    @Test
    fun `'validate' should fail when external payment method does not have external_ prefix`() {
        val configWithInvalidExternalPaymentMethod = configuration.newBuilder()
            .externalPaymentMethods(listOf("paypal", "external_fawry"))
            .build()
            .asCommonConfiguration()

        assertFailsWith(
            IllegalArgumentException::class,
            message = "External payment method 'paypal' does not start with 'external_'. " +
                "All external payment methods must use the 'external_' prefix. " +
                "See https://docs.stripe.com/payments/external-payment-methods?platform=android#available-external-" +
                "payment-methods"
        ) {
            configWithInvalidExternalPaymentMethod.validate(isLiveMode = false)
        }
    }

    @Test
    fun `'validate' should succeed when external payment methods list is empty`() {
        val configWithEmptyExternalPaymentMethods = configuration.newBuilder()
            .externalPaymentMethods(emptyList())
            .build()
            .asCommonConfiguration()

        // Should not throw
        configWithEmptyExternalPaymentMethods.validate(isLiveMode = false)
    }

    @Test
    fun `'validate' should fail when multiple external payment methods have incorrect prefix`() {
        val configWithMultipleInvalidExternalPaymentMethods = configuration.newBuilder()
            .externalPaymentMethods(listOf("paypal", "venmo", "external_fawry"))
            .build()
            .asCommonConfiguration()

        assertFailsWith(
            IllegalArgumentException::class,
            message = "External payment method 'paypal' does not start with 'external_'. " +
                "All external payment methods must use the 'external_' prefix. " +
                "See https://docs.stripe.com/payments/external-payment-methods?platform=android#available-external" +
                "-payment-methods"
        ) {
            configWithMultipleInvalidExternalPaymentMethods.validate(isLiveMode = false)
        }
    }

    @Test
    fun `'validate' should succeed when in live mode with invalid external payment methods`() {
        val configWithInvalidExternalPaymentMethods = configuration.newBuilder()
            .externalPaymentMethods(listOf("paypal", "venmo"))
            .build()
            .asCommonConfiguration()

        // Should not throw when in live mode
        configWithInvalidExternalPaymentMethods.validate(isLiveMode = true)
    }

    private companion object {
        val configuration = PaymentSheet.Configuration(
            merchantDisplayName = "Merchant, Inc.",
            customer = CustomerConfiguration(
                id = "1",
                ephemeralKeySecret = "ek_123",
            ),
            googlePay = PaymentSheet.GooglePayConfiguration(
                environment = PaymentSheet.GooglePayConfiguration.Environment.Test,
                countryCode = "CA",
                currencyCode = "CAD",
                amount = 5099,
                label = "Merchant, Inc.",
                buttonType = PaymentSheet.GooglePayConfiguration.ButtonType.Checkout,
            ),
            primaryButtonColor = ColorStateList.valueOf(Color.BLUE),
            defaultBillingDetails = BillingDetails(
                name = "Jenny Rosen",
            ),
            shippingDetails = AddressDetails(
                name = "Jenny Rosen",
            ),
            primaryButtonLabel = "Buy",
            billingDetailsCollectionConfiguration = BillingDetailsCollectionConfiguration(
                name = BillingDetailsCollectionConfiguration.CollectionMode.Always,
            ),
        )
    }
}
