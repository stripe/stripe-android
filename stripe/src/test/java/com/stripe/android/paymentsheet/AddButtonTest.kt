package com.stripe.android.paymentsheet

import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AddButtonTest {
    private val addButton = AddButton(ApplicationProvider.getApplicationContext())

    @Test
    fun `disabling button should update label alpha`() {
        addButton.isEnabled = false
        assertThat(addButton.viewBinding.label.alpha)
            .isEqualTo(0.5f)
    }

    @Test
    fun `onReadyState() should update label`() {
        addButton.onReadyState()
        assertThat(
            addButton.viewBinding.label.text.toString()
        ).isEqualTo(
            "Add"
        )
    }

    @Test
    fun `onProcessingState() should update label`() {
        addButton.onProcessingState()
        assertThat(
            addButton.viewBinding.label.text.toString()
        ).isEqualTo(
            "Processing…"
        )
    }
}
