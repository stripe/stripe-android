package com.stripe.android.networktesting

import com.stripe.android.networktesting.RequestMatchers.composite
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
    private val extraRequests: MutableList<RecordedRequest> = Collections.synchronizedList(mutableListOf())

    fun enqueue(vararg requestMatcher: RequestMatcher, responseFactory: (MockResponse) -> Unit) {
        enqueuedResponses.add(
            Entry(composite(*requestMatcher)) {
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
        enqueuedResponses.add(
            Entry(composite(*requestMatcher)) {
                val response = MockResponse()
                response.setResponseCode(200)
                responseFactory(it, response)
                response
            }
        )
    }

    fun clear() {
        enqueuedResponses.clear()
        extraRequests.clear()
    }

    fun validate() {
        val exceptionMessage = StringBuilder()
        if (hasResponsesInQueue()) {
            exceptionMessage.append(
                "Mock responses is not empty. Remaining: ${enqueuedResponses.size}.\nRemaining Matchers: " +
                    remainingMatchersDescription()
            )
        }
        if (extraRequests.isNotEmpty()) {
            if (exceptionMessage.isNotEmpty()) {
                exceptionMessage.append('\n')
            }
            exceptionMessage.append(
                "Production code made extra requests that your test did not enqueue. Remaining: " +
                    "${extraRequests.size}.\n${extraRequestDescriptions()}"
            )
        }
        if (exceptionMessage.isNotEmpty()) {
            throw IllegalStateException(exceptionMessage.toString())
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
        return extraRequests.joinToString { it.requestUrl.toString() }
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

        val exception = RequestNotFoundException("$request not mocked\n${testRequest.bodyText}")
        println(exception)

        extraRequests.add(request)

        throw exception
    }
}

private class Entry(
    val requestMatcher: RequestMatcher,
    val responseFactory: (TestRecordedRequest) -> MockResponse
)

internal class RequestNotFoundException(message: String) : Exception(message)
