package com.stripe.android.uicore.elements

import com.google.common.truth.Truth.assertThat
import com.stripe.android.uicore.elements.TextFieldStateConstants.Error.Blank
import com.stripe.android.uicore.elements.TextFieldStateConstants.Error.Incomplete
import com.stripe.android.uicore.elements.TextFieldStateConstants.Error.Invalid
import com.stripe.android.uicore.elements.TextFieldStateConstants.Valid.Limitless
import org.junit.Test

class EmailConfigTest {
    private val emailConfig = EmailConfig()

    @Test
    fun `verify determine state returns blank and required when empty`() {
        assertThat(emailConfig.determineState("")).isEqualTo(Blank)
    }

    @Test
    fun `verify the if email doesn't match the pattern it returns incomplete`() {
        assertThat(emailConfig.determineState("sdf")).isInstanceOf(Incomplete::class.java)
    }

    @Test
    fun `verify if it doesn't pattern match but has an @ and period it is malformed`() {
        assertThat(emailConfig.determineState("@.")).isInstanceOf(Incomplete::class.java)
        assertThat(emailConfig.determineState("@.x")).isInstanceOf(Invalid::class.java)
    }

    @Test
    fun `verify the if email matches the pattern it returns Limitless`() {
        assertThat(emailConfig.determineState("sdf@gmail.com")).isEqualTo(Limitless)
    }

    @Test
    fun `verify more than one @ is invalid`() {
        assertThat(emailConfig.determineState("invalid@email@")).isInstanceOf(Invalid::class.java)
    }

    @Test
    fun `verify filters spaces`() {
        assertThat(emailConfig.filter("12 3^@gm ail[\uD83E\uDD57 .com"))
            .isEqualTo("123^@gmail[\uD83E\uDD57.com")
    }

    @Test
    fun `verify filters whitespace`() {
        assertThat(emailConfig.filter("example@email.com\n\t "))
            .isEqualTo("example@email.com")
    }
}
