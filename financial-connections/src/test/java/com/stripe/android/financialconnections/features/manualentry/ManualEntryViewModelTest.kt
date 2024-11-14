package com.stripe.android.financialconnections.features.manualentry

import androidx.lifecycle.SavedStateHandle
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
import com.stripe.android.financialconnections.domain.UpdateCachedAccounts
import com.stripe.android.financialconnections.features.manualentry.ManualEntryState.Payload
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.financialconnections.model.LinkAccountSessionPaymentAccount
import com.stripe.android.financialconnections.model.ManualEntryMode
import com.stripe.android.financialconnections.navigation.Destination
import com.stripe.android.financialconnections.presentation.Async.Success
import com.stripe.android.financialconnections.presentation.withState
import com.stripe.android.financialconnections.repository.SuccessContentRepository
import com.stripe.android.financialconnections.utils.TestNavigationManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class ManualEntryViewModelTest {
    @get:Rule
    val testRule = CoroutineTestRule()

    private val getSync = mock<GetOrFetchSync>()
    private val navigationManager = TestNavigationManager()
    private val pollAttachPaymentAccount = mock<PollAttachPaymentAccount>()
    private val eventTracker = TestFinancialConnectionsAnalyticsTracker()
    private val updateCachedAccounts = mock<UpdateCachedAccounts>()
    private val nativeAuthFlowCoordinator = NativeAuthFlowCoordinator()

    private fun buildViewModel() = ManualEntryViewModel(
        initialState = ManualEntryState(),
        nativeAuthFlowCoordinator = nativeAuthFlowCoordinator,
        pollAttachPaymentAccount = pollAttachPaymentAccount,
        successContentRepository = SuccessContentRepository(SavedStateHandle()),
        eventTracker = eventTracker,
        getOrFetchSync = getSync,
        navigationManager = navigationManager,
        updateCachedAccounts = updateCachedAccounts,
        logger = Logger.noop(),
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

    @Test
    fun `onSubmit - when manual entry account is attached, clears linked accounts and navigates to next pane`() =
        runTest {
            val nextPane = FinancialConnectionsSessionManifest.Pane.SUCCESS
            whenever(getSync()).thenReturn(syncResponse())
            whenever(
                pollAttachPaymentAccount(
                    sync = any(),
                    activeInstitution = anyOrNull(),
                    params = any()
                )
            ).thenReturn(
                LinkAccountSessionPaymentAccount(
                    id = "1234",
                    nextPane = nextPane
                )
            )
            val viewModel = buildViewModel()
            viewModel.onSubmit()
            navigationManager.assertNavigatedTo(
                destination = Destination.Success,
                pane = Pane.MANUAL_ENTRY,
            )
            verify(updateCachedAccounts).invoke(eq(emptyList()))
        }
}
