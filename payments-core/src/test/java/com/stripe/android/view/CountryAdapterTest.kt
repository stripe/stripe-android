package com.stripe.android.view

import android.content.Context
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.model.Country
import com.stripe.android.core.model.CountryCode
import com.stripe.android.core.model.CountryUtils
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Locale
import kotlin.test.Test

/**
 * Test class for [CountryAdapter]
 */
@RunWith(RobolectricTestRunner::class)
class CountryAdapterTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val orderedCountries = CountryUtils.getOrderedCountries(Locale.US)
    private val countryAdapter =
        CountryAdapter(
            ApplicationProvider.getApplicationContext(),
            orderedCountries
        ) {
            TextView(context)
        }

    private val suggestions: List<Country>
        get() {
            return (0 until countryAdapter.count).map {
                countryAdapter.getItem(it)
            }
        }

    @Test
    fun filter_whenEmptyConstraint_showsAllResults() {
        countryAdapter.filter.filter("")
        assertThat(suggestions)
            .isEqualTo(orderedCountries)
    }

    @Test
    fun filter_whenCountryInputNoMatch_showsAllResults() {
        countryAdapter.filter.filter("NONEXISTENT COUNTRY")
        assertThat(suggestions)
            .isEqualTo(orderedCountries)
    }

    @Test
    fun filter_whenCountryInputMatches_filters() {
        countryAdapter.filter.filter("Ar")
        assertThat(suggestions.map { it.name })
            .isEqualTo(
                listOf(
                    "Argentina",
                    "Armenia",
                    "Aruba"
                )
            )
    }

    @Test
    fun filter_whenCountryInputMatchesExactly_showsAllResults() {
        countryAdapter.filter.filter("Uganda")
        assertThat(suggestions)
            .isEqualTo(orderedCountries)
    }

    @Test
    fun updateUnfilteredCountries_withPopulatedSet_shouldUpdateSuggestions() {
        assertThat(countryAdapter.firstItem.code)
            .isEqualTo(CountryCode.create("US"))
        assertThat(countryAdapter.updateUnfilteredCountries(setOf("fr", "de")))
            .isTrue()
        assertThat(countryAdapter.firstItem.code)
            .isEqualTo(CountryCode.create("FR"))
    }

    @Test
    fun updateUnfilteredCountries_withEmptySet_shouldNotUpdateSuggestions() {
        assertThat(countryAdapter.firstItem.code)
            .isEqualTo(CountryCode.US)
        assertThat(countryAdapter.updateUnfilteredCountries(emptySet()))
            .isFalse()
        assertThat(countryAdapter.firstItem.code)
            .isEqualTo(CountryCode.US)
    }

    @Test
    fun updateUnfilteredCountries_shouldUpdateFilter() {
        countryAdapter.filter.filter("Ar")
        assertThat(suggestions.map { it.name })
            .isEqualTo(
                listOf(
                    "Argentina",
                    "Armenia",
                    "Aruba"
                )
            )

        countryAdapter.updateUnfilteredCountries(setOf("ar"))
        countryAdapter.filter.filter("Ar")
        assertThat(suggestions.map { it.name })
            .isEqualTo(
                listOf(
                    "Argentina"
                )
            )
    }
}
