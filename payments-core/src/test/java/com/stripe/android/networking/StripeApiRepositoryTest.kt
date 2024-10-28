package com.stripe.android.networking

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.FakeFraudDetectionDataRepository
import com.stripe.android.FileFactory
import com.stripe.android.FinancialConnectionsFixtures
import com.stripe.android.Stripe
import com.stripe.android.core.exception.APIConnectionException
import com.stripe.android.core.exception.InvalidRequestException
import com.stripe.android.core.frauddetection.FraudDetectionData
import com.stripe.android.core.frauddetection.FraudDetectionDataParamsUtils
import com.stripe.android.core.frauddetection.FraudDetectionDataRepository
import com.stripe.android.core.model.StripeFileParams
import com.stripe.android.core.model.StripeFilePurpose
import com.stripe.android.core.networking.AnalyticsRequest
import com.stripe.android.core.networking.AnalyticsRequestExecutor
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.core.networking.DefaultStripeNetworkClient
import com.stripe.android.core.networking.FileUploadRequest
import com.stripe.android.core.networking.StripeNetworkClient
import com.stripe.android.core.networking.StripeRequest
import com.stripe.android.core.networking.StripeResponse
import com.stripe.android.core.version.StripeSdkVersion
import com.stripe.android.model.BankAccountTokenParamsFixtures
import com.stripe.android.model.BinFixtures
import com.stripe.android.model.CardParams
import com.stripe.android.model.CardParamsFixtures
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.ConfirmSetupIntentParams
import com.stripe.android.model.ConfirmStripeIntentParams
import com.stripe.android.model.ConsumerFixtures
import com.stripe.android.model.CreateFinancialConnectionsSessionForDeferredPaymentParams
import com.stripe.android.model.CreateFinancialConnectionsSessionParams
import com.stripe.android.model.DeferredIntentParams
import com.stripe.android.model.ElementsSessionFixtures
import com.stripe.android.model.ElementsSessionParams
import com.stripe.android.model.LinkMode
import com.stripe.android.model.ListPaymentMethodsParams
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodCreateParamsFixtures
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.model.PaymentMethodMessageFixtures
import com.stripe.android.model.PaymentMethodOptionsParams
import com.stripe.android.model.SetupIntent
import com.stripe.android.model.SetupIntentFixtures
import com.stripe.android.model.SourceFixtures
import com.stripe.android.model.SourceParams
import com.stripe.android.model.Stripe3ds2AuthParams
import com.stripe.android.model.Stripe3ds2Fixtures
import com.stripe.android.model.StripeFileFixtures
import com.stripe.android.model.TokenFixtures
import com.stripe.android.model.VerificationMethodParam
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import org.mockito.kotlin.KArgumentCaptor
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.argWhere
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import java.io.IOException
import java.net.HttpURLConnection
import java.net.UnknownHostException
import java.util.Calendar
import java.util.Locale
import java.util.UUID
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Ignore
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
@Suppress("MaximumLineLength")
internal class StripeApiRepositoryTest {
    private val testDispatcher = UnconfinedTestDispatcher()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val stripeApiRepository = StripeApiRepository(
        context,
        { DEFAULT_OPTIONS.apiKey },
        workContext = testDispatcher
    )
    private val fileFactory = FileFactory(context)

    private val stripeNetworkClient: StripeNetworkClient = mock()
    private val analyticsRequestExecutor: AnalyticsRequestExecutor = mock()
    private val fraudDetectionDataRepository: FraudDetectionDataRepository = mock()

    private val apiRequestArgumentCaptor: KArgumentCaptor<ApiRequest> = argumentCaptor()
    private val fileUploadRequestArgumentCaptor: KArgumentCaptor<FileUploadRequest> =
        argumentCaptor()
    private val analyticsRequestArgumentCaptor: KArgumentCaptor<AnalyticsRequest> = argumentCaptor()

    @BeforeTest
    fun before() {
        whenever(fraudDetectionDataRepository.getCached()).thenReturn(
            FraudDetectionData(
                guid = UUID.randomUUID().toString(),
                muid = UUID.randomUUID().toString(),
                sid = UUID.randomUUID().toString(),
                timestamp = Calendar.getInstance().timeInMillis,
            )
        )
    }

    @AfterTest
    fun cleanup() {
        Stripe.advancedFraudSignalsEnabled = true
    }

    @Test
    fun testGetApiUrl() {
        val tokensApi = StripeApiRepository.tokensUrl
        assertThat(tokensApi).isEqualTo("https://api.stripe.com/v1/tokens")
    }

    @Test
    fun testGetSourcesUrl() {
        val sourcesUrl = StripeApiRepository.sourcesUrl
        assertThat(sourcesUrl).isEqualTo("https://api.stripe.com/v1/sources")
    }

    @Test
    fun testGetRetrieveSourceUrl() {
        val sourceUrlWithId = StripeApiRepository.getRetrieveSourceApiUrl("abc123")
        assertThat(sourceUrlWithId).isEqualTo("https://api.stripe.com/v1/sources/abc123")
    }

    @Test
    fun testGetRequestTokenApiUrl() {
        val tokenId = "tok_sample"
        val requestApi = StripeApiRepository.getRetrieveTokenApiUrl(tokenId)
        assertThat(requestApi).isEqualTo("https://api.stripe.com/v1/tokens/$tokenId")
    }

    @Test
    fun testGetRetrieveCustomerUrl() {
        val customerId = "cus_123abc"
        val customerRequestUrl = StripeApiRepository.getRetrieveCustomerUrl(customerId)
        assertThat(customerRequestUrl)
            .isEqualTo("https://api.stripe.com/v1/customers/$customerId")
    }

    @Test
    fun testGetAddCustomerSourceUrl() {
        val customerId = "cus_123abc"
        val addSourceUrl = StripeApiRepository.getAddCustomerSourceUrl(customerId)
        assertThat(addSourceUrl)
            .isEqualTo("https://api.stripe.com/v1/customers/$customerId/sources")
    }

    @Test
    fun testGetDeleteCustomerSourceUrl() {
        val customerId = "cus_123abc"
        val sourceId = "src_456xyz"
        val deleteSourceUrl = StripeApiRepository.getDeleteCustomerSourceUrl(customerId, sourceId)
        assertThat(deleteSourceUrl)
            .isEqualTo("https://api.stripe.com/v1/customers/$customerId/sources/$sourceId")
    }

    @Test
    fun testGetAttachPaymentMethodUrl() {
        val paymentMethodId = "pm_1ETDEa2eZvKYlo2CN5828c52"
        val attachUrl = StripeApiRepository.getAttachPaymentMethodUrl(paymentMethodId)
        val expectedUrl = arrayOf(
            "https://api.stripe.com/v1/payment_methods/",
            paymentMethodId,
            "/attach"
        ).joinToString("")
        assertThat(attachUrl).isEqualTo(expectedUrl)
    }

    @Test
    fun testGetDetachPaymentMethodUrl() {
        val paymentMethodId = "pm_1ETDEa2eZvKYlo2CN5828c52"
        val detachUrl = stripeApiRepository.getDetachPaymentMethodUrl(paymentMethodId)
        val expectedUrl = arrayOf(
            "https://api.stripe.com/v1/payment_methods/",
            paymentMethodId,
            "/detach"
        ).joinToString("")
        assertThat(detachUrl).isEqualTo(expectedUrl)
    }

    @Test
    fun testGetPaymentMethodsUrl() {
        assertThat(StripeApiRepository.paymentMethodsUrl)
            .isEqualTo("https://api.stripe.com/v1/payment_methods")
    }

    @Test
    fun testGetIssuingCardPinUrl() {
        assertThat(StripeApiRepository.getIssuingCardPinUrl("card123"))
            .isEqualTo("https://api.stripe.com/v1/issuing/cards/card123/pin")
    }

    @Test
    fun testRetrievePaymentIntentUrl() {
        assertThat(StripeApiRepository.getRetrievePaymentIntentUrl("pi123"))
            .isEqualTo("https://api.stripe.com/v1/payment_intents/pi123")
    }

    @Test
    fun testGetRefreshPaymentIntentUrl() {
        assertThat(StripeApiRepository.getRefreshPaymentIntentUrl("pi123"))
            .isEqualTo("https://api.stripe.com/v1/payment_intents/pi123/refresh")
    }

    @Test
    fun testGetRefreshSetupIntentUrl() {
        assertThat(StripeApiRepository.getRefreshSetupIntentUrl("pi123"))
            .isEqualTo("https://api.stripe.com/v1/setup_intents/pi123/refresh")
    }

    @Test
    fun testConfirmPaymentIntentUrl() {
        assertThat(StripeApiRepository.getConfirmPaymentIntentUrl("pi123"))
            .isEqualTo("https://api.stripe.com/v1/payment_intents/pi123/confirm")
    }

    @Test
    fun testLogoutConsumerUrl() {
        assertThat(StripeApiRepository.logoutConsumerUrl)
            .isEqualTo("https://api.stripe.com/v1/consumers/sessions/log_out")
    }

    @Test
    fun testLinkFinancialConnectionsSessionUrlUrl() {
        assertThat(StripeApiRepository.linkFinancialConnectionsSessionUrl)
            .isEqualTo("https://api.stripe.com/v1/consumers/link_account_sessions")
    }

    @Test
    fun testDeferredFinancialConnectionsSessionUrlUrl() {
        assertThat(StripeApiRepository.deferredFinancialConnectionsSessionUrl)
            .isEqualTo("https://api.stripe.com/v1/connections/link_account_sessions_for_deferred_payment")
    }

    @Test
    fun testConsumerPaymentDetailsUrl() {
        assertThat(StripeApiRepository.consumerPaymentDetailsUrl)
            .isEqualTo("https://api.stripe.com/v1/consumers/payment_details")
    }

    @Test
    fun testListConsumerPaymentDetailsUrl() {
        assertThat(StripeApiRepository.listConsumerPaymentDetailsUrl)
            .isEqualTo("https://api.stripe.com/v1/consumers/payment_details/list")
    }

    @Test
    fun testGetConsumerPaymentDetailsUrl() {
        assertThat(StripeApiRepository.getConsumerPaymentDetailsUrl("csmrpd*123"))
            .isEqualTo("https://api.stripe.com/v1/consumers/payment_details/csmrpd*123")
    }

    @Test
    fun createSource_shouldLogSourceCreation_andReturnSource() = runTest {
        // Check that we get a token back; we don't care about its fields for this test.
        requireNotNull(
            stripeApiRepository.createSource(
                SourceParams.createCardParams(CARD_PARAMS),
                DEFAULT_OPTIONS
            )
        )
    }

    @Test
    fun createCardSource_setsCorrectPaymentUserAgent() =
        runTest {
            val stripeResponse = StripeResponse(
                200,
                SourceFixtures.SOURCE_CARD_JSON.toString(),
                emptyMap()
            )
            whenever(stripeNetworkClient.executeRequest(any<ApiRequest>()))
                .thenReturn(stripeResponse)

            val productUsage = "TestProductUsage"
            create(setOf(productUsage)).createSource(
                SourceParams.createCardParams(CardParamsFixtures.DEFAULT),
                DEFAULT_OPTIONS
            )

            verify(stripeNetworkClient).executeRequest(apiRequestArgumentCaptor.capture())
            val apiRequest = apiRequestArgumentCaptor.firstValue
            assertThat(apiRequest.params?.get("payment_user_agent"))
                .isEqualTo(
                    "stripe-android/${StripeSdkVersion.VERSION_NAME};$productUsage"
                )
        }

    @Test
    fun createCardSource_withAttribution_setsCorrectPaymentUserAgent() =
        runTest {
            val stripeResponse = StripeResponse(
                200,
                SourceFixtures.SOURCE_CARD_JSON.toString(),
                emptyMap()
            )
            whenever(stripeNetworkClient.executeRequest(any<ApiRequest>()))
                .thenReturn(stripeResponse)

            val productUsage = "TestProductUsage"
            create(setOf(productUsage)).createSource(
                SourceParams.createCardParams(CardParamsFixtures.WITH_ATTRIBUTION),
                DEFAULT_OPTIONS
            )

            verify(stripeNetworkClient).executeRequest(apiRequestArgumentCaptor.capture())
            val apiRequest = apiRequestArgumentCaptor.firstValue
            assertThat(apiRequest.params?.get("payment_user_agent"))
                .isEqualTo(
                    "stripe-android/${StripeSdkVersion.VERSION_NAME}" +
                        ";$productUsage;CardInputView"
                )
        }

