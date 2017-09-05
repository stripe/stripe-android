package com.stripe.android.view;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputLayout;
import android.util.AttributeSet;
import android.view.View;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.Spinner;

import com.stripe.android.R;
import com.stripe.android.model.Address;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * A widget used to collect address data from a user.
 */
public class AddAddressWidget extends LinearLayout {

    /**
     * Constants used to specify fields in the AddAddressWidget. Can be used to mark fields as
     * optional or hidden.
     */
    public static final String ADDRESS_LINE_ONE_FIELD = "address_line_one";
    public static final String ADDRESS_LINE_TWO_FIELD = "address_line_two";
    public static final String CITY_FIELD = "city";
    public static final String NAME_FIELD = "name";
    public static final String POSTAL_CODE_FIELD = "postal_code";
    public static final String STATE_FIELD = "state";
    public static final String PHONE_FIELD = "phone";

    private List<String> mOptionalAddressFields = new ArrayList<>();
    private List<String> mHiddenAddressFields = new ArrayList<>();

    private TextInputLayout mAddressLine1TextInputLayout;
    private TextInputLayout mAddressLine2TextInputLayout;
    private TextInputLayout mCityTextInputLayout;
    private TextInputLayout mNameTextInputLayout;
    private TextInputLayout mPostalCodeTextInputLayout;
    private TextInputLayout mStateTextInputLayout;
    private TextInputLayout mPhoneNumberTextInputLayout;
    private StripeEditText mAddressEditText;
    private StripeEditText mAddressEditText2;
    private StripeEditText mCityEditText;
    private StripeEditText mNameEditText;
    private StripeEditText mPostalCodeEditText;
    private StripeEditText mStateEditText;
    private StripeEditText mPhoneNumberEditText;
    private String mCountrySelected;

    public AddAddressWidget(Context context) {
        super(context);
        initView();
    }

    public AddAddressWidget(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView();
    }

    public AddAddressWidget(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView();
    }

    /**
     * @param optionalAddressFields address fields that should be optional.
     */
    public void setOptionalFields(@Nullable List<String> optionalAddressFields) {
        if (optionalAddressFields == null) {
            return;
        }
        mOptionalAddressFields = optionalAddressFields;
        renderLabels();
        renderCountrySpecificLabels(mCountrySelected);
    }

    /**
     * @param hiddenAddressFields address fields that should be hidden. Hidden fields are
     *                            automatically optional.
     */
    public void setHiddenFields(@Nullable List<String> hiddenAddressFields) {
        if (hiddenAddressFields ==  null) {
            return;
        }
        mHiddenAddressFields = hiddenAddressFields;
        renderLabels();
        renderCountrySpecificLabels(mCountrySelected);
    }

    /**
     * @return An {@link Address} object with the information the user has entered if it is valid, {@code null} otherwise
     */
    public @Nullable Address getAddress() {
        if (!validateAllFields()) {
            return null;
        }

        Address address = new Address.Builder()
                .setCity(mCityEditText.getText().toString())
                .setCountry(mCountrySelected)
                .setLine1(mAddressEditText.getText().toString())
                .setLine2(mAddressEditText2.getText().toString())
                .setName(mNameEditText.getText().toString())
                .setPhoneNumber(mPhoneNumberEditText.getText().toString())
                .setPostalCode(mPostalCodeEditText.getText().toString())
                .setState(mStateEditText.getText().toString()).build();
        return address;
    }

    /**
     * @param address address to populated into the widget input fields.
     */
    public void populateAddress(@Nullable Address address) {
        if (address == null) {
            return;
        }
        mCityEditText.setText(address.getCity());
        mCountrySelected = address.getCountry();
        mAddressEditText.setText(address.getLine1());
        mAddressEditText2.setText(address.getLine2());
        mNameEditText.setText(address.getName());
        mPhoneNumberEditText.setText(address.getPhoneNumber());
        mPostalCodeEditText.setText(address.getPostalCode());
        mStateEditText.setText(address.getState());
    }

