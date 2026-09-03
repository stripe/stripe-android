package com.stripe.android.ui.core.elements

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.ui.core.R
import com.stripe.android.uicore.elements.DropdownConfig
import org.junit.Test

class SimpleDropdownConfigTest {
    private val config: DropdownConfig = SimpleDropdownConfig(
        resolvableString(R.string.stripe_ideal_bank),
        listOf(
            DropdownItem(displayText = "ABN AMRO", apiValue = "abn_amro"),
            DropdownItem(displayText = "ASN Bank", apiValue = "asn_bank"),
            DropdownItem(displayText = "Bunq", apiValue = "bunq"),
            DropdownItem(displayText = "Handelsbanken", apiValue = "handelsbanken"),
            DropdownItem(displayText = "ING", apiValue = "ing"),
            DropdownItem(displayText = "Knab", apiValue = "knab"),
            DropdownItem(displayText = "Rabobank", apiValue = "rabobank"),
            DropdownItem(displayText = "Revolut", apiValue = "revolut"),
            DropdownItem(displayText = "RegioBank", apiValue = "regiobank"),
            DropdownItem(displayText = "SNS Bank (De Volksbank)", apiValue = "sns_bank"),
            DropdownItem(displayText = "Triodos Bank", apiValue = "triodos_bank"),
            DropdownItem(displayText = "Van Lanschot", apiValue = "van_lanschot")
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