    @Test
    fun createCardSource_withAttribution_shouldPopulateProductUsage() =
        runTest {
            val stripeResponse = StripeResponse(
                200,
                SourceFixtures.SOURCE_CARD_JSON.toString(),
                emptyMap()
            )
            whenever(stripeNetworkClient.executeRequest(any<ApiRequest>()))
                .thenReturn(stripeResponse)
            create().createSource(
                SourceParams.createCardParams(CardParamsFixtures.WITH_ATTRIBUTION),
                DEFAULT_OPTIONS
            )

            verifyFraudDetectionDataAndAnalyticsRequests(
                PaymentAnalyticsEvent.SourceCreate,
                productUsage = listOf("CardInputView")
            )
        }

    @Test
    fun createAlipaySource_withAttribution_shouldPopulateProductUsage() =
        runTest {
            val stripeResponse = StripeResponse(
                200,
                SourceFixtures.ALIPAY_JSON.toString(),
                emptyMap()
            )
            whenever(stripeNetworkClient.executeRequest(any<ApiRequest>()))
                .thenReturn(stripeResponse)
            create().createSource(
                SourceParams.createMultibancoParams(
                    100,
                    "return_url",
                    "jenny@example.com"
                ),
                DEFAULT_OPTIONS
            )

            verifyFraudDetectionDataAndAnalyticsRequests(
                PaymentAnalyticsEvent.SourceCreate,
                productUsage = null
            )
        }

    @Test
    fun createSource_withConnectAccount_keepsHeaderInAccount() = runTest {
        val connectAccountId = "acct_1Acj2PBUgO3KuWzz"

        // Check that we get a source back; we don't care about its fields for this test.
        requireNotNull(
            stripeApiRepository.createSource(
                SourceParams.createCardParams(CARD_PARAMS),
                ApiRequest.Options(
                    ApiKeyFixtures.CONNECTED_ACCOUNT_PUBLISHABLE_KEY,
                    connectAccountId
                )
            )
        )
    }

    @Test
    fun retrieveSource_shouldFireAnalytics_andReturnSource() = runTest {
        val stripeResponse = StripeResponse(
            200,
            SourceFixtures.SOURCE_CARD_JSON.toString(),
            emptyMap()
        )
        whenever(stripeNetworkClient.executeRequest(any<ApiRequest>()))
            .thenReturn(stripeResponse)

        val sourceId = "src_19t3xKBZqEXluyI4uz2dxAfQ"
        val source = create().retrieveSource(
            sourceId,
            "mocked",
            DEFAULT_OPTIONS
        ).getOrThrow()

        assertThat(source.id)
            .isEqualTo(sourceId)

        verifyAnalyticsRequest(PaymentAnalyticsEvent.SourceRetrieve)
    }

    @Test
    fun start3ds2Auth_withInvalidSource_shouldThrowInvalidRequestException() =
        runTest {
            val authParams = Stripe3ds2AuthParams(
                "src_invalid",
                "1.0.0",
                "3DS_LOA_SDK_STIN_12345",
                UUID.randomUUID().toString(),
                "eyJlbmMiOiJBMTI4Q0JDLUhTMjU2IiwiYWxnIjoiUlNBLU9BRVAtMjU2In0.nid2Q-Ii21c" +
                    "SPHBaszR5KSXz866yX9I7AthLKpfWZoc7RIfz11UJ1EHuvIRDIyqqJ8txNUKKoL4keqMTqK5Yc5Tqsx" +
                    "Mn0nML8pZaPn40nXsJm_HFv3zMeOtRR7UTewsDWIgf5J-A6bhowIOmvKPCJRxspn_Cmja-" +
                    "YpgFWTp08uoJvqgntgg1lHmI1kh1UV6DuseYFUfuQlICTqC3TspAzah2CALWZORF_" +
                    "QtSeHc_RuqK02wOQMs-7079jRuSdBXvI6dQnL5ESH25wHHosfjHMZ9vtdUFNJo9J35U" +
                    "I1sdWFDzzj8k7bt0BupZhyeU0PSM9EHP-yv01-MQ9eslPTVNbFJ9YOHtq8WamvlKDr" +
                    "1sKxz6Ac_gUM8NgEcPP9SafPVxDd4H1Fwb5-4NYu2AD4xoAgMWE-YtzvfIFXZcU46N" +
                    "Doi6Xum3cHJqTH0UaOhBoqJJft9XZXYW80fjts-v28TkA76-QPF7CTDM6KbupvBkSoR" +
                    "q218eJLEywySXgCwf-Q95fsBtnnyhKcvfRaByq5kT7PH3DYD1rCQLexJ76A79kurre9p" +
                    "DjTKAv85G9DNkOFuVUYnNB3QGFReCcF9wzkGnZXdfkgN2BkB6n94bbkEyjbRb5r37XH6oR" +
                    "agx2fWLVj7kC5baeIwUPVb5kV_x4Kle7C-FPY1Obz4U7s6SVRnLGXY.IP9OcQx5uZxBRluO" +
                    "pn1m6Q.w-Ko5Qg6r-KCmKnprXEbKA7wV-SdLNDAKqjtuku6hda_0crOPRCPU4nn26Yxj7EG.p" +
                    "01pl8CKukuXzjLeY3a_Ew",
                """
            {
                "kty": "EC",
                "use": "sig",
                "crv": "P-256",
                "kid": "b23da28b-d611-46a8-93af-44ad57ce9c9d",
                "x": "hSwyaaAp3ppSGkpt7d9G8wnp3aIXelsZVo05EPpqetg",
                "y": "OUVOv9xPh5RYWapla0oz3vCJWRRXlDmppy5BGNeSl-A"
            }
                """.trimIndent(),
                Stripe3ds2Fixtures.MESSAGE_VERSION,
                10,
                "stripe://payment-auth-return"
            )

            val invalidRequestException = stripeApiRepository.start3ds2Auth(
                authParams,
                DEFAULT_OPTIONS
            ).exceptionOrNull() as InvalidRequestException

            assertThat(invalidRequestException.stripeError?.param).isEqualTo("source")
            assertThat(invalidRequestException.stripeError?.code).isEqualTo("resource_missing")
        }

    @Test
    fun complete3ds2Auth_withInvalidSource_shouldThrowInvalidRequestException() =
        runTest {
            val invalidRequestException = stripeApiRepository.complete3ds2Auth(
                "src_123",
                DEFAULT_OPTIONS
            ).exceptionOrNull() as InvalidRequestException

            assertThat(invalidRequestException.statusCode)
                .isEqualTo(HttpURLConnection.HTTP_NOT_FOUND)
            assertThat(invalidRequestException.stripeError?.param)
                .isEqualTo("source")
            assertThat(invalidRequestException.stripeError?.code)
                .isEqualTo("resource_missing")
        }

    @Test
    fun fireAnalyticsRequest_shouldReturnSuccessful() {
        // This is the one and only test where we actually log something, because
        // we are testing whether or not we log.
        stripeApiRepository.fireAnalyticsRequest(
            AnalyticsRequest(emptyMap<String, String>(), emptyMap())
        )
    }

    @Test
    fun requestData_withConnectAccount_shouldReturnCorrectResponseHeaders() =
        runTest {
            val connectAccountId = "acct_1Acj2PBUgO3KuWzz"
            val response = requireNotNull(
                stripeApiRepository.makeApiRequest(
                    DEFAULT_API_REQUEST_FACTORY.createPost(
                        StripeApiRepository.sourcesUrl,
                        ApiRequest.Options(
                            ApiKeyFixtures.CONNECTED_ACCOUNT_PUBLISHABLE_KEY,
                            connectAccountId
                        ),
                        SourceParams.createCardParams(CARD_PARAMS).toParamMap()
                    ),
                    onResponse = {}
                )
            )

            val accountsHeader = requireNotNull(
                response.getHeaderValue(STRIPE_ACCOUNT_RESPONSE_HEADER)
            ) {
                "Stripe API response should contain 'Stripe-Account' header"
            }
            assertThat(accountsHeader)
                .containsExactly(connectAccountId)
        }

    @Test
    fun confirmPaymentIntent_withSourceData_canSuccessfulConfirm() =
        runTest {
            // put a private key here to simulate the backend
            val clientSecret = "pi_12345_secret_fake"

            whenever(stripeNetworkClient.executeRequest(any<ApiRequest>()))
                .thenReturn(
                    StripeResponse(
                        200,
                        PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2_JSON.toString(),
                        emptyMap()
                    )
                )

            val confirmPaymentIntentParams =
                ConfirmPaymentIntentParams.createWithPaymentMethodCreateParams(
                    PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
                    clientSecret
                )

            requireNotNull(
                create().confirmPaymentIntent(
                    confirmPaymentIntentParams,
                    ApiRequest.Options(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY)
                ).getOrNull()
            )

            verify(stripeNetworkClient).executeRequest(apiRequestArgumentCaptor.capture())
            val apiRequest = apiRequestArgumentCaptor.firstValue
            val paymentMethodDataParams =
                apiRequest.params?.get("payment_method_data") as Map<*, *>
            assertThat(paymentMethodDataParams["muid"]).isInstanceOf(String::class.java)
            assertThat(paymentMethodDataParams["guid"]).isInstanceOf(String::class.java)
            assertThat(paymentMethodDataParams["type"]).isEqualTo("card")

            verifyFraudDetectionDataAndAnalyticsRequests(PaymentAnalyticsEvent.PaymentIntentConfirm)

            val analyticsRequest = analyticsRequestArgumentCaptor.firstValue
            assertThat(analyticsRequest.params["source_type"])
                .isEqualTo(PaymentMethod.Type.Card.code)
        }

    @Test
    fun confirmPaymentIntent_setsCorrectPaymentUserAgent() =
        runTest {
            // put a private key here to simulate the backend
            val clientSecret = "pi_12345_secret_fake"
            whenever(stripeNetworkClient.executeRequest(any<ApiRequest>()))
                .thenReturn(
                    StripeResponse(
                        200,
                        PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2_JSON.toString(),
                        emptyMap()
                    )
                )

            val confirmPaymentIntentParams =
                ConfirmPaymentIntentParams.createWithPaymentMethodCreateParams(
                    PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
                    clientSecret
                )

            val productUsage = "TestProductUsage"
            requireNotNull(
                create(setOf(productUsage)).confirmPaymentIntent(
                    confirmPaymentIntentParams,
                    ApiRequest.Options(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY)
                ).getOrNull()
            )

            verify(stripeNetworkClient).executeRequest(apiRequestArgumentCaptor.capture())
            val apiRequest = apiRequestArgumentCaptor.firstValue
            val paymentMethodDataParams =
                apiRequest.params?.get(ConfirmStripeIntentParams.PARAM_PAYMENT_METHOD_DATA) as Map<*, *>
            assertThat(paymentMethodDataParams["payment_user_agent"])
                .isEqualTo("stripe-android/${StripeSdkVersion.VERSION_NAME};$productUsage")

            verifyAnalyticsRequest(
                event = PaymentAnalyticsEvent.PaymentIntentConfirm,
                productUsage = listOf(productUsage),
            )
        }

    @Test
    fun confirmPaymentIntent_sendsErrorMessageAnalytic() =
        runTest {
            // put a private key here to simulate the backend
            val clientSecret = "pi_12345_secret_fake"
            whenever(stripeNetworkClient.executeRequest(any<ApiRequest>()))
                .thenAnswer {
                    throw IOException()
                }

            val confirmPaymentIntentParams =
                ConfirmPaymentIntentParams.createWithPaymentMethodCreateParams(
                    PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
                    clientSecret
                )

            val productUsage = "TestProductUsage"
            requireNotNull(
                create(setOf(productUsage)).confirmPaymentIntent(
                    confirmPaymentIntentParams,
                    ApiRequest.Options(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY)
                )
            )

            verifyAnalyticsRequest(
                event = PaymentAnalyticsEvent.PaymentIntentConfirm,
                productUsage = listOf(productUsage),
                errorMessage = "ioException",
            )
        }

    @Test
    fun confirmPaymentIntent_sendsErrorMessageAnalyticForResponseError() =
        runTest {
            // put a private key here to simulate the backend
            val clientSecret = "pi_12345_secret_fake"
            whenever(stripeNetworkClient.executeRequest(any<ApiRequest>()))
                .thenAnswer {
                    StripeResponse(
                        code = 500,
                        body = "An internal error has occurred"
                    )
                }

            val confirmPaymentIntentParams =
                ConfirmPaymentIntentParams.createWithPaymentMethodCreateParams(
                    PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
                    clientSecret
                )

            val productUsage = "TestProductUsage"
            requireNotNull(
                create(setOf(productUsage)).confirmPaymentIntent(
                    confirmPaymentIntentParams,
                    ApiRequest.Options(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY)
                )
            )

            verifyAnalyticsRequest(
                event = PaymentAnalyticsEvent.PaymentIntentConfirm,
                productUsage = listOf(productUsage),
                errorMessage = "apiError",
            )
        }

