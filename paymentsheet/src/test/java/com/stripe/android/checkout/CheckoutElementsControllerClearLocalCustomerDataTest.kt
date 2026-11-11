package com.stripe.android.checkout

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.PaymentConfiguration
import com.stripe.android.Stripe
import com.stripe.android.core.networking.AnalyticsRequest
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.link.account.DefaultLinkStore
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.networktesting.RequestMatcher
import com.stripe.android.networktesting.RequestMatchers.analyticsPayloadField
import com.stripe.android.networktesting.RequestMatchers.hasQueryParam
import com.stripe.android.networktesting.RequestMatchers.host
import com.stripe.android.networktesting.RequestMatchers.method
import com.stripe.android.networktesting.RequestMatchers.not
import com.stripe.android.paymentelement.CheckoutSessionPreview
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertFailsWith
import kotlin.time.Duration.Companion.seconds

@OptIn(CheckoutSessionPreview::class)
@RunWith(RobolectricTestRunner::class)
internal class CheckoutElementsControllerClearLocalCustomerDataTest {
    @get:Rule
    val networkRule = NetworkRule(
        hostsToTrack = listOf(AnalyticsRequest.HOST),
        validationTimeout = 5.seconds,
    )

    private val application = ApplicationProvider.getApplicationContext<Application>()
    private val linkStore = DefaultLinkStore(application)

    @Before
    fun setUp() {
        clearPaymentConfiguration()
    }

    @After
    fun tearDown() {
        linkStore.clear()
        clearPaymentConfiguration()
    }

    @Test
    fun `clearLocalCustomerData clears persisted Link state`() {
        val advancedFraudSignalsEnabled = Stripe.advancedFraudSignalsEnabled
        try {
            Stripe.advancedFraudSignalsEnabled = false
            PaymentConfiguration.init(application, PUBLISHABLE_KEY)
        } finally {
            Stripe.advancedFraudSignalsEnabled = advancedFraudSignalsEnabled
        }
        linkStore.markLinkAsUsed()
        assertThat(DefaultLinkStore(application).hasUsedLink()).isTrue()

        expectEvent(PUBLISHABLE_KEY)
        CheckoutElementsController.clearLocalCustomerData(application)

        assertThat(DefaultLinkStore(application).hasUsedLink()).isFalse()
    }

    @Test
    fun `clearLocalCustomerData reports without PaymentConfiguration`() {
        assertFailsWith<IllegalStateException> {
            PaymentConfiguration.getInstance(application)
        }
        linkStore.markLinkAsUsed()
        assertThat(DefaultLinkStore(application).hasUsedLink()).isTrue()

        expectEvent(ApiRequest.Options.UNDEFINED_PUBLISHABLE_KEY)
        CheckoutElementsController.clearLocalCustomerData(application)

        assertThat(DefaultLinkStore(application).hasUsedLink()).isFalse()
    }

    @Test
    fun `clearLocalCustomerData reports each call when persisted Link state is empty`() {
        assertThat(DefaultLinkStore(application).hasUsedLink()).isFalse()

        expectEvent(ApiRequest.Options.UNDEFINED_PUBLISHABLE_KEY)
        CheckoutElementsController.clearLocalCustomerData(application)
        networkRule.validate()

        expectEvent(ApiRequest.Options.UNDEFINED_PUBLISHABLE_KEY)
        CheckoutElementsController.clearLocalCustomerData(application)
    }

    private fun expectEvent(publishableKey: String) {
        networkRule.enqueue(
            host("q.stripe.com"),
            method("GET"),
            analyticsPayloadField("event", "checkout_elements.clear_local_customer_data"),
            analyticsPayloadField("publishable_key", publishableKey),
            analyticsPayloadField("product_usage", "checkout_elements"),
            not(hasQueryParam("duration")),
            not(hasQueryParam("error_type")),
            not(hasQueryParam("error_code")),
            not(hasQueryParam("request_id")),
            not(hasQueryParam("checkout_session_id")),
            not(hasQueryParam("checkout_config_id")),
            not(hasQueryParam("currency")),
            not(hasQueryParam("session_status")),
            not(hasQueryParam("payment_status")),
            not(hasQueryParam("elements_session_config_id")),
            not(hasQueryParam("adaptive_pricing_active")),
            not(hasQueryParam("automatic_tax_enabled")),
            not(hasQueryParam("ordered_lpms")),
            RequestMatcher { request ->
                request.queryParams.keys.none { key -> key.substringBefore('[') in CONFIGURATION_KEYS }
            },
            applyDefaultAuthorization = false,
        ) { response ->
            response.setResponseCode(200)
        }
    }

    private fun clearPaymentConfiguration() {
        PaymentConfiguration.clearInstance()
        application.getSharedPreferences(PaymentConfiguration::class.java.name, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
    }

    private companion object {
        const val PUBLISHABLE_KEY = "pk_test_123"
        val CONFIGURATION_KEYS = setOf(
            "merchant_display_name",
            "defaults",
            "payment_element",
            "express_checkout_element",
            "currency_selector_element",
            "shipping_address_element",
        )
    }
}
