package com.stripe.android.paymentsheet

import androidx.activity.result.ActivityResultLauncher
import com.stripe.android.payments.bankaccount.CollectBankAccountConfiguration
import com.stripe.android.payments.bankaccount.navigation.CollectBankAccountContract
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CollectBankAccountForPaymentSheetLauncherTest {

    private val mockHostActivityLauncher =
        mock<ActivityResultLauncher<CollectBankAccountContract.Args>>()

    private val launcher = CollectBankAccountForPaymentSheetLauncher(mockHostActivityLauncher)

    @Test
    fun `presentWithPaymentIntent - launches CollectBankAccountActivity with correct arguments`() {
        launcher.presentWithPaymentIntent(
            PUBLISHABLE_KEY,
            CLIENT_SECRET,
            CONFIGURATION
        )

        verify(mockHostActivityLauncher).launch(
            CollectBankAccountContract.Args.ForPaymentIntent(
                publishableKey = PUBLISHABLE_KEY,
                clientSecret = CLIENT_SECRET,
                configuration = CONFIGURATION,
                attachToIntent = false
            )
        )
    }

    @Test
    fun `presentWithSetupIntent - launches CollectBankAccountActivity with correct arguments`() {
        launcher.presentWithSetupIntent(
            PUBLISHABLE_KEY,
            CLIENT_SECRET,
            CONFIGURATION
        )

        verify(mockHostActivityLauncher).launch(
            CollectBankAccountContract.Args.ForSetupIntent(
                publishableKey = PUBLISHABLE_KEY,
                clientSecret = CLIENT_SECRET,
                configuration = CONFIGURATION,
                attachToIntent = false
            )
        )
    }

    companion object {
        private const val CLIENT_SECRET = "client_secret"
        private const val PUBLISHABLE_KEY = "publishableKey"
        private val CONFIGURATION = CollectBankAccountConfiguration.USBankAccount(
            name = "Carlos",
            email = null
        )
    }
}
