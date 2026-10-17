package com.stripe.android.ui.core.elements

import com.google.common.truth.Truth.assertThat
import com.stripe.android.uicore.elements.TextFieldStateConstants.Error
import com.stripe.android.uicore.elements.TextFieldStateConstants.Valid
import org.junit.Test

class TextSectionFieldStateConstantsTest {

    @Test
    fun `verify incomplete errors are shown when don't have focus`() {
        assertThat(
            Error.Incomplete(-1).shouldShowValidationMessage(
                true
            )
        ).isFalse()
        assertThat(
            Error.Incomplete(-1).shouldShowValidationMessage(
                false
            )
        ).isTrue()
    }

    @Test
    fun `verify malformed are shown when you do and don't have focus`() {
        assertThat(
            Error.Invalid(-1).shouldShowValidationMessage(
                true
            )
        ).isTrue()
        assertThat(
            Error.Invalid(-1).shouldShowValidationMessage(
                false
            )
        ).isTrue()
    }

    @Test
    fun `verify blank and required errors are never shown`() {
        assertThat(
            Error.Blank.shouldShowValidationMessage(
                true
            )
        ).isFalse()
        assertThat(
            Error.Blank.shouldShowValidationMessage(
                false
            )
        ).isFalse()
    }

    @Test
    fun `verify Limitless states are never shown as error`() {
        assertThat(
            Valid.Limitless.shouldShowValidationMessage(
                true
            )
        ).isFalse()
        assertThat(
            Valid.Limitless.shouldShowValidationMessage(
                false
            )
        ).isFalse()
    }
}
