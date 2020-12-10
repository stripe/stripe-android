package com.stripe.android.paymentsheet.ui

import android.content.Context
import androidx.core.view.isVisible
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.PaymentConfiguration
import com.stripe.android.model.Address
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
        billingAddressView.postalCodeView.setText("94107-1234")
        assertThat(billingAddressView.address.value)
            .isEqualTo(
                Address(
                    country = "US",
                    postalCode = "94107-1234"
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

    private companion object {
        private val USA = Country("US", "United States")
        private val FRANCE = Country("FR", "France")
        private val ZIMBABWE = Country("ZW", "Zimbabwe")
        private val MEXICO = Country("MX", "Mexico")
    }
}
