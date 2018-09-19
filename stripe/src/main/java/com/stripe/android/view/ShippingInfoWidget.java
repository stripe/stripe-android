package com.stripe.android.view;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.annotation.StringDef;
import android.support.design.widget.TextInputLayout;
import android.telephony.PhoneNumberFormattingTextWatcher;
import android.util.AttributeSet;
import android.widget.LinearLayout;

import com.stripe.android.R;
import com.stripe.android.model.Address;
import com.stripe.android.model.ShippingInformation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * A widget used to collect address data from a user.
 */
public class ShippingInfoWidget extends LinearLayout {

    /**
     * Constants that can be used to mark fields in this widget as optional or hidden.
     * Some fields cannot be hidden.
     */
    @Retention(RetentionPolicy.SOURCE)
    @StringDef({ADDRESS_LINE_ONE_FIELD, ADDRESS_LINE_TWO_FIELD, CITY_FIELD, POSTAL_CODE_FIELD,
            STATE_FIELD, PHONE_FIELD})
    public @interface CustomizableShippingField {
    }

    public static final String ADDRESS_LINE_ONE_FIELD = "address_line_one";
    // address line two is optional by default
    public static final String ADDRESS_LINE_TWO_FIELD = "address_line_two";
    public static final String CITY_FIELD = "city";
    public static final String POSTAL_CODE_FIELD = "postal_code";
    public static final String STATE_FIELD = "state";
    public static final String PHONE_FIELD = "phone";

    private List<String> mOptionalShippingInfoFields = new ArrayList<>();
    private List<String> mHiddenShippingInfoFields = new ArrayList<>();

    private CountryAutoCompleteTextView mCountryAutoCompleteTextView;
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

    public ShippingInfoWidget(Context context) {
        super(context);
        initView();
    }

    public ShippingInfoWidget(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView();
    }

    public ShippingInfoWidget(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView();
    }

    /**
     * @param optionalAddressFields address fields that should be optional.
     */
    public void setOptionalFields(@Nullable List<String> optionalAddressFields) {
        if (optionalAddressFields != null) {
            mOptionalShippingInfoFields = optionalAddressFields;
        } else {
            mOptionalShippingInfoFields = new ArrayList<>();
        }
        renderLabels();
        renderCountrySpecificLabels(mCountryAutoCompleteTextView.getSelectedCountryCode());
    }

    /**
     * @param hiddenAddressFields address fields that should be hidden. Hidden fields are
     *                            automatically optional.
     */
    public void setHiddenFields(@Nullable List<String> hiddenAddressFields) {
        if (hiddenAddressFields != null) {
            mHiddenShippingInfoFields = hiddenAddressFields;
        } else {
            mHiddenShippingInfoFields = new ArrayList<>();
        }
        renderLabels();
        renderCountrySpecificLabels(mCountryAutoCompleteTextView.getSelectedCountryCode());
    }

    public ShippingInformation getShippingInformation() {
        if (!validateAllFields()) {
            return null;
        }

        Address address = new Address.Builder().setCity(mCityEditText.getText().toString())
                .setCountry(mCountryAutoCompleteTextView.getSelectedCountryCode()).setLine1
                        (mAddressEditText.getText().toString()).setLine2
                        (mAddressEditText2.getText().toString()).setPostalCode
                        (mPostalCodeEditText.getText().toString()).setState(mStateEditText
                        .getText().toString()).build();
        ShippingInformation shippingInformation = new ShippingInformation(address, mNameEditText
                .getText().toString(), mPhoneNumberEditText.getText().toString());
        return shippingInformation;
    }

    /**
     * @param shippingInformation shippingInformation to populated into the widget input fields.
     */
    public void populateShippingInfo(@Nullable ShippingInformation shippingInformation) {
        if (shippingInformation == null) {
            return;
        }
        Address address = shippingInformation.getAddress();
        if (address != null) {
            mCityEditText.setText(address.getCity());
            if (address.getCountry() != null && !address.getCountry().isEmpty()) {
                mCountryAutoCompleteTextView.setCountrySelected(address.getCountry());
            }
            mAddressEditText.setText(address.getLine1());
            mAddressEditText2.setText(address.getLine2());
            mPostalCodeEditText.setText(address.getPostalCode());
            mStateEditText.setText(address.getState());
        }
        mNameEditText.setText(shippingInformation.getName());
        mPhoneNumberEditText.setText(shippingInformation.getPhone());
    }

