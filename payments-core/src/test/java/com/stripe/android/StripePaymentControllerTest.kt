package com.stripe.android

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.StripePaymentController.Companion.EXPAND_PAYMENT_METHOD
import com.stripe.android.exception.InvalidRequestException
import com.stripe.android.model.AlipayAuthResult
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.SetupIntentFixtures
import com.stripe.android.model.Source
import com.stripe.android.model.SourceFixtures
import com.stripe.android.model.Stripe3ds2Fixtures
import com.stripe.android.model.StripeIntent
import com.stripe.android.networking.AbsFakeStripeRepository
import com.stripe.android.networking.AlipayRepository
import com.stripe.android.networking.AnalyticsEvent
import com.stripe.android.networking.AnalyticsRequest
import com.stripe.android.networking.AnalyticsRequestExecutor
import com.stripe.android.networking.AnalyticsRequestFactory
import com.stripe.android.networking.ApiRequest
import com.stripe.android.payments.PaymentFlowResult
import com.stripe.android.stripe3ds2.transaction.SdkTransactionId
import com.stripe.android.stripe3ds2.transaction.Transaction
import com.stripe.android.utils.ParcelUtils
import com.stripe.android.view.AuthActivityStarterHost
import com.stripe.android.view.PaymentRelayActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.test.setMain
import org.junit.runner.RunWith
import org.mockito.kotlin.KArgumentCaptor
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import java.util.UUID
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
@ExperimentalCoroutinesApi
internal class StripePaymentControllerTest {
    private val activity: ComponentActivity = mock()
    private val sdkTransactionId = mock<SdkTransactionId>().also {
        whenever(it.value).thenReturn(UUID.randomUUID().toString())
    }
    private val transaction: Transaction = mock<Transaction>().also {
        whenever(it.sdkTransactionId)
            .thenReturn(sdkTransactionId)
    }
    private val analyticsRequestExecutor: AnalyticsRequestExecutor = mock()
    private val alipayRepository = mock<AlipayRepository>()
    private val stripeRepository = FakeStripeRepository()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val analyticsRequestFactory = AnalyticsRequestFactory(
        context,
        ApiKeyFixtures.FAKE_PUBLISHABLE_KEY
    )
    private val host = AuthActivityStarterHost.create(activity)

    private val intentArgumentCaptor: KArgumentCaptor<Intent> = argumentCaptor()
    private val analyticsRequestArgumentCaptor: KArgumentCaptor<AnalyticsRequest> = argumentCaptor()

    private val testDispatcher = TestCoroutineDispatcher()

