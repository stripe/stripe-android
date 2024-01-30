package com.stripe.android.paymentsheet

import android.content.res.ColorStateList
import android.graphics.Color
import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentsheet.addresselement.AddressDetails
import org.junit.Test

class PaymentSheetConfigurationKtxTest {
    @Test
    fun `'containVolatileDifferences' should return false when no volatile differences are found`() {
        val changedConfiguration = configuration.copy(
            merchantDisplayName = "New merchant, Inc.",
            primaryButtonColor = ColorStateList.valueOf(Color.GREEN),
            googlePay = PaymentSheet.GooglePayConfiguration(
                environment = PaymentSheet.GooglePayConfiguration.Environment.Test,
                countryCode = "CA",
                currencyCode = "CAD",
                amount = 6899,
                label = "New merchant, Inc.",
                buttonType = PaymentSheet.GooglePayConfiguration.ButtonType.Plain,
            ),
            primaryButtonLabel = "Pay",
        )

        assertThat(configuration.containsVolatileDifferences(changedConfiguration)).isFalse()
    }

    @Test
    fun `'containVolatileDifferences' should return true when volatile differences are found`() {
        val configWithGooglePayChanges = configuration.copy(
            googlePay = PaymentSheet.GooglePayConfiguration(
                environment = PaymentSheet.GooglePayConfiguration.Environment.Production,
                countryCode = "US",
                currencyCode = "USD",
                amount = 6899,
                label = "New merchant, Inc.",
                buttonType = PaymentSheet.GooglePayConfiguration.ButtonType.Plain,
            ),
        )

        assertThat(configuration.containsVolatileDifferences(configWithGooglePayChanges)).isTrue()

        val configWithBillingDetailsChanges = configuration.copy(
            defaultBillingDetails = PaymentSheet.BillingDetails(
                name = "Jenny Richards",
            ),
        )

        assertThat(configuration.containsVolatileDifferences(configWithBillingDetailsChanges)).isTrue()

        val configWithShippingDetailsChanges = configuration.copy(
            shippingDetails = AddressDetails(
                name = "Jenny Richards",
            ),
        )

        assertThat(configuration.containsVolatileDifferences(configWithShippingDetailsChanges)).isTrue()

        val configWithBillingConfigChanges = configuration.copy(
            billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                name = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Never,
            ),
        )

        assertThat(configuration.containsVolatileDifferences(configWithBillingConfigChanges)).isTrue()
    }

    private companion object {
        val configuration = PaymentSheet.Configuration(
            merchantDisplayName = "Merchant, Inc.",
            customer = PaymentSheet.CustomerConfiguration(
                id = "1",
                ephemeralKeySecret = "secret",
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
            defaultBillingDetails = PaymentSheet.BillingDetails(
                name = "Jenny Rosen",
            ),
            shippingDetails = AddressDetails(
                name = "Jenny Rosen",
            ),
            primaryButtonLabel = "Buy",
            billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                name = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
            ),
        )
    }
}
