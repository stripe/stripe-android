package com.stripe.android

import com.nhaarman.mockitokotlin2.mock
import com.stripe.android.model.StripeModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestCoroutineScope
import org.junit.Assert.assertTrue
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class ApiResultCoroutineBridgeTest {

    private val scope = TestCoroutineScope()

    @Test
    fun onSuccess_shouldResumeCoroutine() {
        var callback: ApiResultCallback<StripeModel>? = null
        val deferred = scope.async<StripeModel> {
            suspendApiResultCoroutine { callback = it }
        }

        assertFalse(deferred.isCompleted)

        val result = mock<StripeModel>()
        callback!!.onSuccess(result)

        assertTrue(deferred.isCompleted)

        runBlocking {
            assertEquals(result, deferred.await())
        }
    }

    @Test
    fun onError_shouldResumeCoroutineWithException() {
        var callback: ApiResultCallback<StripeModel>? = null
        val deferred = scope.async<StripeModel> {
            suspendApiResultCoroutine { callback = it }
        }

        assertFalse(deferred.isCompleted)

        callback!!.onError(IllegalStateException("hello world"))

        assertTrue(deferred.isCompleted)

        runBlocking {
            val result = runCatching { deferred.await() }
            val error = result.exceptionOrNull()
            assertTrue(error is IllegalStateException)
            assertEquals("hello world", error?.message)
        }
    }
}