    @Test
    fun confirmPaymentIntent_withSourceAttribution_setsCorrectPaymentUserAgent() =
        runTest {
            // put a private key here to simulate the backend
            val clientSecret = "pi_12345_secret_fake"
            whenever(stripeNetworkClient.executeRequest(any<ApiRequest>()))
                .thenReturn(
                    StripeResponse(
                        200,
                        PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2_JSON.toString(),
                        emptyMap()
                    )
                )

            val sourceParamsProductUsage = "SourceParamsToken"
            val confirmPaymentIntentParams =
                ConfirmPaymentIntentParams.createWithSourceParams(
                    SourceParams.createCardParams(
                        CARD_PARAMS.copy(loggingTokens = setOf(sourceParamsProductUsage))
                    ),
                    clientSecret,
                    "returnUrl"
                )

            val stripeApiRequestProductUsage = "TestProductUsage"
            requireNotNull(
                create(setOf(stripeApiRequestProductUsage)).confirmPaymentIntent(
                    confirmPaymentIntentParams,
                    ApiRequest.Options(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY)
                ).getOrNull()
            )

            verify(stripeNetworkClient).executeRequest(apiRequestArgumentCaptor.capture())
            val apiRequest = apiRequestArgumentCaptor.firstValue
            val paymentMethodDataParams =
                apiRequest.params?.get(ConfirmPaymentIntentParams.PARAM_SOURCE_DATA) as Map<*, *>
            assertThat(paymentMethodDataParams["payment_user_agent"])
                .isEqualTo(
                    "stripe-android/${StripeSdkVersion.VERSION_NAME};" +
                        "$stripeApiRequestProductUsage;$sourceParamsProductUsage"
                )
        }

    @Test
    fun confirmPaymentIntent_withPaymentMethodCreateParamsAttribution_setsCorrectPaymentUserAgent() =
        runTest {
            // put a private key here to simulate the backend
            val clientSecret = "pi_12345_secret_fake"
            whenever(stripeNetworkClient.executeRequest(any<ApiRequest>()))
                .thenReturn(
                    StripeResponse(
                        200,
                        PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2_JSON.toString(),
                        emptyMap()
                    )
                )

            val pmCreateParamsProductUsage = "PMCreateParamsToken"
            val confirmPaymentIntentParams =
                ConfirmPaymentIntentParams.createWithPaymentMethodCreateParams(
                    PaymentMethodCreateParamsFixtures.DEFAULT_CARD.copy(
                        productUsage = setOf(
                            pmCreateParamsProductUsage
                        )
                    ),
                    clientSecret
                )

            val productUsage = "TestProductUsage"
            requireNotNull(
                create(setOf(productUsage)).confirmPaymentIntent(
                    confirmPaymentIntentParams,
                    ApiRequest.Options(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY)
                ).getOrNull()
            )

            verify(stripeNetworkClient).executeRequest(apiRequestArgumentCaptor.capture())
            val apiRequest = apiRequestArgumentCaptor.firstValue
            val paymentMethodDataParams =
                apiRequest.params?.get(ConfirmStripeIntentParams.PARAM_PAYMENT_METHOD_DATA) as Map<*, *>
            assertThat(paymentMethodDataParams["payment_user_agent"])
                .isEqualTo(
                    "stripe-android/${StripeSdkVersion.VERSION_NAME};" +
                        "$productUsage;$pmCreateParamsProductUsage"
                )
        }

    @Test
    fun confirmPaymentIntent_withApiUserKey_sendsValidRequest() =
        runTest {
            val apiKey = "uk_12345"
            val clientSecret = "pi_12345_secret_fake"
            val confirmPaymentIntentParams = ConfirmPaymentIntentParams.create(clientSecret)
            whenever(stripeNetworkClient.executeRequest(any<ApiRequest>()))
                .thenReturn(
                    StripeResponse(
                        200,
                        PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2_JSON.toString(),
                        emptyMap()
                    )
                )

            create().confirmPaymentIntent(
                confirmPaymentIntentParams = confirmPaymentIntentParams,
                options = ApiRequest.Options(apiKey)
            )

            verify(stripeNetworkClient).executeRequest(apiRequestArgumentCaptor.capture())
            val apiRequest = apiRequestArgumentCaptor.firstValue
            assertThat(apiRequest.baseUrl)
                .contains("pi_12345/confirm")
            assertThat(apiRequest.params)
                .doesNotContainKey(ConfirmStripeIntentParams.PARAM_CLIENT_SECRET)
        }

    @Test
    fun confirmSetupIntent_setsCorrectPaymentUserAgent() =
        runTest {
            // put a private key here to simulate the backend
            val clientSecret = "seti_12345_secret_fake"
            whenever(stripeNetworkClient.executeRequest(any<ApiRequest>()))
                .thenReturn(
                    StripeResponse(
                        200,
                        SetupIntentFixtures.SI_REQUIRES_PAYMENT_METHOD_JSON.toString(),
                        emptyMap()
                    )
                )

            val confirmSetupIntentParams =
                ConfirmSetupIntentParams.create(
                    PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
                    clientSecret
                )

            val productUsage = "TestProductUsage"
            requireNotNull(
                create(setOf(productUsage)).confirmSetupIntent(
                    confirmSetupIntentParams,
                    ApiRequest.Options(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY)
                ).getOrNull()
            )

            verify(stripeNetworkClient).executeRequest(apiRequestArgumentCaptor.capture())
            val apiRequest = apiRequestArgumentCaptor.firstValue
            val paymentMethodDataParams =
                apiRequest.params?.get(ConfirmStripeIntentParams.PARAM_PAYMENT_METHOD_DATA) as Map<*, *>
            assertThat(paymentMethodDataParams["payment_user_agent"])
                .isEqualTo(
                    "stripe-android/${StripeSdkVersion.VERSION_NAME};$productUsage"
                )

            verifyAnalyticsRequest(
                event = PaymentAnalyticsEvent.SetupIntentConfirm,
                productUsage = listOf(productUsage),
            )
        }

    @Test
    fun confirmSetupIntent_sendsErrorMessageAnalytic() =
        runTest {
            // put a private key here to simulate the backend
            val clientSecret = "seti_12345_secret_fake"
            whenever(stripeNetworkClient.executeRequest(any<ApiRequest>()))
                .thenAnswer {
                    throw IOException()
                }

            val confirmSetupIntentParams =
                ConfirmSetupIntentParams.create(
                    PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
                    clientSecret
                )

            val productUsage = "TestProductUsage"
            requireNotNull(
                create(setOf(productUsage)).confirmSetupIntent(
                    confirmSetupIntentParams,
                    ApiRequest.Options(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY)
                )
            )

            verify(stripeNetworkClient).executeRequest(apiRequestArgumentCaptor.capture())

            verifyAnalyticsRequest(
                event = PaymentAnalyticsEvent.SetupIntentConfirm,
                productUsage = listOf(productUsage),
                errorMessage = "ioException",
            )
        }

    @Test
    fun confirmSetupIntent_sendsErrorMessageAnalyticForResponseError() =
        runTest {
            // put a private key here to simulate the backend
            val clientSecret = "seti_12345_secret_fake"
            whenever(stripeNetworkClient.executeRequest(any<ApiRequest>()))
                .thenAnswer {
                    StripeResponse(
                        code = 500,
                        body = "An internal error has occurred"
                    )
                }

            val confirmSetupIntentParams =
                ConfirmSetupIntentParams.create(
                    PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
                    clientSecret
                )

            val productUsage = "TestProductUsage"
            requireNotNull(
                create(setOf(productUsage)).confirmSetupIntent(
                    confirmSetupIntentParams,
                    ApiRequest.Options(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY)
                )
            )

            verify(stripeNetworkClient).executeRequest(apiRequestArgumentCaptor.capture())

            verifyAnalyticsRequest(
                event = PaymentAnalyticsEvent.SetupIntentConfirm,
                productUsage = listOf(productUsage),
                errorMessage = "apiError",
            )
        }

    @Test
    fun confirmSetupIntent_withPaymentMethodCreateParamsAttribution_setsCorrectPaymentUserAgent() =
        runTest {
            // put a private key here to simulate the backend
            val clientSecret = "seti_12345_secret_fake"
            whenever(stripeNetworkClient.executeRequest(any<ApiRequest>()))
                .thenReturn(
                    StripeResponse(
                        200,
                        SetupIntentFixtures.SI_REQUIRES_PAYMENT_METHOD_JSON.toString(),
                        emptyMap()
                    )
                )

            val pmCreateParamsProductUsage = "PMCreateParamsToken"
            val confirmSetupIntentParams =
                ConfirmSetupIntentParams.create(
                    PaymentMethodCreateParamsFixtures.DEFAULT_CARD.copy(
                        productUsage = setOf(
                            pmCreateParamsProductUsage
                        )
                    ),
                    clientSecret
                )

            val productUsage = "TestProductUsage"
            requireNotNull(
                create(setOf(productUsage)).confirmSetupIntent(
                    confirmSetupIntentParams,
                    ApiRequest.Options(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY)
                ).getOrNull()
            )

            verify(stripeNetworkClient).executeRequest(apiRequestArgumentCaptor.capture())
            val apiRequest = apiRequestArgumentCaptor.firstValue
            val paymentMethodDataParams =
                apiRequest.params?.get(ConfirmStripeIntentParams.PARAM_PAYMENT_METHOD_DATA) as Map<*, *>
            assertThat(paymentMethodDataParams["payment_user_agent"])
                .isEqualTo(
                    "stripe-android/${StripeSdkVersion.VERSION_NAME};" +
                        "$productUsage;$pmCreateParamsProductUsage"
                )
        }

    @Ignore("requires a secret key")
    fun disabled_confirmPaymentIntent_withSourceId_canSuccessfulConfirm() =
        runTest {
            val clientSecret = "temporarily put a private key here simulate the backend"
            val publishableKey = "put a public key that matches the private key here"
            val sourceId = "id of the source created on the backend"

            val confirmPaymentIntentParams = ConfirmPaymentIntentParams.createWithSourceId(
                sourceId,
                clientSecret,
                "yourapp://post-authentication-return-url"
            )
            requireNotNull(
                stripeApiRepository.confirmPaymentIntent(
                    confirmPaymentIntentParams,
                    ApiRequest.Options(publishableKey)
                ).getOrNull()
            )
        }

    @Ignore("requires a secret key")
    fun disabled_confirmRetrieve_withSourceId_canSuccessfulRetrieve() =
        runTest {
            val clientSecret = "temporarily put a private key here simulate the backend"
            val publishableKey = "put a public key that matches the private key here"

            stripeApiRepository.retrievePaymentIntent(
                clientSecret,
                ApiRequest.Options(publishableKey)
            ).getOrThrow()
        }

    @Test
    fun createSource_createsObjectAndLogs() = runTest {
        val stripeApiRepository = StripeApiRepository(
            context,
            { DEFAULT_OPTIONS.apiKey },
            workContext = testDispatcher,
            stripeNetworkClient = DefaultStripeNetworkClient(
                workContext = testDispatcher
            ),
            analyticsRequestExecutor = analyticsRequestExecutor,
            fraudDetectionDataRepository = fraudDetectionDataRepository
        )

        requireNotNull(
            stripeApiRepository.createSource(
                SourceParams.createCardParams(CARD_PARAMS),
                DEFAULT_OPTIONS
            )
        )

        verifyFraudDetectionDataAndAnalyticsRequests(PaymentAnalyticsEvent.SourceCreate)
    }

    @Test
    fun fireAnalyticsRequest_whenShouldLogRequestIsFalse_doesNotCreateAConnection() {
        val stripeApiRepository = create()
        stripeApiRepository.fireAnalyticsRequest(
            AnalyticsRequest(emptyMap<String, String>(), emptyMap())
        )
        verifyNoMoreInteractions(stripeNetworkClient)
    }

