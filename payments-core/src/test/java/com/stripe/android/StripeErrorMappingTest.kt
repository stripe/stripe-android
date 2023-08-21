package com.stripe.android

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.StripeError
import com.stripe.android.networking.withLocalizedMessage
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
class StripeErrorMappingTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `Uses backend message for non-Spanish locale`() {
        withLocale(Locale.US) {
            val error = StripeError(
                code = "incorrect_number",
                message = "The backend message",
            )
            val updatedError = error.withLocalizedMessage(context)
            assertThat(updatedError).isEqualTo(error)
        }
    }

    @Test
    fun `Uses backend message for Spanish locale from Spain`() {
        withLocale(Locale("es", "es")) {
            val error = StripeError(
                code = "incorrect_number",
                message = "The backend message",
            )
            val updatedError = error.withLocalizedMessage(context)
            assertThat(updatedError).isEqualTo(error)
        }
    }

    @Test
    fun `Uses client message for Spanish locale from outside Spain`() {
        withLocale(Locale("es", "ar")) {
            val error = StripeError(
                code = "incorrect_number",
                message = "The backend message",
            )
            val updatedError = error.withLocalizedMessage(context)
            assertThat(updatedError).isNotEqualTo(error)
        }
    }

    private inline fun <T> withLocale(locale: Locale, block: () -> T): T {
        val original = Locale.getDefault()
        Locale.setDefault(locale)
        val result = block()
        Locale.setDefault(original)
        return result
    }
}
