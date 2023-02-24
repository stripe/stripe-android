package com.stripe.android.payments.core.authentication

import android.os.Bundle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.testing.TestLifecycleOwner
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.FakeActivityResultLauncher
import com.stripe.android.PaymentRelayContract
import com.stripe.android.PaymentRelayStarter
import com.stripe.android.StripePaymentController.Companion.PAYMENT_REQUEST_CODE
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.payments.PaymentFlowResult
import com.stripe.android.view.AuthActivityStarterHost
import com.stripe.android.view.PaymentRelayActivity
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.KArgumentCaptor
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NoOpIntentAuthenticatorTest {
    private val host = mock<AuthActivityStarterHost> {
        on { lifecycleOwner } doReturn TestLifecycleOwner(initialState = Lifecycle.State.RESUMED)
    }

    private val paymentRelayStarterFactory =
        mock<(AuthActivityStarterHost) -> PaymentRelayStarter>()

    private val authenticator = NoOpIntentAuthenticator(
        paymentRelayStarterFactory
    )

    @Test
    fun verifyModernPaymentRelayStarter() = runTest {
        val launcher = FakeActivityResultLauncher(PaymentRelayContract())

        whenever(paymentRelayStarterFactory(any())).thenReturn(
            PaymentRelayStarter.Modern(
                launcher
            )
        )

        authenticator.authenticate(
            host,
            PaymentIntentFixtures.PI_WITH_LAST_PAYMENT_ERROR,
            REQUEST_OPTIONS
        )

        assertThat(launcher.launchArgs)
            .containsExactly(
                PaymentRelayStarter.Args.PaymentIntentArgs(
                    PaymentIntentFixtures.PI_WITH_LAST_PAYMENT_ERROR
                )
            )
    }

    @Test
    fun verifyLegacyPaymentRelayStarter() = runTest {
        val classArgumentCaptor: KArgumentCaptor<Class<*>> = argumentCaptor()
        val bundleArgumentCaptor: KArgumentCaptor<Bundle> = argumentCaptor()
        val requestCodeArgumentCaptor: KArgumentCaptor<Int> = argumentCaptor()
        whenever(paymentRelayStarterFactory(any())).thenReturn(
            PaymentRelayStarter.Legacy(
                host
            )
        )
        authenticator.authenticate(
            host,
            PaymentIntentFixtures.PI_WITH_LAST_PAYMENT_ERROR,
            REQUEST_OPTIONS
        )

        verify(host).startActivityForResult(
            classArgumentCaptor.capture(),
            bundleArgumentCaptor.capture(),
            requestCodeArgumentCaptor.capture()
        )

        assertThat(classArgumentCaptor.firstValue).isEqualTo(PaymentRelayActivity::class.java)

        assertThat(bundleArgumentCaptor.firstValue.get("extra_args")).isInstanceOf(
            PaymentFlowResult.Unvalidated::class.java
        )
        val result =
            bundleArgumentCaptor.firstValue.get("extra_args") as PaymentFlowResult.Unvalidated
        assertThat(result.stripeAccountId).isNull()
        assertThat(result.clientSecret).isEqualTo("pi_1F7J1aCRMbs6FrXfaJcvbxF6_secret_mIuDLsSfoo1m6s")

        assertThat(requestCodeArgumentCaptor.firstValue).isEqualTo(PAYMENT_REQUEST_CODE)
    }

    private companion object {
        private val REQUEST_OPTIONS = ApiRequest.Options(
            apiKey = ApiKeyFixtures.FAKE_PUBLISHABLE_KEY
        )
    }
}
