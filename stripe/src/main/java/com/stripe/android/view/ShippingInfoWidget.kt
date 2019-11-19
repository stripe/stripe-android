package com.stripe.android.view

import android.content.Context
import android.os.Build
import android.telephony.PhoneNumberFormattingTextWatcher
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import androidx.annotation.StringDef
import com.google.android.material.textfield.TextInputLayout
import com.stripe.android.R
import com.stripe.android.model.Address
import com.stripe.android.model.ShippingInformation
import java.util.Locale

/**
 * A widget used to collect address data from a user.
 */
class ShippingInfoWidget @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val shippingPostalCodeValidator: ShippingPostalCodeValidator
    private var optionalShippingInfoFields: List<String> = emptyList()
    private var hiddenShippingInfoFields: List<String> = emptyList()

    private val countryAutoCompleteTextView: CountryAutoCompleteTextView
    private val addressLine1TextInputLayout: TextInputLayout
    private val addressLine2TextInputLayout: TextInputLayout
    private val cityTextInputLayout: TextInputLayout
    private val nameTextInputLayout: TextInputLayout
    private val postalCodeTextInputLayout: TextInputLayout
    private val stateTextInputLayout: TextInputLayout
    private val phoneNumberTextInputLayout: TextInputLayout
    private val addressEditText: StripeEditText
    private val addressEditText2: StripeEditText
    private val cityEditText: StripeEditText
    private val nameEditText: StripeEditText
    private val postalCodeEditText: StripeEditText
    private val stateEditText: StripeEditText
    private val phoneNumberEditText: StripeEditText

    /**
     * Return [ShippingInformation] based on user input if valid, otherwise null.
     */
    val shippingInformation: ShippingInformation?
        get() {
            return if (!validateAllFields()) {
                null
            } else {
                rawShippingInformation
            }
        }

    /**
     * Return [ShippingInformation] based on user input.
     */
    private val rawShippingInformation: ShippingInformation
        get() {
            return ShippingInformation(
                Address.Builder()
                    .setCity(cityEditText.text?.toString())
                    .setCountry(countryAutoCompleteTextView.selectedCountry?.code)
                    .setLine1(addressEditText.text?.toString())
                    .setLine2(addressEditText2.text?.toString())
                    .setPostalCode(postalCodeEditText.text?.toString())
                    .setState(stateEditText.text?.toString())
                    .build(),
                nameEditText.text?.toString(),
                phoneNumberEditText.text?.toString()
            )
        }

    /**
     * Constants that can be used to mark fields in this widget as optional or hidden.
     * Some fields cannot be hidden.
     */
    @Retention(AnnotationRetention.SOURCE)
    @StringDef(CustomizableShippingField.ADDRESS_LINE_ONE_FIELD,
        CustomizableShippingField.ADDRESS_LINE_TWO_FIELD,
        CustomizableShippingField.CITY_FIELD,
        CustomizableShippingField.POSTAL_CODE_FIELD,
        CustomizableShippingField.STATE_FIELD,
        CustomizableShippingField.PHONE_FIELD)
    annotation class CustomizableShippingField {
        companion object {
            const val ADDRESS_LINE_ONE_FIELD: String = "address_line_one"
            // address line two is optional by default
            const val ADDRESS_LINE_TWO_FIELD: String = "address_line_two"
            const val CITY_FIELD: String = "city"
            const val POSTAL_CODE_FIELD: String = "postal_code"
            const val STATE_FIELD: String = "state"
            const val PHONE_FIELD: String = "phone"
        }
    }

    init {
        orientation = VERTICAL
        View.inflate(context, R.layout.add_address_widget, this)

        countryAutoCompleteTextView = findViewById(R.id.country_autocomplete_aaw)
        addressLine1TextInputLayout = findViewById(R.id.tl_address_line1_aaw)
        addressLine2TextInputLayout = findViewById(R.id.tl_address_line2_aaw)
        cityTextInputLayout = findViewById(R.id.tl_city_aaw)
        nameTextInputLayout = findViewById(R.id.tl_name_aaw)
        postalCodeTextInputLayout = findViewById(R.id.tl_postal_code_aaw)
        stateTextInputLayout = findViewById(R.id.tl_state_aaw)
        addressEditText = findViewById(R.id.et_address_line_one_aaw)
        addressEditText2 = findViewById(R.id.et_address_line_two_aaw)
        cityEditText = findViewById(R.id.et_city_aaw)
        nameEditText = findViewById(R.id.et_name_aaw)
        postalCodeEditText = findViewById(R.id.et_postal_code_aaw)
        stateEditText = findViewById(R.id.et_state_aaw)
        phoneNumberEditText = findViewById(R.id.et_phone_number_aaw)
        phoneNumberTextInputLayout = findViewById(R.id.tl_phone_number_aaw)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nameEditText.setAutofillHints(View.AUTOFILL_HINT_NAME)
            addressLine1TextInputLayout.setAutofillHints(View.AUTOFILL_HINT_POSTAL_ADDRESS)
            postalCodeEditText.setAutofillHints(View.AUTOFILL_HINT_POSTAL_CODE)
            phoneNumberEditText.setAutofillHints(View.AUTOFILL_HINT_PHONE)
        }

        shippingPostalCodeValidator = ShippingPostalCodeValidator()

        initView()
    }

    /**
     * @param optionalAddressFields address fields that should be optional.
     */
    fun setOptionalFields(optionalAddressFields: List<String>?) {
        optionalShippingInfoFields = optionalAddressFields.orEmpty()
        renderLabels()

        countryAutoCompleteTextView.selectedCountry?.let(::renderCountrySpecificLabels)
    }

    /**
     * @param hiddenAddressFields address fields that should be hidden. Hidden fields are
     * automatically optional.
     */
    fun setHiddenFields(hiddenAddressFields: List<String>?) {
        hiddenShippingInfoFields = hiddenAddressFields.orEmpty()
        renderLabels()

        countryAutoCompleteTextView.selectedCountry?.let(::renderCountrySpecificLabels)
    }

    /**
     * @param shippingInformation shippingInformation to populated into the widget input fields.
     */
    fun populateShippingInfo(shippingInformation: ShippingInformation?) {
        if (shippingInformation == null) {
            return
        }

        shippingInformation.address?.let { address ->
            cityEditText.setText(address.city)
            address.country?.let { country ->
                if (country.isNotEmpty()) {
                    countryAutoCompleteTextView.setCountrySelected(country)
                }
            }
            addressEditText.setText(address.line1)
            addressEditText2.setText(address.line2)
            postalCodeEditText.setText(address.postalCode)
            stateEditText.setText(address.state)
        }
        nameEditText.setText(shippingInformation.name)
        phoneNumberEditText.setText(shippingInformation.phone)
    }

    fun setAllowedCountryCodes(allowedCountryCodes: Set<String>) {
        countryAutoCompleteTextView.setAllowedCountryCodes(allowedCountryCodes)
    }

    /**
     * Validates all fields and shows error messages if appropriate.
     *
     * @return `true` if all shown fields are valid, `false` otherwise
     */
    fun validateAllFields(): Boolean {
        val address = addressEditText.text?.toString() ?: return false
        val name = nameEditText.text?.toString() ?: return false
        val city = cityEditText.text?.toString() ?: return false
        val state = stateEditText.text?.toString() ?: return false
        val postalCode = postalCodeEditText.text?.toString() ?: return false
        val phoneNumber = phoneNumberEditText.text?.toString() ?: return false

        countryAutoCompleteTextView.validateCountry()
        val selectedCountry = countryAutoCompleteTextView.selectedCountry

        val isPostalCodeValid = shippingPostalCodeValidator.isValid(
            postalCode,
            selectedCountry?.code,
            optionalShippingInfoFields,
            hiddenShippingInfoFields
        )
        postalCodeEditText.shouldShowError = !isPostalCodeValid

        val requiredAddressLine1Empty = address.isEmpty() &&
            isFieldRequired(CustomizableShippingField.ADDRESS_LINE_ONE_FIELD)
        addressEditText.shouldShowError = requiredAddressLine1Empty

        val requiredCityEmpty = city.isEmpty() &&
            isFieldRequired(CustomizableShippingField.CITY_FIELD)
        cityEditText.shouldShowError = requiredCityEmpty

        val requiredNameEmpty = name.isEmpty()
        nameEditText.shouldShowError = requiredNameEmpty

        val requiredStateEmpty = state.isEmpty() &&
            isFieldRequired(CustomizableShippingField.STATE_FIELD)
        stateEditText.shouldShowError = requiredStateEmpty

        val requiredPhoneNumberEmpty = phoneNumber.isEmpty() &&
            isFieldRequired(CustomizableShippingField.PHONE_FIELD)
        phoneNumberEditText.shouldShowError = requiredPhoneNumberEmpty

        return isPostalCodeValid && !requiredAddressLine1Empty && !requiredCityEmpty &&
            !requiredStateEmpty && !requiredNameEmpty && !requiredPhoneNumberEmpty &&
            selectedCountry != null
    }

    private fun isFieldRequired(@CustomizableShippingField field: String): Boolean {
        return !isFieldOptional(field) && !isFieldHidden(field)
    }

    private fun isFieldOptional(@CustomizableShippingField field: String): Boolean {
        return optionalShippingInfoFields.contains(field)
    }

    private fun isFieldHidden(@CustomizableShippingField field: String): Boolean {
        return hiddenShippingInfoFields.contains(field)
    }

    private fun initView() {
        countryAutoCompleteTextView.countryChangeCallback = ::renderCountrySpecificLabels

        phoneNumberEditText.addTextChangedListener(PhoneNumberFormattingTextWatcher())
        setupErrorHandling()
        renderLabels()

        countryAutoCompleteTextView.selectedCountry?.let(::renderCountrySpecificLabels)
    }

    private fun setupErrorHandling() {
        addressEditText.setErrorMessageListener(ErrorListener(addressLine1TextInputLayout))
        cityEditText.setErrorMessageListener(ErrorListener(cityTextInputLayout))
        nameEditText.setErrorMessageListener(ErrorListener(nameTextInputLayout))
        postalCodeEditText.setErrorMessageListener(ErrorListener(postalCodeTextInputLayout))
        stateEditText.setErrorMessageListener(ErrorListener(stateTextInputLayout))
        phoneNumberEditText.setErrorMessageListener(ErrorListener(phoneNumberTextInputLayout))
        addressEditText.setErrorMessage(resources.getString(R.string.address_required))
        cityEditText.setErrorMessage(resources.getString(R.string.address_city_required))
        nameEditText.setErrorMessage(resources.getString(R.string.address_name_required))
        phoneNumberEditText.setErrorMessage(resources.getString(R.string
            .address_phone_number_required))
    }

    private fun renderLabels() {
        nameTextInputLayout.hint = resources.getString(R.string.address_label_name)
        cityTextInputLayout.hint =
            if (isFieldOptional(CustomizableShippingField.CITY_FIELD)) {
                resources.getString(R.string.address_label_city_optional)
            } else {
                resources.getString(R.string.address_label_city)
            }
        phoneNumberTextInputLayout.hint =
            if (isFieldOptional(CustomizableShippingField.PHONE_FIELD)) {
                resources.getString(R.string.address_label_phone_number_optional)
            } else {
                resources.getString(R.string.address_label_phone_number)
            }
        hideHiddenFields()
    }

    private fun hideHiddenFields() {
        if (isFieldHidden(CustomizableShippingField.ADDRESS_LINE_ONE_FIELD)) {
            addressLine1TextInputLayout.visibility = View.GONE
        }
        if (isFieldHidden(CustomizableShippingField.ADDRESS_LINE_TWO_FIELD)) {
            addressLine2TextInputLayout.visibility = View.GONE
        }
        if (isFieldHidden(CustomizableShippingField.STATE_FIELD)) {
            stateTextInputLayout.visibility = View.GONE
        }
        if (isFieldHidden(CustomizableShippingField.CITY_FIELD)) {
            cityTextInputLayout.visibility = View.GONE
        }
        if (isFieldHidden(CustomizableShippingField.POSTAL_CODE_FIELD)) {
            postalCodeTextInputLayout.visibility = View.GONE
        }
        if (isFieldHidden(CustomizableShippingField.PHONE_FIELD)) {
            phoneNumberTextInputLayout.visibility = View.GONE
        }
    }

    private fun renderCountrySpecificLabels(country: Country) {
        when (country.code) {
            Locale.US.country -> renderUSForm()
            Locale.UK.country -> renderGreatBritainForm()
            Locale.CANADA.country -> renderCanadianForm()
            else -> renderInternationalForm()
        }

        postalCodeTextInputLayout.visibility =
            if (CountryUtils.doesCountryUsePostalCode(country.code) &&
                !isFieldHidden(CustomizableShippingField.POSTAL_CODE_FIELD)) {
                View.VISIBLE
            } else {
                View.GONE
            }
    }

    private fun renderUSForm() {
        addressLine1TextInputLayout.hint =
            if (isFieldOptional(CustomizableShippingField.ADDRESS_LINE_ONE_FIELD)) {
                resources.getString(R.string.address_label_address_optional)
            } else {
                resources.getString(R.string.address_label_address)
            }
        addressLine2TextInputLayout.hint = resources.getString(R.string
            .address_label_apt_optional)
        postalCodeTextInputLayout.hint =
            if (isFieldOptional(CustomizableShippingField.POSTAL_CODE_FIELD)) {
                resources.getString(R.string.address_label_zip_code_optional)
            } else {
                resources.getString(R.string.address_label_zip_code)
            }
        stateTextInputLayout.hint =
            if (isFieldOptional(CustomizableShippingField.STATE_FIELD)) {
                resources.getString(R.string.address_label_state_optional)
            } else {
                resources.getString(R.string.address_label_state)
            }
        postalCodeEditText.setErrorMessage(resources.getString(R.string.address_zip_invalid))
        stateEditText.setErrorMessage(resources.getString(R.string.address_state_required))
    }

    private fun renderGreatBritainForm() {
        addressLine1TextInputLayout.hint =
            if (isFieldOptional(CustomizableShippingField.ADDRESS_LINE_ONE_FIELD)) {
                resources.getString(R.string.address_label_address_line1_optional)
            } else {
                resources.getString(R.string.address_label_address_line1)
            }
        addressLine2TextInputLayout.hint = resources.getString(
            R.string.address_label_address_line2_optional
        )
        postalCodeTextInputLayout.hint =
            if (isFieldOptional(CustomizableShippingField.POSTAL_CODE_FIELD)) {
                resources.getString(R.string.address_label_postcode_optional)
            } else {
                resources.getString(R.string.address_label_postcode)
            }
        stateTextInputLayout.hint =
            if (isFieldOptional(CustomizableShippingField.STATE_FIELD)) {
                resources.getString(R.string.address_label_county_optional)
            } else {
                resources.getString(R.string.address_label_county)
            }
        postalCodeEditText.setErrorMessage(resources.getString(R.string.address_postcode_invalid))
        stateEditText.setErrorMessage(resources.getString(R.string.address_county_required))
    }

    private fun renderCanadianForm() {
        addressLine1TextInputLayout.hint =
            if (isFieldOptional(CustomizableShippingField.ADDRESS_LINE_ONE_FIELD)) {
                resources.getString(R.string.address_label_address_optional)
            } else {
                resources.getString(R.string.address_label_address)
            }
        addressLine2TextInputLayout.hint = resources.getString(R.string.address_label_apt_optional)
        postalCodeTextInputLayout.hint =
            if (isFieldOptional(CustomizableShippingField.POSTAL_CODE_FIELD)) {
                resources.getString(R.string.address_label_postal_code_optional)
            } else {
                resources.getString(R.string.address_label_postal_code)
            }
        stateTextInputLayout.hint =
            if (isFieldOptional(CustomizableShippingField.STATE_FIELD)) {
                resources.getString(R.string.address_label_province_optional)
            } else {
                resources.getString(R.string.address_label_province)
            }

        postalCodeEditText.setErrorMessage(resources.getString(R.string
            .address_postal_code_invalid))
        stateEditText.setErrorMessage(resources.getString(R.string
            .address_province_required))
    }

    private fun renderInternationalForm() {
        addressLine1TextInputLayout.hint =
            if (isFieldOptional(CustomizableShippingField.ADDRESS_LINE_ONE_FIELD)) {
                resources.getString(R.string.address_label_address_line1_optional)
            } else {
                resources.getString(R.string.address_label_address_line1)
            }
        addressLine2TextInputLayout.hint =
            resources.getString(R.string.address_label_address_line2_optional)

        postalCodeTextInputLayout.hint =
            if (isFieldOptional(CustomizableShippingField.POSTAL_CODE_FIELD)) {
                resources.getString(R.string.address_label_zip_postal_code_optional)
            } else {
                resources.getString(R.string.address_label_zip_postal_code)
            }

        stateTextInputLayout.hint =
            if (isFieldOptional(CustomizableShippingField.STATE_FIELD)) {
                resources.getString(R.string.address_label_region_generic_optional)
            } else {
                resources.getString(R.string.address_label_region_generic)
            }

        postalCodeEditText.setErrorMessage(resources.getString(R.string
            .address_zip_postal_invalid))
        stateEditText.setErrorMessage(resources.getString(R.string
            .address_region_generic_required))
    }
}
