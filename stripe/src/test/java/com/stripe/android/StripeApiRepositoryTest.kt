package com.stripe.android

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.nhaarman.mockitokotlin2.KArgumentCaptor
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argThat
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.stripe.android.exception.APIConnectionException
import com.stripe.android.exception.APIException
import com.stripe.android.exception.AuthenticationException
import com.stripe.android.exception.CardException
import com.stripe.android.exception.InvalidRequestException
import com.stripe.android.exception.StripeException
import com.stripe.android.model.Card
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParamsFixtures
import com.stripe.android.model.SourceParams
import com.stripe.android.view.FpxBank
import java.io.UnsupportedEncodingException
import java.net.HttpURLConnection
import java.util.Locale
import java.util.UUID
import kotlin.test.BeforeTest
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.times
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner

/**
 * Test class for [StripeApiRepository].
 */
@RunWith(RobolectricTestRunner::class)
class StripeApiRepositoryTest {

    private val stripeApiRepository = StripeApiRepository(ApplicationProvider.getApplicationContext<Context>())

    @Mock
    private lateinit var stripeApiRequestExecutor: ApiRequestExecutor
    @Mock
    private lateinit var fireAndForgetRequestExecutor: FireAndForgetRequestExecutor

    private lateinit var apiRequestArgumentCaptor: KArgumentCaptor<ApiRequest>
    private lateinit var stripeRequestArgumentCaptor: KArgumentCaptor<StripeRequest>

    @BeforeTest
    fun before() {
        MockitoAnnotations.initMocks(this)
        apiRequestArgumentCaptor = argumentCaptor()
        stripeRequestArgumentCaptor = argumentCaptor()
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
        assertEquals("https://api.stripe.com/v1/customers/$customerId/sources",
            addSourceUrl)
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
        val expectedUrl = arrayOf("https://api.stripe.com/v1/payment_methods/",
            paymentMethodId, "/attach").joinToString("")
        assertEquals(expectedUrl, attachUrl)
    }

    @Test
    fun testGetDetachPaymentMethodUrl() {
        val paymentMethodId = "pm_1ETDEa2eZvKYlo2CN5828c52"
        val detachUrl = stripeApiRepository.getDetachPaymentMethodUrl(paymentMethodId)
        val expectedUrl = arrayOf("https://api.stripe.com/v1/payment_methods/",
            paymentMethodId, "/detach").joinToString("")
        assertEquals(expectedUrl, detachUrl)
    }

    @Test
    fun testGetPaymentMethodsUrl() {
        assertEquals("https://api.stripe.com/v1/payment_methods",
            StripeApiRepository.paymentMethodsUrl)
    }

    @Test
    fun testGetIssuingCardPinUrl() {
        assertEquals("https://api.stripe.com/v1/issuing/cards/card123/pin",
            StripeApiRepository.getIssuingCardPinUrl("card123"))
    }

    @Test
    fun testRetrievePaymentIntentUrl() {
        assertEquals("https://api.stripe.com/v1/payment_intents/pi123",
            StripeApiRepository.getRetrievePaymentIntentUrl("pi123"))
    }

    @Test
    fun testConfirmPaymentIntentUrl() {
        assertEquals("https://api.stripe.com/v1/payment_intents/pi123/confirm",
            StripeApiRepository.getConfirmPaymentIntentUrl("pi123"))
    }

    @Test
    @Throws(APIException::class, AuthenticationException::class, InvalidRequestException::class, APIConnectionException::class)
    fun createSource_shouldLogSourceCreation_andReturnSource() {
        val source = stripeApiRepository.createSource(SourceParams.createCardParams(CARD),
            ApiRequest.Options(ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY))

        // Check that we get a token back; we don't care about its fields for this test.
        assertNotNull(source)
    }

    @Test
    @Throws(APIException::class, AuthenticationException::class, InvalidRequestException::class, APIConnectionException::class)
    fun createSource_withConnectAccount_keepsHeaderInAccount() {
        val connectAccountId = "acct_1Acj2PBUgO3KuWzz"
        val source = stripeApiRepository.createSource(SourceParams.createCardParams(CARD),
            ApiRequest.Options(ApiKeyFixtures.CONNECTED_ACCOUNT_PUBLISHABLE_KEY, connectAccountId))

        // Check that we get a source back; we don't care about its fields for this test.
        assertNotNull(source)
    }

    @Test
    fun start3ds2Auth_withInvalidSource_shouldThrowInvalidRequestException() {
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

        val invalidRequestException = assertFailsWith<InvalidRequestException> {
            stripeApiRepository.start3ds2Auth(authParams, "pi_12345",
                ApiRequest.Options(ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY))
        }

        assertEquals("source", invalidRequestException.param)
        assertEquals("resource_missing", invalidRequestException.errorCode)
    }

    @Test
    fun complete3ds2Auth_withInvalidSource_shouldThrowInvalidRequestException() {
        val invalidRequestException =
            assertFailsWith<InvalidRequestException> {
                stripeApiRepository.complete3ds2Auth("src_123",
                    ApiRequest.Options(ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY))
            }
        assertEquals(HttpURLConnection.HTTP_NOT_FOUND, invalidRequestException.statusCode)
        assertEquals("source", invalidRequestException.param)
        assertEquals("resource_missing", invalidRequestException.errorCode)
    }

