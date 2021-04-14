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
import android.widget.EditText
import android.widget.FrameLayout
import androidx.annotation.VisibleForTesting
import androidx.core.os.ConfigurationCompat
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.stripe.android.R
import com.stripe.android.databinding.StripeBillingAddressLayoutBinding
import com.stripe.android.model.Address
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.view.Country
import com.stripe.android.view.CountryCode
import com.stripe.android.view.CountryUtils
import com.stripe.android.view.PostalCodeValidator
import java.util.Locale
import kotlin.properties.Delegates

internal class BillingAddressView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    /**
     * Listener to notify events related to [postalCodeView]
     */
    interface PostalCodeViewListener {
        fun onLosingFocus(country: Country?, isPostalValid: Boolean)
        fun onGainingFocus(country: Country?, isPostalValid: Boolean)
        fun onCountryChanged(country: Country?, isPostalValid: Boolean)
    }

    @VisibleForTesting
    internal var level: PaymentSheet.BillingAddressCollectionLevel by Delegates.observable(
        PaymentSheet.BillingAddressCollectionLevel.Automatic
    ) { _, oldLevel, newLevel ->
        if (oldLevel != newLevel) {
            configureForLevel()
        }
    }

    internal var onFocus: () -> Unit = {}

    private val viewBinding = StripeBillingAddressLayoutBinding.inflate(
        LayoutInflater.from(context),
        this
    )

    private val postalCodeValidator = PostalCodeValidator()

    private val _address = MutableLiveData<Address?>(null)
    internal val address: LiveData<Address?> = _address

    @VisibleForTesting
    internal val countryLayout = viewBinding.countryLayout

    @VisibleForTesting
    internal val countryView = countryLayout.countryAutocomplete

    @VisibleForTesting
    internal val cityPostalContainer = viewBinding.cityPostalContainer

    @VisibleForTesting
    internal val postalCodeView = viewBinding.postalCode

    @VisibleForTesting
    internal val postalCodeLayout = viewBinding.postalCodeLayout

    @VisibleForTesting
    internal val address1View = viewBinding.address1

    @VisibleForTesting
    internal val address2View = viewBinding.address2

    @VisibleForTesting
    internal val cityLayout = viewBinding.cityLayout

    @VisibleForTesting
    internal val cityView = viewBinding.city

    @VisibleForTesting
    internal val stateView = viewBinding.state

    @VisibleForTesting
    internal val stateLayout = viewBinding.stateLayout

    @VisibleForTesting
    internal var postalCodeViewListener: PostalCodeViewListener? = null

    private val isUnitedStates: Boolean get() = CountryCode.isUS(countryLayout.selectedCountry)

    private var postalCodeConfig: PostalCodeConfig by Delegates.observable(
        PostalCodeConfig.Global
    ) { _, _, config ->
        postalCodeView.filters = arrayOf(InputFilter.LengthFilter(config.maxLength))
        postalCodeView.keyListener = config.getKeyListener()
        postalCodeView.inputType = config.inputType
    }

    private val newCountryCallback = { newCountryCode: CountryCode ->
        updateStateView(newCountryCode)
        updatePostalCodeView(newCountryCode)
        _address.value = createAddress()

        postalCodeValidator.isValid(
            postalCode = postalCodeView.value.orEmpty(),
            countryCode = newCountryCode
        ).let { isPostalValid ->
            postalCodeViewListener?.onCountryChanged(
                CountryUtils.getCountryByCode(
                    newCountryCode,
                    ConfigurationCompat.getLocales(context.resources.configuration)[0]
                ), isPostalValid
            )
            postalCodeView.shouldShowError = !isPostalValid
        }
    }

    private val requiredViews = setOf(
        viewBinding.address1Divider,
        viewBinding.address1Layout,
        address1View,

        viewBinding.address2Divider,
        viewBinding.address2Layout,
        address2View,

        viewBinding.cityLayout,
        cityView,

        viewBinding.stateDivider,
        stateLayout,
        stateView,
    )

    private val allFields = setOf(
        address1View,
        address2View,
        cityView,
        stateView,
        postalCodeView,
        countryView
    )

    init {
        countryLayout.countryChangeCallback = newCountryCallback
        // Since the callback is set after CountryAutoCompleteTextView is fully initialized,
        // need to manually trigger the callback once to pick up the initial country
        countryLayout.selectedCountry?.let {
            newCountryCallback(it)
        }

        configureForLevel()

        allFields.forEach { editText ->
            editText.doAfterTextChanged {
                _address.value = createAddress()
            }

            editText.onFocusChangeListener = OnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    onFocus()
                }
            }
        }

        postalCodeView.internalFocusChangeListeners.add { _, hasFocus ->
            val isPostalValid = countryLayout.selectedCountry?.let { countryCode ->
                postalCodeValidator.isValid(
                    postalCode = postalCodeView.value.orEmpty(),
                    countryCode = countryCode
                )
            } ?: false

            postalCodeView.shouldShowError =
                !hasFocus && !postalCodeView.value.isNullOrBlank() && !isPostalValid

            if (hasFocus) {
                postalCodeViewListener?.onGainingFocus(
                    CountryUtils.getCountryByCode(
                        countryLayout.selectedCountry,
                        ConfigurationCompat.getLocales(context.resources.configuration)[0]
                    ),
                    isPostalValid
                )
            } else {
                postalCodeViewListener?.onLosingFocus(
                    CountryUtils.getCountryByCode(
                        countryLayout.selectedCountry,
                        ConfigurationCompat.getLocales(context.resources.configuration)[0]
                    ),
                    isPostalValid
                )
                postalCodeView.shouldShowError =
                    !postalCodeView.value.isNullOrBlank() && !isPostalValid
            }
        }
    }

    /**
     * An [Address] if the country and postal code are valid; otherwise `null`.
     */
    private fun createAddress(): Address? {
        return countryLayout.selectedCountry?.let { countryCode ->
            val postalCode = postalCodeView.value
            val isPostalCodeValid = postalCodeValidator.isValid(
                postalCode = postalCode.orEmpty(),
                countryCode = countryCode
            )
            if (isPostalCodeValid) {
                when (level) {
                    PaymentSheet.BillingAddressCollectionLevel.Automatic -> {
                        Address(
                            country = countryCode.twoLetters,
                            postalCode = postalCode
                        )
                    }
                    PaymentSheet.BillingAddressCollectionLevel.Required -> {
                        createRequiredAddress(countryCode, postalCode)
                    }
                }
            } else {
                null
            }
        }
    }

    private fun createRequiredAddress(
        countryCode: CountryCode,
        postalCode: String?
    ): Address? {
        val line1 = address1View.value
        val line2 = address2View.value
        val city = cityView.value
        val state = stateView.value

        return if (line1 != null && city != null) {
            if (!isUnitedStates) {
                Address(
                    country = countryCode.twoLetters,
                    postalCode = postalCode,
                    line1 = line1,
                    line2 = line2,
                    city = city
                )
            } else if (state != null) {
                Address(
                    country = countryCode.twoLetters,
                    postalCode = postalCode,
                    line1 = line1,
                    line2 = line2,
                    city = city,
                    state = state
                )
            } else {
                null
            }
        } else {
            null
        }
    }

    private fun updateStateView(countryCode: CountryCode?) {
        when {
            CountryCode.isUS(countryCode) -> {
                R.string.address_label_state
            }
            CountryCode.isCA(countryCode) -> {
                R.string.address_label_province
            }
            CountryCode.isGB(countryCode) -> {
                R.string.address_label_county
            }
            else -> {
                R.string.address_label_region_generic
            }
        }.let {
            stateLayout.hint = resources.getString(it)
        }
    }

    private fun updatePostalCodeView(countryCode: CountryCode?) {
        val shouldShowPostalCode = countryCode == null ||
            CountryUtils.doesCountryUsePostalCode(countryCode)
        postalCodeLayout.isVisible = shouldShowPostalCode

        val shouldShowPostalCodeContainer =
            level == PaymentSheet.BillingAddressCollectionLevel.Required || shouldShowPostalCode
        viewBinding.cityPostalDivider.isVisible = shouldShowPostalCodeContainer
        viewBinding.cityPostalContainer.isVisible = shouldShowPostalCodeContainer

        postalCodeConfig = if (CountryCode.isUS(countryCode)) {
            PostalCodeConfig.UnitedStates
        } else {
            PostalCodeConfig.Global
        }

        viewBinding.postalCodeLayout.hint = resources.getString(
            if (CountryCode.isUS(countryCode)) {
                R.string.acc_label_zip_short
            } else {
                R.string.address_label_postal_code
            }
        )
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)

        setOf(
            countryLayout,
            viewBinding.address1Layout,
            viewBinding.address2Layout,
            viewBinding.cityLayout,
            postalCodeLayout,
            stateLayout
        ).forEach {
            it.isEnabled = enabled
        }
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

    fun focusFirstField() {
        when (level) {
            PaymentSheet.BillingAddressCollectionLevel.Automatic -> {
                postalCodeLayout.requestFocus()
            }
            PaymentSheet.BillingAddressCollectionLevel.Required -> {
                viewBinding.address1Layout.requestFocus()
            }
        }
    }

    internal fun populate(address: Address?) {
        address?.let { it ->
            // The postal code needs to be set prior to the country, because the
            // country will trigger a validation of the postal code, which will be
            // invalid if not set first.
            this.postalCodeView.setText(it.postalCode)

            it.country?.let { country ->
                countryLayout.selectedCountry = CountryCode(country)
                this.countryView.setText(
                    CountryUtils.getDisplayCountry(
                        CountryCode(country),
                        ConfigurationCompat.getLocales(context.resources.configuration)[0]
                    )
                )
            }
            this.address1View.setText(it.line1)
            this.address2View.setText(it.line2)
            this.cityView.setText(it.city)
            this.stateView.setText(it.state)
        }
    }

    private val EditText.value: String?
        get() {
            return takeIf { it.isVisible }?.text?.toString().takeUnless {
                it.isNullOrBlank()
            }
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
