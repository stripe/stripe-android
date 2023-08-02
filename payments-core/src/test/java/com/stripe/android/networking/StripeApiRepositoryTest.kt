package com.stripe.android.networking

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.FakeFraudDetectionDataRepository
import com.stripe.android.FileFactory
import com.stripe.android.FinancialConnectionsFixtures
import com.stripe.android.FraudDetectionDataFixtures
import com.stripe.android.FraudDetectionDataRepository
import com.stripe.android.Stripe
import com.stripe.android.core.exception.APIConnectionException
import com.stripe.android.core.exception.InvalidRequestException
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
import com.stripe.android.model.ConsumerPaymentDetailsCreateParams
import com.stripe.android.model.ConsumerSignUpConsentAction
import com.stripe.android.model.CreateFinancialConnectionsSessionForDeferredPaymentParams
import com.stripe.android.model.CreateFinancialConnectionsSessionParams
import com.stripe.android.model.DeferredIntentParams
import com.stripe.android.model.ElementsSessionFixtures
import com.stripe.android.model.ElementsSessionParams
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
import java.net.HttpURLConnection
import java.net.UnknownHostException
import java.util.Calendar
import java.util.Locale
import java.util.UUID
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

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
            FraudDetectionDataFixtures.create(Calendar.getInstance().timeInMillis)
        )
    }

    @AfterTest
    fun cleanup() {
        Stripe.advancedFraudSignalsEnabled = true
    }

    @Test
    fun testGetApiUrl() {
        val tokensApi = StripeApiRepository.tokensUrl
        assertEquals("https://api.stripe.com/v1/tokens", tokensApi)
    }

    @Test
    fun testGetSourcesUrl() {
        val sourcesUrl = StripeApiRepository.sourcesUrl
        assertEquals("https://api.stripe.com/v1/sources", sourcesUrl)
    }

    @Test
    fun testGetRetrieveSourceUrl() {
        val sourceUrlWithId = StripeApiRepository.getRetrieveSourceApiUrl("abc123")
        assertEquals("https://api.stripe.com/v1/sources/abc123", sourceUrlWithId)
    }

    @Test
    fun testGetRequestTokenApiUrl() {
        val tokenId = "tok_sample"
        val requestApi = StripeApiRepository.getRetrieveTokenApiUrl(tokenId)
        assertEquals("https://api.stripe.com/v1/tokens/$tokenId", requestApi)
    }

    @Test
    fun testGetRetrieveCustomerUrl() {
        val customerId = "cus_123abc"
        val customerRequestUrl = StripeApiRepository.getRetrieveCustomerUrl(customerId)
        assertEquals("https://api.stripe.com/v1/customers/$customerId", customerRequestUrl)
    }

    @Test
    fun testGetAddCustomerSourceUrl() {
        val customerId = "cus_123abc"
        val addSourceUrl = StripeApiRepository.getAddCustomerSourceUrl(customerId)
        assertEquals(
            "https://api.stripe.com/v1/customers/$customerId/sources",
            addSourceUrl
        )
    }

    @Test
    fun testGetDeleteCustomerSourceUrl() {
        val customerId = "cus_123abc"
        val sourceId = "src_456xyz"
        val deleteSourceUrl = StripeApiRepository.getDeleteCustomerSourceUrl(customerId, sourceId)
        assertEquals(
            "https://api.stripe.com/v1/customers/$customerId/sources/$sourceId",
            deleteSourceUrl
        )
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
        assertEquals(expectedUrl, attachUrl)
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
        assertEquals(expectedUrl, detachUrl)
    }

    @Test
    fun testGetPaymentMethodsUrl() {
        assertEquals(
            "https://api.stripe.com/v1/payment_methods",
            StripeApiRepository.paymentMethodsUrl
        )
    }

    @Test
    fun testGetIssuingCardPinUrl() {
        assertEquals(
            "https://api.stripe.com/v1/issuing/cards/card123/pin",
            StripeApiRepository.getIssuingCardPinUrl("card123")
        )
    }

    @Test
    fun testRetrievePaymentIntentUrl() {
        assertEquals(
            "https://api.stripe.com/v1/payment_intents/pi123",
            StripeApiRepository.getRetrievePaymentIntentUrl("pi123")
        )
    }

    @Test
    fun testGetRefreshPaymentIntentUrl() {
        assertEquals(
            "https://api.stripe.com/v1/payment_intents/pi123/refresh",
            StripeApiRepository.getRefreshPaymentIntentUrl("pi123")
        )
    }

    @Test
    fun testConfirmPaymentIntentUrl() {
        assertEquals(
            "https://api.stripe.com/v1/payment_intents/pi123/confirm",
            StripeApiRepository.getConfirmPaymentIntentUrl("pi123")
        )
    }

    @Test
    fun testConsumerSignUpUrl() {
        assertEquals(
            "https://api.stripe.com/v1/consumers/accounts/sign_up",
            StripeApiRepository.consumerSignUpUrl
        )
    }

    @Test
    fun testLogoutConsumerUrl() {
        assertEquals(
            "https://api.stripe.com/v1/consumers/sessions/log_out",
            StripeApiRepository.logoutConsumerUrl
        )
    }

    @Test
    fun testLinkFinancialConnectionsSessionUrlUrl() {
        assertEquals(
            "https://api.stripe.com/v1/consumers/link_account_sessions",
            StripeApiRepository.linkFinancialConnectionsSessionUrl
        )
    }

    @Test
    fun testDeferredFinancialConnectionsSessionUrlUrl() {
        assertEquals(
            "https://api.stripe.com/v1/connections/link_account_sessions_for_deferred_payment",
            StripeApiRepository.deferredFinancialConnectionsSessionUrl
        )
    }

    @Test
    fun testConsumerPaymentDetailsUrl() {
        assertEquals(
            "https://api.stripe.com/v1/consumers/payment_details",
            StripeApiRepository.consumerPaymentDetailsUrl
        )
    }

    @Test
    fun testListConsumerPaymentDetailsUrl() {
        assertEquals(
            "https://api.stripe.com/v1/consumers/payment_details/list",
            StripeApiRepository.listConsumerPaymentDetailsUrl
        )
    }

    @Test
    fun testGetConsumerPaymentDetailsUrl() {
        assertEquals(
            "https://api.stripe.com/v1/consumers/payment_details/csmrpd*123",
            StripeApiRepository.getConsumerPaymentDetailsUrl("csmrpd*123")
        )
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

            assertEquals("source", invalidRequestException.stripeError?.param)
            assertEquals("resource_missing", invalidRequestException.stripeError?.code)
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
            assertEquals("source", invalidRequestException.stripeError?.param)
            assertEquals("resource_missing", invalidRequestException.stripeError?.code)
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
                )
            )

            verify(stripeNetworkClient).executeRequest(apiRequestArgumentCaptor.capture())
            val apiRequest = apiRequestArgumentCaptor.firstValue
            val paymentMethodDataParams =
                apiRequest.params?.get("payment_method_data") as Map<*, *>
            assertTrue(paymentMethodDataParams["muid"] is String)
            assertTrue(paymentMethodDataParams["guid"] is String)
            assertEquals("card", paymentMethodDataParams["type"])

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
                )
            )

            verify(stripeNetworkClient).executeRequest(apiRequestArgumentCaptor.capture())
            val apiRequest = apiRequestArgumentCaptor.firstValue
            val paymentMethodDataParams =
                apiRequest.params?.get(ConfirmStripeIntentParams.PARAM_PAYMENT_METHOD_DATA) as Map<*, *>
            assertThat(paymentMethodDataParams["payment_user_agent"])
                .isEqualTo("stripe-android/${StripeSdkVersion.VERSION_NAME};$productUsage")
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
                )
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
                )
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
                )
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
                ListPaymentMethodsParams(
                    "cus_123",
                    PaymentMethod.Type.Card
                ),
                DEFAULT_OPTIONS.apiKey,
                emptySet(),
                ApiRequest.Options(ApiKeyFixtures.FAKE_EPHEMERAL_KEY)
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
                ListPaymentMethodsParams(
                    "cus_123",
                    PaymentMethod.Type.Card
                ),
                DEFAULT_OPTIONS.apiKey,
                emptySet(),
                ApiRequest.Options(ApiKeyFixtures.FAKE_EPHEMERAL_KEY)
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
            .isEqualTo(26)
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
            .isEqualTo(26)
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
            assertEquals(
                "No such setupintent: 'seti_1CkiBMLENEVhOs7YMtUehLau'",
                ex.stripeError?.message
            )
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
    //            assertEquals(
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
    fun `consumerSignUp() sends all parameters`() =
        runTest {
            val stripeResponse = StripeResponse(
                200,
                ConsumerFixtures.CONSUMER_VERIFIED_JSON.toString(),
                emptyMap()
            )
            whenever(stripeNetworkClient.executeRequest(any<ApiRequest>()))
                .thenReturn(stripeResponse)

            val email = "email@example.com"
            val phoneNumber = "phone number"
            val country = "US"
            val name = "name"
            val locale = Locale.US
            val cookie = "cookie1"
            val consentAction = ConsumerSignUpConsentAction.Button
            create().consumerSignUp(
                email,
                phoneNumber,
                country,
                name,
                locale,
                cookie,
                consentAction,
                DEFAULT_OPTIONS
            )

            verify(stripeNetworkClient).executeRequest(apiRequestArgumentCaptor.capture())
            val params = requireNotNull(apiRequestArgumentCaptor.firstValue.params)

            with(params) {
                assertEquals(this["email_address"], email)
                assertEquals(this["phone_number"], phoneNumber)
                assertEquals(this["country"], country)
                assertEquals(this["legal_name"], name)
                assertEquals(this["locale"], locale.toLanguageTag())
                assertEquals(this["consent_action"], "clicked_button_mobile")
                withNestedParams("cookies") {
                    assertEquals(this["verification_session_client_secrets"], listOf(cookie))
                }
            }
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
                    amount = 1000,
                    currency = "usd"
                ),
                DEFAULT_OPTIONS
            )

            verify(stripeNetworkClient).executeRequest(apiRequestArgumentCaptor.capture())
            val params = requireNotNull(apiRequestArgumentCaptor.firstValue.params)

            with(params) {
                assertEquals("uuid", this["unique_id"])
                assertEquals("initial_institution", this["initial_institution"])
                assertEquals(false, this["manual_entry_only"])
                assertEquals("search_session_id", this["search_session"])
                assertEquals("automatic", this["verification_method"])
                assertEquals("customer_id", this["customer"])
                assertEquals(null, this["on_behalf_of"])
                assertEquals(1000, this["amount"])
                assertEquals("usd", this["currency"])
            }
        }

    @Test
    fun `createPaymentDetails() for card sends all parameters`() =
        runTest {
            val stripeResponse = StripeResponse(
                200,
                ConsumerFixtures.CONSUMER_SINGLE_CARD_PAYMENT_DETAILS_JSON.toString(),
                emptyMap()
            )
            whenever(stripeNetworkClient.executeRequest(any<ApiRequest>()))
                .thenReturn(stripeResponse)

            val clientSecret = "secret"
            val email = "email@stripe.com"
            val paymentDetailsCreateParams = ConsumerPaymentDetailsCreateParams.Card(
                PaymentMethodCreateParamsFixtures.DEFAULT_CARD.toParamMap(),
                email
            )
            create().createPaymentDetails(
                clientSecret,
                paymentDetailsCreateParams,
                DEFAULT_OPTIONS
            )

            verify(stripeNetworkClient).executeRequest(apiRequestArgumentCaptor.capture())
            val params = requireNotNull(apiRequestArgumentCaptor.firstValue.params)

            with(params) {
                assertEquals(this["request_surface"], "android_payment_element")
                withNestedParams("credentials") {
                    assertEquals(this["consumer_session_client_secret"], clientSecret)
                }
                assertEquals(this["active"], false)
                assertEquals(this["type"], "card")
                assertEquals(this["billing_email_address"], email)
                withNestedParams("billing_address") {
                    assertEquals(this["country_code"], "US")
                    assertEquals(this["postal_code"], "94111")
                }
                withNestedParams("card") {
                    assertEquals(this["number"], "4242424242424242")
                    assertEquals(this["exp_month"], 1)
                    assertEquals(this["exp_year"], 2024)
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

            assertEquals("pm_abcdefg", response.paymentMethodId)
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

            assertEquals("pm_abcdefg", response.paymentMethodId)
        }

    @Test
    fun `paymentIntentsFinancialConnectionsSession() sends all parameters`() = runTest {
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
            params = CreateFinancialConnectionsSessionParams(
                clientSecret = clientSecret,
                customerName = customerName,
                customerEmailAddress = customerEmailAddress
            ),
            DEFAULT_OPTIONS
        )

        verify(stripeNetworkClient).executeRequest(apiRequestArgumentCaptor.capture())
        val request = apiRequestArgumentCaptor.firstValue
        val params = requireNotNull(request.params)

        assertEquals(
            "https://api.stripe.com/v1/payment_intents/pi_1234/link_account_sessions",
            request.baseUrl
        )
        with(params) {
            assertEquals(clientSecret, this["client_secret"])
            withNestedParams("payment_method_data") {
                assertEquals("us_bank_account", this["type"])
                withNestedParams("billing_details") {
                    assertEquals(customerName, this["name"])
                }
            }
        }
    }

    @Test
    fun `setupIntentsFinancialConnectionsSession() sends all parameters`() = runTest {
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
            params = CreateFinancialConnectionsSessionParams(
                clientSecret = clientSecret,
                customerName = customerName,
                customerEmailAddress = customerEmailAddress
            ),
            DEFAULT_OPTIONS
        )

        verify(stripeNetworkClient).executeRequest(apiRequestArgumentCaptor.capture())

        val request = apiRequestArgumentCaptor.firstValue
        val params = requireNotNull(request.params)

        assertEquals(
            "https://api.stripe.com/v1/setup_intents/seti_1234/link_account_sessions",
            request.baseUrl
        )

        with(params) {
            assertEquals(clientSecret, this["client_secret"])
            withNestedParams("payment_method_data") {
                assertEquals("us_bank_account", this["type"])
                withNestedParams("billing_details") {
                    assertEquals(customerName, this["name"])
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

        assertEquals(
            "https://api.stripe.com/v1/payment_intents/" +
                PaymentIntent.ClientSecret(clientSecret).paymentIntentId +
                "/verify_microdeposits",
            request.baseUrl
        )

        with(params) {
            assertEquals(clientSecret, this["client_secret"])
            assertEquals(listOf(12, 34), this["amounts"])
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

            assertEquals(
                "https://api.stripe.com/v1/payment_intents/" +
                    PaymentIntent.ClientSecret(clientSecret).paymentIntentId +
                    "/verify_microdeposits",
                request.baseUrl
            )

            with(params) {
                assertEquals(clientSecret, this["client_secret"])
                assertEquals("some_description", this["descriptor_code"])
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

        assertEquals(
            "https://api.stripe.com/v1/setup_intents/" +
                SetupIntent.ClientSecret(clientSecret).setupIntentId +
                "/verify_microdeposits",
            request.baseUrl
        )

        with(params) {
            assertEquals(clientSecret, this["client_secret"])
            assertEquals(listOf(12, 34), this["amounts"])
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

            assertEquals(
                "https://api.stripe.com/v1/setup_intents/" +
                    SetupIntent.ClientSecret(clientSecret).setupIntentId +
                    "/verify_microdeposits",
                request.baseUrl
            )

            with(params) {
                assertEquals(clientSecret, this["client_secret"])
                assertEquals("some_description", this["descriptor_code"])
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

            assertEquals(
                "https://ppm.stripe.com/content",
                request.baseUrl
            )

            with(params) {
                assertEquals("klarna", this["payment_methods[0]"])
                assertEquals("afterpay", this["payment_methods[1]"])
                assertEquals(999, this["amount"])
                assertEquals("usd", this["currency"])
                assertEquals("us", this["country"])
                assertEquals("en-US", this["locale"])
                assertEquals("color", this["logo_color"])
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
            params = ElementsSessionParams.PaymentIntentType(clientSecret = "client_secret"),
            options = DEFAULT_OPTIONS,
        )

        verify(stripeNetworkClient).executeRequest(apiRequestArgumentCaptor.capture())

        val request = apiRequestArgumentCaptor.firstValue
        val params = requireNotNull(request.params)

        assertEquals(
            "https://api.stripe.com/v1/elements/sessions",
            request.baseUrl
        )

        with(params) {
            assertEquals("payment_intent", this["type"])
            assertEquals("en-US", this["locale"])
            assertEquals("client_secret", this["client_secret"])
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
            params = ElementsSessionParams.SetupIntentType(clientSecret = "client_secret"),
            options = DEFAULT_OPTIONS,
        )

        verify(stripeNetworkClient).executeRequest(apiRequestArgumentCaptor.capture())

        val request = apiRequestArgumentCaptor.firstValue
        val params = requireNotNull(request.params)

        assertEquals(
            "https://api.stripe.com/v1/elements/sessions",
            request.baseUrl
        )

        with(params) {
            assertEquals("setup_intent", this["type"])
            assertEquals("en-US", this["locale"])
            assertEquals("client_secret", this["client_secret"])
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
                    onBehalfOf = null,
                )
            ),
            options = DEFAULT_OPTIONS,
        )

        verify(stripeNetworkClient).executeRequest(apiRequestArgumentCaptor.capture())

        val request = apiRequestArgumentCaptor.firstValue
        val params = requireNotNull(request.params)

        assertEquals(
            "https://api.stripe.com/v1/elements/sessions",
            request.baseUrl
        )

        with(params) {
            assertEquals("deferred_intent", this["type"])
            assertEquals("en-US", this["locale"])
            assertEquals("payment", this["deferred_intent[mode]"])
            assertEquals(2000L, this["deferred_intent[amount]"])
            assertEquals("usd", this["deferred_intent[currency]"])
            assertEquals(null, this["deferred_intent[setup_future_usage]"])
            assertEquals("automatic", this["deferred_intent[capture_method]"])
            assertEquals("card", this["deferred_intent[payment_method_types][0]"])
            assertEquals("link", this["deferred_intent[payment_method_types][1]"])
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
                    assertEquals(true, this["moto"])
                    assertEquals("off_session", this["setup_future_usage"])
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
        productUsage: List<String>? = null
    ) {
        verify(analyticsRequestExecutor)
            .executeAsync(analyticsRequestArgumentCaptor.capture())

        val analyticsRequest = analyticsRequestArgumentCaptor.firstValue
        val analyticsParams = analyticsRequest.params
        assertEquals(
            event.toString(),
            analyticsParams["event"]
        )

        assertEquals(
            productUsage,
            analyticsParams["product_usage"]
        )
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
