package com.stripe.android.view

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.os.Looper
import android.view.View
import android.widget.LinearLayout
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.google.android.material.textfield.TextInputLayout
import com.google.common.truth.Truth.assertThat
import com.stripe.android.R
import com.stripe.android.core.model.getCountryCode
import com.stripe.android.model.Address
import com.stripe.android.model.ShippingInformation
import com.stripe.android.utils.createTestActivityRule
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.util.Locale
import kotlin.test.BeforeTest
import kotlin.test.Test
import com.stripe.android.core.R as CoreR
import com.stripe.android.uicore.R as UiCoreR

/**
 * Test class for [ShippingInfoWidget]
 */
@RunWith(RobolectricTestRunner::class)
class ShippingInfoWidgetTest {
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
    private lateinit var countryTextInputLayout: CountryTextInputLayout

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val res = context.resources

    @get:Rule
    internal val testActivityRule = createTestActivityRule<TestActivity>()

    @BeforeTest
    fun setup() {
        Locale.setDefault(Locale.US)

        ActivityScenario.launch(TestActivity::class.java).use { activityScenario ->
            activityScenario.onActivity {
                shippingInfoWidget = ShippingInfoWidget(it)
                it.layout.addView(shippingInfoWidget)

                addressLine1TextInputLayout =
                    shippingInfoWidget.findViewById(R.id.tl_address_line1_aaw)
                addressLine2TextInputLayout =
                    shippingInfoWidget.findViewById(R.id.tl_address_line2_aaw)
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
                countryTextInputLayout =
                    shippingInfoWidget.findViewById(R.id.country_autocomplete_aaw)
            }
        }
    }

    @Test
    fun shippingInfoWidget_whenCountryChanged_fieldsRenderCorrectly() {
        countryTextInputLayout.updateUiForCountryEntered(Locale.US.getCountryCode())
        assertThat(addressLine1TextInputLayout.hint)
            .isEqualTo(res.getString(UiCoreR.string.stripe_address_label_address))
        assertThat(addressLine2TextInputLayout.hint)
            .isEqualTo(res.getString(R.string.stripe_address_label_apt_optional))
        assertThat(postalCodeTextInputLayout.hint)
            .isEqualTo(res.getString(CoreR.string.stripe_address_label_zip_code))
        assertThat(stateTextInputLayout.hint)
            .isEqualTo(res.getString(CoreR.string.stripe_address_label_state))

        countryTextInputLayout.updateUiForCountryEntered(Locale.CANADA.getCountryCode())
        assertThat(addressLine1TextInputLayout.hint)
            .isEqualTo(res.getString(UiCoreR.string.stripe_address_label_address))
        assertThat(addressLine2TextInputLayout.hint)
            .isEqualTo(res.getString(R.string.stripe_address_label_apt_optional))
        assertThat(postalCodeTextInputLayout.hint)
            .isEqualTo(res.getString(CoreR.string.stripe_address_label_postal_code))
        assertThat(stateTextInputLayout.hint)
            .isEqualTo(res.getString(CoreR.string.stripe_address_label_province))

        countryTextInputLayout.updateUiForCountryEntered(Locale.UK.getCountryCode())
        assertThat(addressLine1TextInputLayout.hint)
            .isEqualTo(res.getString(CoreR.string.stripe_address_label_address_line1))
        assertThat(addressLine2TextInputLayout.hint)
            .isEqualTo(res.getString(R.string.stripe_address_label_address_line2_optional))
        assertThat(postalCodeTextInputLayout.hint)
            .isEqualTo(res.getString(R.string.stripe_address_label_postcode))
        assertThat(stateTextInputLayout.hint)
            .isEqualTo(res.getString(CoreR.string.stripe_address_label_county))

        countryTextInputLayout.updateUiForCountryEntered(
            Locale("", NO_POSTAL_CODE_COUNTRY_CODE).getCountryCode()
        )
        assertThat(addressLine1TextInputLayout.hint)
            .isEqualTo(res.getString(CoreR.string.stripe_address_label_address_line1))
        assertThat(addressLine2TextInputLayout.hint)
            .isEqualTo(res.getString(R.string.stripe_address_label_address_line2_optional))
        assertThat(postalCodeTextInputLayout.visibility)
            .isEqualTo(View.GONE)
        assertThat(stateTextInputLayout.hint)
            .isEqualTo(res.getString(R.string.stripe_address_label_region_generic))
    }

    @Test
    fun shippingInfoWidget_addressSaved_validationTriggers() {
        countryTextInputLayout.updateUiForCountryEntered(Locale.US.getCountryCode())
        assertThat(shippingInfoWidget.validateAllFields())
            .isFalse()
        addressLine1EditText.setText("123 Fake Address")
        nameEditText.setText("Fake Name")
        cityEditText.setText("Fake City")
        postalEditText.setText("12345")
        stateEditText.setText("CA")
        phoneEditText.setText("(123) 456 - 7890")
        assertThat(shippingInfoWidget.validateAllFields())
            .isTrue()
        postalEditText.setText("")
        assertThat(shippingInfoWidget.validateAllFields())
            .isFalse()
        postalEditText.setText("ABCDEF")
        assertThat(shippingInfoWidget.validateAllFields())
            .isFalse()
        countryTextInputLayout
            .updateUiForCountryEntered(Locale("", NO_POSTAL_CODE_COUNTRY_CODE).getCountryCode())
        assertThat(shippingInfoWidget.validateAllFields())
            .isTrue()
    }

