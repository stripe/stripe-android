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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Test class for {@link AddAddressWidget}
 */
@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 25)
public class AddAddressWidgetTest {

    private AddAddressWidget mAddAddressWidget;
    private TextInputLayout mAddressLine1TextInputLayout;
    private TextInputLayout mAddressLine2TextInputLayout;
    private TextInputLayout mCityTextInputLayout;
    private TextInputLayout mNameTextInputLayout;
    private TextInputLayout mPostalCodeTextInputLayout;
    private TextInputLayout mStateTextInputLayout;
    private StripeEditText mAddressEditText;
    private StripeEditText mPostalEditText;
    private StripeEditText mCityEditText;
    private StripeEditText mNameEditText;
    private StripeEditText mStateEditText;
    private StripeEditText mPhoneEditText;
    private Spinner mCountrySpinner;
    private CountryAdapter mCountryAdapter;
    private List<Pair<String, String>> mOrderedCountries;

    private String mNoPostalCodeCountry = "ZW"; // Zimbabwe

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        Locale.setDefault(Locale.US);
        ActivityController<AddressInputTestActivity> activityController =
                Robolectric.buildActivity(AddressInputTestActivity.class).create().start();
        mAddAddressWidget = activityController.get().getAddAddressWidget();
        mAddressLine1TextInputLayout = mAddAddressWidget.findViewById(R.id.tl_address_line1_aaw);
        mAddressLine2TextInputLayout = mAddAddressWidget.findViewById(R.id.tl_address_line2_aaw);
        mCityTextInputLayout = mAddAddressWidget.findViewById(R.id.tl_city_aaw);
        mNameTextInputLayout = mAddAddressWidget.findViewById(R.id.tl_name_aaw);
        mPostalCodeTextInputLayout = mAddAddressWidget.findViewById(R.id.tl_postal_code_aaw);
        mStateTextInputLayout = mAddAddressWidget.findViewById(R.id.tl_state_aaw);
        mAddressEditText = mAddAddressWidget.findViewById(R.id.et_address_line_one_aaw);
        mCityEditText = mAddAddressWidget.findViewById(R.id.et_city_aaw);
        mNameEditText = mAddAddressWidget.findViewById(R.id.et_name_aaw);
        mPostalEditText = mAddAddressWidget.findViewById(R.id.et_postal_code_aaw);
        mStateEditText = mAddAddressWidget.findViewById(R.id.et_state_aaw);
        mPhoneEditText = mAddAddressWidget.findViewById(R.id.et_phone_number_aaw);
        mCountrySpinner = mAddAddressWidget.findViewById(R.id.spinner_country_aaw);
        mCountryAdapter = (CountryAdapter) mCountrySpinner.getAdapter();
        mOrderedCountries = mCountryAdapter.getOrderedCountries();
    }

    @Test
    public void addAddressWidget_whenCountryChanged_fieldsRenderCorrectly() {
        Pair<String, String> usPair = new Pair(Locale.US.getCountry(), Locale.US.getDisplayCountry());
        int usIndex = mOrderedCountries.indexOf(usPair);
        mCountrySpinner.setSelection(usIndex);
        assertEquals(mAddressLine1TextInputLayout.getHint(), mAddAddressWidget.getResources().getString(R.string.address_label_address));
        assertEquals(mAddressLine2TextInputLayout.getHint(), mAddAddressWidget.getResources().getString(R.string.address_label_apt));
        assertEquals(mPostalCodeTextInputLayout.getHint(), mAddAddressWidget.getResources().getString(R.string.address_label_zip_code));
        assertEquals(mStateTextInputLayout.getHint(), mAddAddressWidget.getResources().getString(R.string.address_label_state));

        Pair<String, String> canadaPair = new Pair(Locale.CANADA.getCountry(), Locale.CANADA.getDisplayCountry());
        int canadaIndex = mOrderedCountries.indexOf(canadaPair);
        mCountrySpinner.setSelection(canadaIndex);
        assertEquals(mAddressLine1TextInputLayout.getHint(), mAddAddressWidget.getResources().getString(R.string.address_label_address));
        assertEquals(mAddressLine2TextInputLayout.getHint(), mAddAddressWidget.getResources().getString(R.string.address_label_apt));
        assertEquals(mPostalCodeTextInputLayout.getHint(), mAddAddressWidget.getResources().getString(R.string.address_label_postal_code));
        assertEquals(mStateTextInputLayout.getHint(), mAddAddressWidget.getResources().getString(R.string.address_label_province));

        Pair<String, String> ukPair = new Pair(Locale.UK.getCountry(), Locale.UK.getDisplayCountry());
        int ukIndex = mOrderedCountries.indexOf(ukPair);
        mCountrySpinner.setSelection(ukIndex);
        assertEquals(mAddressLine1TextInputLayout.getHint(), mAddAddressWidget.getResources().getString(R.string.address_label_address_line1));
        assertEquals(mAddressLine2TextInputLayout.getHint(), mAddAddressWidget.getResources().getString(R.string.address_label_address_line2));
        assertEquals(mPostalCodeTextInputLayout.getHint(), mAddAddressWidget.getResources().getString(R.string.address_label_postcode));
        assertEquals(mStateTextInputLayout.getHint(), mAddAddressWidget.getResources().getString(R.string.address_label_county));

        Pair<String, String> noPostalCodePair = new Pair(mNoPostalCodeCountry, new Locale("", mNoPostalCodeCountry).getDisplayCountry());
        int noPostalCodeIndex = mOrderedCountries.indexOf(noPostalCodePair);
        mCountrySpinner.setSelection(noPostalCodeIndex);
        assertEquals(mAddressLine1TextInputLayout.getHint(), mAddAddressWidget.getResources().getString(R.string.address_label_address_line1));
        assertEquals(mAddressLine2TextInputLayout.getHint(), mAddAddressWidget.getResources().getString(R.string.address_label_address_line2));
        assertEquals(mPostalCodeTextInputLayout.getVisibility(), View.GONE);
        assertEquals(mStateTextInputLayout.getHint(), mAddAddressWidget.getResources().getString(R.string.address_label_region_generic));
    }

    @Test
    public void addAddressWidget_addressSaved_validationTriggers() {
        Pair<String, String> usPair = new Pair(Locale.US.getCountry(), Locale.US.getDisplayCountry());
        int usIndex = mOrderedCountries.indexOf(usPair);
        mCountrySpinner.setSelection(usIndex);
        assertFalse(mAddAddressWidget.validateAllFields());
        mAddressEditText.setText("123 Fake Address");
        mNameEditText.setText("Fake Name");
        mCityEditText.setText("Fake City");
        mPostalEditText.setText("12345");
        mStateEditText.setText("CA");
        mPhoneEditText.setText("(123) 456 - 7890");
        assertTrue(mAddAddressWidget.validateAllFields());
        mPostalEditText.setText("");
        assertFalse(mAddAddressWidget.validateAllFields());
        mPostalEditText.setText("ABCDEF");
        assertFalse(mAddAddressWidget.validateAllFields());
        Pair<String, String> noPostalCodePair = new Pair(mNoPostalCodeCountry, new Locale("", mNoPostalCodeCountry).getDisplayCountry());
        int noPostalCodeIndex = mOrderedCountries.indexOf(noPostalCodePair);
        mCountrySpinner.setSelection(noPostalCodeIndex);
        assertTrue(mAddAddressWidget.validateAllFields());
    }

    @Test
    public void addAddressTest_whenValidationFails_errorTextRenders() {
        Pair<String, String> usPair = new Pair(Locale.US.getCountry(), Locale.US.getDisplayCountry());
        int usIndex = mOrderedCountries.indexOf(usPair);
        mCountrySpinner.setSelection(usIndex);
        mAddAddressWidget.validateAllFields();
        assertTrue(mAddressLine1TextInputLayout.isErrorEnabled());
        assertTrue(mCityTextInputLayout.isErrorEnabled());
        assertTrue(mNameTextInputLayout.isErrorEnabled());
        assertTrue(mPostalCodeTextInputLayout.isErrorEnabled());
        assertTrue(mStateTextInputLayout.isErrorEnabled());
        mAddressEditText.setText("123 Fake Address");
        mNameEditText.setText("Fake Name");
        mCityEditText.setText("Fake City");
        mPostalEditText.setText("12345");
        mStateEditText.setText("CA");
        mAddAddressWidget.validateAllFields();
        assertFalse(mAddressLine1TextInputLayout.isErrorEnabled());
        assertFalse(mCityTextInputLayout.isErrorEnabled());
        assertFalse(mNameTextInputLayout.isErrorEnabled());
        assertFalse(mPostalCodeTextInputLayout.isErrorEnabled());
        assertFalse(mStateTextInputLayout.isErrorEnabled());
        mPostalEditText.setText("");
        mAddAddressWidget.validateAllFields();
        assertTrue(mPostalCodeTextInputLayout.isErrorEnabled());
        Pair<String, String> noPostalCodePair = new Pair(mNoPostalCodeCountry, new Locale("", mNoPostalCodeCountry).getDisplayCountry());
        int noPostalCodeIndex = mOrderedCountries.indexOf(noPostalCodePair);
        mCountrySpinner.setSelection(noPostalCodeIndex);
        mAddAddressWidget.validateAllFields();
        assertFalse(mStateTextInputLayout.isErrorEnabled());
    }

    @Test
    public void addAddressWidget_whenErrorOccurs_errorsRenderInternationalized() {
        Pair<String, String> usPair = new Pair(Locale.US.getCountry(), Locale.US.getDisplayCountry());
        int usIndex = mOrderedCountries.indexOf(usPair);
        mCountrySpinner.setSelection(usIndex);
        mAddAddressWidget.validateAllFields();
        assertEquals(mStateTextInputLayout.getError(), mAddAddressWidget.getResources().getString(R.string.address_state_required));
        assertEquals(mPostalCodeTextInputLayout.getError(), mAddAddressWidget.getResources().getString(R.string.address_zip_invalid));

        Pair<String, String> ukPair = new Pair(Locale.UK.getCountry(), Locale.UK.getDisplayCountry());
        int ukIndex = mOrderedCountries.indexOf(ukPair);
        mCountrySpinner.setSelection(ukIndex);
        mAddAddressWidget.validateAllFields();
        assertEquals(mStateTextInputLayout.getError(), mAddAddressWidget.getResources().getString(R.string.address_county_required));
        assertEquals(mPostalCodeTextInputLayout.getError(), mAddAddressWidget.getResources().getString(R.string.address_postcode_invalid));

        Pair<String, String> canadaPair = new Pair(Locale.CANADA.getCountry(), Locale.CANADA.getDisplayCountry());
        int canadaIndex = mOrderedCountries.indexOf(canadaPair);
        mCountrySpinner.setSelection(canadaIndex);
        mAddAddressWidget.validateAllFields();
        assertEquals(mStateTextInputLayout.getError(), mAddAddressWidget.getResources().getString(R.string.address_province_required));
        assertEquals(mPostalCodeTextInputLayout.getError(), mAddAddressWidget.getResources().getString(R.string.address_postal_code_invalid));

        Pair<String, String> noPostalCodePair = new Pair(mNoPostalCodeCountry, new Locale("", mNoPostalCodeCountry).getDisplayCountry());
        int noPostalCodeIndex = mOrderedCountries.indexOf(noPostalCodePair);
        mCountrySpinner.setSelection(noPostalCodeIndex);
        mAddAddressWidget.validateAllFields();
        assertEquals(mStateTextInputLayout.getError(), mAddAddressWidget.getResources().getString(R.string.address_region_generic_required));
    }

    @Test
    public void addAddressWidget_whenFieldsOptional_markedAsOptional(){
        assertEquals(mPostalCodeTextInputLayout.getHint().toString(), mAddAddressWidget.getResources().getString(R.string.address_label_zip_code));
        assertEquals(mNameTextInputLayout.getHint().toString(), mAddAddressWidget.getResources().getString(R.string.address_label_name));
        List<String> optionalFields = new ArrayList<>();
        optionalFields.add(AddAddressWidget.POSTAL_CODE_FIELD);
        optionalFields.add(AddAddressWidget.NAME_FIELD);
        mAddAddressWidget.setOptionalFields(optionalFields);
        assertEquals(mPostalCodeTextInputLayout.getHint().toString(), mAddAddressWidget.getResources().getString(R.string.address_label_zip_code_optional));
        assertEquals(mNameTextInputLayout.getHint().toString(), mAddAddressWidget.getResources().getString(R.string.address_label_name_optional));
        Pair<String, String> canadaPair = new Pair(Locale.CANADA.getCountry(), Locale.CANADA.getDisplayCountry());
        int canadaIndex = mOrderedCountries.indexOf(canadaPair);
        mCountrySpinner.setSelection(canadaIndex);
        assertEquals(mStateTextInputLayout.getHint().toString(), mAddAddressWidget.getResources().getString(R.string.address_label_province));
        optionalFields.add(AddAddressWidget.STATE_FIELD);
        mAddAddressWidget.setOptionalFields(optionalFields);
        assertEquals(mStateTextInputLayout.getHint().toString(), mAddAddressWidget.getResources().getString(R.string.address_label_province_optional));
    }

    @Test
    public void addAddressWidget_whenFieldsHidden_renderedHidden() {
        assertEquals(mNameTextInputLayout.getVisibility(), View.VISIBLE);
        assertEquals(mPostalCodeTextInputLayout.getVisibility(), View.VISIBLE);
        List<String> hiddenFields = new ArrayList<>();
        hiddenFields.add(AddAddressWidget.NAME_FIELD);
        hiddenFields.add(AddAddressWidget.POSTAL_CODE_FIELD);
        mAddAddressWidget.setHiddenFields(hiddenFields);
        assertEquals(mNameTextInputLayout.getVisibility(), View.GONE);
        assertEquals(mPostalCodeTextInputLayout.getVisibility(), View.GONE);
        Pair<String, String> canadaPair = new Pair(Locale.CANADA.getCountry(), Locale.CANADA.getDisplayCountry());
        int canadaIndex = mOrderedCountries.indexOf(canadaPair);
        mCountrySpinner.setSelection(canadaIndex);
        assertEquals(mPostalCodeTextInputLayout.getVisibility(), View.GONE);
    }

    @Test
    public void validateAllFields_whenFieldsOptional_notChecked() {
        List<String> optionalFields = new ArrayList<>();
        optionalFields.add(AddAddressWidget.POSTAL_CODE_FIELD);
        optionalFields.add(AddAddressWidget.NAME_FIELD);
        optionalFields.add(AddAddressWidget.STATE_FIELD);
        optionalFields.add(AddAddressWidget.ADDRESS_LINE_ONE_FIELD);
        optionalFields.add(AddAddressWidget.ADDRESS_LINE_TWO_FIELD);
        optionalFields.add(AddAddressWidget.PHONE_FIELD);
        optionalFields.add(AddAddressWidget.CITY_FIELD);
        mAddAddressWidget.setOptionalFields(optionalFields);
        assertTrue(mAddAddressWidget.validateAllFields());
    }

    @Test
    public void validateAllFields_whenFieldsHidden_notChecked() {
        List<String> optionalFields = new ArrayList<>();
        optionalFields.add(AddAddressWidget.POSTAL_CODE_FIELD);
        optionalFields.add(AddAddressWidget.NAME_FIELD);
        optionalFields.add(AddAddressWidget.STATE_FIELD);
        optionalFields.add(AddAddressWidget.ADDRESS_LINE_ONE_FIELD);
        optionalFields.add(AddAddressWidget.ADDRESS_LINE_TWO_FIELD);
        optionalFields.add(AddAddressWidget.PHONE_FIELD);
        optionalFields.add(AddAddressWidget.CITY_FIELD);
        mAddAddressWidget.setHiddenFields(optionalFields);
        assertTrue(mAddAddressWidget.validateAllFields());
    }
}
