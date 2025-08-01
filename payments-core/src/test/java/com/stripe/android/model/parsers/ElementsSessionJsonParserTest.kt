package com.stripe.android.model.parsers

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.model.StripeJsonUtils
import com.stripe.android.core.model.parsers.ModelJsonParser
import com.stripe.android.core.utils.FeatureFlags
import com.stripe.android.model.CardBrand
import com.stripe.android.model.DeferredIntentParams
import com.stripe.android.model.ElementsSession
import com.stripe.android.model.ElementsSessionFixtures
import com.stripe.android.model.ElementsSessionFixtures.PAYMENT_METHODS_WITH_LINK_DETAILS
import com.stripe.android.model.ElementsSessionFixtures.createPaymentIntentWithCustomerSession
import com.stripe.android.model.ElementsSessionFixtures.createWithCustomPaymentMethods
import com.stripe.android.model.ElementsSessionParams
import com.stripe.android.model.LinkConsumerIncentive
import com.stripe.android.model.PassiveCaptchaParams
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.SetupIntent
import com.stripe.android.model.StripeIntent
import com.stripe.android.testing.FeatureFlagTestRule
import org.json.JSONObject
import org.junit.Rule
import org.junit.Test
import java.util.UUID

class ElementsSessionJsonParserTest {

    @get:Rule
    val incentivesFeatureFlagRule = FeatureFlagTestRule(
        featureFlag = FeatureFlags.instantDebitsIncentives,
        isEnabled = false,
    )

    @Test
    fun parsePaymentIntent_shouldCreateObjectWithOrderedPaymentMethods() {
        val elementsSession = ElementsSessionJsonParser(
            ElementsSessionParams.PaymentIntentType(
                clientSecret = "secret",
                externalPaymentMethods = emptyList(),
                customPaymentMethods = emptyList(),
                appId = APP_ID
            ),
            isLiveMode = false
        ).parse(
            ElementsSessionFixtures.EXPANDED_PAYMENT_INTENT_JSON
        )
        val orderedPaymentMethods =
            ModelJsonParser.jsonArrayToList(
                ElementsSessionFixtures.EXPANDED_PAYMENT_INTENT_JSON
                    .optJSONObject("payment_method_preference")!!
                    .optJSONArray("ordered_payment_method_types")
            )

        assertThat(elementsSession?.stripeIntent?.id)
            .isEqualTo("pi_3JTDhYIyGgrkZxL71IDUGKps")
        assertThat(elementsSession?.stripeIntent?.paymentMethodTypes)
            .containsExactlyElementsIn(orderedPaymentMethods)
            .inOrder()
        assertThat(elementsSession?.linkSettings?.linkPassthroughModeEnabled).isFalse()
    }

    @Test
    fun parseSetupIntent_shouldCreateObjectWithOrderedPaymentMethods() {
        val elementsSession = ElementsSessionJsonParser(
            ElementsSessionParams.SetupIntentType(
                clientSecret = "secret",
                externalPaymentMethods = emptyList(),
                customPaymentMethods = emptyList(),
                appId = APP_ID
            ),
            isLiveMode = false
        ).parse(
            ElementsSessionFixtures.EXPANDED_SETUP_INTENT_JSON
        )
        val orderedPaymentMethods =
            ModelJsonParser.jsonArrayToList(
                ElementsSessionFixtures.EXPANDED_SETUP_INTENT_JSON
                    .optJSONObject("payment_method_preference")!!
                    .optJSONArray("ordered_payment_method_types")
            )

        assertThat(elementsSession?.stripeIntent?.id)
            .isEqualTo("seti_1JTDqGIyGgrkZxL7reCXkpr5")
        assertThat(elementsSession?.stripeIntent?.paymentMethodTypes)
            .containsExactlyElementsIn(orderedPaymentMethods)
            .inOrder()
    }

    @Test
    fun parsePaymentIntent_shouldCreateObjectLinkFundingSources() {
        val elementsSession = ElementsSessionJsonParser(
            ElementsSessionParams.PaymentIntentType(
                clientSecret = "secret",
                externalPaymentMethods = emptyList(),
                customPaymentMethods = emptyList(),
                appId = APP_ID
            ),
            isLiveMode = false
        ).parse(
            ElementsSessionFixtures.EXPANDED_PAYMENT_INTENT_WITH_LINK_FUNDING_SOURCES_JSON
        )!!

        assertThat(elementsSession.stripeIntent.linkFundingSources)
            .containsExactly("card", "bank_account")
        assertThat(elementsSession.linkSettings?.linkPassthroughModeEnabled).isTrue()
    }

    @Test
    fun parsePaymentIntent_shouldCreateMapLinkFlags() {
        val elementsSession = ElementsSessionJsonParser(
            ElementsSessionParams.PaymentIntentType(
                clientSecret = "secret",
                externalPaymentMethods = emptyList(),
                customPaymentMethods = emptyList(),
                appId = APP_ID
            ),
            isLiveMode = false
        ).parse(
            ElementsSessionFixtures.EXPANDED_PAYMENT_INTENT_WITH_LINK_FUNDING_SOURCES_JSON
        )!!

        assertThat(elementsSession.linkSettings?.linkFlags).containsExactlyEntriesIn(
            mapOf(
                "link_authenticated_change_event_enabled" to false,
                "link_bank_incentives_enabled" to false,
                "link_bank_onboarding_enabled" to false,
                "link_email_verification_login_enabled" to false,
                "link_financial_incentives_experiment_enabled" to false,
                "link_local_storage_login_enabled" to true,
                "link_only_for_payment_method_types_enabled" to false,
                "link_passthrough_mode_enabled" to true,
            )
        )
        assertThat(elementsSession.linkSettings?.linkPassthroughModeEnabled).isTrue()
    }

    @Test
    fun parsePaymentIntent_shouldDisableLinkSignUp() {
        val elementsSession = ElementsSessionJsonParser(
            ElementsSessionParams.PaymentIntentType(
                clientSecret = "secret",
                externalPaymentMethods = emptyList(),
                customPaymentMethods = emptyList(),
                appId = APP_ID
            ),
            isLiveMode = false
        ).parse(
            ElementsSessionFixtures.EXPANDED_SETUP_INTENT_WITH_LINK_SIGNUP_DISABLED_JSON
        )!!

        assertThat(elementsSession.linkSettings?.disableLinkSignup).isTrue()
    }

