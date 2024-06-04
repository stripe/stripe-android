package com.stripe.android.ui.core.elements

import com.google.common.truth.Truth.assertThat
import com.stripe.android.ui.core.R
import com.stripe.android.uicore.elements.DropdownConfig
import org.junit.Test

class SimpleDropdownConfigTest {
    private val config: DropdownConfig = SimpleDropdownConfig(
        R.string.stripe_ideal_bank,
        listOf(
            DropdownItemSpec(displayText = "ABN AMRO", apiValue = "abn_amro"),
            DropdownItemSpec(displayText = "ASN Bank", apiValue = "asn_bank"),
            DropdownItemSpec(displayText = "Bunq", apiValue = "bunq"),
            DropdownItemSpec(displayText = "Handelsbanken", apiValue = "handelsbanken"),
            DropdownItemSpec(displayText = "ING", apiValue = "ing"),
            DropdownItemSpec(displayText = "Knab", apiValue = "knab"),
            DropdownItemSpec(displayText = "Rabobank", apiValue = "rabobank"),
            DropdownItemSpec(displayText = "Revolut", apiValue = "revolut"),
            DropdownItemSpec(displayText = "RegioBank", apiValue = "regiobank"),
            DropdownItemSpec(displayText = "SNS Bank (De Volksbank)", apiValue = "sns_bank"),
            DropdownItemSpec(displayText = "Triodos Bank", apiValue = "triodos_bank"),
            DropdownItemSpec(displayText = "Van Lanschot", apiValue = "van_lanschot")
        )
    )

    @Test
    fun `Verify getDisplayItems gets list of display strings`() {
        assertThat(config.displayItems)
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
}
