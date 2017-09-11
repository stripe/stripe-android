package com.stripe.android.view;

import android.support.design.widget.TextInputLayout;
import android.widget.AutoCompleteTextView;

import com.stripe.android.BuildConfig;
import com.stripe.android.R;

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
import static org.junit.Assert.assertTrue;

/**
 * Test class for {@link CountryAutoCompleteTextView}
 */
@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 25)
public class CountryAutoCompleteTextViewTest {

    private CountryAutoCompleteTextView mCountryAutoCompleteTextView;
    private AutoCompleteTextView mAutoCompleteTextView;
    private TextInputLayout mCountryTextInputLayout;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        Locale.setDefault(Locale.US);
        ActivityController<AddressInputTestActivity> activityController =
                Robolectric.buildActivity(AddressInputTestActivity.class).create().start();
        mCountryAutoCompleteTextView = activityController.get().findViewById(R.id.country_autocomplete_aaw);
        mCountryTextInputLayout = mCountryAutoCompleteTextView.findViewById(R.id.tl_country_cat);
        mAutoCompleteTextView = mCountryAutoCompleteTextView.findViewById(R.id.autocomplete_country_cat);
    }

    @Test
    public void countryAutoCompleteTextView_whenInitialized_displaysDefaultLocaleDisplayName() {
        assertEquals(Locale.US.getCountry(), mCountryAutoCompleteTextView.getSelectedCountryCode());
        assertEquals(Locale.US.getDisplayCountry(), mAutoCompleteTextView.getText().toString());
    }

    @Test
    public void updateUIForCountryEntered_whenInvalidCountry_rendersError() {
        assertTrue( mAutoCompleteTextView.getError() == null);
        mCountryAutoCompleteTextView.setCountrySelected("FAKE COUNTRY");
        assertEquals(mAutoCompleteTextView.getResources().getString(R.string.address_country_invalid), mCountryTextInputLayout.getError());
    }

    @Test
    public void updateUIForCountryEntered_whenValidCountry_UIUpdates() {
        assertEquals(mCountryAutoCompleteTextView.getSelectedCountryCode(), Locale.US.getCountry());
        mCountryAutoCompleteTextView.setCountrySelected(Locale.UK.getCountry());
        assertEquals(mCountryAutoCompleteTextView.getSelectedCountryCode(), Locale.UK.getCountry());
    }

    @Test
    public void countryAutoCompleteTextView_onInputFocus_displayDropDown() {
        mAutoCompleteTextView.clearFocus();
        assertFalse(mAutoCompleteTextView.isPopupShowing());
        mAutoCompleteTextView.requestFocus();
        assertTrue(mAutoCompleteTextView.isPopupShowing());
    }

}
