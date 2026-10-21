package com.stripe.android.financialconnections.lite

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterValuesProvider
import com.stripe.android.core.Logger
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityArgs
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityArgs.ForData
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityArgs.ForInstantDebits
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityArgs.ForToken
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityResult
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityResult.Failed
import com.stripe.android.financialconnections.lite.FinancialConnectionsLiteViewModel.ViewEffect
import com.stripe.android.financialconnections.lite.TextFixtures.configuration
import com.stripe.android.financialconnections.lite.TextFixtures.financialConnectionsSessionNoAccounts
import com.stripe.android.financialconnections.lite.TextFixtures.syncResponse
import com.stripe.android.financialconnections.lite.repository.FinancialConnectionsLiteRepository
import com.stripe.android.financialconnections.lite.repository.model.SynchronizeSessionResponse
import com.stripe.android.testing.ViewModelStoreTestRule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestParameterInjector
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestParameterInjector::class)
internal class FinancialConnectionsLiteViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val viewModelStoreRule = ViewModelStoreTestRule()

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private val logger: Logger = Logger.noop()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

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
    ).also { viewModelStoreRule.track(it) }

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

    @Test
    fun `handleUrl - success result query completes instant debits`() = testScope.runTest {
        val viewModel = viewModel(
            args = ForInstantDebits(configuration),
            repository = repository()
        )
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.viewEffects.test {
            viewModel.handleUrl(
                "${syncResponse.manifest.successUrl}?payment_method=eyJpZCI6InBtXzEyMyJ9" +
                    "&last4=6789&bank_name=Test%20Bank&incentive_eligible=true"
            )
            testDispatcher.scheduler.advanceUntilIdle()

            val result = (awaitItem() as ViewEffect.FinishWithResult).result as
                FinancialConnectionsSheetActivityResult.Completed
            assertThat(result.instantDebits?.encodedPaymentMethod).isEqualTo("{\"id\":\"pm_123\"}")
            assertThat(result.instantDebits?.last4).isEqualTo("6789")
            assertThat(result.instantDebits?.bankName).isEqualTo("Test Bank")
            assertThat(result.instantDebits?.eligibleForIncentive).isTrue()
            expectNoEvents()
        }
    }

    @Test
    fun `handleUrl - cancel with query cancels token flow`() = testScope.runTest {
        val viewModel = viewModel(
            args = ForToken(configuration),
            repository = repository()
        )
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.viewEffects.test {
            viewModel.handleUrl("${syncResponse.manifest.cancelUrl}?reason=user_cancelled")
            testDispatcher.scheduler.advanceUntilIdle()

            assertThat((awaitItem() as ViewEffect.FinishWithResult).result)
                .isEqualTo(FinancialConnectionsSheetActivityResult.Canceled)
            expectNoEvents()
        }
    }

    @Test
    fun `handleUrl - callback lookalike cannot complete data flow`(
        @TestParameter(valuesProvider = InvalidSuccessUrlProvider::class) url: RedirectCase
    ) = testScope.runTest {
        val viewModel = viewModel(repository = repository())
            testDispatcher.scheduler.advanceUntilIdle()
            viewModel.viewEffects.test {
                viewModel.handleUrl(url.url)
                testDispatcher.scheduler.advanceUntilIdle()

                assertThat(awaitItem()).isEqualTo(ViewEffect.OpenCustomTab(url.url))
                expectNoEvents()
            }
    }

    @Test
    fun `handleUrl - cancellation lookalike cannot cancel token flow`(
        @TestParameter(valuesProvider = InvalidCancelUrlProvider::class) url: RedirectCase
    ) = testScope.runTest {
        val viewModel = viewModel(args = ForToken(configuration), repository = repository())
            testDispatcher.scheduler.advanceUntilIdle()
            viewModel.viewEffects.test {
                viewModel.handleUrl(url.url)
                testDispatcher.scheduler.advanceUntilIdle()

                assertThat(awaitItem()).isEqualTo(ViewEffect.OpenCustomTab(url.url))
                expectNoEvents()
            }
    }

    @Test
    fun `handleUrl - forged instant debits result on lookalike URL cannot complete`() = testScope.runTest {
        val url = "https://attacker.example/?next=${syncResponse.manifest.successUrl}" +
            "&payment_method=eyJpZCI6InBtXzEyMyJ9"
        val viewModel = viewModel(args = ForInstantDebits(configuration), repository = repository())
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.viewEffects.test {
            viewModel.handleUrl(url)
            testDispatcher.scheduler.advanceUntilIdle()

            assertThat(awaitItem()).isEqualTo(ViewEffect.OpenCustomTab(url))
            expectNoEvents()
        }
    }

    @Test
    fun `handleUrl - expected query requirement cannot be removed or replaced`() = testScope.runTest {
        val success = requireNotNull(syncResponse.manifest.successUrl)
        val response = syncResponse.copy(
            manifest = syncResponse.manifest.copy(successUrl = "$success?state=expected")
        )
        val viewModel = viewModel(repository = repository(response))
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.viewEffects.test {
            val missing = "$success?linked_account=fca_123"
            val replaced = "$success?state=wrong&linked_account=fca_123"
            val duplicated = "$success?state=expected&state=wrong"
            val encodedDuplicate = "$success?state=expected&st%61te=wrong"
            listOf(missing, replaced, duplicated, encodedDuplicate).forEach { url ->
                viewModel.handleUrl(url)
                testDispatcher.scheduler.advanceUntilIdle()
                assertThat(awaitItem()).isEqualTo(ViewEffect.OpenCustomTab(url))
            }

            viewModel.handleUrl("$success?state=expected&linked_account=fca_123")
            testDispatcher.scheduler.advanceUntilIdle()
            assertThat((awaitItem() as ViewEffect.FinishWithResult).result)
                .isInstanceOf(FinancialConnectionsSheetActivityResult.Completed::class.java)
            expectNoEvents()
        }
    }

    @Test
    fun `handleUrl - a protocol relative expected endpoint cannot complete`() = testScope.runTest {
        val url = "//link-accounts/com.example/success"
        val response = syncResponse.copy(manifest = syncResponse.manifest.copy(successUrl = url))
        val viewModel = viewModel(repository = repository(response))
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.viewEffects.test {
            viewModel.handleUrl(url)
            testDispatcher.scheduler.advanceUntilIdle()

            assertThat(awaitItem()).isEqualTo(ViewEffect.OpenCustomTab(url))
            expectNoEvents()
        }
    }

    @Test
    fun `handleUrl - duplicate required query keys cannot complete`() = testScope.runTest {
        val success = requireNotNull(syncResponse.manifest.successUrl)
        val url = "$success?state=expected"
        val response = syncResponse.copy(
            manifest = syncResponse.manifest.copy(successUrl = "$url&st%61te=expected")
        )
        val viewModel = viewModel(repository = repository(response))
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.viewEffects.test {
            viewModel.handleUrl(url)
            testDispatcher.scheduler.advanceUntilIdle()

            assertThat(awaitItem()).isEqualTo(ViewEffect.OpenCustomTab(url))
            expectNoEvents()
        }
    }

    @Test
    fun `handleUrl - accepted terminal callback finishes once`() = testScope.runTest {
        val repository = repository()
        val viewModel = viewModel(repository = repository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.viewEffects.test {
            viewModel.handleUrl(requireNotNull(syncResponse.manifest.successUrl))
            viewModel.handleUrl(requireNotNull(syncResponse.manifest.cancelUrl))
            testDispatcher.scheduler.advanceUntilIdle()

            assertThat((awaitItem() as ViewEffect.FinishWithResult).result)
                .isInstanceOf(FinancialConnectionsSheetActivityResult.Completed::class.java)
            expectNoEvents()
        }

        repository.getSessionCalls.awaitItem()
        repository.getSessionCalls.ensureAllEventsConsumed()
    }

    private fun repository(
        response: SynchronizeSessionResponse = syncResponse
    ) = TestFinancialConnectionsLiteRepository(
        synchronizeResponse = Result.success(response),
        sessionResponse = Result.success(financialConnectionsSessionNoAccounts)
    )
}

