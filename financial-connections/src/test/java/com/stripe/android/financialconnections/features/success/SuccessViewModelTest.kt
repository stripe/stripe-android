package com.stripe.android.financialconnections.features.success

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.Logger
import com.stripe.android.financialconnections.ApiKeyFixtures
import com.stripe.android.financialconnections.CoroutineTestRule
import com.stripe.android.financialconnections.TestFinancialConnectionsAnalyticsTracker
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsEvent.PaneLoaded
import com.stripe.android.financialconnections.domain.GetCachedAccounts
import com.stripe.android.financialconnections.domain.GetManifest
import com.stripe.android.financialconnections.domain.NativeAuthFlowCoordinator
import com.stripe.android.financialconnections.domain.NativeAuthFlowCoordinator.Message.Complete
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.financialconnections.repository.PersistingRepository
import com.stripe.android.financialconnections.repository.SuccessContentRepository
import com.stripe.android.financialconnections.ui.TextResource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@Suppress("MaxLineLength")
internal class SuccessViewModelTest {

    @get:Rule
    val testRule = CoroutineTestRule()

    private val getManifest = mock<GetManifest>()
    private val eventTracker = TestFinancialConnectionsAnalyticsTracker()
    private val nativeAuthFlowCoordinator = NativeAuthFlowCoordinator()
    private val getCachedAccounts = mock<GetCachedAccounts>()

    private fun buildViewModel(
        state: SuccessState
    ) = SuccessViewModel(
        getManifest = getManifest,
        logger = Logger.noop(),
        eventTracker = eventTracker,
        initialState = state,
        nativeAuthFlowCoordinator = nativeAuthFlowCoordinator,
        successContentRepository = fakeSuccessContentRepository(),
        getCachedAccounts = getCachedAccounts,
    )

    @Test
    fun `init - when skipSuccessPane is true, complete session and emit Finish`() = runTest {
        val accounts = ApiKeyFixtures.partnerAccountList().data
        val manifest = ApiKeyFixtures.sessionManifest().copy(
            skipSuccessPane = true,
            activeAuthSession = ApiKeyFixtures.authorizationSession(),
            activeInstitution = ApiKeyFixtures.institution()
        )
        whenever(getCachedAccounts()).thenReturn(accounts)
        whenever(getManifest()).thenReturn(manifest)

        nativeAuthFlowCoordinator().filterIsInstance<Complete>().test {
            buildViewModel(SuccessState())
            // Triggers flow termination.
            assertThat(eventTracker.sentEvents).isEmpty()
            assertThat(awaitItem()).isEqualTo(Complete())
        }
    }

    @Test
    fun `init - when skipSuccessPane is false, session is not auto completed`() = runTest {
        val accounts = ApiKeyFixtures.partnerAccountList()
        val manifest = ApiKeyFixtures.sessionManifest().copy(
            skipSuccessPane = false,
            activeAuthSession = ApiKeyFixtures.authorizationSession(),
            activeInstitution = ApiKeyFixtures.institution()
        )
        whenever(getCachedAccounts()).thenReturn(accounts.data)
        whenever(getManifest()).thenReturn(manifest)

        nativeAuthFlowCoordinator().filterIsInstance<Complete>().test {
            buildViewModel(SuccessState())
            assertThat(eventTracker.sentEvents).containsExactly(
                PaneLoaded(
                    pane = Pane.SUCCESS,
                )
            )
            expectNoEvents()
        }
    }

    @Test
    fun `onDoneClick - complete session is triggered`() = runTest {
        nativeAuthFlowCoordinator().filterIsInstance<Complete>().test {
            buildViewModel(SuccessState()).onDoneClick()
            assertThat(awaitItem()).isEqualTo(Complete())
        }
    }

    private fun fakeSuccessContentRepository(
        initialState: SuccessContentRepository.State = SuccessContentRepository.State(
            customSuccessMessage = TextResource.Text("Yay!"),
        ),
    ): SuccessContentRepository {
        return SuccessContentRepository(
            savedStateHandle = SavedStateHandle(
                initialState = mapOf(
                    PersistingRepository.KeyState to initialState,
                )
            ),
        )
    }
}
