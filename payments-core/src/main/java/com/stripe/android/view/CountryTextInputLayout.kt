package com.stripe.android.view

import android.content.Context
import android.os.Parcelable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View.OnFocusChangeListener
import android.widget.AdapterView
import android.widget.AutoCompleteTextView
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.annotation.RestrictTo
import androidx.annotation.StyleRes
import androidx.annotation.VisibleForTesting
import androidx.core.content.withStyledAttributes
import androidx.core.os.LocaleListCompat
import androidx.core.view.doOnNextLayout
import com.google.android.material.textfield.TextInputLayout
import com.stripe.android.R
import com.stripe.android.core.model.Country
import com.stripe.android.core.model.CountryCode
import com.stripe.android.core.model.CountryUtils
import kotlinx.parcelize.Parcelize
import java.util.Locale
import kotlin.properties.Delegates
import androidx.appcompat.R as AppCompatR
import com.google.android.material.R as MaterialR

// TODO: can this be deleted? Yes, this is only used in ShippingInfoWidgetTest. Delete it when I delete that class.
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
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // For paymentsheet
class CountryTextInputLayout @JvmOverloads internal constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = MaterialR.attr.textInputStyle
) : TextInputLayout(context, attrs, defStyleAttr) {

    @StyleRes
    private var countryAutoCompleteStyleRes: Int = INVALID_COUNTRY_AUTO_COMPLETE_STYLE

    @LayoutRes
    private var itemLayoutRes: Int = DEFAULT_ITEM_LAYOUT

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    // For paymentsheet
    @VisibleForTesting
    val countryAutocomplete: AutoCompleteTextView

    /**
     * The 2 digit country code of the country selected by this input.
     */
    @VisibleForTesting
    internal var selectedCountryCode: CountryCode? by Delegates.observable(
        null
    ) { _, _, newCountryValue ->
        newCountryValue?.let {
            countryCodeChangeCallback(it)
            CountryUtils.getCountryByCode(it, getLocale())?.let { country ->
                countryChangeCallback(country)
            }
        }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // For paymentsheet
    fun getSelectedCountryCode() = selectedCountryCode

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // For paymentsheet
    fun setSelectedCountryCode(countryCode: CountryCode?) {
        selectedCountryCode = countryCode
    }

    internal val selectedCountry: Country?
        get() = selectedCountryCode?.let { CountryUtils.getCountryByCode(it, getLocale()) }

    @Deprecated(
        message = "Will be removed in a future version",
        replaceWith = ReplaceWith("countryCodeChangeCallback")
    )
    @JvmSynthetic
    internal var countryChangeCallback: (Country) -> Unit = {}

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    // For paymentsheet
    @JvmSynthetic
    var countryCodeChangeCallback: (CountryCode) -> Unit = {}

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
                CountryUtils.getCountryCodeByName(countryEntered, getLocale())?.let {
                    updateUiForCountryEntered(it)
                } ?: CountryUtils.getCountryByCode(CountryCode.create(countryEntered), getLocale())
                    ?.let {
                        updateUiForCountryEntered(CountryCode.create(countryEntered))
                    }
            }
        }

        selectedCountryCode = countryAdapter.firstItem.code
        updateInitialCountry()

        val errorMessage = resources.getString(R.string.stripe_address_country_invalid)

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

    override fun onSaveInstanceState(): Parcelable? {
        selectedCountry?.let {
            return SelectedCountryState(
                it.code,
                super.onSaveInstanceState()
            )
        } ?: run {
            return super.onSaveInstanceState()
        }
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        if (state is SelectedCountryState) {
            restoreSelectedCountry(state)
        } else {
            super.onRestoreInstanceState(state)
        }
    }

    @VisibleForTesting
    internal fun restoreSelectedCountry(state: SelectedCountryState) {
        super.onRestoreInstanceState(state.superState)
        state.countryCode.let { countryCode ->
            updatedSelectedCountryCode(countryCode)
            updateUiForCountryEntered(countryCode)
            requestLayout()
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
                AppCompatR.attr.autoCompleteTextViewStyle
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

    @Deprecated(
        message = "This will be removed in a future version",
        replaceWith = ReplaceWith(
            expression = "setCountrySelected(CountryCode.create(countryCode))",
            imports = ["com.stripe.android.model.CountryCode"]
        )
    )
    @VisibleForTesting
    internal fun setCountrySelected(countryCode: String) {
        updateUiForCountryEntered(CountryCode.create(countryCode))
    }

    @VisibleForTesting
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun updateUiForCountryEntered(countryCode: CountryCode) {
        // If the user-typed country matches a valid country, update the selected country
        // Otherwise, revert back to last valid country if country is not recognized.
        val displayCountry = CountryUtils.getCountryByCode(countryCode, getLocale())?.let {
            updatedSelectedCountryCode(countryCode)
            it
        } ?: CountryUtils.getCountryByCode(selectedCountryCode, getLocale())

        countryAutocomplete.setText(displayCountry?.name)
    }

    internal fun updatedSelectedCountryCode(countryCode: CountryCode) {
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
        return LocaleListCompat.getAdjustedDefault().get(0)!!
    }

    private companion object {
        const val INVALID_COUNTRY_AUTO_COMPLETE_STYLE = 0
        val DEFAULT_ITEM_LAYOUT = R.layout.stripe_country_text_view
    }

    @Parcelize
    internal data class SelectedCountryState(
        val countryCode: CountryCode,
        val superState: Parcelable?
    ) : Parcelable
}
