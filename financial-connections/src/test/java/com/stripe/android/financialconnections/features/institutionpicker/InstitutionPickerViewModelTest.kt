package com.stripe.android.financialconnections.features.institutionpicker

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.Logger
import com.stripe.android.financialconnections.ApiKeyFixtures
import com.stripe.android.financialconnections.CoroutineTestRule
import com.stripe.android.financialconnections.FinancialConnectionsSheet
import com.stripe.android.financialconnections.TestFinancialConnectionsAnalyticsTracker
import com.stripe.android.financialconnections.domain.FeaturedInstitutions
import com.stripe.android.financialconnections.domain.GetOrFetchSync
import com.stripe.android.financialconnections.domain.NativeAuthFlowCoordinator
import com.stripe.android.financialconnections.domain.PostAuthorizationSession
import com.stripe.android.financialconnections.domain.SearchInstitutions
import com.stripe.android.financialconnections.domain.UpdateLocalManifest
import com.stripe.android.financialconnections.exception.InstitutionPlannedDowntimeError
import com.stripe.android.financialconnections.model.FinancialConnectionsAuthorizationSession
import com.stripe.android.financialconnections.model.FinancialConnectionsInstitution
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.financialconnections.model.InstitutionResponse
import com.stripe.android.financialconnections.navigation.Destination
import com.stripe.android.financialconnections.presentation.Async
import com.stripe.android.financialconnections.presentation.withState
import com.stripe.android.financialconnections.utils.TestHandleError
import com.stripe.android.financialconnections.utils.TestNavigationManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
internal class InstitutionPickerViewModelTest {

    @get:Rule
    val rule: TestRule = CoroutineTestRule(UnconfinedTestDispatcher())

    private val searchInstitutions = mock<SearchInstitutions>()
    private val featuredInstitutions = mock<FeaturedInstitutions>()
    private val sync = mock<GetOrFetchSync>()
    private val handleError = TestHandleError()
    private val updateLocalManifest = mock<UpdateLocalManifest>()
    private val navigationManager = TestNavigationManager()
    private val postAuthorizationSession = mock<PostAuthorizationSession>()
    private val eventTracker = TestFinancialConnectionsAnalyticsTracker()
    private val nativeAuthFlowCoordinator = NativeAuthFlowCoordinator()
    private val defaultConfiguration = FinancialConnectionsSheet.Configuration(
        ApiKeyFixtures.DEFAULT_FINANCIAL_CONNECTIONS_SESSION_SECRET,
        ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY
    )

    private fun buildViewModel(
        state: InstitutionPickerState
    ): InstitutionPickerViewModel {
        return InstitutionPickerViewModel(
            configuration = defaultConfiguration,
            searchInstitutions = searchInstitutions,
            featuredInstitutions = featuredInstitutions,
            getOrFetchSync = sync,
            navigationManager = navigationManager,
            updateLocalManifest = updateLocalManifest,
            logger = Logger.noop(),
            eventTracker = eventTracker,
            postAuthorizationSession = postAuthorizationSession,
            handleError = handleError,
            initialState = state,
            nativeAuthFlowCoordinator = nativeAuthFlowCoordinator,
        )
    }

    @Test
    fun `init - featured institutions are fetched`() = runTest {
        val institutionResponse = InstitutionResponse(
            showManualEntry = false,
            data = listOf(
                FinancialConnectionsInstitution(
                    id = "id",
                    name = "name",
                    url = "url",
                    featured = true,
                    featuredOrder = null,
                    mobileHandoffCapable = false
                )
            )
        )

        givenManifestReturns(ApiKeyFixtures.sessionManifest())
        givenFeaturedInstitutionsReturns(institutionResponse)

        val viewModel = buildViewModel(InstitutionPickerState())

        withState(viewModel) { state ->
            assertEquals(state.payload()!!.featuredInstitutions, institutionResponse)
            assertIs<Async.Uninitialized>(state.searchInstitutions)
        }
    }

