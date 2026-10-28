package com.stripe.android.checkout

import com.google.common.truth.Truth.assertThat
import com.stripe.android.googlepaylauncher.GooglePayPaymentDataUpdate
import com.stripe.android.isInstanceOf
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(CheckoutSessionPreview::class)
internal class DefaultCheckoutGooglePayPaymentDataUpdateAddressBuilderTest {
    @Test
    fun `build returns unavailable when shipping address is missing`() = runScenario {
        val result = builder.build(
            paymentDataUpdate = GooglePayPaymentDataUpdate(
                callbackTrigger = GooglePayPaymentDataUpdate.CallbackTrigger.Initialize,
                shippingAddress = null,
                billingAddress = null,
            ),
            addressSource = CheckoutSessionResponse.TaxAddressSource.SHIPPING,
        )

        assertThat(result).isEqualTo(CheckoutGooglePayPaymentDataUpdateAddressBuilder.Result.Unavailable)
    }

    @Test
    fun `build returns unavailable for missing billing address despite present shipping address`() = runScenario {
        val result = builder.build(
            paymentDataUpdate = GooglePayPaymentDataUpdate(
                callbackTrigger = GooglePayPaymentDataUpdate.CallbackTrigger.PaymentMethod,
                shippingAddress = googlePayAddress(),
                billingAddress = null,
            ),
            addressSource = CheckoutSessionResponse.TaxAddressSource.BILLING,
        )

        assertThat(result).isEqualTo(CheckoutGooglePayPaymentDataUpdateAddressBuilder.Result.Unavailable)
    }

    @Test
    fun `build returns address from shipping address fields`() = runScenario {
        val result = builder.build(
            paymentDataUpdate = GooglePayPaymentDataUpdate(
                callbackTrigger = GooglePayPaymentDataUpdate.CallbackTrigger.ShippingAddress,
                shippingAddress = googlePayAddress(
                    administrativeArea = "CA",
                    countryCode = "US",
                    locality = "South San Francisco",
                    postalCode = "94080",
                    iso3166AdministrativeArea = null,
                ),
                billingAddress = null,
            ),
            addressSource = CheckoutSessionResponse.TaxAddressSource.SHIPPING,
        )

        val available = result.assertAvailable()

        assertThat(available.address.country).isEqualTo("US")
        assertThat(available.address.city).isEqualTo("South San Francisco")
        assertThat(available.address.postalCode).isEqualTo("94080")
        assertThat(available.address.state).isEqualTo("CA")
    }

    @Test
    fun `build returns address from billing address fields`() = runScenario {
        val result = builder.build(
            paymentDataUpdate = GooglePayPaymentDataUpdate(
                callbackTrigger = GooglePayPaymentDataUpdate.CallbackTrigger.Initialize,
                shippingAddress = googlePayAddress(postalCode = "94103"),
                billingAddress = googlePayAddress(
                    administrativeArea = "New York",
                    countryCode = "US",
                    locality = "New York",
                    postalCode = "10011",
                    iso3166AdministrativeArea = "US-NY",
                ),
            ),
            addressSource = CheckoutSessionResponse.TaxAddressSource.BILLING,
        )

        val available = result.assertAvailable()

        assertThat(available.address.country).isEqualTo("US")
        assertThat(available.address.city).isEqualTo("New York")
        assertThat(available.address.postalCode).isEqualTo("10011")
        assertThat(available.address.state).isEqualTo("NY")
    }

    @Test
    fun `build uses normalized iso3166AdministrativeArea subdivision for US addresses`() = runScenario {
        val result = builder.build(
            paymentDataUpdate = GooglePayPaymentDataUpdate(
                callbackTrigger = GooglePayPaymentDataUpdate.CallbackTrigger.ShippingAddress,
                shippingAddress = googlePayAddress(
                    administrativeArea = "California",
                    countryCode = "US",
                    iso3166AdministrativeArea = "US-ca",
                ),
                billingAddress = null,
            ),
            addressSource = CheckoutSessionResponse.TaxAddressSource.SHIPPING,
        )

        val available = result.assertAvailable()

        assertThat(available.address.state).isEqualTo("CA")
    }

    @Test
    fun `build falls back to administrativeArea for non-US addresses`() = runScenario {
        val result = builder.build(
            paymentDataUpdate = GooglePayPaymentDataUpdate(
                callbackTrigger = GooglePayPaymentDataUpdate.CallbackTrigger.ShippingAddress,
                shippingAddress = googlePayAddress(
                    administrativeArea = "Ontario",
                    countryCode = "CA",
                    iso3166AdministrativeArea = "CA-ON",
                ),
                billingAddress = null,
            ),
            addressSource = CheckoutSessionResponse.TaxAddressSource.SHIPPING,
        )

        val available = result.assertAvailable()

        assertThat(available.address.state).isEqualTo("Ontario")
    }

    @Test
    fun `build treats empty locality and postal code as unset`() = runScenario {
        val result = builder.build(
            paymentDataUpdate = GooglePayPaymentDataUpdate(
                callbackTrigger = GooglePayPaymentDataUpdate.CallbackTrigger.ShippingAddress,
                shippingAddress = googlePayAddress(countryCode = "US"),
                billingAddress = null,
            ),
            addressSource = CheckoutSessionResponse.TaxAddressSource.SHIPPING,
        )

        val available = result.assertAvailable()

        assertThat(available.address.city).isNull()
        assertThat(available.address.postalCode).isNull()
    }

    private fun runScenario(
        block: suspend Scenario.() -> Unit
    ) = runTest {
        block(
            Scenario(
                builder = DefaultCheckoutGooglePayPaymentDataUpdateAddressBuilder()
            )
        )
    }

    private class Scenario(
        val builder: DefaultCheckoutGooglePayPaymentDataUpdateAddressBuilder
    )

    private fun CheckoutGooglePayPaymentDataUpdateAddressBuilder.Result.assertAvailable():
        CheckoutGooglePayPaymentDataUpdateAddressBuilder.Result.Available {
        assertThat(this).isInstanceOf<CheckoutGooglePayPaymentDataUpdateAddressBuilder.Result.Available>()
        return this as CheckoutGooglePayPaymentDataUpdateAddressBuilder.Result.Available
    }

    private fun googlePayAddress(
        administrativeArea: String = "",
        countryCode: String = "US",
        locality: String = "",
        postalCode: String = "",
        iso3166AdministrativeArea: String? = null,
    ) = GooglePayPaymentDataUpdate.GooglePayAddress(
        administrativeArea = administrativeArea,
        countryCode = countryCode,
        locality = locality,
        postalCode = postalCode,
        iso3166AdministrativeArea = iso3166AdministrativeArea,
    )
}
