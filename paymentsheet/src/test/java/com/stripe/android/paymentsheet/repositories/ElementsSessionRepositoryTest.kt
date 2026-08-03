package com.stripe.android.paymentsheet.repositories

import androidx.core.os.LocaleListCompat
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.LinkDisallowFundingSourceCreationPreview
import com.stripe.android.PaymentConfiguration
import com.stripe.android.SharedPaymentTokenSessionPreview
import com.stripe.android.core.networking.AnalyticsRequestFactory
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.core.networking.StripeNetworkClient
import com.stripe.android.core.networking.StripeRequest
import com.stripe.android.core.networking.StripeResponse
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.model.ElementsSession
import com.stripe.android.model.ElementsSessionFixtures
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.parsers.MobileSessionContractException
import com.stripe.android.networking.StripeRepository
import com.stripe.android.paymentelement.PaymentMethodOptionsSetupFutureUsagePreview
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.json.JSONArray
import org.json.JSONObject
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import java.util.Locale
import java.util.UUID
import kotlin.test.Test
import com.stripe.android.paymentsheet.forms.generated.BillingDetailsCollectionConfigV1 as BillingDetailsCollectionConfig
import com.stripe.android.paymentsheet.forms.generated.CardBrandAcceptanceV1 as CardBrandAcceptance
import com.stripe.android.paymentsheet.forms.generated.LinkConfigV1 as LinkConfig
import com.stripe.android.paymentsheet.forms.generated.MobileSessionContractV1 as MobileSessionContract
import com.stripe.android.paymentsheet.forms.generated.PaymentSheetConfigV1 as PaymentSheetConfig

