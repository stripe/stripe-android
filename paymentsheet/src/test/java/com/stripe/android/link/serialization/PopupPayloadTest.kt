package com.stripe.android.link.serialization

import android.content.Context
import android.os.LocaleList
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.networking.AnalyticsRequestFactory
import com.stripe.android.link.LinkConfiguration
import com.stripe.android.link.TestFactory
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFixtures
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.StripeIntent
import com.stripe.android.testing.PaymentIntentFactory
import com.stripe.android.testing.SetupIntentFactory
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Locale
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Suppress("MaxLineLength")
internal class PopupPayloadTest {

    @Test
    fun testJsonSerialization() {
        val currentSessionId = AnalyticsRequestFactory.sessionId
        val json = PopupPayload.PopupPayloadJson.encodeToString(
            serializer = PopupPayload.serializer(),
            value = createPayload(),
        )

        assertThat(json).isEqualTo(
            """{"publishableKey":"pk_test_abc","stripeAccount":"123","merchantInfo":{"businessName":"Jay's Taco Stand","country":"US"},"customerInfo":{"email":"jaystacostandfake@stripe.com","country":"US"},"paymentInfo":{"currency":"USD","amount":5555},"appId":"example.stripe.unittest","locale":"US","paymentUserAgent":"test","paymentObject":"link_payment_method","intentMode":"payment","setupFutureUsage":true,"cardBrandChoice":{"isMerchantEligibleForCBC":true,"stripePreferredNetworks":["cartes_bancaires"]},"flags":{"link_authenticated_change_event_enabled":false,"link_bank_incentives_enabled":false,"link_bank_onboarding_enabled":false,"link_email_verification_login_enabled":false,"link_financial_incentives_experiment_enabled":false,"link_local_storage_login_enabled":true,"link_only_for_payment_method_types_enabled":false,"link_passthrough_mode_enabled":true},"linkFundingSources":["CARD"],"clientAttributionMetadata":{"merchant_integration_source":"elements","merchant_integration_subtype":"mobile","merchant_integration_version":"stripe-android/21.28.2","client_session_id":"$currentSessionId","payment_method_selection_flow":"automatic","payment_intent_creation_flow":"standard","elements_session_config_id":"e961790f-43ed-4fcc-a534-74eeca28d042"},"path":"mobile_pay","integrationType":"mobile","loggerMetadata":{"mobile_session_id":"$currentSessionId"},"experiments":{}}"""
        )
    }

