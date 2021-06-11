package com.stripe.android.paymentsheet.elements

import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentsheet.elements.common.TextFieldStateConstants.Error
import com.stripe.android.paymentsheet.elements.common.TextFieldStateConstants.Valid
import org.junit.Test

class TextSectionFieldStateConstantsTest {

    @Test
    fun `verify incomplete errors are shown when don't have focus`() {
        assertThat(
            Error.Incomplete.shouldShowError(
                true
            )
        ).isEqualTo(false)
        assertThat(
            Error.Incomplete.shouldShowError(
                false
            )
        ).isEqualTo(true)
    }

    @Test
    fun `verify malformed are shown when you do and don't have focus`() {
        assertThat(
            Error.Invalid.shouldShowError(
                true
            )
        ).isEqualTo(true)
        assertThat(
            Error.Invalid.shouldShowError(
                false
            )
        ).isEqualTo(true)
    }

    @Test
    fun `verify blank and required errors are never shown`() {
        assertThat(
            Error.Blank.shouldShowError(
                true
            )
        ).isEqualTo(false)
        assertThat(
            Error.Blank.shouldShowError(
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