    @Test
    fun shippingInfoWidget_whenValidationFails_errorTextRenders() {
        countryTextInputLayout.updateUiForCountryEntered(Locale.US.getCountryCode())
        assertThat(shippingInfoWidget.validateAllFields())
            .isFalse()

        assertThat(addressLine1TextInputLayout.isErrorEnabled)
            .isTrue()
        assertThat(cityTextInputLayout.isErrorEnabled)
            .isTrue()
        assertThat(nameTextInputLayout.isErrorEnabled)
            .isTrue()
        assertThat(postalCodeTextInputLayout.isErrorEnabled)
            .isTrue()
        assertThat(stateTextInputLayout.isErrorEnabled)
            .isTrue()
        addressLine1EditText.setText("123 Fake Address")
        nameEditText.setText("Fake Name")
        cityEditText.setText("Fake City")
        postalEditText.setText("12345")
        stateEditText.setText("CA")
        phoneEditText.setText("8675555309")

        shadowOf(Looper.getMainLooper()).idle()

        assertThat(shippingInfoWidget.validateAllFields())
            .isTrue()

        assertThat(addressLine1TextInputLayout.isErrorEnabled)
            .isFalse()
        assertThat(cityTextInputLayout.isErrorEnabled)
            .isFalse()
        assertThat(nameTextInputLayout.isErrorEnabled)
            .isFalse()
        assertThat(postalCodeTextInputLayout.isErrorEnabled)
            .isFalse()
        assertThat(stateTextInputLayout.isErrorEnabled)
            .isFalse()

        nameEditText.setText("      ")
        assertThat(shippingInfoWidget.validateAllFields())
            .isFalse()
        assertThat(nameTextInputLayout.isErrorEnabled)
            .isTrue()
        nameEditText.setText("Valid Name")
        assertThat(shippingInfoWidget.validateAllFields())
            .isTrue()

        postalEditText.setText("")
        assertThat(shippingInfoWidget.validateAllFields())
            .isFalse()
        assertThat(postalCodeTextInputLayout.isErrorEnabled)
            .isTrue()

        countryTextInputLayout
            .updateUiForCountryEntered(
                Locale("", NO_POSTAL_CODE_COUNTRY_CODE).getCountryCode()
            )
        assertThat(shippingInfoWidget.validateAllFields())
            .isTrue()
        assertThat(stateTextInputLayout.isErrorEnabled)
            .isFalse()
    }

    @Test
    fun shippingInfoWidget_whenErrorOccurs_errorsRenderInternationalized() {
        countryTextInputLayout.updateUiForCountryEntered(Locale.US.getCountryCode())
        assertThat(shippingInfoWidget.validateAllFields())
            .isFalse()

        assertThat(stateTextInputLayout.error)
            .isEqualTo(res.getString(R.string.stripe_address_state_required))
        assertThat(postalCodeTextInputLayout.error)
            .isEqualTo(res.getString(UiCoreR.string.stripe_address_zip_invalid))

        countryTextInputLayout.updateUiForCountryEntered(Locale.UK.getCountryCode())
        assertThat(shippingInfoWidget.validateAllFields())
            .isFalse()
        assertThat(stateTextInputLayout.error)
            .isEqualTo(res.getString(R.string.stripe_address_county_required))
        assertThat(postalCodeTextInputLayout.error)
            .isEqualTo(res.getString(R.string.stripe_address_postcode_invalid))

        countryTextInputLayout.updateUiForCountryEntered(Locale.CANADA.getCountryCode())
        assertThat(shippingInfoWidget.validateAllFields())
            .isFalse()
        assertThat(stateTextInputLayout.error)
            .isEqualTo(res.getString(R.string.stripe_address_province_required))
        assertThat(postalCodeTextInputLayout.error)
            .isEqualTo(res.getString(R.string.stripe_address_postal_code_invalid))

        countryTextInputLayout
            .updateUiForCountryEntered(Locale("", NO_POSTAL_CODE_COUNTRY_CODE).getCountryCode())
        assertThat(shippingInfoWidget.validateAllFields())
            .isFalse()
        assertThat(stateTextInputLayout.error)
            .isEqualTo(res.getString(R.string.stripe_address_region_generic_required))
    }

