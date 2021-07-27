package com.stripe.android.paymentsheet.elements

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class IdealBankConfigTest {
    private val config: DropdownConfig = IdealBankConfig()

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
