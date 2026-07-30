package com.stripe.android.paymentsheet.ui

import com.google.common.truth.Truth.assertThat
import org.junit.Test

internal class PrimaryButtonRevealTest {
    @Test
    fun `reveals primary button when it becomes enabled with IME visible`() {
        assertThat(
            shouldRevealWhenEnabled(
                wasEnabled = false,
                isEnabled = true,
                isImeVisible = true,
            )
        ).isTrue()
    }

    @Test
    fun `does not reveal primary button when it was already enabled`() {
        assertThat(
            shouldRevealWhenEnabled(
                wasEnabled = true,
                isEnabled = true,
                isImeVisible = true,
            )
        ).isFalse()
    }

    @Test
    fun `does not reveal primary button when IME is hidden`() {
        assertThat(
            shouldRevealWhenEnabled(
                wasEnabled = false,
                isEnabled = true,
                isImeVisible = false,
            )
        ).isFalse()
    }

    @Test
    fun `does not reveal primary button when it remains disabled`() {
        assertThat(
            shouldRevealWhenEnabled(
                wasEnabled = false,
                isEnabled = false,
                isImeVisible = true,
            )
        ).isFalse()
    }
}
