package com.stripe.android.link

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.link.model.StripeIntentFixtures
import com.stripe.android.link.ui.inline.UserInput
import com.stripe.android.link.utils.FakeAndroidKeyStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LinkInteractorTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val config = LinkPaymentLauncher.Configuration(
        stripeIntent = StripeIntentFixtures.PI_SUCCEEDED,
        merchantName = MERCHANT_NAME,
        customerName = CUSTOMER_NAME,
        customerEmail = CUSTOMER_EMAIL,
        customerPhone = CUSTOMER_PHONE,
        customerBillingCountryCode = CUSTOMER_BILLING_COUNTRY_CODE,
        shippingValues = null,
    )

    private var linkInteractor = LinkInteractor(
        context,
        setOf(PRODUCT_USAGE),
        { PUBLISHABLE_KEY },
        { STRIPE_ACCOUNT_ID },
        enableLogging = true,
        ioContext = Dispatchers.IO,
        uiContext = mock(),
        paymentAnalyticsRequestFactory = mock(),
        analyticsRequestExecutor = mock(),
        stripeRepository = mock(),
        addressRepository = mock(),
    )

    init {
        FakeAndroidKeyStore.setup()
    }

    @Test
    fun `verify component is reused for same configuration`() = runTest {
        linkInteractor.getAccountStatusFlow(config)
        val component = linkInteractor.component
        linkInteractor.signInWithUserInput(config, mock<UserInput.SignIn>())
        assertThat(linkInteractor.component).isEqualTo(component)
    }

    @Test
    fun `verify component is recreated for different configuration`() {
        val component = linkInteractor.component
        linkInteractor.getAccountStatusFlow(config.copy(merchantName = "anotherName"))
        assertThat(linkInteractor.component).isNotEqualTo(component)
    }

    companion object {
        const val PRODUCT_USAGE = "productUsage"
        const val PUBLISHABLE_KEY = "publishableKey"
        const val STRIPE_ACCOUNT_ID = "stripeAccountId"

        const val MERCHANT_NAME = "merchantName"
        const val CUSTOMER_EMAIL = "email"
        const val CUSTOMER_PHONE = "phone"
        const val CUSTOMER_NAME = "name"
        const val CUSTOMER_BILLING_COUNTRY_CODE = "country_code"
    }
}