    @Test
    fun parsePaymentIntent_shouldSetDisableLinkSignUpToFalse() {
        val elementsSession = ElementsSessionJsonParser(
            ElementsSessionParams.PaymentIntentType(
                clientSecret = "secret",
                externalPaymentMethods = emptyList(),
                customPaymentMethods = emptyList(),
                appId = APP_ID
            ),
            isLiveMode = false
        ).parse(
            ElementsSessionFixtures.EXPANDED_SETUP_INTENT_WITH_LINK_SIGNUP_DISABLED_FLAG_FALSE_JSON
        )!!

        assertThat(elementsSession.linkSettings?.disableLinkSignup).isFalse()
    }

    @Test
    fun parseSetupIntent_shouldCreateObjectLinkFundingSources() {
        val elementsSession = ElementsSessionJsonParser(
            ElementsSessionParams.SetupIntentType(
                clientSecret = "secret",
                externalPaymentMethods = emptyList(),
                customPaymentMethods = emptyList(),
                appId = APP_ID
            ),
            isLiveMode = false
        ).parse(
            ElementsSessionFixtures.EXPANDED_SETUP_INTENT_WITH_LINK_FUNDING_SOURCES_JSON
        )!!

        assertThat(elementsSession.stripeIntent.linkFundingSources)
            .containsExactly("card", "bank_account")
        assertThat(elementsSession.linkSettings?.linkPassthroughModeEnabled).isFalse()
    }

    @Test
    fun `Test ordered payment methods returned in PI payment_method_type variable`() {
        val parsedData = ElementsSessionJsonParser(
            ElementsSessionParams.PaymentIntentType(
                clientSecret = "secret",
                externalPaymentMethods = emptyList(),
                customPaymentMethods = emptyList(),
                appId = APP_ID
            ),
            isLiveMode = false
        ).parse(
            JSONObject(
                ElementsSessionFixtures.PI_WITH_CARD_AFTERPAY_AU_BECS
            )
        )
        assertThat(parsedData?.stripeIntent?.paymentMethodTypes).isEqualTo(
            listOf(
                "au_becs_debit",
                "afterpay_clearpay",
                "card"
            )
        )
    }

    @Test
    fun `Test ordered payment methods not required in response`() {
        val parsedData = ElementsSessionJsonParser(
            ElementsSessionParams.PaymentIntentType(
                clientSecret = "secret",
                externalPaymentMethods = emptyList(),
                customPaymentMethods = emptyList(),
                appId = APP_ID
            ),
            isLiveMode = false
        ).parse(
            JSONObject(
                ElementsSessionFixtures.PI_WITH_CARD_AFTERPAY_AU_BECS_NO_ORDERED_LPMS
            )
        )
        // This is the order in the original payment intent
        assertThat(parsedData?.stripeIntent?.paymentMethodTypes).isEqualTo(
            listOf(
                "card",
                "afterpay_clearpay",
                "au_becs_debit"
            )
        )
    }

    @Test
    fun `Test ordered payment methods is not required`() {
        val parsedData = ElementsSessionJsonParser(
            ElementsSessionParams.PaymentIntentType(
                clientSecret = "secret",
                externalPaymentMethods = emptyList(),
                customPaymentMethods = emptyList(),
                appId = APP_ID
            ),
            isLiveMode = false
        ).parse(
            JSONObject(
                """
                    {
                      "payment_method_preference": {
                         "object": "payment_method_preference",
                         "payment_intent": {
                         }
                      }
                    }
                """.trimIndent()
            )
        )
        assertThat(parsedData?.stripeIntent).isNull()
    }

    @Test
    fun `Test fail to parse the payment intent`() {
        val parsedData = ElementsSessionJsonParser(
            ElementsSessionParams.PaymentIntentType(
                clientSecret = "secret",
                externalPaymentMethods = emptyList(),
                customPaymentMethods = emptyList(),
                appId = APP_ID
            ),
            isLiveMode = false
        ).parse(
            JSONObject(
                """
                    {
                      "payment_method_preference": {
                         "object": "payment_method_preference",
                         "payment_intent": {
                         }
                      }
                    }
                """.trimIndent()
            )
        )
        assertThat(parsedData?.stripeIntent).isNull()
    }

    @Test
    fun `Test fail to find the payment intent`() {
        val parsedData = ElementsSessionJsonParser(
            ElementsSessionParams.PaymentIntentType(
                clientSecret = "secret",
                externalPaymentMethods = emptyList(),
                customPaymentMethods = emptyList(),
                appId = APP_ID
            ),
            isLiveMode = false
        ).parse(
            JSONObject(
                """
                    {
                      "payment_method_preference": {
                         "object": "payment_method_preference"
                      }
                    }
                """.trimIndent()
            )
        )
        assertThat(parsedData?.stripeIntent).isNull()
    }

    @Test
    fun `Test PI with country code`() {
        val parsedData = ElementsSessionJsonParser(
            ElementsSessionParams.PaymentIntentType(
                clientSecret = "secret",
                externalPaymentMethods = emptyList(),
                customPaymentMethods = emptyList(),
                appId = APP_ID
            ),
            isLiveMode = false
        ).parse(
            ElementsSessionFixtures.EXPANDED_PAYMENT_INTENT_JSON
        )

        val countryCode = when (val intent = parsedData?.stripeIntent) {
            is PaymentIntent -> intent.countryCode
            is SetupIntent -> intent.countryCode
            null -> null
        }

        assertThat(countryCode).isEqualTo("US")
    }

    @Test
    fun `Test SI with country code`() {
        val parsedData = ElementsSessionJsonParser(
            ElementsSessionParams.SetupIntentType(
                clientSecret = "secret",
                externalPaymentMethods = emptyList(),
                customPaymentMethods = emptyList(),
                appId = APP_ID
            ),
            isLiveMode = false
        ).parse(
            ElementsSessionFixtures.EXPANDED_SETUP_INTENT_JSON
        )

        val countryCode = when (val intent = parsedData?.stripeIntent) {
            is PaymentIntent -> intent.countryCode
            is SetupIntent -> intent.countryCode
            null -> null
        }

        assertThat(countryCode).isEqualTo("US")
    }

    @Test
    fun `Test omitted setup intent`() {
        val parsedData = ElementsSessionJsonParser(
            ElementsSessionParams.PaymentIntentType(
                clientSecret = "secret",
                externalPaymentMethods = emptyList(),
                customPaymentMethods = emptyList(),
                appId = APP_ID
            ),
            isLiveMode = false
        ).parse(
            ElementsSessionFixtures.EXPANDED_PAYMENT_INTENT_JSON
        )

        assertThat(
            runCatching {
                UUID.fromString(parsedData?.elementsSessionId)
            }.isSuccess
        ).isTrue()
    }

