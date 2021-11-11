package com.stripe.android.paymentsheet.elements

import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentsheet.R
import org.junit.Test

class SimpleDropdownConfigTest {
    private val config: DropdownConfig = SimpleDropdownConfig(
        R.string.stripe_paymentsheet_ideal_bank,
        listOf(
            DropdownItemSpec(text = "ABN AMRO", value = "abn_amro"),
            DropdownItemSpec(text = "ASN Bank", value = "asn_bank"),
            DropdownItemSpec(text = "Bunq", value = "bunq"),
            DropdownItemSpec(text = "Handelsbanken", value = "handelsbanken"),
            DropdownItemSpec(text = "ING", value = "ing"),
            DropdownItemSpec(text = "Knab", value = "knab"),
            DropdownItemSpec(text = "Rabobank", value = "rabobank"),
            DropdownItemSpec(text = "Revolut", value = "revolut"),
            DropdownItemSpec(text = "RegioBank", value = "regiobank"),
            DropdownItemSpec(text = "SNS Bank (De Volksbank)", value = "sns_bank"),
            DropdownItemSpec(text = "Triodos Bank", value = "triodos_bank"),
            DropdownItemSpec(text = "Van Lanschot", value = "van_lanschot"),
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