    @Test
    fun `init - fails to fetch featured institutions succeeds with empty list`() = runTest {
        val error = RuntimeException("error")
        whenever(featuredInstitutions(defaultConfiguration.financialConnectionsSessionClientSecret))
            .thenThrow(error)

        givenManifestReturns(ApiKeyFixtures.sessionManifest())

        val viewModel = buildViewModel(InstitutionPickerState())

        // payload with empty list
        assertTrue(viewModel.stateFlow.value.payload()!!.featuredInstitutions.data.isEmpty())
    }

    @Test
    fun `init - fail to fetch payload launches error screen`() = runTest {
        val error = RuntimeException("error")
        whenever(sync()).thenThrow(error)

        val viewModel = buildViewModel(InstitutionPickerState())

        withState(viewModel) { state ->
            assertTrue(state.payload() == null)
            handleError.assertError(
                error = error,
                extraMessage = "Error fetching initial payload",
                pane = Pane.INSTITUTION_PICKER,
                displayErrorScreen = true
            )
        }
    }

    @Test
    fun `init - allows back navigation if coming from screen other than signup`() = runTest {
        val featuredResults = InstitutionResponse(
            showManualEntry = false,
            data = listOf(
                FinancialConnectionsInstitution(
                    id = "featured_id",
                    name = "featured_name",
                    url = "featured_url",
                    featured = true,
                    featuredOrder = null,
                    mobileHandoffCapable = false
                )
            )
        )

        givenManifestReturns(ApiKeyFixtures.sessionManifest())
        givenFeaturedInstitutionsReturns(featuredResults)

        nativeAuthFlowCoordinator().test {
            buildViewModel(
                state = InstitutionPickerState(
                    referrer = Pane.CONSENT,
                ),
            )

            val updateMessage = expectMostRecentItem() as NativeAuthFlowCoordinator.Message.UpdateTopAppBar
            assertThat(updateMessage.update.allowBackNavigation).isTrue()
        }
    }

    @Test
    fun `init - does not allow back navigation if coming from signup`() = runTest {
        val featuredResults = InstitutionResponse(
            showManualEntry = false,
            data = listOf(
                FinancialConnectionsInstitution(
                    id = "featured_id",
                    name = "featured_name",
                    url = "featured_url",
                    featured = true,
                    featuredOrder = null,
                    mobileHandoffCapable = false
                )
            )
        )

        givenManifestReturns(ApiKeyFixtures.sessionManifest())
        givenFeaturedInstitutionsReturns(featuredResults)

        nativeAuthFlowCoordinator().test {
            buildViewModel(
                state = InstitutionPickerState(
                    referrer = Pane.LINK_LOGIN,
                ),
            )

            val updateMessage = expectMostRecentItem() as NativeAuthFlowCoordinator.Message.UpdateTopAppBar
            assertThat(updateMessage.update.allowBackNavigation).isFalse()
        }
    }

    @Test
    fun `onQueryChanged - institutions are searched and event sent`() = runTest {
        val query = "query"
        val searchResults = InstitutionResponse(
            showManualEntry = false,
            data = listOf(
                FinancialConnectionsInstitution(
                    id = "id",
                    name = "name",
                    url = "url",
                    featured = false,
                    featuredOrder = null,
                    mobileHandoffCapable = false
                )
            )
        )
        val featuredResults = InstitutionResponse(
            showManualEntry = false,
            data = listOf(
                FinancialConnectionsInstitution(
                    id = "featured_id",
                    name = "featured_name",
                    url = "featured_url",
                    featured = true,
                    featuredOrder = null,
                    mobileHandoffCapable = false
                )
            )
        )

        givenManifestReturns(ApiKeyFixtures.sessionManifest())
        givenFeaturedInstitutionsReturns(featuredResults)
        givenSearchInstitutionsReturns(query, searchResults)

        val viewModel = buildViewModel(InstitutionPickerState())
        viewModel.onQueryChanged(query)
        advanceUntilIdle()

        withState(viewModel) { state ->
            assertEquals(state.payload()!!.featuredInstitutions, featuredResults)
            assertEquals(state.searchInstitutions()!!, searchResults)
            eventTracker.assertContainsEvent(
                expectedEventName = "linked_accounts.search.succeeded",
                expectedParams = mapOf(
                    "pane" to "institution_picker",
                    "query" to query
                )
            )
        }
    }

