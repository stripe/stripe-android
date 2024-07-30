package com.stripe.android.stripe3ds2.transaction

import com.google.common.truth.Truth.assertThat
import com.stripe.android.stripe3ds2.ChallengeMessageFixtures
import com.stripe.android.stripe3ds2.transactions.ErrorData
import com.stripe.android.stripe3ds2.transactions.ProtocolError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.mockito.Mockito.verify
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import java.util.concurrent.TimeUnit
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class TransactionTimerTest {
    private val errorRequestExecutor: ErrorRequestExecutor = mock()

    private val errorDataArgumentCaptor = argumentCaptor<ErrorData>()
    private val onReceiverCompletedArgumentCaptor = argumentCaptor<() -> Unit>()

    private val testDispatcher = StandardTestDispatcher()

    private val transactionTimer = DefaultTransactionTimer(
        TIMEOUT,
        errorRequestExecutor,
        CREQ_DATA,
    )

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun cleanup() {
        Dispatchers.resetMain()
    }

    @Test
    fun `start and timeout should call onTimeout`() = runTest {
        transactionTimer.start()

        // advance time beyond timeout threshold
        advanceTimeBy(TimeUnit.MINUTES.toMillis((TIMEOUT + 1).toLong()))

        verify(errorRequestExecutor).executeAsync(errorDataArgumentCaptor.capture())

        assertEquals(
            ProtocolError.TransactionTimedout.code,
            errorDataArgumentCaptor.firstValue.errorCode.toInt()
        )
    }

    @Test
    fun `start and stop before timeout should not call onTimeout`() = runTest {
        val job = backgroundScope.launch {
            transactionTimer.start()
        }

        // call stop() just before timeout is hit, then move past timeout
        advanceTimeBy(TimeUnit.MINUTES.toMillis((TIMEOUT - 1).toLong()))
        job.cancel()
        advanceTimeBy(TimeUnit.MINUTES.toMillis((TIMEOUT + 1).toLong()))

        verify(errorRequestExecutor, never()).executeAsync(any())
    }

    @Test
    fun `after onTimeout(), timeout value should be true`() = runTest {
        transactionTimer.onTimeout()
        transactionTimer.onTimeout()
        transactionTimer.onTimeout()

        // call each onReceiverCompleted callback
        onReceiverCompletedArgumentCaptor.allValues.forEach { it() }

        assertThat(transactionTimer.timeout.value)
            .isTrue()
    }

    private companion object {
        private const val TIMEOUT = 5
        private val CREQ_DATA = ChallengeMessageFixtures.CREQ
    }
}
