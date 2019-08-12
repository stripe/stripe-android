package com.stripe.android.view;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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
import java.util.Objects;

@RestrictTo(RestrictTo.Scope.LIBRARY)
public class CountryAutoCompleteTextView extends FrameLayout {
    @NonNull private final AutoCompleteTextView mCountryAutocomplete;
    @NonNull private final Map<String, String> mCountryNameToCode;

    @VisibleForTesting String mCountrySelected;
    @Nullable private CountryChangeListener mCountryChangeListener;

    public CountryAutoCompleteTextView(@NonNull Context context) {
        this(context, null);
    }

    public CountryAutoCompleteTextView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CountryAutoCompleteTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

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
                final String countryEntered = mCountryAutocomplete.getText().toString();
                updateUiForCountryEntered(countryEntered);
            }
        });
        final String defaultCountryEntered =
                (String) Objects.requireNonNull(countryAdapter.getItem(0));
        updateUiForCountryEntered(defaultCountryEntered);
        mCountryAutocomplete.setText(defaultCountryEntered);
        mCountryAutocomplete.setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean focused) {
                final String countryEntered = mCountryAutocomplete.getText().toString();
                if (focused) {
                    mCountryAutocomplete.showDropDown();
                } else {
                    updateUiForCountryEntered(countryEntered);
                }
            }
        });
    }

    /**
     * @return 2 digit country code of the country selected by this input.
     */
    @Nullable
    String getSelectedCountryCode() {
        return mCountrySelected;
    }

    /**
     * @param countryCode specify a country code to display in the input. The input will display
     *                    the full country display name.
     */
    void setCountrySelected(@Nullable String countryCode) {
        if (countryCode == null) {
            return;
        }
        updateUiForCountryEntered(getDisplayCountry(countryCode));
    }

    void setCountryChangeListener(@Nullable CountryChangeListener countryChangeListener) {
        mCountryChangeListener = countryChangeListener;
    }

    @VisibleForTesting
    void updateUiForCountryEntered(@NonNull String displayCountryEntered) {
        final String countryCodeEntered = mCountryNameToCode.get(displayCountryEntered);
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
            mCountryAutocomplete.setText(getDisplayCountry(mCountrySelected));
        }
    }

    @NonNull
    private static String getDisplayCountry(@NonNull String countryCode) {
        return new Locale("", countryCode).getDisplayCountry();
    }

    interface CountryChangeListener {
        void onCountryChanged(@NonNull String countryCode);
    }
}
