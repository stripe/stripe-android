package com.stripe.android.financialconnections.features.genericerror

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.Logger
import com.stripe.android.financialconnections.ApiKeyFixtures.authorizationSession
import com.stripe.android.financialconnections.ApiKeyFixtures.institution
import com.stripe.android.financialconnections.ApiKeyFixtures.sessionManifest
import com.stripe.android.financialconnections.ApiKeyFixtures.syncResponse
import com.stripe.android.financialconnections.CoroutineTestRule
import com.stripe.android.financialconnections.TestFinancialConnectionsAnalyticsTracker
import com.stripe.android.financialconnections.domain.CancelAuthorizationSession
import com.stripe.android.financialconnections.domain.GetOrFetchSync
import com.stripe.android.financialconnections.domain.NativeAuthFlowCoordinator
import com.stripe.android.financialconnections.domain.UpdateLocalManifest
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.financialconnections.model.GenericErrorPane
import com.stripe.android.financialconnections.model.GenericErrorPane.PrimaryCtaAction
import com.stripe.android.financialconnections.navigation.Destination
import com.stripe.android.financialconnections.repository.GenericErrorContentRepository
import com.stripe.android.financialconnections.utils.TestNavigationManager
import com.stripe.android.testing.ViewModelStoreTestRule
import com.stripe.android.uicore.navigation.NavigationIntent
import com.stripe.android.uicore.navigation.PopUpToBehavior
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.assertIs

@ExperimentalCoroutinesApi
internal class GenericErrorViewModelTest {

    @get:Rule
    val testRule = CoroutineTestRule()

    @get:Rule
    val viewModelStoreRule = ViewModelStoreTestRule()

    private val contentRepository = GenericErrorContentRepository(SavedStateHandle())
    private val getOrFetchSync = mock<GetOrFetchSync>()
    private val cancelAuthorizationSession = mock<CancelAuthorizationSession>()
    private val updateLocalManifest = mock<UpdateLocalManifest>()
    private val navigationManager = TestNavigationManager()
    private val eventTracker = TestFinancialConnectionsAnalyticsTracker()
    private val nativeAuthFlowCoordinator = NativeAuthFlowCoordinator()

    @Test
    fun `init - tracks the pane as loaded`() = runTest {
        buildViewModel()

        eventTracker.assertContainsEvent(
            expectedEventName = "linked_accounts.pane.loaded",
            expectedParams = mapOf("pane" to "generic_error"),
        )
    }

    @Test
    fun `init - exposes the stored pane for rendering`() = runTest {
        val viewModel = buildViewModel()

        assertThat(viewModel.stateFlow.value.pane?.heading)
            .isEqualTo("There was a problem accessing your account")
    }

    @Test
    fun `init - logs an error when the CTA action is one we can't perform`() = runTest {
        buildViewModel(pane = genericErrorPane(primaryCtaAction = null))

        eventTracker.assertContainsEvent(
            expectedEventName = "linked_accounts.error.expected",
            expectedParams = mapOf(
                "pane" to "generic_error",
                "error_type" to "GenericErrorPaneUnknownCtaAction",
            ),
        )
    }

    @Test
    fun `init - moves the user along when there is no content to render`() = runTest {
        buildViewModel(pane = null)

        navigationManager.assertNavigatedTo(
            destination = Destination.InstitutionPicker,
            pane = Pane.GENERIC_ERROR,
            popUpTo = PopUpToBehavior.Current(inclusive = true),
        )
        eventTracker.assertContainsEvent(
            expectedEventName = "linked_accounts.error.expected",
            expectedParams = mapOf("error_type" to "GenericErrorPaneMissingContent"),
        )
    }

    @Test
    fun `restart auth flow - cancels the auth session we're abandoning`() = runTest {
        val viewModel = buildViewModel()

        viewModel.onPrimaryCtaClick().join()

        verify(cancelAuthorizationSession).invoke(authorizationSession().id)
    }

    @Test
    fun `restart auth flow - clears the active auth session so a new one is created`() = runTest {
        val viewModel = buildViewModel()

        viewModel.onPrimaryCtaClick().join()

        val captor = argumentCaptor<
            (FinancialConnectionsSessionManifest) -> FinancialConnectionsSessionManifest
            >()
        verify(updateLocalManifest).invoke(captor.capture())

        val updated = captor.firstValue(
            sessionManifest().copy(activeAuthSession = authorizationSession())
        )
        assertThat(updated.activeAuthSession).isNull()
    }

