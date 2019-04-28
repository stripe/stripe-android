package com.stripe.android.view;

import android.widget.Filter;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Test class for {@link CountryAdapter}
 */
@RunWith(RobolectricTestRunner.class)
public class CountryAdapterTest {

    private CountryAdapter mCountryAdapter;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        Locale.setDefault(Locale.US);

        List<String> countries = new ArrayList<>(CountryUtils.getCountryNameToCodeMap().keySet());
        mCountryAdapter = new CountryAdapter(ApplicationProvider.getApplicationContext(), countries);
    }

    @Test
    public void getOrderedSystemLocalesTest() {
        Locale currentLocale = mCountryAdapter.getCurrentLocale();
        assertEquals(mCountryAdapter.getItem(0), currentLocale.getDisplayCountry());
        boolean ordered = true;
        // Skip the first comparision since we moved the current locale up
        for (int i = 2; i < mCountryAdapter.getCount(); i++) {
            if (mCountryAdapter.getItem(i).compareTo(mCountryAdapter.getItem(i-1)) < 0) {
                ordered = false;
            }
        }
        assertTrue(ordered);
    }

    @Test
    public void filter_whenCountryInputNoMatch_showsAllResults() {
        Filter filter = mCountryAdapter.getFilter();
        filter.filter("NONEXISTANT COUNTRY");
        int countryLength = mCountryAdapter.mCountries.size();
        assertEquals(mCountryAdapter.mSuggestions.size(), countryLength);
    }

    @Test
    public void filter_whenCountryInputMatches_filters() {
        Filter filter = mCountryAdapter.getFilter();
        int countryLength = mCountryAdapter.mCountries.size();
        filter.filter("a");
        assertTrue(mCountryAdapter.mSuggestions.size() < countryLength);
        for (String suggestedCountry: mCountryAdapter.mSuggestions) {
            assertTrue(suggestedCountry.toLowerCase(Locale.ROOT).startsWith("a"));
        }
    }

    @Test
    public void filter_whenCountryInputMatchesExactly_showsAllResults() {
        Filter filter = mCountryAdapter.getFilter();
        int countryLength = mCountryAdapter.mCountries.size();
        filter.filter("Uganda");
        assertEquals(mCountryAdapter.mSuggestions.size(), countryLength);
    }


}