    @Test
    fun `Test deferred PaymentIntent`() {
        val pmoMap = mapOf(
            "card" to mapOf(
                "setup_future_usage" to "off_session"
            ),
            "affirm" to mapOf(
                "setup_future_usage" to "none"
            )
        )
        val data = ElementsSessionJsonParser(
            ElementsSessionParams.DeferredIntentType(
                deferredIntentParams = DeferredIntentParams(
                    mode = DeferredIntentParams.Mode.Payment(
                        amount = 2000,
                        currency = "usd",
                        captureMethod = PaymentIntent.CaptureMethod.Automatic,
                        setupFutureUsage = null,
                        paymentMethodOptionsJsonString = StripeJsonUtils.mapToJsonObject(pmoMap).toString()
                    ),
                    paymentMethodTypes = emptyList(),
                    paymentMethodConfigurationId = null,
                    onBehalfOf = null,
                ),
                externalPaymentMethods = emptyList(),
                customPaymentMethods = emptyList(),
                appId = APP_ID
            ),
            isLiveMode = false,
            timeProvider = { 1 }
        ).parse(
            ElementsSessionFixtures.DEFERRED_INTENT_JSON
        )

        val deferredIntent = data?.stripeIntent

        assertThat(data?.elementsSessionId).isEqualTo("elements_session_1t6ejApXCS5")
        assertThat(deferredIntent).isNotNull()
        assertThat(deferredIntent).isEqualTo(
            PaymentIntent(
                id = "elements_session_1t6ejApXCS5",
                clientSecret = null,
                amount = 2000L,
                currency = "usd",
                captureMethod = PaymentIntent.CaptureMethod.Automatic,
                countryCode = "CA",
                created = 1,
                isLiveMode = false,
                setupFutureUsage = null,
                unactivatedPaymentMethods = listOf(),
                paymentMethodTypes = listOf("card", "link", "cashapp"),
                linkFundingSources = listOf("card"),
                paymentMethodOptionsJsonString = "{\"affirm\":{\"setup_future_usage\":\"none\"}," +
                    "\"card\":{\"setup_future_usage\":\"off_session\"}}"
            )
        )
    }

    @Test
    fun `Test deferred SetupIntent`() {
        val data = ElementsSessionJsonParser(
            ElementsSessionParams.DeferredIntentType(
                deferredIntentParams = DeferredIntentParams(
                    mode = DeferredIntentParams.Mode.Setup(
                        currency = "usd",
                        setupFutureUsage = StripeIntent.Usage.OffSession,
                    ),
                    paymentMethodTypes = emptyList(),
                    paymentMethodConfigurationId = null,
                    onBehalfOf = null,
                ),
                externalPaymentMethods = emptyList(),
                customPaymentMethods = emptyList(),
                appId = APP_ID
            ),
            isLiveMode = false,
            timeProvider = { 1 }
        ).parse(
            ElementsSessionFixtures.DEFERRED_INTENT_JSON
        )

        val deferredIntent = data?.stripeIntent

        assertThat(deferredIntent).isNotNull()
        assertThat(deferredIntent).isEqualTo(
            SetupIntent(
                id = "elements_session_1t6ejApXCS5",
                clientSecret = null,
                cancellationReason = null,
                description = null,
                nextActionData = null,
                paymentMethodId = null,
                paymentMethod = null,
                status = null,
                countryCode = "CA",
                created = 1,
                isLiveMode = false,
                usage = StripeIntent.Usage.OffSession,
                unactivatedPaymentMethods = listOf(),
                paymentMethodTypes = listOf("card", "link", "cashapp"),
                linkFundingSources = listOf("card")
            )
        )
    }

    @Test
    fun `Is eligible for CBC if response says so`() {
        val parser = ElementsSessionJsonParser(
            ElementsSessionParams.PaymentIntentType(
                clientSecret = "secret",
                externalPaymentMethods = emptyList(),
                customPaymentMethods = emptyList(),
                appId = APP_ID
            ),
            isLiveMode = false,
        )

        val intent = ElementsSessionFixtures.EXPANDED_PAYMENT_INTENT_JSON_WITH_CBC_ELIGIBLE
        val session = parser.parse(intent)

        assertThat(session?.cardBrandChoice?.eligible).isTrue()
        assertThat(session?.cardBrandChoice?.preferredNetworks).isEqualTo(listOf("cartes_bancaires"))
    }

    @Test
    fun `Is not eligible for CBC if response says so`() {
        val parser = ElementsSessionJsonParser(
            ElementsSessionParams.PaymentIntentType(
                clientSecret = "secret",
                externalPaymentMethods = emptyList(),
                customPaymentMethods = emptyList(),
                appId = APP_ID
            ),
            isLiveMode = false,
        )

        val intent = ElementsSessionFixtures.EXPANDED_PAYMENT_INTENT_JSON_WITH_CBC_NOT_ELIGIBLE
        val session = parser.parse(intent)

        assertThat(session?.cardBrandChoice?.eligible).isFalse()
        assertThat(session?.cardBrandChoice?.preferredNetworks).isEqualTo(listOf("cartes_bancaires"))
    }

    @Test
    fun `Is card brand choice is null if no info in the response`() {
        val parser = ElementsSessionJsonParser(
            ElementsSessionParams.PaymentIntentType(
                clientSecret = "secret",
                externalPaymentMethods = emptyList(),
                customPaymentMethods = emptyList(),
                appId = APP_ID
            ),
            isLiveMode = false,
        )

        val intent = ElementsSessionFixtures.EXPANDED_PAYMENT_INTENT_JSON
        val session = parser.parse(intent)

        assertThat(session?.cardBrandChoice).isNull()
    }

    @Test
    fun `Preferred networks is empty if not passed through response`() {
        val parser = ElementsSessionJsonParser(
            ElementsSessionParams.SetupIntentType(
                clientSecret = "secret",
                externalPaymentMethods = emptyList(),
                customPaymentMethods = emptyList(),
                appId = APP_ID
            ),
            isLiveMode = false,
        )

        val intent = ElementsSessionFixtures.EXPANDED_SETUP_INTENT_JSON_WITH_CBC_ELIGIBLE_BUT_NO_NETWORKS
        val session = parser.parse(intent)

        assertThat(session?.cardBrandChoice?.eligible).isTrue()
        assertThat(session?.cardBrandChoice?.preferredNetworks).isEmpty()
    }

