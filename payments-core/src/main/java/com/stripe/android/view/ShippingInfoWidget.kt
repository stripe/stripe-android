package com.stripe.android.view

import android.content.Context
import android.os.Build
import android.telephony.PhoneNumberFormattingTextWatcher
import android.text.InputFilter
import android.text.InputFilter.AllCaps
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import androidx.annotation.VisibleForTesting
import com.stripe.android.R
import com.stripe.android.core.model.Country
import com.stripe.android.core.model.CountryUtils
import com.stripe.android.databinding.StripeAddressWidgetBinding
import com.stripe.android.model.Address
import com.stripe.android.model.ShippingInformation
import java.util.Locale
import com.stripe.android.core.R as CoreR
import com.stripe.android.uicore.R as UiCoreR

/**
 * A widget used to collect address data from a user.
 */
class ShippingInfoWidget @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    @VisibleForTesting
    internal val viewBinding: StripeAddressWidgetBinding by lazy {
        StripeAddressWidgetBinding.inflate(
            LayoutInflater.from(context),
            this
        )
    }
    private val postalCodeValidator: PostalCodeValidator = PostalCodeValidator()

    /**
     * Address fields that should be optional.
     */
    var optionalFields: List<CustomizableShippingField> = emptyList()
        set(value) {
            field = value

            renderLabels()
            countryAutoCompleteTextView.selectedCountry?.let(::updateConfigForCountry)
        }

    /**
     * Address fields that should be hidden. Hidden fields are automatically optional.
     */
    var hiddenFields: List<CustomizableShippingField> = emptyList()
        set(value) {
            field = value

            renderLabels()
            countryAutoCompleteTextView.selectedCountry?.let(::updateConfigForCountry)
        }

    private val countryAutoCompleteTextView = viewBinding.countryAutocompleteAaw
    private val addressLine1TextInputLayout = viewBinding.tlAddressLine1Aaw
    private val addressLine2TextInputLayout = viewBinding.tlAddressLine2Aaw
    private val cityTextInputLayout = viewBinding.tlCityAaw
    private val nameTextInputLayout = viewBinding.tlNameAaw
    private val postalCodeTextInputLayout = viewBinding.tlPostalCodeAaw
    private val stateTextInputLayout = viewBinding.tlStateAaw
    private val phoneNumberTextInputLayout = viewBinding.tlPhoneNumberAaw
    private val addressEditText = viewBinding.etAddressLineOneAaw
    private val addressEditText2 = viewBinding.etAddressLineTwoAaw
    private val cityEditText = viewBinding.etCityAaw
    private val nameEditText = viewBinding.etNameAaw
    private val postalCodeEditText = viewBinding.etPostalCodeAaw
    private val stateEditText = viewBinding.etStateAaw
    private val phoneNumberEditText = viewBinding.etPhoneNumberAaw

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
                    .setCity(cityEditText.fieldText)
                    .setCountryCode(countryAutoCompleteTextView.selectedCountry?.code)
                    .setLine1(addressEditText.fieldText)
                    .setLine2(addressEditText2.fieldText)
                    .setPostalCode(postalCodeEditText.fieldText)
                    .setState(stateEditText.fieldText)
                    .build(),
                nameEditText.fieldText,
                phoneNumberEditText.fieldText
            )
        }

    /**
     * Constants that can be used to mark fields in this widget as optional or hidden.
     * Some fields cannot be hidden.
     */
    enum class CustomizableShippingField {
        Line1,
        Line2, // optional by default
        City,
        PostalCode,
        State,
        Phone
    }

    init {
        orientation = VERTICAL

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nameEditText.setAutofillHints(View.AUTOFILL_HINT_NAME)
            addressLine1TextInputLayout.setAutofillHints(View.AUTOFILL_HINT_POSTAL_ADDRESS)
            postalCodeEditText.setAutofillHints(View.AUTOFILL_HINT_POSTAL_CODE)
            phoneNumberEditText.setAutofillHints(View.AUTOFILL_HINT_PHONE)
        }

        initView()
    }

    /**
     * @param shippingInformation shippingInformation to populated into the widget input fields.
     */
    fun populateShippingInfo(shippingInformation: ShippingInformation?) {
        if (shippingInformation == null) {
            return
        }

        shippingInformation.address?.let(::populateAddressFields)
        nameEditText.setText(shippingInformation.name)
        phoneNumberEditText.setText(shippingInformation.phone)
    }

    private fun populateAddressFields(address: Address) {
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

    fun setAllowedCountryCodes(allowedCountryCodes: Set<String>) {
        countryAutoCompleteTextView.setAllowedCountryCodes(allowedCountryCodes)
    }

    /**
     * Validates all fields and shows error messages if appropriate.
     *
     * @return `true` if all shown fields are valid, `false` otherwise
     */
    @Suppress("ComplexMethod", "ReturnCount")
    fun validateAllFields(): Boolean {
        val address = addressEditText.text?.toString() ?: return false
        val name = nameEditText.text?.toString() ?: return false
        val city = cityEditText.text?.toString() ?: return false
        val state = stateEditText.text?.toString() ?: return false
        val postalCode = postalCodeEditText.text?.toString() ?: return false
        val phoneNumber = phoneNumberEditText.text?.toString() ?: return false

        countryAutoCompleteTextView.validateCountry()
        val selectedCountry = countryAutoCompleteTextView.selectedCountry

        val isPostalCodeValid = postalCodeValidator.isValid(
            postalCode,
            selectedCountry?.code?.value,
            optionalFields,
            hiddenFields
        )
        postalCodeEditText.shouldShowError = !isPostalCodeValid

        val requiredAddressLine1Empty = address.isBlank() &&
            isFieldRequired(CustomizableShippingField.Line1)
        addressEditText.shouldShowError = requiredAddressLine1Empty

        val requiredCityEmpty = city.isBlank() &&
            isFieldRequired(CustomizableShippingField.City)
        cityEditText.shouldShowError = requiredCityEmpty

        val requiredNameEmpty = name.isBlank()
        nameEditText.shouldShowError = requiredNameEmpty

        val requiredStateEmpty = state.isBlank() &&
            isFieldRequired(CustomizableShippingField.State)
        stateEditText.shouldShowError = requiredStateEmpty

        val requiredPhoneNumberEmpty = phoneNumber.isBlank() &&
            isFieldRequired(CustomizableShippingField.Phone)
        phoneNumberEditText.shouldShowError = requiredPhoneNumberEmpty

        return isPostalCodeValid && !requiredAddressLine1Empty && !requiredCityEmpty &&
            !requiredStateEmpty && !requiredNameEmpty && !requiredPhoneNumberEmpty &&
            selectedCountry != null
    }

    private fun isFieldRequired(field: CustomizableShippingField): Boolean {
        return !isFieldOptional(field) && !isFieldHidden(field)
    }

    private fun isFieldOptional(field: CustomizableShippingField): Boolean {
        return optionalFields.contains(field)
    }

    private fun isFieldHidden(field: CustomizableShippingField): Boolean {
        return hiddenFields.contains(field)
    }

    private fun initView() {
        countryAutoCompleteTextView.countryChangeCallback = ::updateConfigForCountry

        phoneNumberEditText.addTextChangedListener(PhoneNumberFormattingTextWatcher())
        setupErrorHandling()
        renderLabels()

        countryAutoCompleteTextView.selectedCountry?.let(::updateConfigForCountry)
    }

    private fun setupErrorHandling() {
        addressEditText.setErrorMessageListener(ErrorListener(addressLine1TextInputLayout))
        cityEditText.setErrorMessageListener(ErrorListener(cityTextInputLayout))
        nameEditText.setErrorMessageListener(ErrorListener(nameTextInputLayout))
        postalCodeEditText.setErrorMessageListener(ErrorListener(postalCodeTextInputLayout))
        stateEditText.setErrorMessageListener(ErrorListener(stateTextInputLayout))
        phoneNumberEditText.setErrorMessageListener(ErrorListener(phoneNumberTextInputLayout))
        addressEditText.setErrorMessage(resources.getString(R.string.stripe_address_required))
        cityEditText.setErrorMessage(resources.getString(R.string.stripe_address_city_required))
        nameEditText.setErrorMessage(resources.getString(R.string.stripe_address_name_required))
        phoneNumberEditText.setErrorMessage(
            resources.getString(
                R.string.stripe_address_phone_number_required
            )
        )
    }

    private fun renderLabels() {
        nameTextInputLayout.hint = resources.getString(CoreR.string.stripe_address_label_full_name)
        cityTextInputLayout.hint =
            if (isFieldOptional(CustomizableShippingField.City)) {
                resources.getString(R.string.stripe_address_label_city_optional)
            } else {
                resources.getString(CoreR.string.stripe_address_label_city)
            }
        phoneNumberTextInputLayout.hint =
            if (isFieldOptional(CustomizableShippingField.Phone)) {
                resources.getString(R.string.stripe_address_label_phone_number_optional)
            } else {
                resources.getString(CoreR.string.stripe_address_label_phone_number)
            }
        hideHiddenFields()
    }

    private fun hideHiddenFields() {
        if (isFieldHidden(CustomizableShippingField.Line1)) {
            addressLine1TextInputLayout.visibility = View.GONE
        }
        if (isFieldHidden(CustomizableShippingField.Line2)) {
            addressLine2TextInputLayout.visibility = View.GONE
        }
        if (isFieldHidden(CustomizableShippingField.State)) {
            stateTextInputLayout.visibility = View.GONE
        }
        if (isFieldHidden(CustomizableShippingField.City)) {
            cityTextInputLayout.visibility = View.GONE
        }
        if (isFieldHidden(CustomizableShippingField.PostalCode)) {
            postalCodeTextInputLayout.visibility = View.GONE
        }
        if (isFieldHidden(CustomizableShippingField.Phone)) {
            phoneNumberTextInputLayout.visibility = View.GONE
        }
    }

    private fun updateConfigForCountry(country: Country) {
        when (country.code.value) {
            Locale.US.country -> renderUSForm()
            Locale.UK.country -> renderGreatBritainForm()
            Locale.CANADA.country -> renderCanadianForm()
            else -> renderInternationalForm()
        }

        updatePostalCodeInputFilter(country)

        postalCodeTextInputLayout.visibility =
            if (CountryUtils.doesCountryUsePostalCode(country.code) &&
                !isFieldHidden(CustomizableShippingField.PostalCode)
            ) {
                View.VISIBLE
            } else {
                View.GONE
            }
    }

    private fun updatePostalCodeInputFilter(country: Country) {
        postalCodeEditText.filters = when (country.code.value) {
            Locale.CANADA.country -> arrayOf<InputFilter>(AllCaps())
            else -> arrayOf<InputFilter>()
        }
    }

    private fun renderUSForm() {
        addressLine1TextInputLayout.hint =
            if (isFieldOptional(CustomizableShippingField.Line1)) {
                resources.getString(R.string.stripe_address_label_address_optional)
            } else {
                resources.getString(UiCoreR.string.stripe_address_label_address)
            }
        addressLine2TextInputLayout.hint = resources.getString(
            R.string.stripe_address_label_apt_optional
        )
        postalCodeTextInputLayout.hint =
            if (isFieldOptional(CustomizableShippingField.PostalCode)) {
                resources.getString(R.string.stripe_address_label_zip_code_optional)
            } else {
                resources.getString(CoreR.string.stripe_address_label_zip_code)
            }
        stateTextInputLayout.hint =
            if (isFieldOptional(CustomizableShippingField.State)) {
                resources.getString(R.string.stripe_address_label_state_optional)
            } else {
                resources.getString(CoreR.string.stripe_address_label_state)
            }
        postalCodeEditText.setErrorMessage(resources.getString(UiCoreR.string.stripe_address_zip_invalid))
        stateEditText.setErrorMessage(resources.getString(R.string.stripe_address_state_required))
    }

    private fun renderGreatBritainForm() {
        addressLine1TextInputLayout.hint =
            if (isFieldOptional(CustomizableShippingField.Line1)) {
                resources.getString(R.string.stripe_address_label_address_line1_optional)
            } else {
                resources.getString(CoreR.string.stripe_address_label_address_line1)
            }
        addressLine2TextInputLayout.hint = resources.getString(
            R.string.stripe_address_label_address_line2_optional
        )
        postalCodeTextInputLayout.hint =
            if (isFieldOptional(CustomizableShippingField.PostalCode)) {
                resources.getString(R.string.stripe_address_label_postcode_optional)
            } else {
                resources.getString(R.string.stripe_address_label_postcode)
            }
        stateTextInputLayout.hint =
            if (isFieldOptional(CustomizableShippingField.State)) {
                resources.getString(R.string.stripe_address_label_county_optional)
            } else {
                resources.getString(CoreR.string.stripe_address_label_county)
            }
        postalCodeEditText.setErrorMessage(resources.getString(R.string.stripe_address_postcode_invalid))
        stateEditText.setErrorMessage(resources.getString(R.string.stripe_address_county_required))
    }

    private fun renderCanadianForm() {
        addressLine1TextInputLayout.hint =
            if (isFieldOptional(CustomizableShippingField.Line1)) {
                resources.getString(R.string.stripe_address_label_address_optional)
            } else {
                resources.getString(UiCoreR.string.stripe_address_label_address)
            }
        addressLine2TextInputLayout.hint = resources.getString(R.string.stripe_address_label_apt_optional)
        postalCodeTextInputLayout.hint =
            if (isFieldOptional(CustomizableShippingField.PostalCode)) {
                resources.getString(R.string.stripe_address_label_postal_code_optional)
            } else {
                resources.getString(CoreR.string.stripe_address_label_postal_code)
            }
        stateTextInputLayout.hint =
            if (isFieldOptional(CustomizableShippingField.State)) {
                resources.getString(R.string.stripe_address_label_province_optional)
            } else {
                resources.getString(CoreR.string.stripe_address_label_province)
            }

        postalCodeEditText.setErrorMessage(
            resources.getString(
                R.string.stripe_address_postal_code_invalid
            )
        )
        stateEditText.setErrorMessage(
            resources.getString(
                R.string.stripe_address_province_required
            )
        )
    }

    private fun renderInternationalForm() {
        addressLine1TextInputLayout.hint =
            if (isFieldOptional(CustomizableShippingField.Line1)) {
                resources.getString(R.string.stripe_address_label_address_line1_optional)
            } else {
                resources.getString(CoreR.string.stripe_address_label_address_line1)
            }
        addressLine2TextInputLayout.hint =
            resources.getString(R.string.stripe_address_label_address_line2_optional)

        postalCodeTextInputLayout.hint =
            if (isFieldOptional(CustomizableShippingField.PostalCode)) {
                resources.getString(R.string.stripe_address_label_zip_postal_code_optional)
            } else {
                resources.getString(R.string.stripe_address_label_zip_postal_code)
            }

        stateTextInputLayout.hint =
            if (isFieldOptional(CustomizableShippingField.State)) {
                resources.getString(R.string.stripe_address_label_region_generic_optional)
            } else {
                resources.getString(R.string.stripe_address_label_region_generic)
            }

        postalCodeEditText.setErrorMessage(
            resources.getString(
                UiCoreR.string.stripe_address_zip_postal_invalid
            )
        )
        stateEditText.setErrorMessage(
            resources.getString(
                R.string.stripe_address_region_generic_required
            )
        )
    }
}
