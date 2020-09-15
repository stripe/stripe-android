package com.stripe.android.view

import androidx.appcompat.view.ContextThemeWrapper
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.R
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
class EmailEditTextTest {
    private val emailEditText = EmailEditText(
        ContextThemeWrapper(
            ApplicationProvider.getApplicationContext(),
            R.style.StripeDefaultTheme
        )
    )

    @Test
    fun email_withEmptyEmail_shouldSetError() {
        assertThat(emailEditText.email)
            .isNull()
        assertThat(emailEditText.errorMessage)
            .isEqualTo("Your email address is required.")
    }

    @Test
    fun email_withIncompleteEmail_shouldSetError() {
        emailEditText.setText("jenny@")
        assertThat(emailEditText.email)
            .isNull()
        assertThat(emailEditText.errorMessage)
            .isEqualTo("Your email address is invalid.")
    }

    @Test
    fun email_withValidEmail_shouldNotSetError() {
        emailEditText.setText("jrosen@example.com")
        assertThat(emailEditText.email)
            .isEqualTo("jrosen@example.com")
        assertThat(emailEditText.errorMessage)
            .isNull()
    }
}