    @Test
    fun `ElementsSesssion has no external payment methods when they are not included in response`() {
        val parser = ElementsSessionJsonParser(
            ElementsSessionParams.PaymentIntentType(
                clientSecret = "secret",
                externalPaymentMethods = listOf("external_venmo"),
                customPaymentMethods = listOf(),
                appId = APP_ID
            ),
            isLiveMode = false,
        )

        val intent = ElementsSessionFixtures.EXPANDED_PAYMENT_INTENT_JSON
        val session = parser.parse(intent)

        assertThat(session?.externalPaymentMethodData).isNull()
    }

    @Test
    fun `ElementsSession has external payment methods when they are included in response`() {
        val venmo = "external_venmo"
        val parser = ElementsSessionJsonParser(
            ElementsSessionParams.PaymentIntentType(
                clientSecret = "secret",
                externalPaymentMethods = listOf(venmo),
                customPaymentMethods = listOf(),
                appId = APP_ID
            ),
            isLiveMode = false,
        )

        val intent = ElementsSessionFixtures.PAYMENT_INTENT_WITH_EXTERNAL_VENMO_JSON
        val session = parser.parse(intent)

        assertThat(session?.externalPaymentMethodData).contains(venmo)
    }

    @Test
    fun `ElementsSession has ordered payment method types and wallets when in response`() {
        val parser = ElementsSessionJsonParser(
            ElementsSessionParams.PaymentIntentType(
                clientSecret = "secret",
                externalPaymentMethods = emptyList(),
                customPaymentMethods = emptyList(),
                appId = APP_ID
            ),
            isLiveMode = false,
        )

        val intent = JSONObject(ElementsSessionFixtures.PI_WITH_CARD_AFTERPAY_AU_BECS)
        val session = parser.parse(intent)

        assertThat(session?.orderedPaymentMethodTypesAndWallets).containsExactly(
            "card",
            "apple_pay",
            "google_pay",
            "afterpay_clearpay",
            "au_becs_debit",
        )
    }

    @Test
    fun `ElementsSession has custom payment methods when they are included in response`() {
        val parser = ElementsSessionJsonParser(
            ElementsSessionParams.PaymentIntentType(
                clientSecret = "secret",
                externalPaymentMethods = listOf(),
                customPaymentMethods = listOf("cpmt_123", "cpmt_456"),
                appId = APP_ID
            ),
            isLiveMode = false,
        )

        val intent = createWithCustomPaymentMethods(
            """
                [
                    {
                        "type": "cpmt_123",
                        "display_name": "CPM #1",
                        "logo_url": "https://image1"
                    },
                    {
                        "type": "cpmt_456",
                        "display_name": "CPM #2",
                        "logo_url": "https://image2"
                    }
                ]
            """.trimIndent()
        )

        val session = parser.parse(intent)

        assertThat(session?.customPaymentMethods).containsExactly(
            ElementsSession.CustomPaymentMethod.Available(
                type = "cpmt_123",
                displayName = "CPM #1",
                logoUrl = "https://image1",
            ),
            ElementsSession.CustomPaymentMethod.Available(
                type = "cpmt_456",
                displayName = "CPM #2",
                logoUrl = "https://image2",
            ),
        )
    }

    @Test
    fun `ElementsSession has no custom payment methods when they are not included in response`() {
        val parser = ElementsSessionJsonParser(
            ElementsSessionParams.PaymentIntentType(
                clientSecret = "secret",
                externalPaymentMethods = listOf(),
                customPaymentMethods = listOf(),
                appId = APP_ID
            ),
            isLiveMode = false,
        )

        val intent = createWithCustomPaymentMethods(customPaymentMethodData = "null")

        val session = parser.parse(intent)

        assertThat(session?.customPaymentMethods).isEmpty()
    }

    @Test
    fun `ElementsSession has expected customer session information in the response`() {
        val parser = ElementsSessionJsonParser(
            ElementsSessionParams.PaymentIntentType(
                clientSecret = "secret",
                customerSessionClientSecret = "customer_session_client_secret",
                externalPaymentMethods = emptyList(),
                customPaymentMethods = emptyList(),
                appId = APP_ID
            ),
            isLiveMode = false,
        )

        val intent = createPaymentIntentWithCustomerSession()
        val elementsSession = parser.parse(intent)

        assertThat(elementsSession?.customer).isEqualTo(
            ElementsSession.Customer(
                session = ElementsSession.Customer.Session(
                    id = "cuss_123",
                    apiKey = "ek_test_1234",
                    apiKeyExpiry = 1713890664,
                    customerId = "cus_1",
                    liveMode = false,
                    components = ElementsSession.Customer.Components(
                        mobilePaymentElement = ElementsSession.Customer.Components.MobilePaymentElement.Enabled(
                            isPaymentMethodSaveEnabled = false,
                            isPaymentMethodRemoveEnabled = true,
                            canRemoveLastPaymentMethod = true,
                            allowRedisplayOverride = PaymentMethod.AllowRedisplay.LIMITED,
                            isPaymentMethodSetAsDefaultEnabled = false,
                        ),
                        customerSheet = ElementsSession.Customer.Components.CustomerSheet.Enabled(
                            isPaymentMethodRemoveEnabled = true,
                            canRemoveLastPaymentMethod = true,
                            isPaymentMethodSyncDefaultEnabled = false,
                        ),
                    )
                ),
                defaultPaymentMethod = "pm_123",
                paymentMethods = listOf(
                    PaymentMethod(
                        id = "pm_123",
                        customerId = "cus_1",
                        type = PaymentMethod.Type.Card,
                        code = "card",
                        created = 1550757934255,
                        liveMode = false,
                        billingDetails = null,
                        card = PaymentMethod.Card(
                            brand = CardBrand.Visa,
                            last4 = "4242",
                            expiryMonth = 8,
                            expiryYear = 2032,
                            country = "US",
                            funding = "credit",
                            fingerprint = "fingerprint123",
                            checks = PaymentMethod.Card.Checks(
                                addressLine1Check = "unchecked",
                                cvcCheck = "unchecked",
                                addressPostalCodeCheck = null,
                            ),
                            threeDSecureUsage = PaymentMethod.Card.ThreeDSecureUsage(
                                isSupported = true
                            )
                        )
                    )
                )
            )
        )
    }

