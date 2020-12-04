package com.stripe.android.paymentsheet.ui

import android.content.Context
import androidx.core.view.isVisible
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.PaymentConfiguration
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
        billingAddressView.selectedCountry = Country("ZW", "Zimbabwe")
        idleLooper()
        assertThat(billingAddressView.postalCodeLayout.isVisible)
            .isFalse()
    }

    @Test
    fun `changing selectedCountry to country with postal code should show postal code view`() {
        billingAddressView.selectedCountry = Country("FR", "France")
        idleLooper()
        assertThat(billingAddressView.postalCodeLayout.isVisible)
            .isTrue()
    }
}
