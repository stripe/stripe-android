package com.stripe.android.payments.bankaccount

import androidx.activity.result.ActivityResultLauncher
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.financialconnections.FinancialConnectionsPreCollectedConsent
import com.stripe.android.payments.bankaccount.navigation.CollectBankAccountContract
import com.stripe.android.payments.financialconnections.FinancialConnectionsAvailability.Full
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CollectBankAccountForInstantDebitsLauncherTest {

    private val mockHostActivityLauncher =
        mock<ActivityResultLauncher<CollectBankAccountContract.Args>>()

    @Test
    fun `presentWithPaymentIntent - ignores preCollectedConsent for hosted surface`() {
        val launcher = makeLauncher()
        val preCollectedConsent = FinancialConnectionsPreCollectedConsent(
            consent = "fccons_123",
            collectedAt = 1_725_000_000L,
        )

        launcher.presentWithPaymentIntent(
            publishableKey = PUBLISHABLE_KEY,
            stripeAccountId = STRIPE_ACCOUNT_ID,
            clientSecret = CLIENT_SECRET,
            configuration = CONFIGURATION,
            preCollectedConsent = preCollectedConsent,
        )

        verify(mockHostActivityLauncher).launch(
            CollectBankAccountContract.Args.ForPaymentIntent(
                apiConfiguration = ApiKeyFixtures.DEFAULT_API_CONFIG,
                clientSecret = CLIENT_SECRET,
                configuration = CONFIGURATION,
                attachToIntent = true,
                hostedSurface = HOSTED_SURFACE,
                financialConnectionsAvailability = Full,
                preCollectedConsent = null
            )
        )
    }

    @Test
    fun `presentWithSetupIntent - ignores preCollectedConsent for hosted surface`() {
        val launcher = makeLauncher()
        val preCollectedConsent = FinancialConnectionsPreCollectedConsent(
            consent = "fccons_123",
            collectedAt = 1_725_000_000L,
        )

        launcher.presentWithSetupIntent(
            publishableKey = PUBLISHABLE_KEY,
            stripeAccountId = STRIPE_ACCOUNT_ID,
            clientSecret = CLIENT_SECRET,
            configuration = CONFIGURATION,
            preCollectedConsent = preCollectedConsent,
        )

        verify(mockHostActivityLauncher).launch(
            CollectBankAccountContract.Args.ForSetupIntent(
                apiConfiguration = ApiKeyFixtures.DEFAULT_API_CONFIG,
                clientSecret = CLIENT_SECRET,
                configuration = CONFIGURATION,
                attachToIntent = true,
                hostedSurface = HOSTED_SURFACE,
                financialConnectionsAvailability = Full,
                preCollectedConsent = null
            )
        )
    }

    @Test
    fun `presentWithPaymentIntent - forwards existing API configuration`() {
        val launcher = makeLauncher()

        launcher.presentWithPaymentIntent(
            apiConfiguration = ApiKeyFixtures.DEFAULT_API_CONFIG,
            clientSecret = CLIENT_SECRET,
            configuration = CONFIGURATION,
            preCollectedConsent = null,
        )

        val argsCaptor = argumentCaptor<CollectBankAccountContract.Args.ForPaymentIntent>()
        verify(mockHostActivityLauncher).launch(argsCaptor.capture())
        assertThat(argsCaptor.firstValue.apiConfiguration).isSameInstanceAs(ApiKeyFixtures.DEFAULT_API_CONFIG)
    }

    @Test
    fun `presentWithSetupIntent - forwards existing API configuration`() {
        val launcher = makeLauncher()

        launcher.presentWithSetupIntent(
            apiConfiguration = ApiKeyFixtures.DEFAULT_API_CONFIG,
            clientSecret = CLIENT_SECRET,
            configuration = CONFIGURATION,
            preCollectedConsent = null,
        )

        val argsCaptor = argumentCaptor<CollectBankAccountContract.Args.ForSetupIntent>()
        verify(mockHostActivityLauncher).launch(argsCaptor.capture())
        assertThat(argsCaptor.firstValue.apiConfiguration).isSameInstanceAs(ApiKeyFixtures.DEFAULT_API_CONFIG)
    }

    @Test
    fun `presentWithDeferredPayment - forwards existing API configuration`() {
        val launcher = makeLauncher()

        launcher.presentWithDeferredPayment(
            apiConfiguration = ApiKeyFixtures.DEFAULT_API_CONFIG,
            configuration = CONFIGURATION,
            elementsSessionId = "elements_session_id",
            customerId = "customer_id",
            onBehalfOf = "on_behalf_of_id",
            amount = 1000,
            currency = "usd",
        )

        val argsCaptor = argumentCaptor<CollectBankAccountContract.Args.ForDeferredPaymentIntent>()
        verify(mockHostActivityLauncher).launch(argsCaptor.capture())
        assertThat(argsCaptor.firstValue.apiConfiguration).isSameInstanceAs(ApiKeyFixtures.DEFAULT_API_CONFIG)
    }

    @Test
    fun `presentWithDeferredSetup - forwards existing API configuration`() {
        val launcher = makeLauncher()

        launcher.presentWithDeferredSetup(
            apiConfiguration = ApiKeyFixtures.DEFAULT_API_CONFIG,
            configuration = CONFIGURATION,
            elementsSessionId = "elements_session_id",
            customerId = "customer_id",
            onBehalfOf = "on_behalf_of_id",
        )

        val argsCaptor = argumentCaptor<CollectBankAccountContract.Args.ForDeferredSetupIntent>()
        verify(mockHostActivityLauncher).launch(argsCaptor.capture())
        assertThat(argsCaptor.firstValue.apiConfiguration).isSameInstanceAs(ApiKeyFixtures.DEFAULT_API_CONFIG)
    }

    private fun makeLauncher(): CollectBankAccountForInstantDebitsLauncher {
        return CollectBankAccountForInstantDebitsLauncher(
            hostActivityLauncher = mockHostActivityLauncher,
            financialConnectionsAvailability = Full,
            hostedSurface = HOSTED_SURFACE,
        )
    }

    companion object {
        private const val CLIENT_SECRET = "client_secret"
        private const val PUBLISHABLE_KEY = ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY
        private const val STRIPE_ACCOUNT_ID = ApiKeyFixtures.FAKE_STRIPE_ACCOUNT
        private const val HOSTED_SURFACE = "payment_element"
        private val CONFIGURATION = CollectBankAccountConfiguration.USBankAccount(
            name = "Carlos",
            email = null
        )
    }
}
