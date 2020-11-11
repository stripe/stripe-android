package com.stripe.android.paymentsheet.ui

import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
class ToolbarTest {

    private val toolbar = Toolbar(ApplicationProvider.getApplicationContext())

    @Test
    fun `buttons are enabled by default`() {
        assertThat(toolbar.closeButton.isEnabled)
            .isTrue()
        assertThat(toolbar.backButton.isEnabled)
            .isTrue()
    }

    @Test
    fun `buttons are disabled when processing`() {
        toolbar.updateProcessing(true)
        assertThat(toolbar.closeButton.isEnabled)
            .isFalse()
        assertThat(toolbar.backButton.isEnabled)
            .isFalse()
    }

    @Test
    fun `action emits Back when back button is clicked`() {
        var action: Toolbar.Action? = null
        toolbar.action.observeForever {
            action = it
        }
        toolbar.backButton.performClick()
        assertThat(action)
            .isEqualTo(Toolbar.Action.Back)
    }
}
