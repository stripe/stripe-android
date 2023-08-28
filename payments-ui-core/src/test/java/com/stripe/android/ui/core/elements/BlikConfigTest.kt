package com.stripe.android.ui.core.elements

import com.google.common.truth.Truth.assertThat
import com.stripe.android.uicore.elements.TextFieldStateConstants.Error.Blank
import com.stripe.android.uicore.elements.TextFieldStateConstants.Error.Incomplete
import com.stripe.android.uicore.elements.TextFieldStateConstants.Error.Invalid
import com.stripe.android.uicore.elements.TextFieldStateConstants.Valid.Limitless
import org.junit.Test

class BlikConfigTest {
    private val config = BlikConfig()

    @Test
    fun `Treats empty input as blank`() {
        val state = config.determineState("")
        assertThat(state).isEqualTo(Blank)
    }

    @Test
    fun `Rejects blank input`() {
        assertThat(config.filter(" ")).isEqualTo("")
    }

    @Test
    fun `Rejects non-numeric input`() {
        assertThat(config.filter(" ")).isEqualTo("")
    }

    @Test
    fun `Treats input with less than six digits as incomplete`() {
        val state = config.determineState("12345")
        assertThat(state).isInstanceOf(Incomplete::class.java)
    }

    @Test
    fun `Treats input more than six digits as invalid`() {
        assertThat(config.determineState("1234567")).isInstanceOf(Invalid::class.java)
    }

    @Test
    fun `Treats non-numeric input as blank`() {
        assertThat(config.determineState("12a456")).isInstanceOf(Invalid::class.java)
        assertThat(config.determineState("abcdef")).isInstanceOf(Invalid::class.java)
        assertThat(config.determineState("stripe.com")).isInstanceOf(Invalid::class.java)
    }

    @Test
    fun `Treats valid input as valid and limitless`() {
        val state = config.determineState("123456")
        assertThat(state).isInstanceOf(Limitless::class.java)
    }
}
