package com.stripe.android.paymentsheet.ui

import android.content.Context
import androidx.core.view.isVisible
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.PaymentConfiguration
import com.stripe.android.model.Address
import com.stripe.android.model.AddressFixtures
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.utils.TestUtils.idleLooper
import com.stripe.android.view.ActivityScenarioFactory
import com.stripe.android.view.Country
import com.stripe.android.view.CountryCode
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Locale
import kotlin.test.BeforeTest
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
class BillingAddressViewTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val activityScenarioFactory = ActivityScenarioFactory(context)

    private val mockPostalCodeViewListener: BillingAddressView.PostalCodeViewListener = mock()

    private val billingAddressView: BillingAddressView by lazy {
        activityScenarioFactory.createView {
            BillingAddressView(it).also { bav ->
                bav.postalCodeViewListener = mockPostalCodeViewListener
            }
        }
    }

    @BeforeTest
    fun setup() {
        PaymentConfiguration.init(context, ApiKeyFixtures.FAKE_PUBLISHABLE_KEY)
    }

    @Test
    fun `default country when locale is US should be United States`() {
        Locale.setDefault(Locale.US)
        assertThat(
            billingAddressView.countryView.text.toString()
        ).isEqualTo("United States")
    }

    @Test
    fun `changing selectedCountry to country without postal code should hide postal code view`() {
        setupPostalCode(ZIMBABWE)
        assertThat(billingAddressView.postalCodeLayout.isVisible)
            .isFalse()
    }

    @Test
    fun `changing selectedCountry to country without postal code when level=Required should hide postal code view but show city view`() {
        billingAddressView.level = PaymentSheet.BillingAddressCollectionLevel.Required
        billingAddressView.countryLayout.selectedCountryCode = ZIMBABWE.code
        idleLooper()
        assertThat(billingAddressView.postalCodeLayout.isVisible)
            .isFalse()

        assertThat(billingAddressView.cityPostalContainer.isVisible)
            .isTrue()
        assertThat(billingAddressView.cityView.isVisible)
            .isTrue()
        assertThat(billingAddressView.cityLayout.isVisible)
            .isTrue()
    }

    @Test
    fun `changing selectedCountry to France should show postal code view`() {
        setupPostalCode(FRANCE)
        assertThat(billingAddressView.postalCodeLayout.isVisible)
            .isTrue()
    }

    @Test
    fun `changing selectedCountry to US should show postal code view`() {
        setupPostalCode(USA)
        assertThat(billingAddressView.postalCodeLayout.isVisible)
            .isTrue()
    }

    @Test
    fun `changing selectedCountry to US should show postal code view and set shouldShowError to true`() {
        billingAddressView.postalCodeView.setText("123")

        // This will have the effect of switching the country selected
        billingAddressView.countryLayout.selectedCountryCode = GB.code
        billingAddressView.countryLayout.updateUiForCountryEntered(USA.code)
        idleLooper()

        assertThat(billingAddressView.postalCodeLayout.isVisible)
            .isTrue()
        assertThat(billingAddressView.postalCodeView.shouldShowError)
            .isTrue()
    }

    @Test
    fun `changing selectedCountry to UK should show postal code view and set shouldShowError to true`() {
        setupPostalCode(GB, "123")
        assertThat(billingAddressView.postalCodeLayout.isVisible)
            .isTrue()
        assertThat(billingAddressView.postalCodeView.shouldShowError)
            .isFalse()
    }

    @Test
    fun `when focus is lost and zip code is incomplete it should show error`() {
        setupPostalCode(USA, "123", hasFocus = false)
        assertThat(billingAddressView.postalCodeLayout.isVisible)
            .isTrue()
        assertThat(billingAddressView.postalCodeView.shouldShowError)
            .isTrue()
    }

    @Test
    fun `when focus is lost and zip code is valid then postal listener's onLosingFocus is correctly called`() {
        setupPostalCode(USA, "94107", hasFocus = false)
        verify(mockPostalCodeViewListener).onLosingFocus(USA, true)
    }

    @Test
    fun `when focus is lost and zip code is invalid then postal listener's onLosingFocus is correctly called`() {
        setupPostalCode(USA, "123", hasFocus = false)
        verify(mockPostalCodeViewListener).onLosingFocus(USA, false)
    }

    @Test
    fun `when focus is gained and zip code is valid then postal listener's onGainingFocus is correctly called`() {
        setupPostalCode(USA, "94107", hasFocus = true)
        verify(mockPostalCodeViewListener).onGainingFocus(USA, true)
    }

    @Test
    fun `when focus is gained and zip code is invalid then postal listener's onGainingFocus is correctly called`() {
        setupPostalCode(USA, "123", hasFocus = true)
        verify(mockPostalCodeViewListener).onGainingFocus(USA, false)
    }

    @Test
    fun `when country is changed and zip code is valid then listener's onCountryChanged is correctly called`() {
        setupPostalCode(USA, "94107")
        verify(mockPostalCodeViewListener).onCountryChanged(USA, true)
    }

    @Test
    fun `when country is changed and zip code is invalid then listener's onCountryChanged is correctly called`() {
        setupPostalCode(USA, "123")
        verify(mockPostalCodeViewListener).onCountryChanged(USA, false)
    }

    @Test
    fun `when selectedCountry is null should show postal code view`() {
        setupPostalCode(null)
        assertThat(billingAddressView.postalCodeLayout.isVisible)
            .isTrue()
    }

    @Test
    fun `address with no postal code country and no postal code should return expected value`() {
        setupPostalCode(ZIMBABWE)
        assertThat(billingAddressView.address.value)
            .isEqualTo(
                Address(
                    country = "ZW"
                )
            )
    }

    @Test
    fun `address with validated postal code country and no postal code should return null`() {
        setupPostalCode(USA)
        assertThat(billingAddressView.address.value)
            .isNull()
    }

    @Test
    fun `address with validated postal code country and invalid postal code should return null`() {
        setupPostalCode(USA, "abc")
        assertThat(billingAddressView.address.value)
            .isNull()
    }

    @Test
    fun `address with validated postal code country and valid postal code should return expected value`() {
        setupPostalCode(USA, "94107")
        assertThat(billingAddressView.address.value)
            .isEqualTo(
                Address(
                    country = "US",
                    postalCode = "94107"
                )
            )
    }

    @Test
    fun `address with unvalidated postal code country and null postal code should return null`() {
        setupPostalCode(MEXICO, "    ")
        assertThat(billingAddressView.address.value)
            .isNull()
    }

    @Test
    fun `address with unvalidated postal code country and non-empty postal code should return expected value`() {
        setupPostalCode(MEXICO, "12345")
        assertThat(billingAddressView.address.value)
            .isEqualTo(
                Address(
                    country = "MX",
                    postalCode = "12345"
                )
            )
    }

    @Test
    fun `changing country should update postalCodeView inputType`() {
        billingAddressView.countryLayout.selectedCountryCode = MEXICO.code
        assertThat(billingAddressView.postalCodeView.inputType)
            .isEqualTo(BillingAddressView.PostalCodeConfig.Global.inputType)

        billingAddressView.countryLayout.selectedCountryCode = USA.code
        assertThat(billingAddressView.postalCodeView.inputType)
            .isEqualTo(BillingAddressView.PostalCodeConfig.UnitedStates.inputType)
    }

    @Test
    fun `changing country should update state hint text`() {
        billingAddressView.countryLayout.selectedCountryCode = MEXICO.code
        assertThat(billingAddressView.stateLayout.hint)
            .isEqualTo("State / Province / Region")

        billingAddressView.countryLayout.selectedCountryCode = USA.code
        assertThat(billingAddressView.stateLayout.hint)
            .isEqualTo("State")
    }

    @Test
    fun `Calling populate should not result in an erroneous zip code if it has a value`() {
        billingAddressView.populate(AddressFixtures.ADDRESS)
        assertThat(billingAddressView.postalCodeView.shouldShowError).isFalse()
    }

    @Test
    fun `address value should react to level`() {
        billingAddressView.countryLayout.selectedCountryCode = USA.code
        billingAddressView.postalCodeView.setText("94107")

        billingAddressView.address1View.setText("123 Main St")
        billingAddressView.address2View.setText("Apt 4")
        billingAddressView.cityView.setText("San Francisco")
        billingAddressView.stateView.setText("California")

        billingAddressView.level = PaymentSheet.BillingAddressCollectionLevel.Required
        assertThat(billingAddressView.address.value)
            .isEqualTo(
                Address(
                    line1 = "123 Main St",
                    line2 = "Apt 4",
                    city = "San Francisco",
                    country = "US",
                    postalCode = "94107",
                    state = "California"
                )
            )

        billingAddressView.level = PaymentSheet.BillingAddressCollectionLevel.Automatic
        assertThat(billingAddressView.address.value)
            .isEqualTo(
                Address(
                    country = "US",
                    postalCode = "94107"
                )
            )
    }

    private fun setupPostalCode(
        country: Country?,
        postalCode: String? = null,
        hasFocus: Boolean? = null
    ) {
        postalCode?.let {
            billingAddressView.postalCodeView.setText(it)
        }
        billingAddressView.countryLayout.selectedCountryCode = country?.code
        hasFocus?.let {
            billingAddressView.postalCodeView.getParentOnFocusChangeListener()!!.onFocusChange(
                billingAddressView.postalCodeView,
                it
            )
        }
        idleLooper()
    }

    private companion object {
        private val USA = Country(CountryCode.US, "United States")
        private val GB = Country(CountryCode.GB, "United Kingdom")
        private val FRANCE = Country(CountryCode("FR"), "France")
        private val ZIMBABWE = Country(CountryCode("ZW"), "Zimbabwe")
        private val MEXICO = Country(CountryCode("MX"), "Mexico")
    }
}
