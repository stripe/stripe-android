package com.stripe.android.ui.core.elements

import com.google.common.truth.Truth
import com.stripe.android.uicore.elements.NameConfig
import com.stripe.android.uicore.elements.TextFieldStateConstants.Error.Blank
import com.stripe.android.uicore.elements.TextFieldStateConstants.Valid.Limitless
import org.junit.Test

class NameConfigTest {
    private val nameConfig = NameConfig()

    @Test
    fun `verify determine state returns blank and required when empty or null`() {
        Truth.assertThat(nameConfig.determineState(""))
            .isEqualTo(Blank)
    }

    @Test
    fun `verify the if name has any characters it returns Limitless`() {
        Truth.assertThat(nameConfig.determineState("Susan Smith"))
            .isEqualTo(Limitless)
    }

    @Test
    fun `verify that only letters are allowed in the field`() {
        Truth.assertThat(nameConfig.filter("123^@gmail[\uD83E\uDD57.com"))
            .isEqualTo("gmailcom")
    }
}