    @Test
    fun testToUrl() {
        AnalyticsRequestFactory.setSessionId(UUID.fromString("537a88ff-a54f-42cc-ba52-c7c5623730b6"))

        assertThat(createPayload().toUrl()).isEqualTo("https://checkout.link.com/#eyJwdWJsaXNoYWJsZUtleSI6InBrX3Rlc3RfYWJjIiwic3RyaXBlQWNjb3VudCI6IjEyMyIsIm1lcmNoYW50SW5mbyI6eyJidXNpbmVzc05hbWUiOiJKYXkncyBUYWNvIFN0YW5kIiwiY291bnRyeSI6IlVTIn0sImN1c3RvbWVySW5mbyI6eyJlbWFpbCI6ImpheXN0YWNvc3RhbmRmYWtlQHN0cmlwZS5jb20iLCJjb3VudHJ5IjoiVVMifSwicGF5bWVudEluZm8iOnsiY3VycmVuY3kiOiJVU0QiLCJhbW91bnQiOjU1NTV9LCJhcHBJZCI6ImV4YW1wbGUuc3RyaXBlLnVuaXR0ZXN0IiwibG9jYWxlIjoiVVMiLCJwYXltZW50VXNlckFnZW50IjoidGVzdCIsInBheW1lbnRPYmplY3QiOiJsaW5rX3BheW1lbnRfbWV0aG9kIiwiaW50ZW50TW9kZSI6InBheW1lbnQiLCJzZXR1cEZ1dHVyZVVzYWdlIjp0cnVlLCJjYXJkQnJhbmRDaG9pY2UiOnsiaXNNZXJjaGFudEVsaWdpYmxlRm9yQ0JDIjp0cnVlLCJzdHJpcGVQcmVmZXJyZWROZXR3b3JrcyI6WyJjYXJ0ZXNfYmFuY2FpcmVzIl19LCJmbGFncyI6eyJsaW5rX2F1dGhlbnRpY2F0ZWRfY2hhbmdlX2V2ZW50X2VuYWJsZWQiOmZhbHNlLCJsaW5rX2JhbmtfaW5jZW50aXZlc19lbmFibGVkIjpmYWxzZSwibGlua19iYW5rX29uYm9hcmRpbmdfZW5hYmxlZCI6ZmFsc2UsImxpbmtfZW1haWxfdmVyaWZpY2F0aW9uX2xvZ2luX2VuYWJsZWQiOmZhbHNlLCJsaW5rX2ZpbmFuY2lhbF9pbmNlbnRpdmVzX2V4cGVyaW1lbnRfZW5hYmxlZCI6ZmFsc2UsImxpbmtfbG9jYWxfc3RvcmFnZV9sb2dpbl9lbmFibGVkIjp0cnVlLCJsaW5rX29ubHlfZm9yX3BheW1lbnRfbWV0aG9kX3R5cGVzX2VuYWJsZWQiOmZhbHNlLCJsaW5rX3Bhc3N0aHJvdWdoX21vZGVfZW5hYmxlZCI6dHJ1ZX0sImxpbmtGdW5kaW5nU291cmNlcyI6WyJDQVJEIl0sImNsaWVudEF0dHJpYnV0aW9uTWV0YWRhdGEiOnsibWVyY2hhbnRfaW50ZWdyYXRpb25fc291cmNlIjoiZWxlbWVudHMiLCJtZXJjaGFudF9pbnRlZ3JhdGlvbl9zdWJ0eXBlIjoibW9iaWxlIiwibWVyY2hhbnRfaW50ZWdyYXRpb25fdmVyc2lvbiI6InN0cmlwZS1hbmRyb2lkLzIxLjI4LjIiLCJjbGllbnRfc2Vzc2lvbl9pZCI6IjUzN2E4OGZmLWE1NGYtNDJjYy1iYTUyLWM3YzU2MjM3MzBiNiIsInBheW1lbnRfbWV0aG9kX3NlbGVjdGlvbl9mbG93IjoiYXV0b21hdGljIiwicGF5bWVudF9pbnRlbnRfY3JlYXRpb25fZmxvdyI6InN0YW5kYXJkIiwiZWxlbWVudHNfc2Vzc2lvbl9jb25maWdfaWQiOiJlOTYxNzkwZi00M2VkLTRmY2MtYTUzNC03NGVlY2EyOGQwNDIifSwicGF0aCI6Im1vYmlsZV9wYXkiLCJpbnRlZ3JhdGlvblR5cGUiOiJtb2JpbGUiLCJsb2dnZXJNZXRhZGF0YSI6eyJtb2JpbGVfc2Vzc2lvbl9pZCI6IjUzN2E4OGZmLWE1NGYtNDJjYy1iYTUyLWM3YzU2MjM3MzBiNiJ9LCJleHBlcmltZW50cyI6e319")
    }

    @Test
    fun `on 'create' with customer billing country provided, should use provided country`() {
        val payload = PopupPayload.create(
            configuration = createLinkConfiguration(customerCountryCode = "US"),
            context = getContext(Locale.CANADA),
            paymentUserAgent = "Android",
            publishableKey = "pk_123",
            stripeAccount = "str_123"
        )

        assertThat(payload.customerInfo.country).isEqualTo("US")
    }

    @Test
    fun `on 'create' with no customer billing country provided, should use locale`() {
        val payload = PopupPayload.create(
            configuration = createLinkConfiguration(customerCountryCode = null),
            context = getContext(Locale.CANADA),
            paymentUserAgent = "Android",
            publishableKey = "pk_123",
            stripeAccount = "str_123"
        )

        assertThat(payload.customerInfo.country).isEqualTo("CA")
    }

    @Test
    fun `on 'create' with payment intent, intent mode should be Payment`() {
        val payload = PopupPayload.create(
            configuration = createLinkConfiguration(
                customerCountryCode = null,
                intent = PaymentIntentFactory.create()
            ),
            context = getContext(Locale.CANADA),
            paymentUserAgent = "Android",
            publishableKey = "pk_123",
            stripeAccount = "str_123"
        )

        assertThat(payload.intentMode).isEqualTo(PopupPayload.IntentMode.Payment.type)
    }

