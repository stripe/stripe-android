package com.stripe.android.view;

import android.support.v4.util.Pair;

import com.stripe.android.BuildConfig;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;

import java.util.List;
import java.util.Locale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Test class for {@link CountryAdapter}
 */

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 25)
public class CountryAdapterTest {

    private CountryAdapter mCountryAdapter;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        ActivityController<AddressInputTestActivity> activityController =
                Robolectric.buildActivity(AddressInputTestActivity.class).create().start();
        mCountryAdapter = new CountryAdapter(activityController.get());
    }

    @Test
    public void getOrderedSystemLocalesTest() {
        List<Pair<String, String>> countryToDisplayNames = mCountryAdapter.getOrderedCountries();
        Locale currentLocale = mCountryAdapter.getCurrentLocale();
        assertEquals(countryToDisplayNames.get(0).first, currentLocale.getCountry());
        boolean ordered = true;
        // Skip the first comparision since we moved the current locale up
        for (int i = 2; i < countryToDisplayNames.size(); i++) {
            if (countryToDisplayNames.get(i).second.compareTo(countryToDisplayNames.get(i - 1).second) < 0) {
                ordered = false;
            }
        }
        assertTrue(ordered);
    }
}
