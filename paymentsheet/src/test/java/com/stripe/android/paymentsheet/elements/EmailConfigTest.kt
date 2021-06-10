package com.stripe.android.paymentsheet.elements

import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentsheet.elements.common.TextFieldStateConstants.Error.*
import com.stripe.android.paymentsheet.elements.common.TextFieldStateConstants.Valid.*
import org.junit.Test

class EmailConfigTest {
    private val emailConfig = EmailConfig()

    @Test
    fun `verify determine state returns blank and required when empty`() {
        assertThat(emailConfig.determineState("")).isEqualTo(BlankAndRequired)
    }

    @Test
    fun `verify the if email doesn't match the pattern it returns incomplete`() {
        assertThat(emailConfig.determineState("sdf")).isEqualTo(Incomplete)
    }

    @Test
    fun `verify if it doesn't pattern match but has an @ and period it is malformed`() {
        assertThat(emailConfig.determineState("@.")).isEqualTo(Incomplete)
        assertThat(emailConfig.determineState("@.x")).isEqualTo(Invalid)
    }

    @Test
    fun `verify the if email matches the pattern it returns Limitless`() {
        assertThat(emailConfig.determineState("sdf@gmail.com")).isEqualTo(Limitless)
    }

    @Test
    fun `verify there is no filter`() {
        assertThat(emailConfig.filter("123^@gmail[\uD83E\uDD57.com"))
            .isEqualTo("123^@gmail[\uD83E\uDD57.com")
    }

}