@RunWith(RobolectricTestRunner::class)
internal class ElementsSessionRepositoryTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private val stripeRepository = mock<StripeRepository>()
    private val stripeNetworkClient = mock<StripeNetworkClient>()
    private val requestCaptor = argumentCaptor<StripeRequest>()

    @Test
    fun `get with locale should retrieve with element session`() = runTest {
        whenever(stripeNetworkClient.executeRequest(any())).thenReturn(
            StripeResponse(200, ElementsSessionFixtures.EXPANDED_PAYMENT_INTENT_JSON.toString(), emptyMap())
        )

        val locale = Locale.GERMANY
        val session = withLocale(locale) {
            createRepository().get(
                initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(
                    clientSecret = "client_secret",
                ),
                customer = null,
                externalPaymentMethods = emptyList(),
                customPaymentMethods = emptyList(),
                savedPaymentMethodSelectionId = null,
                countryOverride = null,
                paymentSheetConfig = null,
            ).getOrThrow()
        }

        verify(stripeNetworkClient).executeRequest(requestCaptor.capture())
        verify(stripeRepository, never()).retrievePaymentIntent(any(), any(), any())
        assertThat(session).isNotNull()

        val request = requestCaptor.firstValue as ApiRequest
        val params = requireNotNull(request.params)
        assertThat(request.headers).doesNotContainKey(MOBILE_SESSION_CONTRACT_HEADER)
        assertThat(params["mobile_app_id"]).isEqualTo(APP_ID)
        assertThat(params["locale"]).isEqualTo(locale.toLanguageTag())
    }

    @Test
    fun `get sends generated PaymentSheet config`() = runTest {
        whenever(stripeNetworkClient.executeRequest(any())).thenReturn(
            negotiatedElementsSessionResponse()
        )
        val paymentSheetConfig = PaymentSheetConfig(
            merchantCountryCode = "GB",
            allowsDelayedPaymentMethods = true,
            allowsPaymentMethodsRequiringShippingAddress = true,
            link = LinkConfig(display = "never", disabledFundingSources = listOf("card")),
            preferredNetworks = listOf("visa"),
            billingDetailsCollectionConfiguration = BillingDetailsCollectionConfig(
                name = "always",
                allowedCountries = listOf("GB"),
            ),
            externalPaymentMethods = listOf("external_paypal"),
            customPaymentMethodIds = listOf("cpmt_123"),
            paymentMethodOrder = listOf("card", "link"),
            paymentMethodLayout = "vertical",
            cardBrandAcceptance = CardBrandAcceptance(filter = "allowed", brands = listOf("visa")),
            allowedCardFundingTypes = listOf("credit"),
            termsDisplay = mapOf("card" to "never"),
        )

        createRepository().get(
            initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(
                clientSecret = "client_secret",
            ),
            customer = null,
            externalPaymentMethods = emptyList(),
            customPaymentMethods = emptyList(),
            savedPaymentMethodSelectionId = null,
            countryOverride = null,
            paymentSheetConfig = paymentSheetConfig,
        ).getOrThrow()

        verify(stripeNetworkClient).executeRequest(requestCaptor.capture())
        val request = requestCaptor.firstValue as ApiRequest
        assertThat(request.headers["Stripe-Mobile-Session-Contract"]).isEqualTo(
            "major=${MobileSessionContract.CONTRACT_MAJOR}; revision=${MobileSessionContract.CONTRACT_REVISION}"
        )

        @Suppress("UNCHECKED_CAST")
        val config = requireNotNull(request.params?.get("payment_sheet_config")) as Map<String, Any?>
        assertThat(config["merchant_country_code"]).isEqualTo("GB")
        assertThat(config["allows_delayed_payment_methods"]).isEqualTo("true")
        assertThat(config["allows_payment_methods_requiring_shipping_address"]).isEqualTo("true")
        assertThat(config["payment_method_order"]).isEqualTo(listOf("card", "link"))
        assertThat(config["payment_method_layout"]).isEqualTo("vertical")
        assertThat(config["terms_display"]).isEqualTo(mapOf("card" to "never"))
    }

    @Test
    fun `negotiated response accepts a same-major server revision`() = runTest {
        val serverRevision = "0000000000000000"
        whenever(stripeNetworkClient.executeRequest(any())).thenReturn(
            negotiatedElementsSessionResponse(
                bodyRevision = serverRevision,
                responseHeaderValue = "major=${MobileSessionContract.CONTRACT_MAJOR}; revision=$serverRevision",
            )
        )

        val result = getNegotiatedElementsSession()

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrThrow().mobilePaymentElement?.contract?.revision).isEqualTo(serverRevision)
    }

    @Test
    fun `negotiated response fails when response header is missing without fallback`() = runTest {
        whenever(stripeNetworkClient.executeRequest(any())).thenReturn(
            negotiatedElementsSessionResponse(responseHeaderValue = null)
        )

        val result = getNegotiatedElementsSession()

        assertMobileSessionContractFailure(
            result,
            MobileSessionContractException.ErrorCode.MissingResponseHeader,
        )
        verify(stripeRepository, never()).retrievePaymentIntent(any(), any(), any())
    }

    @Test
    fun `negotiated response fails when response header is malformed without fallback`() = runTest {
        whenever(stripeNetworkClient.executeRequest(any())).thenReturn(
            negotiatedElementsSessionResponse(responseHeaderValue = "major=1, revision=bad")
        )

        val result = getNegotiatedElementsSession()

        assertMobileSessionContractFailure(
            result,
            MobileSessionContractException.ErrorCode.MalformedResponseHeader,
        )
        verify(stripeRepository, never()).retrievePaymentIntent(any(), any(), any())
    }

    @Test
    fun `negotiated response fails when response header and body metadata differ without fallback`() = runTest {
        whenever(stripeNetworkClient.executeRequest(any())).thenReturn(
            negotiatedElementsSessionResponse(
                responseHeaderValue =
                    "major=${MobileSessionContract.CONTRACT_MAJOR}; revision=0000000000000000"
            )
        )

        val result = getNegotiatedElementsSession()

        assertMobileSessionContractFailure(
            result,
            MobileSessionContractException.ErrorCode.ResponseHeaderBodyMismatch,
        )
        verify(stripeRepository, never()).retrievePaymentIntent(any(), any(), any())
    }

    @Test
    fun `negotiated response fails when mobile payment element is missing without fallback`() = runTest {
        whenever(stripeNetworkClient.executeRequest(any())).thenReturn(
            negotiatedElementsSessionResponse(includeMobilePaymentElement = false)
        )

        val result = getNegotiatedElementsSession()

        assertMobileSessionContractFailure(
            result,
            MobileSessionContractException.ErrorCode.MissingPayload,
        )
        verify(stripeRepository, never()).retrievePaymentIntent(any(), any(), any())
    }

    @Test
    fun `negotiated server failure does not enter legacy fallback`() = runTest {
        whenever(stripeNetworkClient.executeRequest(any())).thenReturn(
            StripeResponse(500, """{"error":{"message":"Server error"}}""", emptyMap())
        )

        val result = getNegotiatedElementsSession()

        assertThat(result.isFailure).isTrue()
        verify(stripeRepository, never()).retrievePaymentIntent(any(), any(), any())
    }

    @Test
    fun `get with locale when element session fails should fallback to retrievePaymentIntent()`() =
        runTest {
            whenever(stripeNetworkClient.executeRequest(any())).thenReturn(
                StripeResponse(500, """{"error":{"message":"Server error"}}""", emptyMap())
            )

            whenever(stripeRepository.retrievePaymentIntent(any(), any(), any()))
                .thenReturn(Result.success(PaymentIntentFixtures.PI_WITH_SHIPPING))

            val session = withLocale(Locale.ITALY) {
                createRepository().get(
                    initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(
                        clientSecret = "client_secret",
                    ),
                    customer = null,
                    externalPaymentMethods = emptyList(),
                    customPaymentMethods = emptyList(),
                    savedPaymentMethodSelectionId = null,
                    countryOverride = null,
                    paymentSheetConfig = null,
                ).getOrThrow()
            }

            verify(stripeNetworkClient).executeRequest(any())
            verify(stripeRepository).retrievePaymentIntent(any(), any(), any())
            assertThat(session.stripeIntent).isEqualTo(PaymentIntentFixtures.PI_WITH_SHIPPING)
        }

    @Test
    fun `get with locale when element session fails with exception should fallback to retrievePaymentIntent()`() =
        runTest {
            whenever(stripeNetworkClient.executeRequest(any())).thenReturn(
                StripeResponse(500, """{"error":{"message":"Server error"}}""", emptyMap())
            )

            whenever(stripeRepository.retrievePaymentIntent(any(), any(), any()))
                .thenReturn(Result.success(PaymentIntentFixtures.PI_WITH_SHIPPING))

            val session = withLocale(Locale.ITALY) {
                createRepository().get(
                    initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(
                        clientSecret = "client_secret",
                    ),
                    customer = null,
                    externalPaymentMethods = emptyList(),
                    customPaymentMethods = emptyList(),
                    savedPaymentMethodSelectionId = null,
                    countryOverride = null,
                    paymentSheetConfig = null,
                ).getOrThrow()
            }

            verify(stripeNetworkClient).executeRequest(any())
            verify(stripeRepository).retrievePaymentIntent(any(), any(), any())
            assertThat(session.stripeIntent).isEqualTo(PaymentIntentFixtures.PI_WITH_SHIPPING)
        }

    @Test
    fun `get without locale should retrieve ordered payment methods in default locale`() = runTest {
        whenever(stripeNetworkClient.executeRequest(any())).thenReturn(
            StripeResponse(200, ElementsSessionFixtures.EXPANDED_PAYMENT_INTENT_JSON.toString(), emptyMap())
        )

        val session = RealElementsSessionRepository(
            ApplicationProvider.getApplicationContext(),
            stripeNetworkClient,
            stripeRepository,
            { PaymentConfiguration(ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY) },
            testDispatcher,
            clientParams = TEST_CLIENT_PARAMS
        ).get(
            initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(
                clientSecret = "client_secret",
            ),
            customer = null,
            externalPaymentMethods = emptyList(),
            customPaymentMethods = emptyList(),
            savedPaymentMethodSelectionId = null,
            countryOverride = null,
            paymentSheetConfig = null,
        ).getOrThrow()

        verify(stripeNetworkClient).executeRequest(requestCaptor.capture())
        verify(stripeRepository, never()).retrievePaymentIntent(any(), any(), any())
        assertThat(session).isNotNull()

        val request = requestCaptor.firstValue as ApiRequest
        val params = requireNotNull(request.params)
        val defaultLocale = LocaleListCompat.getAdjustedDefault()[0]?.toLanguageTag()
        assertThat(params["locale"]).isEqualTo(defaultLocale)
        assertThat(params["mobile_app_id"]).isEqualTo(APP_ID)
    }

    @Test
    fun `Handles deferred intent elements session lookup failure gracefully`() = runTest {
        whenever(stripeNetworkClient.executeRequest(any())).thenReturn(
            StripeResponse(500, """{"error":{"message":"this didn't work"}}""", emptyMap())
        )

        val session = RealElementsSessionRepository(
            ApplicationProvider.getApplicationContext(),
            stripeNetworkClient,
            stripeRepository,
            { PaymentConfiguration(ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY) },
            testDispatcher,
            clientParams = TEST_CLIENT_PARAMS
        ).get(
            initializationMode = PaymentElementLoader.InitializationMode.DeferredIntent(
                intentConfiguration = PaymentSheet.IntentConfiguration(
                    mode = PaymentSheet.IntentConfiguration.Mode.Payment(
                        amount = 1234,
                        currency = "cad",
                    )
                )
            ),
            customer = null,
            externalPaymentMethods = emptyList(),
            customPaymentMethods = emptyList(),
            savedPaymentMethodSelectionId = null,
            countryOverride = null,
            paymentSheetConfig = null,
        )

        assertThat(session.isSuccess).isTrue()
        assertThat(session.getOrNull()?.stripeIntent?.paymentMethodTypes).containsExactly("card")
    }

    @Test
    fun `Does not create fallback for exception caused by client error`() = runTest {
        whenever(stripeNetworkClient.executeRequest(any())).thenReturn(
            StripeResponse(401, """{"error":{"message":"Unauthorized"}}""", emptyMap())
        )

        val session = createRepository().get(
            initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(
                clientSecret = "client_secret",
            ),
            customer = null,
            externalPaymentMethods = emptyList(),
            customPaymentMethods = emptyList(),
            savedPaymentMethodSelectionId = null,
            countryOverride = null,
            paymentSheetConfig = null,
        )

        verify(stripeNetworkClient).executeRequest(any())
        assertThat(session.isFailure).isTrue()
    }

    @Test
    fun `Does not create fallback for exception when no response`() = runTest {
        whenever(stripeNetworkClient.executeRequest(any())).thenAnswer {
            throw java.io.IOException("Connection failed")
        }

        val session = createRepository().get(
            initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(
                clientSecret = "client_secret",
            ),
            customer = null,
            externalPaymentMethods = emptyList(),
            customPaymentMethods = emptyList(),
            savedPaymentMethodSelectionId = null,
            countryOverride = null,
            paymentSheetConfig = null,
        )

        verify(stripeNetworkClient).executeRequest(any())
        assertThat(session.isFailure).isTrue()
    }

    @Test
    fun `Deferred intent elements session failure uses payment method types if specified`() = runTest {
        whenever(stripeNetworkClient.executeRequest(any())).thenReturn(
            StripeResponse(500, """{"error":{"message":"this didn't work"}}""", emptyMap())
        )
        val expectedPaymentMethodTypes = listOf("card", "amazon_pay")

        val session = RealElementsSessionRepository(
            ApplicationProvider.getApplicationContext(),
            stripeNetworkClient,
            stripeRepository,
            { PaymentConfiguration(ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY) },
            testDispatcher,
            clientParams = TEST_CLIENT_PARAMS
        ).get(
            initializationMode = PaymentElementLoader.InitializationMode.DeferredIntent(
                intentConfiguration = PaymentSheet.IntentConfiguration(
                    mode = PaymentSheet.IntentConfiguration.Mode.Payment(
                        amount = 1234,
                        currency = "cad",
                    ),
                    paymentMethodTypes = expectedPaymentMethodTypes
                )
            ),
            customer = null,
            externalPaymentMethods = emptyList(),
            customPaymentMethods = emptyList(),
            savedPaymentMethodSelectionId = null,
            countryOverride = null,
            paymentSheetConfig = null,
        )

        assertThat(session.isSuccess).isTrue()
        assertThat(session.getOrNull()?.stripeIntent?.paymentMethodTypes).isEqualTo(expectedPaymentMethodTypes)
    }

    @Test
    fun `Verify customer session client secret is passed to 'StripeRepository'`() = runTest {
        whenever(stripeNetworkClient.executeRequest(any())).thenReturn(
            StripeResponse(200, ElementsSessionFixtures.DEFERRED_INTENT_JSON.toString(), emptyMap())
        )

        val repository = RealElementsSessionRepository(
            ApplicationProvider.getApplicationContext(),
            stripeNetworkClient,
            stripeRepository,
            { PaymentConfiguration(ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY) },
            testDispatcher,
            clientParams = TEST_CLIENT_PARAMS
        )

        repository.get(
            initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(
                clientSecret = "client_secret"
            ),
            customer = PaymentSheet.CustomerConfiguration.createWithCustomerSession(
                id = "cus_1",
                clientSecret = "customer_session_client_secret"
            ),
            externalPaymentMethods = emptyList(),
            customPaymentMethods = emptyList(),
            savedPaymentMethodSelectionId = null,
            countryOverride = null,
            paymentSheetConfig = null,
        )

        verify(stripeNetworkClient).executeRequest(requestCaptor.capture())
        val request = requestCaptor.firstValue as ApiRequest
        val params = requireNotNull(request.params)
        assertThat(params["customer_session_client_secret"]).isEqualTo("customer_session_client_secret")
    }

    @Test
    fun `Verify legacy customer ephemeral key is passed to 'StripeRepository'`() = runTest {
        whenever(stripeNetworkClient.executeRequest(any())).thenReturn(
            StripeResponse(200, ElementsSessionFixtures.DEFERRED_INTENT_JSON.toString(), emptyMap())
        )

        val repository = RealElementsSessionRepository(
            ApplicationProvider.getApplicationContext(),
            stripeNetworkClient,
            stripeRepository,
            { PaymentConfiguration(ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY) },
            testDispatcher,
            clientParams = TEST_CLIENT_PARAMS
        )

        repository.get(
            initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(
                clientSecret = "client_secret"
            ),
            customer = PaymentSheet.CustomerConfiguration(
                id = "cus_1",
                ephemeralKeySecret = "legacy_customer_ephemeral_key"
            ),
            externalPaymentMethods = emptyList(),
            customPaymentMethods = emptyList(),
            savedPaymentMethodSelectionId = null,
            countryOverride = null,
            paymentSheetConfig = null,
        )

        verify(stripeNetworkClient).executeRequest(requestCaptor.capture())
        val request = requestCaptor.firstValue as ApiRequest
        val params = requireNotNull(request.params)
        assertThat(params["legacy_customer_ephemeral_key"]).isEqualTo("legacy_customer_ephemeral_key")
    }

    @Test
    fun `Verify default payment method id is passed to 'StripeRepository'`() = runTest {
        whenever(stripeNetworkClient.executeRequest(any())).thenReturn(
            StripeResponse(200, ElementsSessionFixtures.DEFERRED_INTENT_JSON.toString(), emptyMap())
        )

        val repository = RealElementsSessionRepository(
            ApplicationProvider.getApplicationContext(),
            stripeNetworkClient,
            stripeRepository,
            { PaymentConfiguration(ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY) },
            testDispatcher,
            clientParams = TEST_CLIENT_PARAMS
        )

        repository.get(
            initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(
                clientSecret = "client_secret"
            ),
            customer = null,
            externalPaymentMethods = emptyList(),
            customPaymentMethods = emptyList(),
            savedPaymentMethodSelectionId = "pm_123",
            countryOverride = null,
            paymentSheetConfig = null,
        )

        verify(stripeNetworkClient).executeRequest(requestCaptor.capture())
        val request = requestCaptor.firstValue as ApiRequest
        val params = requireNotNull(request.params)
        assertThat(params["client_default_payment_method"]).isEqualTo("pm_123")
    }

    @Test
    fun `Verify custom payment methods ids are passed to 'StripeRepository'`() = runTest {
        whenever(stripeNetworkClient.executeRequest(any())).thenReturn(
            StripeResponse(200, ElementsSessionFixtures.DEFERRED_INTENT_JSON.toString(), emptyMap())
        )

        val repository = RealElementsSessionRepository(
            ApplicationProvider.getApplicationContext(),
            stripeNetworkClient,
            stripeRepository,
            { PaymentConfiguration(ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY) },
            testDispatcher,
            clientParams = TEST_CLIENT_PARAMS
        )

        repository.get(
            initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(
                clientSecret = "client_secret"
            ),
            customer = null,
            externalPaymentMethods = emptyList(),
            customPaymentMethods = listOf(
                PaymentSheet.CustomPaymentMethod(
                    id = "cpmt_123",
                    subtitle = "Pay now".resolvableString,
                    disableBillingDetailCollection = true,
                ),
                PaymentSheet.CustomPaymentMethod(
                    id = "cpmt_456",
                    subtitle = "Pay later".resolvableString,
                    disableBillingDetailCollection = true,
                ),
                PaymentSheet.CustomPaymentMethod(
                    id = "cpmt_789",
                    subtitle = "Pay never".resolvableString,
                    disableBillingDetailCollection = true,
                ),
            ),
            savedPaymentMethodSelectionId = "pm_123",
            countryOverride = null,
            paymentSheetConfig = null,
        )

        verify(stripeNetworkClient).executeRequest(requestCaptor.capture())
        val request = requestCaptor.firstValue as ApiRequest
        val params = requireNotNull(request.params)
        assertThat(params["custom_payment_methods"]).isEqualTo(listOf("cpmt_123", "cpmt_456", "cpmt_789"))
    }

    @OptIn(SharedPaymentTokenSessionPreview::class)
    @Test
    fun `Verify seller details is passed to 'StripeRepository'`() = runTest {
        whenever(stripeNetworkClient.executeRequest(any())).thenReturn(
            StripeResponse(200, ElementsSessionFixtures.DEFERRED_INTENT_JSON.toString(), emptyMap())
        )

        val repository = RealElementsSessionRepository(
            ApplicationProvider.getApplicationContext(),
            stripeNetworkClient,
            stripeRepository,
            { PaymentConfiguration(ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY) },
            testDispatcher,
            clientParams = TEST_CLIENT_PARAMS
        )

        repository.get(
            initializationMode = PaymentElementLoader.InitializationMode.DeferredIntent(
                intentConfiguration = PaymentSheet.IntentConfiguration(
                    sharedPaymentTokenSessionWithMode =
                    PaymentSheet.IntentConfiguration.Mode.Payment(amount = 1234, currency = "cad"),
                    sellerDetails = PaymentSheet.IntentConfiguration.SellerDetails(
                        businessName = "My business, Inc.",
                        networkId = "network_123",
                        externalId = "external_123",
                    ),
                ),
            ),
            customer = null,
            externalPaymentMethods = emptyList(),
            customPaymentMethods = emptyList(),
            savedPaymentMethodSelectionId = null,
            countryOverride = null,
            paymentSheetConfig = null,
        )

        verify(stripeNetworkClient).executeRequest(requestCaptor.capture())
        val request = requestCaptor.firstValue as ApiRequest
        val params = requireNotNull(request.params)
        assertThat(params["seller_details[network_id]"]).isEqualTo("network_123")
        assertThat(params["seller_details[external_id]"]).isEqualTo("external_123")
    }

    @Test
    fun `Verify mobile session ID is passed to 'StripeRepository'`() = runTest {
        AnalyticsRequestFactory.setSessionId(UUID.fromString("537a88ff-a54f-42cc-ba52-c7c5623730b6"))

        whenever(stripeNetworkClient.executeRequest(any())).thenReturn(
            StripeResponse(200, ElementsSessionFixtures.DEFERRED_INTENT_JSON.toString(), emptyMap())
        )

        val repository = RealElementsSessionRepository(
            ApplicationProvider.getApplicationContext(),
            stripeNetworkClient,
            stripeRepository,
            { PaymentConfiguration(ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY) },
            testDispatcher,
            clientParams = TEST_CLIENT_PARAMS
        )

        repository.get(
            initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(
                clientSecret = "client_secret"
            ),
            customer = null,
            externalPaymentMethods = emptyList(),
            customPaymentMethods = listOf(
                PaymentSheet.CustomPaymentMethod(
                    id = "cpmt_123",
                    subtitle = "Pay now".resolvableString,
                    disableBillingDetailCollection = true,
                ),
                PaymentSheet.CustomPaymentMethod(
                    id = "cpmt_456",
                    subtitle = "Pay later".resolvableString,
                    disableBillingDetailCollection = true,
                ),
                PaymentSheet.CustomPaymentMethod(
                    id = "cpmt_789",
                    subtitle = "Pay never".resolvableString,
                    disableBillingDetailCollection = true,
                ),
            ),
            savedPaymentMethodSelectionId = "pm_123",
            countryOverride = null,
            paymentSheetConfig = null,
        )

        verify(stripeNetworkClient).executeRequest(requestCaptor.capture())
        val request = requestCaptor.firstValue as ApiRequest
        val params = requireNotNull(request.params)
        assertThat(params["mobile_session_id"]).isEqualTo(MOBILE_SESSION_ID)
    }

    @OptIn(PaymentMethodOptionsSetupFutureUsagePreview::class)
    @Test
    fun `Verify PMO SFU params are passed to 'StripeRepository'`() = runTest {
        whenever(stripeNetworkClient.executeRequest(any())).thenReturn(
            StripeResponse(200, ElementsSessionFixtures.DEFERRED_INTENT_JSON.toString(), emptyMap())
        )

        val paymentMethodOptions = PaymentSheet.IntentConfiguration.Mode.Payment.PaymentMethodOptions(
            setupFutureUsageValues = mapOf(
                PaymentMethod.Type.Card to PaymentSheet.IntentConfiguration.SetupFutureUse.OnSession,
                PaymentMethod.Type.Affirm to PaymentSheet.IntentConfiguration.SetupFutureUse.None,
                PaymentMethod.Type.AmazonPay
                    to PaymentSheet.IntentConfiguration.SetupFutureUse.OffSession,
            )
        )

        createRepository().get(
            initializationMode = PaymentElementLoader.InitializationMode.DeferredIntent(
                intentConfiguration = PaymentSheet.IntentConfiguration(
                    mode = PaymentSheet.IntentConfiguration.Mode.Payment(
                        amount = 1000,
                        currency = "usd",
                        setupFutureUse = null,
                        paymentMethodOptions = paymentMethodOptions
                    ),
                )
            ),
            customer = null,
            customPaymentMethods = listOf(),
            externalPaymentMethods = listOf(),
            savedPaymentMethodSelectionId = null,
            countryOverride = null,
            paymentSheetConfig = null,
        )

        verify(stripeNetworkClient).executeRequest(requestCaptor.capture())
        val request = requestCaptor.firstValue as ApiRequest
        val params = requireNotNull(request.params)
        assertThat(params["deferred_intent[payment_method_options]"]).isEqualTo(
            mapOf(
                "card" to mapOf("setup_future_usage" to "on_session"),
                "affirm" to mapOf("setup_future_usage" to "none"),
                "amazon_pay" to mapOf("setup_future_usage" to "off_session"),
            )
        )
    }

    @OptIn(PaymentMethodOptionsSetupFutureUsagePreview::class)
    @Test
    fun `Verify PMO SFU + requireCvcRecollection params are passed to 'StripeRepository'`() = runTest {
        whenever(stripeNetworkClient.executeRequest(any())).thenReturn(
            StripeResponse(200, ElementsSessionFixtures.DEFERRED_INTENT_JSON.toString(), emptyMap())
        )

        val paymentMethodOptions = PaymentSheet.IntentConfiguration.Mode.Payment.PaymentMethodOptions(
            setupFutureUsageValues = mapOf(
                PaymentMethod.Type.Card to PaymentSheet.IntentConfiguration.SetupFutureUse.OnSession,
                PaymentMethod.Type.Affirm to PaymentSheet.IntentConfiguration.SetupFutureUse.None,
                PaymentMethod.Type.AmazonPay
                    to PaymentSheet.IntentConfiguration.SetupFutureUse.OffSession,
            )
        )

        createRepository().get(
            initializationMode = PaymentElementLoader.InitializationMode.DeferredIntent(
                intentConfiguration = PaymentSheet.IntentConfiguration(
                    mode = PaymentSheet.IntentConfiguration.Mode.Payment(
                        amount = 1000,
                        currency = "usd",
                        setupFutureUse = null,
                        paymentMethodOptions = paymentMethodOptions,
                    ),
                    requireCvcRecollection = true,
                ),
            ),
            customer = null,
            customPaymentMethods = listOf(),
            externalPaymentMethods = listOf(),
            savedPaymentMethodSelectionId = null,
            countryOverride = null,
            paymentSheetConfig = null,
        ).getOrThrow()

        verify(stripeNetworkClient).executeRequest(requestCaptor.capture())
        val request = requestCaptor.firstValue as ApiRequest
        val params = requireNotNull(request.params)
        assertThat(params["deferred_intent[payment_method_options]"]).isEqualTo(
            mapOf(
                "card" to mapOf("setup_future_usage" to "on_session", "require_cvc_recollection" to "true"),
                "affirm" to mapOf("setup_future_usage" to "none"),
                "amazon_pay" to mapOf("setup_future_usage" to "off_session"),
            )
        )
    }

    @Test
    fun `Verify requireCvcRecollection param is passed to 'StripeRepository'`() = runTest {
        whenever(stripeNetworkClient.executeRequest(any())).thenReturn(
            StripeResponse(200, ElementsSessionFixtures.DEFERRED_INTENT_JSON.toString(), emptyMap())
        )

        createRepository().get(
            initializationMode = PaymentElementLoader.InitializationMode.DeferredIntent(
                intentConfiguration = PaymentSheet.IntentConfiguration(
                    mode = PaymentSheet.IntentConfiguration.Mode.Payment(
                        amount = 1000,
                        currency = "usd",
                        setupFutureUse = null,
                    ),
                    requireCvcRecollection = true,
                ),
            ),
            customer = null,
            customPaymentMethods = listOf(),
            externalPaymentMethods = listOf(),
            savedPaymentMethodSelectionId = null,
            countryOverride = null,
            paymentSheetConfig = null,
        ).getOrThrow()

        verify(stripeNetworkClient).executeRequest(requestCaptor.capture())
        val request = requestCaptor.firstValue as ApiRequest
        val params = requireNotNull(request.params)
        assertThat(params["deferred_intent[payment_method_options]"]).isEqualTo(
            mapOf(
                "card" to mapOf("require_cvc_recollection" to "true"),
            )
        )
    }

    @OptIn(LinkDisallowFundingSourceCreationPreview::class)
    @Test
    fun `Link disallowedFundingSourceCreation is passed through correctly`() = runTest {
        whenever(stripeNetworkClient.executeRequest(any())).thenReturn(
            StripeResponse(200, ElementsSessionFixtures.DEFERRED_INTENT_JSON.toString(), emptyMap())
        )

        createRepository().get(
            initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(
                clientSecret = "client_secret",
            ),
            customer = null,
            customPaymentMethods = emptyList(),
            externalPaymentMethods = emptyList(),
            savedPaymentMethodSelectionId = null,
            countryOverride = null,
            paymentSheetConfig = null,
            linkDisallowedFundingSourceCreation = setOf("somethingThatsNotAllowed"),
        )

        verify(stripeNetworkClient).executeRequest(requestCaptor.capture())
        val request = requestCaptor.firstValue as ApiRequest
        val params = requireNotNull(request.params)
        assertThat(params["link[disallow_funding_source_creation][0]"]).isEqualTo("somethingThatsNotAllowed")
    }

    @Test
    fun `Request URL is elements sessions endpoint`() = runTest {
        whenever(stripeNetworkClient.executeRequest(any())).thenReturn(
            StripeResponse(200, ElementsSessionFixtures.EXPANDED_PAYMENT_INTENT_JSON.toString(), emptyMap())
        )

        createRepository().get(
            initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(
                clientSecret = "client_secret",
            ),
            customer = null,
            externalPaymentMethods = emptyList(),
            customPaymentMethods = emptyList(),
            savedPaymentMethodSelectionId = null,
            countryOverride = null,
            paymentSheetConfig = null,
        )

        verify(stripeNetworkClient).executeRequest(requestCaptor.capture())
        val request = requestCaptor.firstValue as ApiRequest
        assertThat(request.baseUrl).isEqualTo("https://api.stripe.com/v1/elements/sessions")
    }

    @Test
    fun `Verify payment intent params include type, client_secret, and expand`() = runTest {
        whenever(stripeNetworkClient.executeRequest(any())).thenReturn(
            StripeResponse(200, ElementsSessionFixtures.EXPANDED_PAYMENT_INTENT_JSON.toString(), emptyMap())
        )

        createRepository().get(
            initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(
                clientSecret = "pi_123_secret_456",
            ),
            customer = null,
            externalPaymentMethods = emptyList(),
            customPaymentMethods = emptyList(),
            savedPaymentMethodSelectionId = null,
            countryOverride = null,
            paymentSheetConfig = null,
        )

        verify(stripeNetworkClient).executeRequest(requestCaptor.capture())
        val request = requestCaptor.firstValue as ApiRequest
        val params = requireNotNull(request.params)
        assertThat(params["type"]).isEqualTo("payment_intent")
        assertThat(params["client_secret"]).isEqualTo("pi_123_secret_456")
        @Suppress("UNCHECKED_CAST")
        val expand = params["expand"] as? List<String>
        assertThat(expand).containsExactly("payment_method_preference.payment_intent.payment_method")
    }

    @Test
    fun `Verify setup intent params include type, client_secret, and expand`() = runTest {
        whenever(stripeNetworkClient.executeRequest(any())).thenReturn(
            StripeResponse(200, ElementsSessionFixtures.DEFERRED_INTENT_JSON.toString(), emptyMap())
        )

        createRepository().get(
            initializationMode = PaymentElementLoader.InitializationMode.SetupIntent(
                clientSecret = "seti_123_secret_456",
            ),
            customer = null,
            externalPaymentMethods = emptyList(),
            customPaymentMethods = emptyList(),
            savedPaymentMethodSelectionId = null,
            countryOverride = null,
            paymentSheetConfig = null,
        )

        verify(stripeNetworkClient).executeRequest(requestCaptor.capture())
        val request = requestCaptor.firstValue as ApiRequest
        val params = requireNotNull(request.params)
        assertThat(params["type"]).isEqualTo("setup_intent")
        assertThat(params["client_secret"]).isEqualTo("seti_123_secret_456")
        @Suppress("UNCHECKED_CAST")
        val expand = params["expand"] as? List<String>
        assertThat(expand).containsExactly("payment_method_preference.setup_intent.payment_method")
    }

    @Test
    fun `Verify deferred intent params include deferred_intent fields`() = runTest {
        whenever(stripeNetworkClient.executeRequest(any())).thenReturn(
            StripeResponse(200, ElementsSessionFixtures.DEFERRED_INTENT_JSON.toString(), emptyMap())
        )

        createRepository().get(
            initializationMode = PaymentElementLoader.InitializationMode.DeferredIntent(
                intentConfiguration = PaymentSheet.IntentConfiguration(
                    mode = PaymentSheet.IntentConfiguration.Mode.Payment(
                        amount = 2000,
                        currency = "usd",
                    ),
                    paymentMethodTypes = listOf("card", "link"),
                )
            ),
            customer = null,
            externalPaymentMethods = emptyList(),
            customPaymentMethods = emptyList(),
            savedPaymentMethodSelectionId = null,
            countryOverride = null,
            paymentSheetConfig = null,
        )

        verify(stripeNetworkClient).executeRequest(requestCaptor.capture())
        val request = requestCaptor.firstValue as ApiRequest
        val params = requireNotNull(request.params)
        assertThat(params["type"]).isEqualTo("deferred_intent")
        assertThat(params["client_secret"]).isNull()
        assertThat(params["deferred_intent[mode]"]).isEqualTo("payment")
        assertThat(params["deferred_intent[amount]"]).isEqualTo(2000L)
        assertThat(params["deferred_intent[currency]"]).isEqualTo("usd")
        assertThat(params["deferred_intent[capture_method]"]).isEqualTo("automatic")
        assertThat(params["deferred_intent[payment_method_types][0]"]).isEqualTo("card")
        assertThat(params["deferred_intent[payment_method_types][1]"]).isEqualTo("link")
    }

    @Test
    fun `Verify external payment methods are included in params when non-empty`() = runTest {
        whenever(stripeNetworkClient.executeRequest(any())).thenReturn(
            StripeResponse(200, ElementsSessionFixtures.DEFERRED_INTENT_JSON.toString(), emptyMap())
        )

        createRepository().get(
            initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(
                clientSecret = "client_secret",
            ),
            customer = null,
            externalPaymentMethods = listOf("external_paypal", "external_venmo"),
            customPaymentMethods = emptyList(),
            savedPaymentMethodSelectionId = null,
            countryOverride = null,
            paymentSheetConfig = null,
        )

        verify(stripeNetworkClient).executeRequest(requestCaptor.capture())
        val request = requestCaptor.firstValue as ApiRequest
        val params = requireNotNull(request.params)
        assertThat(params["external_payment_methods"]).isEqualTo(listOf("external_paypal", "external_venmo"))
    }

    @Test
    fun `Verify external payment methods are not in params when empty`() = runTest {
        whenever(stripeNetworkClient.executeRequest(any())).thenReturn(
            StripeResponse(200, ElementsSessionFixtures.DEFERRED_INTENT_JSON.toString(), emptyMap())
        )

        createRepository().get(
            initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(
                clientSecret = "client_secret",
            ),
            customer = null,
            externalPaymentMethods = emptyList(),
            customPaymentMethods = emptyList(),
            savedPaymentMethodSelectionId = null,
            countryOverride = null,
            paymentSheetConfig = null,
        )

        verify(stripeNetworkClient).executeRequest(requestCaptor.capture())
        val request = requestCaptor.firstValue as ApiRequest
        val params = requireNotNull(request.params)
        assertThat(params.containsKey("external_payment_methods")).isFalse()
    }

    @Test
    fun `Verify customer session client secret not in params when null`() = runTest {
        whenever(stripeNetworkClient.executeRequest(any())).thenReturn(
            StripeResponse(200, ElementsSessionFixtures.DEFERRED_INTENT_JSON.toString(), emptyMap())
        )

        createRepository().get(
            initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(
                clientSecret = "client_secret",
            ),
            customer = null,
            externalPaymentMethods = emptyList(),
            customPaymentMethods = emptyList(),
            savedPaymentMethodSelectionId = null,
            countryOverride = null,
            paymentSheetConfig = null,
        )

        verify(stripeNetworkClient).executeRequest(requestCaptor.capture())
        val request = requestCaptor.firstValue as ApiRequest
        val params = requireNotNull(request.params)
        assertThat(params.containsKey("customer_session_client_secret")).isFalse()
    }

    @Test
    fun `Verify legacy customer ephemeral key not in params when null`() = runTest {
        whenever(stripeNetworkClient.executeRequest(any())).thenReturn(
            StripeResponse(200, ElementsSessionFixtures.DEFERRED_INTENT_JSON.toString(), emptyMap())
        )

        createRepository().get(
            initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(
                clientSecret = "client_secret",
            ),
            customer = null,
            externalPaymentMethods = emptyList(),
            customPaymentMethods = emptyList(),
            savedPaymentMethodSelectionId = null,
            countryOverride = null,
            paymentSheetConfig = null,
        )

        verify(stripeNetworkClient).executeRequest(requestCaptor.capture())
        val request = requestCaptor.firstValue as ApiRequest
        val params = requireNotNull(request.params)
        assertThat(params.containsKey("legacy_customer_ephemeral_key")).isFalse()
    }

    @Test
    fun `Verify seller details not in params when not provided`() = runTest {
        whenever(stripeNetworkClient.executeRequest(any())).thenReturn(
            StripeResponse(200, ElementsSessionFixtures.DEFERRED_INTENT_JSON.toString(), emptyMap())
        )

        createRepository().get(
            initializationMode = PaymentElementLoader.InitializationMode.DeferredIntent(
                intentConfiguration = PaymentSheet.IntentConfiguration(
                    mode = PaymentSheet.IntentConfiguration.Mode.Payment(
                        amount = 1000,
                        currency = "usd",
                    ),
                )
            ),
            customer = null,
            externalPaymentMethods = emptyList(),
            customPaymentMethods = emptyList(),
            savedPaymentMethodSelectionId = null,
            countryOverride = null,
            paymentSheetConfig = null,
        )

        verify(stripeNetworkClient).executeRequest(requestCaptor.capture())
        val request = requestCaptor.firstValue as ApiRequest
        val params = requireNotNull(request.params)
        assertThat(params.containsKey("seller_details[network_id]")).isFalse()
        assertThat(params.containsKey("seller_details[external_id]")).isFalse()
    }

    @Test
    fun `Verify client_default_payment_method not in params when not provided`() = runTest {
        whenever(stripeNetworkClient.executeRequest(any())).thenReturn(
            StripeResponse(200, ElementsSessionFixtures.DEFERRED_INTENT_JSON.toString(), emptyMap())
        )

        createRepository().get(
            initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(
                clientSecret = "client_secret",
            ),
            customer = null,
            externalPaymentMethods = emptyList(),
            customPaymentMethods = emptyList(),
            savedPaymentMethodSelectionId = null,
            countryOverride = null,
            paymentSheetConfig = null,
        )

        verify(stripeNetworkClient).executeRequest(requestCaptor.capture())
        val request = requestCaptor.firstValue as ApiRequest
        val params = requireNotNull(request.params)
        assertThat(params.containsKey("client_default_payment_method")).isFalse()
    }

    @Test
    fun `User key sends valid request for payment intents`() = runTest {
        whenever(stripeNetworkClient.executeRequest(any())).thenReturn(
            StripeResponse(200, ElementsSessionFixtures.EXPANDED_PAYMENT_INTENT_JSON.toString(), emptyMap())
        )

        createRepository(publishableKey = "uk_12345").get(
            initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(
                clientSecret = "client_secret",
            ),
            customer = null,
            externalPaymentMethods = emptyList(),
            customPaymentMethods = emptyList(),
            savedPaymentMethodSelectionId = null,
            countryOverride = null,
            paymentSheetConfig = null,
        )

        verify(stripeNetworkClient).executeRequest(requestCaptor.capture())
        val request = requestCaptor.firstValue as ApiRequest
        val params = requireNotNull(request.params)
        assertThat(request.baseUrl).isEqualTo("https://api.stripe.com/v1/elements/sessions")
        assertThat(params["type"]).isEqualTo("payment_intent")
        assertThat(params["client_secret"]).isEqualTo("client_secret")
        assertThat(params["mobile_app_id"]).isEqualTo(APP_ID)
    }

    @Test
    fun `User key sends valid request for setup intents`() = runTest {
        whenever(stripeNetworkClient.executeRequest(any())).thenReturn(
            StripeResponse(200, ElementsSessionFixtures.DEFERRED_INTENT_JSON.toString(), emptyMap())
        )

        createRepository(publishableKey = "uk_12345").get(
            initializationMode = PaymentElementLoader.InitializationMode.SetupIntent(
                clientSecret = "client_secret",
            ),
            customer = null,
            externalPaymentMethods = emptyList(),
            customPaymentMethods = emptyList(),
            savedPaymentMethodSelectionId = null,
            countryOverride = null,
            paymentSheetConfig = null,
        )

        verify(stripeNetworkClient).executeRequest(requestCaptor.capture())
        val request = requestCaptor.firstValue as ApiRequest
        val params = requireNotNull(request.params)
        assertThat(request.baseUrl).isEqualTo("https://api.stripe.com/v1/elements/sessions")
        assertThat(params["type"]).isEqualTo("setup_intent")
        assertThat(params["client_secret"]).isEqualTo("client_secret")
        assertThat(params["mobile_app_id"]).isEqualTo(APP_ID)
    }

    @Test
    fun `User key sends valid request for deferred intents`() = runTest {
        whenever(stripeNetworkClient.executeRequest(any())).thenReturn(
            StripeResponse(200, ElementsSessionFixtures.DEFERRED_INTENT_JSON.toString(), emptyMap())
        )

        createRepository(publishableKey = "uk_12345").get(
            initializationMode = PaymentElementLoader.InitializationMode.DeferredIntent(
                intentConfiguration = PaymentSheet.IntentConfiguration(
                    mode = PaymentSheet.IntentConfiguration.Mode.Payment(
                        amount = 2000,
                        currency = "usd",
                    ),
                    paymentMethodTypes = listOf("card"),
                )
            ),
            customer = null,
            externalPaymentMethods = emptyList(),
            customPaymentMethods = emptyList(),
            savedPaymentMethodSelectionId = null,
            countryOverride = null,
            paymentSheetConfig = null,
        )

        verify(stripeNetworkClient).executeRequest(requestCaptor.capture())
        val request = requestCaptor.firstValue as ApiRequest
        val params = requireNotNull(request.params)
        assertThat(request.baseUrl).isEqualTo("https://api.stripe.com/v1/elements/sessions")
        assertThat(params["type"]).isEqualTo("deferred_intent")
        assertThat(params["mobile_app_id"]).isEqualTo(APP_ID)
    }

    private fun createRepository(
        publishableKey: String = ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY,
    ) = RealElementsSessionRepository(
        ApplicationProvider.getApplicationContext(),
        stripeNetworkClient,
        stripeRepository,
        { PaymentConfiguration(publishableKey) },
        testDispatcher,
        clientParams = TEST_CLIENT_PARAMS,
    )

    private suspend fun getNegotiatedElementsSession(): Result<ElementsSession> {
        return createRepository().get(
            initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(
                clientSecret = "client_secret",
            ),
            customer = null,
            externalPaymentMethods = emptyList(),
            customPaymentMethods = emptyList(),
            savedPaymentMethodSelectionId = null,
            countryOverride = null,
            paymentSheetConfig = PaymentSheetConfig(),
        )
    }

    private fun negotiatedElementsSessionResponse(
        bodyMajor: Int = MobileSessionContract.CONTRACT_MAJOR,
        bodyRevision: String = MobileSessionContract.CONTRACT_REVISION,
        responseHeaderValue: String? = MOBILE_SESSION_CONTRACT_HEADER_VALUE,
        includeMobilePaymentElement: Boolean = true,
    ): StripeResponse<String> {
        val responseBody = JSONObject(ElementsSessionFixtures.EXPANDED_PAYMENT_INTENT_JSON.toString())
        if (includeMobilePaymentElement) {
            responseBody.put(
                "mobile_payment_element",
                JSONObject()
                    .put(
                        "contract",
                        JSONObject()
                            .put("major", bodyMajor)
                            .put("revision", bodyRevision)
                    )
                    .put(
                        "payment_method_availability",
                        JSONObject().put("entries", JSONArray())
                    )
            )
        }
        val responseHeaders = responseHeaderValue?.let {
            mapOf(MOBILE_SESSION_CONTRACT_HEADER to listOf(it))
        }.orEmpty()
        return StripeResponse(200, responseBody.toString(), responseHeaders)
    }

    private fun assertMobileSessionContractFailure(
        result: Result<ElementsSession>,
        expectedErrorCode: MobileSessionContractException.ErrorCode,
    ) {
        assertThat(result.isFailure).isTrue()
        val error = result.exceptionOrNull() as MobileSessionContractException
        assertThat(error.errorCode).isEqualTo(expectedErrorCode)
    }

    private inline fun <T> withLocale(locale: Locale, block: () -> T): T {
        val original = Locale.getDefault()
        Locale.setDefault(locale)
        val result = block()
        Locale.setDefault(original)
        return result
    }

    companion object {
        private const val APP_ID = "com.app.id"
        private const val MOBILE_SESSION_ID = "session_123"
        private const val MOBILE_SESSION_CONTRACT_HEADER = "Stripe-Mobile-Session-Contract"
        private val MOBILE_SESSION_CONTRACT_HEADER_VALUE =
            "major=${MobileSessionContract.CONTRACT_MAJOR}; revision=${MobileSessionContract.CONTRACT_REVISION}"
        private val TEST_CLIENT_PARAMS = ElementsSessionClientParams(
            mobileAppId = APP_ID,
            mobileSessionIdProvider = { MOBILE_SESSION_ID },
        )
    }
}
