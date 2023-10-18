package com.stripe.android.financialconnections.features.manualentry

import app.cash.turbine.test
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.test.MavericksTestRule
import com.airbnb.mvrx.withState
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.Logger
import com.stripe.android.financialconnections.ApiKeyFixtures.sessionManifest
import com.stripe.android.financialconnections.ApiKeyFixtures.syncResponse
import com.stripe.android.financialconnections.TestFinancialConnectionsAnalyticsTracker
import com.stripe.android.financialconnections.domain.GetOrFetchSync
import com.stripe.android.financialconnections.domain.NativeAuthFlowCoordinator
import com.stripe.android.financialconnections.domain.NativeAuthFlowCoordinator.Message.Complete
import com.stripe.android.financialconnections.domain.NativeAuthFlowCoordinator.Message.Complete.EarlyTerminationCause.USER_INITIATED_WITH_CUSTOM_MANUAL_ENTRY
import com.stripe.android.financialconnections.domain.PollAttachPaymentAccount
import com.stripe.android.financialconnections.features.manualentry.ManualEntryState.Payload
import com.stripe.android.financialconnections.model.ManualEntryMode
import com.stripe.android.financialconnections.utils.TestNavigationManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class ManualEntryViewModelTest {
    @get:Rule
    val mavericksTestRule = MavericksTestRule()

    private val getSync = mock<GetOrFetchSync>()
    private val navigationManager = TestNavigationManager()
    private val pollAttachPaymentAccount = mock<PollAttachPaymentAccount>()
    private val eventTracker = TestFinancialConnectionsAnalyticsTracker()
    private val nativeAuthFlowCoordinator = mock<NativeAuthFlowCoordinator>()

    private fun buildViewModel() = ManualEntryViewModel(
        getOrFetchSync = getSync,
        navigationManager = navigationManager,
        logger = Logger.noop(),
        eventTracker = eventTracker,
        initialState = ManualEntryState(),
        pollAttachPaymentAccount = pollAttachPaymentAccount,
        nativeAuthFlowCoordinator = nativeAuthFlowCoordinator,
    )

    @Test
    fun `init - when custom manual entry, Complete events is emitted`() = runTest {
        val sync = syncResponse(
            sessionManifest().copy(manualEntryMode = ManualEntryMode.CUSTOM)
        )

        whenever(getSync()).thenReturn(sync)
        whenever(nativeAuthFlowCoordinator()).thenReturn(MutableSharedFlow())

        nativeAuthFlowCoordinator().test {
            val viewModel = buildViewModel()
            assertEquals(
                expected = Complete(USER_INITIATED_WITH_CUSTOM_MANUAL_ENTRY),
                actual = awaitItem(),
            )
            withState(viewModel) {
                assertThat(it.payload).isEqualTo(
                    Success(
                        Payload(
                            customManualEntry = true,
                            verifyWithMicrodeposits = sync.manifest.manualEntryUsesMicrodeposits
                        )
                    )
                )
            }
        }
    }
}
