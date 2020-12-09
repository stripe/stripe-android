package com.stripe.android.paymentsheet.ui

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View.OnFocusChangeListener
import android.widget.AdapterView
import android.widget.FrameLayout
import androidx.annotation.VisibleForTesting
import androidx.core.os.ConfigurationCompat
import androidx.core.view.isVisible
import com.stripe.android.R
import com.stripe.android.databinding.StripeBillingAddressLayoutBinding
import com.stripe.android.databinding.StripeCountryDropdownItemBinding
import com.stripe.android.model.Address
import com.stripe.android.view.Country
import com.stripe.android.view.CountryAdapter
import com.stripe.android.view.CountryAutoCompleteTextViewValidator
import com.stripe.android.view.CountryUtils
import com.stripe.android.view.PostalCodeValidator
import kotlin.properties.Delegates

internal class BillingAddressView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    private val viewBinding = StripeBillingAddressLayoutBinding.inflate(
        LayoutInflater.from(context),
        this
    )

    private val countryAdapter = CountryAdapter(
        context,
        CountryUtils.getOrderedCountries(
            ConfigurationCompat.getLocales(context.resources.configuration)[0]
        ),
        R.layout.stripe_country_dropdown_item
    ) {
        StripeCountryDropdownItemBinding.inflate(
            LayoutInflater.from(context),
            it,
            false
        ).root
    }

    private val postalCodeValidator = PostalCodeValidator()

    internal val address: Address?
        get() {
            return selectedCountry?.code?.let { countryCode ->
                val postalCode = postalCodeView.text.toString()
                val isPostalCodeValid = postalCodeValidator.isValid(
                    postalCode = postalCode,
                    countryCode = countryCode
                )
                if (isPostalCodeValid) {
                    Address(
                        country = countryCode,
                        postalCode = postalCode.takeUnless { it.isBlank() }
                    )
                } else {
                    null
                }
            }
        }

    @VisibleForTesting
    internal val countryView = viewBinding.country

    @VisibleForTesting
    internal val postalCodeView = viewBinding.postalCode

    @VisibleForTesting
    internal val postalCodeLayout = viewBinding.postalCodeLayout

    @VisibleForTesting
    internal var selectedCountry: Country? by Delegates.observable(
        null
    ) { _, _, newCountry ->
        val shouldShowPostalCode = newCountry == null ||
            CountryUtils.doesCountryUsePostalCode(newCountry.code)
        viewBinding.postalCodeDivider.isVisible = shouldShowPostalCode
        postalCodeLayout.isVisible = shouldShowPostalCode
    }

    init {
        configureCountryAutoComplete()
    }

    private fun configureCountryAutoComplete() {
        countryView.threshold = 0
        countryView.setAdapter(countryAdapter)
        countryView.onItemClickListener =
            AdapterView.OnItemClickListener { _, _, position, _ ->
                updatedSelectedCountryCode(countryAdapter.getItem(position))
            }
        countryView.onFocusChangeListener = OnFocusChangeListener { _, focused ->
            if (focused) {
                countryView.showDropDown()
            } else {
                val countryEntered = countryView.text.toString()
                updateUiForCountryEntered(countryEntered)
            }
        }

        selectedCountry = countryAdapter.firstItem
        updateInitialCountry()

        countryView.validator = CountryAutoCompleteTextViewValidator(
            countryAdapter
        ) { country ->
            selectedCountry = country
        }
    }

    private fun updateInitialCountry() {
        val initialCountry = countryAdapter.firstItem
        countryView.setText(initialCountry.name)
        selectedCountry = initialCountry
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

        countryView.setText(displayCountry)
    }

    private fun updatedSelectedCountryCode(country: Country) {
        if (selectedCountry != country) {
            selectedCountry = country
        }
    }

    internal fun validateCountry() {
        countryView.performValidation()
    }
}