    @Test
    fun getPaymentMethods_whenPopulated_returnsExpectedList() = runTest {
        val responseBody =
            """
            {
                "object": "list",
                "data": [{
                        "id": "pm_1EVNYJCRMbs6FrXfG8n52JaK",
                        "object": "payment_method",
                        "billing_details": {
                            "address": {
                                "city": null,
                                "country": null,
                                "line1": null,
                                "line2": null,
                                "postal_code": null,
                                "state": null
                            },
                            "email": null,
                            "name": null,
                            "phone": null
                        },
                        "card": {
                            "brand": "visa",
                            "checks": {
                                "address_line1_check": null,
                                "address_postal_code_check": null,
                                "cvc_check": null
                            },
                            "country": "US",
                            "exp_month": 5,
                            "exp_year": 2020,
                            "fingerprint": "atmHgDo9nxHpQJiw",
                            "funding": "credit",
                            "generated_from": null,
                            "last4": "4242",
                            "three_d_secure_usage": {
                                "supported": true
                            },
                            "wallet": null
                        },
                        "created": 1556736791,
                        "customer": "cus_EzHwfOXxvAwRIW",
                        "livemode": false,
                        "metadata": {},
                        "type": "card"
                    },
                    {
                        "id": "pm_1EVNXtCRMbs6FrXfTlZGIdGq",
                        "object": "payment_method",
                        "billing_details": {
                            "address": {
                                "city": null,
                                "country": null,
                                "line1": null,
                                "line2": null,
                                "postal_code": null,
                                "state": null
                            },
                            "email": null,
                            "name": null,
                            "phone": null
                        },
                        "card": {
                            "brand": "visa",
                            "checks": {
                                "address_line1_check": null,
                                "address_postal_code_check": null,
                                "cvc_check": null
                            },
                            "country": "US",
                            "exp_month": 5,
                            "exp_year": 2020,
                            "fingerprint": "atmHgDo9nxHpQJiw",
                            "funding": "credit",
                            "generated_from": null,
                            "last4": "4242",
                            "three_d_secure_usage": {
                                "supported": true
                            },
                            "wallet": null
                        },
                        "created": 1556736765,
                        "customer": "cus_EzHwfOXxvAwRIW",
                        "livemode": false,
                        "metadata": {},
                        "type": "card"
                    },
                    {
                        "id": "src_1EVO8DCRMbs6FrXf2Dspj49a",
                        "object": "payment_method",
                        "billing_details": {
                            "address": {
                                "city": null,
                                "country": null,
                                "line1": null,
                                "line2": null,
                                "postal_code": null,
                                "state": null
                            },
                            "email": null,
                            "name": null,
                            "phone": null
                        },
                        "card": {
                            "brand": "visa",
                            "checks": {
                                "address_line1_check": null,
                                "address_postal_code_check": null,
                                "cvc_check": null
                            },
                            "country": "US",
                            "exp_month": 5,
                            "exp_year": 2020,
                            "fingerprint": "Ep3vs1pdQAjtri7D",
                            "funding": "credit",
                            "generated_from": null,
                            "last4": "3063",
                            "three_d_secure_usage": {
                                "supported": true
                            },
                            "wallet": null
                        },
                        "created": 1556739017,
                        "customer": "cus_EzHwfOXxvAwRIW",
                        "livemode": false,
                        "metadata": {},
                        "type": "card"
                    }
                ],
                "has_more": false,
                "url": "/v1/payment_methods"
            }
            """.trimIndent()
        val stripeResponse = StripeResponse(200, responseBody)
        val queryParams = mapOf(
            "customer" to "cus_123",
            "type" to PaymentMethod.Type.Card.code
        )

        val options = ApiRequest.Options(ApiKeyFixtures.FAKE_EPHEMERAL_KEY)
        val url = DEFAULT_API_REQUEST_FACTORY.createGet(
            StripeApiRepository.paymentMethodsUrl,
            options,
            queryParams
        ).url

        whenever(
            stripeNetworkClient.executeRequest(
                argThat<ApiRequest> {
                    ApiRequestMatcher(StripeRequest.Method.GET, url, options, queryParams)
                        .matches(this)
                }
            )
        ).thenReturn(stripeResponse)
        val stripeApiRepository = create()
        val paymentMethods = stripeApiRepository
            .getPaymentMethods(
                listPaymentMethodsParams = ListPaymentMethodsParams(
                    "cus_123",
                    PaymentMethod.Type.Card
                ),
                productUsageTokens = emptySet(),
                requestOptions = ApiRequest.Options(ApiKeyFixtures.FAKE_EPHEMERAL_KEY)
            ).getOrThrow()
        assertThat(paymentMethods)
            .hasSize(3)
        assertThat(paymentMethods.map { it.id })
            .containsExactly(
                "pm_1EVNYJCRMbs6FrXfG8n52JaK",
                "pm_1EVNXtCRMbs6FrXfTlZGIdGq",
                "src_1EVO8DCRMbs6FrXf2Dspj49a"
            )

        verifyAnalyticsRequest(
            PaymentAnalyticsEvent.CustomerRetrievePaymentMethods,
            null
        )
    }

    @Test
    fun getPaymentMethods_whenNotPopulated_returnsEmptyList() = runTest {
        val responseBody =
            """
            {
                "object": "list",
                "data": [],
                "has_more": false,
                "url": "/v1/payment_methods"
            }
            """.trimIndent()
        val stripeResponse = StripeResponse(200, responseBody)
        val queryParams = mapOf(
            "customer" to "cus_123",
            "type" to PaymentMethod.Type.Card.code
        )

        val options = ApiRequest.Options(ApiKeyFixtures.FAKE_EPHEMERAL_KEY)
        val url = DEFAULT_API_REQUEST_FACTORY.createGet(
            StripeApiRepository.paymentMethodsUrl,
            options,
            queryParams
        ).url

        whenever(
            stripeNetworkClient.executeRequest(
                argThat<ApiRequest> {
                    ApiRequestMatcher(StripeRequest.Method.GET, url, options, queryParams)
                        .matches(this)
                }
            )
        ).thenReturn(stripeResponse)
        val stripeApiRepository = create()
        val paymentMethods = stripeApiRepository
            .getPaymentMethods(
                listPaymentMethodsParams = ListPaymentMethodsParams(
                    "cus_123",
                    PaymentMethod.Type.Card
                ),
                productUsageTokens = emptySet(),
                requestOptions = ApiRequest.Options(ApiKeyFixtures.FAKE_EPHEMERAL_KEY)
            ).getOrThrow()
        assertThat(paymentMethods)
            .isEmpty()
    }

    @Test
    fun getFpxBankStatus_withFpxKey() = runTest {
        val fpxBankStatuses = stripeApiRepository.getFpxBankStatus(
            ApiRequest.Options(ApiKeyFixtures.FPX_PUBLISHABLE_KEY)
        ).getOrThrow()
        assertThat(fpxBankStatuses.size())
            .isEqualTo(27)
    }

    @Test
    fun getFpxBankStatus_withFpxKey_ignoresStripeAccountId() = runTest {
        val fpxBankStatuses = stripeApiRepository.getFpxBankStatus(
            ApiRequest.Options(
                apiKey = ApiKeyFixtures.FPX_PUBLISHABLE_KEY,
                stripeAccount = "acct_1234"
            )
        ).getOrThrow()
        assertThat(fpxBankStatuses.size())
            .isEqualTo(27)
    }

    @Test
    fun `getCardMetadata with valid bin prefix should succeed`() = runTest {
        val cardMetadata =
            stripeApiRepository.getCardMetadata(
                BinFixtures.VISA,
                ApiRequest.Options(ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY)
            ).getOrThrow()
        assertThat(cardMetadata.bin)
            .isEqualTo(BinFixtures.VISA)
        assertThat(cardMetadata.accountRanges)
            .isNotEmpty()
    }

    @Test
    fun cancelPaymentIntentSource_whenAlreadyCanceled_throwsInvalidRequestException() =
        runTest {
            val exception = stripeApiRepository.cancelPaymentIntentSource(
                paymentIntentId = "pi_1FejpSH8dsfnfKo38L276wr6",
                sourceId = "src_1FejpbH8dsfnfKo3KR7EqCzJ",
                options = ApiRequest.Options(ApiKeyFixtures.FPX_PUBLISHABLE_KEY),
            ).exceptionOrNull() as InvalidRequestException

            assertThat(exception.message).isEqualTo(
                "This PaymentIntent could be not be fulfilled via this session because" +
                    " a different payment method was attached to it. " +
                    "Another session could be attempting to fulfill this PaymentIntent." +
                    " Please complete that session or try again.",
            )
            assertThat(exception.stripeError?.code).isEqualTo("payment_intent_unexpected_state")
        }

    @Test
    fun createSource_whenUnknownHostExceptionThrown_convertsToAPIConnectionException() =
        runTest {
            whenever(stripeNetworkClient.executeRequest(any<ApiRequest>()))
                .thenAnswer { throw UnknownHostException() }

            val exception = create().createSource(
                sourceParams = SourceParams.createCardParams(CARD_PARAMS),
                options = DEFAULT_OPTIONS,
            ).exceptionOrNull()

            assertThat(exception).isInstanceOf(APIConnectionException::class.java)
        }

    @Test
    fun createFile_shouldFireExpectedRequests() = runTest {
        whenever(stripeNetworkClient.executeRequest(any<FileUploadRequest>()))
            .thenReturn(
                StripeResponse(
                    200,
                    StripeFileFixtures.DEFAULT.toString(),
                    emptyMap()
                )
            )

        val stripeRepository = create()

        stripeRepository.createFile(
            StripeFileParams(
                file = fileFactory.create(),
                purpose = StripeFilePurpose.IdentityDocument
            ),
            DEFAULT_OPTIONS
        )

        verify(stripeNetworkClient, never()).executeRequest(any<ApiRequest>())
        verify(stripeNetworkClient).executeRequest(fileUploadRequestArgumentCaptor.capture())
        assertThat(fileUploadRequestArgumentCaptor.firstValue)
            .isNotNull()

        verifyAnalyticsRequest(PaymentAnalyticsEvent.FileCreate)
    }

    @Test
    fun retrieveObject_shouldFireExpectedRequestsAndNotParseResult() = runTest {
        val responseBody = "not a valid json"
        whenever(stripeNetworkClient.executeRequest(any<ApiRequest>()))
            .thenReturn(
                StripeResponse(
                    200,
                    responseBody,
                    emptyMap()
                )
            )

        val response = create().retrieveObject(
            StripeApiRepository.paymentMethodsUrl,
            DEFAULT_OPTIONS
        ).getOrThrow()

        verify(stripeNetworkClient).executeRequest(any())
        assertThat(response.body).isEqualTo(responseBody)
        verifyAnalyticsRequest(PaymentAnalyticsEvent.StripeUrlRetrieve)
    }

    @Test
    fun apiRequest_withErrorResponse_onUnsupportedSdkVersion_shouldNotBeTranslated() =
        runTest {
            Locale.setDefault(Locale.JAPAN)

            val stripeRepository = StripeApiRepository(
                context,
                { DEFAULT_OPTIONS.apiKey },
                workContext = testDispatcher,
                sdkVersion = "AndroidBindings/13.0.0"
            )

            val ex = stripeRepository.retrieveSetupIntent(
                "seti_1CkiBMLENEVhOs7YMtUehLau_secret_invalid",
                DEFAULT_OPTIONS
            ).exceptionOrNull() as InvalidRequestException
            assertThat(ex.stripeError?.message)
                .isEqualTo("No such setupintent: 'seti_1CkiBMLENEVhOs7YMtUehLau'")
        }

    // TODO(ccen): Re-enable this test when it's fixed on backend
    //    @Test
    //    fun apiRequest_withErrorResponse_onSupportedSdkVersion_shouldBeTranslated() =
    //        runTest {
    //            Locale.setDefault(Locale.JAPAN)
    //
    //            val stripeRepository = StripeApiRepository(
    //                context,
    //                { DEFAULT_OPTIONS.apiKey },
    //                workContext = testDispatcher,
    //                sdkVersion = "AndroidBindings/14.0.0"
    //            )
    //
    //            val ex = assertFailsWith<InvalidRequestException> {
    //                stripeRepository.retrieveSetupIntent(
    //                    "seti_1CkiBMLENEVhOs7YMtUehLau_secret_invalid",
    //                    DEFAULT_OPTIONS
    //                )
    //            }
    //            assertThat(
    //                "そのような setupintent はありません : 'seti_1CkiBMLENEVhOs7YMtUehLau'",
    //                ex.stripeError?.message
    //            )
    //        }