    @Test
    fun shippingInfoWidget_whenFieldsOptional_markedAsOptional() {
        assertThat(postalCodeTextInputLayout.hint.toString())
            .isEqualTo(res.getString(CoreR.string.stripe_address_label_zip_code))
        assertThat(nameTextInputLayout.hint.toString())
            .isEqualTo(res.getString(CoreR.string.stripe_address_label_full_name))
        shippingInfoWidget.optionalFields = listOf(
            ShippingInfoWidget.CustomizableShippingField.PostalCode
        )
        assertThat(postalCodeTextInputLayout.hint.toString())
            .isEqualTo(res.getString(R.string.stripe_address_label_zip_code_optional))
        assertThat(nameTextInputLayout.hint.toString())
            .isEqualTo(res.getString(CoreR.string.stripe_address_label_full_name))
        countryTextInputLayout.updateUiForCountryEntered(Locale.CANADA.getCountryCode())
        assertThat(stateTextInputLayout.hint.toString())
            .isEqualTo(res.getString(CoreR.string.stripe_address_label_province))
        shippingInfoWidget.optionalFields = listOf(
            ShippingInfoWidget.CustomizableShippingField.PostalCode,
            ShippingInfoWidget.CustomizableShippingField.State
        )
        assertThat(stateTextInputLayout.hint.toString())
            .isEqualTo(res.getString(R.string.stripe_address_label_province_optional))
    }

    @Test
    fun shippingInfoWidget_whenFieldsHidden_renderedHidden() {
        assertThat(nameTextInputLayout.visibility)
            .isEqualTo(View.VISIBLE)
        assertThat(postalCodeTextInputLayout.visibility)
            .isEqualTo(View.VISIBLE)
        shippingInfoWidget.hiddenFields = listOf(
            ShippingInfoWidget.CustomizableShippingField.PostalCode
        )
        assertThat(postalCodeTextInputLayout.visibility)
            .isEqualTo(View.GONE)
        countryTextInputLayout.updateUiForCountryEntered(
            Locale.CANADA.getCountryCode()
        )
        assertThat(postalCodeTextInputLayout.visibility)
            .isEqualTo(View.GONE)
    }

    @Test
    fun getShippingInfo_whenShippingInfoInvalid_returnsNull() {
        assertThat(shippingInfoWidget.shippingInformation)
            .isNull()
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
        countryTextInputLayout.updateUiForCountryEntered(Locale.US.getCountryCode())

        assertThat(shippingInfoWidget.shippingInformation)
            .isEqualTo(SHIPPING_INFO)
    }

    @Test
    fun populateShippingInfo_whenShippingInfoProvided_populates() {
        shippingInfoWidget.populateShippingInfo(SHIPPING_INFO)
        assertThat(stateEditText.fieldText)
            .isEqualTo("CA")
        assertThat(cityEditText.fieldText)
            .isEqualTo("San Francisco")
        assertThat(addressLine1EditText.fieldText)
            .isEqualTo("185 Berry St")
        assertThat(addressLine2EditText.fieldText)
            .isEqualTo("10th Floor")
        assertThat(phoneEditText.fieldText)
            .isEqualTo("(123) 456 - 7890")
        assertThat(postalEditText.fieldText)
            .isEqualTo("12345")
        assertThat(nameEditText.fieldText)
            .isEqualTo("Fake Name")
        assertThat(countryTextInputLayout.selectedCountry?.code?.value)
            .isEqualTo("US")
    }

    @Test
    fun getSelectedShippingCountry_whenShippingInfoProvided_returnsExpected() {
        shippingInfoWidget.setAllowedCountryCodes(setOf("US", "CA"))
        shippingInfoWidget.populateShippingInfo(SHIPPING_INFO_CA)
        assertThat(stateEditText.fieldText)
            .isEqualTo("Ontario")
        assertThat(cityEditText.fieldText)
            .isEqualTo("Ontario")
        assertThat(addressLine1EditText.fieldText)
            .isEqualTo("185 Berry St")
        assertThat(addressLine2EditText.fieldText)
            .isEqualTo("10th Floor")
        assertThat(phoneEditText.fieldText)
            .isEqualTo("416-759-0260")
        assertThat(postalEditText.fieldText)
            .isEqualTo("M4B1B5")
        assertThat(nameEditText.fieldText)
            .isEqualTo("Fake Name")
        assertThat(countryTextInputLayout.selectedCountry?.code?.value)
            .isEqualTo("CA")
    }

    private companion object {
        private const val NO_POSTAL_CODE_COUNTRY_CODE = "ZW" // Zimbabwe

        private val SHIPPING_INFO = ShippingInformation(
            Address.Builder()
                .setCity("San Francisco")
                .setState("CA")
                .setCountry("US")
                .setLine1("185 Berry St")
                .setLine2("10th Floor")
                .setPostalCode("12345")
                .build(),
            "Fake Name",
            "(123) 456 - 7890"
        )

        private val SHIPPING_INFO_CA = ShippingInformation(
            Address.Builder()
                .setCity("Ontario")
                .setState("Ontario")
                .setCountry("CA")
                .setLine1("185 Berry St")
                .setLine2("10th Floor")
                .setPostalCode("M4B1B5")
                .build(),
            "Fake Name",
            "416-759-0260"
        )
    }

    internal class TestActivity : Activity() {
        lateinit var layout: LinearLayout

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

            layout = LinearLayout(this)
            setContentView(layout)
        }
    }
}