    @Test
    fun fireAnalyticsRequest_shouldReturnSuccessful() {
        // This is the one and only test where we actually log something, because
        // we are testing whether or not we log.
        stripeApiRepository.fireAnalyticsRequest(
            emptyMap(),
            ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY
        )
    }

    @Test
    @Throws(CardException::class, APIException::class, AuthenticationException::class,
        InvalidRequestException::class, APIConnectionException::class)
    fun requestData_withConnectAccount_shouldReturnCorrectResponseHeaders() {
        val connectAccountId = "acct_1Acj2PBUgO3KuWzz"
        val response = stripeApiRepository.makeApiRequest(
            ApiRequest.createPost(
                StripeApiRepository.sourcesUrl,
                ApiRequest.Options(
                    ApiKeyFixtures.CONNECTED_ACCOUNT_PUBLISHABLE_KEY,
                    connectAccountId
                ),
                SourceParams.createCardParams(CARD).toParamMap(),
                null)
        )
        assertNotNull(response)

        val responseHeaders = response.responseHeaders.orEmpty()

        // the Stripe API response will either have a 'Stripe-Account' or 'stripe-account' header,
        // so we need to check both
        val accounts = when {
            responseHeaders.containsKey(STRIPE_ACCOUNT_RESPONSE_HEADER) ->
                responseHeaders[STRIPE_ACCOUNT_RESPONSE_HEADER]
            responseHeaders.containsKey(
                STRIPE_ACCOUNT_RESPONSE_HEADER.toLowerCase(Locale.ROOT)) ->
                responseHeaders[STRIPE_ACCOUNT_RESPONSE_HEADER.toLowerCase(Locale.ROOT)]
            else -> null
        }

        requireNotNull(accounts, { "Stripe API response should contain 'Stripe-Account' header" })
        assertEquals(1, accounts.size)
        assertEquals(connectAccountId, accounts[0])
    }

    @Test
    @Throws(APIException::class, AuthenticationException::class, InvalidRequestException::class,
        APIConnectionException::class)
    fun confirmPaymentIntent_withSourceData_canSuccessfulConfirm() {
        val clientSecret = "temporarily put a private key here simulate the backend"

        `when`(stripeApiRequestExecutor.execute(any()))
            .thenReturn(
                StripeResponse(200,
                    PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2_JSON.toString(),
                    emptyMap()
                )
            )

        val confirmPaymentIntentParams =
            ConfirmPaymentIntentParams.createWithPaymentMethodCreateParams(
                PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
                clientSecret,
                "yourapp://post-authentication-return-url"
            )
        val paymentIntent = create().confirmPaymentIntent(
            confirmPaymentIntentParams,
            ApiRequest.Options(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY)
        )

        assertNotNull(paymentIntent)

        verify(stripeApiRequestExecutor).execute(apiRequestArgumentCaptor.capture())
        val apiRequest = apiRequestArgumentCaptor.firstValue
        val paymentMethodDataParams =
            apiRequest.params?.get("payment_method_data") as Map<String, *>
        assertTrue(paymentMethodDataParams["muid"] is String)
        assertTrue(paymentMethodDataParams["guid"] is String)
        assertEquals("card", paymentMethodDataParams["type"])

        verify(fireAndForgetRequestExecutor, times(2))
            .executeAsync(stripeRequestArgumentCaptor.capture())
        val stripeRequests = stripeRequestArgumentCaptor.allValues
        val analyticsRequest = stripeRequests[1] as ApiRequest
        assertEquals(PaymentMethod.Type.Card.code, analyticsRequest.params?.get("source_type"))
    }

    @Ignore("requires a secret key")
    @Throws(APIException::class, AuthenticationException::class, InvalidRequestException::class,
        APIConnectionException::class)
    fun disabled_confirmPaymentIntent_withSourceId_canSuccessfulConfirm() {
        val clientSecret = "temporarily put a private key here simulate the backend"
        val publishableKey = "put a public key that matches the private key here"
        val sourceId = "id of the source created on the backend"

        val confirmPaymentIntentParams = ConfirmPaymentIntentParams.createWithSourceId(
            sourceId,
            clientSecret,
            "yourapp://post-authentication-return-url"
        )
        val paymentIntent = stripeApiRepository.confirmPaymentIntent(
            confirmPaymentIntentParams, ApiRequest.Options(publishableKey))
        assertNotNull(paymentIntent)
    }

    @Ignore("requires a secret key")
    @Throws(APIException::class, AuthenticationException::class, InvalidRequestException::class, APIConnectionException::class)
    fun disabled_confirmRetrieve_withSourceId_canSuccessfulRetrieve() {
        val clientSecret = "temporarily put a private key here simulate the backend"
        val publishableKey = "put a public key that matches the private key here"

        val paymentIntent = stripeApiRepository.retrievePaymentIntent(
            clientSecret,
            ApiRequest.Options(publishableKey)
        )
        assertNotNull(paymentIntent)
    }

