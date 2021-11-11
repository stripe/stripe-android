package com.stripe.android.payments.core.authentication

import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.PaymentAuthConfig
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.networking.ApiRequest
import com.stripe.android.payments.core.authentication.threeds2.Stripe3DS2Authenticator
import com.stripe.android.payments.core.authentication.threeds2.Stripe3ds2TransactionContract
import com.stripe.android.payments.core.injection.DUMMY_INJECTOR_KEY
import com.stripe.android.view.AuthActivityStarterHost
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.argWhere
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@ExperimentalCoroutinesApi
class Stripe3DS2AuthenticatorTest {
    private val testDispatcher = TestCoroutineDispatcher()
    private val activity: ComponentActivity = mock()
    private val host = AuthActivityStarterHost.create(activity)

    private val paymentAuthConfig = PaymentAuthConfig.Builder().set3ds2Config(
        PaymentAuthConfig.Stripe3ds2Config.Builder()
            .setTimeout(5)
            .build()
    ).build()

    private val authenticator = Stripe3DS2Authenticator(
        paymentAuthConfig,
        enableLogging = false,
        injectorKey = DUMMY_INJECTOR_KEY,
        publishableKeyProvider = { ApiKeyFixtures.FAKE_PUBLISHABLE_KEY },
        productUsage = setOf()
    )

    @Test
    fun `authenticate() should invoke startActivityForResult() when stripe3ds2CompletionLauncher is null`() =
        testDispatcher.runBlockingTest {
            val paymentIntent = PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2

            authenticator.authenticate(
                host,
                paymentIntent,
                REQUEST_OPTIONS
            )

            verify(activity).startActivityForResult(
                argWhere { intent ->
                    val args = requireNotNull(
                        Stripe3ds2TransactionContract.Args.fromIntent(intent)
                    )
                    args.stripeIntent == paymentIntent
                },
                eq(50000)
            )
        }

    @Test
    fun `authenticate() should invoke launch() when stripe3ds2CompletionLauncher is not null`() =
        testDispatcher.runBlockingTest {
            val mockLauncher = mock<ActivityResultLauncher<Stripe3ds2TransactionContract.Args>>()
            authenticator.stripe3ds2CompletionLauncher = mockLauncher

            val paymentIntent = PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2
            authenticator.authenticate(
                host,
                paymentIntent,
                REQUEST_OPTIONS
            )

            verify(mockLauncher).launch(
                argWhere { args ->
                    args.stripeIntent == paymentIntent
                },
            )
        }

    private companion object {
        private const val ACCOUNT_ID = "acct_123"
        private val REQUEST_OPTIONS = ApiRequest.Options(
            apiKey = ApiKeyFixtures.FAKE_PUBLISHABLE_KEY,
            stripeAccount = ACCOUNT_ID
        )
    }
}
