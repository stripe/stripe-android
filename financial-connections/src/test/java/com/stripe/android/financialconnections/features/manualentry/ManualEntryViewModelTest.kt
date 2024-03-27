package com.stripe.android.financialconnections.features.manualentry

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.Logger
import com.stripe.android.financialconnections.ApiKeyFixtures.sessionManifest
import com.stripe.android.financialconnections.ApiKeyFixtures.syncResponse
import com.stripe.android.financialconnections.CoroutineTestRule
import com.stripe.android.financialconnections.TestFinancialConnectionsAnalyticsTracker
import com.stripe.android.financialconnections.domain.GetOrFetchSync
import com.stripe.android.financialconnections.domain.NativeAuthFlowCoordinator
import com.stripe.android.financialconnections.domain.NativeAuthFlowCoordinator.Message.Complete
import com.stripe.android.financialconnections.domain.NativeAuthFlowCoordinator.Message.Complete.EarlyTerminationCause.USER_INITIATED_WITH_CUSTOM_MANUAL_ENTRY
import com.stripe.android.financialconnections.domain.PollAttachPaymentAccount
import com.stripe.android.financialconnections.features.manualentry.ManualEntryState.Payload
import com.stripe.android.financialconnections.mock.TestSuccessContentRepository
import com.stripe.android.financialconnections.model.ManualEntryMode
import com.stripe.android.financialconnections.presentation.Async.Success
import com.stripe.android.financialconnections.presentation.withState
import com.stripe.android.financialconnections.utils.TestNavigationManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class ManualEntryViewModelTest {
    @get:Rule
    val testRule = CoroutineTestRule()

    private val getSync = mock<GetOrFetchSync>()
    private val navigationManager = TestNavigationManager()
    private val pollAttachPaymentAccount = mock<PollAttachPaymentAccount>()
    private val eventTracker = TestFinancialConnectionsAnalyticsTracker()
    private val nativeAuthFlowCoordinator = NativeAuthFlowCoordinator()

    private fun buildViewModel() = ManualEntryViewModel(
        getOrFetchSync = getSync,
        navigationManager = navigationManager,
        logger = Logger.noop(),
        eventTracker = eventTracker,
        initialState = ManualEntryState(),
        pollAttachPaymentAccount = pollAttachPaymentAccount,
        successContentRepository = TestSuccessContentRepository(),
        nativeAuthFlowCoordinator = nativeAuthFlowCoordinator,
    )

    @Test
    fun `init - when custom manual entry, Complete events is emitted`() = runTest {
        val sync = syncResponse(
            sessionManifest().copy(manualEntryMode = ManualEntryMode.CUSTOM)
        )

        whenever(getSync()).thenReturn(sync)

        nativeAuthFlowCoordinator().filterIsInstance<Complete>().test {
            val viewModel = buildViewModel()
            assertThat(awaitItem()).isEqualTo(
                Complete(USER_INITIATED_WITH_CUSTOM_MANUAL_ENTRY)
            )
            withState(viewModel) {
                assertThat(it.payload).isEqualTo(
                    Success(
                        Payload(
                            customManualEntry = true,
                            verifyWithMicrodeposits = sync.manifest.manualEntryUsesMicrodeposits,
                            testMode = false
                        )
                    )
                )
            }
        }
    }
}