    /**
     * Validates all fields and shows error messages if appropriate.
     *
     * @return {@code true} if all shown fields are valid, {@code false} otherwise
     */
    public boolean validateAllFields() {
        boolean postalCodeValid = true;
        String countrySelected = mCountryAutoCompleteTextView.getSelectedCountryCode();
        if (mPostalCodeEditText.getText().toString().isEmpty() && (mOptionalShippingInfoFields
                .contains(POSTAL_CODE_FIELD) || mHiddenShippingInfoFields.contains
                (POSTAL_CODE_FIELD))) {
            postalCodeValid = true;
        } else if (countrySelected.equals(Locale.US.getCountry())) {
            postalCodeValid = CountryUtils.isUSZipCodeValid(mPostalCodeEditText.getText()
                    .toString().trim());
        } else if (countrySelected.equals(Locale.UK.getCountry())) {
            postalCodeValid = CountryUtils.isUKPostcodeValid(mPostalCodeEditText.getText()
                    .toString().trim());
        } else if (countrySelected.equals(Locale.CANADA.getCountry())) {
            postalCodeValid = CountryUtils.isCanadianPostalCodeValid(mPostalCodeEditText.getText()
                    .toString().trim());
        } else if (CountryUtils.doesCountryUsePostalCode(countrySelected)) {
            postalCodeValid = !mPostalCodeEditText.getText().toString().isEmpty();
        }
        mPostalCodeEditText.setShouldShowError(!postalCodeValid);

        boolean requiredAddressLine1Empty = mAddressEditText.getText().toString().isEmpty() &&
                !mOptionalShippingInfoFields.contains(ADDRESS_LINE_ONE_FIELD) &&
                !mHiddenShippingInfoFields.contains(ADDRESS_LINE_ONE_FIELD);
        mAddressEditText.setShouldShowError(requiredAddressLine1Empty);

        boolean requiredCityEmpty = mCityEditText.getText().toString().isEmpty() &&
                !mOptionalShippingInfoFields.contains(CITY_FIELD) && !mHiddenShippingInfoFields
                .contains(CITY_FIELD);
        mCityEditText.setShouldShowError(requiredCityEmpty);

        boolean requiredNameEmpty = mNameEditText.getText().toString().isEmpty();
        mNameEditText.setShouldShowError(requiredNameEmpty);

        boolean requiredStateEmpty = mStateEditText.getText().toString().isEmpty() &&
                !mOptionalShippingInfoFields.contains(STATE_FIELD) && !mHiddenShippingInfoFields
                .contains(STATE_FIELD);
        mStateEditText.setShouldShowError(requiredStateEmpty);

        boolean requiredPhoneNumberEmpty = mPhoneNumberEditText.getText().toString().isEmpty() &&
                !mOptionalShippingInfoFields.contains(PHONE_FIELD) && !mHiddenShippingInfoFields
                .contains(PHONE_FIELD);
        mPhoneNumberEditText.setShouldShowError(requiredPhoneNumberEmpty);

        return postalCodeValid && !requiredAddressLine1Empty && !requiredCityEmpty &&
                !requiredStateEmpty && !requiredNameEmpty && !requiredPhoneNumberEmpty;
    }

    private void initView() {
        setOrientation(VERTICAL);
        inflate(getContext(), R.layout.add_address_widget, this);
        mCountryAutoCompleteTextView = findViewById(R.id.country_autocomplete_aaw);
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
        mCountryAutoCompleteTextView.setCountryChangeListener(new CountryAutoCompleteTextView
                .CountryChangeListener() {
            @Override
            public void onCountryChanged(String countryCode) {
                renderCountrySpecificLabels(mCountryAutoCompleteTextView.getSelectedCountryCode());
            }
        });
        mPhoneNumberEditText.addTextChangedListener(new PhoneNumberFormattingTextWatcher());
        setupErrorHandling();
        renderLabels();
        renderCountrySpecificLabels(mCountryAutoCompleteTextView.getSelectedCountryCode());
    }