    @Test
    fun `onQueryChanged - no institutions are searched when blank query`() = runTest {
        val query = "  "

        givenManifestReturns(ApiKeyFixtures.sessionManifest())

        val viewModel = buildViewModel(InstitutionPickerState())
        viewModel.onQueryChanged(query)
        advanceUntilIdle()

        withState(viewModel) { state ->
            verifyNoInteractions(searchInstitutions)
            assertTrue(eventTracker.sentEvents.none { it.eventName == "linked_accounts.search.succeeded" })
            assertEquals(state.searchInstitutions()!!.data, emptyList())
        }
    }

    @Test
    fun `onInstitutionSelected - OAuth institution navigates to partner Auth in modal mode`() = runTest {
        val institution = ApiKeyFixtures.institution()

        givenManifestReturns(ApiKeyFixtures.sessionManifest())
        givenCreateSessionForInstitutionReturns(ApiKeyFixtures.authorizationSession().copy(_isOAuth = true))

        val viewModel = buildViewModel(InstitutionPickerState())

        viewModel.onInstitutionSelected(institution, fromFeatured = true)

        navigationManager.assertNavigatedTo(
            destination = Destination.PartnerAuthDrawer,
            pane = Pane.INSTITUTION_PICKER,
        )
    }

    @Test
    fun `onInstitutionSelected - non-OAuth institution navigates to partner Auth in full-screen mode`() = runTest {
        val institution = ApiKeyFixtures.institution()

        givenManifestReturns(ApiKeyFixtures.sessionManifest())
        givenCreateSessionForInstitutionReturns(ApiKeyFixtures.authorizationSession().copy(_isOAuth = false))

        val viewModel = buildViewModel(InstitutionPickerState())

        viewModel.onInstitutionSelected(institution, fromFeatured = true)

        navigationManager.assertNavigatedTo(
            destination = Destination.PartnerAuth,
            pane = Pane.INSTITUTION_PICKER,
        )
    }

    @Test
    fun `onInstitutionSelected - Failed to create AuthSession navigates to error screen`() = runTest {
        val institution = ApiKeyFixtures.institution()

        givenManifestReturns(ApiKeyFixtures.sessionManifest())
        val error = InstitutionPlannedDowntimeError(
            institution,
            showManualEntry = true,
            isToday = true,
            backUpAt = 10000L,
            stripeException = mock()
        )
        givenCreateSessionForInstitutionThrows(error)

        val viewModel = buildViewModel(InstitutionPickerState())

        viewModel.onInstitutionSelected(institution, fromFeatured = true)

        handleError.assertError(
            error = error,
            extraMessage = "Error selecting or creating session for institution",
            pane = Pane.INSTITUTION_PICKER,
            displayErrorScreen = true
        )
    }

    private suspend fun givenCreateSessionForInstitutionThrows(throwable: Throwable) {
        whenever(postAuthorizationSession(any(), any())).then { throw throwable }
    }

    private suspend fun InstitutionPickerViewModelTest.givenCreateSessionForInstitutionReturns(
        financialConnectionsAuthorizationSession: FinancialConnectionsAuthorizationSession
    ) {
        whenever(postAuthorizationSession(any(), any()))
            .thenReturn(financialConnectionsAuthorizationSession)
    }

    private suspend fun givenManifestReturns(manifest: FinancialConnectionsSessionManifest) {
        whenever(sync()).thenReturn(ApiKeyFixtures.syncResponse(manifest))
    }

    private suspend fun givenSearchInstitutionsReturns(
        query: String,
        institutionResponse: InstitutionResponse
    ) {
        whenever(
            searchInstitutions(
                clientSecret = defaultConfiguration.financialConnectionsSessionClientSecret,
                query = query
            )
        ).thenReturn(
            institutionResponse
        )
    }

    private suspend fun givenFeaturedInstitutionsReturns(institutionResponse: InstitutionResponse) {
        whenever(
            featuredInstitutions(
                clientSecret = defaultConfiguration.financialConnectionsSessionClientSecret
            )
        ).thenReturn(
            institutionResponse
        )
    }
}