    @Test
    fun `on 'create' with setup intent, intent mode should be Setup`() {
        val payload = PopupPayload.create(
            configuration = createLinkConfiguration(
                customerCountryCode = null,
                intent = SetupIntentFactory.create()
            ),
            context = getContext(Locale.CANADA),
            paymentUserAgent = "Android",
            publishableKey = "pk_123",
            stripeAccount = "str_123"
        )

        assertThat(payload.intentMode).isEqualTo(PopupPayload.IntentMode.Setup.type)
    }

    @Test
    fun `on 'create' with setup intent, 'setupForFutureUsage' should be true`() {
        val payload = PopupPayload.create(
            configuration = createLinkConfiguration(
                customerCountryCode = null,
                intent = SetupIntentFactory.create()
            ),
            context = getContext(Locale.CANADA),
            paymentUserAgent = "Android",
            publishableKey = "pk_123",
            stripeAccount = "str_123"
        )

        assertThat(payload.setupFutureUsage).isTrue()
    }

    @Test
    fun `on 'create' with payment intent with no usage, 'setupForFutureUsage' should be false`() {
        val payload = PopupPayload.create(
            configuration = createLinkConfiguration(
                customerCountryCode = null,
                intent = PaymentIntentFactory.create(
                    setupFutureUsage = null
                )
            ),
            context = getContext(Locale.CANADA),
            paymentUserAgent = "Android",
            publishableKey = "pk_123",
            stripeAccount = "str_123"
        )

        assertThat(payload.setupFutureUsage).isFalse()
    }

    @Test
    fun `on 'create' with payment intent with one time usage, 'setupForFutureUsage' should be false`() {
        val payload = PopupPayload.create(
            configuration = createLinkConfiguration(
                customerCountryCode = null,
                intent = PaymentIntentFactory.create(
                    setupFutureUsage = StripeIntent.Usage.OneTime
                )
            ),
            context = getContext(Locale.CANADA),
            paymentUserAgent = "Android",
            publishableKey = "pk_123",
            stripeAccount = "str_123"
        )

        assertThat(payload.setupFutureUsage).isFalse()
    }

    @Test
    fun `on 'create' with payment intent with off session usage, 'setupForFutureUsage' should be true`() {
        val payload = PopupPayload.create(
            configuration = createLinkConfiguration(
                customerCountryCode = null,
                intent = PaymentIntentFactory.create(
                    setupFutureUsage = StripeIntent.Usage.OffSession
                )
            ),
            context = getContext(Locale.CANADA),
            paymentUserAgent = "Android",
            publishableKey = "pk_123",
            stripeAccount = "str_123"
        )

        assertThat(payload.setupFutureUsage).isTrue()
    }

    @Test
    fun `on 'create' with payment intent with on session usage, 'setupForFutureUsage' should be true`() {
        val payload = PopupPayload.create(
            configuration = createLinkConfiguration(
                customerCountryCode = null,
                intent = PaymentIntentFactory.create(
                    setupFutureUsage = StripeIntent.Usage.OnSession
                )
            ),
            context = getContext(Locale.CANADA),
            paymentUserAgent = "Android",
            publishableKey = "pk_123",
            stripeAccount = "str_123"
        )

        assertThat(payload.setupFutureUsage).isTrue()
    }

    @Test
    fun `on 'create' with link PMO SFU on session usage, 'setupForFutureUsage' should be true`() {
        val payload = PopupPayload.create(
            configuration = createLinkConfiguration(
                customerCountryCode = null,
                intent = PaymentIntentFactory.create(
                    paymentMethodOptionsJsonString = PaymentIntentFixtures.getPaymentMethodOptionsJsonString(
                        code = PaymentMethod.Type.Link.code,
                        sfuValue = "on_session"
                    )
                )
            ),
            context = getContext(Locale.CANADA),
            paymentUserAgent = "Android",
            publishableKey = "pk_123",
            stripeAccount = "str_123"
        )

        assertThat(payload.setupFutureUsage).isTrue()
    }

