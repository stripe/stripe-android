package com.stripe.android.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View.OnFocusChangeListener
import android.widget.AdapterView
import android.widget.FrameLayout
import androidx.annotation.VisibleForTesting
import androidx.core.os.ConfigurationCompat
import com.stripe.android.R
import com.stripe.android.databinding.CountryAutocompleteViewBinding
import com.stripe.android.databinding.CountryTextViewBinding

internal class CountryAutoCompleteTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    private val viewBinding =
        CountryAutocompleteViewBinding.inflate(
            LayoutInflater.from(context),
            this
        )
    private val countryTextInputLayout = viewBinding.countryTextInputLayout

    private val layoutInflater = LayoutInflater.from(context)
    private val countryAdapter = CountryAdapter(
        context,
        CountryUtils.getOrderedCountries(
            ConfigurationCompat.getLocales(context.resources.configuration)[0]
        )
    ) {
        CountryTextViewBinding.inflate(
            layoutInflater,
            it,
            false
        ).root
    }

    @VisibleForTesting
    internal val countryAutocomplete = viewBinding.countryAutocomplete

    /**
     * The 2 digit country code of the country selected by this input.
     */
    @VisibleForTesting
    var selectedCountry: Country?

    @JvmSynthetic
    internal var countryChangeCallback: (Country) -> Unit = {}

    init {
        countryAutocomplete.threshold = 0
        countryAutocomplete.setAdapter(countryAdapter)
        countryAutocomplete.onItemClickListener =
            AdapterView.OnItemClickListener { _, _, position, _ ->
                updatedSelectedCountryCode(countryAdapter.getItem(position))
            }
        countryAutocomplete.onFocusChangeListener = OnFocusChangeListener { _, focused ->
            if (focused) {
                countryAutocomplete.showDropDown()
            } else {
                val countryEntered = countryAutocomplete.text.toString()
                updateUiForCountryEntered(countryEntered)
            }
        }

        selectedCountry = countryAdapter.firstItem
        updateInitialCountry()

        val errorMessage = resources.getString(R.string.address_country_invalid)

        countryAutocomplete.validator = CountryAutoCompleteTextViewValidator(
            countryAdapter
        ) { country ->
            selectedCountry = country

            if (country != null) {
                clearError()
            } else {
                countryTextInputLayout.error = errorMessage
                countryTextInputLayout.isErrorEnabled = true
            }
        }
    }

    private fun updateInitialCountry() {
        val initialCountry = countryAdapter.firstItem
        countryAutocomplete.setText(initialCountry.name)
        selectedCountry = initialCountry
        countryChangeCallback(initialCountry)
    }

    /**
     * @param allowedCountryCodes A set of allowed country codes. Will be ignored if empty.
     */
    internal fun setAllowedCountryCodes(allowedCountryCodes: Set<String>) {
        val isUpdated = countryAdapter.updateUnfilteredCountries(allowedCountryCodes)
        if (isUpdated) {
            updateInitialCountry()
        }
    }

    /**
     * @param countryCode specify a country code to display in the input. The input will display
     * the full country display name.
     */
    internal fun setCountrySelected(countryCode: String) {
        updateUiForCountryEntered(CountryUtils.getDisplayCountry(countryCode))
    }

    @VisibleForTesting
    internal fun updateUiForCountryEntered(displayCountryEntered: String) {
        val country = CountryUtils.getCountryByName(displayCountryEntered)

        // If the user-typed country matches a valid country, update the selected country
        // Otherwise, revert back to last valid country if country is not recognized.
        val displayCountry = country?.let {
            updatedSelectedCountryCode(it)
            displayCountryEntered
        } ?: selectedCountry?.name

        countryAutocomplete.setText(displayCountry)
    }

    private fun updatedSelectedCountryCode(country: Country) {
        clearError()
        if (selectedCountry != country) {
            selectedCountry = country
            countryChangeCallback(country)
        }
    }

    internal fun validateCountry() {
        countryAutocomplete.performValidation()
    }

    private fun clearError() {
        viewBinding.countryTextInputLayout.error = null
        viewBinding.countryTextInputLayout.isErrorEnabled = false
    }
}