    @Test
    fun `ElementsSession has expected CS information in the response if 'mobile_payment_element' is null`() {
        val parser = ElementsSessionJsonParser(
            ElementsSessionParams.PaymentIntentType(
                clientSecret = "secret",
                customerSessionClientSecret = "customer_session_client_secret",
                externalPaymentMethods = emptyList(),
                customPaymentMethods = emptyList(),
                appId = APP_ID
            ),
            isLiveMode = false,
        )

        val intent = createPaymentIntentWithCustomerSession(
            passMobilePaymentElement = false,
        )
        val elementsSession = parser.parse(intent)

        assertThat(elementsSession?.customer).isEqualTo(
            ElementsSession.Customer(
                session = ElementsSession.Customer.Session(
                    id = "cuss_123",
                    apiKey = "ek_test_1234",
                    apiKeyExpiry = 1713890664,
                    customerId = "cus_1",
                    liveMode = false,
                    components = ElementsSession.Customer.Components(
                        mobilePaymentElement = ElementsSession.Customer.Components.MobilePaymentElement.Disabled,
                        customerSheet = ElementsSession.Customer.Components.CustomerSheet.Enabled(
                            isPaymentMethodRemoveEnabled = true,
                            canRemoveLastPaymentMethod = true,
                            isPaymentMethodSyncDefaultEnabled = false,
                        ),
                    )
                ),
                defaultPaymentMethod = "pm_123",
                paymentMethods = listOf(
                    PaymentMethod(
                        id = "pm_123",
                        customerId = "cus_1",
                        type = PaymentMethod.Type.Card,
                        code = "card",
                        created = 1550757934255,
                        liveMode = false,
                        billingDetails = null,
                        card = PaymentMethod.Card(
                            brand = CardBrand.Visa,
                            last4 = "4242",
                            expiryMonth = 8,
                            expiryYear = 2032,
                            country = "US",
                            funding = "credit",
                            fingerprint = "fingerprint123",
                            checks = PaymentMethod.Card.Checks(
                                addressLine1Check = "unchecked",
                                cvcCheck = "unchecked",
                                addressPostalCodeCheck = null,
                            ),
                            threeDSecureUsage = PaymentMethod.Card.ThreeDSecureUsage(
                                isSupported = true
                            )
                        )
                    )
                )
            )
        )
    }

    @Test
    fun `ElementsSession has expected customer session information in the response if 'customer_sheet' is null`() {
        val parser = ElementsSessionJsonParser(
            ElementsSessionParams.PaymentIntentType(
                clientSecret = "secret",
                customerSessionClientSecret = "customer_session_client_secret",
                externalPaymentMethods = emptyList(),
                customPaymentMethods = emptyList(),
                appId = APP_ID
            ),
            isLiveMode = false,
        )

        val intent = createPaymentIntentWithCustomerSession(
            passCustomerSheet = false,
        )
        val elementsSession = parser.parse(intent)

        assertThat(elementsSession?.customer).isEqualTo(
            ElementsSession.Customer(
                session = ElementsSession.Customer.Session(
                    id = "cuss_123",
                    apiKey = "ek_test_1234",
                    apiKeyExpiry = 1713890664,
                    customerId = "cus_1",
                    liveMode = false,
                    components = ElementsSession.Customer.Components(
                        mobilePaymentElement = ElementsSession.Customer.Components.MobilePaymentElement.Enabled(
                            isPaymentMethodSaveEnabled = false,
                            isPaymentMethodRemoveEnabled = true,
                            canRemoveLastPaymentMethod = true,
                            allowRedisplayOverride = PaymentMethod.AllowRedisplay.LIMITED,
                            isPaymentMethodSetAsDefaultEnabled = false,
                        ),
                        customerSheet = ElementsSession.Customer.Components.CustomerSheet.Disabled,
                    )
                ),
                defaultPaymentMethod = "pm_123",
                paymentMethods = listOf(
                    PaymentMethod(
                        id = "pm_123",
                        customerId = "cus_1",
                        type = PaymentMethod.Type.Card,
                        code = "card",
                        created = 1550757934255,
                        liveMode = false,
                        billingDetails = null,
                        card = PaymentMethod.Card(
                            brand = CardBrand.Visa,
                            last4 = "4242",
                            expiryMonth = 8,
                            expiryYear = 2032,
                            country = "US",
                            funding = "credit",
                            fingerprint = "fingerprint123",
                            checks = PaymentMethod.Card.Checks(
                                addressLine1Check = "unchecked",
                                cvcCheck = "unchecked",
                                addressPostalCodeCheck = null,
                            ),
                            threeDSecureUsage = PaymentMethod.Card.ThreeDSecureUsage(
                                isSupported = true
                            )
                        )
                    )
                )
            )
        )
    }

    @Test
    fun `ElementsSession has 'unspecified' allow redisplay override`() {
        allowRedisplayTest(
            rawAllowRedisplayValue = "unspecified",
            allowRedisplay = PaymentMethod.AllowRedisplay.UNSPECIFIED,
        )
    }

    @Test
    fun `ElementsSession has 'limited' allow redisplay override`() {
        allowRedisplayTest(
            rawAllowRedisplayValue = "limited",
            allowRedisplay = PaymentMethod.AllowRedisplay.LIMITED,
        )
    }

    @Test
    fun `ElementsSession has 'always' allow redisplay override`() {
        allowRedisplayTest(
            rawAllowRedisplayValue = "always",
            allowRedisplay = PaymentMethod.AllowRedisplay.ALWAYS,
        )
    }

    @Test
    fun `ElementsSession has null allow redisplay override`() {
        allowRedisplayTest(
            rawAllowRedisplayValue = null,
            allowRedisplay = null,
        )
    }

    @Test
    fun `when 'payment_method_remove' is 'enabled', 'canRemovePaymentMethods' should be true`() {
        permissionsTest(
            paymentMethodRemoveFeatureValue = "enabled",
            canRemovePaymentMethods = true,
        )
    }

    @Test
    fun `when 'payment_method_remove' is 'disabled', 'canRemovePaymentMethods' should be false`() {
        permissionsTest(
            paymentMethodRemoveFeatureValue = "disabled",
            canRemovePaymentMethods = false,
        )
    }

    @Test
    fun `when 'payment_method_remove' is unknown value, 'canRemovePaymentMethods' should be false`() {
        permissionsTest(
            paymentMethodRemoveFeatureValue = "something",
            canRemovePaymentMethods = false,
        )
    }

    @Test
    fun `when 'payment_method_remove_last' is 'enabled', 'canRemoveLastPaymentMethod' should be true`() {
        permissionsTest(
            paymentMethodRemoveLastFeatureValue = "enabled",
            canRemoveLastPaymentMethod = true,
        )
    }

