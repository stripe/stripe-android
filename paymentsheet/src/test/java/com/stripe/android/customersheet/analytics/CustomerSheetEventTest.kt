package com.stripe.android.customersheet.analytics

import com.google.common.truth.Truth.assertThat
import com.stripe.android.customersheet.CustomerSheetFixtures
import com.stripe.android.customersheet.CustomerSheetIntegration
import kotlin.test.Test

class CustomerSheetEventTest {
    @Test
    fun `Init event with full config should return expected params`() {
        val event = CustomerSheetEvent.Init(
            configuration = CustomerSheetFixtures.CONFIG_WITH_GOOGLE_PAY_ENABLED,
            integrationType = CustomerSheetIntegration.Type.CustomerAdapter,
        )

        assertThat(event.eventName).isEqualTo("cs_init_with_customer_adapter")

        val expectedConfigAnalyticsValue = mapOf(
            "google_pay_enabled" to true,
            "default_billing_details" to false,
            "appearance" to mapOf(
                "colorsLight" to false,
                "colorsDark" to false,
                "corner_radius" to false,
                "border_width" to false,
                "font" to false,
                "size_scale_factor" to false,
                "primary_button" to mapOf(
                    "colorsLight" to false,
                    "colorsDark" to false,
                    "corner_radius" to false,
                    "border_width" to false,
                    "font" to false,
                ),
                "embedded_payment_element" to null,
                "usage" to false,
            ),
            "payment_method_order" to listOf<String>(),
            "allows_removal_of_last_saved_payment_method" to true,
            "billing_details_collection_configuration" to mapOf(
                "attach_defaults" to false,
                "name" to "Automatic",
                "email" to "Automatic",
                "phone" to "Automatic",
                "address" to "Automatic",
            ),
            "preferred_networks" to null,
            "card_brand_acceptance" to false,
        )

        assertThat(event.additionalParams)
            .containsEntry("cs_config", expectedConfigAnalyticsValue)
    }

    @Test
    fun `Init event with minimum config should return expected params`() {
        val event = CustomerSheetEvent.Init(
            configuration = CustomerSheetFixtures.MINIMUM_CONFIG,
            integrationType = CustomerSheetIntegration.Type.CustomerAdapter,
        )

        assertThat(event.eventName).isEqualTo("cs_init_with_customer_adapter")

        val expectedConfigAnalyticsValue = mapOf(
            "google_pay_enabled" to false,
            "default_billing_details" to false,
            "appearance" to mapOf(
                "colorsLight" to false,
                "colorsDark" to false,
                "corner_radius" to false,
                "border_width" to false,
                "font" to false,
                "size_scale_factor" to false,
                "primary_button" to mapOf(
                    "colorsLight" to false,
                    "colorsDark" to false,
                    "corner_radius" to false,
                    "border_width" to false,
                    "font" to false,
                ),
                "embedded_payment_element" to null,
                "usage" to false,
            ),
            "payment_method_order" to listOf<String>(),
            "allows_removal_of_last_saved_payment_method" to true,
            "billing_details_collection_configuration" to mapOf(
                "attach_defaults" to false,
                "name" to "Automatic",
                "email" to "Automatic",
                "phone" to "Automatic",
                "address" to "Automatic",
            ),
            "preferred_networks" to null,
            "card_brand_acceptance" to false,
        )

        assertThat(event.additionalParams)
            .containsEntry("cs_config", expectedConfigAnalyticsValue)
    }

    @Test
    fun `Init event should should mark all optional params present if they are there`() {
        val expectedPrimaryButton = mapOf(
            "colorsLight" to true,
            "colorsDark" to true,
            "corner_radius" to true,
            "border_width" to true,
            "font" to true
        )

        val expectedAppearance = mapOf(
            "colorsLight" to true,
            "colorsDark" to true,
            "corner_radius" to true,
            "border_width" to true,
            "font" to true,
            "size_scale_factor" to true,
            "primary_button" to expectedPrimaryButton,
            "embedded_payment_element" to null,
            "usage" to true
        )

        val expectedBillingDetailsCollection = mapOf(
            "attach_defaults" to true,
            "name" to "Always",
            "email" to "Always",
            "phone" to "Always",
            "address" to "Full",
        )

        val expectedConfigMap = mapOf(
            "google_pay_enabled" to true,
            "default_billing_details" to true,
            "appearance" to expectedAppearance,
            "allows_removal_of_last_saved_payment_method" to false,
            "payment_method_order" to listOf("klarna", "afterpay", "card"),
            "billing_details_collection_configuration" to expectedBillingDetailsCollection,
            "preferred_networks" to "cartes_bancaires, visa",
            "card_brand_acceptance" to false,
        )

        val event = CustomerSheetEvent.Init(
            configuration = CustomerSheetFixtures.CONFIG_WITH_EVERYTHING,
            integrationType = CustomerSheetIntegration.Type.CustomerAdapter,
        )

        assertThat(event.additionalParams).isEqualTo(
            mapOf(
                "cs_config" to expectedConfigMap,
            )
        )
    }

    @Test
    fun `Init event should return expected name when integration is customer session`() {
        val event = CustomerSheetEvent.Init(
            configuration = CustomerSheetFixtures.MINIMUM_CONFIG,
            integrationType = CustomerSheetIntegration.Type.CustomerSession,
        )

        assertThat(event.eventName).isEqualTo("cs_init_with_customer_session")
    }

    @Test
    fun `SelectPaymentMethod event should return expected toString()`() {
        val event = CustomerSheetEvent.SelectPaymentMethod(code = "card")

        assertThat(event.eventName).isEqualTo("cs_carousel_payment_method_selected")

        assertThat(event.additionalParams)
            .isEqualTo(
                mapOf(
                    "selected_lpm" to "card",
                )
            )
    }
}
