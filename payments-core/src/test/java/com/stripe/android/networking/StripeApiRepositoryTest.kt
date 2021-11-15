package com.stripe.android.networking

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.FakeFraudDetectionDataRepository
import com.stripe.android.FileFactory
import com.stripe.android.FraudDetectionDataFixtures
import com.stripe.android.FraudDetectionDataRepository
import com.stripe.android.Stripe
import com.stripe.android.core.exception.APIConnectionException
import com.stripe.android.core.exception.InvalidRequestException
import com.stripe.android.core.networking.AnalyticsRequest
import com.stripe.android.core.networking.AnalyticsRequestExecutor
import com.stripe.android.core.networking.DefaultStripeNetworkClient
import com.stripe.android.core.networking.StripeNetworkClient
import com.stripe.android.core.networking.StripeRequest
import com.stripe.android.core.networking.StripeResponse
import com.stripe.android.model.BankAccountTokenParamsFixtures
import com.stripe.android.model.BinFixtures
import com.stripe.android.model.CardParams
import com.stripe.android.model.CardParamsFixtures
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.ConfirmSetupIntentParams
import com.stripe.android.model.ConfirmStripeIntentParams
import com.stripe.android.model.ListPaymentMethodsParams
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodCreateParamsFixtures
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.model.PaymentMethodPreferenceFixtures
import com.stripe.android.model.SetupIntentFixtures
import com.stripe.android.model.SourceFixtures
import com.stripe.android.model.SourceParams
import com.stripe.android.model.Stripe3ds2AuthParams
import com.stripe.android.model.Stripe3ds2Fixtures
import com.stripe.android.model.StripeFileFixtures
import com.stripe.android.model.StripeFileParams
import com.stripe.android.model.StripeFilePurpose
import com.stripe.android.model.TokenFixtures
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.runBlockingTest
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
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@ExperimentalCoroutinesApi
internal class StripeApiRepositoryTest {
    private val testDispatcher = TestCoroutineDispatcher()

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
        testDispatcher.cleanupTestCoroutines()
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
    fun createSource_shouldLogSourceCreation_andReturnSource() = testDispatcher.runBlockingTest {
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
        testDispatcher.runBlockingTest {
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
                    "stripe-android/${Stripe.VERSION_NAME};$productUsage"
                )
        }

