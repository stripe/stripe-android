package com.stripe.android.view

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import java.util.Locale
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.runner.RunWith
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner

/**
 * Test class for [CountryAdapter]
 */
@RunWith(RobolectricTestRunner::class)
class CountryAdapterTest {

    private lateinit var countryAdapter: CountryAdapter
    private lateinit var orderedCountries: List<Country>

    private val suggestions: List<Country>
        get() {
            return (0 until countryAdapter.count).map {
                countryAdapter.getItem(it)
            }
        }

    @BeforeTest
    fun setup() {
        MockitoAnnotations.initMocks(this)
        Locale.setDefault(Locale.US)

        orderedCountries = CountryUtils.getOrderedCountries(Locale.getDefault())
        countryAdapter = CountryAdapter(
            ApplicationProvider.getApplicationContext<Context>(),
            orderedCountries
        )
    }

    @Test
    fun filter_whenEmptyConstraint_showsAllResults() {
        countryAdapter.filter.filter("")
        assertEquals(
            orderedCountries,
            suggestions
        )
    }

    @Test
    fun filter_whenCountryInputNoMatch_showsAllResults() {
        countryAdapter.filter.filter("NONEXISTENT COUNTRY")
        assertEquals(
            orderedCountries,
            suggestions
        )
    }

    @Test
    fun filter_whenCountryInputMatches_filters() {
        countryAdapter.filter.filter("United")
        assertEquals(
            listOf(
                "United States",
                "United Arab Emirates",
                "United Kingdom",
                "United States Minor Outlying Islands"
            ),
            suggestions.map { it.name }
        )
    }

    @Test
    fun filter_whenCountryInputMatchesExactly_showsAllResults() {
        countryAdapter.filter.filter("Uganda")
        assertEquals(
            orderedCountries,
            suggestions
        )
    }

    @Test
    fun updateUnfilteredCountries_withPopulatedSet_shouldUpdateSuggestions() {
        assertEquals("US", countryAdapter.firstItem.code)
        assertTrue(countryAdapter.updateUnfilteredCountries(setOf("fr", "de")))
        assertEquals("FR", countryAdapter.firstItem.code)
    }

    @Test
    fun updateUnfilteredCountries_withEmptySet_shouldNotUpdateSuggestions() {
        assertEquals("US", countryAdapter.firstItem.code)
        assertFalse(countryAdapter.updateUnfilteredCountries(emptySet()))
        assertEquals("US", countryAdapter.firstItem.code)
    }

    @Test
    fun updateUnfilteredCountries_shouldUpdateFilter() {
        countryAdapter.filter.filter("United")
        assertEquals(
            listOf(
                "United States",
                "United Arab Emirates",
                "United Kingdom",
                "United States Minor Outlying Islands"
            ),
            suggestions.map { it.name }
        )

        countryAdapter.updateUnfilteredCountries(setOf("gb", "ae"))
        countryAdapter.filter.filter("United")
        assertEquals(
            listOf(
                "United Arab Emirates",
                "United Kingdom"
            ),
            suggestions.map { it.name }
        )
    }
}
