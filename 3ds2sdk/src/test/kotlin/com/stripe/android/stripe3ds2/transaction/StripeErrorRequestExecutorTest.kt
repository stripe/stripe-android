package com.stripe.android.stripe3ds2.transaction

import com.stripe.android.stripe3ds2.observability.FakeErrorReporter
import com.stripe.android.stripe3ds2.transactions.ErrorDataFixtures
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import kotlin.test.AfterTest

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class StripeErrorRequestExecutorTest {
    private val httpClient = mock<HttpClient>()
    private val testDispatcher = TestCoroutineDispatcher()

    @AfterTest
    fun cleanup() {
        testDispatcher.cleanupTestCoroutines()
    }

    @Test
    fun testExecuteAsync() = testDispatcher.runBlockingTest {
        StripeErrorRequestExecutor(httpClient, FakeErrorReporter(), testDispatcher)
            .executeAsync(ErrorDataFixtures.ERROR_DATA)

        verify(httpClient)
            .doPostRequest(any(), eq("application/json; charset=utf-8"))
    }
}
