package com.stripe.android.paymentsheet.ui

import android.content.Context
import android.text.InputFilter
import android.text.InputType
import android.text.method.DigitsKeyListener
import android.text.method.KeyListener
import android.text.method.TextKeyListener
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View.OnFocusChangeListener
import android.widget.AdapterView
import android.widget.FrameLayout
import androidx.annotation.VisibleForTesting
import androidx.core.os.ConfigurationCompat
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.stripe.android.R
import com.stripe.android.databinding.StripeBillingAddressLayoutBinding
import com.stripe.android.databinding.StripeCountryDropdownItemBinding
import com.stripe.android.model.Address
import com.stripe.android.paymentsheet.PaymentSheet
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
    @VisibleForTesting
    internal var level: PaymentSheet.BillingAddressCollectionLevel by Delegates.observable(
        PaymentSheet.BillingAddressCollectionLevel.Automatic
    ) { _, oldLevel, newLevel ->
        if (oldLevel != newLevel) {
            configureForLevel()
        }
    }

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

    private val _address = MutableLiveData<Address?>(null)
    internal val address: LiveData<Address?> = _address

    @VisibleForTesting
    internal val countryView = viewBinding.country

    @VisibleForTesting
    internal val postalCodeView = viewBinding.postalCode

    @VisibleForTesting
    internal val postalCodeLayout = viewBinding.postalCodeLayout

    @VisibleForTesting
    internal val address1View = viewBinding.address1
    private val address1Value: String?
        get() {
            return address1View.takeIf { it.isVisible }?.text?.toString().takeUnless {
                it.isNullOrBlank()
            }
        }

    @VisibleForTesting
    internal val address2View = viewBinding.address2
    private val address2Value: String?
        get() {
            return address2View.takeIf { it.isVisible }?.text?.toString().takeUnless {
                it.isNullOrBlank()
            }
        }

    @VisibleForTesting
    internal var selectedCountry: Country? by Delegates.observable(
        null
    ) { _, _, newCountry ->
        updatePostalCodeView(newCountry)
        _address.value = createAddress()
    }

    private var postalCodeConfig: PostalCodeConfig by Delegates.observable(
        PostalCodeConfig.Global
    ) { _, _, config ->
        postalCodeView.filters = arrayOf(InputFilter.LengthFilter(config.maxLength))
        postalCodeView.keyListener = config.getKeyListener()
        postalCodeView.inputType = config.inputType
    }

    private val requiredViews = setOf(
        viewBinding.address1Divider,
        viewBinding.address1Layout,
        address1View,

        viewBinding.address2Divider,
        viewBinding.address2Layout,
        address2View
    )

    init {
        configureCountryAutoComplete()
        configureForLevel()

        setOf(postalCodeView, address1View, address2View).forEach { editText ->
            editText.doAfterTextChanged {
                _address.value = createAddress()
            }
        }
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

    /**
     * An [Address] if the country and postal code are valid; otherwise `null`.
     */
    private fun createAddress(): Address? {
        return selectedCountry?.code?.let { countryCode ->
            val postalCode = postalCodeView.text.toString()
            val isPostalCodeValid = postalCodeValidator.isValid(
                postalCode = postalCode,
                countryCode = countryCode
            )
            if (isPostalCodeValid) {
                Address(
                    country = countryCode,
                    postalCode = postalCode.takeUnless { it.isBlank() },
                    line1 = address1Value,
                    line2 = address2Value
                )
            } else {
                null
            }
        }
    }

    private fun updatePostalCodeView(country: Country?) {
        val shouldShowPostalCode = country == null ||
            CountryUtils.doesCountryUsePostalCode(country.code)
        viewBinding.postalCodeDivider.isVisible = shouldShowPostalCode
        postalCodeLayout.isVisible = shouldShowPostalCode

        postalCodeConfig = if (country?.code == "US") {
            PostalCodeConfig.UnitedStates
        } else {
            PostalCodeConfig.Global
        }
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        viewBinding.countryLayout.isEnabled = enabled
        postalCodeLayout.isEnabled = enabled
    }

    private fun configureForLevel() {
        when (level) {
            PaymentSheet.BillingAddressCollectionLevel.Automatic -> {
                requiredViews.forEach { it.isVisible = false }
            }
            PaymentSheet.BillingAddressCollectionLevel.Required -> {
                requiredViews.forEach { it.isVisible = true }
            }
        }
        _address.value = createAddress()
    }

    internal sealed class PostalCodeConfig {
        abstract val maxLength: Int
        abstract val inputType: Int

        abstract fun getKeyListener(): KeyListener

        object UnitedStates : PostalCodeConfig() {
            override val maxLength = 5
            override val inputType = InputType.TYPE_CLASS_NUMBER
            override fun getKeyListener(): KeyListener {
                return DigitsKeyListener.getInstance(false, true)
            }
        }

        object Global : PostalCodeConfig() {
            override val maxLength = 13
            override val inputType = InputType.TYPE_TEXT_VARIATION_POSTAL_ADDRESS
            override fun getKeyListener(): KeyListener {
                return TextKeyListener.getInstance()
            }
        }
    }
}