    @Test
    fun createCardToken_setsCorrectPaymentUserAgent() =
        runTest {
            val stripeResponse = StripeResponse(
                200,
                TokenFixtures.CARD_TOKEN_JSON.toString(),
                emptyMap()
            )
            whenever(stripeNetworkClient.executeRequest(any<ApiRequest>())).thenReturn(
                stripeResponse
            )

            val productUsage = "TestProductUsage"
            create(setOf(productUsage)).createToken(
                CardParamsFixtures.DEFAULT,
                DEFAULT_OPTIONS
            )

            verify(stripeNetworkClient).executeRequest(apiRequestArgumentCaptor.capture())
            val apiRequest = apiRequestArgumentCaptor.firstValue
            assertThat(apiRequest.params?.get("payment_user_agent"))
                .isEqualTo(
                    "stripe-android/${StripeSdkVersion.VERSION_NAME};$productUsage"
                )
        }

    @Test
    fun createCardToken_withAttribution_setsCorrectPaymentUserAgent() =
        runTest {
            val stripeResponse = StripeResponse(
                200,
                TokenFixtures.CARD_TOKEN_JSON.toString(),
                emptyMap()
            )
            whenever(stripeNetworkClient.executeRequest(any<ApiRequest>())).thenReturn(
                stripeResponse
            )

            val productUsage = "TestProductUsage"
            create(setOf(productUsage)).createToken(
                CardParamsFixtures.WITH_ATTRIBUTION,
                DEFAULT_OPTIONS
            )

            verify(stripeNetworkClient).executeRequest(apiRequestArgumentCaptor.capture())
            val apiRequest = apiRequestArgumentCaptor.firstValue
            assertThat(apiRequest.params?.get("payment_user_agent"))
                .isEqualTo(
                    "stripe-android/${StripeSdkVersion.VERSION_NAME}" +
                        ";$productUsage;CardInputView"
                )
        }

    @Test
    fun createCardToken_withAttribution_shouldPopulateProductUsage() =
        runTest {
            val stripeResponse = StripeResponse(
                200,
                TokenFixtures.CARD_TOKEN_JSON.toString(),
                emptyMap()
            )
            whenever(stripeNetworkClient.executeRequest(any<ApiRequest>())).thenReturn(
                stripeResponse
            )
            create().createToken(
                CardParamsFixtures.WITH_ATTRIBUTION,
                DEFAULT_OPTIONS
            )

            verifyFraudDetectionDataAndAnalyticsRequests(
                PaymentAnalyticsEvent.TokenCreate,
                listOf("CardInputView")
            )
        }

    @Test
    fun createBankToken_shouldNotPopulateProductUsage() = runTest {
        val stripeResponse = StripeResponse(
            200,
            TokenFixtures.BANK_TOKEN_JSON.toString(),
            emptyMap()
        )
        whenever(stripeNetworkClient.executeRequest(any<ApiRequest>())).thenReturn(stripeResponse)
        create().createToken(
            BankAccountTokenParamsFixtures.DEFAULT,
            DEFAULT_OPTIONS
        )

        verifyFraudDetectionDataAndAnalyticsRequests(
            PaymentAnalyticsEvent.TokenCreate,
            productUsage = null
        )
    }

    @Test
    fun createCardPaymentMethod_setsCorrectPaymentUserAgent() =
        runTest {
            val stripeResponse = StripeResponse(
                200,
                PaymentMethodFixtures.CARD_JSON.toString(),
                emptyMap()
            )
            whenever(stripeNetworkClient.executeRequest(any<ApiRequest>()))
                .thenReturn(stripeResponse)

            val productUsage = "TestProductUsage"
            create(setOf(productUsage)).createPaymentMethod(
                PaymentMethodCreateParams.create(PaymentMethodCreateParamsFixtures.CARD),
                DEFAULT_OPTIONS
            )

            verify(stripeNetworkClient).executeRequest(apiRequestArgumentCaptor.capture())
            val apiRequest = apiRequestArgumentCaptor.firstValue
            assertThat(apiRequest.params?.get("payment_user_agent"))
                .isEqualTo("stripe-android/${StripeSdkVersion.VERSION_NAME};$productUsage")
        }

    @Test
    fun createCardPaymentMethod_withAttribution_setsCorrectPaymentUserAgent() =
        runTest {
            val stripeResponse = StripeResponse(
                200,
                PaymentMethodFixtures.CARD_JSON.toString(),
                emptyMap()
            )
            whenever(stripeNetworkClient.executeRequest(any<ApiRequest>()))
                .thenReturn(stripeResponse)

            val productUsage = "TestProductUsage"
            val cardAttribution = "CardInputView"
            create(setOf(productUsage)).createPaymentMethod(
                PaymentMethodCreateParams.create(
                    PaymentMethodCreateParamsFixtures.CARD
                        .copy(attribution = setOf(cardAttribution))
                ),
                DEFAULT_OPTIONS
            )

            verify(stripeNetworkClient).executeRequest(apiRequestArgumentCaptor.capture())
            val apiRequest = apiRequestArgumentCaptor.firstValue
            assertThat(apiRequest.params?.get("payment_user_agent"))
                .isEqualTo(
                    "stripe-android/${StripeSdkVersion.VERSION_NAME}" +
                        ";$productUsage;$cardAttribution"
                )
        }

    @Test
    fun createCardPaymentMethod_withAttribution_shouldPopulateProductUsage() =
        runTest {
            val stripeResponse = StripeResponse(
                200,
                PaymentMethodFixtures.CARD_JSON.toString(),
                emptyMap()
            )
            whenever(stripeNetworkClient.executeRequest(any<ApiRequest>()))
                .thenReturn(stripeResponse)

            create().createPaymentMethod(
                PaymentMethodCreateParams.create(
                    PaymentMethodCreateParamsFixtures.CARD
                        .copy(attribution = setOf("CardInputView"))
                ),
                DEFAULT_OPTIONS
            )

            verifyFraudDetectionDataAndAnalyticsRequests(
                PaymentAnalyticsEvent.PaymentMethodCreate,
                productUsage = listOf("CardInputView")
            )
        }

    @Test
    fun createPaymentMethodWithOverriddenParamMap_withAttribution_shouldPopulateProductUsage() =
        runTest {
            val stripeResponse = StripeResponse(
                200,
                PaymentMethodFixtures.CARD_JSON.toString(),
                emptyMap()
            )
            whenever(stripeNetworkClient.executeRequest(any<ApiRequest>()))
                .thenReturn(stripeResponse)

            val paramMap = mapOf(
                "type" to "test",
                "bacs_debit" to mapOf(
                    "account_number" to "00012345",
                    "sort_code" to "108800"
                ),
                "some_key" to mapOf(
                    "other_key" to mapOf(
                        "third_key" to "value"
                    )
                ),
                "phone" to "1-800-555-1234"
            )

            create().createPaymentMethod(
                PaymentMethodCreateParams(
                    PaymentMethod.Type.Card,
                    overrideParamMap = paramMap,
                    productUsage = setOf("PaymentSheet")
                ),
                DEFAULT_OPTIONS
            )

            verifyFraudDetectionDataAndAnalyticsRequests(
                PaymentAnalyticsEvent.PaymentMethodCreate,
                productUsage = listOf("PaymentSheet")
            )
        }

    @Test
    fun createSepaDebitPaymentMethod_shouldNotPopulateProductUsage() =
        runTest {
            val stripeResponse = StripeResponse(
                200,
                PaymentMethodFixtures.SEPA_DEBIT_JSON.toString(),
                emptyMap()
            )
            whenever(stripeNetworkClient.executeRequest(any<ApiRequest>())).thenReturn(
                stripeResponse
            )
            create().createPaymentMethod(
                PaymentMethodCreateParams.create(
                    PaymentMethodCreateParams.SepaDebit("my_iban")
                ),
                DEFAULT_OPTIONS
            )

            verifyFraudDetectionDataAndAnalyticsRequests(
                PaymentAnalyticsEvent.PaymentMethodCreate,
                productUsage = null
            )
        }

    @Test
    fun `createRadarSession() with FraudDetectionData should return expected value`() =
        runTest {
            val stripeRepository = StripeApiRepository(
                context,
                { DEFAULT_OPTIONS.apiKey },
                analyticsRequestExecutor = analyticsRequestExecutor,
                fraudDetectionDataRepository = FakeFraudDetectionDataRepository(
                    FraudDetectionData(
                        guid = "8ae65368-76c5-4dd5-81b9-279f61efa591c80a51",
                        muid = "ac3febde-f658-41b5-8c4d-94905501c7a6f4ca3c",
                        sid = "02892cd4-183a-4074-bca2-5dc0647dd816ce4cbf"
                    )
                ),
                workContext = testDispatcher
            )
            val radarSession = stripeRepository.createRadarSession(DEFAULT_OPTIONS).getOrThrow()

            assertThat(radarSession.id)
                .startsWith("rse_")

            verifyAnalyticsRequest(PaymentAnalyticsEvent.RadarSessionCreate)
        }

    @Test
    fun `createRadarSession() with null FraudDetectionData should throw an exception`() =
        runTest {
            val stripeRepository = StripeApiRepository(
                context,
                { DEFAULT_OPTIONS.apiKey },
                fraudDetectionDataRepository = FakeFraudDetectionDataRepository(
                    null
                ),
                workContext = testDispatcher
            )

            val invalidRequestException = stripeRepository.createRadarSession(DEFAULT_OPTIONS).exceptionOrNull()

            assertThat(invalidRequestException).isInstanceOf(InvalidRequestException::class.java)
            assertThat(invalidRequestException?.message)
                .isEqualTo("Could not obtain fraud data required to create a Radar Session.")
        }

    @Test
    fun `createRadarSession() with advancedFraudSignalsEnabled set to false should throw an exception`() =
        runTest {
            verifyNoInteractions(fraudDetectionDataRepository)
            Stripe.advancedFraudSignalsEnabled = false

            val stripeRepository = create()
            val invalidRequestException = stripeRepository.createRadarSession(DEFAULT_OPTIONS).exceptionOrNull()

            assertThat(invalidRequestException).isInstanceOf(InvalidRequestException::class.java)
            assertThat(invalidRequestException?.message)
                .isEqualTo("Stripe.advancedFraudSignalsEnabled must be set to 'true' to create a Radar Session.")
        }

    @Test
    fun `retrieveStripeIntent() with invalid client secret should throw exception`() =
        runTest {
            val error = stripeApiRepository.retrieveStripeIntent(
                clientSecret = "invalid!",
                options = DEFAULT_OPTIONS,
            ).exceptionOrNull()

            assertThat(error).isInstanceOf(IllegalStateException::class.java)
            assertThat(error?.message).isEqualTo("Invalid client secret.")
        }

    @Test
    fun verifyRefreshPaymentIntent() =
        runTest {
            val clientSecret = "pi_3JkCxKBNJ02ErVOj0kNqBMAZ_secret_bC6oXqo976LFM06Z9rlhmzUQq"
            whenever(stripeNetworkClient.executeRequest(any<ApiRequest>()))
                .thenReturn(
                    StripeResponse(
                        200,
                        PaymentIntentFixtures.PI_REFRESH_RESPONSE_REQUIRES_WECHAT_PAY_AUTHORIZE_JSON.toString(),
                        emptyMap()
                    )
                )

            create().refreshPaymentIntent(
                clientSecret,
                ApiRequest.Options(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY)
            ).getOrThrow()

            verify(stripeNetworkClient).executeRequest(
                argWhere<ApiRequest> {
                    it.options.apiKey == ApiKeyFixtures.FAKE_PUBLISHABLE_KEY &&
                        it.params?.get("client_secret") == clientSecret
                }
            )

            verifyFraudDetectionDataAndAnalyticsRequests(PaymentAnalyticsEvent.PaymentIntentRefresh)
        }

    @Test
    fun verifyRefreshSetupIntent() =
        runTest {
            val clientSecret = "seti_1234_secret_5678"
            whenever(stripeNetworkClient.executeRequest(any<ApiRequest>()))
                .thenReturn(
                    StripeResponse(
                        200,
                        SetupIntentFixtures.CASH_APP_PAY_REQUIRES_ACTION_JSON.toString(),
                        emptyMap()
                    )
                )

            create().refreshSetupIntent(
                clientSecret,
                ApiRequest.Options(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY)
            ).getOrThrow()

            verify(stripeNetworkClient).executeRequest(
                argWhere<ApiRequest> {
                    it.options.apiKey == ApiKeyFixtures.FAKE_PUBLISHABLE_KEY &&
                        it.params?.get("client_secret") == clientSecret
                }
            )

            verifyFraudDetectionDataAndAnalyticsRequests(PaymentAnalyticsEvent.SetupIntentRefresh)
        }

