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
    fun `initially, label alpha is 50%`() {
        assertThat(addButton.viewBinding.label.alpha)
            .isEqualTo(0.5f)
    }

    @Test
    fun `after enabled, label alpha is 100%`() {
        addButton.isEnabled = true
        assertThat(addButton.viewBinding.label.alpha)
            .isEqualTo(1.0f)
    }
}
