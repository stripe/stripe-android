package com.stripe.android.link.serialization

import android.content.Context
import android.os.LocaleList
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.networking.AnalyticsRequestFactory
import com.stripe.android.link.LinkConfiguration
import com.stripe.android.link.ui.inline.LinkSignupMode
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

        assertThat(json).isEqualTo("""{"publishableKey":"pk_test_abc","stripeAccount":"123","merchantInfo":{"businessName":"Jay's Taco Stand","country":"US"},"customerInfo":{"email":"jaystacostandfake@gmail.com","country":"US"},"paymentInfo":{"currency":"USD","amount":5555},"appId":"example.stripe.unittest","locale":"US","paymentUserAgent":"test","paymentObject":"link_payment_method","intentMode":"payment","setupFutureUsage":true,"flags":{"link_authenticated_change_event_enabled":false,"link_bank_incentives_enabled":false,"link_bank_onboarding_enabled":false,"link_email_verification_login_enabled":false,"link_financial_incentives_experiment_enabled":false,"link_local_storage_login_enabled":true,"link_only_for_payment_method_types_enabled":false,"link_passthrough_mode_enabled":true},"path":"mobile_pay","integrationType":"mobile","loggerMetadata":{"mobile_session_id":"$currentSessionId"},"experiments":{}}""")
    }

    @Test
    fun testToUrl() {
        AnalyticsRequestFactory.setSessionId(UUID.fromString("537a88ff-a54f-42cc-ba52-c7c5623730b6"))

        assertThat(createPayload().toUrl()).isEqualTo("https://checkout.link.com/#eyJwdWJsaXNoYWJsZUtleSI6InBrX3Rlc3RfYWJjIiwic3RyaXBlQWNjb3VudCI6IjEyMyIsIm1lcmNoYW50SW5mbyI6eyJidXNpbmVzc05hbWUiOiJKYXkncyBUYWNvIFN0YW5kIiwiY291bnRyeSI6IlVTIn0sImN1c3RvbWVySW5mbyI6eyJlbWFpbCI6ImpheXN0YWNvc3RhbmRmYWtlQGdtYWlsLmNvbSIsImNvdW50cnkiOiJVUyJ9LCJwYXltZW50SW5mbyI6eyJjdXJyZW5jeSI6IlVTRCIsImFtb3VudCI6NTU1NX0sImFwcElkIjoiZXhhbXBsZS5zdHJpcGUudW5pdHRlc3QiLCJsb2NhbGUiOiJVUyIsInBheW1lbnRVc2VyQWdlbnQiOiJ0ZXN0IiwicGF5bWVudE9iamVjdCI6ImxpbmtfcGF5bWVudF9tZXRob2QiLCJpbnRlbnRNb2RlIjoicGF5bWVudCIsInNldHVwRnV0dXJlVXNhZ2UiOnRydWUsImZsYWdzIjp7ImxpbmtfYXV0aGVudGljYXRlZF9jaGFuZ2VfZXZlbnRfZW5hYmxlZCI6ZmFsc2UsImxpbmtfYmFua19pbmNlbnRpdmVzX2VuYWJsZWQiOmZhbHNlLCJsaW5rX2Jhbmtfb25ib2FyZGluZ19lbmFibGVkIjpmYWxzZSwibGlua19lbWFpbF92ZXJpZmljYXRpb25fbG9naW5fZW5hYmxlZCI6ZmFsc2UsImxpbmtfZmluYW5jaWFsX2luY2VudGl2ZXNfZXhwZXJpbWVudF9lbmFibGVkIjpmYWxzZSwibGlua19sb2NhbF9zdG9yYWdlX2xvZ2luX2VuYWJsZWQiOnRydWUsImxpbmtfb25seV9mb3JfcGF5bWVudF9tZXRob2RfdHlwZXNfZW5hYmxlZCI6ZmFsc2UsImxpbmtfcGFzc3Rocm91Z2hfbW9kZV9lbmFibGVkIjp0cnVlfSwicGF0aCI6Im1vYmlsZV9wYXkiLCJpbnRlZ3JhdGlvblR5cGUiOiJtb2JpbGUiLCJsb2dnZXJNZXRhZGF0YSI6eyJtb2JpbGVfc2Vzc2lvbl9pZCI6IjUzN2E4OGZmLWE1NGYtNDJjYy1iYTUyLWM3YzU2MjM3MzBiNiJ9LCJleHBlcmltZW50cyI6e319")
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

    private fun createLinkConfiguration(
        customerCountryCode: String?,
        intent: StripeIntent = PaymentIntentFactory.create()
    ): LinkConfiguration {
        return LinkConfiguration(
            merchantName = "Jay's Taco Stand",
            merchantCountryCode = "US",
            customerInfo = LinkConfiguration.CustomerInfo(
                name = "John",
                email = "john@email.com",
                billingCountryCode = customerCountryCode,
                phone = null,
            ),
            flags = emptyMap(),
            passthroughModeEnabled = true,
            shippingValues = emptyMap(),
            signupMode = LinkSignupMode.InsteadOfSaveForFutureUse,
            stripeIntent = intent
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
            email = "jaystacostandfake@gmail.com",
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
    )

    private fun getContext(locale: Locale): Context {
        val context = ApplicationProvider.getApplicationContext<Context>()

        val configuration = context.resources.configuration

        configuration.setLocales(LocaleList(locale))

        context.createConfigurationContext(configuration)

        return context
    }
}
