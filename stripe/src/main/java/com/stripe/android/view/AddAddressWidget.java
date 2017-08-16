package com.stripe.android.view;

import android.content.Context;
import android.support.design.widget.TextInputLayout;
import android.support.v7.widget.AppCompatSpinner;
import android.util.AttributeSet;
import android.widget.LinearLayout;

import com.stripe.android.R;

/**
 * An add address widget using the support design library's {@link TextInputLayout}
 * to match Material Design.
 */
public class AddAddressWidget extends LinearLayout {

    private AppCompatSpinner mCountrySpinner;
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
        CountryAdapter countryAdapter = new CountryAdapter(getContext());
        mCountrySpinner.setAdapter(countryAdapter);
    }
}
