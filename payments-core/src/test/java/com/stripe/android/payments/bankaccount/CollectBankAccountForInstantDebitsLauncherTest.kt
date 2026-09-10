package com.stripe.android.payments.bankaccount

import androidx.activity.result.ActivityResultLauncher
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
    fun `presentWithPaymentIntent - launches CollectBankAccountActivity with correct arguments`() {
        val launcher = makeLauncher()

        launcher.presentWithPaymentIntent(
            publishableKey = PUBLISHABLE_KEY,
            stripeAccountId = STRIPE_ACCOUNT_ID,
            clientSecret = CLIENT_SECRET,
            configuration = CONFIGURATION,
        )

        verify(mockHostActivityLauncher).launch(
            CollectBankAccountContract.Args.ForPaymentIntent(
                publishableKey = PUBLISHABLE_KEY,
                stripeAccountId = STRIPE_ACCOUNT_ID,
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
    fun `presentWithPaymentIntent - passes preCollectedConsent through to Args when provided`() {
        val launcher = makeLauncher()
        val preCollectedConsent = FinancialConnectionsPreCollectedConsent(consent = "fccons_123")

        launcher.presentWithPaymentIntent(
            publishableKey = PUBLISHABLE_KEY,
            stripeAccountId = STRIPE_ACCOUNT_ID,
            clientSecret = CLIENT_SECRET,
            configuration = CONFIGURATION,
            preCollectedConsent = preCollectedConsent,
        )

        verify(mockHostActivityLauncher).launch(
            CollectBankAccountContract.Args.ForPaymentIntent(
                publishableKey = PUBLISHABLE_KEY,
                stripeAccountId = STRIPE_ACCOUNT_ID,
                clientSecret = CLIENT_SECRET,
                configuration = CONFIGURATION,
                attachToIntent = true,
                hostedSurface = HOSTED_SURFACE,
                financialConnectionsAvailability = Full,
                preCollectedConsent = preCollectedConsent
            )
        )
    }

    @Test
    fun `presentWithSetupIntent - launches CollectBankAccountActivity with correct arguments`() {
        val launcher = makeLauncher()

        launcher.presentWithSetupIntent(
            publishableKey = PUBLISHABLE_KEY,
            stripeAccountId = STRIPE_ACCOUNT_ID,
            clientSecret = CLIENT_SECRET,
            configuration = CONFIGURATION,
        )

        verify(mockHostActivityLauncher).launch(
            CollectBankAccountContract.Args.ForSetupIntent(
                publishableKey = PUBLISHABLE_KEY,
                stripeAccountId = STRIPE_ACCOUNT_ID,
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
    fun `presentWithSetupIntent - passes preCollectedConsent through to Args when provided`() {
        val launcher = makeLauncher()
        val preCollectedConsent = FinancialConnectionsPreCollectedConsent(consent = "fccons_123")

        launcher.presentWithSetupIntent(
            publishableKey = PUBLISHABLE_KEY,
            stripeAccountId = STRIPE_ACCOUNT_ID,
            clientSecret = CLIENT_SECRET,
            configuration = CONFIGURATION,
            preCollectedConsent = preCollectedConsent,
        )

        verify(mockHostActivityLauncher).launch(
            CollectBankAccountContract.Args.ForSetupIntent(
                publishableKey = PUBLISHABLE_KEY,
                stripeAccountId = STRIPE_ACCOUNT_ID,
                clientSecret = CLIENT_SECRET,
                configuration = CONFIGURATION,
                attachToIntent = true,
                hostedSurface = HOSTED_SURFACE,
                financialConnectionsAvailability = Full,
                preCollectedConsent = preCollectedConsent
            )
        )
    }

    @Test
    fun `presentWithDeferredPayment - launches CollectBankAccountActivity with correct arguments`() {
        val launcher = makeLauncher()

        launcher.presentWithDeferredPayment(
            publishableKey = PUBLISHABLE_KEY,
            stripeAccountId = STRIPE_ACCOUNT_ID,
            configuration = CONFIGURATION,
            elementsSessionId = "elements_session_id",
            customerId = "customer_id",
            onBehalfOf = "on_behalf_of_id",
            amount = 1000,
            currency = "usd"
        )

        verify(mockHostActivityLauncher).launch(
            CollectBankAccountContract.Args.ForDeferredPaymentIntent(
                publishableKey = PUBLISHABLE_KEY,
                stripeAccountId = STRIPE_ACCOUNT_ID,
                configuration = CONFIGURATION,
                elementsSessionId = "elements_session_id",
                customerId = "customer_id",
                onBehalfOf = "on_behalf_of_id",
                amount = 1000,
                currency = "usd",
                hostedSurface = HOSTED_SURFACE,
                financialConnectionsAvailability = Full
            )
        )
    }

    @Test
    fun `presentWithDeferredSetup - launches CollectBankAccountActivity with correct arguments`() {
        val launcher = makeLauncher()

        launcher.presentWithDeferredSetup(
            publishableKey = PUBLISHABLE_KEY,
            stripeAccountId = STRIPE_ACCOUNT_ID,
            configuration = CONFIGURATION,
            elementsSessionId = "elements_session_id",
            customerId = "customer_id",
            onBehalfOf = "on_behalf_of_id",
        )

        verify(mockHostActivityLauncher).launch(
            CollectBankAccountContract.Args.ForDeferredSetupIntent(
                publishableKey = PUBLISHABLE_KEY,
                stripeAccountId = STRIPE_ACCOUNT_ID,
                configuration = CONFIGURATION,
                elementsSessionId = "elements_session_id",
                customerId = "customer_id",
                onBehalfOf = "on_behalf_of_id",
                hostedSurface = HOSTED_SURFACE,
                financialConnectionsAvailability = Full
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
        private const val PUBLISHABLE_KEY = "publishableKey"
        private const val STRIPE_ACCOUNT_ID = "stripe_account_id"
        private const val HOSTED_SURFACE = "payment_element"
        private val CONFIGURATION = CollectBankAccountConfiguration.USBankAccount(
            name = "Carlos",
            email = null
        )
    }
}
