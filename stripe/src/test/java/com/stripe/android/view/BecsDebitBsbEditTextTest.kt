package com.stripe.android.view

import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlin.test.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BecsDebitBsbEditTextTest {

    private val bsbEditText = BecsDebitBsbEditText(ApplicationProvider.getApplicationContext())

    @Test
    fun onCompletedCallback_isCalled() {
        var isCompleted = false
        bsbEditText.onCompletedCallback = {
            isCompleted = true
        }

        bsbEditText.setText("212121")
        assertThat(isCompleted)
            .isTrue()
    }

    @Test
    fun bsb_isFormatted() {
        bsbEditText.append("212")
        assertThat(bsbEditText.fieldText)
            .isEqualTo("212-")

        bsbEditText.append("1")
        assertThat(bsbEditText.fieldText)
            .isEqualTo("212-1")
    }
}
