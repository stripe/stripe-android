package com.stripe.android.view

import android.widget.AutoCompleteTextView
import com.stripe.android.R
import java.util.Locale
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.runner.RunWith
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner

/**
 * Test class for [CountryAutoCompleteTextView]
 */
@RunWith(RobolectricTestRunner::class)
class CountryAutoCompleteTextViewTest : BaseViewTest<ShippingInfoTestActivity>(
    ShippingInfoTestActivity::class.java
) {
    private lateinit var countryAutoCompleteTextView: CountryAutoCompleteTextView
    private lateinit var autoCompleteTextView: AutoCompleteTextView

    @BeforeTest
    fun setup() {
        MockitoAnnotations.initMocks(this)
        Locale.setDefault(Locale.US)
        countryAutoCompleteTextView = createStartedActivity()
            .findViewById(R.id.country_autocomplete_aaw)
        autoCompleteTextView = countryAutoCompleteTextView
            .findViewById(R.id.autocomplete_country_cat)
    }

    @Test
    fun countryAutoCompleteTextView_whenInitialized_displaysDefaultLocaleDisplayName() {
        assertEquals(Locale.US.country, countryAutoCompleteTextView.selectedCountry.code)
        assertEquals(Locale.US.displayCountry, autoCompleteTextView.text.toString())
    }

    @Test
    fun updateUIForCountryEntered_whenInvalidCountry_revertsToLastCountry() {
        val previousValidCountryCode = countryAutoCompleteTextView.selectedCountry.code
        countryAutoCompleteTextView.setCountrySelected("FAKE COUNTRY CODE")
        assertNull(autoCompleteTextView.error)
        assertEquals(autoCompleteTextView.text.toString(),
            Locale("", previousValidCountryCode).displayCountry)
        countryAutoCompleteTextView.setCountrySelected(Locale.UK.country)
        assertNotEquals(autoCompleteTextView.text.toString(),
            Locale("", previousValidCountryCode).displayCountry)
        assertEquals(autoCompleteTextView.text.toString(), Locale.UK.displayCountry)
    }

    @Test
    fun updateUIForCountryEntered_whenValidCountry_UIUpdates() {
        assertEquals(Locale.US.country, countryAutoCompleteTextView.selectedCountry.code)
        countryAutoCompleteTextView.setCountrySelected(Locale.UK.country)
        assertEquals(Locale.UK.country, countryAutoCompleteTextView.selectedCountry.code)
    }

    @Test
    fun countryAutoCompleteTextView_onInputFocus_displayDropDown() {
        autoCompleteTextView.clearFocus()
        assertFalse(autoCompleteTextView.isPopupShowing)
        autoCompleteTextView.requestFocus()
        assertTrue(autoCompleteTextView.isPopupShowing)
    }

    @AfterTest
    fun teardown() {
        Locale.setDefault(Locale.US)
    }
}
