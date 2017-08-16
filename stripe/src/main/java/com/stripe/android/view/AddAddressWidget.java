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

    private TextInputLayout mAddressLine1;
    private TextInputLayout mAddressLine2;
    private TextInputLayout mPostalCode;
    private TextInputLayout mStateInput;


    public AddAddressWidget(Context context) {
        super(context);
        initView();
    }

    public AddAddressWidget(Context context, AttributeSet attrs) {
        super(context);
        initView();
    }

    public AddAddressWidget(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView();
    }

    private void initView() {
        setOrientation(VERTICAL);
        inflate(getContext(), R.layout.add_address_widget, this);
        mCountrySpinner = findViewById(R.id.spinner_country);
        mAddressLine1 = findViewById(R.id.tl_address_line1);
        mAddressLine2 = findViewById(R.id.tl_address_line2);
        mPostalCode = findViewById(R.id.tl_postal_code);
        mStateInput = findViewById(R.id.tl_state);
        final CountryAdapter countryAdapter = new CountryAdapter(getContext());
        mCountrySpinner.setAdapter(countryAdapter);
        mCountrySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                String selectedCountry = countryAdapter.getItem(i).first;
                if (selectedCountry.equals(Locale.US.getCountry())) {
                    renderUSForm();
                } else if (selectedCountry.equals(Locale.UK.getCountry())) {
                    renderGreatBritainForm();
                } else if (selectedCountry.equals(Locale.CANADA.getCountry())) {
                    renderCanadianForm();
                } else {
                    renderInternationalForm();
                }
                if (CountryUtils.doesCountryUsePostalCode(selectedCountry)) {
                    mPostalCode.setVisibility(VISIBLE);
                } else {
                    mPostalCode.setVisibility(GONE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });
    }

    private void renderUSForm() {
        mAddressLine1.setHint(getResources().getString(R.string.address_label_address));
        mAddressLine2.setHint(getResources().getString(R.string.address_label_apt));
        mPostalCode.setHint(getResources().getString(R.string.address_label_zip_code));
        mStateInput.setHint(getResources().getString(R.string.address_label_state));
    }

    private void renderGreatBritainForm() {
        mAddressLine1.setHint(getResources().getString(R.string.address_label_address_line1));
        mAddressLine2.setHint(getResources().getString(R.string.address_label_address_line2));
        mPostalCode.setHint(getResources().getString(R.string.address_label_postcode));
        mStateInput.setHint(getResources().getString(R.string.address_label_county));
    }

    private void renderCanadianForm() {
        mAddressLine1.setHint(getResources().getString(R.string.address_label_address));
        mAddressLine2.setHint(getResources().getString(R.string.address_label_apt));
        mPostalCode.setHint(getResources().getString(R.string.address_label_postal_code));
        mStateInput.setHint(getResources().getString(R.string.address_label_province));
    }

    private void renderInternationalForm() {
        mAddressLine1.setHint(getResources().getString(R.string.address_label_address_line1));
        mAddressLine2.setHint(getResources().getString(R.string.address_label_address_line2));
        mPostalCode.setHint(getResources().getString(R.string.address_label_zip_postal_code));
        mStateInput.setHint(getResources().getString(R.string.address_label_region_generic));
    }

}
