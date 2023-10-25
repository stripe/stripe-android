package com.stripe.android.payments.core.authentication

import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import com.google.common.truth.Truth.assertThat
import com.stripe.android.PaymentRelayContract
import com.stripe.android.PaymentRelayStarter
import com.stripe.android.auth.PaymentBrowserAuthContract
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.Source
import com.stripe.android.model.StripeIntent
import com.stripe.android.model.StripeIntent.NextActionData
import com.stripe.android.payments.PaymentFlowResult
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.eq
import org.mockito.kotlin.isA
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

@RunWith(RobolectricTestRunner::class)
class DefaultPaymentAuthenticatorRegistryTest {
    private val noOpIntentAuthenticator = mock<NoOpIntentAuthenticator>()
    private val sourceAuthenticator = mock<SourceAuthenticator>()
    private val threeDs1lAuthenticator = mock<PaymentAuthenticator<StripeIntent>>()
    private val threeDs2lAuthenticator = mock<PaymentAuthenticator<StripeIntent>>()
    private val redirectToUrlAuthenticator = mock<PaymentAuthenticator<StripeIntent>>()
    private val alipayRedirectAuthenticator = mock<PaymentAuthenticator<StripeIntent>>()
    private val dispayOxxoDetailsAuthenticator = mock<PaymentAuthenticator<StripeIntent>>()

    private val registry = DefaultPaymentAuthenticatorRegistry(
        noOpIntentAuthenticator = noOpIntentAuthenticator,
        sourceAuthenticator = sourceAuthenticator,
        paymentAuthenticators = mapOf(
            NextActionData.SdkData.Use3DS1::class.java to threeDs1lAuthenticator,
            NextActionData.SdkData.Use3DS2::class.java to threeDs2lAuthenticator,
            NextActionData.RedirectToUrl::class.java to redirectToUrlAuthenticator,
            NextActionData.AlipayRedirect::class.java to alipayRedirectAuthenticator,
            NextActionData.DisplayOxxoDetails::class.java to dispayOxxoDetailsAuthenticator
        ),
        includePaymentSheetAuthenticators = false,
    )

    private val allAuthenticators = setOf(
        noOpIntentAuthenticator,
        sourceAuthenticator,
        threeDs1lAuthenticator,
        threeDs2lAuthenticator,
        redirectToUrlAuthenticator,
        alipayRedirectAuthenticator,
        dispayOxxoDetailsAuthenticator
    )

    @Test
    fun `verify Source gets a SourceAuthenticator`() {
        assertThat(registry.getAuthenticator(mock<Source>())).isEqualTo(sourceAuthenticator)
    }

    @Test
    fun `verify StripeIntent with nextAction gets the correct PaymentAuthenticator`() {
        verifyIntentWithType(
            mock<NextActionData.SdkData.Use3DS1>(),
            threeDs1lAuthenticator
        )

        verifyIntentWithType(
            mock<NextActionData.SdkData.Use3DS2>(),
            threeDs2lAuthenticator
        )

        verifyIntentWithType(
            mock<NextActionData.RedirectToUrl>(),
            redirectToUrlAuthenticator
        )

        verifyIntentWithType(
            mock<NextActionData.AlipayRedirect>(),
            alipayRedirectAuthenticator
        )

        verifyIntentWithType(
            mock<NextActionData.DisplayOxxoDetails>(),
            dispayOxxoDetailsAuthenticator
        )

        verifyIntentWithType(
            mock<NextActionData.BlikAuthorize>(),
            noOpIntentAuthenticator
        )
    }

    private fun verifyIntentWithType(
        nextActionData: NextActionData,
        paymentAuthenticator: PaymentAuthenticator<StripeIntent>
    ) {
        val stripeIntent = mock<StripeIntent>()
        whenever(stripeIntent.requiresAction()).thenReturn(true)
        whenever(stripeIntent.nextActionData).thenReturn(nextActionData)
        assertThat(
            registry.getAuthenticator(stripeIntent)
        ).isEqualTo(
            paymentAuthenticator
        )
    }

    @Test
    fun `verify StripeIntent without nextAction gets NoOpIntentAuthenticator`() {
        val stripeIntent = mock<StripeIntent>()
        whenever(stripeIntent.requiresAction()).thenReturn(false)
        assertThat(
            registry.getAuthenticator(stripeIntent)
        ).isEqualTo(
            noOpIntentAuthenticator
        )
    }

    @Test
    fun `verify incompatible type throws exception`() {
        // any type that is not StripeIntent or Source
        val incompatibleType = mock<ConfirmPaymentIntentParams>()
        assertFailsWith<IllegalStateException>(
            "No suitable PaymentAuthenticator for $incompatibleType}"
        ) {
            registry.getAuthenticator(incompatibleType)
        }
    }

    @Test
    fun `verify Launchers updated with new ActivityResultCaller and invalided correctly`() {
        assertNull(registry.paymentRelayLauncher)
        assertNull(registry.paymentBrowserAuthLauncher)

        val mockActivityResultCaller = mock<ActivityResultCaller>()
        val mockActivityResultCallback =
            mock<ActivityResultCallback<PaymentFlowResult.Unvalidated>>()
        val mockPaymentRelayLauncher = mock<ActivityResultLauncher<PaymentRelayStarter.Args>>()
        val mockPaymentBrowserAuthLauncher =
            mock<ActivityResultLauncher<PaymentBrowserAuthContract.Args>>()

        whenever(
            mockActivityResultCaller.registerForActivityResult(
                isA<PaymentRelayContract>(),
                eq(mockActivityResultCallback)
            )
        ).thenReturn(mockPaymentRelayLauncher)

        whenever(
            mockActivityResultCaller.registerForActivityResult(
                isA<PaymentBrowserAuthContract>(),
                eq(mockActivityResultCallback)
            )
        ).thenReturn(mockPaymentBrowserAuthLauncher)

        registry.onNewActivityResultCaller(mockActivityResultCaller, mockActivityResultCallback)

        allAuthenticators.forEach {
            verify(it).onNewActivityResultCaller(
                mockActivityResultCaller,
                mockActivityResultCallback
            )
        }
        assertThat(registry.paymentRelayLauncher).isEqualTo(mockPaymentRelayLauncher)
        assertThat(registry.paymentBrowserAuthLauncher).isEqualTo(mockPaymentBrowserAuthLauncher)

        registry.onLauncherInvalidated()
        allAuthenticators.forEach {
            verify(it).onLauncherInvalidated()
        }

        verify(mockPaymentRelayLauncher).unregister()
        verify(mockPaymentBrowserAuthLauncher).unregister()
        assertNull(registry.paymentRelayLauncher)
        assertNull(registry.paymentBrowserAuthLauncher)
    }
}