    /**
     * Validates all fields and shows error messages if appropriate.
     *
     * @return {@code true} if all shown fields are valid, {@code false} otherwise
     */
    public boolean validateAllFields() {

        boolean postalCodeValid = true;
        if (mPostalCodeEditText.getText().toString().isEmpty() &&
                (mOptionalAddressFields.contains(POSTAL_CODE_FIELD) || mHiddenAddressFields.contains(POSTAL_CODE_FIELD))) {
            postalCodeValid = true;
        } else if (mCountrySelected.equals(Locale.US.getCountry())) {
            postalCodeValid = CountryUtils.isUSZipCodeValid(mPostalCodeEditText.getText().toString().trim());
        } else if (mCountrySelected.equals(Locale.UK.getCountry())) {
            postalCodeValid = CountryUtils.isUSZipCodeValid(mPostalCodeEditText.getText().toString().trim());
        } else if (mCountrySelected.equals(Locale.CANADA.getCountry())) {
            postalCodeValid = CountryUtils.isUSZipCodeValid(mPostalCodeEditText.getText().toString().trim());
        } else if (CountryUtils.doesCountryUsePostalCode(mCountrySelected)){
            postalCodeValid = !mPostalCodeEditText.getText().toString().isEmpty();
        }
        mPostalCodeEditText.setShouldShowError(!postalCodeValid);

        boolean requiredAddressLine1Empty = mAddressEditText.getText().toString().isEmpty() && !mOptionalAddressFields.contains(ADDRESS_LINE_ONE_FIELD) && !mHiddenAddressFields.contains(ADDRESS_LINE_ONE_FIELD);
        mAddressEditText.setShouldShowError(requiredAddressLine1Empty);

        boolean requiredAddressLine2Empty = mAddressEditText2.getText().toString().isEmpty() && !mOptionalAddressFields.contains(ADDRESS_LINE_TWO_FIELD) && !mHiddenAddressFields.contains(ADDRESS_LINE_TWO_FIELD);
        mAddressEditText2.setShouldShowError(requiredAddressLine2Empty);

        boolean requiredCityEmpty = mCityEditText.getText().toString().isEmpty() && !mOptionalAddressFields.contains(CITY_FIELD) && !mHiddenAddressFields.contains(CITY_FIELD);
        mCityEditText.setShouldShowError(requiredCityEmpty);

        boolean requiredNameEmpty = mNameEditText.getText().toString().isEmpty() && !mOptionalAddressFields.contains(NAME_FIELD) && !mHiddenAddressFields.contains(NAME_FIELD);
        mNameEditText.setShouldShowError(requiredNameEmpty);

        boolean requiredStateEmpty = mStateEditText.getText().toString().isEmpty() && !mOptionalAddressFields.contains(STATE_FIELD) && !mHiddenAddressFields.contains(STATE_FIELD);
        mStateEditText.setShouldShowError(requiredStateEmpty);

        boolean requiredPhoneNumberEmpty = mPhoneNumberEditText.getText().toString().isEmpty() && !mOptionalAddressFields.contains(PHONE_FIELD) && !mHiddenAddressFields.contains(PHONE_FIELD);
        mPhoneNumberEditText.setShouldShowError(requiredPhoneNumberEmpty);

        return postalCodeValid &&
                !requiredAddressLine1Empty &&
                !requiredCityEmpty &&
                !requiredStateEmpty &&
                !requiredNameEmpty &&
                !requiredPhoneNumberEmpty;
    }

