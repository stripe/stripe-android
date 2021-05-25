package com.stripe.android.paymentsheet.elements

import com.google.common.truth.Truth
import org.junit.Test

class NameConfigTest {

    private val nameConfig = NameConfig()

    @Test
    fun `verify determine state returns blank and required when empty or null`() {
        Truth.assertThat(nameConfig.determineState(null))
            .isEqualTo(NameConfig.Companion.Error.BlankAndRequired)
        Truth.assertThat(nameConfig.determineState(""))
            .isEqualTo(NameConfig.Companion.Error.BlankAndRequired)
    }

    @Test
    fun `verify the if name has any characters it returns Limitless`() {
        Truth.assertThat(nameConfig.determineState("Susan Smith"))
            .isEqualTo(NameConfig.Companion.Valid.Limitless)
    }

    @Test
    fun `verify blank and required errors are never shown`() {
        Truth.assertThat(
            nameConfig.shouldShowError(
                NameConfig.Companion.Error.BlankAndRequired,
                true
            )
        ).isEqualTo(false)
        Truth.assertThat(
            nameConfig.shouldShowError(
                NameConfig.Companion.Error.BlankAndRequired,
                false
            )
        ).isEqualTo(false)
    }

    @Test
    fun `verify Limitless states are never shown as error`() {
        Truth.assertThat(
            nameConfig.shouldShowError(
                NameConfig.Companion.Valid.Limitless,
                true
            )
        ).isEqualTo(false)
        Truth.assertThat(
            nameConfig.shouldShowError(
                NameConfig.Companion.Valid.Limitless,
                false
            )
        ).isEqualTo(false)
    }

    @Test
    fun `verify that only letters are allowed in the field`() {
        Truth.assertThat(nameConfig.filter("123^@gmail[\uD83E\uDD57.com"))
            .isEqualTo("gmailcom")
    }
}