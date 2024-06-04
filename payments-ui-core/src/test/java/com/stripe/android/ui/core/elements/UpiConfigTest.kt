package com.stripe.android.ui.core.elements

import com.google.common.truth.Truth.assertThat
import com.stripe.android.uicore.elements.TextFieldStateConstants.Error.Blank
import com.stripe.android.uicore.elements.TextFieldStateConstants.Error.Incomplete
import com.stripe.android.uicore.elements.TextFieldStateConstants.Valid.Limitless
import org.junit.Test

class UpiConfigTest {

    private val config = UpiConfig()

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
    fun `Treats input without @ sign as incomplete`() {
        val state = config.determineState("a")
        assertThat(state).isInstanceOf(Incomplete::class.java)
    }

    @Test
    fun `Treats input not matching VPA format as incomplete`() {
        val state = config.determineState("abc@abc.de")
        assertThat(state).isInstanceOf(Incomplete::class.java)
    }

    @Test
    fun `Treats valid input as valid and limitless`() {
        val state = config.determineState("payment.success@stripeupi")
        assertThat(state).isInstanceOf(Limitless::class.java)
    }
}