    private void initView() {
        setOrientation(VERTICAL);
        inflate(getContext(), R.layout.add_address_widget, this);

        Spinner countrySpinner = findViewById(R.id.spinner_country_aaw);
        mAddressLine1TextInputLayout = findViewById(R.id.tl_address_line1_aaw);
        mAddressLine2TextInputLayout = findViewById(R.id.tl_address_line2_aaw);
        mCityTextInputLayout = findViewById(R.id.tl_city_aaw);
        mNameTextInputLayout = findViewById(R.id.tl_name_aaw);
        mPostalCodeTextInputLayout = findViewById(R.id.tl_postal_code_aaw);
        mStateTextInputLayout = findViewById(R.id.tl_state_aaw);
        mAddressEditText = findViewById(R.id.et_address_line_one_aaw);
        mAddressEditText2 = findViewById(R.id.et_address_line_two_aaw);
        mCityEditText = findViewById(R.id.et_city_aaw);
        mNameEditText = findViewById(R.id.et_name_aaw);
        mPostalCodeEditText = findViewById(R.id.et_postal_code_aaw);
        mStateEditText = findViewById(R.id.et_state_aaw);
        mPhoneNumberEditText = findViewById(R.id.et_phone_number_aaw);
        mPhoneNumberTextInputLayout = findViewById(R.id.tl_phone_number_aaw);

        setupErrorHandling();

        final CountryAdapter countryAdapter = new CountryAdapter(getContext());
        countrySpinner.setAdapter(countryAdapter);
        countrySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                mCountrySelected = countryAdapter.getItem(i).first;
                renderCountrySpecificLabels(mCountrySelected);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });
        mCountrySelected = countryAdapter.getItem(0).first;
        renderLabels();
        renderCountrySpecificLabels(mCountrySelected);
    }

    private void setupErrorHandling() {
        mAddressEditText.setErrorMessageListener(new ErrorListener(mAddressLine1TextInputLayout));
        mCityEditText.setErrorMessageListener(new ErrorListener(mCityTextInputLayout));
        mNameEditText.setErrorMessageListener(new ErrorListener(mNameTextInputLayout));
        mPostalCodeEditText.setErrorMessageListener(new ErrorListener(mPostalCodeTextInputLayout));
        mStateEditText.setErrorMessageListener(new ErrorListener(mStateTextInputLayout));
        mAddressEditText.setErrorMessage(getResources().getString(R.string.address_required));
        mCityEditText.setErrorMessage(getResources().getString(R.string.address_city_required));
        mNameEditText.setErrorMessage(getResources().getString(R.string.address_name_required));
//        mPhoneNumberEditText.setErrorMessage(getResources().getString(R.string.address_phone_required));
    }

    private void renderLabels() {
        if (mOptionalAddressFields.contains(NAME_FIELD)) {
            mNameTextInputLayout.setHint(getResources().getString(R.string.address_label_name_optional));
        } else {
            mNameTextInputLayout.setHint(getResources().getString(R.string.address_label_name));
        }
        if (mOptionalAddressFields.contains(CITY_FIELD)) {
            mCityTextInputLayout.setHint(getResources().getString(R.string.address_label_city_optional));
        } else {
            mCityTextInputLayout.setHint(getResources().getString(R.string.address_label_city));
        }
        if (mOptionalAddressFields.contains(PHONE_FIELD)) {
            mPhoneNumberTextInputLayout.setHint(getResources().getString(R.string.address_label_phone_number_optional));
        } else {
            mPhoneNumberTextInputLayout.setHint(getResources().getString(R.string.address_label_phone_number));
        }
        hideHiddenFields();
    }

    private void hideHiddenFields() {
        if (mHiddenAddressFields.contains(NAME_FIELD)) {
            mNameTextInputLayout.setVisibility(GONE);
        }
        if (mHiddenAddressFields.contains(ADDRESS_LINE_ONE_FIELD)) {
            mAddressLine1TextInputLayout.setVisibility(GONE);
        }
        if (mHiddenAddressFields.contains(ADDRESS_LINE_TWO_FIELD)) {
            mAddressLine2TextInputLayout.setVisibility(GONE);
        }
        if (mHiddenAddressFields.contains(STATE_FIELD)) {
            mStateTextInputLayout.setVisibility(GONE);
        }
        if (mHiddenAddressFields.contains(CITY_FIELD)) {
            mCityTextInputLayout.setVisibility(GONE);
        }
        if (mHiddenAddressFields.contains(POSTAL_CODE_FIELD)) {
            mPostalCodeTextInputLayout.setVisibility(GONE);
        }
        if (mHiddenAddressFields.contains(PHONE_FIELD)) {
            mPhoneNumberTextInputLayout.setVisibility(GONE);
        }
    }

    private void renderCountrySpecificLabels(String countrySelected) {
        if (countrySelected.equals(Locale.US.getCountry())) {
            renderUSForm();
        } else if (countrySelected.equals(Locale.UK.getCountry())) {
            renderGreatBritainForm();
        } else if (countrySelected.equals(Locale.CANADA.getCountry())) {
            renderCanadianForm();
        } else {
            renderInternationalForm();
        }

        if (CountryUtils.doesCountryUsePostalCode(countrySelected) &&
                !mHiddenAddressFields.contains(POSTAL_CODE_FIELD)) {
            mPostalCodeTextInputLayout.setVisibility(VISIBLE);
        } else {
            mPostalCodeTextInputLayout.setVisibility(GONE);
        }
    }

    private void renderUSForm() {
        if (mOptionalAddressFields.contains(ADDRESS_LINE_ONE_FIELD)) {
            mAddressLine1TextInputLayout.setHint(getResources().getString(R.string.address_label_address_optional));
        } else {
            mAddressLine1TextInputLayout.setHint(getResources().getString(R.string.address_label_address));
        }
        if (mOptionalAddressFields.contains(ADDRESS_LINE_TWO_FIELD)) {
            mAddressLine2TextInputLayout.setHint(getResources().getString(R.string.address_label_apt_optional));
        } else {
            mAddressLine2TextInputLayout.setHint(getResources().getString(R.string.address_label_apt));
        }
        if (mOptionalAddressFields.contains(POSTAL_CODE_FIELD)) {
            mPostalCodeTextInputLayout.setHint(getResources().getString(R.string.address_label_zip_code_optional));
        } else {
            mPostalCodeTextInputLayout.setHint(getResources().getString(R.string.address_label_zip_code));
        }
        if (mOptionalAddressFields.contains(STATE_FIELD)) {
            mStateTextInputLayout.setHint(getResources().getString(R.string.address_label_state_optional));
        } else {
            mStateTextInputLayout.setHint(getResources().getString(R.string.address_label_state));
        }
        mPostalCodeEditText.setErrorMessage(getResources().getString(R.string.address_zip_invalid));
        mStateEditText.setErrorMessage(getResources().getString(R.string.address_state_required));
    }

    private void renderGreatBritainForm() {
        if (mOptionalAddressFields.contains(ADDRESS_LINE_ONE_FIELD)) {
            mAddressLine1TextInputLayout.setHint(getResources().getString(R.string.address_label_address_line1_optional));
        } else {
            mAddressLine1TextInputLayout.setHint(getResources().getString(R.string.address_label_address_line1));
        }
        if (mOptionalAddressFields.contains(ADDRESS_LINE_TWO_FIELD)) {
            mAddressLine2TextInputLayout.setHint(getResources().getString(R.string.address_label_address_line2_optional));
        } else {
            mAddressLine2TextInputLayout.setHint(getResources().getString(R.string.address_label_address_line2));
        }
        if (mOptionalAddressFields.contains(POSTAL_CODE_FIELD)) {
            mPostalCodeTextInputLayout.setHint(getResources().getString(R.string.address_label_postcode_optional));
        } else {
            mPostalCodeTextInputLayout.setHint(getResources().getString(R.string.address_label_postcode));
        }
        if (mOptionalAddressFields.contains(STATE_FIELD)) {
            mStateTextInputLayout.setHint(getResources().getString(R.string.address_label_county_optional));
        } else {
            mStateTextInputLayout.setHint(getResources().getString(R.string.address_label_county));
        }
        mPostalCodeEditText.setErrorMessage(getResources().getString(R.string.address_postcode_invalid));
        mStateEditText.setErrorMessage(getResources().getString(R.string.address_county_required));
    }

    private void renderCanadianForm() {
        if (mOptionalAddressFields.contains(ADDRESS_LINE_ONE_FIELD)) {
            mAddressLine1TextInputLayout.setHint(getResources().getString(R.string.address_label_address_optional));
        } else {
            mAddressLine1TextInputLayout.setHint(getResources().getString(R.string.address_label_address));
        }
        if (mOptionalAddressFields.contains(ADDRESS_LINE_TWO_FIELD)) {
            mAddressLine2TextInputLayout.setHint(getResources().getString(R.string.address_label_apt_optional));
        } else {
            mAddressLine2TextInputLayout.setHint(getResources().getString(R.string.address_label_apt));
        }
        if (mOptionalAddressFields.contains(POSTAL_CODE_FIELD)) {
            mPostalCodeTextInputLayout.setHint(getResources().getString(R.string.address_label_postal_code_optional));
        } else {
            mPostalCodeTextInputLayout.setHint(getResources().getString(R.string.address_label_postal_code));
        }
        if (mOptionalAddressFields.contains(STATE_FIELD)) {
            mStateTextInputLayout.setHint(getResources().getString(R.string.address_label_province_optional));
        } else {
            mStateTextInputLayout.setHint(getResources().getString(R.string.address_label_province));
        }

        mPostalCodeEditText.setErrorMessage(getResources().getString(R.string.address_postal_code_invalid));
        mStateEditText.setErrorMessage(getResources().getString(R.string.address_province_required));
    }

    private void renderInternationalForm() {
        if (mOptionalAddressFields.contains(ADDRESS_LINE_ONE_FIELD)) {
            mAddressLine1TextInputLayout.setHint(getResources().getString(R.string.address_label_address_line1_optional));
        } else {
            mAddressLine1TextInputLayout.setHint(getResources().getString(R.string.address_label_address_line1));
        }
        if (mOptionalAddressFields.contains(ADDRESS_LINE_TWO_FIELD)) {
            mAddressLine2TextInputLayout.setHint(getResources().getString(R.string.address_label_address_line2_optional));
        } else {
            mAddressLine2TextInputLayout.setHint(getResources().getString(R.string.address_label_address_line2));
        }
        if (mOptionalAddressFields.contains(POSTAL_CODE_FIELD)) {
            mPostalCodeTextInputLayout.setHint(getResources().getString(R.string.address_label_zip_postal_code_optional));
        } else {
            mPostalCodeTextInputLayout.setHint(getResources().getString(R.string.address_label_zip_postal_code));
        }
        if (mOptionalAddressFields.contains(STATE_FIELD)) {
            mStateTextInputLayout.setHint(getResources().getString(R.string.address_label_region_generic_optional));
        } else {
            mStateTextInputLayout.setHint(getResources().getString(R.string.address_label_region_generic));
        }

        mPostalCodeEditText.setErrorMessage(getResources().getString(R.string.address_zip_postal_invalid));
        mStateEditText.setErrorMessage(getResources().getString(R.string.address_region_generic_required));
    }

}
