package com.stripe.android.paymentsheet.paymentdatacollection.polling

import android.app.Application
import android.net.Uri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.testing.TestLifecycleOwner
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.StripeIntent
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.paymentsheet.R
import com.stripe.android.testing.DummyActivityResultCaller
import com.stripe.android.testing.FakeErrorReporter
import com.stripe.android.view.AuthActivityStarterHost
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class PollingNextActionHandlerTest {

    @Test
    fun `launches polling with correct args for UPI next action`() = runScenario(
        nextActionData = StripeIntent.NextActionData.UpiAwaitNotification,
    ) { args ->
        assertThat(args.timeLimitInSeconds).isEqualTo(5 * 60)
        assertThat(args.initialDelayInSeconds).isEqualTo(5)
        assertThat(args.ctaText).isEqualTo(R.string.stripe_upi_polling_message)
        assertThat(args.qrCodeUrl).isNull()
        assertThat(args.stripeAccountId).isEqualTo(ACCOUNT_ID)
    }

    @Test
    fun `launches polling with correct args for Blik next action`() = runScenario(
        nextActionData = StripeIntent.NextActionData.BlikAuthorize,
    ) { args ->
        assertThat(args.timeLimitInSeconds).isEqualTo(60)
        assertThat(args.initialDelayInSeconds).isEqualTo(5)
        assertThat(args.ctaText).isEqualTo(R.string.stripe_blik_confirm_payment)
        assertThat(args.qrCodeUrl).isNull()
    }

    @Test
    fun `launches polling with correct args for PayNow next action`() = runScenario(
        nextActionData = StripeIntent.NextActionData.DisplayPayNowDetails(
            qrCodeUrl = "https://example.com/qr"
        ),
    ) { args ->
        assertThat(args.timeLimitInSeconds).isEqualTo(60 * 60)
        assertThat(args.initialDelayInSeconds).isEqualTo(5)
        assertThat(args.ctaText).isEqualTo(R.string.stripe_qrcode_lpm_confirm_payment)
        assertThat(args.qrCodeUrl).isEqualTo("https://example.com/qr")
    }

    @Test
    fun `launches polling with correct args for PromptPay next action`() = runScenario(
        nextActionData = StripeIntent.NextActionData.DisplayPromptPayDetails(
            qrCodeUrl = "https://example.com/promptpay-qr"
        ),
    ) { args ->
        assertThat(args.timeLimitInSeconds).isEqualTo(60 * 60)
        assertThat(args.initialDelayInSeconds).isEqualTo(5)
        assertThat(args.ctaText).isEqualTo(R.string.stripe_qrcode_lpm_confirm_payment)
        assertThat(args.qrCodeUrl).isEqualTo("https://example.com/promptpay-qr")
    }

    @Test
    fun `launches polling correctly when paymentMethod is null`() = runScenario(
        nextActionData = StripeIntent.NextActionData.BlikAuthorize,
        paymentMethod = null,
    ) { args ->
        assertThat(args.timeLimitInSeconds).isEqualTo(60)
        assertThat(args.ctaText).isEqualTo(R.string.stripe_blik_confirm_payment)
    }

    @Test
    fun `reports error for unexpected next action type`() = runTest {
        val fakeErrorReporter = FakeErrorReporter()

        DummyActivityResultCaller.test {
            val handler = PollingNextActionHandler(
                errorReporterProvider = { fakeErrorReporter },
            )

            handler.onNewActivityResultCaller(
                activityResultCaller = activityResultCaller,
                activityResultCallback = {}
            )
            awaitRegisterCall()
            awaitNextRegisteredLauncher()

            val intent = PaymentIntentFixtures.PI_SUCCEEDED.copy(
                status = StripeIntent.Status.RequiresAction,
                nextActionData = StripeIntent.NextActionData.RedirectToUrl(
                    url = Uri.parse("https://example.com/redirect"),
                    returnUrl = "stripe://return",
                ),
            )
            handler.performNextAction(
                createHost(),
                intent,
                REQUEST_OPTIONS,
            )

            // Launcher should NOT have been called
        }

        val call = fakeErrorReporter.awaitCall()
        assertThat(call.errorEvent).isEqualTo(
            ErrorReporter.UnexpectedErrorEvent.POLLING_NEXT_ACTION_INVALID_NEXT_ACTION_TYPE
        )
        assertThat(call.additionalNonPiiParams["next_action_type"]).isEqualTo("RedirectToUrl")
        fakeErrorReporter.ensureAllEventsConsumed()
    }

    /**
     * Runs a test scenario that verifies a [PollingNextActionHandler] launches with the
     * expected [PollingContract.Args] for the given [nextActionData].
     */
    private fun runScenario(
        nextActionData: StripeIntent.NextActionData,
        paymentMethod: PaymentMethod? = DEFAULT_PAYMENT_METHOD,
        block: suspend (PollingContract.Args) -> Unit,
    ) = runTest {
        DummyActivityResultCaller.test {
            val handler = PollingNextActionHandler()

            handler.onNewActivityResultCaller(
                activityResultCaller = activityResultCaller,
                activityResultCallback = {}
            )
            awaitRegisterCall()
            awaitNextRegisteredLauncher()

            val intent = PaymentIntentFixtures.PI_SUCCEEDED.copy(
                status = StripeIntent.Status.RequiresAction,
                nextActionData = nextActionData,
                paymentMethod = paymentMethod,
            )

            handler.performNextAction(
                createHost(),
                intent,
                REQUEST_OPTIONS,
            )

            val launchedArgs = awaitLaunchCall() as PollingContract.Args
            block(launchedArgs)
        }
    }

    private fun createHost(): AuthActivityStarterHost {
        return object : AuthActivityStarterHost {
            override val statusBarColor: Int? = null
            override val lifecycleOwner: LifecycleOwner =
                TestLifecycleOwner(initialState = Lifecycle.State.RESUMED)
            override val application: Application =
                ApplicationProvider.getApplicationContext()

            override fun startActivityForResult(
                target: Class<*>,
                extras: android.os.Bundle,
                requestCode: Int
            ) {
                error("Not expected")
            }
        }
    }

    private companion object {
        private const val ACCOUNT_ID = "acct_123"

        private val DEFAULT_PAYMENT_METHOD = PaymentMethod(
            id = "pm_123",
            created = null,
            liveMode = false,
            type = PaymentMethod.Type.Blik,
            code = PaymentMethod.Type.Blik.code,
        )

        private val REQUEST_OPTIONS = ApiRequest.Options(
            apiKey = ApiKeyFixtures.FAKE_PUBLISHABLE_KEY,
            stripeAccount = ACCOUNT_ID,
        )
    }
}
