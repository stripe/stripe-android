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
import androidx.core.view.doOnNextLayout
import com.google.android.material.textfield.TextInputLayout
import com.stripe.android.R
import java.util.Locale
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
    var selectedCountryCode: CountryCode? by Delegates.observable(
        null
    ) { _, _, newCountryValue ->
        newCountryValue?.let {
            countryCodeChangeCallback(it)
            CountryUtils.getCountryByCode(it)?.let { country ->
                countryChangeCallback(country)
            }
        }
    }

    val selectedCountry: Country?
        get() = selectedCountryCode?.let { CountryUtils.getCountryByCode(it) }

    @JvmSynthetic
    internal var countryChangeCallback: (Country) -> Unit = {}

    @JvmSynthetic
    internal var countryCodeChangeCallback: (CountryCode) -> Unit = {}

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
            CountryUtils.getOrderedCountries(getLocale()),
            itemLayoutRes
        ) {
            // item must be a TextView
            LayoutInflater.from(context).inflate(itemLayoutRes, it, false) as TextView
        }

        countryAutocomplete.threshold = 0
        countryAutocomplete.setAdapter(countryAdapter)
        countryAutocomplete.onItemClickListener =
            AdapterView.OnItemClickListener { _, _, position, _ ->
                updatedSelectedCountryCode(countryAdapter.getItem(position).code)
            }
        countryAutocomplete.onFocusChangeListener = OnFocusChangeListener { _, focused ->
            if (focused) {
                countryAutocomplete.showDropDown()
            } else {
                val countryEntered = countryAutocomplete.text.toString()
                CountryUtils.getCountryCodeByName(countryEntered)?.let {
                    updateUiForCountryEntered(it)
                }
            }
        }

        selectedCountryCode = countryAdapter.firstItem.code
        updateInitialCountry()

        val errorMessage = resources.getString(R.string.address_country_invalid)

        countryAutocomplete.validator = CountryAutoCompleteTextViewValidator(
            countryAdapter
        ) { country ->
            selectedCountryCode = country?.code

            if (country != null) {
                clearError()
            } else {
                error = errorMessage
                isErrorEnabled = true
            }
        }
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        doOnNextLayout {
            countryAutocomplete.isEnabled = enabled
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
        selectedCountryCode = initialCountry.code
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
     * the full country display name.  This is preferred over the function that takes a string
     */
    @VisibleForTesting
    internal fun setCountrySelected(countryCode: CountryCode) {
        updateUiForCountryEntered(countryCode)
    }

    @VisibleForTesting
    internal fun setCountrySelected(twoLetterCountryCode: String) {
        updateUiForCountryEntered(CountryCode(twoLetterCountryCode))
    }

    @VisibleForTesting
    internal fun updateUiForCountryEntered(countryCode: CountryCode) {

        // If the user-typed country matches a valid country, update the selected country
        // Otherwise, revert back to last valid country if country is not recognized.
        val displayCountry = CountryUtils.getCountryByCode(countryCode)?.let {
            updatedSelectedCountryCode(countryCode)
            it
        } ?: selectedCountryCode?.let { CountryUtils.getCountryByCode(it) }

        countryAutocomplete.setText(displayCountry?.name)
    }

    private fun updatedSelectedCountryCode(countryCode: CountryCode) {
        clearError()
        if (selectedCountryCode != countryCode) {
            selectedCountryCode = countryCode
        }
    }

    internal fun validateCountry() {
        countryAutocomplete.performValidation()
    }

    private fun clearError() {
        error = null
        isErrorEnabled = false
    }

    private fun getLocale(): Locale {
        return ConfigurationCompat.getLocales(context.resources.configuration)[0]
    }

    private companion object {
        const val INVALID_COUNTRY_AUTO_COMPLETE_STYLE = 0
        val DEFAULT_ITEM_LAYOUT = R.layout.country_text_view
    }
}