    @Test
    fun `when 'payment_method_remove_last' is 'disabled', 'canRemoveLastPaymentMethod' should be false`() {
        permissionsTest(
            paymentMethodRemoveLastFeatureValue = "disabled",
            canRemoveLastPaymentMethod = false,
        )
    }

    @Test
    fun `when 'payment_method_remove_last' is unknown value, 'canRemoveLastPaymentMethod' should be false`() {
        permissionsTest(
            paymentMethodRemoveLastFeatureValue = "something",
            canRemoveLastPaymentMethod = false,
        )
    }

    @Test
    fun `when 'payment_method_set_as_default' is enabled, 'isSetAsDefaultEnabled' should be true`() {
        testPaymentMethodSetAsDefault(
            paymentMethodSetAsDefaultValue = "enabled",
            expectedIsSetAsDefaultEnabledValue = true,
        )
    }

    @Test
    fun `when 'payment_method_set_as_default' is disabled, 'isSetAsDefaultEnabled' should be false`() {
        testPaymentMethodSetAsDefault(
            paymentMethodSetAsDefaultValue = "disabled",
            expectedIsSetAsDefaultEnabledValue = false,
        )
    }

    @Test
    fun `when 'payment_method_set_as_default' is invalid value, 'isSetAsDefaultEnabled' should be false`() {
        testPaymentMethodSetAsDefault(
            paymentMethodSetAsDefaultValue = "not an accepted value",
            expectedIsSetAsDefaultEnabledValue = false,
        )
    }

    private fun testPaymentMethodSetAsDefault(
        paymentMethodSetAsDefaultValue: String,
        expectedIsSetAsDefaultEnabledValue: Boolean,
    ) {
        val parser = ElementsSessionJsonParser(
            ElementsSessionParams.PaymentIntentType(
                clientSecret = "secret",
                customerSessionClientSecret = "customer_session_client_secret",
                externalPaymentMethods = emptyList(),
                customPaymentMethods = emptyList(),
                appId = APP_ID
            ),
            isLiveMode = false,
        )

        val intent = createPaymentIntentWithCustomerSession(
            paymentMethodSetAsDefaultFeature = paymentMethodSetAsDefaultValue
        )

        val elementsSession = parser.parse(intent)

        val mobilePaymentElementComponent = elementsSession?.customer?.session?.components?.mobilePaymentElement

        assertThat(mobilePaymentElementComponent)
            .isInstanceOf(ElementsSession.Customer.Components.MobilePaymentElement.Enabled::class.java)

        val enabledPaymentElementComponent = mobilePaymentElementComponent as?
            ElementsSession.Customer.Components.MobilePaymentElement.Enabled

        assertThat(enabledPaymentElementComponent?.isPaymentMethodSetAsDefaultEnabled).isEqualTo(
            expectedIsSetAsDefaultEnabledValue
        )
    }

    @Test
    fun `when 'payment_method_sync_default' is enabled, 'isPaymentMethodSyncDefaultEnabled' should be true`() {
        testPaymentMethodSyncDefault(
            paymentMethodSyncDefaultValue = "enabled",
            expectedIsSyncDefaultEnabledValue = true,
        )
    }

    @Test
    fun `when 'payment_method_sync_default' is disabled, 'isPaymentMethodSyncDefaultEnabled' should be false`() {
        testPaymentMethodSyncDefault(
            paymentMethodSyncDefaultValue = "disabled",
            expectedIsSyncDefaultEnabledValue = false,
        )
    }

    @Test
    fun `when 'payment_method_sync_default' is invalid value, 'isPaymentMethodSyncDefaultEnabled' should be false`() {
        testPaymentMethodSyncDefault(
            paymentMethodSyncDefaultValue = "not an accepted value",
            expectedIsSyncDefaultEnabledValue = false,
        )
    }

    private fun testPaymentMethodSyncDefault(
        paymentMethodSyncDefaultValue: String,
        expectedIsSyncDefaultEnabledValue: Boolean,
    ) {
        val parser = ElementsSessionJsonParser(
            ElementsSessionParams.PaymentIntentType(
                clientSecret = "secret",
                customerSessionClientSecret = "customer_session_client_secret",
                externalPaymentMethods = emptyList(),
                customPaymentMethods = emptyList(),
                appId = APP_ID
            ),
            isLiveMode = false,
        )

        val intent = createPaymentIntentWithCustomerSession(
            paymentMethodSyncDefaultFeature = paymentMethodSyncDefaultValue,
        )

        val elementsSession = parser.parse(intent)

        val customerSheetComponent = elementsSession?.customer?.session?.components?.customerSheet

        assertThat(customerSheetComponent)
            .isInstanceOf(ElementsSession.Customer.Components.CustomerSheet.Enabled::class.java)

        val enabledCustomerSheetComponent = customerSheetComponent as?
            ElementsSession.Customer.Components.CustomerSheet.Enabled

        assertThat(enabledCustomerSheetComponent?.isPaymentMethodSyncDefaultEnabled).isEqualTo(
            expectedIsSyncDefaultEnabledValue
        )
    }

    @Suppress("LongMethod")
    @Test
    fun `ElementsSession has expected customer session information with customer sheet component in the response`() {
        val parser = ElementsSessionJsonParser(
            ElementsSessionParams.PaymentIntentType(
                clientSecret = "secret",
                customerSessionClientSecret = "customer_session_client_secret",
                externalPaymentMethods = emptyList(),
                customPaymentMethods = emptyList(),
                appId = APP_ID
            ),
            isLiveMode = false,
        )

        val intent = ElementsSessionFixtures.EXPANDED_PAYMENT_INTENT_WITH_CUSTOMER_SESSION_AND_CUSTOMER_SHEET_COMPONENT
        val elementsSession = parser.parse(intent)

        assertThat(elementsSession?.customer).isEqualTo(
            ElementsSession.Customer(
                session = ElementsSession.Customer.Session(
                    id = "cuss_123",
                    apiKey = "ek_test_1234",
                    apiKeyExpiry = 1713890664,
                    customerId = "cus_1",
                    liveMode = false,
                    components = ElementsSession.Customer.Components(
                        mobilePaymentElement = ElementsSession.Customer.Components.MobilePaymentElement.Disabled,
                        customerSheet = ElementsSession.Customer.Components.CustomerSheet.Enabled(
                            isPaymentMethodRemoveEnabled = true,
                            canRemoveLastPaymentMethod = true,
                            isPaymentMethodSyncDefaultEnabled = false,
                        ),
                    )
                ),
                defaultPaymentMethod = "pm_123",
                paymentMethods = listOf(
                    PaymentMethod(
                        id = "pm_123",
                        customerId = "cus_1",
                        type = PaymentMethod.Type.Card,
                        code = "card",
                        created = 1550757934255,
                        liveMode = false,
                        billingDetails = null,
                        card = PaymentMethod.Card(
                            brand = CardBrand.Visa,
                            last4 = "4242",
                            expiryMonth = 8,
                            expiryYear = 2032,
                            country = "US",
                            funding = "credit",
                            fingerprint = "fingerprint123",
                            checks = PaymentMethod.Card.Checks(
                                addressLine1Check = "unchecked",
                                cvcCheck = "unchecked",
                                addressPostalCodeCheck = null,
                            ),
                            threeDSecureUsage = PaymentMethod.Card.ThreeDSecureUsage(
                                isSupported = true
                            )
                        )
                    )
                )
            )
        )
    }

