package com.stripe.android.view

import androidx.appcompat.view.ContextThemeWrapper
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.R
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
class BecsDebitAccountNumberEditTextTest {
    private val accountNumberEditText = BecsDebitAccountNumberEditText(
        ContextThemeWrapper(
            ApplicationProvider.getApplicationContext(),
            R.style.StripeDefaultTheme
        )
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
            .isEqualTo("The account number you entered is incomplete.")
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

    @Test
    fun `field should remove non-digits from input`() {
        accountNumberEditText.append("212.121")
        assertThat(accountNumberEditText.fieldText)
            .isEqualTo("212121")
    }
}