internal data class RedirectCase(val name: String, val url: String) {
    override fun toString(): String = name
}

internal object InvalidSuccessUrlProvider : TestParameterValuesProvider() {
    override fun provideValues(context: TestParameterValuesProvider.Context?): List<RedirectCase> {
        val success = requireNotNull(TextFixtures.syncResponse.manifest.successUrl)
        return listOf(
            RedirectCase("query embedding", "https://attacker.example/?return=$success"),
            RedirectCase("fragment embedding", "https://attacker.example/#$success"),
            RedirectCase("path embedding", "stripe-auth://link-accounts/other/$success"),
            RedirectCase("path suffix", "$success/extra"),
            RedirectCase("encoded suffix", "stripe-auth://link-accounts/com.example/success%2Fextra"),
            RedirectCase("encoded separator", "stripe-auth://link-accounts/com.example%2Fsuccess"),
            RedirectCase("wrong application", "stripe-auth://link-accounts/other.app/success?next=$success"),
            RedirectCase("wrong host", "stripe-auth://other-host/com.example/success?next=$success"),
            RedirectCase("unexpected port", "stripe-auth://link-accounts:443/com.example/success"),
            RedirectCase("user info", "stripe-auth://attacker@link-accounts/com.example/success"),
            RedirectCase("wrong scheme", "https://link-accounts/com.example/success?next=$success"),
            RedirectCase("unexpected fragment", "$success#unexpected"),
            RedirectCase("malformed escape", "$success%"),
        )
    }
}

internal object InvalidCancelUrlProvider : TestParameterValuesProvider() {
    override fun provideValues(context: TestParameterValuesProvider.Context?): List<RedirectCase> {
        val cancel = requireNotNull(TextFixtures.syncResponse.manifest.cancelUrl)
        return listOf(
            RedirectCase("query embedding", "https://attacker.example/?return=$cancel"),
            RedirectCase("path suffix", "$cancel/extra"),
            RedirectCase("unexpected fragment", "$cancel#unexpected"),
            RedirectCase("user info", "stripe-auth://attacker@link-accounts/com.example/cancel"),
        )
    }
}