    @Test
    fun parsePaymentIntent_shouldCreateObjectWithCorrectGooglePayEnabled() {
        val parser = ElementsSessionJsonParser(
            ElementsSessionParams.PaymentIntentType(
                clientSecret = "secret",
                externalPaymentMethods = emptyList(),
                customPaymentMethods = emptyList(),
                appId = APP_ID
            ),
            isLiveMode = false
        )

        fun assertIsGooglePayEnabled(expectedValue: Boolean, jsonTransform: JSONObject.() -> Unit) {
            val json = ElementsSessionFixtures.EXPANDED_PAYMENT_INTENT_JSON
            json.jsonTransform()
            assertThat(parser.parse(json)?.isGooglePayEnabled).isEqualTo(expectedValue)
        }

        assertIsGooglePayEnabled(true) { remove(ElementsSessionJsonParser.FIELD_GOOGLE_PAY_PREFERENCE) }
        assertIsGooglePayEnabled(true) { put(ElementsSessionJsonParser.FIELD_GOOGLE_PAY_PREFERENCE, "enabled") }
        assertIsGooglePayEnabled(true) { put(ElementsSessionJsonParser.FIELD_GOOGLE_PAY_PREFERENCE, "unknown") }
        assertIsGooglePayEnabled(false) { put(ElementsSessionJsonParser.FIELD_GOOGLE_PAY_PREFERENCE, "disabled") }
    }

    @Test
    fun parsePaymentIntent_excludesUnactivatedPaymentMethodTypesInLiveMode() {
        val elementsSession = ElementsSessionJsonParser(
            ElementsSessionParams.PaymentIntentType(
                clientSecret = "secret",
                externalPaymentMethods = emptyList(),
                customPaymentMethods = emptyList(),
                appId = APP_ID
            ),
            isLiveMode = true,
        ).parse(
            JSONObject(
                ElementsSessionFixtures.PI_WITH_CARD_AFTERPAY_AU_BECS
            )
        )

        assertThat(elementsSession?.stripeIntent?.unactivatedPaymentMethods).containsExactly("au_becs_debit")
    }

    @Test
    fun `Parses Link consumer incentives if feature flag is enabled`() {
        incentivesFeatureFlagRule.setEnabled(true)

        val elementsSession = ElementsSessionJsonParser(
            ElementsSessionParams.PaymentIntentType(
                clientSecret = "secret",
                externalPaymentMethods = emptyList(),
                customPaymentMethods = emptyList(),
                appId = APP_ID
            ),
            isLiveMode = true,
        ).parse(
            ElementsSessionFixtures.EXPANDED_PAYMENT_INTENT_WITH_LINK_INCENTIVE_JSON
        )

        assertThat(elementsSession?.linkSettings?.linkConsumerIncentive).isEqualTo(
            LinkConsumerIncentive(
                incentiveParams = LinkConsumerIncentive.IncentiveParams(
                    paymentMethod = "link_instant_debits",
                ),
                incentiveDisplayText = "$5",
            )
        )
    }

    @Test
    fun `Does not parse Link consumer incentives if feature flag is disabled`() {
        incentivesFeatureFlagRule.setEnabled(false)

        val elementsSession = ElementsSessionJsonParser(
            ElementsSessionParams.PaymentIntentType(
                clientSecret = "secret",
                externalPaymentMethods = emptyList(),
                customPaymentMethods = emptyList(),
                appId = APP_ID
            ),
            isLiveMode = true,
        ).parse(
            ElementsSessionFixtures.EXPANDED_PAYMENT_INTENT_WITH_LINK_INCENTIVE_JSON
        )

        assertThat(elementsSession?.linkSettings?.linkConsumerIncentive).isNull()
    }

    @Test
    fun `Parses Native Link flags as enabled`() {
        val elementsSession = ElementsSessionJsonParser(
            ElementsSessionParams.PaymentIntentType(
                clientSecret = "secret",
                externalPaymentMethods = emptyList(),
                customPaymentMethods = emptyList(),
                appId = APP_ID
            ),
            isLiveMode = true,
        ).parse(
            ElementsSessionFixtures.EXPANDED_PAYMENT_INTENT_WITH_NATIVE_LINK_FLAGS_ENABLED_JSON
        )

        assertThat(elementsSession?.linkSettings?.useAttestationEndpoints).isTrue()
        assertThat(elementsSession?.linkSettings?.suppress2faModal).isTrue()
    }

    @Test
    fun `Parses Native Link flags as disabled`() {
        val elementsSession = ElementsSessionJsonParser(
            ElementsSessionParams.PaymentIntentType(
                clientSecret = "secret",
                externalPaymentMethods = emptyList(),
                customPaymentMethods = emptyList(),
                appId = APP_ID
            ),
            isLiveMode = true,
        ).parse(
            ElementsSessionFixtures.EXPANDED_PAYMENT_INTENT_WITH_NATIVE_LINK_FLAGS_DISABLED_JSON
        )

        assertThat(elementsSession?.linkSettings?.useAttestationEndpoints).isFalse()
        assertThat(elementsSession?.linkSettings?.suppress2faModal).isFalse()
    }

    @Test
    fun `Parses Native Link flags as disabled when field is omitted`() {
        val elementsSession = ElementsSessionJsonParser(
            ElementsSessionParams.PaymentIntentType(
                clientSecret = "secret",
                externalPaymentMethods = emptyList(),
                customPaymentMethods = emptyList(),
                appId = APP_ID
            ),
            isLiveMode = true,
        ).parse(
            ElementsSessionFixtures.EXPANDED_PAYMENT_INTENT_WITH_LINK_ATTESTATION_ENDPOINTS_MISSING_JSON
        )

        assertThat(elementsSession?.linkSettings?.useAttestationEndpoints).isFalse()
        assertThat(elementsSession?.linkSettings?.suppress2faModal).isFalse()
    }

