package com.stripe.android.view;

import android.support.annotation.NonNull;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static org.junit.Assert.assertEquals;

/**
 * Test class for {@link CountryAdapter}
 */
@RunWith(RobolectricTestRunner.class)
public class CountryAdapterTest {

    private CountryAdapter mCountryAdapter;
    private List<String> mOrderedCountries;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        Locale.setDefault(Locale.US);

        mOrderedCountries = CountryUtils.getOrderedCountries(Locale.getDefault());
        mCountryAdapter = new CountryAdapter(
                ApplicationProvider.getApplicationContext(),
                mOrderedCountries
        );
    }

    @Test
    public void filter_whenEmptyConstraint_showsAllResults() {
        mCountryAdapter.getFilter().filter("");
        assertEquals(
                mOrderedCountries,
                getSuggestions()
        );
    }

    @Test
    public void filter_whenCountryInputNoMatch_showsAllResults() {
        mCountryAdapter.getFilter().filter("NONEXISTENT COUNTRY");
        assertEquals(
                mOrderedCountries,
                getSuggestions()
        );
    }

    @Test
    public void filter_whenCountryInputMatches_filters() {
        mCountryAdapter.getFilter().filter("United");
        assertEquals(
                Arrays.asList(
                        "United States",
                        "United Arab Emirates",
                        "United Kingdom",
                        "United States Minor Outlying Islands"
                ),
                getSuggestions()
        );
    }

    @Test
    public void filter_whenCountryInputMatchesExactly_showsAllResults() {
        mCountryAdapter.getFilter().filter("Uganda");
        assertEquals(
                mOrderedCountries,
                getSuggestions()
        );
    }

    @NonNull
    private List<String> getSuggestions() {
        final List<String> suggestions = new ArrayList<>(mCountryAdapter.getCount());
        for (int i = 0; i < mCountryAdapter.getCount(); i++) {
            suggestions.add(mCountryAdapter.getItem(i));
        }
        return suggestions;
    }
}