    @Test
    fun createCardSource_withAttribution_setsCorrectPaymentUserAgent() =
        testDispatcher.runBlockingTest {
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
                    "stripe-android/${Stripe.VERSION_NAME}" +
                        ";$productUsage;CardInputView"
                )
        }

    @Test
    fun createCardSource_withAttribution_shouldPopulateProductUsage() =
        testDispatcher.runBlockingTest {
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
        testDispatcher.runBlockingTest {
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
    fun createSource_withConnectAccount_keepsHeaderInAccount() = testDispatcher.runBlockingTest {
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
    fun retrieveSource_shouldFireAnalytics_andReturnSource() = testDispatcher.runBlockingTest {
        val stripeResponse = StripeResponse(
            200,
            SourceFixtures.SOURCE_CARD_JSON.toString(),
            emptyMap()
        )
        whenever(stripeNetworkClient.executeRequest(any<ApiRequest>()))
            .thenReturn(stripeResponse)

        val sourceId = "src_19t3xKBZqEXluyI4uz2dxAfQ"
        val source = requireNotNull(
            create().retrieveSource(
                sourceId,
                "mocked",
                DEFAULT_OPTIONS
            )
        )

        assertThat(source.id)
            .isEqualTo(sourceId)

        verifyAnalyticsRequest(PaymentAnalyticsEvent.SourceRetrieve)
    }

    @Test
    fun start3ds2Auth_withInvalidSource_shouldThrowInvalidRequestException() =
        testDispatcher.runBlockingTest {
            val authParams = Stripe3ds2AuthParams(
                "src_invalid",
                "1.0.0",
                "3DS_LOA_SDK_STIN_12345",
                UUID.randomUUID().toString(),
                "eyJlbmMiOiJBMTI4Q0JDLUhTMjU2IiwiYWxnIjoiUlNBLU9BRVAtMjU2In0.nid2Q-Ii21cSPHBaszR5KSXz866yX9I7AthLKpfWZoc7RIfz11UJ1EHuvIRDIyqqJ8txNUKKoL4keqMTqK5Yc5TqsxMn0nML8pZaPn40nXsJm_HFv3zMeOtRR7UTewsDWIgf5J-A6bhowIOmvKPCJRxspn_Cmja-YpgFWTp08uoJvqgntgg1lHmI1kh1UV6DuseYFUfuQlICTqC3TspAzah2CALWZORF_QtSeHc_RuqK02wOQMs-7079jRuSdBXvI6dQnL5ESH25wHHosfjHMZ9vtdUFNJo9J35UI1sdWFDzzj8k7bt0BupZhyeU0PSM9EHP-yv01-MQ9eslPTVNbFJ9YOHtq8WamvlKDr1sKxz6Ac_gUM8NgEcPP9SafPVxDd4H1Fwb5-4NYu2AD4xoAgMWE-YtzvfIFXZcU46NDoi6Xum3cHJqTH0UaOhBoqJJft9XZXYW80fjts-v28TkA76-QPF7CTDM6KbupvBkSoRq218eJLEywySXgCwf-Q95fsBtnnyhKcvfRaByq5kT7PH3DYD1rCQLexJ76A79kurre9pDjTKAv85G9DNkOFuVUYnNB3QGFReCcF9wzkGnZXdfkgN2BkB6n94bbkEyjbRb5r37XH6oRagx2fWLVj7kC5baeIwUPVb5kV_x4Kle7C-FPY1Obz4U7s6SVRnLGXY.IP9OcQx5uZxBRluOpn1m6Q.w-Ko5Qg6r-KCmKnprXEbKA7wV-SdLNDAKqjtuku6hda_0crOPRCPU4nn26Yxj7EG.p01pl8CKukuXzjLeY3a_Ew",
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

            val invalidRequestException =
                assertFailsWith<InvalidRequestException> {
                    stripeApiRepository.start3ds2Auth(
                        authParams,
                        DEFAULT_OPTIONS
                    )
                }

            assertEquals("source", invalidRequestException.stripeError?.param)
            assertEquals("resource_missing", invalidRequestException.stripeError?.code)
        }

    @Test
    fun complete3ds2Auth_withInvalidSource_shouldThrowInvalidRequestException() =
        testDispatcher.runBlockingTest {
            val invalidRequestException =
                assertFailsWith<InvalidRequestException> {
                    stripeApiRepository.complete3ds2Auth(
                        "src_123",
                        DEFAULT_OPTIONS
                    )
                }
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
            AnalyticsRequest(emptyMap<String, String>())
        )
    }

    @Test
    fun requestData_withConnectAccount_shouldReturnCorrectResponseHeaders() =
        testDispatcher.runBlockingTest {
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
        testDispatcher.runBlockingTest {
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
        testDispatcher.runBlockingTest {
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
                .isEqualTo("stripe-android/${Stripe.VERSION_NAME};$productUsage")
        }

    @Test
    fun confirmPaymentIntent_withSourceAttribution_setsCorrectPaymentUserAgent() =
        testDispatcher.runBlockingTest {
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
                    "stripe-android/${Stripe.VERSION_NAME};" +
                        "$stripeApiRequestProductUsage;$sourceParamsProductUsage"
                )
        }

    @Test
    fun confirmPaymentIntent_withPaymentMethodCreateParamsAttribution_setsCorrectPaymentUserAgent() =
        testDispatcher.runBlockingTest {
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
                    "stripe-android/${Stripe.VERSION_NAME};" +
                        "$productUsage;$pmCreateParamsProductUsage"
                )
        }

    @Test
    fun confirmSetupIntent_setsCorrectPaymentUserAgent() =
        testDispatcher.runBlockingTest {
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
                )
            )

            verify(stripeNetworkClient).executeRequest(apiRequestArgumentCaptor.capture())
            val apiRequest = apiRequestArgumentCaptor.firstValue
            val paymentMethodDataParams =
                apiRequest.params?.get(ConfirmStripeIntentParams.PARAM_PAYMENT_METHOD_DATA) as Map<*, *>
            assertThat(paymentMethodDataParams["payment_user_agent"])
                .isEqualTo(
                    "stripe-android/${Stripe.VERSION_NAME};$productUsage"
                )
        }

    @Test
    fun confirmSetupIntent_withPaymentMethodCreateParamsAttribution_setsCorrectPaymentUserAgent() =
        testDispatcher.runBlockingTest {
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
                )
            )

            verify(stripeNetworkClient).executeRequest(apiRequestArgumentCaptor.capture())
            val apiRequest = apiRequestArgumentCaptor.firstValue
            val paymentMethodDataParams =
                apiRequest.params?.get(ConfirmStripeIntentParams.PARAM_PAYMENT_METHOD_DATA) as Map<*, *>
            assertThat(paymentMethodDataParams["payment_user_agent"])
                .isEqualTo(
                    "stripe-android/${Stripe.VERSION_NAME};" +
                        "$productUsage;$pmCreateParamsProductUsage"
                )
        }

    @Ignore("requires a secret key")
    fun disabled_confirmPaymentIntent_withSourceId_canSuccessfulConfirm() =
        testDispatcher.runBlockingTest {
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
        testDispatcher.runBlockingTest {
            val clientSecret = "temporarily put a private key here simulate the backend"
            val publishableKey = "put a public key that matches the private key here"

            requireNotNull(
                stripeApiRepository.retrievePaymentIntent(
                    clientSecret,
                    ApiRequest.Options(publishableKey)
                )
            )
        }

    @Test
    fun createSource_createsObjectAndLogs() = testDispatcher.runBlockingTest {
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
            AnalyticsRequest(emptyMap<String, String>())
        )
        verifyNoMoreInteractions(stripeNetworkClient)
    }

    @Test
    fun getPaymentMethods_whenPopulated_returnsExpectedList() = testDispatcher.runBlockingTest {
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
            )
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
    fun getPaymentMethods_whenNotPopulated_returnsEmptyList() = testDispatcher.runBlockingTest {
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
            )
        assertThat(paymentMethods)
            .isEmpty()
    }

    @Test
    fun getFpxBankStatus_withFpxKey() = testDispatcher.runBlockingTest {
        val fpxBankStatuses = stripeApiRepository.getFpxBankStatus(
            ApiRequest.Options(ApiKeyFixtures.FPX_PUBLISHABLE_KEY)
        )
        assertThat(fpxBankStatuses.size())
            .isEqualTo(25)
    }

    @Test
    fun getFpxBankStatus_withFpxKey_ignoresStripeAccountId() = testDispatcher.runBlockingTest {
        val fpxBankStatuses = stripeApiRepository.getFpxBankStatus(
            ApiRequest.Options(
                apiKey = ApiKeyFixtures.FPX_PUBLISHABLE_KEY,
                stripeAccount = "acct_1234"
            )
        )
        assertThat(fpxBankStatuses.size())
            .isEqualTo(25)
    }

    @Test
    fun `getCardMetadata with valid bin prefix should succeed`() = testDispatcher.runBlockingTest {
        val cardMetadata =
            stripeApiRepository.getCardMetadata(
                BinFixtures.VISA,
                ApiRequest.Options(ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY)
            )
        requireNotNull(cardMetadata)
        assertThat(cardMetadata.bin)
            .isEqualTo(BinFixtures.VISA)
        assertThat(cardMetadata.accountRanges)
            .isNotEmpty()
    }

    @Test
    fun cancelPaymentIntentSource_whenAlreadyCanceled_throwsInvalidRequestException() =
        testDispatcher.runBlockingTest {
            val exception = assertFailsWith<InvalidRequestException> {
                stripeApiRepository.cancelPaymentIntentSource(
                    "pi_1FejpSH8dsfnfKo38L276wr6",
                    "src_1FejpbH8dsfnfKo3KR7EqCzJ",
                    ApiRequest.Options(ApiKeyFixtures.FPX_PUBLISHABLE_KEY)
                )
            }
            assertEquals(
                "This PaymentIntent could be not be fulfilled via this session because a different payment method was attached to it. Another session could be attempting to fulfill this PaymentIntent. Please complete that session or try again.",
                exception.message
            )
            assertEquals("payment_intent_unexpected_state", exception.stripeError?.code)
        }

    @Test
    fun createSource_whenUnknownHostExceptionThrown_convertsToAPIConnectionException() =
        testDispatcher.runBlockingTest {
            whenever(stripeNetworkClient.executeRequest(any<ApiRequest>()))
                .thenAnswer { throw UnknownHostException() }

            assertFailsWith<APIConnectionException> {
                create().createSource(
                    SourceParams.createCardParams(CARD_PARAMS),
                    DEFAULT_OPTIONS
                )
            }
        }

    @Test
    fun createFile_shouldFireExpectedRequests() = testDispatcher.runBlockingTest {
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
    fun apiRequest_withErrorResponse_onUnsupportedSdkVersion_shouldNotBeTranslated() =
        testDispatcher.runBlockingTest {
            Locale.setDefault(Locale.JAPAN)

            val stripeRepository = StripeApiRepository(
                context,
                { DEFAULT_OPTIONS.apiKey },
                workContext = testDispatcher,
                sdkVersion = "AndroidBindings/13.0.0"
            )

            val ex = assertFailsWith<InvalidRequestException> {
                stripeRepository.retrieveSetupIntent(
                    "seti_1CkiBMLENEVhOs7YMtUehLau_secret_invalid",
                    DEFAULT_OPTIONS
                )
            }
            assertEquals(
                "No such setupintent: 'seti_1CkiBMLENEVhOs7YMtUehLau'",
                ex.stripeError?.message
            )
        }

    @Test
    fun apiRequest_withErrorResponse_onSupportedSdkVersion_shouldBeTranslated() =
        testDispatcher.runBlockingTest {
            Locale.setDefault(Locale.JAPAN)

            val stripeRepository = StripeApiRepository(
                context,
                { DEFAULT_OPTIONS.apiKey },
                workContext = testDispatcher,
                sdkVersion = "AndroidBindings/14.0.0"
            )

            val ex = assertFailsWith<InvalidRequestException> {
                stripeRepository.retrieveSetupIntent(
                    "seti_1CkiBMLENEVhOs7YMtUehLau_secret_invalid",
                    DEFAULT_OPTIONS
                )
            }
            assertEquals(
                "そのような setupintent はありません : 'seti_1CkiBMLENEVhOs7YMtUehLau' ",
                ex.stripeError?.message
            )
        }

    @Test
    fun createCardToken_setsCorrectPaymentUserAgent() =
        testDispatcher.runBlockingTest {
            val stripeResponse = StripeResponse(
                200,
                TokenFixtures.CARD_TOKEN_JSON.toString(),
                emptyMap()
            )
            whenever(stripeNetworkClient.executeRequest(any<ApiRequest>())).thenReturn(stripeResponse)

            val productUsage = "TestProductUsage"
            create(setOf(productUsage)).createToken(
                CardParamsFixtures.DEFAULT,
                DEFAULT_OPTIONS
            )

            verify(stripeNetworkClient).executeRequest(apiRequestArgumentCaptor.capture())
            val apiRequest = apiRequestArgumentCaptor.firstValue
            assertThat(apiRequest.params?.get("payment_user_agent"))
                .isEqualTo(
                    "stripe-android/${Stripe.VERSION_NAME};$productUsage"
                )
        }

    @Test
    fun createCardToken_withAttribution_setsCorrectPaymentUserAgent() =
        testDispatcher.runBlockingTest {
            val stripeResponse = StripeResponse(
                200,
                TokenFixtures.CARD_TOKEN_JSON.toString(),
                emptyMap()
            )
            whenever(stripeNetworkClient.executeRequest(any<ApiRequest>())).thenReturn(stripeResponse)

            val productUsage = "TestProductUsage"
            create(setOf(productUsage)).createToken(
                CardParamsFixtures.WITH_ATTRIBUTION,
                DEFAULT_OPTIONS
            )

            verify(stripeNetworkClient).executeRequest(apiRequestArgumentCaptor.capture())
            val apiRequest = apiRequestArgumentCaptor.firstValue
            assertThat(apiRequest.params?.get("payment_user_agent"))
                .isEqualTo(
                    "stripe-android/${Stripe.VERSION_NAME}" +
                        ";$productUsage;CardInputView"
                )
        }

    @Test
    fun createCardToken_withAttribution_shouldPopulateProductUsage() =
        testDispatcher.runBlockingTest {
            val stripeResponse = StripeResponse(
                200,
                TokenFixtures.CARD_TOKEN_JSON.toString(),
                emptyMap()
            )
            whenever(stripeNetworkClient.executeRequest(any<ApiRequest>())).thenReturn(stripeResponse)
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
    fun createBankToken_shouldNotPopulateProductUsage() = testDispatcher.runBlockingTest {
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
        testDispatcher.runBlockingTest {
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
                .isEqualTo("stripe-android/${Stripe.VERSION_NAME};$productUsage")
        }

    @Test
    fun createCardPaymentMethod_withAttribution_setsCorrectPaymentUserAgent() =
        testDispatcher.runBlockingTest {
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
                    "stripe-android/${Stripe.VERSION_NAME}" +
                        ";$productUsage;$cardAttribution"
                )
        }

    @Test
    fun createCardPaymentMethod_withAttribution_shouldPopulateProductUsage() =
        testDispatcher.runBlockingTest {
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
        testDispatcher.runBlockingTest {
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
        testDispatcher.runBlockingTest {
            val stripeResponse = StripeResponse(
                200,
                PaymentMethodFixtures.SEPA_DEBIT_JSON.toString(),
                emptyMap()
            )
            whenever(stripeNetworkClient.executeRequest(any<ApiRequest>())).thenReturn(stripeResponse)
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
        testDispatcher.runBlockingTest {
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
            val radarSession = requireNotNull(
                stripeRepository.createRadarSession(DEFAULT_OPTIONS)
            )
            assertThat(radarSession.id)
                .startsWith("rse_")

            verifyAnalyticsRequest(PaymentAnalyticsEvent.RadarSessionCreate)
        }

    @Test
    fun `createRadarSession() with null FraudDetectionData should throw an exception`() =
        testDispatcher.runBlockingTest {
            val stripeRepository = StripeApiRepository(
                context,
                { DEFAULT_OPTIONS.apiKey },
                fraudDetectionDataRepository = FakeFraudDetectionDataRepository(
                    null
                ),
                workContext = testDispatcher
            )

            val invalidRequestException = assertFailsWith<InvalidRequestException> {
                stripeRepository.createRadarSession(DEFAULT_OPTIONS)
            }
            assertThat(invalidRequestException.message)
                .isEqualTo("Could not obtain fraud data required to create a Radar Session.")
        }

    @Test
    fun `createRadarSession() with advancedFraudSignalsEnabled set to false should throw an exception`() =
        testDispatcher.runBlockingTest {
            verifyNoInteractions(fraudDetectionDataRepository)

            Stripe.advancedFraudSignalsEnabled = false
            val stripeRepository = create()
            val invalidRequestException = assertFailsWith<InvalidRequestException> {
                stripeRepository.createRadarSession(DEFAULT_OPTIONS)
            }
            assertThat(invalidRequestException.message)
                .isEqualTo("Stripe.advancedFraudSignalsEnabled must be set to 'true' to create a Radar Session.")
        }

    @Test
    fun `retrieveStripeIntent() with invalid client secret should throw exception`() =
        testDispatcher.runBlockingTest {
            val error = assertFailsWith<IllegalStateException> {
                stripeApiRepository.retrieveStripeIntent(
                    "invalid!",
                    DEFAULT_OPTIONS
                )
            }
            assertThat(error.message)
                .isEqualTo("Invalid client secret.")
        }

    @Test
    fun `retrievePaymentIntentWithOrderedPaymentMethods() sends all parameters`() =
        testDispatcher.runBlockingTest {
            val stripeResponse = StripeResponse(
                200,
                PaymentMethodPreferenceFixtures.EXPANDED_PAYMENT_INTENT_JSON.toString(),
                emptyMap()
            )
            whenever(stripeNetworkClient.executeRequest(any<ApiRequest>()))
                .thenReturn(stripeResponse)

            val clientSecret = "test_locale"
            val locale = Locale.GERMANY
            create().retrievePaymentIntentWithOrderedPaymentMethods(
                clientSecret,
                DEFAULT_OPTIONS,
                locale
            )

            verify(stripeNetworkClient).executeRequest(apiRequestArgumentCaptor.capture())
            val apiRequest = apiRequestArgumentCaptor.firstValue
            val paymentMethodDataParams = apiRequest.params?.get("expand") as Collection<*>
            assertTrue(paymentMethodDataParams.contains("payment_method_preference.payment_intent.payment_method"))
            assertEquals(apiRequest.params?.get("locale"), locale.toLanguageTag())
            assertEquals(apiRequest.params?.get("type"), "payment_intent")
            assertEquals(apiRequest.params?.get("client_secret"), clientSecret)

            verifyFraudDetectionDataAndAnalyticsRequests(PaymentAnalyticsEvent.PaymentIntentRetrieve)
        }

    @Test
    fun `retrieveSetupIntentWithOrderedPaymentMethods() sends all parameters`() =
        testDispatcher.runBlockingTest {
            val stripeResponse = StripeResponse(
                200,
                PaymentMethodPreferenceFixtures.EXPANDED_SETUP_INTENT_JSON.toString(),
                emptyMap()
            )
            whenever(stripeNetworkClient.executeRequest(any<ApiRequest>()))
                .thenReturn(stripeResponse)

            val clientSecret = "test_client_secret"
            val locale = Locale.FRANCE
            create().retrieveSetupIntentWithOrderedPaymentMethods(
                clientSecret,
                DEFAULT_OPTIONS,
                locale
            )

            verify(stripeNetworkClient).executeRequest(apiRequestArgumentCaptor.capture())
            val apiRequest = apiRequestArgumentCaptor.firstValue
            val paymentMethodDataParams = apiRequest.params?.get("expand") as Collection<*>
            assertTrue(paymentMethodDataParams.contains("payment_method_preference.setup_intent.payment_method"))
            assertEquals(apiRequest.params?.get("locale"), locale.toLanguageTag())
            assertEquals(apiRequest.params?.get("type"), "setup_intent")
            assertEquals(apiRequest.params?.get("client_secret"), clientSecret)

            verifyFraudDetectionDataAndAnalyticsRequests(PaymentAnalyticsEvent.SetupIntentRetrieve)
        }

    @Test
    fun verifyRefreshPaymentIntent() =
        testDispatcher.runBlockingTest {
            val clientSecret = "pi_3JkCxKBNJ02ErVOj0kNqBMAZ_secret_bC6oXqo976LFM06Z9rlhmzUQq"
            whenever(stripeNetworkClient.executeRequest(any<ApiRequest>()))
                .thenReturn(
                    StripeResponse(
                        200,
                        PaymentIntentFixtures.PI_REFRESH_RESPONSE_REQUIRES_WECHAT_PAY_AUTHORIZE_JSON.toString(),
                        emptyMap()
                    )
                )

            requireNotNull(
                create().refreshPaymentIntent(
                    clientSecret,
                    ApiRequest.Options(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY),
                )
            )

            verify(stripeNetworkClient).executeRequest(
                argWhere<ApiRequest> {
                    it.options.apiKey == ApiKeyFixtures.FAKE_PUBLISHABLE_KEY &&
                        it.params?.get("client_secret") == clientSecret
                }
            )

            verifyFraudDetectionDataAndAnalyticsRequests(PaymentAnalyticsEvent.PaymentIntentRefresh)
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