    @Test
    fun `Parses payment_methods_with_link_details if feature is enabled for merchant`() {
        testPaymentMethodsAndLinkDetails(
            merchantEnabled = true,
            expectedPaymentMethods = 2,
            expectLinkPaymentDetails = true,
        )
    }

    @Test
    fun `Does not parse payment_methods_with_link_details if feature is disabled for merchant`() {
        testPaymentMethodsAndLinkDetails(
            merchantEnabled = false,
            expectedPaymentMethods = 1,
            expectLinkPaymentDetails = false,
        )
    }

    private fun testPaymentMethodsAndLinkDetails(
        merchantEnabled: Boolean,
        expectedPaymentMethods: Int,
        expectLinkPaymentDetails: Boolean,
    ) {
        val parser = ElementsSessionJsonParser(
            ElementsSessionParams.PaymentIntentType(
                clientSecret = "secret",
                externalPaymentMethods = listOf(),
                customPaymentMethods = listOf(),
                appId = APP_ID
            ),
            isLiveMode = false,
        )

        val intent = createPaymentIntentWithCustomerSession(
            enableLinkSpm = merchantEnabled,
            paymentMethodsWithLinkDetails = PAYMENT_METHODS_WITH_LINK_DETAILS,
        )
        val session = parser.parse(intent)
        val paymentMethods = session!!.customer!!.paymentMethods

        assertThat(paymentMethods).hasSize(expectedPaymentMethods)
        assertThat(paymentMethods.first().linkPaymentDetails != null).isEqualTo(expectLinkPaymentDetails)
    }

    private fun allowRedisplayTest(
        rawAllowRedisplayValue: String?,
        allowRedisplay: PaymentMethod.AllowRedisplay?,
    ) {
        val parser = ElementsSessionJsonParser(
            ElementsSessionParams.PaymentIntentType(
                clientSecret = "secret",
                customerSessionClientSecret = "customer_session_client_secret",
                externalPaymentMethods = emptyList(),
                customPaymentMethods = emptyList(),
                appId = APP_ID
            ),
            isLiveMode = false,
        )

        val intent = createPaymentIntentWithCustomerSession(allowRedisplay = rawAllowRedisplayValue)
        val elementsSession = parser.parse(intent)

        val mobilePaymentElementComponent = elementsSession?.customer?.session?.components?.mobilePaymentElement

        val enabledComponent = mobilePaymentElementComponent as?
            ElementsSession.Customer.Components.MobilePaymentElement.Enabled

        assertThat(enabledComponent?.allowRedisplayOverride).isEqualTo(allowRedisplay)
    }

    private fun permissionsTest(
        paymentMethodRemoveFeatureValue: String? = "enabled",
        paymentMethodRemoveLastFeatureValue: String? = "enabled",
        canRemovePaymentMethods: Boolean = true,
        canRemoveLastPaymentMethod: Boolean = true,
    ) {
        val parser = ElementsSessionJsonParser(
            ElementsSessionParams.PaymentIntentType(
                clientSecret = "secret",
                customerSessionClientSecret = "customer_session_client_secret",
                externalPaymentMethods = emptyList(),
                customPaymentMethods = emptyList(),
                appId = APP_ID
            ),
            isLiveMode = false,
        )

        val intent = createPaymentIntentWithCustomerSession(
            paymentMethodRemoveFeature = paymentMethodRemoveFeatureValue,
            paymentMethodRemoveLastFeature = paymentMethodRemoveLastFeatureValue,
        )

        val elementsSession = parser.parse(intent)

        val mobilePaymentElementComponent = elementsSession?.customer?.session?.components?.mobilePaymentElement

        assertThat(mobilePaymentElementComponent)
            .isInstanceOf(ElementsSession.Customer.Components.MobilePaymentElement.Enabled::class.java)

        val enabledPaymentElementComponent = mobilePaymentElementComponent as?
            ElementsSession.Customer.Components.MobilePaymentElement.Enabled

        assertThat(enabledPaymentElementComponent?.isPaymentMethodRemoveEnabled).isEqualTo(canRemovePaymentMethods)
        assertThat(enabledPaymentElementComponent?.canRemoveLastPaymentMethod).isEqualTo(canRemoveLastPaymentMethod)

        val customerSheetComponent = elementsSession?.customer?.session?.components?.customerSheet

        assertThat(customerSheetComponent)
            .isInstanceOf(ElementsSession.Customer.Components.CustomerSheet.Enabled::class.java)

        val enabledCustomerSheetComponent = customerSheetComponent as?
            ElementsSession.Customer.Components.CustomerSheet.Enabled

        assertThat(enabledCustomerSheetComponent?.isPaymentMethodRemoveEnabled).isEqualTo(canRemovePaymentMethods)
        assertThat(enabledCustomerSheetComponent?.canRemoveLastPaymentMethod).isEqualTo(canRemoveLastPaymentMethod)
    }

    @Test
    fun `Parses passive captcha when present in response`() {
        val parser = ElementsSessionJsonParser(
            ElementsSessionParams.PaymentIntentType(
                clientSecret = "secret",
                externalPaymentMethods = emptyList(),
                customPaymentMethods = emptyList(),
                appId = APP_ID
            ),
            isLiveMode = false,
        )

        val session = parser.parse(ElementsSessionFixtures.EXPANDED_PAYMENT_INTENT_JSON_WITH_PASSIVE_CAPTCHA)

        assertThat(session?.passiveCaptchaParams).isEqualTo(
            PassiveCaptchaParams(
                siteKey = "test_site_key",
                rqData = "test_rq_data"
            )
        )
    }

    @Test
    fun `Returns null passive captcha when passive_captcha field is missing`() {
        val parser = ElementsSessionJsonParser(
            ElementsSessionParams.PaymentIntentType(
                clientSecret = "secret",
                externalPaymentMethods = emptyList(),
                customPaymentMethods = emptyList(),
                appId = APP_ID
            ),
            isLiveMode = false,
        )

        val session = parser.parse(ElementsSessionFixtures.EXPANDED_PAYMENT_INTENT_JSON_WITHOUT_PASSIVE_CAPTCHA)

        assertThat(session?.passiveCaptchaParams).isNull()
    }

    companion object {
        private const val APP_ID = "com.app.id"
    }
}
