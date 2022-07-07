package com.stripe.android.financialconnections.presentation

import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.test.MvRxTestRule
import com.airbnb.mvrx.withState
import com.stripe.android.core.Logger
import com.stripe.android.financialconnections.ApiKeyFixtures
import com.stripe.android.financialconnections.FinancialConnectionsSheet
import com.stripe.android.financialconnections.domain.NativeAuthFlowCoordinator
import com.stripe.android.financialconnections.domain.PostAuthorizationSession
import com.stripe.android.financialconnections.domain.SearchInstitutions
import com.stripe.android.financialconnections.features.institutionpicker.InstitutionPickerState
import com.stripe.android.financialconnections.features.institutionpicker.InstitutionPickerViewModel
import com.stripe.android.financialconnections.model.Institution
import com.stripe.android.financialconnections.model.InstitutionResponse
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals
import kotlin.test.assertIs

internal class InstitutionPickerViewModelTest {

    @get:Rule
    val mvrxRule = MvRxTestRule()

    private val searchInstitutions = mock<SearchInstitutions>()
    private val postAuthorizationSession = mock<PostAuthorizationSession>()
    private val nativeAuthFlowCoordinator = mock<NativeAuthFlowCoordinator>()
    private val defaultConfiguration = FinancialConnectionsSheet.Configuration(
        ApiKeyFixtures.DEFAULT_FINANCIAL_CONNECTIONS_SESSION_SECRET,
        ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY
    )

    private fun buildViewModel(
        state: InstitutionPickerState
    ) = InstitutionPickerViewModel(
        configuration = defaultConfiguration,
        searchInstitutions = searchInstitutions,
        postAuthorizationSession = postAuthorizationSession,
        nativeAuthFlowCoordinator = nativeAuthFlowCoordinator,
        logger = Logger.noop(),
        initialState = state
    )

    @Test
    fun `init - featured institutions are fetched`() = runTest {
        val institutionResponse = InstitutionResponse(
            listOf(
                Institution(
                    id = "id",
                    name = "name",
                    url = "url",
                    featured = true,
                    featuredOrder = null
                )
            )
        )
        givenFeaturedInstitutionsReturns(institutionResponse)

        val viewModel = buildViewModel(InstitutionPickerState())

        withState(viewModel) { state ->
            assertEquals(state.featuredInstitutions()!!.data, institutionResponse.data)
            assertIs<Uninitialized>(state.searchInstitutions)
        }
    }

    @Test
    fun `onQueryChanged - institutions are searched`() = runTest {
        val query = "query"
        val searchResults = InstitutionResponse(
            listOf(
                Institution(
                    id = "id",
                    name = "name",
                    url = "url",
                    featured = false,
                    featuredOrder = null
                )
            )
        )
        val featuredResults = InstitutionResponse(
            listOf(
                Institution(
                    id = "featured_id",
                    name = "featured_name",
                    url = "featured_url",
                    featured = true,
                    featuredOrder = null
                )
            )
        )
        givenFeaturedInstitutionsReturns(featuredResults)
        givenSearchInstitutionsReturns(query, searchResults)

        val viewModel = buildViewModel(InstitutionPickerState())
        viewModel.onQueryChanged(query)
        advanceUntilIdle()

        withState(viewModel) { state ->
            assertEquals(state.featuredInstitutions()!!.data, featuredResults.data)
            assertEquals(state.searchInstitutions()!!.data, searchResults.data)
        }
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
            searchInstitutions(
                clientSecret = defaultConfiguration.financialConnectionsSessionClientSecret
            )
        ).thenReturn(
            institutionResponse
        )
    }
}
