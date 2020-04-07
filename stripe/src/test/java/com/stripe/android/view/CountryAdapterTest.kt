package com.stripe.android.view

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import java.util.Locale
import kotlin.test.BeforeTest
import kotlin.test.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Test class for [CountryAdapter]
 */
@RunWith(RobolectricTestRunner::class)
class CountryAdapterTest {

    private val countryAdapter: CountryAdapter by lazy {
        CountryAdapter(
            ApplicationProvider.getApplicationContext<Context>(),
            orderedCountries
        )
    }
    private val orderedCountries: List<Country> by lazy {
        CountryUtils.getOrderedCountries(Locale.getDefault())
    }

    private val suggestions: List<Country>
        get() {
            return (0 until countryAdapter.count).map {
                countryAdapter.getItem(it)
            }
        }

    @BeforeTest
    fun setup() {
        Locale.setDefault(Locale.US)
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
        countryAdapter.filter.filter("An")
        assertThat(suggestions.map { it.name })
            .isEqualTo(
                listOf(
                    "Andorra",
                    "Angola",
                    "Anguilla",
                    "Antarctica",
                    "Antigua and Barbuda"
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
            .isEqualTo("US")
        assertThat(countryAdapter.updateUnfilteredCountries(setOf("fr", "de")))
            .isTrue()
        assertThat(countryAdapter.firstItem.code)
            .isEqualTo("FR")
    }

    @Test
    fun updateUnfilteredCountries_withEmptySet_shouldNotUpdateSuggestions() {
        assertThat(countryAdapter.firstItem.code)
            .isEqualTo("US")
        assertThat(countryAdapter.updateUnfilteredCountries(emptySet()))
            .isFalse()
        assertThat(countryAdapter.firstItem.code)
            .isEqualTo("US")
    }

    @Test
    fun updateUnfilteredCountries_shouldUpdateFilter() {
        countryAdapter.filter.filter("An")
        assertThat(suggestions.map { it.name })
            .isEqualTo(
                listOf(
                    "Andorra",
                    "Angola",
                    "Anguilla",
                    "Antarctica",
                    "Antigua and Barbuda"
                )
            )

        countryAdapter.updateUnfilteredCountries(setOf("ao", "ad"))
        countryAdapter.filter.filter("An")
        assertThat(suggestions.map { it.name })
            .isEqualTo(
                listOf(
                    "Andorra",
                    "Angola"
                )
            )
    }
}
