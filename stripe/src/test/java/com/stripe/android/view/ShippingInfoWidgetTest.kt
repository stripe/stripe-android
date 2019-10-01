package com.stripe.android.view

import android.view.View
import com.google.android.material.textfield.TextInputLayout
import com.stripe.android.R
import com.stripe.android.model.Address
import com.stripe.android.model.ShippingInformation
import java.util.Locale
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.runner.RunWith
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner

/**
 * Test class for [ShippingInfoWidget]
 */
@RunWith(RobolectricTestRunner::class)
class ShippingInfoWidgetTest : BaseViewTest<ShippingInfoTestActivity>(
    ShippingInfoTestActivity::class.java
) {
    private lateinit var shippingInfoWidget: ShippingInfoWidget
    private lateinit var addressLine1TextInputLayout: TextInputLayout
    private lateinit var addressLine2TextInputLayout: TextInputLayout
    private lateinit var cityTextInputLayout: TextInputLayout
    private lateinit var nameTextInputLayout: TextInputLayout
    private lateinit var postalCodeTextInputLayout: TextInputLayout
    private lateinit var stateTextInputLayout: TextInputLayout
    private lateinit var addressLine1EditText: StripeEditText
    private lateinit var addressLine2EditText: StripeEditText
    private lateinit var postalEditText: StripeEditText
    private lateinit var cityEditText: StripeEditText
    private lateinit var nameEditText: StripeEditText
    private lateinit var stateEditText: StripeEditText
    private lateinit var phoneEditText: StripeEditText
    private lateinit var countryAutoCompleteTextView: CountryAutoCompleteTextView

    private val mNoPostalCodeCountry = "ZW" // Zimbabwe
    private var mShippingInfo: ShippingInformation? = null

    @BeforeTest
    fun setup() {
        MockitoAnnotations.initMocks(this)
        Locale.setDefault(Locale.US)
        shippingInfoWidget = createStartedActivity().shippingInfoWidget
        addressLine1TextInputLayout = shippingInfoWidget.findViewById(R.id.tl_address_line1_aaw)
        addressLine2TextInputLayout = shippingInfoWidget.findViewById(R.id.tl_address_line2_aaw)
        cityTextInputLayout = shippingInfoWidget.findViewById(R.id.tl_city_aaw)
        nameTextInputLayout = shippingInfoWidget.findViewById(R.id.tl_name_aaw)
        postalCodeTextInputLayout = shippingInfoWidget.findViewById(R.id.tl_postal_code_aaw)
        stateTextInputLayout = shippingInfoWidget.findViewById(R.id.tl_state_aaw)
        addressLine1EditText = shippingInfoWidget.findViewById(R.id.et_address_line_one_aaw)
        addressLine2EditText = shippingInfoWidget.findViewById(R.id.et_address_line_two_aaw)
        cityEditText = shippingInfoWidget.findViewById(R.id.et_city_aaw)
        nameEditText = shippingInfoWidget.findViewById(R.id.et_name_aaw)
        postalEditText = shippingInfoWidget.findViewById(R.id.et_postal_code_aaw)
        stateEditText = shippingInfoWidget.findViewById(R.id.et_state_aaw)
        phoneEditText = shippingInfoWidget.findViewById(R.id.et_phone_number_aaw)
        countryAutoCompleteTextView = shippingInfoWidget.findViewById(R.id.country_autocomplete_aaw)
        val address = Address.Builder()
            .setCity("San Francisco")
            .setState("CA")
            .setCountry("US")
            .setLine1("185 Berry St")
            .setLine2("10th Floor")
            .setPostalCode("12345")
            .build()
        mShippingInfo = ShippingInformation(address, "Fake Name", "(123) 456 - 7890")
    }

    @AfterTest
    override fun tearDown() {
        super.tearDown()
    }

    @Test
    fun shippingInfoWidget_whenCountryChanged_fieldsRenderCorrectly() {
        countryAutoCompleteTextView.updateUiForCountryEntered(Locale.US.displayCountry)
        assertEquals(addressLine1TextInputLayout.hint, shippingInfoWidget.resources.getString(R.string.address_label_address))
        assertEquals(addressLine2TextInputLayout.hint, shippingInfoWidget.resources.getString(R.string.address_label_apt_optional))
        assertEquals(postalCodeTextInputLayout.hint, shippingInfoWidget.resources.getString(R.string.address_label_zip_code))
        assertEquals(stateTextInputLayout.hint, shippingInfoWidget.resources.getString(R.string.address_label_state))

        countryAutoCompleteTextView.updateUiForCountryEntered(Locale.CANADA.displayCountry)
        assertEquals(addressLine1TextInputLayout.hint, shippingInfoWidget.resources.getString(R.string.address_label_address))
        assertEquals(addressLine2TextInputLayout.hint, shippingInfoWidget.resources.getString(R.string.address_label_apt_optional))
        assertEquals(postalCodeTextInputLayout.hint, shippingInfoWidget.resources.getString(R.string.address_label_postal_code))
        assertEquals(stateTextInputLayout.hint, shippingInfoWidget.resources.getString(R.string.address_label_province))

        countryAutoCompleteTextView.updateUiForCountryEntered(Locale.UK.displayCountry)
        assertEquals(addressLine1TextInputLayout.hint, shippingInfoWidget.resources.getString(R.string.address_label_address_line1))
        assertEquals(addressLine2TextInputLayout.hint, shippingInfoWidget.resources.getString(R.string.address_label_address_line2_optional))
        assertEquals(postalCodeTextInputLayout.hint, shippingInfoWidget.resources.getString(R.string.address_label_postcode))
        assertEquals(stateTextInputLayout.hint, shippingInfoWidget.resources.getString(R.string.address_label_county))

        countryAutoCompleteTextView.updateUiForCountryEntered(Locale("", mNoPostalCodeCountry).displayCountry)
        assertEquals(addressLine1TextInputLayout.hint, shippingInfoWidget.resources.getString(R.string.address_label_address_line1))
        assertEquals(addressLine2TextInputLayout.hint, shippingInfoWidget.resources.getString(R.string.address_label_address_line2_optional))
        assertEquals(postalCodeTextInputLayout.visibility, View.GONE)
        assertEquals(stateTextInputLayout.hint, shippingInfoWidget.resources.getString(R.string.address_label_region_generic))
    }

    @Test
    fun shippingInfoWidget_addressSaved_validationTriggers() {
        countryAutoCompleteTextView.updateUiForCountryEntered(Locale.US.displayCountry)
        assertFalse(shippingInfoWidget.validateAllFields())
        addressLine1EditText.setText("123 Fake Address")
        nameEditText.setText("Fake Name")
        cityEditText.setText("Fake City")
        postalEditText.setText("12345")
        stateEditText.setText("CA")
        phoneEditText.setText("(123) 456 - 7890")
        assertTrue(shippingInfoWidget.validateAllFields())
        postalEditText.setText("")
        assertFalse(shippingInfoWidget.validateAllFields())
        postalEditText.setText("ABCDEF")
        assertFalse(shippingInfoWidget.validateAllFields())
        countryAutoCompleteTextView
            .updateUiForCountryEntered(Locale("", mNoPostalCodeCountry).displayCountry)
        assertTrue(shippingInfoWidget.validateAllFields())
    }

    @Test
    fun shippingInfoWidget_whenValidationFails_errorTextRenders() {
        countryAutoCompleteTextView.updateUiForCountryEntered(Locale.US.displayCountry)
        shippingInfoWidget.validateAllFields()
        assertTrue(addressLine1TextInputLayout.isErrorEnabled)
        assertTrue(cityTextInputLayout.isErrorEnabled)
        assertTrue(nameTextInputLayout.isErrorEnabled)
        assertTrue(postalCodeTextInputLayout.isErrorEnabled)
        assertTrue(stateTextInputLayout.isErrorEnabled)
        addressLine1EditText.setText("123 Fake Address")
        nameEditText.setText("Fake Name")
        cityEditText.setText("Fake City")
        postalEditText.setText("12345")
        stateEditText.setText("CA")
        shippingInfoWidget.validateAllFields()
        assertFalse(addressLine1TextInputLayout.isErrorEnabled)
        assertFalse(cityTextInputLayout.isErrorEnabled)
        assertFalse(nameTextInputLayout.isErrorEnabled)
        assertFalse(postalCodeTextInputLayout.isErrorEnabled)
        assertFalse(stateTextInputLayout.isErrorEnabled)
        postalEditText.setText("")
        shippingInfoWidget.validateAllFields()
        assertTrue(postalCodeTextInputLayout.isErrorEnabled)
        countryAutoCompleteTextView
            .updateUiForCountryEntered(Locale("", mNoPostalCodeCountry).displayCountry)
        shippingInfoWidget.validateAllFields()
        assertFalse(stateTextInputLayout.isErrorEnabled)
    }

    @Test
    fun shippingInfoWidget_whenErrorOccurs_errorsRenderInternationalized() {
        countryAutoCompleteTextView.updateUiForCountryEntered(Locale.US.displayCountry)
        shippingInfoWidget.validateAllFields()
        assertEquals(stateTextInputLayout.error, shippingInfoWidget.resources.getString(R.string.address_state_required))
        assertEquals(postalCodeTextInputLayout.error, shippingInfoWidget.resources.getString(R.string.address_zip_invalid))

        countryAutoCompleteTextView.updateUiForCountryEntered(Locale.UK.displayCountry)
        shippingInfoWidget.validateAllFields()
        assertEquals(stateTextInputLayout.error, shippingInfoWidget.resources.getString(R.string.address_county_required))
        assertEquals(postalCodeTextInputLayout.error, shippingInfoWidget.resources.getString(R.string.address_postcode_invalid))

        countryAutoCompleteTextView.updateUiForCountryEntered(Locale.CANADA.displayCountry)
        shippingInfoWidget.validateAllFields()
        assertEquals(stateTextInputLayout.error, shippingInfoWidget.resources.getString(R.string.address_province_required))
        assertEquals(postalCodeTextInputLayout.error, shippingInfoWidget.resources.getString(R.string.address_postal_code_invalid))

        countryAutoCompleteTextView
            .updateUiForCountryEntered(Locale("", mNoPostalCodeCountry).displayCountry)
        shippingInfoWidget.validateAllFields()
        assertEquals(stateTextInputLayout.error, shippingInfoWidget.resources.getString(R.string.address_region_generic_required))
    }

    @Test
    fun shippingInfoWidget_whenFieldsOptional_markedAsOptional() {
        assertEquals(postalCodeTextInputLayout.hint.toString(), shippingInfoWidget.resources.getString(R.string.address_label_zip_code))
        assertEquals(nameTextInputLayout.hint.toString(), shippingInfoWidget.resources.getString(R.string.address_label_name))
        shippingInfoWidget.setOptionalFields(
            listOf(ShippingInfoWidget.CustomizableShippingField.POSTAL_CODE_FIELD)
        )
        assertEquals(postalCodeTextInputLayout.hint.toString(), shippingInfoWidget.resources.getString(R.string.address_label_zip_code_optional))
        assertEquals(nameTextInputLayout.hint.toString(), shippingInfoWidget.resources.getString(R.string.address_label_name))
        countryAutoCompleteTextView.updateUiForCountryEntered(Locale.CANADA.displayCountry)
        assertEquals(stateTextInputLayout.hint.toString(), shippingInfoWidget.resources.getString(R.string.address_label_province))
        shippingInfoWidget.setOptionalFields(listOf(
            ShippingInfoWidget.CustomizableShippingField.POSTAL_CODE_FIELD,
            ShippingInfoWidget.CustomizableShippingField.STATE_FIELD
        ))
        assertEquals(
            stateTextInputLayout.hint.toString(),
            shippingInfoWidget.resources.getString(R.string.address_label_province_optional)
        )
    }

    @Test
    fun shippingInfoWidget_whenFieldsHidden_renderedHidden() {
        assertEquals(nameTextInputLayout.visibility, View.VISIBLE)
        assertEquals(postalCodeTextInputLayout.visibility, View.VISIBLE)
        shippingInfoWidget
            .setHiddenFields(listOf(ShippingInfoWidget.CustomizableShippingField.POSTAL_CODE_FIELD))
        assertEquals(postalCodeTextInputLayout.visibility, View.GONE)
        countryAutoCompleteTextView.updateUiForCountryEntered(Locale.CANADA.displayCountry)
        assertEquals(postalCodeTextInputLayout.visibility, View.GONE)
    }

    @Test
    fun getShippingInfo_whenShippingInfoInvalid_returnsNull() {
        assertNull(shippingInfoWidget.shippingInformation)
    }

    @Test
    fun getShippingInfo_whenShippingInfoValid_returnsExpected() {
        stateEditText.setText("CA")
        cityEditText.setText("San Francisco")
        addressLine1EditText.setText("185 Berry St")
        addressLine2EditText.setText("10th Floor")
        nameEditText.setText("Fake Name")
        phoneEditText.setText("(123) 456 - 7890")
        postalEditText.setText("12345")
        countryAutoCompleteTextView.updateUiForCountryEntered(Locale.US.displayCountry)
        val inputShippingInfo = shippingInfoWidget.shippingInformation
        assertEquals(inputShippingInfo, mShippingInfo)
    }

    @Test
    fun populateShippingInfo_whenShippingInfoProvided_populates() {
        shippingInfoWidget.populateShippingInfo(mShippingInfo)
        assertEquals(stateEditText.text.toString(), "CA")
        assertEquals(cityEditText.text.toString(), "San Francisco")
        assertEquals(addressLine1EditText.text.toString(), "185 Berry St")
        assertEquals(addressLine2EditText.text.toString(), "10th Floor")
        assertEquals(phoneEditText.text.toString(), "(123) 456 - 7890")
        assertEquals(postalEditText.text.toString(), "12345")
        assertEquals(nameEditText.text.toString(), "Fake Name")
        assertEquals(countryAutoCompleteTextView.selectedCountryCode, "US")
    }
}