    private val controller = createController()

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        runBlocking {
            whenever(transaction.createAuthenticationRequestParameters())
                .thenReturn(Stripe3ds2Fixtures.createAreqParams(sdkTransactionId))
        }
        whenever(activity.applicationContext)
            .thenReturn(context)
    }

    @AfterTest
    fun cleanup() {
        Dispatchers.resetMain()
        testDispatcher.cleanupTestCoroutines()
        stripeRepository.retrieveStripeIntentResponse = PaymentIntentFixtures.PI_REQUIRES_REDIRECT
    }

    @Test
    fun shouldHandleResult_withInvalidResultCode() {
        assertThat(controller.shouldHandlePaymentResult(500, Intent())).isFalse()
        assertThat(controller.shouldHandleSetupResult(500, Intent())).isFalse()
    }

    @Test
    fun getRequestCode_withIntents_correctCodeReturned() {
        assertThat(StripePaymentController.getRequestCode(PaymentIntentFixtures.PI_REQUIRES_3DS1))
            .isEqualTo(StripePaymentController.PAYMENT_REQUEST_CODE)
        assertThat(StripePaymentController.getRequestCode(SetupIntentFixtures.SI_NEXT_ACTION_REDIRECT))
            .isEqualTo(StripePaymentController.SETUP_REQUEST_CODE)
    }

    @Test
    fun getRequestCode_withParams_correctCodeReturned() {
        assertThat(
            StripePaymentController.getRequestCode(
                ConfirmPaymentIntentParams.createWithPaymentMethodId(
                    "pm_123",
                    "client_secret"
                )
            )
        ).isEqualTo(StripePaymentController.PAYMENT_REQUEST_CODE)
    }

    @Test
    fun handlePaymentResult_whenSourceShouldBeCanceled_onlyCallsCancelIntentOnce() =
        testDispatcher.runBlockingTest {
            // use a PaymentIntent in `requires_action` state
            val paymentIntent = PaymentIntentFixtures.PI_REQUIRES_3DS1

            val clientSecret = paymentIntent.clientSecret.orEmpty()
            stripeRepository.retrievePaymentIntentResponse = paymentIntent
            stripeRepository.cancelPaymentIntentResponse = paymentIntent

            val sourceId = "src_1Ff87qCRMbs6FrXfPABTYaEd"

            val intent = Intent().putExtras(
                PaymentFlowResult.Unvalidated(
                    clientSecret = clientSecret,
                    sourceId = sourceId,
                    canCancelSource = true,
                    stripeAccountId = ACCOUNT_ID
                ).toBundle()
            )

            val paymentIntentResult = controller.getPaymentIntentResult(intent)

            assertThat(stripeRepository.retrievePaymentIntentArgs)
                .containsExactly(
                    Triple(clientSecret, REQUEST_OPTIONS, listOf("payment_method"))
                )

            // verify that cancelIntent is only called once
            assertThat(stripeRepository.cancelPaymentIntentArgs)
                .containsExactly(
                    Triple(paymentIntent.id.orEmpty(), sourceId, REQUEST_OPTIONS)
                )

            assertThat(paymentIntentResult).isEqualTo(PaymentIntentResult(paymentIntent))
        }

    @Test
    fun shouldHandleSourceResult_withSourceRequestCode_returnsTrue() {
        assertThat(
            controller.shouldHandleSourceResult(
                StripePaymentController.SOURCE_REQUEST_CODE,
                Intent()
            )
        ).isTrue()
    }

    @Test
    fun startAuthenticateSource_withNoneFlowSource_shouldBypassAuth() =
        testDispatcher.runBlockingTest {
            controller.startAuthenticateSource(
                host = host,
                source = SourceFixtures.SOURCE_WITH_SOURCE_ORDER.copy(
                    flow = Source.Flow.None
                ),
                requestOptions = REQUEST_OPTIONS
            )
            verify(activity).startActivityForResult(
                intentArgumentCaptor.capture(),
                eq(StripePaymentController.SOURCE_REQUEST_CODE)
            )
            val intent = intentArgumentCaptor.firstValue
            assertThat(intent.component?.className)
                .isEqualTo(PaymentRelayActivity::class.java.name)

            verifyAnalytics(AnalyticsEvent.AuthSourceStart)
        }

    @Test
    fun result_creationRoundTrip_shouldReturnExpectedObject() {
        val expectedResult = PaymentFlowResult.Unvalidated(
            clientSecret = "client_secret",
            exception = InvalidRequestException(
                stripeError = StripeErrorFixtures.INVALID_REQUEST_ERROR,
                requestId = "req_123",
                statusCode = 404,
                message = "There was an exception",
                cause = IllegalArgumentException()
            ),
            source = SourceFixtures.CARD,
            sourceId = SourceFixtures.CARD.id,
            flowOutcome = StripeIntentResult.Outcome.SUCCEEDED,
            canCancelSource = true
        )
        val resultBundle = ParcelUtils.copy(
            expectedResult.toBundle(),
            Bundle.CREATOR
        )
        assertThat(
            PaymentFlowResult.Unvalidated.fromIntent(
                Intent().putExtras(resultBundle)
            )
        ).isEqualTo(expectedResult)
    }

    @Test
    fun `confirmAndAuthenticateAlipay() should return expected outcome`() =
        testDispatcher.runBlockingTest {
            whenever(alipayRepository.authenticate(any(), any(), any())).thenReturn(
                AlipayAuthResult(
                    StripeIntentResult.Outcome.SUCCEEDED
                )
            )
            stripeRepository.retrievePaymentIntentResponse =
                PaymentIntentFixtures.ALIPAY_REQUIRES_ACTION

            val actualResponse = controller.confirmAndAuthenticateAlipay(
                ConfirmPaymentIntentParams.createWithPaymentMethodId(
                    "pm_123",
                    "client_secret",
                ),
                mock(),
                REQUEST_OPTIONS
            )

            assertThat(stripeRepository.confirmPaymentIntentArgs).hasSize(1)
            assertThat(stripeRepository.confirmPaymentIntentArgs[0].first.shouldUseStripeSdk()).isTrue()
            assertThat(stripeRepository.confirmPaymentIntentArgs[0].second).isSameInstanceAs(
                REQUEST_OPTIONS
            )
            assertThat(stripeRepository.confirmPaymentIntentArgs[0].third).isSameInstanceAs(
                EXPAND_PAYMENT_METHOD
            )
            assertThat(actualResponse.intent).isEqualTo(PaymentIntentFixtures.ALIPAY_REQUIRES_ACTION)
            assertThat(actualResponse.outcome).isEqualTo(StripeIntentResult.Outcome.SUCCEEDED)
        }

    @Test
    fun `returnUrl is overridden when PM from confirmStripeIntentParams is not in the blocklist`() =
        testDispatcher.runBlockingTest {
            controller.startConfirmAndAuth(
                mock(),
                ConfirmPaymentIntentParams(
                    clientSecret = "clientSecret",
                    paymentMethodCreateParams = PaymentMethodCreateParams.Companion.createBlik()
                ),
                REQUEST_OPTIONS
            )
            assertThat(stripeRepository.confirmPaymentIntentArgs).hasSize(1)
            assertThat(stripeRepository.confirmPaymentIntentArgs[0].first.returnUrl)
                .isEqualTo("stripesdk://payment_return_url/com.stripe.android.test")
        }

    @Test
    fun `returnUrl is not overridden when PM from confirmStripeIntentParams is in the blocklist`() =
        testDispatcher.runBlockingTest {
            controller.startConfirmAndAuth(
                mock(),
                ConfirmPaymentIntentParams(
                    clientSecret = "clientSecret",
                    paymentMethodCreateParams = PaymentMethodCreateParams.Companion.createWeChatPay()
                ),
                REQUEST_OPTIONS
            )
            assertThat(stripeRepository.confirmPaymentIntentArgs).hasSize(1)
            assertThat(stripeRepository.confirmPaymentIntentArgs[0].first.returnUrl).isNull()
        }

    @Test
    fun `returnUrl is overridden when fetched PM is not in the blockList`() =
        testDispatcher.runBlockingTest {
            controller.startConfirmAndAuth(
                mock(),
                ConfirmPaymentIntentParams(
                    clientSecret = "clientSecret",
                ),
                REQUEST_OPTIONS
            )
            assertThat(stripeRepository.confirmPaymentIntentArgs).hasSize(1)
            assertThat(stripeRepository.confirmPaymentIntentArgs[0].first.returnUrl)
                .isEqualTo("stripesdk://payment_return_url/com.stripe.android.test")
        }

    @Test
    fun `returnUrl is not overridden when fetched PM is in the blockList`() =
        testDispatcher.runBlockingTest {
            stripeRepository.retrieveStripeIntentResponse =
                PaymentIntentFixtures.PI_REQUIRES_WECHAT_PAY_AUTHORIZE
            controller.startConfirmAndAuth(
                mock(),
                ConfirmPaymentIntentParams(
                    clientSecret = "clientSecret",
                ),
                REQUEST_OPTIONS
            )
            assertThat(stripeRepository.confirmPaymentIntentArgs).hasSize(1)
            assertThat(stripeRepository.confirmPaymentIntentArgs[0].first.returnUrl).isNull()
        }

    private fun createController(): StripePaymentController {
        return StripePaymentController(
            context,
            { ApiKeyFixtures.FAKE_PUBLISHABLE_KEY },
            stripeRepository,
            false,
            analyticsRequestExecutor,
            analyticsRequestFactory,
            alipayRepository,
            workContext = testDispatcher,
            uiContext = testDispatcher
        )
    }

    private class FakeStripeRepository : AbsFakeStripeRepository() {
        var retrieveStripeIntentResponse = PaymentIntentFixtures.PI_REQUIRES_REDIRECT
        var retrievePaymentIntentResponse = PaymentIntentFixtures.PI_REQUIRES_REDIRECT
        var cancelPaymentIntentResponse = PaymentIntentFixtures.CANCELLED
        var confirmPaymentIntentResponse = PaymentIntentFixtures.PI_WITH_SHIPPING

        val retrievePaymentIntentArgs =
            mutableListOf<Triple<String, ApiRequest.Options, List<String>>>()
        val cancelPaymentIntentArgs = mutableListOf<Triple<String, String, ApiRequest.Options>>()
        val confirmPaymentIntentArgs =
            mutableListOf<Triple<ConfirmPaymentIntentParams, ApiRequest.Options, List<String>>>()

        override suspend fun retrieveSetupIntent(
            clientSecret: String,
            options: ApiRequest.Options,
            expandFields: List<String>
        ) = SetupIntentFixtures.SI_NEXT_ACTION_REDIRECT

        override suspend fun retrievePaymentIntent(
            clientSecret: String,
            options: ApiRequest.Options,
            expandFields: List<String>
        ): PaymentIntent {
            retrievePaymentIntentArgs.add(
                Triple(clientSecret, options, expandFields)
            )
            return retrievePaymentIntentResponse
        }

        override suspend fun retrieveStripeIntent(
            clientSecret: String,
            options: ApiRequest.Options,
            expandFields: List<String>
        ): StripeIntent {
            return retrieveStripeIntentResponse
        }

        override suspend fun retrieveSource(
            sourceId: String,
            clientSecret: String,
            options: ApiRequest.Options
        ) = SourceFixtures.SOURCE_CARD.copy(status = Source.Status.Chargeable)

        override suspend fun cancelPaymentIntentSource(
            paymentIntentId: String,
            sourceId: String,
            options: ApiRequest.Options
        ): PaymentIntent {
            cancelPaymentIntentArgs.add(
                Triple(paymentIntentId, sourceId, options)
            )
            return cancelPaymentIntentResponse
        }

        override suspend fun cancelSetupIntentSource(
            setupIntentId: String,
            sourceId: String,
            options: ApiRequest.Options
        ) = SetupIntentFixtures.CANCELLED

        override suspend fun confirmPaymentIntent(
            confirmPaymentIntentParams: ConfirmPaymentIntentParams,
            options: ApiRequest.Options,
            expandFields: List<String>
        ): PaymentIntent {
            confirmPaymentIntentArgs.add(
                Triple(confirmPaymentIntentParams, options, expandFields)
            )
            return confirmPaymentIntentResponse
        }
    }

    private fun verifyAnalytics(event: AnalyticsEvent) {
        verify(analyticsRequestExecutor)
            .executeAsync(analyticsRequestArgumentCaptor.capture())
        val analyticsRequest = analyticsRequestArgumentCaptor.firstValue
        assertThat(
            analyticsRequest.compactParams?.get(AnalyticsRequestFactory.FIELD_EVENT)
        ).isEqualTo(event.toString())
    }

    private companion object {
        private const val ACCOUNT_ID = "acct_123"
        private val REQUEST_OPTIONS = ApiRequest.Options(
            apiKey = ApiKeyFixtures.FAKE_PUBLISHABLE_KEY,
            stripeAccount = ACCOUNT_ID
        )
    }
}
