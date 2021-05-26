package com.stripe.android.paymentsheet.elements

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.util.regex.Pattern

class EmailConfigTest {
    private val emailConfig = EmailConfig(PATTERN)

    @Test
    fun `verify determine state returns blank and required when empty`() {
        assertThat(emailConfig.determineState("")).isEqualTo(EmailConfig.Companion.Error.BlankAndRequired)
    }

    @Test
    fun `verify the if email doesn't match the pattern it returns incomplete`() {
        assertThat(emailConfig.determineState("sdf")).isEqualTo(EmailConfig.Companion.Error.Incomplete)
    }

    @Test
    fun `verify if it doesn't pattern match but has an @ and period it is malformed`() {
        assertThat(emailConfig.determineState("@.")).isEqualTo(EmailConfig.Companion.Error.Incomplete)
        assertThat(emailConfig.determineState("@.x")).isEqualTo(EmailConfig.Companion.Error.Malformed)
    }

    @Test
    fun `verify the if email matches the pattern it returns Limitless`() {
        assertThat(emailConfig.determineState("sdf@gmail.com")).isEqualTo(EmailConfig.Companion.Valid.Limitless)
    }

    @Test
    fun `verify incomplete errors are shown when don't have focus`() {
        assertThat(
            emailConfig.shouldShowError(
                EmailConfig.Companion.Error.Incomplete,
                true
            )
        ).isEqualTo(false)
        assertThat(
            emailConfig.shouldShowError(
                EmailConfig.Companion.Error.Incomplete,
                false
            )
        ).isEqualTo(true)
    }

    @Test
    fun `verify malformed are shown when you do and don't have focus`() {
        assertThat(
            emailConfig.shouldShowError(
                EmailConfig.Companion.Error.Malformed,
                true
            )
        ).isEqualTo(true)
        assertThat(
            emailConfig.shouldShowError(
                EmailConfig.Companion.Error.Malformed,
                false
            )
        ).isEqualTo(true)
    }

    @Test
    fun `verify blank and required errors are never shown`() {
        assertThat(
            emailConfig.shouldShowError(
                EmailConfig.Companion.Error.BlankAndRequired,
                true
            )
        ).isEqualTo(false)
        assertThat(
            emailConfig.shouldShowError(
                EmailConfig.Companion.Error.BlankAndRequired,
                false
            )
        ).isEqualTo(false)
    }

    @Test
    fun `verify Limitless states are never shown as error`() {
        assertThat(
            emailConfig.shouldShowError(
                EmailConfig.Companion.Valid.Limitless,
                true
            )
        ).isEqualTo(false)
        assertThat(
            emailConfig.shouldShowError(
                EmailConfig.Companion.Valid.Limitless,
                false
            )
        ).isEqualTo(false)
    }

    @Test
    fun `verify there is no filter`() {
        assertThat(emailConfig.filter("123^@gmail[\uD83E\uDD57.com"))
            .isEqualTo("123^@gmail[\uD83E\uDD57.com")
    }

    companion object {
        // This is here because it is not defined during unit tests.
        val PATTERN: Pattern = Pattern.compile(
            "[a-zA-Z0-9\\+\\.\\_\\%\\-\\+]{1,256}" +
                "\\@" +
                "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,64}" +
                "(" +
                "\\." +
                "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,25}" +
                ")+"
        )
    }
}