    @Test
    fun `createDeferredFinancialConnectionsSession() sends all parameters`() =
        runTest {
            val stripeResponse = StripeResponse(
                200,
                FinancialConnectionsFixtures.SESSION.toString(),
                emptyMap()
            )
            whenever(stripeNetworkClient.executeRequest(any<ApiRequest>()))
                .thenReturn(stripeResponse)

            create().createFinancialConnectionsSessionForDeferredPayments(
                CreateFinancialConnectionsSessionForDeferredPaymentParams(
                    uniqueId = "uuid",
                    initialInstitution = "initial_institution",
                    manualEntryOnly = false,
                    searchSession = "search_session_id",
                    verificationMethod = VerificationMethodParam.Automatic,
                    customer = "customer_id",
                    onBehalfOf = null,
                    linkMode = LinkMode.LinkPaymentMethod,
                    amount = 1000,
                    hostedSurface = "payment_element",
                    currency = "usd",
                    product = "instant_debits",
                ),
                DEFAULT_OPTIONS
            )

            verify(stripeNetworkClient).executeRequest(apiRequestArgumentCaptor.capture())
            val params = requireNotNull(apiRequestArgumentCaptor.firstValue.params)

            with(params) {
                assertThat(this["unique_id"]).isEqualTo("uuid")
                assertThat(this["initial_institution"]).isEqualTo("initial_institution")
                assertThat(this["manual_entry_only"]).isEqualTo(false)
                assertThat(this["search_session"]).isEqualTo("search_session_id")
                assertThat(this["verification_method"]).isEqualTo("automatic")
                assertThat(this["customer"]).isEqualTo("customer_id")
                assertThat(this["on_behalf_of"]).isEqualTo(null)
                assertThat(this["link_mode"]).isEqualTo("LINK_PAYMENT_METHOD")
                assertThat(this["amount"]).isEqualTo(1000)
                assertThat(this["hosted_surface"]).isEqualTo("payment_element")
                assertThat(this["currency"]).isEqualTo("usd")
                assertThat(this["product"]).isEqualTo("instant_debits")
            }
        }

    @Test
    fun `createDeferredFinancialConnectionsSession() sends correct link_mode if disabled`() = runTest {
        val stripeResponse = StripeResponse(
            code = 200,
            body = FinancialConnectionsFixtures.SESSION.toString(),
            headers = emptyMap()
        )
        whenever(stripeNetworkClient.executeRequest(any<ApiRequest>())).thenReturn(stripeResponse)

        create().createFinancialConnectionsSessionForDeferredPayments(
            params = CreateFinancialConnectionsSessionForDeferredPaymentParams(
                uniqueId = "uuid",
                initialInstitution = "initial_institution",
                manualEntryOnly = false,
                searchSession = "search_session_id",
                verificationMethod = VerificationMethodParam.Automatic,
                customer = "customer_id",
                onBehalfOf = null,
                linkMode = null,
                amount = 1000,
                hostedSurface = "payment_element",
                currency = "usd"
            ),
            requestOptions = DEFAULT_OPTIONS,
        )

        verify(stripeNetworkClient).executeRequest(apiRequestArgumentCaptor.capture())
        val params = requireNotNull(apiRequestArgumentCaptor.firstValue.params)

        assertThat(params["link_mode"]).isEqualTo("LINK_DISABLED")
    }

    @Test
    fun `sharePaymentDetails() sends all parameters`() =
        runTest {
            val stripeResponse = StripeResponse(
                200,
                ConsumerFixtures.PAYMENT_DETAILS_SHARE_JSON.toString(),
                emptyMap()
            )
            whenever(stripeNetworkClient.executeRequest(any<ApiRequest>()))
                .thenReturn(stripeResponse)

            val clientSecret = "secret"
            val id = "csmrpd*AYq4D_sXdAAAAOQ0"
            create().sharePaymentDetails(
                consumerSessionClientSecret = clientSecret,
                id = id,
                requestOptions = DEFAULT_OPTIONS,
                extraParams = mapOf("payment_method_options" to mapOf("card" to mapOf("cvc" to "123")))
            )

            verify(stripeNetworkClient).executeRequest(apiRequestArgumentCaptor.capture())
            val params = requireNotNull(apiRequestArgumentCaptor.firstValue.params)

            with(params) {
                assertThat(this["request_surface"]).isEqualTo("android_payment_element")
                withNestedParams("credentials") {
                    assertThat(this["consumer_session_client_secret"]).isEqualTo(clientSecret)
                }
                assertThat(this["id"]).isEqualTo(id)
                assertThat(this["payment_user_agent"].toString()).startsWith("stripe-android/")
                withNestedParams("payment_method_options") {
                    withNestedParams("card") {
                        assertThat(this["cvc"]).isEqualTo("123")
                    }
                }
            }
        }

    @Test
    fun `attachLinkAccountSessionToPaymentIntent attaches LAS to PI`() =
        runTest {
            val stripeResponse = StripeResponse(
                200,
                """
                    {
                        "id": "pi_12345",
                        "object": "payment_intent",
                        "amount": 100,
                        "currency": "usd",
                        "cancellation_reason": null,
                        "client_secret": "pi_abc_secret_def",
                        "created": 1647000000,
                        "description": null,
                        "last_setup_error": null,
                        "livemode": false,
                        "next_action": null,
                        "payment_method": "pm_abcdefg",
                        "payment_method_options": {
                            "us_bank_account": {
                                "verification_method": "instant"
                            }
                        },
                        "payment_method_types": [
                            "us_bank_account"
                        ],
                        "status": "requires_payment_method"
                    }
                """.trimIndent(),
                emptyMap()
            )
            whenever(stripeNetworkClient.executeRequest(any<ApiRequest>()))
                .thenReturn(stripeResponse)

            val clientSecret = "pi_client_secret_123"
            val response = create().attachFinancialConnectionsSessionToPaymentIntent(
                clientSecret = clientSecret,
                paymentIntentId = "pi_12345",
                financialConnectionsSessionId = "las_123456",
                requestOptions = DEFAULT_OPTIONS,
                expandFields = listOf("payment_method")
            ).getOrThrow()

            verify(stripeNetworkClient).executeRequest(
                argWhere<ApiRequest> {
                    it.params?.get("client_secret") == clientSecret
                }
            )

            assertThat(response.paymentMethodId).isEqualTo("pm_abcdefg")
        }

    @Test
    fun `attachFinancialConnectionsSessionToSetupIntent attaches LAS to SI`() =
        runTest {
            val stripeResponse = StripeResponse(
                200,
                """
                    {
                        "id": "seti_12345",
                        "object": "setup_intent",
                        "cancellation_reason": null,
                        "client_secret": "seti_abc_secret_def",
                        "created": 1647000000,
                        "description": null,
                        "last_setup_error": null,
                        "livemode": false,
                        "next_action": null,
                        "payment_method": "pm_abcdefg",
                        "payment_method_options": {
                            "us_bank_account": {
                                "verification_method": "instant"
                            }
                        },
                        "payment_method_types": [
                            "us_bank_account"
                        ],
                        "status": "requires_confirmation",
                        "usage": "off_session"
                    }
                """.trimIndent(),
                emptyMap()
            )
            whenever(stripeNetworkClient.executeRequest(any<ApiRequest>()))
                .thenReturn(stripeResponse)

            val clientSecret = "si_client_secret_123"
            val response = create().attachFinancialConnectionsSessionToSetupIntent(
                clientSecret = clientSecret,
                setupIntentId = "si_12345",
                financialConnectionsSessionId = "las_123456",
                requestOptions = DEFAULT_OPTIONS,
                expandFields = listOf("payment_method")
            ).getOrThrow()

            verify(stripeNetworkClient).executeRequest(
                argWhere<ApiRequest> {
                    it.params?.get("client_secret") == clientSecret
                }
            )

            assertThat(response.paymentMethodId).isEqualTo("pm_abcdefg")
        }

    @Test
    fun `paymentIntentsFinancialConnectionsSession() for ACH sends all parameters`() = runTest {
        val stripeResponse = StripeResponse(
            200,
            PaymentIntentFixtures.PI_LINK_ACCOUNT_SESSION_JSON.toString(),
            emptyMap()
        )
        whenever(stripeNetworkClient.executeRequest(any<ApiRequest>()))
            .thenReturn(stripeResponse)

        val clientSecret = "pi_1234_secret_5678"
        val id = "pi_1234"
        val customerName = "John Doe"
        val customerEmailAddress = "johndoe@gmail.com"
        create().createPaymentIntentFinancialConnectionsSession(
            paymentIntentId = id,
            params = CreateFinancialConnectionsSessionParams.USBankAccount(
                clientSecret = clientSecret,
                customerName = customerName,
                hostedSurface = "payment_element",
                customerEmailAddress = customerEmailAddress,
                linkMode = LinkMode.Passthrough,
            ),
            DEFAULT_OPTIONS
        )

        verify(stripeNetworkClient).executeRequest(apiRequestArgumentCaptor.capture())
        val request = apiRequestArgumentCaptor.firstValue
        val params = requireNotNull(request.params)

        assertThat(request.baseUrl)
            .isEqualTo("https://api.stripe.com/v1/payment_intents/pi_1234/link_account_sessions")

        with(params) {
            assertThat(this["client_secret"]).isEqualTo(clientSecret)
            assertThat(this["hosted_surface"]).isEqualTo("payment_element")
            assertThat(this["link_mode"]).isEqualTo("PASSTHROUGH")
            withNestedParams("payment_method_data") {
                assertThat(this["type"]).isEqualTo("us_bank_account")
                withNestedParams("billing_details") {
                    assertThat(this["name"]).isEqualTo(customerName)
                }
            }
        }
    }

    @Test
    fun `paymentIntentsFinancialConnectionsSession() for Instant Debits sends all parameters`() = runTest {
        val stripeResponse = StripeResponse(
            200,
            PaymentIntentFixtures.PI_LINK_ACCOUNT_SESSION_JSON.toString(),
            emptyMap()
        )
        whenever(stripeNetworkClient.executeRequest(any<ApiRequest>()))
            .thenReturn(stripeResponse)

        val clientSecret = "pi_1234_secret_5678"
        val id = "pi_1234"
        val customerEmailAddress = "johndoe@gmail.com"
        create().createPaymentIntentFinancialConnectionsSession(
            paymentIntentId = id,
            params = CreateFinancialConnectionsSessionParams.InstantDebits(
                clientSecret = clientSecret,
                customerEmailAddress = customerEmailAddress,
                hostedSurface = "payment_element",
                linkMode = LinkMode.LinkCardBrand,
            ),
            DEFAULT_OPTIONS
        )

        verify(stripeNetworkClient).executeRequest(apiRequestArgumentCaptor.capture())
        val request = apiRequestArgumentCaptor.firstValue
        val params = requireNotNull(request.params)

        assertThat(request.baseUrl)
            .isEqualTo("https://api.stripe.com/v1/payment_intents/pi_1234/link_account_sessions")

        with(params) {
            assertThat(this["client_secret"]).isEqualTo(clientSecret)
            assertThat(this["product"]).isEqualTo("instant_debits")
            assertThat(this["hosted_surface"]).isEqualTo("payment_element")
            assertThat(this["attach_required"]).isEqualTo(true)
            assertThat(this["link_mode"]).isEqualTo("LINK_CARD_BRAND")
            withNestedParams("payment_method_data") {
                assertThat(this["type"]).isEqualTo("link")
                withNestedParams("billing_details") {
                    assertThat(this["email"]).isEqualTo(customerEmailAddress)
                }
            }
        }
    }

    @Test
    fun `setupIntentsFinancialConnectionsSession() for ACH sends all parameters`() = runTest {
        val stripeResponse = StripeResponse(
            200,
            PaymentIntentFixtures.SI_LINK_ACCOUNT_SESSION_JSON.toString(),
            emptyMap()
        )
        whenever(stripeNetworkClient.executeRequest(any<ApiRequest>()))
            .thenReturn(stripeResponse)

        val clientSecret = "seti_1234_secret_5678"
        val id = "seti_1234"
        val customerName = "John Doe"
        val customerEmailAddress = "johndoe@gmail.com"
        create().createSetupIntentFinancialConnectionsSession(
            setupIntentId = id,
            params = CreateFinancialConnectionsSessionParams.USBankAccount(
                clientSecret = clientSecret,
                customerName = customerName,
                hostedSurface = "payment_element",
                customerEmailAddress = customerEmailAddress,
                linkMode = null,
            ),
            DEFAULT_OPTIONS
        )

        verify(stripeNetworkClient).executeRequest(apiRequestArgumentCaptor.capture())

        val request = apiRequestArgumentCaptor.firstValue
        val params = requireNotNull(request.params)

        assertThat(request.baseUrl)
            .isEqualTo("https://api.stripe.com/v1/setup_intents/seti_1234/link_account_sessions")

        with(params) {
            assertThat(this["client_secret"]).isEqualTo(clientSecret)
            assertThat(this["hosted_surface"]).isEqualTo("payment_element")
            assertThat(this["link_mode"]).isEqualTo("LINK_DISABLED")
            withNestedParams("payment_method_data") {
                assertThat(this["type"]).isEqualTo("us_bank_account")
                withNestedParams("billing_details") {
                    assertThat(this["name"]).isEqualTo(customerName)
                }
            }
        }
    }