    @Test
    fun `on 'create' with link PMO SFU off session usage, 'setupForFutureUsage' should be true`() {
        val payload = PopupPayload.create(
            configuration = createLinkConfiguration(
                customerCountryCode = null,
                intent = PaymentIntentFactory.create(
                    paymentMethodOptionsJsonString = PaymentIntentFixtures.getPaymentMethodOptionsJsonString(
                        code = PaymentMethod.Type.Link.code,
                        sfuValue = "off_session"
                    )
                )
            ),
            context = getContext(Locale.CANADA),
            paymentUserAgent = "Android",
            publishableKey = "pk_123",
            stripeAccount = "str_123"
        )

        assertThat(payload.setupFutureUsage).isTrue()
    }

    @Test
    fun `on 'create' with card PMO SFU with on session usage, 'setupForFutureUsage' should be true`() {
        val payload = PopupPayload.create(
            configuration = createLinkConfiguration(
                customerCountryCode = null,
                intent = PaymentIntentFactory.create(
                    paymentMethodOptionsJsonString = PaymentIntentFixtures.getPaymentMethodOptionsJsonString(
                        code = PaymentMethod.Type.Card.code,
                        sfuValue = "on_session"
                    )
                ),
                passthroughModeEnabled = true
            ),
            context = getContext(Locale.CANADA),
            paymentUserAgent = "Android",
            publishableKey = "pk_123",
            stripeAccount = "str_123"
        )

        assertThat(payload.setupFutureUsage).isTrue()
    }

    @Test
    fun `on 'create' with card PMO SFU with off session usage, 'setupForFutureUsage' should be true`() {
        val payload = PopupPayload.create(
            configuration = createLinkConfiguration(
                customerCountryCode = null,
                intent = PaymentIntentFactory.create(
                    paymentMethodOptionsJsonString = PaymentIntentFixtures.getPaymentMethodOptionsJsonString(
                        code = PaymentMethod.Type.Card.code,
                        sfuValue = "off_session"
                    )
                ),
                passthroughModeEnabled = true
            ),
            context = getContext(Locale.CANADA),
            paymentUserAgent = "Android",
            publishableKey = "pk_123",
            stripeAccount = "str_123"
        )

        assertThat(payload.setupFutureUsage).isTrue()
    }

    @Test
    fun `on 'create' with card PMO SFU in payment method mode, 'setupForFutureUsage' should be false`() {
        val payload = PopupPayload.create(
            configuration = createLinkConfiguration(
                customerCountryCode = null,
                intent = PaymentIntentFactory.create(
                    paymentMethodOptionsJsonString = PaymentIntentFixtures.getPaymentMethodOptionsJsonString(
                        code = PaymentMethod.Type.Card.code,
                        sfuValue = "on_session"
                    )
                )
            ),
            context = getContext(Locale.CANADA),
            paymentUserAgent = "Android",
            publishableKey = "pk_123",
            stripeAccount = "str_123"
        )

        assertThat(payload.setupFutureUsage).isFalse()
    }

    @Test
    fun `on 'create' with link PMO SFU in payment method mode, 'setupForFutureUsage' should be true`() {
        val payload = PopupPayload.create(
            configuration = createLinkConfiguration(
                customerCountryCode = null,
                intent = PaymentIntentFactory.create(
                    paymentMethodOptionsJsonString = PaymentIntentFixtures.getPaymentMethodOptionsJsonString(
                        code = PaymentMethod.Type.Link.code,
                        sfuValue = "off_session"
                    )
                )
            ),
            context = getContext(Locale.CANADA),
            paymentUserAgent = "Android",
            publishableKey = "pk_123",
            stripeAccount = "str_123"
        )

        assertThat(payload.setupFutureUsage).isTrue()
    }

    @Test
    fun `on 'create' with card brand choice, should contain expected card brand choice values`() {
        val payload = PopupPayload.create(
            configuration = createLinkConfiguration(
                customerCountryCode = null,
                intent = PaymentIntentFactory.create(
                    setupFutureUsage = StripeIntent.Usage.OnSession
                ),
                cardBrandChoice = LinkConfiguration.CardBrandChoice(
                    eligible = true,
                    preferredNetworks = listOf("cartes_bancaires")
                )
            ),
            context = getContext(Locale.CANADA),
            paymentUserAgent = "Android",
            publishableKey = "pk_123",
            stripeAccount = "str_123"
        )

        assertThat(payload.cardBrandChoice?.eligible).isTrue()
        assertThat(payload.cardBrandChoice?.preferredNetworks).isEqualTo(listOf("cartes_bancaires"))
    }

