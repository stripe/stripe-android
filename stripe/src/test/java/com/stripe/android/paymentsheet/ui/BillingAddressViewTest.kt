package com.stripe.android.paymentsheet.ui

import android.content.Context
import androidx.core.view.isVisible
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.PaymentConfiguration
import com.stripe.android.model.Address
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.utils.TestUtils.idleLooper
import com.stripe.android.view.ActivityScenarioFactory
import com.stripe.android.view.Country
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Locale
import kotlin.test.BeforeTest
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
class BillingAddressViewTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val activityScenarioFactory = ActivityScenarioFactory(context)

    private val billingAddressView: BillingAddressView by lazy {
        activityScenarioFactory.createView { BillingAddressView(it) }
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
        billingAddressView.selectedCountry = ZIMBABWE
        idleLooper()
        assertThat(billingAddressView.postalCodeLayout.isVisible)
            .isFalse()
    }

    @Test
    fun `changing selectedCountry to country without postal code when level=Required should hide postal code view but show city view`() {
        billingAddressView.level = PaymentSheet.BillingAddressCollectionLevel.Required
        billingAddressView.selectedCountry = ZIMBABWE
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
        billingAddressView.selectedCountry = FRANCE
        idleLooper()
        assertThat(billingAddressView.postalCodeLayout.isVisible)
            .isTrue()
    }

    @Test
    fun `changing selectedCountry to US should show postal code view`() {
        billingAddressView.selectedCountry = USA
        idleLooper()
        assertThat(billingAddressView.postalCodeLayout.isVisible)
            .isTrue()
    }

    @Test
    fun `when selectedCountry is null should show postal code view`() {
        billingAddressView.selectedCountry = null
        idleLooper()
        assertThat(billingAddressView.postalCodeLayout.isVisible)
            .isTrue()
    }

    @Test
    fun `address with no postal code country and no postal code should return expected value`() {
        billingAddressView.selectedCountry = ZIMBABWE
        assertThat(billingAddressView.address.value)
            .isEqualTo(
                Address(
                    country = "ZW"
                )
            )
    }

    @Test
    fun `address with validated postal code country and no postal code should return null`() {
        billingAddressView.selectedCountry = USA
        assertThat(billingAddressView.address.value)
            .isNull()
    }

    @Test
    fun `address with validated postal code country and invalid postal code should return null`() {
        billingAddressView.selectedCountry = USA
        billingAddressView.postalCodeView.setText("abc")
        assertThat(billingAddressView.address.value)
            .isNull()
    }

    @Test
    fun `address with validated postal code country and valid postal code should return expected value`() {
        billingAddressView.selectedCountry = USA
        billingAddressView.postalCodeView.setText("94107")
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
        billingAddressView.selectedCountry = MEXICO
        billingAddressView.postalCodeView.setText("    ")
        assertThat(billingAddressView.address.value)
            .isNull()
    }

    @Test
    fun `address with unvalidated postal code country and non-empty postal code should return expected value`() {
        billingAddressView.selectedCountry = MEXICO
        billingAddressView.postalCodeView.setText("12345")
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
        billingAddressView.selectedCountry = MEXICO
        assertThat(billingAddressView.postalCodeView.inputType)
            .isEqualTo(BillingAddressView.PostalCodeConfig.Global.inputType)

        billingAddressView.selectedCountry = USA
        assertThat(billingAddressView.postalCodeView.inputType)
            .isEqualTo(BillingAddressView.PostalCodeConfig.UnitedStates.inputType)
    }

    @Test
    fun `changing country should update state hint text`() {
        billingAddressView.selectedCountry = MEXICO
        assertThat(billingAddressView.stateLayout.hint)
            .isEqualTo("State / Province / Region")

        billingAddressView.selectedCountry = USA
        assertThat(billingAddressView.stateLayout.hint)
            .isEqualTo("State")
    }

    @Test
    fun `address value should react to level`() {
        billingAddressView.selectedCountry = USA
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

    private companion object {
        private val USA = Country("US", "United States")
        private val FRANCE = Country("FR", "France")
        private val ZIMBABWE = Country("ZW", "Zimbabwe")
        private val MEXICO = Country("MX", "Mexico")
    }
}
