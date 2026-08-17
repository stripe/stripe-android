package com.stripe.android.checkout

import com.google.common.truth.Truth.assertThat
import com.stripe.android.googlepaylauncher.GooglePayPaymentDataError
import com.stripe.android.googlepaylauncher.GooglePayPaymentDataUpdate
import com.stripe.android.isInstanceOf
import com.stripe.android.paymentelement.CheckoutSessionPreview
import org.junit.Test

@OptIn(CheckoutSessionPreview::class)
internal class DefaultCheckoutGooglePayPaymentDataUpdateAddressBuilderTest {
    private val builder = DefaultCheckoutGooglePayPaymentDataUpdateAddressBuilder()

    @Test
    fun `build returns failure when shipping address is missing`() {
        val result = builder.build(
            GooglePayPaymentDataUpdate(
                callbackTrigger = GooglePayPaymentDataUpdate.CallbackTrigger.Initialize,
                shippingAddress = null,
            )
        )

        val failure = result.assertFailed()

        assertThat(failure.response.newTransactionInfo).isNull()
        assertThat(failure.response.error).isEqualTo(
            GooglePayPaymentDataError(
                reason = GooglePayPaymentDataError.Reason.ShippingAddressInvalid,
                message = "Shipping address is required.",
                intent = GooglePayPaymentDataError.Intent.ShippingAddress,
            )
        )
    }

    @Test
    fun `build returns failure when country code is blank`() {
        val result = builder.build(
            GooglePayPaymentDataUpdate(
                callbackTrigger = GooglePayPaymentDataUpdate.CallbackTrigger.ShippingAddress,
                shippingAddress = shippingAddress(countryCode = ""),
            )
        )

        val failure = result.assertFailed()

        assertThat(failure.response.newTransactionInfo).isNull()
        assertThat(failure.response.error).isEqualTo(
            GooglePayPaymentDataError(
                reason = GooglePayPaymentDataError.Reason.ShippingAddressInvalid,
                message = "Country is required.",
                intent = GooglePayPaymentDataError.Intent.ShippingAddress,
            )
        )
    }

    @Test
    fun `build returns address from shipping address fields`() {
        val result = builder.build(
            GooglePayPaymentDataUpdate(
                callbackTrigger = GooglePayPaymentDataUpdate.CallbackTrigger.ShippingAddress,
                shippingAddress = shippingAddress(
                    administrativeArea = "CA",
                    countryCode = "US",
                    locality = "South San Francisco",
                    postalCode = "94080",
                    iso3166AdministrativeArea = null,
                ),
            )
        )

        val success = result.assertSuccess()

        assertThat(success.address.country).isEqualTo("US")
        assertThat(success.address.city).isEqualTo("South San Francisco")
        assertThat(success.address.postalCode).isEqualTo("94080")
        assertThat(success.address.state).isEqualTo("CA")
    }

    @Test
    fun `build prefers iso3166AdministrativeArea over administrativeArea`() {
        val result = builder.build(
            GooglePayPaymentDataUpdate(
                callbackTrigger = GooglePayPaymentDataUpdate.CallbackTrigger.ShippingAddress,
                shippingAddress = shippingAddress(
                    administrativeArea = "CA",
                    countryCode = "US",
                    iso3166AdministrativeArea = "US-CA",
                ),
            )
        )

        val success = result.assertSuccess()

        assertThat(success.address.state).isEqualTo("US-CA")
    }

    @Test
    fun `build treats empty locality and postal code as unset`() {
        val result = builder.build(
            GooglePayPaymentDataUpdate(
                callbackTrigger = GooglePayPaymentDataUpdate.CallbackTrigger.ShippingAddress,
                shippingAddress = shippingAddress(countryCode = "US"),
            )
        )

        val success = result.assertSuccess()

        assertThat(success.address.city).isNull()
        assertThat(success.address.postalCode).isNull()
    }

    private fun CheckoutGooglePayPaymentDataUpdateAddressBuilder.Result.assertSuccess():
        CheckoutGooglePayPaymentDataUpdateAddressBuilder.Result.Success {
        assertThat(this).isInstanceOf<CheckoutGooglePayPaymentDataUpdateAddressBuilder.Result.Success>()
        return this as CheckoutGooglePayPaymentDataUpdateAddressBuilder.Result.Success
    }

    private fun CheckoutGooglePayPaymentDataUpdateAddressBuilder.Result.assertFailed():
        CheckoutGooglePayPaymentDataUpdateAddressBuilder.Result.Failed {
        assertThat(this).isInstanceOf<CheckoutGooglePayPaymentDataUpdateAddressBuilder.Result.Failed>()
        return this as CheckoutGooglePayPaymentDataUpdateAddressBuilder.Result.Failed
    }

    private fun shippingAddress(
        administrativeArea: String = "",
        countryCode: String = "US",
        locality: String = "",
        postalCode: String = "",
        iso3166AdministrativeArea: String? = null,
    ) = GooglePayPaymentDataUpdate.ShippingAddress(
        administrativeArea = administrativeArea,
        countryCode = countryCode,
        locality = locality,
        postalCode = postalCode,
        iso3166AdministrativeArea = iso3166AdministrativeArea,
    )
}
