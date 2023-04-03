package com.stripe.android.financialconnections.features.institutionpicker

import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.test.MavericksTestRule
import com.airbnb.mvrx.withState
import com.stripe.android.core.Logger
import com.stripe.android.financialconnections.ApiKeyFixtures
import com.stripe.android.financialconnections.FinancialConnectionsSheet
import com.stripe.android.financialconnections.TestFinancialConnectionsAnalyticsTracker
import com.stripe.android.financialconnections.domain.FeaturedInstitutions
import com.stripe.android.financialconnections.domain.GetManifest
import com.stripe.android.financialconnections.domain.SearchInstitutions
import com.stripe.android.financialconnections.domain.UpdateLocalManifest
import com.stripe.android.financialconnections.model.FinancialConnectionsInstitution
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest
import com.stripe.android.financialconnections.model.InstitutionResponse
import com.stripe.android.financialconnections.navigation.NavigationManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
internal class InstitutionPickerViewModelTest {

    @get:Rule
    val mavericksTestRule = MavericksTestRule()

    private val searchInstitutions = mock<SearchInstitutions>()
    private val featuredInstitutions = mock<FeaturedInstitutions>()
    private val getManifest = mock<GetManifest>()
    private val updateLocalManifest = mock<UpdateLocalManifest>()
    private val navigationManager = mock<NavigationManager>()
    private val eventTracker = TestFinancialConnectionsAnalyticsTracker()
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
            getManifest = getManifest,
            navigationManager = navigationManager,
            updateLocalManifest = updateLocalManifest,
            logger = Logger.noop(),
            eventTracker = eventTracker,
            initialState = state
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
            assertEquals(state.payload()!!.featuredInstitutions, institutionResponse.data)
            assertIs<Uninitialized>(state.searchInstitutions)
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
            assertEquals(state.payload()!!.featuredInstitutions, featuredResults.data)
            assertEquals(state.searchInstitutions()!!.data, searchResults.data)
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

    private suspend fun givenManifestReturns(manifest: FinancialConnectionsSessionManifest) {
        whenever(getManifest()).thenReturn(manifest)
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
