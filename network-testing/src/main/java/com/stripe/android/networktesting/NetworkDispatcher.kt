package com.stripe.android.networktesting

import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import java.util.Collections
import java.util.Queue
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

internal class NetworkDispatcher(private val validationTimeout: Duration?) : Dispatcher() {
    private val enqueuedResponses: Queue<Entry> = ConcurrentLinkedQueue()
    private val unmatchedRequests: MutableList<UnmatchedRequest> = Collections.synchronizedList(mutableListOf())

    fun enqueue(vararg requestMatcher: RequestMatcher, responseFactory: (MockResponse) -> Unit) {
        validateEnqueueState()
        enqueuedResponses.add(
            Entry(RequestMatchers.composite(*requestMatcher)) {
                val response = MockResponse()
                response.setResponseCode(200)
                responseFactory(response)
                response
            }
        )
    }

    fun enqueue(
        vararg requestMatcher: RequestMatcher,
        responseFactory: (TestRecordedRequest, MockResponse) -> Unit
    ) {
        validateEnqueueState()
        enqueuedResponses.add(
            Entry(RequestMatchers.composite(*requestMatcher)) {
                val response = MockResponse()
                response.setResponseCode(200)
                responseFactory(it, response)
                response
            }
        )
    }

    private fun validateEnqueueState() {
        val exceptionMessage = StringBuilder()
        addExtraRequestsToExceptionMessage(exceptionMessage)
        if (exceptionMessage.isNotEmpty()) {
            exceptionMessage.insert(0, "Responses must be enqueued before the production code makes the request.\n")
            throw IllegalStateException(exceptionMessage.toString())
        }
    }

    fun clear() {
        enqueuedResponses.clear()
        unmatchedRequests.clear()
    }

    fun validate() {
        val exceptionMessage = StringBuilder()
        if (hasResponsesInQueue()) {
            exceptionMessage.append(
                "Mock responses is not empty. Remaining: ${enqueuedResponses.size}.\nRemaining Matchers: " +
                    remainingMatchersDescription()
            )
        }
        addExtraRequestsToExceptionMessage(exceptionMessage)
        if (exceptionMessage.isNotEmpty()) {
            throw IllegalStateException(exceptionMessage.toString())
        }
    }

    private fun addExtraRequestsToExceptionMessage(exceptionMessage: StringBuilder) {
        if (unmatchedRequests.isNotEmpty()) {
            if (exceptionMessage.isNotEmpty()) {
                exceptionMessage.append('\n')
            }
            exceptionMessage.append(
                "Production code made extra requests that your test did not enqueue. " +
                    "Remaining: ${unmatchedRequests.size}.\n${extraRequestDescriptions()}"
            )
        }
    }

    private fun hasResponsesInQueue(): Boolean {
        if (validationTimeout == null) {
            return enqueuedResponses.size != 0
        }

        var timeWaited = 0.milliseconds
        val sleepDuration = 100.milliseconds
        while (enqueuedResponses.size != 0 && timeWaited < validationTimeout) {
            Thread.sleep(sleepDuration.inWholeMilliseconds)
            timeWaited = timeWaited.plus(sleepDuration)
        }
        return enqueuedResponses.size != 0
    }

    private fun remainingMatchersDescription(): String {
        return enqueuedResponses.joinToString { it.requestMatcher.toString() }
    }

    private fun extraRequestDescriptions(): String {
        return unmatchedRequests.joinToString(separator = "\n\n") { it.describe() }
    }

    override fun dispatch(request: RecordedRequest): MockResponse {
        val testRequest = TestRecordedRequest(request)
        val matchedEntry = enqueuedResponses.firstOrNull { entry ->
            entry.requestMatcher.matches(testRequest)
        }

        matchedEntry?.let { capturedEntry ->
            enqueuedResponses.remove(capturedEntry)
            return capturedEntry.responseFactory(testRequest)
        }

        val diagnostics = buildNearMissDiagnostics(testRequest)
        val message = "$request not mocked\n" +
            "Request body params: ${testRequest.bodyParams}\n" +
            diagnostics
        System.err.println("NetworkDispatcher: $message")

        unmatchedRequests.add(
            UnmatchedRequest(
                url = request.requestUrl.toString(),
                method = request.method ?: "UNKNOWN",
                bodyParams = testRequest.bodyParams,
                diagnostics = diagnostics,
            )
        )

        @Suppress("MagicNumber")
        return MockResponse().setResponseCode(500).setBody("Request not mocked")
    }

    private fun buildNearMissDiagnostics(request: TestRecordedRequest): String {
        if (enqueuedResponses.isEmpty()) return "No enqueued mocks to match against."

        val nearestMiss = enqueuedResponses.maxByOrNull { entry ->
            val matcher = entry.requestMatcher
            if (matcher is CompositeRequestMatcher) matcher.passCount(request) else 0
        } ?: return "No enqueued mocks to match against."

        val matcher = nearestMiss.requestMatcher
        val lines = mutableListOf("Nearest mock: $matcher")
        if (matcher is CompositeRequestMatcher) {
            lines.add(matcher.diagnose(request))
        } else {
            val matched = matcher.matches(request)
            lines.add(if (matched) "  + PASS" else "  - FAIL")
        }
        return lines.joinToString("\n")
    }
}

private class Entry(
    val requestMatcher: RequestMatcher,
    val responseFactory: (TestRecordedRequest) -> MockResponse
)

private class UnmatchedRequest(
    val url: String,
    val method: String,
    val bodyParams: Map<String, String>,
    val diagnostics: String,
) {
    fun describe(): String {
        val lines = mutableListOf("$method $url")
        if (bodyParams.isNotEmpty()) {
            lines.add("  Body params: $bodyParams")
        }
        lines.add("  $diagnostics")
        return lines.joinToString("\n")
    }
}

internal class RequestNotFoundException(message: String) : Exception(message)
