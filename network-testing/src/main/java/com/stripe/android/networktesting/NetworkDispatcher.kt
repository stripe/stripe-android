package com.stripe.android.networktesting

import android.util.Log
import com.stripe.android.networktesting.RequestMatchers.composite
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import java.util.Queue
import java.util.concurrent.ConcurrentLinkedQueue

internal class NetworkDispatcher : Dispatcher() {
    private val enqueuedResponses: Queue<Entry> = ConcurrentLinkedQueue()

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
    }

    fun numberRemainingInQueue(): Int {
        return enqueuedResponses.size
    }

    fun remainingMatchersDescription(): String {
        return enqueuedResponses.joinToString { it.requestMatcher.toString() }
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
        Log.d("NetworkDispatcher", "Request not found.", exception)
        android.os.Process.killProcess(android.os.Process.myPid())
        throw exception
    }
}

private class Entry(
    val requestMatcher: RequestMatcher,
    val responseFactory: (TestRecordedRequest) -> MockResponse
)

internal class RequestNotFoundException(message: String) : Exception(message)
