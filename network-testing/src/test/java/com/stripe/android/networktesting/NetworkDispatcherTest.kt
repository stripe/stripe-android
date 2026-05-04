package com.stripe.android.networktesting

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class NetworkDispatcherTest {

    @Test
    fun `dispatch returns 500 for unmatched request`() = runScenario {
        enqueue(RequestMatchers.path("/v1/expected"))

        val response = dispatch(path = "/v1/unexpected")

        assertThat(response.status).isEqualTo("HTTP/1.1 500 Server Error")
    }

    @Test
    fun `dispatch matches and consumes enqueued response`() = runScenario {
        enqueue(RequestMatchers.path("/v1/test"))

        val response = dispatch(path = "/v1/test")

        assertThat(response.status).isEqualTo("HTTP/1.1 200 OK")
        dispatcher.validate()
    }

    @Test
    fun `validate fails when mocks remain unconsumed`() = runScenario {
        enqueue(RequestMatchers.path("/v1/confirm"))

        val error = assertValidationFails()

        assertThat(error.message).startsWith("Mock responses is not empty. Remaining: 1.")
    }

    @Test
    fun `validate fails when extra requests were made`() = runScenario {
        enqueue(
            RequestMatchers.path("/v1/confirm"),
            RequestMatchers.bodyPart("email", "expected@test.com"),
        )

        dispatch(path = "/v1/confirm", body = "email" to "actual@test.com")

        val error = assertValidationFails()
        val message = error.message!!
        val lines = message.lines()

        assertThat(lines).contains("  Body params: {email=actual@test.com}")
    }


    @Test
    fun `validate shows only nearest miss when multiple mocks remain`() = runScenario {
        enqueue(RequestMatchers.path("/v1/payment_methods"), RequestMatchers.method("POST"))
        enqueue(
            RequestMatchers.path("/v1/confirm"),
            RequestMatchers.method("POST"),
            RequestMatchers.bodyPart("email", "expected@test.com"),
        )

        dispatch(path = "/v1/confirm", body = "email" to "actual@test.com")

        val error = assertValidationFails()
        val lines = error.message!!.lines()

        assertThat(lines).containsAtLeast(
            "  + PASS: path(/v1/confirm)",
            "  + PASS: method(POST)",
            "  - FAIL: bodyPart(email, expected@test.com)",
        )
        assertThat(lines).containsNoneOf(
            "  + PASS: path(/v1/payment_methods)",
            "  - FAIL: path(/v1/payment_methods)",
        )
    }

    private fun runScenario(block: Scenario.() -> Unit) {
        Scenario().block()
    }

    private class Scenario {
        val dispatcher = NetworkDispatcher(validationTimeout = null)

        fun enqueue(vararg matchers: RequestMatcher) {
            dispatcher.enqueue(*matchers) { it.setBody("{}") }
        }

        fun dispatch(
            path: String = "/v1/test",
            body: Pair<String, String>? = null,
        ) = dispatcher.dispatch(
            MockRecordedRequestBuilder()
                .path(path)
                .method("POST")
                .apply { if (body != null) formBody(body) }
                .build()
        )

        fun assertValidationFails(): IllegalStateException {
            val error = runCatching { dispatcher.validate() }.exceptionOrNull()
            assertThat(error).isInstanceOf(IllegalStateException::class.java)
            return error as IllegalStateException
        }
    }
}
