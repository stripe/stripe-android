package com.stripe.android.ui.core.elements

import com.google.common.truth.Truth.assertThat
import com.stripe.android.uicore.elements.TextFieldStateConstants.Error
import com.stripe.android.uicore.elements.TextFieldStateConstants.Valid
import org.junit.Test

class TextSectionFieldStateConstantsTest {

    @Test
    fun `verify incomplete errors are shown when don't have focus`() {
        assertThat(
            Error.Incomplete(-1).shouldShowError(
                true
            )
        ).isFalse()
        assertThat(
            Error.Incomplete(-1).shouldShowError(
                false
            )
        ).isTrue()
    }

    @Test
    fun `verify malformed are shown when you do and don't have focus`() {
        assertThat(
            Error.Invalid(-1).shouldShowError(
                true
            )
        ).isTrue()
        assertThat(
            Error.Invalid(-1).shouldShowError(
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
