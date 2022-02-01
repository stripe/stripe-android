package com.stripe.android.stripe3ds2.views

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.viewModelScope
import com.google.common.truth.Truth.assertThat
import com.stripe.android.stripe3ds2.observability.FakeErrorReporter
import com.stripe.android.stripe3ds2.transaction.ChallengeAction
import com.stripe.android.stripe3ds2.transaction.ChallengeActionHandler
import com.stripe.android.stripe3ds2.transaction.ChallengeRequestResult
import com.stripe.android.stripe3ds2.transaction.ChallengeRequestResultFixures
import com.stripe.android.stripe3ds2.transaction.FakeTransactionTimer
import com.stripe.android.stripe3ds2.transaction.TransactionTimer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class ChallengeActivityViewModelTest {
    @get:Rule
    val rule = InstantTaskExecutorRule()

    private val testDispatcher = TestCoroutineDispatcher()

    private val actionHandler = object : ChallengeActionHandler {
        override suspend fun submit(action: ChallengeAction): ChallengeRequestResult {
            return ChallengeRequestResultFixures.SUCCESS
        }
    }

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun cleanup() {
        Dispatchers.resetMain()
        testDispatcher.cleanupTestCoroutines()
    }

    @Test
    fun `submit() should trigger challengeRequestResult event`() {
        val viewModel = createViewModel()
        val results = mutableListOf<ChallengeRequestResult>()
        viewModel.challengeRequestResult.observeForever {
            results.add(it)
        }

        viewModel.submit(ChallengeAction.Resend)
        assertThat(results)
            .containsExactly(ChallengeRequestResultFixures.SUCCESS)
    }

    @Test
    fun `transactionTimerJob should be cancelled when viewModelScope is cancelled`() {
        val transactionTimer = FakeTransactionTimer(false, 5000)
        val viewModel = createViewModel(transactionTimer)
        assertThat(viewModel.transactionTimerJob.isActive)
            .isTrue()
        viewModel.viewModelScope.cancel()
        assertThat(viewModel.transactionTimerJob.isCancelled)
            .isTrue()
    }

    @Test
    fun `transactionTimerJob should be cancelled when stopTimer() is called`() {
        val transactionTimer = FakeTransactionTimer(false, 5000)
        val viewModel = createViewModel(transactionTimer)
        assertThat(viewModel.transactionTimerJob.isActive)
            .isTrue()
        viewModel.stopTimer()
        assertThat(viewModel.transactionTimerJob.isCancelled)
            .isTrue()
    }

    private fun createViewModel(
        transactionTimer: TransactionTimer = FakeTransactionTimer(false)
    ): ChallengeActivityViewModel {
        return ChallengeActivityViewModel(
            actionHandler,
            transactionTimer,
            FakeErrorReporter(),
            workContext = testDispatcher
        )
    }
}
