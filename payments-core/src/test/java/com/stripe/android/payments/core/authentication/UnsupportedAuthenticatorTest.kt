package com.stripe.android.payments.core.authentication

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.testing.TestLifecycleOwner
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.FakeActivityResultLauncher
import com.stripe.android.PaymentRelayContract
import com.stripe.android.PaymentRelayStarter
import com.stripe.android.StripePaymentController
import com.stripe.android.core.exception.StripeException
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.core.version.StripeSdkVersion
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentIntentFixtures.PI_SUCCEEDED
import com.stripe.android.model.StripeIntent
import com.stripe.android.view.AuthActivityStarterHost
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class UnsupportedAuthenticatorTest {
    private val paymentRelayStarterFactory =
        mock<(AuthActivityStarterHost) -> PaymentRelayStarter>()

    private val authenticator = UnsupportedAuthenticator(
        paymentRelayStarterFactory
    )

    private val host = mock<AuthActivityStarterHost> {
        on { lifecycleOwner } doReturn TestLifecycleOwner(initialState = Lifecycle.State.RESUMED)
    }

    private val launcher = FakeActivityResultLauncher(PaymentRelayContract())

    @Before
    fun setUpStarterFactory() {
        whenever(paymentRelayStarterFactory(any())).thenReturn(
            PaymentRelayStarter.Modern(
                launcher
            )
        )
    }

    @Test
    fun verifyWeChat() = runTest {
        authenticator.authenticate(
            host,
            PaymentIntentFixtures.PI_REQUIRES_WECHAT_PAY_AUTHORIZE,
            REQUEST_OPTIONS
        )

        assertThat(launcher.launchArgs)
            .containsExactly(
                PaymentRelayStarter.Args.ErrorArgs(
                    exception = StripeException.create(
                        IllegalArgumentException(
                            "${StripeIntent.NextActionData.WeChatPayRedirect::class.java.simpleName} " +
                                "type is not supported, add " +
                                "com.stripe:stripe-wechatpay:${StripeSdkVersion.VERSION_NAME} in build.gradle " +
                                "to support it"
                        )
                    ),
                    requestCode = StripePaymentController.PAYMENT_REQUEST_CODE
                )
            )
    }

    @Test
    fun verifyNullNextActionType() = runTest {
        authenticator.authenticate(
            host,
            PI_SUCCEEDED,
            REQUEST_OPTIONS
        )

        assertThat(launcher.launchArgs)
            .containsExactly(
                PaymentRelayStarter.Args.ErrorArgs(
                    exception = StripeException.create(
                        IllegalArgumentException("stripeIntent.nextActionData is null")
                    ),
                    requestCode = StripePaymentController.PAYMENT_REQUEST_CODE
                )
            )
    }

    private companion object {
        private val REQUEST_OPTIONS = ApiRequest.Options(
            apiKey = ApiKeyFixtures.FAKE_PUBLISHABLE_KEY
        )
    }
}
