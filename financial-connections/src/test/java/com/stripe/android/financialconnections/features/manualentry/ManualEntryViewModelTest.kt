package com.stripe.android.financialconnections.features.manualentry

import app.cash.turbine.test
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.test.MavericksTestRule
import com.airbnb.mvrx.withState
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.Logger
import com.stripe.android.financialconnections.ApiKeyFixtures.sessionManifest
import com.stripe.android.financialconnections.TestFinancialConnectionsAnalyticsTracker
import com.stripe.android.financialconnections.domain.GetManifest
import com.stripe.android.financialconnections.domain.GoNext
import com.stripe.android.financialconnections.domain.NativeAuthFlowCoordinator
import com.stripe.android.financialconnections.domain.NativeAuthFlowCoordinator.Message.Terminate
import com.stripe.android.financialconnections.domain.NativeAuthFlowCoordinator.Message.Terminate.EarlyTerminationCause.USER_INITIATED_WITH_CUSTOM_MANUAL_ENTRY
import com.stripe.android.financialconnections.domain.PollAttachPaymentAccount
import com.stripe.android.financialconnections.features.manualentry.ManualEntryState.Payload
import com.stripe.android.financialconnections.model.ManualEntryMode
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals

class ManualEntryViewModelTest {
    @get:Rule
    val mavericksTestRule = MavericksTestRule()

    private val getManifest = mock<GetManifest>()
    private val goNext = mock<GoNext>()
    private val pollAttachPaymentAccount = mock<PollAttachPaymentAccount>()
    private val eventTracker = TestFinancialConnectionsAnalyticsTracker()
    private val nativeAuthFlowCoordinator = mock<NativeAuthFlowCoordinator>()

    private fun buildViewModel() = ManualEntryViewModel(
        getManifest = getManifest,
        goNext = goNext,
        logger = Logger.noop(),
        eventTracker = eventTracker,
        initialState = ManualEntryState(),
        pollAttachPaymentAccount = pollAttachPaymentAccount,
        nativeAuthFlowCoordinator = nativeAuthFlowCoordinator,
    )

    @Test
    fun `init - when custom manual entry, Terminate events is emitted`() = runTest {
        val manifest = sessionManifest().copy(manualEntryMode = ManualEntryMode.CUSTOM)
        whenever(getManifest()).thenReturn(manifest)
        whenever(nativeAuthFlowCoordinator()).thenReturn(MutableSharedFlow())

        nativeAuthFlowCoordinator().test {
            val viewModel = buildViewModel()
            assertEquals(
                expected = Terminate(USER_INITIATED_WITH_CUSTOM_MANUAL_ENTRY),
                actual = awaitItem(),
            )
            withState(viewModel) {
                assertThat(it.payload).isEqualTo(
                    Success(
                        Payload(
                            customManualEntry = true,
                            verifyWithMicrodeposits = manifest.manualEntryUsesMicrodeposits
                        )
                    )
                )
            }
        }
    }
}