    @Test
    fun `on 'create' without bank funding source, should contain expected funding sources`() {
        val payload = PopupPayload.create(
            configuration = createLinkConfiguration(
                customerCountryCode = null,
                intent = PaymentIntentFactory.create(
                    linkFundingSources = listOf("card"),
                ),
            ),
            context = getContext(Locale.CANADA),
            paymentUserAgent = "Android",
            publishableKey = "pk_123",
            stripeAccount = "str_123"
        )

        assertThat(payload.linkFundingSources).containsExactly("CARD").inOrder()
    }

    @Test
    fun `on 'create' with bank funding source, should contain expected funding sources`() {
        val payload = PopupPayload.create(
            configuration = createLinkConfiguration(
                customerCountryCode = null,
                intent = PaymentIntentFactory.create(
                    linkFundingSources = listOf("card", "bank_account"),
                ),
            ),
            context = getContext(Locale.CANADA),
            paymentUserAgent = "Android",
            publishableKey = "pk_123",
            stripeAccount = "str_123"
        )

        assertThat(payload.linkFundingSources).containsExactly("CARD", "BANK_ACCOUNT").inOrder()
    }

    @Test
    fun `create sets client attribution metadata correctly`() {
        val linkConfiguration = createLinkConfiguration()
        val payload = PopupPayload.create(
            configuration = linkConfiguration,
            context = getContext(Locale.CANADA),
            paymentUserAgent = "Android",
            publishableKey = "pk_123",
            stripeAccount = "str_123"
        )

        assertThat(payload.clientAttributionMetadata).isEqualTo(
            linkConfiguration.clientAttributionMetadata.toParamMap()
        )
    }

    private fun createLinkConfiguration(
        customerCountryCode: String? = null,
        cardBrandChoice: LinkConfiguration.CardBrandChoice? = null,
        intent: StripeIntent = PaymentIntentFactory.create(),
        passthroughModeEnabled: Boolean = false
    ): LinkConfiguration {
        return TestFactory.LINK_CONFIGURATION.copy(
            customerInfo = TestFactory.LINK_CONFIGURATION.customerInfo.copy(
                billingCountryCode = customerCountryCode
            ),
            cardBrandChoice = cardBrandChoice,
            stripeIntent = intent,
            passthroughModeEnabled = passthroughModeEnabled
        )
    }

    private fun createPayload(): PopupPayload = PopupPayload(
        publishableKey = "pk_test_abc",
        stripeAccount = "123",
        merchantInfo = PopupPayload.MerchantInfo(
            businessName = "Jay's Taco Stand",
            country = "US",
        ),
        customerInfo = PopupPayload.CustomerInfo(
            email = "jaystacostandfake@stripe.com",
            country = "US",
        ),
        paymentInfo = PopupPayload.PaymentInfo(
            currency = "USD",
            amount = 5555,
        ),
        appId = "example.stripe.unittest",
        locale = "US",
        paymentUserAgent = "test",
        paymentObject = "link_payment_method",
        intentMode = "payment",
        setupFutureUsage = true,
        cardBrandChoice = PopupPayload.CardBrandChoice(
            eligible = true,
            preferredNetworks = listOf("cartes_bancaires"),
        ),
        flags = mapOf(
            "link_authenticated_change_event_enabled" to false,
            "link_bank_incentives_enabled" to false,
            "link_bank_onboarding_enabled" to false,
            "link_email_verification_login_enabled" to false,
            "link_financial_incentives_experiment_enabled" to false,
            "link_local_storage_login_enabled" to true,
            "link_only_for_payment_method_types_enabled" to false,
            "link_passthrough_mode_enabled" to true,
        ),
        linkFundingSources = listOf("CARD"),
        clientAttributionMetadata = PaymentMethodMetadataFixtures.CLIENT_ATTRIBUTION_METADATA.toParamMap(),
    )

    private fun getContext(locale: Locale): Context {
        val context = ApplicationProvider.getApplicationContext<Context>()

        val configuration = context.resources.configuration

        configuration.setLocales(LocaleList(locale))

        context.createConfigurationContext(configuration)

        return context
    }
}
