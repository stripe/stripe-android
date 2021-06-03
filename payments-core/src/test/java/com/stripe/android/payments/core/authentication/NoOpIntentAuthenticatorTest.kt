package com.stripe.android.payments.core.authentication

import android.os.Bundle
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.KArgumentCaptor
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.FakeActivityResultLauncher
import com.stripe.android.PaymentRelayContract
import com.stripe.android.PaymentRelayStarter
import com.stripe.android.StripePaymentController.Companion.PAYMENT_REQUEST_CODE
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.networking.ApiRequest
import com.stripe.android.payments.PaymentFlowResult
import com.stripe.android.view.AuthActivityStarter
import com.stripe.android.view.PaymentRelayActivity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@ExperimentalCoroutinesApi
class NoOpIntentAuthenticatorTest {
    private val testDispatcher = TestCoroutineDispatcher()

    private val host = mock<AuthActivityStarter.Host>()

    private val paymentRelayStarterFactory =
        mock<(AuthActivityStarter.Host) -> PaymentRelayStarter>()

    private val authenticator = NoOpIntentAuthenticator(
        paymentRelayStarterFactory
    )

    @Test
    fun verifyModernPaymentRelayStarter() =
        testDispatcher.runBlockingTest {

            val launcher = FakeActivityResultLauncher(PaymentRelayContract())

            whenever(paymentRelayStarterFactory(any())).thenReturn(
                PaymentRelayStarter.Modern(
                    launcher
                )
            )
            authenticator.authenticate(
                host,
                PaymentIntentFixtures.PI_WITH_LAST_PAYMENT_ERROR,
                null,
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
    fun verifyLegacyPaymentRelayStarter() =
        testDispatcher.runBlockingTest {
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
                null,
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
            apiKey = ApiKeyFixtures.FAKE_PUBLISHABLE_KEY,
        )
    }
}
