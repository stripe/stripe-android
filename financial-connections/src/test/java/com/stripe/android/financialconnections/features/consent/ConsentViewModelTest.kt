package com.stripe.android.financialconnections.features.consent

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.financialconnections.ApiKeyFixtures
import com.stripe.android.financialconnections.CoroutineTestRule
import com.stripe.android.financialconnections.FinancialConnectionsSheet
import com.stripe.android.financialconnections.domain.AcceptConsent
import com.stripe.android.financialconnections.domain.GetOrFetchSync
import com.stripe.android.financialconnections.domain.LookupAccount
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.financialconnections.model.SynchronizeSessionResponse
import com.stripe.android.financialconnections.model.VisualUpdate
import com.stripe.android.financialconnections.navigation.Destination
import com.stripe.android.uicore.navigation.NavigationIntent
import com.stripe.android.uicore.navigation.NavigationManagerImpl
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class ConsentViewModelTest {

    @get:Rule
    val coroutineTestRule = CoroutineTestRule()

    @Test
    fun `Navigates to Link warmup pane if recognizing a returning Link consumer from prefill details`() = runTest {
        val navigationManager = NavigationManagerImpl()
        val manifest = ApiKeyFixtures.sessionManifest().copy(
            accountholderCustomerEmailAddress = null,
            nextPane = Pane.NETWORKING_LINK_SIGNUP_PANE,
        )

        val getOrFetchSync = mockManifest(manifest)
        val acceptConsent = mockAcceptConsent(manifest)
        val lookupAccount = mockConsumerSessionLookup(exists = true)

        navigationManager.navigationFlow.test {
            val viewModel = ConsentViewModelFactory.create(
                isLinkWithStripe = { true },
                navigationManager = navigationManager,
                getOrFetchSync = getOrFetchSync,
                acceptConsent = acceptConsent,
                lookupAccount = lookupAccount,
                prefillDetails = FinancialConnectionsSheet.ElementsSessionContext.PrefillDetails(
                    email = "email@email.com",
                    phone = null,
                    phoneCountryCode = null,
                ),
            )

            viewModel.onContinueClick()

            assertThat(awaitItem()).isEqualTo(
                NavigationIntent.NavigateTo(
                    route = Destination.NetworkingLinkLoginWarmup(Pane.CONSENT),
                    popUpTo = null,
                    isSingleTop = true,
                )
            )
        }
    }

    @Test
    fun `Navigates to Link signup pane if not recognizing a returning Link consumer from prefill details`() = runTest {
        val navigationManager = NavigationManagerImpl()
        val manifest = ApiKeyFixtures.sessionManifest().copy(
            accountholderCustomerEmailAddress = null,
            nextPane = Pane.NETWORKING_LINK_SIGNUP_PANE,
        )

        val getOrFetchSync = mockManifest(manifest)
        val acceptConsent = mockAcceptConsent(manifest)
        val lookupAccount = mockConsumerSessionLookup(exists = false)

        navigationManager.navigationFlow.test {
            val viewModel = ConsentViewModelFactory.create(
                isLinkWithStripe = { true },
                navigationManager = navigationManager,
                getOrFetchSync = getOrFetchSync,
                acceptConsent = acceptConsent,
                lookupAccount = lookupAccount,
                prefillDetails = FinancialConnectionsSheet.ElementsSessionContext.PrefillDetails(
                    email = "email@email.com",
                    phone = null,
                    phoneCountryCode = null,
                ),
            )

            viewModel.onContinueClick()

            assertThat(awaitItem()).isEqualTo(
                NavigationIntent.NavigateTo(
                    route = Destination.NetworkingLinkSignup(Pane.CONSENT),
                    popUpTo = null,
                    isSingleTop = true,
                )
            )
        }
    }

    @Test
    fun `Doesn't attempt to lookup consumer account when in Financial Connections flow`() = runTest {
        val navigationManager = NavigationManagerImpl()
        val manifest = ApiKeyFixtures.sessionManifest().copy(
            accountholderCustomerEmailAddress = null,
            nextPane = Pane.INSTITUTION_PICKER,
        )

        val getOrFetchSync = mockManifest(manifest)
        val acceptConsent = mockAcceptConsent(manifest)

        navigationManager.navigationFlow.test {
            val viewModel = ConsentViewModelFactory.create(
                isLinkWithStripe = { false },
                navigationManager = navigationManager,
                getOrFetchSync = getOrFetchSync,
                acceptConsent = acceptConsent,
                lookupAccount = throwOnConsumerSessionLookup(),
                prefillDetails = FinancialConnectionsSheet.ElementsSessionContext.PrefillDetails(
                    email = "email@email.com",
                    phone = null,
                    phoneCountryCode = null,
                ),
            )

            viewModel.onContinueClick()

            assertThat(awaitItem()).isEqualTo(
                NavigationIntent.NavigateTo(
                    route = Destination.InstitutionPicker(Pane.CONSENT),
                    popUpTo = null,
                    isSingleTop = true,
                )
            )
        }
    }

    @Test
    fun `Doesn't attempt to lookup consumer account when accountholder customer email is available`() = runTest {
        val navigationManager = NavigationManagerImpl()
        val manifest = ApiKeyFixtures.sessionManifest().copy(
            accountholderCustomerEmailAddress = "account@holder.ca",
            nextPane = Pane.NETWORKING_LINK_SIGNUP_PANE,
        )

        val getOrFetchSync = mockManifest(manifest)
        val acceptConsent = mockAcceptConsent(manifest)

        navigationManager.navigationFlow.test {
            val viewModel = ConsentViewModelFactory.create(
                isLinkWithStripe = { true },
                navigationManager = navigationManager,
                getOrFetchSync = getOrFetchSync,
                acceptConsent = acceptConsent,
                lookupAccount = throwOnConsumerSessionLookup(),
                prefillDetails = FinancialConnectionsSheet.ElementsSessionContext.PrefillDetails(
                    email = "email@email.com",
                    phone = null,
                    phoneCountryCode = null,
                ),
            )

            viewModel.onContinueClick()

            assertThat(awaitItem()).isEqualTo(
                NavigationIntent.NavigateTo(
                    route = Destination.NetworkingLinkSignup(Pane.CONSENT),
                    popUpTo = null,
                    isSingleTop = true,
                )
            )
        }
    }

    private suspend fun mockConsumerSessionLookup(exists: Boolean): LookupAccount {
        val lookupAccount = mock<LookupAccount>()
        val consumerSession = ApiKeyFixtures.consumerSessionLookup(exists = exists)
        whenever(
            lookupAccount(
                email = any(),
                phone = anyOrNull(),
                phoneCountryCode = anyOrNull(),
                emailSource = any(),
                verifiedFlow = any(),
                sessionId = any(),
                pane = any(),
            )
        ).doReturn(consumerSession)
        return lookupAccount
    }

    private suspend fun throwOnConsumerSessionLookup(): LookupAccount {
        val lookupAccount = mock<LookupAccount>()
        whenever(
            lookupAccount(
                email = any(),
                phone = anyOrNull(),
                phoneCountryCode = anyOrNull(),
                emailSource = any(),
                verifiedFlow = any(),
                sessionId = any(),
                pane = any(),
            )
        ).doAnswer {
            throw IllegalStateException("Not expected to be called")
        }
        return lookupAccount
    }

    private suspend fun mockManifest(
        manifest: FinancialConnectionsSessionManifest,
    ): GetOrFetchSync {
        val getOrFetchSync = mock<GetOrFetchSync>()
        whenever(getOrFetchSync(any(), any())).doReturn(
            SynchronizeSessionResponse(
                manifest = manifest,
                text = null,
                visual = VisualUpdate(
                    reducedBranding = false,
                    reducedManualEntryProminenceInErrors = false,
                    merchantLogos = emptyList(),
                ),
            )
        )
        return getOrFetchSync
    }

    private suspend fun mockAcceptConsent(
        manifest: FinancialConnectionsSessionManifest = ApiKeyFixtures.sessionManifest(),
    ): AcceptConsent {
        val acceptConsent = mock<AcceptConsent>()
        whenever(acceptConsent()).doReturn(manifest)
        return acceptConsent
    }
}
