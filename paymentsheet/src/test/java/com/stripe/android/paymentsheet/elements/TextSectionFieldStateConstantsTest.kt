package com.stripe.android.paymentsheet.elements

import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentsheet.elements.common.TextFieldStateConstants.Invalid
import com.stripe.android.paymentsheet.elements.common.TextFieldStateConstants.Valid
import org.junit.Test

class TextSectionFieldStateConstantsTest {

    @Test
    fun `verify incomplete errors are shown when don't have focus`() {
        assertThat(
            Invalid.Incomplete.shouldShowError(
                true
            )
        ).isEqualTo(false)
        assertThat(
            Invalid.Incomplete.shouldShowError(
                false
            )
        ).isEqualTo(true)
    }

    @Test
    fun `verify malformed are shown when you do and don't have focus`() {
        assertThat(
            Invalid.Malformed.shouldShowError(
                true
            )
        ).isEqualTo(true)
        assertThat(
            Invalid.Malformed.shouldShowError(
                false
            )
        ).isEqualTo(true)
    }

    @Test
    fun `verify blank and required errors are never shown`() {
        assertThat(
            Invalid.BlankAndRequired.shouldShowError(
                true
            )
        ).isEqualTo(false)
        assertThat(
            Invalid.BlankAndRequired.shouldShowError(
                false
            )
        ).isEqualTo(false)
    }

    @Test
    fun `verify Limitless states are never shown as error`() {
        assertThat(
            Valid.Limitless.shouldShowError(
                true
            )
        ).isEqualTo(false)
        assertThat(
            Valid.Limitless.shouldShowError(
                false
            )
        ).isEqualTo(false)
    }
}