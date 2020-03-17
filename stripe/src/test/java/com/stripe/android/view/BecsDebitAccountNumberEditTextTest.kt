package com.stripe.android.view

import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlin.test.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BecsDebitAccountNumberEditTextTest {
    private val accountNumberEditText = BecsDebitAccountNumberEditText(
        ApplicationProvider.getApplicationContext()
    )

    @Test
    fun accountNumber_whenEmpty_shouldReturnCorrectErrorMessage() {
        accountNumberEditText.setText("")
        assertThat(accountNumberEditText.accountNumber)
            .isNull()
        assertThat(accountNumberEditText.errorMessage)
            .isEqualTo("Your account number is required.")
    }

    @Test
    fun accountNumber_whenIncomplete_shouldReturnCorrectErrorMessage() {
        accountNumberEditText.minLength = 9
        accountNumberEditText.setText("1234")
        assertThat(accountNumberEditText.accountNumber)
            .isNull()
        assertThat(accountNumberEditText.errorMessage)
            .isEqualTo("Your account number is incomplete.")
    }

    @Test
    fun accountNumber_whenComplete_shouldNotHaveErrorMessage() {
        accountNumberEditText.minLength = 5
        accountNumberEditText.setText("123456")
        assertThat(accountNumberEditText.accountNumber)
            .isEqualTo("123456")
        assertThat(accountNumberEditText.errorMessage)
            .isNull()
    }
}