    @Test
    fun `setupIntentsFinancialConnectionsSession() for Instant Debits sends all parameters`() = runTest {
        val stripeResponse = StripeResponse(
            200,
            PaymentIntentFixtures.SI_LINK_ACCOUNT_SESSION_JSON.toString(),
            emptyMap()
        )
        whenever(stripeNetworkClient.executeRequest(any<ApiRequest>()))
            .thenReturn(stripeResponse)

        val clientSecret = "seti_1234_secret_5678"
        val id = "seti_1234"
        val customerEmailAddress = "johndoe@gmail.com"
        create().createSetupIntentFinancialConnectionsSession(
            setupIntentId = id,
            params = CreateFinancialConnectionsSessionParams.InstantDebits(
                clientSecret = clientSecret,
                customerEmailAddress = customerEmailAddress,
                hostedSurface = "payment_element",
                linkMode = null,
            ),
            DEFAULT_OPTIONS
        )

        verify(stripeNetworkClient).executeRequest(apiRequestArgumentCaptor.capture())

        val request = apiRequestArgumentCaptor.firstValue
        val params = requireNotNull(request.params)

        assertThat(request.baseUrl)
            .isEqualTo("https://api.stripe.com/v1/setup_intents/seti_1234/link_account_sessions")

        with(params) {
            assertThat(this["client_secret"]).isEqualTo(clientSecret)
            assertThat(this["product"]).isEqualTo("instant_debits")
            assertThat(this["hosted_surface"]).isEqualTo("payment_element")
            assertThat(this["attach_required"]).isEqualTo(true)
            assertThat(this["link_mode"]).isEqualTo("LINK_DISABLED")
            withNestedParams("payment_method_data") {
                assertThat(this["type"]).isEqualTo("link")
                withNestedParams("billing_details") {
                    assertThat(this["email"]).isEqualTo(customerEmailAddress)
                }
            }
        }
    }

    @Test
    fun `verifyPaymentIntentWithMicrodeposits() with amounts sends all parameters`() = runTest {
        val stripeResponse = StripeResponse(
            200,
            PaymentIntentFixtures.PI_WITH_US_BANK_ACCOUNT_VERIFY_COMPLETED_JSON.toString(),
            emptyMap()
        )
        whenever(stripeNetworkClient.executeRequest(any<ApiRequest>()))
            .thenReturn(stripeResponse)

        val clientSecret =
            PaymentIntentFixtures.PI_WITH_US_BANK_ACCOUNT_VERIFY_COMPLETED.clientSecret!!
        create().verifyPaymentIntentWithMicrodeposits(
            clientSecret = clientSecret,
            firstAmount = 12,
            secondAmount = 34,
            DEFAULT_OPTIONS
        )

        verify(stripeNetworkClient).executeRequest(apiRequestArgumentCaptor.capture())

        val request = apiRequestArgumentCaptor.firstValue
        val params = requireNotNull(request.params)

        assertThat(request.baseUrl)
            .isEqualTo(
                "https://api.stripe.com/v1/payment_intents/" +
                    PaymentIntent.ClientSecret(clientSecret).paymentIntentId +
                    "/verify_microdeposits"
            )

        with(params) {
            assertThat(this["client_secret"]).isEqualTo(clientSecret)
            assertThat(this["amounts"]).isEqualTo(listOf(12, 34))
        }
    }

    @Test
    fun `verifyPaymentIntentWithMicrodeposits() with descriptorCode sends all parameters`() =
        runTest {
            val stripeResponse = StripeResponse(
                200,
                PaymentIntentFixtures.PI_WITH_US_BANK_ACCOUNT_VERIFY_COMPLETED_JSON.toString(),
                emptyMap()
            )
            whenever(stripeNetworkClient.executeRequest(any<ApiRequest>()))
                .thenReturn(stripeResponse)

            val clientSecret =
                PaymentIntentFixtures.PI_WITH_US_BANK_ACCOUNT_VERIFY_COMPLETED.clientSecret!!
            create().verifyPaymentIntentWithMicrodeposits(
                clientSecret = clientSecret,
                descriptorCode = "some_description",
                DEFAULT_OPTIONS
            )

            verify(stripeNetworkClient).executeRequest(apiRequestArgumentCaptor.capture())

            val request = apiRequestArgumentCaptor.firstValue
            val params = requireNotNull(request.params)

            assertThat(request.baseUrl)
                .isEqualTo(
                    "https://api.stripe.com/v1/payment_intents/" +
                        PaymentIntent.ClientSecret(clientSecret).paymentIntentId +
                        "/verify_microdeposits"
                )

            with(params) {
                assertThat(this["client_secret"]).isEqualTo(clientSecret)
                assertThat(this["descriptor_code"]).isEqualTo("some_description")
            }
        }

    @Test
    fun `verifySetupIntentWithMicrodeposits() with amounts sends all parameters`() = runTest {
        val stripeResponse = StripeResponse(
            200,
            SetupIntentFixtures.SI_WITH_US_BANK_ACCOUNT_VERIFY_COMPLETED_JSON.toString(),
            emptyMap()
        )
        whenever(stripeNetworkClient.executeRequest(any<ApiRequest>()))
            .thenReturn(stripeResponse)

        val clientSecret =
            SetupIntentFixtures.SI_WITH_US_BANK_ACCOUNT_VERIFY_COMPLETED.clientSecret!!
        create().verifySetupIntentWithMicrodeposits(
            clientSecret = clientSecret,
            firstAmount = 12,
            secondAmount = 34,
            DEFAULT_OPTIONS
        )

        verify(stripeNetworkClient).executeRequest(apiRequestArgumentCaptor.capture())

        val request = apiRequestArgumentCaptor.firstValue
        val params = requireNotNull(request.params)

        assertThat(
            request.baseUrl
        ).isEqualTo(
            "https://api.stripe.com/v1/setup_intents/" +
                SetupIntent.ClientSecret(clientSecret).setupIntentId +
                "/verify_microdeposits"
        )

        with(params) {
            assertThat(this["client_secret"]).isEqualTo(clientSecret)
            assertThat(this["amounts"]).isEqualTo(listOf(12, 34))
        }
    }

    @Test
    fun `verifySetupIntentWithMicrodeposits() with descriptorCode sends all parameters`() =
        runTest {
            val stripeResponse = StripeResponse(
                200,
                SetupIntentFixtures.SI_WITH_US_BANK_ACCOUNT_VERIFY_COMPLETED_JSON.toString(),
                emptyMap()
            )
            whenever(stripeNetworkClient.executeRequest(any<ApiRequest>()))
                .thenReturn(stripeResponse)

            val clientSecret =
                SetupIntentFixtures.SI_WITH_US_BANK_ACCOUNT_VERIFY_COMPLETED.clientSecret!!
            create().verifySetupIntentWithMicrodeposits(
                clientSecret = clientSecret,
                descriptorCode = "some_description",
                DEFAULT_OPTIONS
            )

            verify(stripeNetworkClient).executeRequest(apiRequestArgumentCaptor.capture())

            val request = apiRequestArgumentCaptor.firstValue
            val params = requireNotNull(request.params)

            assertThat(request.baseUrl)
                .isEqualTo(
                    "https://api.stripe.com/v1/setup_intents/" +
                        SetupIntent.ClientSecret(clientSecret).setupIntentId +
                        "/verify_microdeposits"
                )

            with(params) {
                assertThat(this["client_secret"]).isEqualTo(clientSecret)
                assertThat(this["descriptor_code"]).isEqualTo("some_description")
            }
        }

    @Test
    fun `getPaymentMethodMessaging() returns PaymentMethodMessage`() =
        runTest {
            val stripeResponse = StripeResponse(
                200,
                PaymentMethodMessageFixtures.DEFAULT,
                emptyMap()
            )
            whenever(stripeNetworkClient.executeRequest(any<ApiRequest>()))
                .thenReturn(stripeResponse)

            create().retrievePaymentMethodMessage(
                paymentMethods = listOf("klarna", "afterpay"),
                amount = 999,
                currency = "usd",
                country = "us",
                locale = Locale.getDefault().toLanguageTag(),
                logoColor = "color",
                requestOptions = DEFAULT_OPTIONS
            )

            verify(stripeNetworkClient).executeRequest(apiRequestArgumentCaptor.capture())

            val request = apiRequestArgumentCaptor.firstValue
            val params = requireNotNull(request.params)

            assertThat(request.baseUrl).isEqualTo("https://ppm.stripe.com/content")

            with(params) {
                assertThat(this["payment_methods[0]"]).isEqualTo("klarna")
                assertThat(this["payment_methods[1]"]).isEqualTo("afterpay")
                assertThat(this["amount"]).isEqualTo(999)
                assertThat(this["currency"]).isEqualTo("usd")
                assertThat(this["country"]).isEqualTo("us")
                assertThat(this["locale"]).isEqualTo("en-US")
                assertThat(this["logo_color"]).isEqualTo("color")
            }
        }

    @Test
    fun `Verify that the elements session endpoint has the right query params for payment intents`() = runTest {
        val stripeResponse = StripeResponse(
            200,
            ElementsSessionFixtures.DEFERRED_INTENT_JSON.toString(),
            emptyMap()
        )
        whenever(stripeNetworkClient.executeRequest(any<ApiRequest>()))
            .thenReturn(stripeResponse)

        create().retrieveElementsSession(
            params = ElementsSessionParams.PaymentIntentType(
                clientSecret = "client_secret",
                externalPaymentMethods = emptyList(),
            ),
            options = DEFAULT_OPTIONS,
        )

        verify(stripeNetworkClient).executeRequest(apiRequestArgumentCaptor.capture())

        val request = apiRequestArgumentCaptor.firstValue
        val params = requireNotNull(request.params)

        assertThat(request.baseUrl).isEqualTo("https://api.stripe.com/v1/elements/sessions")

        with(params) {
            assertThat(this["type"]).isEqualTo("payment_intent")
            assertThat(this["locale"]).isEqualTo("en-US")
            assertThat(this["client_secret"]).isEqualTo("client_secret")
        }
    }

    @Test
    fun `Verify that the elements session endpoint has the right query params for external payment methods`() =
        runTest {
            val externalPaymentMethods = listOf("external_paypal", "external_fawry")
            val stripeResponse = StripeResponse(
                200,
                ElementsSessionFixtures.DEFERRED_INTENT_JSON.toString(),
                emptyMap()
            )
            whenever(stripeNetworkClient.executeRequest(any<ApiRequest>())).thenReturn(stripeResponse)

            create().retrieveElementsSession(
                params = ElementsSessionParams.PaymentIntentType(
                    clientSecret = "client_secret",
                    externalPaymentMethods = externalPaymentMethods,
                ),
                options = DEFAULT_OPTIONS,
            )

            verify(stripeNetworkClient).executeRequest(apiRequestArgumentCaptor.capture())

            val request = apiRequestArgumentCaptor.firstValue
            val params = requireNotNull(request.params)

            assertThat(request.baseUrl).isEqualTo("https://api.stripe.com/v1/elements/sessions")

            with(params) {
                assertThat(this["type"]).isEqualTo("payment_intent")
                assertThat(this["locale"]).isEqualTo("en-US")
                assertThat(this["client_secret"]).isEqualTo("client_secret")
                assertThat(this["external_payment_methods"]).isEqualTo(externalPaymentMethods)
            }
        }

    @Test
    fun `Verify external payment methods not in params if there are no EPMs`() =
        runTest {
            val externalPaymentMethods = emptyList<String>()
            val stripeResponse = StripeResponse(
                200,
                ElementsSessionFixtures.DEFERRED_INTENT_JSON.toString(),
                emptyMap()
            )
            whenever(stripeNetworkClient.executeRequest(any<ApiRequest>())).thenReturn(stripeResponse)

            create().retrieveElementsSession(
                params = ElementsSessionParams.PaymentIntentType(
                    clientSecret = "client_secret",
                    externalPaymentMethods = externalPaymentMethods,
                ),
                options = DEFAULT_OPTIONS,
            )

            verify(stripeNetworkClient).executeRequest(apiRequestArgumentCaptor.capture())

            val request = apiRequestArgumentCaptor.firstValue
            val params = requireNotNull(request.params)

            assertThat(request.baseUrl).isEqualTo("https://api.stripe.com/v1/elements/sessions")

            with(params) {
                assertThat(this["type"]).isEqualTo("payment_intent")
                assertThat(this["locale"]).isEqualTo("en-US")
                assertThat(this["client_secret"]).isEqualTo("client_secret")
                assertThat(this["external_payment_methods"]).isNull()
            }
        }

