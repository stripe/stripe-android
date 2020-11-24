package com.stripe.android.view

import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Locale
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
class CountryAutoCompleteTextViewValidatorTest {
    private val countryAdapter = CountryAdapter(
        ApplicationProvider.getApplicationContext(),
        CountryUtils.getOrderedCountries(Locale.US)
    )

    @Test
    fun `isValid() returns expected results`() {
        val callbackCountries = mutableListOf<Country?>()
        val validator = CountryAutoCompleteTextViewValidator(countryAdapter) {
            callbackCountries.add(it)
        }
        assertThat(validator.isValid("Hello"))
            .isFalse()
        assertThat(validator.isValid("Canada"))
            .isTrue()

        assertThat(callbackCountries)
            .containsExactly(null, Country("CA", "Canada"))
    }
}
