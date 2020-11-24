package com.stripe.android.paymentsheet.ui

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import androidx.annotation.VisibleForTesting
import androidx.core.content.ContextCompat
import androidx.core.os.ConfigurationCompat
import androidx.core.view.ViewCompat
import com.google.android.material.shape.CornerFamily
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.shape.ShapeAppearanceModel
import com.stripe.android.R
import com.stripe.android.databinding.StripeBillingAddressLayoutBinding
import com.stripe.android.view.Country
import com.stripe.android.view.CountryAdapter
import com.stripe.android.view.CountryAutoCompleteTextViewValidator
import com.stripe.android.view.CountryUtils

internal class BillingAddressView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {
    private val viewBinding = StripeBillingAddressLayoutBinding.inflate(
        LayoutInflater.from(context),
        this
    )

    private val countryAdapter = CountryAdapter(
        context,
        CountryUtils.getOrderedCountries(
            ConfigurationCompat.getLocales(context.resources.configuration)[0]
        )
    )

    internal val countryView = viewBinding.country
    private val postalCodeView = viewBinding.postalCode

    private var selectedCountry: Country? = null

    init {
        orientation = VERTICAL

        ViewCompat.setBackground(
            this,
            MaterialShapeDrawable(
                ShapeAppearanceModel()
                    .toBuilder()
                    .setAllCorners(
                        CornerFamily.ROUNDED,
                        resources.getDimension(R.dimen.stripe_paymentsheet_form_corner_radius)
                    )
                    .build()
            ).also {
                it.setStroke(
                    resources.getDimension(R.dimen.stripe_paymentsheet_form_border_width),
                    ContextCompat.getColor(context, R.color.stripe_paymentsheet_form_border)
                )

                it.fillColor = ContextCompat.getColorStateList(context, android.R.color.transparent)
            }
        )

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
