package com.stripe.android.ui.core.elements

import com.google.common.truth.Truth.assertThat
import com.stripe.android.ui.core.R
import org.junit.Test

class SimpleDropdownConfigTest {
    private val config: DropdownConfig = SimpleDropdownConfig(
        R.string.ideal_bank,
        listOf(
            DropdownItemSpec(display_text = "ABN AMRO", api_value = "abn_amro"),
            DropdownItemSpec(display_text = "ASN Bank", api_value = "asn_bank"),
            DropdownItemSpec(display_text = "Bunq", api_value = "bunq"),
            DropdownItemSpec(display_text = "Handelsbanken", api_value = "handelsbanken"),
            DropdownItemSpec(display_text = "ING", api_value = "ing"),
            DropdownItemSpec(display_text = "Knab", api_value = "knab"),
            DropdownItemSpec(display_text = "Rabobank", api_value = "rabobank"),
            DropdownItemSpec(display_text = "Revolut", api_value = "revolut"),
            DropdownItemSpec(display_text = "RegioBank", api_value = "regiobank"),
            DropdownItemSpec(display_text = "SNS Bank (De Volksbank)", api_value = "sns_bank"),
            DropdownItemSpec(display_text = "Triodos Bank", api_value = "triodos_bank"),
            DropdownItemSpec(display_text = "Van Lanschot", api_value = "van_lanschot"),
        )
    )

    @Test
    fun `Verify getDisplayItems gets list of display strings`() {
        assertThat(config.getDisplayItems())
            .isEqualTo(
                listOf(
                    "ABN AMRO",
                    "ASN Bank",
                    "Bunq",
                    "Handelsbanken",
                    "ING",
                    "Knab",
                    "Rabobank",
                    "Revolut",
                    "RegioBank",
                    "SNS Bank (De Volksbank)",
                    "Triodos Bank",
                    "Van Lanschot"
                )
            )
    }

    @Test
    fun `Verify convert from value returns appropriate string`() {
        assertThat(config.convertFromRaw("asn_bank"))
            .isEqualTo("ASN Bank")
    }

    @Test
    fun `Verify convert to rawValue returns appropriate string`() {
        assertThat(config.convertToRaw("SNS Bank (De Volksbank)"))
            .isEqualTo("sns_bank")
    }
}
