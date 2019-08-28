package com.stripe.android.view

import android.content.Context
import android.support.annotation.VisibleForTesting
import android.util.AttributeSet
import android.view.View
import android.view.View.OnFocusChangeListener
import android.widget.AdapterView
import android.widget.AutoCompleteTextView
import android.widget.FrameLayout
import com.stripe.android.R
import java.util.Locale

internal class CountryAutoCompleteTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    private val countryAutocomplete: AutoCompleteTextView
    private val countryNameToCode: Map<String, String>

    /**
     * @return 2 digit country code of the country selected by this input.
     */
    @VisibleForTesting
    var selectedCountryCode: String? = null

    private var countryChangeListener: CountryChangeListener? = null

    init {
        View.inflate(getContext(), R.layout.country_autocomplete_textview, this)
        countryAutocomplete = findViewById(R.id.autocomplete_country_cat)
        countryNameToCode = CountryUtils.countryNameToCodeMap
        val countryAdapter = CountryAdapter(getContext(), countryNameToCode.keys.toList())
        countryAutocomplete.threshold = 0
        countryAutocomplete.setAdapter(countryAdapter)
        countryAutocomplete.onItemClickListener = AdapterView.OnItemClickListener { _, _, _, _ ->
            val countryEntered = countryAutocomplete.text.toString()
            updateUiForCountryEntered(countryEntered)
        }
        val defaultCountryEntered = countryAdapter.getItem(0)
        updateUiForCountryEntered(defaultCountryEntered)
        countryAutocomplete.setText(defaultCountryEntered)
        countryAutocomplete.onFocusChangeListener = OnFocusChangeListener { _, focused ->
            val countryEntered = countryAutocomplete.text.toString()
            if (focused) {
                countryAutocomplete.showDropDown()
            } else {
                updateUiForCountryEntered(countryEntered)
            }
        }
    }

    /**
     * @param countryCode specify a country code to display in the input. The input will display
     * the full country display name.
     */
    fun setCountrySelected(countryCode: String?) {
        if (countryCode == null) {
            return
        }
        updateUiForCountryEntered(getDisplayCountry(countryCode))
    }

    fun setCountryChangeListener(countryChangeListener: CountryChangeListener?) {
        this.countryChangeListener = countryChangeListener
    }

    @VisibleForTesting
    fun updateUiForCountryEntered(displayCountryEntered: String?) {
        val countryCodeEntered = countryNameToCode[displayCountryEntered]
        if (countryCodeEntered != null) {
            if (selectedCountryCode == null || selectedCountryCode != countryCodeEntered) {
                selectedCountryCode = countryCodeEntered
                if (countryChangeListener != null) {
                    countryChangeListener!!.onCountryChanged(selectedCountryCode!!)
                }
            }
            countryAutocomplete.setText(displayCountryEntered)
        } else if (selectedCountryCode != null) {
            // Revert back to last valid country if country is not recognized.
            countryAutocomplete.setText(getDisplayCountry(selectedCountryCode!!))
        }
    }

    private fun getDisplayCountry(countryCode: String): String {
        return Locale("", countryCode).displayCountry
    }

    internal interface CountryChangeListener {
        fun onCountryChanged(countryCode: String)
    }
}
