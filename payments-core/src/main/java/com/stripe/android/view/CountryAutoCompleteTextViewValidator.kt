package com.stripe.android.view

import android.widget.AutoCompleteTextView

internal class CountryAutoCompleteTextViewValidator(
    private val countryAdapter: CountryAdapter,
    private val onCountrySelected: (Country?) -> Unit
) : AutoCompleteTextView.Validator {

    override fun fixText(invalidText: CharSequence?): CharSequence = invalidText ?: ""

    override fun isValid(text: CharSequence?): Boolean {
        val country = countryAdapter.unfilteredCountries.firstOrNull {
            it.name == text.toString()
        }.also(onCountrySelected)

        return country != null
    }
}
