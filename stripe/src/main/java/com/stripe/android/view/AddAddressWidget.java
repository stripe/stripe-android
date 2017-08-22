package com.stripe.android.view;

import android.content.Context;
import android.support.design.widget.TextInputLayout;
import android.support.v7.widget.AppCompatSpinner;
import android.util.AttributeSet;
import android.view.View;
import android.widget.AdapterView;
import android.widget.LinearLayout;

import com.stripe.android.R;

import java.util.Locale;

/**
 * An add address widget using the support design library's {@link TextInputLayout}
 * to match Material Design.
 */
public class AddAddressWidget extends LinearLayout {

    private AppCompatSpinner mCountrySpinner;

    private TextInputLayout mAddressLine1TextInputLayout;
    private TextInputLayout mAddressLine2TextInputLayout;
    private TextInputLayout mCityTextInputLayout;
    private TextInputLayout mNameTextInputLayout;
    private TextInputLayout mPostalCodeTextInputLayout;
    private TextInputLayout mStateTextInputLayout;
    private StripeEditText mAddressEditText;
    private StripeEditText mCityEditText;
    private StripeEditText mNameEditText;
    private StripeEditText mPostalCodeEditText;
    private StripeEditText mStateEditText;
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
     * Validates all fields and shows error messages if appropriate.
     *
     * @return {@code true} if all shown fields are valid, {@code false} otherwise
     */
    public boolean validateAllFields() {
        boolean postalCodeValid = true;
        if (mCountrySelected.equals(Locale.US.getCountry())) {
            postalCodeValid = CountryUtils.isUSZipCodeValid(mPostalCodeEditText.getText().toString());
        } else if (mCountrySelected.equals(Locale.UK.getCountry())) {
            postalCodeValid = CountryUtils.isUSZipCodeValid(mPostalCodeEditText.getText().toString());
        } else if (mCountrySelected.equals(Locale.CANADA.getCountry())) {
            postalCodeValid = CountryUtils.isUSZipCodeValid(mPostalCodeEditText.getText().toString());
        } else if (CountryUtils.doesCountryUsePostalCode(mCountrySelected)){
            postalCodeValid = !mPostalCodeEditText.getText().toString().isEmpty();
        }
        mPostalCodeEditText.setShouldShowError(!postalCodeValid);

        boolean addressEmpty = mAddressEditText.getText().toString().isEmpty();
        mAddressEditText.setShouldShowError(addressEmpty);

        boolean cityEmpty = mCityEditText.getText().toString().isEmpty();
        mCityEditText.setShouldShowError(cityEmpty);

        boolean nameEmpty = mNameEditText.getText().toString().isEmpty();
        mNameEditText.setShouldShowError(nameEmpty);

        boolean stateEmpty = mStateEditText.getText().toString().isEmpty();
        mStateEditText.setShouldShowError(stateEmpty);

        return postalCodeValid && !addressEmpty && !cityEmpty && !stateEmpty && !nameEmpty;
    }

    private void initView() {
        setOrientation(VERTICAL);
        inflate(getContext(), R.layout.add_address_widget, this);
        mCountrySpinner = findViewById(R.id.spinner_country_aaw);
        mAddressLine1TextInputLayout = findViewById(R.id.tl_address_line1_aaw);
        mAddressLine2TextInputLayout = findViewById(R.id.tl_address_line2_aaw);
        mCityTextInputLayout = findViewById(R.id.tl_city_aaw);
        mNameTextInputLayout = findViewById(R.id.tl_name_aaw);
        mPostalCodeTextInputLayout = findViewById(R.id.tl_postal_code_aaw);
        mStateTextInputLayout = findViewById(R.id.tl_state_aaw);
        mAddressEditText = findViewById(R.id.et_address_aaw);
        mCityEditText = findViewById(R.id.et_city_aaw);
        mNameEditText = findViewById(R.id.et_name_aaw);
        mPostalCodeEditText = findViewById(R.id.et_postal_code_aaw);
        mStateEditText = findViewById(R.id.et_state_aaw);
        setupErrorHandling();
        final CountryAdapter countryAdapter = new CountryAdapter(getContext());
        mCountrySpinner.setAdapter(countryAdapter);
        mCountrySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                mCountrySelected = countryAdapter.getItem(i).first;
                if (mCountrySelected.equals(Locale.US.getCountry())) {
                    renderUSForm();
                } else if (mCountrySelected.equals(Locale.UK.getCountry())) {
                    renderGreatBritainForm();
                } else if (mCountrySelected.equals(Locale.CANADA.getCountry())) {
                    renderCanadianForm();
                } else {
                    renderInternationalForm();
                }
                if (CountryUtils.doesCountryUsePostalCode(mCountrySelected)) {
                    mPostalCodeTextInputLayout.setVisibility(VISIBLE);
                } else {
                    mPostalCodeTextInputLayout.setVisibility(GONE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });
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
    }

    private void renderUSForm() {
        mAddressLine1TextInputLayout.setHint(getResources().getString(R.string.address_label_address));
        mAddressLine2TextInputLayout.setHint(getResources().getString(R.string.address_label_apt));
        mPostalCodeTextInputLayout.setHint(getResources().getString(R.string.address_label_zip_code));
        mStateTextInputLayout.setHint(getResources().getString(R.string.address_label_state));
        mPostalCodeEditText.setErrorMessage(getResources().getString(R.string.address_zip_invalid));
        mStateEditText.setErrorMessage(getResources().getString(R.string.address_state_required));
    }

    private void renderGreatBritainForm() {
        mAddressLine1TextInputLayout.setHint(getResources().getString(R.string.address_label_address_line1));
        mAddressLine2TextInputLayout.setHint(getResources().getString(R.string.address_label_address_line2));
        mPostalCodeTextInputLayout.setHint(getResources().getString(R.string.address_label_postcode));
        mStateTextInputLayout.setHint(getResources().getString(R.string.address_label_county));
        mPostalCodeEditText.setErrorMessage(getResources().getString(R.string.address_postcode_invalid));
        mStateEditText.setErrorMessage(getResources().getString(R.string.address_county_required));
    }

    private void renderCanadianForm() {
        mAddressLine1TextInputLayout.setHint(getResources().getString(R.string.address_label_address));
        mAddressLine2TextInputLayout.setHint(getResources().getString(R.string.address_label_apt));
        mPostalCodeTextInputLayout.setHint(getResources().getString(R.string.address_label_postal_code));
        mStateTextInputLayout.setHint(getResources().getString(R.string.address_label_province));
        mPostalCodeEditText.setErrorMessage(getResources().getString(R.string.address_postal_code_invalid));
        mStateEditText.setErrorMessage(getResources().getString(R.string.address_province_required));
    }

    private void renderInternationalForm() {
        mAddressLine1TextInputLayout.setHint(getResources().getString(R.string.address_label_address_line1));
        mAddressLine2TextInputLayout.setHint(getResources().getString(R.string.address_label_address_line2));
        mPostalCodeTextInputLayout.setHint(getResources().getString(R.string.address_label_zip_postal_code));
        mStateTextInputLayout.setHint(getResources().getString(R.string.address_label_region_generic));
        mPostalCodeEditText.setErrorMessage(getResources().getString(R.string.address_zip_postal_invalid));
        mStateEditText.setErrorMessage(getResources().getString(R.string.address_region_generic_required));
    }

}
