package com.stripe.android.payments

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.FakeFraudDetectionDataRepository
import com.stripe.android.auth.PaymentBrowserAuthContract
import com.stripe.android.core.Logger
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.SetupIntent
import com.stripe.android.model.SetupIntentFixtures
import com.stripe.android.model.StripeIntent
import com.stripe.android.model.copy
import com.stripe.android.networking.StripeApiRepository
import com.stripe.android.networking.StripeRepository
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.networktesting.RequestMatchers.bodyPart
import com.stripe.android.networktesting.RequestMatchers.method
import com.stripe.android.networktesting.RequestMatchers.path
import com.stripe.android.testing.AbsFakeStripeRepository
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class SourceCancellationHandlerNetworkTest {
    @get:Rule
    val networkRule = NetworkRule()

    private val testDispatcher = UnconfinedTestDispatcher()
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val apiRepository = StripeApiRepository(
        context = context,
        publishableKeyProvider = { ApiKeyFixtures.FAKE_PUBLISHABLE_KEY },
        requestSurface = StripeRepository.DEFAULT_REQUEST_SURFACE,
        workContext = testDispatcher,
        fraudDetectionDataRepository = FakeFraudDetectionDataRepository(),
    )

    @Test
    fun `native web view cancellation posts source cancel for a payatt PaymentIntent`() = runTest(testDispatcher) {
        val sourceId = "payatt_test"
        val originalPaymentIntent = PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2
        val originalThreeDS2Data =
            originalPaymentIntent.nextActionData as StripeIntent.NextActionData.SdkData.Use3DS2
        val paymentIntent = originalPaymentIntent.copy(
            nextActionData = originalThreeDS2Data.copy(source = sourceId),
        )
        val clientSecret = requireNotNull(paymentIntent.clientSecret)
        val threeDS2Data = paymentIntent.nextActionData as StripeIntent.NextActionData.SdkData.Use3DS2

        networkRule.enqueue(
            path("/v1/payment_intents/${threeDS2Data.threeDS2IntentId}/source_cancel"),
            method("POST"),
            bodyPart("source", threeDS2Data.source),
        ) { response ->
            response.setBody(paymentIntentResponse(clientSecret))
        }

        val sourceWasCanceled = SourceCancellationHandler(
            args = browserAuthArgs(paymentIntent, sourceId),
            stripeRepository = BrowserResultRepository(
                apiRepository = apiRepository,
                paymentIntent = paymentIntent,
                setupIntent = SetupIntentFixtures.SI_NEXT_ACTION_REDIRECT,
            ),
            logger = Logger.noop(),
        ).cancel()

        assertThat(sourceWasCanceled).isTrue()
    }

    @Test
    fun `native browser cancellation posts source cancel for a payatt SetupIntent`() = runTest(testDispatcher) {
        val sourceId = "payatt_test"
        val originalSetupIntent = SetupIntentFixtures.SI_3DS2_PROCESSING.copy(
            status = StripeIntent.Status.RequiresAction,
        )
        val originalThreeDS2Data =
            originalSetupIntent.nextActionData as StripeIntent.NextActionData.SdkData.Use3DS2
        val setupIntent = originalSetupIntent.copy(
            nextActionData = originalThreeDS2Data.copy(source = sourceId),
        )
        val clientSecret = requireNotNull(setupIntent.clientSecret)
        val threeDS2Data = setupIntent.nextActionData as StripeIntent.NextActionData.SdkData.Use3DS2

        networkRule.enqueue(
            path("/v1/setup_intents/${threeDS2Data.threeDS2IntentId ?: setupIntent.id}/source_cancel"),
            method("POST"),
            bodyPart("source", threeDS2Data.source),
        ) { response ->
            response.setBody(setupIntentResponse(clientSecret))
        }

        val sourceWasCanceled = SourceCancellationHandler(
            args = browserAuthArgs(setupIntent, sourceId),
            stripeRepository = BrowserResultRepository(
                apiRepository = apiRepository,
                paymentIntent = PaymentIntentFixtures.PI_REQUIRES_REDIRECT,
                setupIntent = setupIntent,
            ),
            logger = Logger.noop(),
        ).cancel()

        assertThat(sourceWasCanceled).isTrue()
    }

    private fun browserAuthArgs(
        stripeIntent: StripeIntent,
        sourceId: String,
    ): PaymentBrowserAuthContract.Args {
        return PaymentBrowserAuthContract.Args(
            objectId = requireNotNull(stripeIntent.id),
            requestCode = 0,
            clientSecret = requireNotNull(stripeIntent.clientSecret),
            url = "https://hooks.stripe.com/3d_secure_2/hosted/complete",
            statusBarColor = null,
            publishableKey = ApiKeyFixtures.FAKE_PUBLISHABLE_KEY,
            isInstantApp = false,
            shouldCancelSource = true,
            sourceId = sourceId,
        )
    }

    private fun paymentIntentResponse(clientSecret: String): String {
        return """
            {
              "id": "pi_test",
              "object": "payment_intent",
              "amount": 1000,
              "currency": "usd",
              "client_secret": "$clientSecret",
              "livemode": false,
              "payment_method_types": ["card"],
              "status": "requires_payment_method"
            }
        """.trimIndent()
    }

    private fun setupIntentResponse(clientSecret: String): String {
        return """
            {
              "id": "seti_test",
              "object": "setup_intent",
              "client_secret": "$clientSecret",
              "livemode": false,
              "payment_method_types": ["card"],
              "status": "requires_payment_method",
              "usage": "off_session"
            }
        """.trimIndent()
    }

    private class BrowserResultRepository(
        private val apiRepository: StripeApiRepository,
        private val paymentIntent: PaymentIntent,
        private val setupIntent: SetupIntent,
    ) : AbsFakeStripeRepository() {
        override suspend fun retrievePaymentIntent(
            clientSecret: String,
            options: com.stripe.android.core.networking.ApiRequest.Options,
            expandFields: List<String>,
        ): Result<PaymentIntent> = Result.success(paymentIntent)

        override suspend fun cancelPaymentIntentSource(
            paymentIntentId: String,
            sourceId: String,
            options: com.stripe.android.core.networking.ApiRequest.Options,
        ): Result<PaymentIntent> {
            return apiRepository.cancelPaymentIntentSource(paymentIntentId, sourceId, options)
        }

        override suspend fun retrieveSetupIntent(
            clientSecret: String,
            options: com.stripe.android.core.networking.ApiRequest.Options,
            expandFields: List<String>,
        ): Result<SetupIntent> = Result.success(setupIntent)

        override suspend fun cancelSetupIntentSource(
            setupIntentId: String,
            sourceId: String,
            options: com.stripe.android.core.networking.ApiRequest.Options,
        ): Result<SetupIntent> {
            return apiRepository.cancelSetupIntentSource(setupIntentId, sourceId, options)
        }
    }
}
