package com.stripe.android.stripe3ds2.transaction

import com.stripe.android.stripe3ds2.observability.FakeErrorReporter
import com.stripe.android.stripe3ds2.transactions.ErrorDataFixtures
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class StripeErrorRequestExecutorTest {
    private val httpClient = mock<HttpClient>()
    private val testDispatcher = UnconfinedTestDispatcher()
    private val errorRequestExecutor = StripeErrorRequestExecutor(
        httpClient,
        FakeErrorReporter(),
        testDispatcher
    )

    @Test
    fun testExecuteAsync() = runTest {
        errorRequestExecutor.executeAsync(ErrorDataFixtures.ERROR_DATA)

        verify(httpClient)
            .doPostRequest(any(), eq("application/json; charset=utf-8"))
    }
}