    @Test
    @Throws(APIException::class, AuthenticationException::class, InvalidRequestException::class, APIConnectionException::class)
    fun createSource_withNonLoggingListener_doesNotLogButDoesCreateSource() {
        val stripeApiRepository = StripeApiRepository(
            ApplicationProvider.getApplicationContext<Context>(),
            stripeApiRequestExecutor = StripeApiRequestExecutor(),
            fireAndForgetRequestExecutor = FakeFireAndForgetRequestExecutor()
        )
        val source = stripeApiRepository.createSource(SourceParams.createCardParams(CARD),
            ApiRequest.Options(ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY))

        // Check that we get a token back; we don't care about its fields for this test.
        assertNotNull(source)
    }

    @Test
    fun fireAnalyticsRequest_whenShouldLogRequestIsFalse_doesNotCreateAConnection() {
        val stripeApiRepository = create()
        stripeApiRepository.fireAnalyticsRequest(
            emptyMap(),
            ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY
        )
        verifyNoMoreInteractions(stripeApiRequestExecutor)
    }

    @Test
    @Throws(StripeException::class, UnsupportedEncodingException::class)
    fun getPaymentMethods_whenPopulated_returnsExpectedList() {
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
        val stripeResponse = StripeResponse(200, responseBody, null)
        val queryParams = mapOf(
            "customer" to "cus_123",
            "type" to PaymentMethod.Type.Card.code
        )

        val options = ApiRequest.Options(ApiKeyFixtures.FAKE_EPHEMERAL_KEY)
        val url = ApiRequest.createGet(StripeApiRepository.paymentMethodsUrl,
            options, queryParams, null)
            .url

        `when`(
            stripeApiRequestExecutor.execute(argThat {
                ApiRequestMatcher(StripeRequest.Method.GET, url, options, queryParams)
                    .matches(this)
            })
        ).thenReturn(stripeResponse)
        val stripeApiRepository = create()
        val paymentMethods = stripeApiRepository
            .getPaymentMethods("cus_123", PaymentMethod.Type.Card.code,
                ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY, emptySet(),
                ApiRequest.Options(ApiKeyFixtures.FAKE_EPHEMERAL_KEY))
        assertEquals(3, paymentMethods.size)
        assertEquals("pm_1EVNYJCRMbs6FrXfG8n52JaK", paymentMethods[0].id)
        assertEquals("pm_1EVNXtCRMbs6FrXfTlZGIdGq", paymentMethods[1].id)
        assertEquals("src_1EVO8DCRMbs6FrXf2Dspj49a", paymentMethods[2].id)
    }

    @Test
    @Throws(StripeException::class, UnsupportedEncodingException::class)
    fun getPaymentMethods_whenNotPopulated_returnsEmptydList() {
        val responseBody =
            """
            {
                "object": "list",
                "data": [],
                "has_more": false,
                "url": "/v1/payment_methods"
            }
            """.trimIndent()
        val stripeResponse = StripeResponse(200, responseBody, null)
        val queryParams = mapOf(
            "customer" to "cus_123",
            "type" to PaymentMethod.Type.Card.code
        )

        val options = ApiRequest.Options(ApiKeyFixtures.FAKE_EPHEMERAL_KEY)
        val url = ApiRequest.createGet(
            StripeApiRepository.paymentMethodsUrl,
            options,
            queryParams, null)
            .url

        `when`(
            stripeApiRequestExecutor.execute(argThat {
                ApiRequestMatcher(StripeRequest.Method.GET, url, options, queryParams)
                    .matches(this)
            })
        ).thenReturn(stripeResponse)
        val stripeApiRepository = create()
        val paymentMethods = stripeApiRepository
            .getPaymentMethods("cus_123", PaymentMethod.Type.Card.code,
                ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY, emptySet(),
                ApiRequest.Options(ApiKeyFixtures.FAKE_EPHEMERAL_KEY))
        assertTrue(paymentMethods.isEmpty())
    }

    @Test
    fun getFpxBankStatus_withFpxKey() {
        val fpxBankStatuses = stripeApiRepository.getFpxBankStatus(
            ApiRequest.Options(ApiKeyFixtures.FPX_PUBLISHABLE_KEY)
        )
        assertTrue(fpxBankStatuses.isOnline(FpxBank.Hsbc.id))
    }

    @Test
    fun cancelPaymentIntentSource_whenAlreadyCanceled_throwsInvalidRequestException() {
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
        assertEquals("payment_intent_unexpected_state", exception.errorCode)
    }

    private fun create(): StripeApiRepository {
        return StripeApiRepository(
            ApplicationProvider.getApplicationContext<Context>(),
            stripeApiRequestExecutor = stripeApiRequestExecutor,
            fireAndForgetRequestExecutor = fireAndForgetRequestExecutor,
            networkUtils = StripeNetworkUtils(
                UidParamsFactory("foo", FakeUidSupplier())
            )
        )
    }

    private companion object {
        private const val STRIPE_ACCOUNT_RESPONSE_HEADER = "Stripe-Account"
        private val CARD =
            Card.create("4242424242424242", 1, 2050, "123")
    }
}
