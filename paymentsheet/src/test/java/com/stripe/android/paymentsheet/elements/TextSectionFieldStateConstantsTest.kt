package com.stripe.android.paymentsheet.elements

import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentsheet.elements.TextFieldStateConstants.Error
import com.stripe.android.paymentsheet.elements.TextFieldStateConstants.Valid
import org.junit.Test

class TextSectionFieldStateConstantsTest {

    @Test
    fun `verify incomplete errors are shown when don't have focus`() {
        assertThat(
            Error.Incomplete.shouldShowError(
                true
            )
        ).isFalse()
        assertThat(
            Error.Incomplete.shouldShowError(
                false
            )
        ).isTrue()
    }

    @Test
    fun `verify malformed are shown when you do and don't have focus`() {
        assertThat(
            Error.Invalid().shouldShowError(
                true
            )
        ).isTrue()
        assertThat(
            Error.Invalid().shouldShowError(
                false
            )
        ).isTrue()
    }

    @Test
    fun `verify blank and required errors are never shown`() {
        assertThat(
            Error.Blank.shouldShowError(
                true
            )
        ).isFalse()
        assertThat(
            Error.Blank.shouldShowError(
                false
            )
        ).isFalse()
    }

    @Test
    fun `verify Limitless states are never shown as error`() {
        assertThat(
            Valid.Limitless.shouldShowError(
                true
            )
        ).isFalse()
        assertThat(
            Valid.Limitless.shouldShowError(
                false
            )
        ).isFalse()
    }
}