    @Test
    fun `restart auth flow - hands off to partner auth with auto-launch`() = runTest {
        val viewModel = buildViewModel()

        viewModel.onPrimaryCtaClick().join()

        val intent = navigationManager.emittedIntents.last()
        assertIs<NavigationIntent.NavigateTo>(intent)
        assertThat(intent.route).startsWith(Pane.PARTNER_AUTH.value)
        assertThat(intent.route).contains("auto_launch_auth_session=true")
        assertThat(intent.route).contains("referrer=${Pane.GENERIC_ERROR.value}")
        assertThat(intent.popUpTo).isEqualTo(PopUpToBehavior.Current(inclusive = true))
    }

    @Test
    fun `restart auth flow - tracks the click with the action`() = runTest {
        val viewModel = buildViewModel()

        viewModel.onPrimaryCtaClick().join()

        eventTracker.assertContainsEvent(
            expectedEventName = "linked_accounts.click.primary_cta",
            expectedParams = mapOf(
                "pane" to "generic_error",
                "action" to "restart_auth_flow",
            ),
        )
    }

    @Test
    fun `restart auth flow - falls back to the bank list when there is no institution`() = runTest {
        val viewModel = buildViewModel(
            manifest = sessionManifest().copy(
                activeAuthSession = authorizationSession(),
                activeInstitution = null,
            )
        )

        viewModel.onPrimaryCtaClick().join()

        navigationManager.assertNavigatedTo(
            destination = Destination.InstitutionPicker,
            pane = Pane.GENERIC_ERROR,
            popUpTo = PopUpToBehavior.Current(inclusive = true),
        )
        verify(updateLocalManifest, never()).invoke(any())
    }

    @Test
    fun `unknown action - sends the user to the bank list instead`() = runTest {
        val viewModel = buildViewModel(pane = genericErrorPane(primaryCtaAction = null))

        viewModel.onPrimaryCtaClick().join()

        navigationManager.assertNavigatedTo(
            destination = Destination.InstitutionPicker,
            pane = Pane.GENERIC_ERROR,
            popUpTo = PopUpToBehavior.Current(inclusive = true),
        )
    }

    @Test
    fun `unknown action - tracks the click as unknown`() = runTest {
        val viewModel = buildViewModel(pane = genericErrorPane(primaryCtaAction = null))

        viewModel.onPrimaryCtaClick().join()

        eventTracker.assertContainsEvent(
            expectedEventName = "linked_accounts.click.primary_cta",
            expectedParams = mapOf(
                "pane" to "generic_error",
                "action" to "unknown",
            ),
        )
    }

    private suspend fun buildViewModel(
        pane: GenericErrorPane? = genericErrorPane(primaryCtaAction = PrimaryCtaAction.RestartAuthFlow),
        manifest: FinancialConnectionsSessionManifest = sessionManifest().copy(
            activeAuthSession = authorizationSession(),
            activeInstitution = institution(),
        ),
    ): GenericErrorViewModel {
        pane?.let { contentRepository.set(it) }
        whenever(getOrFetchSync(any(), any())).doReturn(syncResponse(manifest))

        return GenericErrorViewModel(
            initialState = GenericErrorState(),
            nativeAuthFlowCoordinator = nativeAuthFlowCoordinator,
            contentRepository = contentRepository,
            getOrFetchSync = getOrFetchSync,
            cancelAuthorizationSession = cancelAuthorizationSession,
            updateLocalManifest = updateLocalManifest,
            navigationManager = navigationManager,
            eventTracker = eventTracker,
            logger = Logger.noop(),
        ).also { viewModelStoreRule.track(it) }
    }

    private fun genericErrorPane(primaryCtaAction: PrimaryCtaAction?) = GenericErrorPane(
        heading = "There was a problem accessing your account",
        subheading = "Please try again and be sure to select **Profile information**.",
        primaryCta = "Try again",
        primaryCtaAction = primaryCtaAction,
        iconUrl = "https://b.stripecdn.com/icon.png",
        imageUrl = "https://b.stripecdn.com/image.png",
    )
}