    @Test
    fun `Verify customer session client secret not in params when null`() = runTest {
        val stripeResponse = StripeResponse(
            200,
            ElementsSessionFixtures.EXPANDED_PAYMENT_INTENT_JSON.toString(),
            emptyMap()
        )

        whenever(stripeNetworkClient.executeRequest(any<ApiRequest>())).thenReturn(stripeResponse)

        create().retrieveElementsSession(
            params = ElementsSessionParams.PaymentIntentType(
                clientSecret = "client_secret",
                customerSessionClientSecret = null,
                externalPaymentMethods = emptyList(),
            ),
            options = DEFAULT_OPTIONS,
        )

        verify(stripeNetworkClient).executeRequest(apiRequestArgumentCaptor.capture())

        val request = apiRequestArgumentCaptor.firstValue
        val params = requireNotNull(request.params)

        assertThat(request.baseUrl).isEqualTo("https://api.stripe.com/v1/elements/sessions")

        with(params) {
            assertThat(this["type"]).isEqualTo("payment_intent")
            assertThat(this["locale"]).isEqualTo("en-US")
            assertThat(this["customer_session_client_secret"]).isNull()
        }
    }

    @Test
    fun `Verify customer session client secret in params when provided`() = runTest {
        val stripeResponse = StripeResponse(
            200,
            ElementsSessionFixtures.EXPANDED_PAYMENT_INTENT_JSON.toString(),
            emptyMap()
        )

        whenever(stripeNetworkClient.executeRequest(any<ApiRequest>())).thenReturn(stripeResponse)

        create().retrieveElementsSession(
            params = ElementsSessionParams.PaymentIntentType(
                clientSecret = "client_secret",
                customerSessionClientSecret = "customer_session_client_secret",
                externalPaymentMethods = emptyList(),
            ),
            options = DEFAULT_OPTIONS,
        )

        verify(stripeNetworkClient).executeRequest(apiRequestArgumentCaptor.capture())

        val request = apiRequestArgumentCaptor.firstValue
        val params = requireNotNull(request.params)

        assertThat(request.baseUrl).isEqualTo("https://api.stripe.com/v1/elements/sessions")

        with(params) {
            assertThat(this["type"]).isEqualTo("payment_intent")
            assertThat(this["locale"]).isEqualTo("en-US")
            assertThat(this["customer_session_client_secret"]).isEqualTo("customer_session_client_secret")
        }
    }

    @Test
    fun `Verify that the elements session endpoint has the right query params for setup intents`() = runTest {
        val stripeResponse = StripeResponse(
            200,
            ElementsSessionFixtures.DEFERRED_INTENT_JSON.toString(),
            emptyMap()
        )
        whenever(stripeNetworkClient.executeRequest(any<ApiRequest>()))
            .thenReturn(stripeResponse)

        create().retrieveElementsSession(
            params = ElementsSessionParams.SetupIntentType(
                clientSecret = "client_secret",
                externalPaymentMethods = emptyList(),
            ),
            options = DEFAULT_OPTIONS,
        )

        verify(stripeNetworkClient).executeRequest(apiRequestArgumentCaptor.capture())

        val request = apiRequestArgumentCaptor.firstValue
        val params = requireNotNull(request.params)

        assertThat(request.baseUrl)
            .isEqualTo("https://api.stripe.com/v1/elements/sessions")

        with(params) {
            assertThat(this["type"]).isEqualTo("setup_intent")
            assertThat(this["locale"]).isEqualTo("en-US")
            assertThat(this["client_secret"]).isEqualTo("client_secret")
        }
    }

    @Test
    fun `Verify 'client_default_payment_method' is in params when provided`() = runTest {
        val stripeResponse = StripeResponse(
            200,
            ElementsSessionFixtures.EXPANDED_PAYMENT_INTENT_JSON.toString(),
            emptyMap()
        )

        whenever(stripeNetworkClient.executeRequest(any<ApiRequest>())).thenReturn(stripeResponse)

        create().retrieveElementsSession(
            params = ElementsSessionParams.PaymentIntentType(
                clientSecret = "client_secret",
                externalPaymentMethods = emptyList(),
                defaultPaymentMethodId = "pm_123",
            ),
            options = DEFAULT_OPTIONS,
        )

        verify(stripeNetworkClient).executeRequest(apiRequestArgumentCaptor.capture())

        val request = apiRequestArgumentCaptor.firstValue
        val params = requireNotNull(request.params)

        assertThat(request.baseUrl).isEqualTo("https://api.stripe.com/v1/elements/sessions")

        with(params) {
            assertThat(this["type"]).isEqualTo("payment_intent")
            assertThat(this["locale"]).isEqualTo("en-US")
            assertThat(this["client_default_payment_method"]).isEqualTo("pm_123")
        }
    }

    @Test
    fun `Verify 'client_default_payment_method' not in params when not provided`() = runTest {
        val stripeResponse = StripeResponse(
            200,
            ElementsSessionFixtures.EXPANDED_PAYMENT_INTENT_JSON.toString(),
            emptyMap()
        )

        whenever(stripeNetworkClient.executeRequest(any<ApiRequest>())).thenReturn(stripeResponse)

        create().retrieveElementsSession(
            params = ElementsSessionParams.PaymentIntentType(
                clientSecret = "client_secret",
                defaultPaymentMethodId = null,
                externalPaymentMethods = emptyList(),
            ),
            options = DEFAULT_OPTIONS,
        )

        verify(stripeNetworkClient).executeRequest(apiRequestArgumentCaptor.capture())

        val request = apiRequestArgumentCaptor.firstValue
        val params = requireNotNull(request.params)

        assertThat(request.baseUrl).isEqualTo("https://api.stripe.com/v1/elements/sessions")

        with(params) {
            assertThat(this["type"]).isEqualTo("payment_intent")
            assertThat(this["locale"]).isEqualTo("en-US")
            assertThat(this["client_default_payment_method"]).isNull()
        }
    }

    @Test
    fun `Verify that the elements session endpoint has the right query params for deferred intents`() = runTest {
        val stripeResponse = StripeResponse(
            200,
            ElementsSessionFixtures.DEFERRED_INTENT_JSON.toString(),
            emptyMap()
        )
        whenever(stripeNetworkClient.executeRequest(any<ApiRequest>()))
            .thenReturn(stripeResponse)

        create().retrieveElementsSession(
            params = ElementsSessionParams.DeferredIntentType(
                deferredIntentParams = DeferredIntentParams(
                    mode = DeferredIntentParams.Mode.Payment(
                        amount = 2000,
                        currency = "usd",
                        captureMethod = PaymentIntent.CaptureMethod.Automatic,
                        setupFutureUsage = null,
                    ),
                    paymentMethodTypes = listOf("card", "link"),
                    paymentMethodConfigurationId = "pmc_234",
                    onBehalfOf = null,
                ),
                externalPaymentMethods = emptyList(),
            ),
            options = DEFAULT_OPTIONS,
        )

        verify(stripeNetworkClient).executeRequest(apiRequestArgumentCaptor.capture())

        val request = apiRequestArgumentCaptor.firstValue
        val params = requireNotNull(request.params)

        assertThat(request.baseUrl)
            .isEqualTo("https://api.stripe.com/v1/elements/sessions")

        with(params) {
            assertThat(this["type"]).isEqualTo("deferred_intent")
            assertThat(this["locale"]).isEqualTo("en-US")
            assertThat(this["deferred_intent[mode]"]).isEqualTo("payment")
            assertThat(this["deferred_intent[amount]"]).isEqualTo(2000L)
            assertThat(this["deferred_intent[currency]"]).isEqualTo("usd")
            assertThat(this["deferred_intent[setup_future_usage]"]).isNull()
            assertThat(this["deferred_intent[capture_method]"]).isEqualTo("automatic")
            assertThat(this["deferred_intent[payment_method_types][0]"]).isEqualTo("card")
            assertThat(this["deferred_intent[payment_method_types][1]"]).isEqualTo("link")
            assertThat(this["deferred_intent[payment_method_configuration][id]"]).isEqualTo("pmc_234")
        }
    }

    @Test
    fun `Verify that retrieveCardMetadata returns failure for BINs that are too short`() = runTest {
        val repository = create()

        val exception = repository.retrieveCardMetadata(
            cardNumber = "4242 4",
            requestOptions = DEFAULT_OPTIONS,
        ).exceptionOrNull()

        assertThat(exception).isInstanceOf(InvalidRequestException::class.java)
    }

    @Test
    fun `Verify that the payment method options are persisted for dashboard card payments`() = runTest {
        // Dashboard payments create a payment first
        whenever(stripeNetworkClient.executeRequest(any<ApiRequest>()))
            .thenReturn(
                StripeResponse(
                    200,
                    PaymentMethodFixtures.CARD_JSON.toString(),
                    emptyMap()
                )
            )

        val confirmPaymentIntentParams =
            ConfirmPaymentIntentParams.createWithPaymentMethodCreateParams(
                paymentMethodCreateParams = PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
                clientSecret = "pi_12345_secret_fake",
                paymentMethodOptions = PaymentMethodOptionsParams.Card(
                    setupFutureUsage = ConfirmPaymentIntentParams.SetupFutureUsage.OffSession,
                    moto = true
                )
            )

        // Dashboard uses user key
        create().confirmPaymentIntent(
            confirmPaymentIntentParams = confirmPaymentIntentParams,
            options = DEFAULT_OPTIONS.copy(
                apiKey = "uk_12345"
            )
        )

        // Once to create the payment method and once for confirming the payment intent
        verify(stripeNetworkClient, times(2))
            .executeRequest(apiRequestArgumentCaptor.capture())

        val request = apiRequestArgumentCaptor.secondValue
        val params = requireNotNull(request.params)

        with(params) {
            withNestedParams("payment_method_options") {
                withNestedParams("card") {
                    assertThat(this["moto"]).isEqualTo(true)
                    assertThat(this["setup_future_usage"]).isEqualTo("off_session")
                }
            }
        }
    }

    /**
     * Helper DSL to validate nested params.
     */
    private fun Map<*, *>.withNestedParams(key: String, nestedParams: Map<*, *>.() -> Unit) {
        nestedParams(this[key] as Map<*, *>)
    }

    private fun verifyFraudDetectionDataAndAnalyticsRequests(
        event: PaymentAnalyticsEvent,
        productUsage: List<String>? = null
    ) {
        verify(fraudDetectionDataRepository, times(2))
            .refresh()

        verifyAnalyticsRequest(event, productUsage)
    }

    private fun verifyAnalyticsRequest(
        event: PaymentAnalyticsEvent,
        productUsage: List<String>? = null,
        errorMessage: String? = null
    ) {
        verify(analyticsRequestExecutor)
            .executeAsync(analyticsRequestArgumentCaptor.capture())

        val analyticsRequest = analyticsRequestArgumentCaptor.firstValue
        val analyticsParams = analyticsRequest.params

        assertThat(analyticsParams["event"]).isEqualTo(event.toString())
        assertThat(analyticsParams["product_usage"]).isEqualTo(productUsage)
        assertThat(analyticsParams["error_message"]).isEqualTo(errorMessage)
    }

    private fun create(productUsage: Set<String> = emptySet()): StripeApiRepository {
        return StripeApiRepository(
            context,
            { DEFAULT_OPTIONS.apiKey },
            workContext = testDispatcher,
            productUsageTokens = productUsage,
            stripeNetworkClient = stripeNetworkClient,
            analyticsRequestExecutor = analyticsRequestExecutor,
            fraudDetectionDataRepository = fraudDetectionDataRepository,
            fraudDetectionDataParamsUtils = FraudDetectionDataParamsUtils()
        )
    }

    private companion object {
        private const val STRIPE_ACCOUNT_RESPONSE_HEADER = "Stripe-Account"
        private val CARD_PARAMS =
            CardParams("4242424242424242", 1, 2050, "123")

        private val DEFAULT_OPTIONS = ApiRequest.Options(ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY)

        private val DEFAULT_API_REQUEST_FACTORY = ApiRequest.Factory()
    }
}
