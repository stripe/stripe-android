package com.stripe.android.paymentsheet.elements

import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.specifications.DropdownItem
import org.junit.Test

class IdealBankConfigTest {
    private val config: DropdownConfig = SimpleDropdownConfig(
        R.string.stripe_paymentsheet_ideal_bank,
        listOf(
            DropdownItem(text = "ABN AMRO", value = "abn_amro"),
            DropdownItem(text = "ASN Bank", value = "asn_bank"),
            DropdownItem(text = "Bunq", value = "bunq"),
            DropdownItem(text = "Handelsbanken", value = "handelsbanken"),
            DropdownItem(text = "ING", value = "ing"),
            DropdownItem(text = "Knab", value = "knab"),
            DropdownItem(text = "Rabobank", value = "rabobank"),
            DropdownItem(text = "Revolut", value = "revolut"),
            DropdownItem(text = "RegioBank", value = "regiobank"),
            DropdownItem(text = "SNS Bank (De Volksbank)", value = "sns_bank"),
            DropdownItem(text = "Triodos Bank", value = "triodos_bank"),
            DropdownItem(text = "Van Lanschot", value = "van_lanschot"),
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
