package com.stripe.android.view;

import android.support.design.widget.TextInputLayout;
import android.view.View;

import com.stripe.android.BuildConfig;
import com.stripe.android.R;
import com.stripe.android.model.Address;

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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Test class for {@link ShippingInfoWidget}
 */
@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 25)
public class ShippingInfoWidgetTest {

    private ShippingInfoWidget mShippingInfoWidget;
    private TextInputLayout mAddressLine1TextInputLayout;
    private TextInputLayout mAddressLine2TextInputLayout;
    private TextInputLayout mCityTextInputLayout;
    private TextInputLayout mNameTextInputLayout;
    private TextInputLayout mPostalCodeTextInputLayout;
    private TextInputLayout mStateTextInputLayout;
    private StripeEditText mAddressLine1EditText;
    private StripeEditText mAddressLine2EditText;
    private StripeEditText mPostalEditText;
    private StripeEditText mCityEditText;
    private StripeEditText mNameEditText;
    private StripeEditText mStateEditText;
    private StripeEditText mPhoneEditText;
    private CountryAutoCompleteTextView mCountryAutoCompleteTextView;

    private String mNoPostalCodeCountry = "ZW"; // Zimbabwe
    private Address mAddress;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        Locale.setDefault(Locale.US);
        ActivityController<AddressInputTestActivity> activityController =
                Robolectric.buildActivity(AddressInputTestActivity.class).create().start();
        mShippingInfoWidget = activityController.get().getShippingInfoWidget();
        mAddressLine1TextInputLayout = mShippingInfoWidget.findViewById(R.id.tl_address_line1_aaw);
        mAddressLine2TextInputLayout = mShippingInfoWidget.findViewById(R.id.tl_address_line2_aaw);
        mCityTextInputLayout = mShippingInfoWidget.findViewById(R.id.tl_city_aaw);
        mNameTextInputLayout = mShippingInfoWidget.findViewById(R.id.tl_name_aaw);
        mPostalCodeTextInputLayout = mShippingInfoWidget.findViewById(R.id.tl_postal_code_aaw);
        mStateTextInputLayout = mShippingInfoWidget.findViewById(R.id.tl_state_aaw);
        mAddressLine1EditText = mShippingInfoWidget.findViewById(R.id.et_address_line_one_aaw);
        mAddressLine2EditText = mShippingInfoWidget.findViewById(R.id.et_address_line_two_aaw);
        mCityEditText = mShippingInfoWidget.findViewById(R.id.et_city_aaw);
        mNameEditText = mShippingInfoWidget.findViewById(R.id.et_name_aaw);
        mPostalEditText = mShippingInfoWidget.findViewById(R.id.et_postal_code_aaw);
        mStateEditText = mShippingInfoWidget.findViewById(R.id.et_state_aaw);
        mPhoneEditText = mShippingInfoWidget.findViewById(R.id.et_phone_number_aaw);
        mCountryAutoCompleteTextView = mShippingInfoWidget.findViewById(R.id.country_autocomplete_aaw);
        mAddress = new Address.Builder()
                .setCity("San Francisco")
                .setName("Fake Name")
                .setState("CA")
                .setCountry("US")
                .setLine1("185 Berry St")
                .setLine2("10th Floor")
                .setPostalCode("12345")
                .setPhoneNumber("(123) 456 - 7890")
                .build();
    }

    @Test
    public void addAddressWidget_whenCountryChanged_fieldsRenderCorrectly() {
        mCountryAutoCompleteTextView.updateUIForCountryEntered(Locale.US.getDisplayCountry());
        assertEquals(mAddressLine1TextInputLayout.getHint(), mShippingInfoWidget.getResources().getString(R.string.address_label_address));
        assertEquals(mAddressLine2TextInputLayout.getHint(), mShippingInfoWidget.getResources().getString(R.string.address_label_apt_optional));
        assertEquals(mPostalCodeTextInputLayout.getHint(), mShippingInfoWidget.getResources().getString(R.string.address_label_zip_code));
        assertEquals(mStateTextInputLayout.getHint(), mShippingInfoWidget.getResources().getString(R.string.address_label_state));

        mCountryAutoCompleteTextView.updateUIForCountryEntered(Locale.CANADA.getDisplayCountry());
        assertEquals(mAddressLine1TextInputLayout.getHint(), mShippingInfoWidget.getResources().getString(R.string.address_label_address));
        assertEquals(mAddressLine2TextInputLayout.getHint(), mShippingInfoWidget.getResources().getString(R.string.address_label_apt_optional));
        assertEquals(mPostalCodeTextInputLayout.getHint(), mShippingInfoWidget.getResources().getString(R.string.address_label_postal_code));
        assertEquals(mStateTextInputLayout.getHint(), mShippingInfoWidget.getResources().getString(R.string.address_label_province));

        mCountryAutoCompleteTextView.updateUIForCountryEntered(Locale.UK.getDisplayCountry());
        assertEquals(mAddressLine1TextInputLayout.getHint(), mShippingInfoWidget.getResources().getString(R.string.address_label_address_line1));
        assertEquals(mAddressLine2TextInputLayout.getHint(), mShippingInfoWidget.getResources().getString(R.string.address_label_address_line2_optional));
        assertEquals(mPostalCodeTextInputLayout.getHint(), mShippingInfoWidget.getResources().getString(R.string.address_label_postcode));
        assertEquals(mStateTextInputLayout.getHint(), mShippingInfoWidget.getResources().getString(R.string.address_label_county));

        mCountryAutoCompleteTextView.updateUIForCountryEntered(new Locale("", mNoPostalCodeCountry).getDisplayCountry());
        assertEquals(mAddressLine1TextInputLayout.getHint(), mShippingInfoWidget.getResources().getString(R.string.address_label_address_line1));
        assertEquals(mAddressLine2TextInputLayout.getHint(), mShippingInfoWidget.getResources().getString(R.string.address_label_address_line2_optional));
        assertEquals(mPostalCodeTextInputLayout.getVisibility(), View.GONE);
        assertEquals(mStateTextInputLayout.getHint(), mShippingInfoWidget.getResources().getString(R.string.address_label_region_generic));
    }

    @Test
    public void addAddressWidget_addressSaved_validationTriggers() {
        mCountryAutoCompleteTextView.updateUIForCountryEntered(Locale.US.getDisplayCountry());
        assertFalse(mShippingInfoWidget.validateAllFields());
        mAddressLine1EditText.setText("123 Fake Address");
        mNameEditText.setText("Fake Name");
        mCityEditText.setText("Fake City");
        mPostalEditText.setText("12345");
        mStateEditText.setText("CA");
        mPhoneEditText.setText("(123) 456 - 7890");
        assertTrue(mShippingInfoWidget.validateAllFields());
        mPostalEditText.setText("");
        assertFalse(mShippingInfoWidget.validateAllFields());
        mPostalEditText.setText("ABCDEF");
        assertFalse(mShippingInfoWidget.validateAllFields());
        mCountryAutoCompleteTextView.updateUIForCountryEntered(new Locale("", mNoPostalCodeCountry).getDisplayCountry());
        assertTrue(mShippingInfoWidget.validateAllFields());
    }

    @Test
    public void addAddressTest_whenValidationFails_errorTextRenders() {
        mCountryAutoCompleteTextView.updateUIForCountryEntered(Locale.US.getDisplayCountry());
        mShippingInfoWidget.validateAllFields();
        assertTrue(mAddressLine1TextInputLayout.isErrorEnabled());
        assertTrue(mCityTextInputLayout.isErrorEnabled());
        assertTrue(mNameTextInputLayout.isErrorEnabled());
        assertTrue(mPostalCodeTextInputLayout.isErrorEnabled());
        assertTrue(mStateTextInputLayout.isErrorEnabled());
        mAddressLine1EditText.setText("123 Fake Address");
        mNameEditText.setText("Fake Name");
        mCityEditText.setText("Fake City");
        mPostalEditText.setText("12345");
        mStateEditText.setText("CA");
        mShippingInfoWidget.validateAllFields();
        assertFalse(mAddressLine1TextInputLayout.isErrorEnabled());
        assertFalse(mCityTextInputLayout.isErrorEnabled());
        assertFalse(mNameTextInputLayout.isErrorEnabled());
        assertFalse(mPostalCodeTextInputLayout.isErrorEnabled());
        assertFalse(mStateTextInputLayout.isErrorEnabled());
        mPostalEditText.setText("");
        mShippingInfoWidget.validateAllFields();
        assertTrue(mPostalCodeTextInputLayout.isErrorEnabled());
        mCountryAutoCompleteTextView.updateUIForCountryEntered(new Locale("", mNoPostalCodeCountry).getDisplayCountry());
        mShippingInfoWidget.validateAllFields();
        assertFalse(mStateTextInputLayout.isErrorEnabled());
    }

    @Test
    public void addAddressWidget_whenErrorOccurs_errorsRenderInternationalized() {
        mCountryAutoCompleteTextView.updateUIForCountryEntered(Locale.US.getDisplayCountry());
        mShippingInfoWidget.validateAllFields();
        assertEquals(mStateTextInputLayout.getError(), mShippingInfoWidget.getResources().getString(R.string.address_state_required));
        assertEquals(mPostalCodeTextInputLayout.getError(), mShippingInfoWidget.getResources().getString(R.string.address_zip_invalid));

        mCountryAutoCompleteTextView.updateUIForCountryEntered(Locale.UK.getDisplayCountry());
        mShippingInfoWidget.validateAllFields();
        assertEquals(mStateTextInputLayout.getError(), mShippingInfoWidget.getResources().getString(R.string.address_county_required));
        assertEquals(mPostalCodeTextInputLayout.getError(), mShippingInfoWidget.getResources().getString(R.string.address_postcode_invalid));

        mCountryAutoCompleteTextView.updateUIForCountryEntered( Locale.CANADA.getDisplayCountry());
        mShippingInfoWidget.validateAllFields();
        assertEquals(mStateTextInputLayout.getError(), mShippingInfoWidget.getResources().getString(R.string.address_province_required));
        assertEquals(mPostalCodeTextInputLayout.getError(), mShippingInfoWidget.getResources().getString(R.string.address_postal_code_invalid));

        mCountryAutoCompleteTextView.updateUIForCountryEntered(new Locale("", mNoPostalCodeCountry).getDisplayCountry());
        mShippingInfoWidget.validateAllFields();
        assertEquals(mStateTextInputLayout.getError(), mShippingInfoWidget.getResources().getString(R.string.address_region_generic_required));
    }

    @Test
    public void addAddressWidget_whenFieldsOptional_markedAsOptional(){
        assertEquals(mPostalCodeTextInputLayout.getHint().toString(), mShippingInfoWidget.getResources().getString(R.string.address_label_zip_code));
        assertEquals(mNameTextInputLayout.getHint().toString(), mShippingInfoWidget.getResources().getString(R.string.address_label_name));
        List<String> optionalFields = new ArrayList<>();
        optionalFields.add(ShippingInfoWidget.POSTAL_CODE_FIELD);
        optionalFields.add(ShippingInfoWidget.NAME_FIELD);
        mShippingInfoWidget.setOptionalFields(optionalFields);
        assertEquals(mPostalCodeTextInputLayout.getHint().toString(), mShippingInfoWidget.getResources().getString(R.string.address_label_zip_code_optional));
        assertEquals(mNameTextInputLayout.getHint().toString(), mShippingInfoWidget.getResources().getString(R.string.address_label_name_optional));
        mCountryAutoCompleteTextView.updateUIForCountryEntered(Locale.CANADA.getDisplayCountry());
        assertEquals(mStateTextInputLayout.getHint().toString(), mShippingInfoWidget.getResources().getString(R.string.address_label_province));
        optionalFields.add(ShippingInfoWidget.STATE_FIELD);
        mShippingInfoWidget.setOptionalFields(optionalFields);
        assertEquals(mStateTextInputLayout.getHint().toString(), mShippingInfoWidget.getResources().getString(R.string.address_label_province_optional));
    }

    @Test
    public void addAddressWidget_whenFieldsHidden_renderedHidden() {
        assertEquals(mNameTextInputLayout.getVisibility(), View.VISIBLE);
        assertEquals(mPostalCodeTextInputLayout.getVisibility(), View.VISIBLE);
        List<String> hiddenFields = new ArrayList<>();
        hiddenFields.add(ShippingInfoWidget.NAME_FIELD);
        hiddenFields.add(ShippingInfoWidget.POSTAL_CODE_FIELD);
        mShippingInfoWidget.setHiddenFields(hiddenFields);
        assertEquals(mNameTextInputLayout.getVisibility(), View.GONE);
        assertEquals(mPostalCodeTextInputLayout.getVisibility(), View.GONE);
        mCountryAutoCompleteTextView.updateUIForCountryEntered(Locale.CANADA.getDisplayCountry());
        assertEquals(mPostalCodeTextInputLayout.getVisibility(), View.GONE);
    }

    @Test
    public void validateAllFields_whenFieldsOptional_notChecked() {
        setAllFieldsOptional();
        assertTrue(mShippingInfoWidget.validateAllFields());
    }

    @Test
    public void validateAllFields_whenFieldsHidden_notChecked() {
        setAllFieldsHidden();
        assertTrue(mShippingInfoWidget.validateAllFields());
    }

    @Test
    public void getAddress_whenAddressInvalid_returnsNull() {
        assertNull(mShippingInfoWidget.getAddress());
    }

    @Test
    public void getAddress_whenAddressValid_returnsExpectedAddress() {
        mStateEditText.setText("CA");
        mCityEditText.setText("San Francisco");
        mAddressLine1EditText.setText("185 Berry St");
        mAddressLine2EditText.setText("10th Floor");
        mNameEditText.setText("Fake Name");
        mPhoneEditText.setText("(123) 456 - 7890");
        mPostalEditText.setText("12345");
        mCountryAutoCompleteTextView.updateUIForCountryEntered(Locale.US.getDisplayCountry());
        Address inputAddress = mShippingInfoWidget.getAddress();
        assertEquals(inputAddress.toMap(), mAddress.toMap());
    }

    @Test
    public void populateAddress_whenAddressProvided_populates() {
        mShippingInfoWidget.populateShippingInfo(mAddress);
        assertEquals(mStateEditText.getText().toString(), "CA");
        assertEquals(mCityEditText.getText().toString(), "San Francisco");
        assertEquals(mAddressLine1EditText.getText().toString(), "185 Berry St");
        assertEquals(mAddressLine2EditText.getText().toString(), "10th Floor");
        assertEquals(mPhoneEditText.getText().toString(), "(123) 456 - 7890");
        assertEquals(mPostalEditText.getText().toString(), "12345");
        assertEquals(mNameEditText.getText().toString(), "Fake Name");
        assertEquals(mCountryAutoCompleteTextView.getSelectedCountryCode(), "US");
    }

    @Test
    public void setHiddenFields_whenNull_noHiddenFields() {
        setAllFieldsHidden();
        assertTrue(mShippingInfoWidget.validateAllFields());
        mShippingInfoWidget.setHiddenFields(null);
        assertFalse(mShippingInfoWidget.validateAllFields());
    }

    @Test
    public void setOptionalFields_whenNull_noOptionalFields() {
        setAllFieldsOptional();
        assertTrue(mShippingInfoWidget.validateAllFields());
        mShippingInfoWidget.setOptionalFields(null);
        assertFalse(mShippingInfoWidget.validateAllFields());
    }

    private void setAllFieldsOptional() {
        List<String> optionalFields = new ArrayList<>();
        optionalFields.add(ShippingInfoWidget.POSTAL_CODE_FIELD);
        optionalFields.add(ShippingInfoWidget.NAME_FIELD);
        optionalFields.add(ShippingInfoWidget.STATE_FIELD);
        optionalFields.add(ShippingInfoWidget.ADDRESS_LINE_ONE_FIELD);
        optionalFields.add(ShippingInfoWidget.ADDRESS_LINE_TWO_FIELD);
        optionalFields.add(ShippingInfoWidget.PHONE_FIELD);
        optionalFields.add(ShippingInfoWidget.CITY_FIELD);
        mShippingInfoWidget.setOptionalFields(optionalFields);
    }

    private void setAllFieldsHidden() {
        List<String> hiddenFields = new ArrayList<>();
        hiddenFields.add(ShippingInfoWidget.POSTAL_CODE_FIELD);
        hiddenFields.add(ShippingInfoWidget.NAME_FIELD);
        hiddenFields.add(ShippingInfoWidget.STATE_FIELD);
        hiddenFields.add(ShippingInfoWidget.ADDRESS_LINE_ONE_FIELD);
        hiddenFields.add(ShippingInfoWidget.ADDRESS_LINE_TWO_FIELD);
        hiddenFields.add(ShippingInfoWidget.PHONE_FIELD);
        hiddenFields.add(ShippingInfoWidget.CITY_FIELD);
        mShippingInfoWidget.setHiddenFields(hiddenFields);
    }
}
