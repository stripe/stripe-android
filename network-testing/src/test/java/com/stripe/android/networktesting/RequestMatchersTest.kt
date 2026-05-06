package com.stripe.android.networktesting

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class RequestMatchersTest {

    @Test
    fun `bodyPart matches plain args and pre-encoded args`() {
        val request = postWithFormBody("billing_details[email]" to "foo@bar.com")

        val plain = RequestMatchers.bodyPart("billing_details[email]", "foo@bar.com")
        val encoded = RequestMatchers.bodyPart("billing_details%5Bemail%5D", "foo%40bar.com")

        assertThat(plain.matches(request)).isTrue()
        assertThat(encoded.matches(request)).isTrue()
    }

    @Test
    fun `bodyPart fails on wrong value or missing key`() {
        val request = postWithFormBody("billing_details[email]" to "foo@bar.com")

        val wrongValue = RequestMatchers.bodyPart("billing_details[email]", "wrong@bar.com")
        val missingKey = RequestMatchers.bodyPart("nonexistent_key", "foo@bar.com")

        assertThat(wrongValue.matches(request)).isFalse()
        assertThat(missingKey.matches(request)).isFalse()
    }

    @Test
    fun `bodyPart regex matches against decoded value`() {
        val request = postWithFormBody("user_agent" to "stripe-android/1.2.3;PaymentSheet")
        val matcher = RequestMatchers.bodyPart(
            "user_agent",
            Regex("stripe-android/\\d+\\.\\d+\\.\\d+;PaymentSheet")
        )
        assertThat(matcher.matches(request)).isTrue()
    }

    @Test
    fun `bodyPart handles plus sign encoding correctly`() {
        // Literal + in value (phone number) — preserved, not treated as space
        val phoneRequest = postWithFormBody("phone" to "+11234567890")
        assertThat(RequestMatchers.bodyPart("phone", "+11234567890").matches(phoneRequest)).isTrue()

        // Space in value — encoded as + in form body, decoded back to space
        val nameRequest = postWithFormBody("name" to "John Doe")
        assertThat(RequestMatchers.bodyPart("name", "John Doe").matches(nameRequest)).isTrue()

        // Backward compat: urlEncode("John Doe") produces "John+Doe", must still match via fallback
        assertThat(RequestMatchers.bodyPart("name", "John+Doe").matches(nameRequest)).isTrue()
    }

    @Test
    fun `hasBodyPart matches plain and pre-encoded keys`() {
        val request = postWithFormBody("billing_details[name]" to "Jane")

        val plain = RequestMatchers.hasBodyPart("billing_details[name]")
        val encoded = RequestMatchers.hasBodyPart("billing_details%5Bname%5D")

        assertThat(plain.matches(request)).isTrue()
        assertThat(encoded.matches(request)).isTrue()
    }

    @Test
    fun `query matches plain and pre-encoded keys`() {
        val request = getWithQuery("deferred_intent[on_behalf_of]" to "acct_123")

        val plain = RequestMatchers.query("deferred_intent[on_behalf_of]", "acct_123")
        val encoded = RequestMatchers.query("deferred_intent%5Bon_behalf_of%5D", "acct_123")

        assertThat(plain.matches(request)).isTrue()
        assertThat(encoded.matches(request)).isTrue()
    }

    @Test
    fun `composite diagnose reports per-matcher pass and fail`() {
        val request = postWithFormBody(
            "billing_details[email]" to "actual@test.com",
            path = "/v1/payment_intents/confirm",
        )

        val composite = RequestMatchers.composite(
            RequestMatchers.path("/v1/payment_intents/confirm"),
            RequestMatchers.method("POST"),
            RequestMatchers.bodyPart("billing_details[email]", "expected@test.com"),
        ) as CompositeRequestMatcher

        assertThat(composite.matches(request)).isFalse()
        assertThat(composite.diagnose(request).lines()).containsExactly(
            "  + PASS: path(/v1/payment_intents/confirm)",
            "  + PASS: method(POST)",
            "  - FAIL: bodyPart(billing_details[email], expected@test.com)",
        ).inOrder()
    }

    @Test
    fun `bodyParams and queryParams parse and decode correctly`() {
        val postRequest = postWithFormBody(
            "key1" to "value1",
            "billing_details[email]" to "foo@bar.com",
        )
        assertThat(postRequest.bodyParams).isEqualTo(
            mapOf("key1" to "value1", "billing_details[email]" to "foo@bar.com")
        )

        val queryRequest = getWithQuery("deferred_intent[mode]" to "setup", "type" to "card")
        assertThat(queryRequest.queryParams).isEqualTo(
            mapOf("deferred_intent[mode]" to "setup", "type" to "card")
        )

        val emptyRequest = createRequest()
        assertThat(emptyRequest.bodyParams).isEmpty()
        assertThat(emptyRequest.queryParams).isEmpty()
    }

    private fun postWithFormBody(
        vararg params: Pair<String, String>,
        path: String = "/v1/test",
    ): TestRecordedRequest {
        return TestRecordedRequest(
            MockRecordedRequestBuilder()
                .path(path)
                .method("POST")
                .formBody(*params)
                .build()
        )
    }

    private fun getWithQuery(vararg params: Pair<String, String>): TestRecordedRequest {
        val queryString = params.joinToString("&") { (key, value) ->
            "${java.net.URLEncoder.encode(key, "UTF-8")}=${java.net.URLEncoder.encode(value, "UTF-8")}"
        }
        return createRequest(path = "/v1/test?$queryString")
    }

    private fun createRequest(
        path: String = "/v1/test",
        body: String = "",
        method: String = "GET",
    ): TestRecordedRequest {
        return TestRecordedRequest(
            MockRecordedRequestBuilder()
                .path(path)
                .body(body)
                .method(method)
                .build()
        )
    }
}