    private void setupErrorHandling() {
        mAddressEditText.setErrorMessageListener(new ErrorListener(mAddressLine1TextInputLayout));
        mCityEditText.setErrorMessageListener(new ErrorListener(mCityTextInputLayout));
        mNameEditText.setErrorMessageListener(new ErrorListener(mNameTextInputLayout));
        mPostalCodeEditText.setErrorMessageListener(new ErrorListener(mPostalCodeTextInputLayout));
        mStateEditText.setErrorMessageListener(new ErrorListener(mStateTextInputLayout));
        mPhoneNumberEditText.setErrorMessageListener(new ErrorListener
                (mPhoneNumberTextInputLayout));
        mAddressEditText.setErrorMessage(getResources().getString(R.string.address_required));
        mCityEditText.setErrorMessage(getResources().getString(R.string.address_city_required));
        mNameEditText.setErrorMessage(getResources().getString(R.string.address_name_required));
        mPhoneNumberEditText.setErrorMessage(getResources().getString(R.string
                .address_phone_number_required));
    }

    private void renderLabels() {
        mNameTextInputLayout.setHint(getResources().getString(R.string.address_label_name));
        if (mOptionalShippingInfoFields.contains(CITY_FIELD)) {
            mCityTextInputLayout.setHint(getResources().getString(R.string
                    .address_label_city_optional));
        } else {
            mCityTextInputLayout.setHint(getResources().getString(R.string.address_label_city));
        }
        if (mOptionalShippingInfoFields.contains(PHONE_FIELD)) {
            mPhoneNumberTextInputLayout.setHint(getResources().getString(R.string
                    .address_label_phone_number_optional));
        } else {
            mPhoneNumberTextInputLayout.setHint(getResources().getString(R.string
                    .address_label_phone_number));
        }
        hideHiddenFields();
    }

