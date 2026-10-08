package com.stripe.android.payments.bankaccount

import androidx.activity.result.ActivityResultLauncher
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.financialconnections.FinancialConnectionsPreCollectedConsent
import com.stripe.android.payments.bankaccount.navigation.CollectBankAccountContract
import com.stripe.android.payments.financialconnections.FinancialConnectionsAvailability.Full
import org.junit.Test
import org.junit.runner.RunWith
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
            publishableKey = ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY,
            stripeAccountId = ApiKeyFixtures.FAKE_STRIPE_ACCOUNT,
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
            publishableKey = ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY,
            stripeAccountId = ApiKeyFixtures.FAKE_STRIPE_ACCOUNT,
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

    private fun makeLauncher(): CollectBankAccountForInstantDebitsLauncher {
        return CollectBankAccountForInstantDebitsLauncher(
            hostActivityLauncher = mockHostActivityLauncher,
            financialConnectionsAvailability = Full,
            hostedSurface = HOSTED_SURFACE,
        )
    }

    companion object {
        private const val CLIENT_SECRET = "client_secret"
        private const val HOSTED_SURFACE = "payment_element"
        private val CONFIGURATION = CollectBankAccountConfiguration.USBankAccount(
            name = "Carlos",
            email = null
        )
    }
}
