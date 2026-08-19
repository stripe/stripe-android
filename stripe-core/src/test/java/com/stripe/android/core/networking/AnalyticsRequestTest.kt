package com.stripe.android.core.networking

import com.google.common.truth.Truth.assertThat
import com.stripe.android.networktesting.formBody
import com.stripe.android.networktesting.formBodyText
import org.junit.Test
import java.util.UUID

class AnalyticsRequestTest {
    @Test
    fun `url is r stripe com`() = runScenario {
        assertThat(request.url).isEqualTo("https://r.stripe.com/0")
    }

    @Test
    fun `method is POST`() = runScenario {
        assertThat(request.method).isEqualTo(StripeRequest.Method.POST)
    }

    @Test
    fun `postHeaders contain content type and origin`() = runScenario {
        assertThat(request.postHeaders).containsEntry(
            "Content-Type",
            "application/x-www-form-urlencoded; charset=UTF-8"
        )
        assertThat(request.postHeaders).containsEntry("origin", "stripe-mobile-payments-sdk-android")
    }

    @Test
    fun `body preserves legacy params`() = runScenario {
        val body = request.formBody()

        assertThat(body).containsEntry("event", "stripe_android.test_event")
        assertThat(body).containsEntry("timestamp", "1.7554E9")
        assertThat(body).containsEntry("analytics_ua", "analytics.stripe_android-1.0")
        assertThat(body).containsEntry("publishable_key", "pk_test_123")
    }

    @Test
    fun `body contains client id required by r stripe com`() = runScenario {
        assertThat(request.formBody()).containsEntry("client_id", "stripe-mobile-payments-sdk")
    }

    @Test
    fun `event name mirrors legacy event param`() = runScenario {
        assertThat(request.formBody()).containsEntry("event_name", "stripe_android.test_event")
    }

    @Test
    fun `created mirrors legacy timestamp param`() = runScenario {
        assertThat(request.formBody()).containsEntry("created", "1.7554E9")
    }

    @Test
    fun `event id is a uuid`() = runScenario {
        val eventId = requireNotNull(request.formBody()["event_id"])

        assertThat(UUID.fromString(eventId).toString()).isEqualTo(eventId)
    }

    @Test
    fun `event id is stable across repeated writes`() = runScenario {
        assertThat(request.formBodyText()).isEqualTo(request.formBodyText())
    }

    @Test
    fun `list params keep bracket encoding`() = runScenario(
        params = DEFAULT_PARAMS + mapOf("product_usage" to listOf("PaymentSheet", "CardWidget")),
    ) {
        assertThat(request.formBodyText())
            .contains("product_usage%5B%5D=PaymentSheet&product_usage%5B%5D=CardWidget")
    }

    @Test
    fun `params are not modified`() = runScenario {
        assertThat(request.params).isEqualTo(DEFAULT_PARAMS)
    }

    @Test
    fun `event name is omitted when event is absent`() = runScenario(
        params = DEFAULT_PARAMS - AnalyticsFields.EVENT,
    ) {
        assertThat(request.formBody()).doesNotContainKey("event_name")
    }

    @Test
    fun `created is omitted when timestamp is absent`() = runScenario(
        params = DEFAULT_PARAMS - AnalyticsFields.TIMESTAMP,
    ) {
        assertThat(request.formBody()).doesNotContainKey("created")
    }

    @Test
    fun `empty params still produce the required params`() = runScenario(
        params = emptyMap<String, Any>(),
    ) {
        val body = request.formBody()

        assertThat(body.keys).containsExactly("client_id", "event_id")
    }

    private fun runScenario(
        params: Map<String, *> = DEFAULT_PARAMS,
        headers: Map<String, String> = emptyMap(),
        block: Scenario.() -> Unit,
    ) {
        Scenario(request = AnalyticsRequest(params = params, headers = headers)).apply { block() }
    }

    private data class Scenario(
        val request: AnalyticsRequest,
    )

    private companion object {
        val DEFAULT_PARAMS: Map<String, *> = mapOf(
            AnalyticsFields.EVENT to "stripe_android.test_event",
            AnalyticsFields.TIMESTAMP to 1_755_400_000.0,
            AnalyticsFields.ANALYTICS_UA to "analytics.stripe_android-1.0",
            AnalyticsFields.PUBLISHABLE_KEY to "pk_test_123",
        )
    }
}
