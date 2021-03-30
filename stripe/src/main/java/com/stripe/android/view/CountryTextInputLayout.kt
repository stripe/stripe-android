package com.stripe.android.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View.OnFocusChangeListener
import android.widget.AdapterView
import android.widget.AutoCompleteTextView
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.annotation.StyleRes
import androidx.annotation.VisibleForTesting
import androidx.core.content.withStyledAttributes
import androidx.core.os.ConfigurationCompat
import com.google.android.material.textfield.TextInputLayout
import com.stripe.android.R
import kotlin.properties.Delegates

/**
 * A subclass of [TextInputLayout] that programmatically wraps a styleable [AutoCompleteTextView],
 * which configures a [CountryAdapter] to display list of countries in its popup.
 *
 * The style of [AutoCompleteTextView] can be changed via
 * [R.styleable.StripeCountryAutoCompleteTextInputLayout_countryAutoCompleteStyle],
 * the layout of popup items can be changed via
 * [R.styleable.StripeCountryAutoCompleteTextInputLayout_countryItemLayout], note this layout must
 * be a [TextView].
 */
internal class CountryTextInputLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = com.google.android.material.R.attr.textInputStyle
) : TextInputLayout(context, attrs, defStyleAttr) {

    @StyleRes
    private var countryAutoCompleteStyleRes: Int = INVALID_COUNTRY_AUTO_COMPLETE_STYLE

    @LayoutRes
    private var itemLayoutRes: Int = DEFAULT_ITEM_LAYOUT

    @VisibleForTesting
    internal val countryAutocomplete: AutoCompleteTextView

    /**
     * The 2 digit country code of the country selected by this input.
     */
    @VisibleForTesting
    var selectedCountry: Country? by Delegates.observable(
        null
    ) { _, _, newCountryValue ->
        newCountryValue?.let {
            countryChangeCallback(it)
        }
    }

    @JvmSynthetic
    internal var countryChangeCallback: (Country) -> Unit = {}

    private var countryAdapter: CountryAdapter

    init {
        context.withStyledAttributes(
            attrs,
            R.styleable.StripeCountryAutoCompleteTextInputLayout
        ) {
            countryAutoCompleteStyleRes = getResourceId(
                R.styleable.StripeCountryAutoCompleteTextInputLayout_countryAutoCompleteStyle,
                INVALID_COUNTRY_AUTO_COMPLETE_STYLE
            )

            itemLayoutRes = getResourceId(
                R.styleable.StripeCountryAutoCompleteTextInputLayout_countryItemLayout,
                DEFAULT_ITEM_LAYOUT
            )
        }

        countryAutocomplete = initializeCountryAutoCompleteWithStyle()
        addView(
            countryAutocomplete,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        )

        countryAdapter = CountryAdapter(
            context,
            CountryUtils.getOrderedCountries(
                ConfigurationCompat.getLocales(context.resources.configuration)[0]
            ),
            itemLayoutRes
        ) {
            // item must be a TextView
            LayoutInflater.from(context).inflate(itemLayoutRes, it, false) as TextView
        }

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
                error = errorMessage
                isErrorEnabled = true
            }
        }
    }

    /**
     * Initialize the encapsulated [AutoCompleteTextView] with [countryAutoCompleteStyleRes] style
     * resource read from styleable.
     * If no style resource is set, create one with default style attributes
     * [R.attr.autoCompleteTextViewStyle].
     */
    private fun initializeCountryAutoCompleteWithStyle() =
        when (countryAutoCompleteStyleRes) {
            INVALID_COUNTRY_AUTO_COMPLETE_STYLE -> AutoCompleteTextView(
                context,
                null,
                R.attr.autoCompleteTextViewStyle
            )
            else -> AutoCompleteTextView(context, null, 0, countryAutoCompleteStyleRes)
        }

    private fun updateInitialCountry() {
        val initialCountry = countryAdapter.firstItem
        countryAutocomplete.setText(initialCountry.name)
        selectedCountry = initialCountry
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
        }
    }

    internal fun validateCountry() {
        countryAutocomplete.performValidation()
    }

    private fun clearError() {
        error = null
        isErrorEnabled = false
    }

    private companion object {
        const val INVALID_COUNTRY_AUTO_COMPLETE_STYLE = 0
        val DEFAULT_ITEM_LAYOUT = R.layout.country_text_view
    }
}
