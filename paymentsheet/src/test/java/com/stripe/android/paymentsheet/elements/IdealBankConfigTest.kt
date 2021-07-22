package com.stripe.android.paymentsheet.elements

import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.specifications.SectionFieldSpec
import org.junit.Test

class IdealBankConfigTest {
    private val config: DropdownConfig = SimpleDropdownConfig(
        R.string.stripe_paymentsheet_ideal_bank,
        listOf(
            SectionFieldSpec.Item("ABN AMRO", "abn_amro"),
            SectionFieldSpec.Item("ASN Bank", "asn_bank"),
            SectionFieldSpec.Item("Bunq", "bunq"),
            SectionFieldSpec.Item("Handelsbanken", "handelsbanken"),
            SectionFieldSpec.Item("ING", "ing"),
            SectionFieldSpec.Item("Knab", "knab"),
            SectionFieldSpec.Item("Rabobank", "rabobank"),
            SectionFieldSpec.Item("Revolut", "revolut"),
            SectionFieldSpec.Item("RegioBank", "regiobank"),
            SectionFieldSpec.Item("SNS Bank (De Volksbank)", "sns_bank"),
            SectionFieldSpec.Item("Triodos Bank", "triodos_bank"),
            SectionFieldSpec.Item("Van Lanschot", "van_lanschot"),
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