    private void hideHiddenFields() {
        if (mHiddenShippingInfoFields.contains(ADDRESS_LINE_ONE_FIELD)) {
            mAddressLine1TextInputLayout.setVisibility(GONE);
        }
        if (mHiddenShippingInfoFields.contains(ADDRESS_LINE_TWO_FIELD)) {
            mAddressLine2TextInputLayout.setVisibility(GONE);
        }
        if (mHiddenShippingInfoFields.contains(STATE_FIELD)) {
            mStateTextInputLayout.setVisibility(GONE);
        }
        if (mHiddenShippingInfoFields.contains(CITY_FIELD)) {
            mCityTextInputLayout.setVisibility(GONE);
        }
        if (mHiddenShippingInfoFields.contains(POSTAL_CODE_FIELD)) {
            mPostalCodeTextInputLayout.setVisibility(GONE);
        }
        if (mHiddenShippingInfoFields.contains(PHONE_FIELD)) {
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

        if (CountryUtils.doesCountryUsePostalCode(countrySelected) && !mHiddenShippingInfoFields
                .contains(POSTAL_CODE_FIELD)) {
            mPostalCodeTextInputLayout.setVisibility(VISIBLE);
        } else {
            mPostalCodeTextInputLayout.setVisibility(GONE);
        }
    }

    private void renderUSForm() {
        if (mOptionalShippingInfoFields.contains(ADDRESS_LINE_ONE_FIELD)) {
            mAddressLine1TextInputLayout.setHint(getResources().getString(R.string
                    .address_label_address_optional));
        } else {
            mAddressLine1TextInputLayout.setHint(getResources().getString(R.string
                    .address_label_address));
        }
        mAddressLine2TextInputLayout.setHint(getResources().getString(R.string
                .address_label_apt_optional));
        if (mOptionalShippingInfoFields.contains(POSTAL_CODE_FIELD)) {
            mPostalCodeTextInputLayout.setHint(getResources().getString(R.string
                    .address_label_zip_code_optional));
        } else {
            mPostalCodeTextInputLayout.setHint(getResources().getString(R.string
                    .address_label_zip_code));
        }
        if (mOptionalShippingInfoFields.contains(STATE_FIELD)) {
            mStateTextInputLayout.setHint(getResources().getString(R.string
                    .address_label_state_optional));
        } else {
            mStateTextInputLayout.setHint(getResources().getString(R.string.address_label_state));
        }
        mPostalCodeEditText.setErrorMessage(getResources().getString(R.string.address_zip_invalid));
        mStateEditText.setErrorMessage(getResources().getString(R.string.address_state_required));
    }

    private void renderGreatBritainForm() {
        if (mOptionalShippingInfoFields.contains(ADDRESS_LINE_ONE_FIELD)) {
            mAddressLine1TextInputLayout.setHint(getResources().getString(R.string
                    .address_label_address_line1_optional));
        } else {
            mAddressLine1TextInputLayout.setHint(getResources().getString(R.string
                    .address_label_address_line1));
        }
        mAddressLine2TextInputLayout.setHint(getResources().getString(R.string
                .address_label_address_line2_optional));
        if (mOptionalShippingInfoFields.contains(POSTAL_CODE_FIELD)) {
            mPostalCodeTextInputLayout.setHint(getResources().getString(R.string
                    .address_label_postcode_optional));
        } else {
            mPostalCodeTextInputLayout.setHint(getResources().getString(R.string
                    .address_label_postcode));
        }
        if (mOptionalShippingInfoFields.contains(STATE_FIELD)) {
            mStateTextInputLayout.setHint(getResources().getString(R.string
                    .address_label_county_optional));
        } else {
            mStateTextInputLayout.setHint(getResources().getString(R.string.address_label_county));
        }
        mPostalCodeEditText.setErrorMessage(getResources().getString(R.string
                .address_postcode_invalid));
        mStateEditText.setErrorMessage(getResources().getString(R.string.address_county_required));
    }

    private void renderCanadianForm() {
        if (mOptionalShippingInfoFields.contains(ADDRESS_LINE_ONE_FIELD)) {
            mAddressLine1TextInputLayout.setHint(getResources().getString(R.string
                    .address_label_address_optional));
        } else {
            mAddressLine1TextInputLayout.setHint(getResources().getString(R.string
                    .address_label_address));
        }
        mAddressLine2TextInputLayout.setHint(getResources().getString(R.string
                .address_label_apt_optional));
        if (mOptionalShippingInfoFields.contains(POSTAL_CODE_FIELD)) {
            mPostalCodeTextInputLayout.setHint(getResources().getString(R.string
                    .address_label_postal_code_optional));
        } else {
            mPostalCodeTextInputLayout.setHint(getResources().getString(R.string
                    .address_label_postal_code));
        }
        if (mOptionalShippingInfoFields.contains(STATE_FIELD)) {
            mStateTextInputLayout.setHint(getResources().getString(R.string
                    .address_label_province_optional));
        } else {
            mStateTextInputLayout.setHint(getResources().getString(R.string
                    .address_label_province));
        }

        mPostalCodeEditText.setErrorMessage(getResources().getString(R.string
                .address_postal_code_invalid));
        mStateEditText.setErrorMessage(getResources().getString(R.string
                .address_province_required));
    }

    private void renderInternationalForm() {
        if (mOptionalShippingInfoFields.contains(ADDRESS_LINE_ONE_FIELD)) {
            mAddressLine1TextInputLayout.setHint(getResources().getString(R.string
                    .address_label_address_line1_optional));
        } else {
            mAddressLine1TextInputLayout.setHint(getResources().getString(R.string
                    .address_label_address_line1));
        }
        mAddressLine2TextInputLayout.setHint(getResources().getString(R.string
                .address_label_address_line2_optional));
        if (mOptionalShippingInfoFields.contains(POSTAL_CODE_FIELD)) {
            mPostalCodeTextInputLayout.setHint(getResources().getString(R.string
                    .address_label_zip_postal_code_optional));
        } else {
            mPostalCodeTextInputLayout.setHint(getResources().getString(R.string
                    .address_label_zip_postal_code));
        }
        if (mOptionalShippingInfoFields.contains(STATE_FIELD)) {
            mStateTextInputLayout.setHint(getResources().getString(R.string
                    .address_label_region_generic_optional));
        } else {
            mStateTextInputLayout.setHint(getResources().getString(R.string
                    .address_label_region_generic));
        }

        mPostalCodeEditText.setErrorMessage(getResources().getString(R.string
                .address_zip_postal_invalid));
        mStateEditText.setErrorMessage(getResources().getString(R.string
                .address_region_generic_required));
    }

}
