package com.stripe.android.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.View.OnFocusChangeListener
import android.widget.AdapterView
import android.widget.AutoCompleteTextView
import android.widget.FrameLayout
import androidx.annotation.VisibleForTesting
import androidx.core.os.ConfigurationCompat
import com.stripe.android.R
import java.util.Locale

internal class CountryAutoCompleteTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    private val countryAutocomplete: AutoCompleteTextView

    /**
     * @return 2 digit country code of the country selected by this input.
     */
    @VisibleForTesting
    var selectedCountryCode: String? = null

    private var countryChangeListener: CountryChangeListener? = null

    init {
        View.inflate(getContext(), R.layout.country_autocomplete_textview, this)
        countryAutocomplete = findViewById(R.id.autocomplete_country_cat)
        val countryAdapter = CountryAdapter(
            getContext(),
            CountryUtils.getOrderedCountries(
                ConfigurationCompat.getLocales(context.resources.configuration)[0]
            )
        )
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
        val displayCountry = CountryUtils.getCountryCode(displayCountryEntered)?.let {
            if (selectedCountryCode == null || selectedCountryCode != it) {
                selectedCountryCode = it
                countryChangeListener?.onCountryChanged(it)
            }
            displayCountryEntered
        } ?: selectedCountryCode?.let {
            // Revert back to last valid country if country is not recognized.
            getDisplayCountry(it)
        }

        countryAutocomplete.setText(displayCountry)
    }

    private fun getDisplayCountry(countryCode: String): String {
        return Locale("", countryCode).displayCountry
    }

    internal interface CountryChangeListener {
        fun onCountryChanged(countryCode: String)
    }
}
