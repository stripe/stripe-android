package com.stripe.android.view

import android.content.Context
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.model.Country
import com.stripe.android.core.model.CountryUtils
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Locale
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
class CountryAutoCompleteTextViewValidatorTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val countryAdapter = CountryAdapter(
        context,
        CountryUtils.getOrderedCountries(Locale.US)
    ) {
        TextView(context)
    }

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
