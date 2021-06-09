package com.stripe.android.paymentsheet.elements

import com.google.common.truth.Truth
import org.junit.Test

class NameConfigTest {

    private val nameConfig = NameConfig()

    @Test
    fun `verify determine state returns blank and required when empty or null`() {
        Truth.assertThat(nameConfig.determineState(""))
            .isEqualTo(NameConfig.Companion.Invalid.BlankAndRequired)
    }

    @Test
    fun `verify the if name has any characters it returns Limitless`() {
        Truth.assertThat(nameConfig.determineState("Susan Smith"))
            .isEqualTo(NameConfig.Companion.Valid.Limitless)
    }

    @Test
    fun `verify blank and required errors are never shown`() {
        Truth.assertThat(
            NameConfig.Companion.Invalid.BlankAndRequired.shouldShowError(
                true
            )
        ).isEqualTo(false)
        Truth.assertThat(
            NameConfig.Companion.Invalid.BlankAndRequired.shouldShowError(
                false
            )
        ).isEqualTo(false)
    }

    @Test
    fun `verify Limitless states are never shown as error`() {
        Truth.assertThat(
            NameConfig.Companion.Valid.Limitless.shouldShowError(
                true
            )
        ).isEqualTo(false)
        Truth.assertThat(
            NameConfig.Companion.Valid.Limitless.shouldShowError(
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