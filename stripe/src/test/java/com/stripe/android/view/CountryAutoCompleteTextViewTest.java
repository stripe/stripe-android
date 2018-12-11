package com.stripe.android.view;

import android.widget.AutoCompleteTextView;

import com.stripe.android.BuildConfig;
import com.stripe.android.R;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;

import java.util.Locale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/**
 * Test class for {@link CountryAutoCompleteTextView}
 */
@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 25)
public class CountryAutoCompleteTextViewTest {

    private CountryAutoCompleteTextView mCountryAutoCompleteTextView;
    private AutoCompleteTextView mAutoCompleteTextView;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        Locale.setDefault(Locale.US);
        ActivityController<ShippingInfoTestActivity> activityController =
                Robolectric.buildActivity(ShippingInfoTestActivity.class).create().start();
        mCountryAutoCompleteTextView = activityController.get().findViewById(R.id.country_autocomplete_aaw);
        mAutoCompleteTextView = mCountryAutoCompleteTextView.findViewById(R.id.autocomplete_country_cat);
    }

    @Test
    public void countryAutoCompleteTextView_whenInitialized_displaysDefaultLocaleDisplayName() {
        assertEquals(Locale.US.getCountry(), mCountryAutoCompleteTextView.getSelectedCountryCode());
        assertEquals(Locale.US.getDisplayCountry(), mAutoCompleteTextView.getText().toString());
    }

    @Test
    public void updateUIForCountryEntered_whenInvalidCountry_revertsToLastCountry() {
        String previousValidCountryCode = mCountryAutoCompleteTextView.getSelectedCountryCode();
        mCountryAutoCompleteTextView.setCountrySelected("FAKE COUNTRY CODE");
        assertTrue( mAutoCompleteTextView.getError() == null);
        assertEquals(mAutoCompleteTextView.getText().toString(), new Locale("", previousValidCountryCode).getDisplayCountry());
        mCountryAutoCompleteTextView.setCountrySelected(Locale.UK.getCountry());
        assertNotEquals(mAutoCompleteTextView.getText().toString(), new Locale("", previousValidCountryCode).getDisplayCountry());
        assertEquals(mAutoCompleteTextView.getText().toString(), Locale.UK.getDisplayCountry());
    }

    @Test
    public void updateUIForCountryEntered_whenValidCountry_UIUpdates() {
        assertEquals(Locale.US.getCountry(), mCountryAutoCompleteTextView.getSelectedCountryCode());
        mCountryAutoCompleteTextView.setCountrySelected(Locale.UK.getCountry());
        assertEquals(Locale.UK.getCountry(), mCountryAutoCompleteTextView.getSelectedCountryCode());
    }

    @Test
    public void countryAutoCompleteTextView_onInputFocus_displayDropDown() {
        mAutoCompleteTextView.clearFocus();
        assertFalse(mAutoCompleteTextView.isPopupShowing());
        mAutoCompleteTextView.requestFocus();
        assertTrue(mAutoCompleteTextView.isPopupShowing());
    }

    @Test
    public void updateUIForCountryEntered_whenCountrySelectedNullAndNoLocale_doesNotCrash() {
        Locale.setDefault(Locale.CHINA);
        mCountryAutoCompleteTextView.mCountrySelected = null;
        mCountryAutoCompleteTextView.updateUIForCountryEntered(null);
    }

    @After
    public void teardown() {
        Locale.setDefault(Locale.US);
    }

}
