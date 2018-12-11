package com.stripe.android.view;

import android.content.Context;
import android.support.annotation.RestrictTo;
import android.support.annotation.VisibleForTesting;
import android.util.AttributeSet;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.FrameLayout;

import com.stripe.android.R;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;

@RestrictTo(RestrictTo.Scope.LIBRARY)
public class CountryAutoCompleteTextView extends FrameLayout {
    private AutoCompleteTextView mCountryAutocomplete;
    private Map<String, String> mCountryNameToCode;
    @VisibleForTesting
    protected String mCountrySelected;
    private CountryChangeListener mCountryChangeListener;

    public CountryAutoCompleteTextView(Context context) {
        super(context);
        initView();
    }

    public CountryAutoCompleteTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView();
    }

    public CountryAutoCompleteTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView();
    }

    /**
     * @return 2 digit country code of the country selected by this input.
     */
    String getSelectedCountryCode() {
        return mCountrySelected;
    }

    /**
     * @param countryCode specify a country code to display in the input. The input will display
     *                    the full country display name.
     */
    void setCountrySelected(String countryCode) {
        if (countryCode == null) {
            return;
        }
        Locale locale = new Locale("", countryCode);
        updateUIForCountryEntered(locale.getDisplayCountry());
    }

    void setCountryChangeListener(CountryChangeListener countryChangeListener) {
        mCountryChangeListener = countryChangeListener;
    }

    private void initView() {
        inflate(getContext(), R.layout.country_autocomplete_textview, this);
        mCountryAutocomplete = findViewById(R.id.autocomplete_country_cat);
        mCountryNameToCode = CountryUtils.getCountryNameToCodeMap();
        final ArrayAdapter countryAdapter = new CountryAdapter(getContext(),
                new ArrayList<>(mCountryNameToCode.keySet()));
        mCountryAutocomplete.setThreshold(0);
        mCountryAutocomplete.setAdapter(countryAdapter);
        mCountryAutocomplete.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                String countryEntered = mCountryAutocomplete.getText().toString();
                updateUIForCountryEntered(countryEntered);
            }
        });
        String defaultCountryEntered = (String) countryAdapter.getItem(0);
        updateUIForCountryEntered(defaultCountryEntered);
        mCountryAutocomplete.setText(defaultCountryEntered);
        mCountryAutocomplete.setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean focused) {
                String countryEntered = mCountryAutocomplete.getText().toString();
                if (focused) {
                    mCountryAutocomplete.showDropDown();
                } else {
                    updateUIForCountryEntered(countryEntered);
                }
            }
        });
    }

    @VisibleForTesting
    void updateUIForCountryEntered(String displayCountryEntered) {
        String countryCodeEntered = mCountryNameToCode.get(displayCountryEntered);
        if (countryCodeEntered != null) {
            if (mCountrySelected == null || !mCountrySelected.equals(countryCodeEntered)) {
                mCountrySelected = countryCodeEntered;
                if (mCountryChangeListener != null) {
                    mCountryChangeListener.onCountryChanged(mCountrySelected);
                }
            }
            mCountryAutocomplete.setText(displayCountryEntered);
        } else if (mCountrySelected != null) {
            // Revert back to last valid country if country is not recognized.
            String displayCountry = new Locale("", mCountrySelected).getDisplayCountry();
            mCountryAutocomplete.setText(displayCountry);
        }
    }

    interface CountryChangeListener {
        void onCountryChanged(String countryCode);
    }
}
