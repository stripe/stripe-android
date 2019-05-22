package com.stripe.android.view;

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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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

        final List<String> countries = new ArrayList<>(
                CountryUtils.getCountryNameToCodeMap().keySet());
        mCountryAdapter = new CountryAdapter(ApplicationProvider.getApplicationContext(),
                countries);
    }

    @Test
    public void getOrderedSystemLocalesTest() {
        assertEquals(mCountryAdapter.getItem(0),
                mCountryAdapter.getCurrentLocale().getDisplayCountry());
        // Skip the first comparision since we moved the current locale up
        for (int i = 2; i < mCountryAdapter.getCount(); i++) {
            final String country = mCountryAdapter.getItem(i);
            final String prevCountry = mCountryAdapter.getItem(i - 1);
            if (prevCountry != null && country != null && country.compareTo(prevCountry) < 0) {
                fail("Countries are not ordered");
            }
        }
    }

    @Test
    public void filter_whenCountryInputNoMatch_showsAllResults() {
        final int initialCount = mCountryAdapter.getCount();
        mCountryAdapter.getFilter().filter("NONEXISTENT COUNTRY");
        assertEquals(mCountryAdapter.getCount(), initialCount);
    }

    @Test
    public void filter_whenCountryInputMatches_filters() {
        final int initialCount = mCountryAdapter.getCount();
        mCountryAdapter.getFilter().filter("a");
        assertTrue(mCountryAdapter.getCount() < initialCount);

        for (int i = 0; i < mCountryAdapter.getCount(); i++) {
            final String suggestedCountry = mCountryAdapter.getItem(i);
            assertNotNull(suggestedCountry);
            assertTrue(suggestedCountry.toLowerCase(Locale.ROOT).startsWith("a"));
        }
    }

    @Test
    public void filter_whenCountryInputMatchesExactly_showsAllResults() {
        final int initialCount = mCountryAdapter.getCount();
        mCountryAdapter.getFilter().filter("Uganda");
        assertEquals(mCountryAdapter.getCount(), initialCount);
    }
}
