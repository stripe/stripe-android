package com.stripe.android.view;

import android.support.design.widget.TextInputLayout;
import android.support.v4.util.Pair;
import android.view.View;
import android.widget.Spinner;

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

import java.util.List;
import java.util.Locale;

import static org.junit.Assert.assertEquals;

/**
 * Test class for {@link AddAddressWidget}
 */
@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 25)
public class AddAddressWidgetTest {

    private AddAddressWidget mAddAddressWidget;
    private TextInputLayout mAddressLine1;
    private TextInputLayout mAddressLine2;
    private TextInputLayout mPostalCodeLayout;
    private TextInputLayout mStateInput;
    private Spinner mCountrySpinner;
    private CountryAdapter mCountryAdapter;
    private List<Pair<String, String>> mOrderedCountries;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        ActivityController<AddressInputTestActivity> activityController =
                Robolectric.buildActivity(AddressInputTestActivity.class).create().start();
        mAddAddressWidget = activityController.get().getAddAddressWidget();
        mAddressLine1 = mAddAddressWidget.findViewById(R.id.tl_address_line1_aaw);
        mAddressLine2 = mAddAddressWidget.findViewById(R.id.tl_address_line2_aaw);
        mPostalCodeLayout = mAddAddressWidget.findViewById(R.id.tl_postal_code_aaw);
        mStateInput = mAddAddressWidget.findViewById(R.id.tl_state_aaw);
        mCountrySpinner = mAddAddressWidget.findViewById(R.id.spinner_country_aaw);
        mCountryAdapter = (CountryAdapter) mCountrySpinner.getAdapter();
        mOrderedCountries = mCountryAdapter.getOrderedCountries();
    }

    @Test
    public void fieldsRenderTest() {
        Pair<String, String> usPair = new Pair(Locale.US.getCountry(), Locale.US.getDisplayCountry());
        int usIndex = mOrderedCountries.indexOf(usPair);
        mCountrySpinner.setSelection(usIndex);
        assertEquals(mAddressLine1.getHint(), mAddAddressWidget.getResources().getString(R.string.address_label_address));
        assertEquals(mAddressLine2.getHint(), mAddAddressWidget.getResources().getString(R.string.address_label_apt));
        assertEquals(mPostalCodeLayout.getHint(), mAddAddressWidget.getResources().getString(R.string.address_label_zip_code));
        assertEquals(mStateInput.getHint(), mAddAddressWidget.getResources().getString(R.string.address_label_state));

        Pair<String, String> canadaPair = new Pair(Locale.CANADA.getCountry(), Locale.CANADA.getDisplayCountry());
        int canadaIndex = mOrderedCountries.indexOf(canadaPair);
        mCountrySpinner.setSelection(canadaIndex);
        assertEquals(mAddressLine1.getHint(), mAddAddressWidget.getResources().getString(R.string.address_label_address));
        assertEquals(mAddressLine2.getHint(), mAddAddressWidget.getResources().getString(R.string.address_label_apt));
        assertEquals(mPostalCodeLayout.getHint(), mAddAddressWidget.getResources().getString(R.string.address_label_postal_code));
        assertEquals(mStateInput.getHint(), mAddAddressWidget.getResources().getString(R.string.address_label_province));

        Pair<String, String> ukPair = new Pair(Locale.UK.getCountry(), Locale.UK.getDisplayCountry());
        int ukIndex = mOrderedCountries.indexOf(ukPair);
        mCountrySpinner.setSelection(ukIndex);
        assertEquals(mAddressLine1.getHint(), mAddAddressWidget.getResources().getString(R.string.address_label_address_line1));
        assertEquals(mAddressLine2.getHint(), mAddAddressWidget.getResources().getString(R.string.address_label_address_line2));
        assertEquals(mPostalCodeLayout.getHint(), mAddAddressWidget.getResources().getString(R.string.address_label_postcode));
        assertEquals(mStateInput.getHint(), mAddAddressWidget.getResources().getString(R.string.address_label_county));

        Pair<String, String> noPostalCodePair = new Pair("ZW", new Locale("", "ZW").getDisplayCountry()); // Zimbabwe
        int noPostalCodeIndex = mOrderedCountries.indexOf(noPostalCodePair);
        mCountrySpinner.setSelection(noPostalCodeIndex);
        assertEquals(mAddressLine1.getHint(), mAddAddressWidget.getResources().getString(R.string.address_label_address_line1));
        assertEquals(mAddressLine2.getHint(), mAddAddressWidget.getResources().getString(R.string.address_label_address_line2));
        assertEquals(mPostalCodeLayout.getVisibility(), View.GONE);
        assertEquals(mStateInput.getHint(), mAddAddressWidget.getResources().getString(R.string.address_label_region_generic));
    }

}
