package com.stripe.android.financialconnections.lite

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.stripe.android.core.Logger
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityArgs
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityArgs.ForData
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityResult
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityResult.Failed
import com.stripe.android.financialconnections.lite.FinancialConnectionsLiteViewModel.ViewEffect
import com.stripe.android.financialconnections.lite.TextFixtures.configuration
import com.stripe.android.financialconnections.lite.TextFixtures.financialConnectionsSessionNoAccounts
import com.stripe.android.financialconnections.lite.TextFixtures.syncResponse
import com.stripe.android.financialconnections.lite.repository.FinancialConnectionsLiteRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class FinancialConnectionsLiteViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private val logger: Logger = Logger.noop()

    private fun viewModel(
        args: FinancialConnectionsSheetActivityArgs = ForData(configuration),
        repository: FinancialConnectionsLiteRepository
    ) = FinancialConnectionsLiteViewModel(
        logger = logger,
        savedStateHandle = SavedStateHandle(
            mapOf(FinancialConnectionsSheetActivityArgs.EXTRA_ARGS to args)
        ),
        repository = repository,
        workContext = testDispatcher,
        applicationId = "com.example"
    )

    @Test
    fun `test view model initializes with correct state`() = testScope.runTest {
        val viewModel = viewModel(
            repository = TestFinancialConnectionsLiteRepository(
                synchronizeResponse = Result.success(syncResponse),
                sessionResponse = Result.success(financialConnectionsSessionNoAccounts)
            )
        )

        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(state!!.successUrl, syncResponse.manifest.successUrl)
        assertEquals(state.cancelUrl, syncResponse.manifest.cancelUrl)
        assertEquals(state.hostedAuthUrl, "${syncResponse.manifest.hostedAuthUrl}&launched_by=android_sdk")
    }

    @Test
    fun `test view effect emits error if sync fails`() = testScope.runTest {
        // Arrange: Return failure for synchronize response
        val expectedError = RuntimeException("Network Error")
        viewModel(
            repository = TestFinancialConnectionsLiteRepository(
                synchronizeResponse = Result.failure(expectedError),
                sessionResponse = Result.success(financialConnectionsSessionNoAccounts)
            )
        ).viewEffects.test {
            testDispatcher.scheduler.advanceUntilIdle()
            val viewEffect = awaitItem() as ViewEffect.FinishWithResult
            val failedResult = viewEffect.result as Failed
            assertEquals(expectedError, (failedResult.error))
        }
    }

    @Test
    fun `handleUrl - success URL should complete session for data flow`() = testScope.runTest {
        val viewModel = viewModel(
            repository = TestFinancialConnectionsLiteRepository(
                synchronizeResponse = Result.success(syncResponse),
                sessionResponse = Result.success(financialConnectionsSessionNoAccounts)
            )
        )

        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.viewEffects.test {
            viewModel.handleUrl(syncResponse.manifest.successUrl!!)
            val viewEffect = awaitItem() as ViewEffect.FinishWithResult
            val completedResult = viewEffect.result as FinancialConnectionsSheetActivityResult.Completed
            assertEquals(financialConnectionsSessionNoAccounts, completedResult.financialConnectionsSession)
        }
    }

    @Test
    fun `handleUrl - cancel URL should cancel session for data flow`() = testScope.runTest {
        val viewModel = viewModel(
            repository = TestFinancialConnectionsLiteRepository(
                synchronizeResponse = Result.success(syncResponse),
                sessionResponse = Result.success(financialConnectionsSessionNoAccounts)
            )
        )

        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.viewEffects.test {
            viewModel.handleUrl(syncResponse.manifest.cancelUrl!!)
            val viewEffect = awaitItem() as ViewEffect.FinishWithResult
            val canceledResult = viewEffect.result as FinancialConnectionsSheetActivityResult.Canceled
            assertEquals(FinancialConnectionsSheetActivityResult.Canceled, canceledResult)
        }
    }
}
