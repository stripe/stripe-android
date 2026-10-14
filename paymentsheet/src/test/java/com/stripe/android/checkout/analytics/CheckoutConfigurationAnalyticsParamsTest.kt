@file:OptIn(com.stripe.android.paymentelement.CheckoutSessionPreview::class)

package com.stripe.android.checkout.analytics

import com.google.common.truth.Truth.assertThat
import com.stripe.android.checkout.CheckoutController
import com.stripe.android.elements.ShippingAddressElement
import org.junit.Test

internal class CheckoutConfigurationAnalyticsParamsTest {
    @Test
    fun `configuration serializes supplied personal details as presence booleans`() {
        val contact = CheckoutController.Configuration.Defaults.ContactDetails()
            .name("private-name")
            .address(CheckoutController.Address().country("US").line1("private-address"))
        val configuration = CheckoutController.Configuration()
            .merchantDisplayName("private-merchant")
            .defaults(
                CheckoutController.Configuration.Defaults()
                    .billingDetails(contact)
                    .shippingDetails(contact)
                    .email("private-email")
                    .phone("private-phone")
            )
            .shippingAddressElement(
                ShippingAddressElement.Configuration().title("private-title").buttonTitle("private-button")
            )

        val params = checkoutConfigurationAnalyticsParams(configuration.build())

        assertThat(params).containsEntry("merchant_display_name", true)
        assertThat(params["defaults"]).isEqualTo(
            mapOf(
                "billing_details" to mapOf("name" to true, "address" to true),
                "shipping_details" to mapOf("name" to true, "address" to true),
                "email" to true,
                "phone" to true,
            )
        )
        assertThat(params.toString()).doesNotContain("private-")
    }

    @Test
    fun `absent personal details report false`() {
        val params = checkoutConfigurationAnalyticsParams(CheckoutController.Configuration().build())

        assertThat(params).containsEntry("merchant_display_name", false)
        assertThat(params["defaults"]).isEqualTo(
            mapOf(
                "billing_details" to mapOf("name" to false, "address" to false),
                "shipping_details" to mapOf("name" to false, "address" to false),
                "email" to false,
                "phone" to false,
            )
        )
    }
}
