package com.stripe.android.financialconnections.features.success

import app.cash.turbine.test
import com.airbnb.mvrx.test.MavericksTestRule
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.Logger
import com.stripe.android.financialconnections.ApiKeyFixtures
import com.stripe.android.financialconnections.R
import com.stripe.android.financialconnections.TestFinancialConnectionsAnalyticsTracker
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsEvent.PaneLoaded
import com.stripe.android.financialconnections.domain.GetCachedAccounts
import com.stripe.android.financialconnections.domain.GetManifest
import com.stripe.android.financialconnections.domain.NativeAuthFlowCoordinator
import com.stripe.android.financialconnections.domain.NativeAuthFlowCoordinator.Message.Complete
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.financialconnections.repository.SaveToLinkWithStripeSucceededRepository
import com.stripe.android.financialconnections.ui.TextResource.PluralId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
@Suppress("MaxLineLength")
internal class SuccessViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @get:Rule
    val mavericksRule = MavericksTestRule(testDispatcher = testDispatcher)

    private val getManifest = mock<GetManifest>()
    private val eventTracker = TestFinancialConnectionsAnalyticsTracker()
    private val nativeAuthFlowCoordinator = mock<NativeAuthFlowCoordinator>()
    private val getCachedAccounts = mock<GetCachedAccounts>()
    private val saveToLinkWithStripeSucceeded = mock<SaveToLinkWithStripeSucceededRepository>()

    private fun buildViewModel(
        state: SuccessState
    ) = SuccessViewModel(
        getManifest = getManifest,
        logger = Logger.noop(),
        eventTracker = eventTracker,
        initialState = state,
        nativeAuthFlowCoordinator = nativeAuthFlowCoordinator,
        getCachedAccounts = getCachedAccounts,
        saveToLinkWithStripeSucceeded = saveToLinkWithStripeSucceeded,
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

        whenever(nativeAuthFlowCoordinator()).thenReturn(MutableSharedFlow())

        nativeAuthFlowCoordinator().test {
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

        whenever(nativeAuthFlowCoordinator()).thenReturn(MutableSharedFlow())

        nativeAuthFlowCoordinator().test {
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
        whenever(nativeAuthFlowCoordinator()).thenReturn(MutableSharedFlow())
        nativeAuthFlowCoordinator().test {
            buildViewModel(SuccessState()).onDoneClick()
            assertEquals(
                expected = Complete(),
                actual = awaitItem(),
            )
        }
    }

    @Test
    fun `getSuccessMessage - link with stripe and connected account name and business name`() {
        val result = buildViewModel(SuccessState()).getSuccessMessages(
            isLinkWithStripe = true,
            isNetworkingUserFlow = null,
            saveToLinkWithStripeSucceeded = true,
            connectedAccountName = "Connected Account Name",
            businessName = "Business Name",
            count = 2
        ) as PluralId
        assertEquals(R.plurals.stripe_success_pane_link_with_connected_account_name, result.value)
        assertEquals(listOf("Connected Account Name", "Business Name"), result.args)
        assertEquals(2, result.count)
    }

    @Test
    fun `getSuccessMessage - link with stripe and business name`() {
        val result = buildViewModel(SuccessState()).getSuccessMessages(
            isLinkWithStripe = true,
            isNetworkingUserFlow = null,
            saveToLinkWithStripeSucceeded = true,
            connectedAccountName = null,
            businessName = "Business Name",
            count = 3
        ) as PluralId
        assertEquals(R.plurals.stripe_success_pane_link_with_business_name, result.value)
        assertEquals(listOf("Business Name"), result.args)
        assertEquals(3, result.count)
    }

    @Test
    fun `getSuccessMessage - link with stripe and no business name`() {
        val result = buildViewModel(SuccessState()).getSuccessMessages(
            isLinkWithStripe = true,
            isNetworkingUserFlow = null,
            saveToLinkWithStripeSucceeded = true,
            connectedAccountName = null,
            businessName = null,
            count = 1
        ) as PluralId
        assertEquals(R.plurals.stripe_success_pane_link_with_no_business_name, result.value)
        assertEquals(listOf<Any>(), result.args)
        assertEquals(1, result.count)
    }

    @Test
    fun `getSuccessMessage - networking user flow and save to link with stripe succeeded and connected account name and business name`() {
        val result = buildViewModel(SuccessState()).getSuccessMessages(
            isLinkWithStripe = false,
            isNetworkingUserFlow = true,
            saveToLinkWithStripeSucceeded = true,
            connectedAccountName = "Connected Account Name",
            businessName = "Business Name",
            count = 4
        ) as PluralId
        assertEquals(R.plurals.stripe_success_pane_link_with_connected_account_name, result.value)
        assertEquals(listOf("Connected Account Name", "Business Name"), result.args)
        assertEquals(4, result.count)
    }

    @Test
    fun `getSuccessMessage - networking user flow and save to link with stripe succeeded and business name`() {
        val result = buildViewModel(SuccessState()).getSuccessMessages(
            isLinkWithStripe = false,
            isNetworkingUserFlow = true,
            saveToLinkWithStripeSucceeded = true,
            connectedAccountName = null,
            businessName = "Business Name",
            count = 5
        ) as PluralId
        assertEquals(R.plurals.stripe_success_pane_link_with_business_name, result.value)
        assertEquals(listOf("Business Name"), result.args)
        assertEquals(5, result.count)
    }

    @Test
    fun `getSuccessMessage - no link with stripe and connected account name and business name`() {
        val result = buildViewModel(SuccessState()).getSuccessMessages(
            isLinkWithStripe = false,
            isNetworkingUserFlow = null,
            saveToLinkWithStripeSucceeded = null,
            connectedAccountName = "Connected Account Name",
            businessName = "Business Name",
            count = 6
        ) as PluralId
        assertEquals(R.plurals.stripe_success_pane_has_connected_account_name, result.value)
        assertEquals(listOf("Connected Account Name", "Business Name"), result.args)
        assertEquals(6, result.count)
    }

    @Test
    fun `getSuccessMessage - no link with stripe and business name`() {
        val result = buildViewModel(SuccessState()).getSuccessMessages(
            isLinkWithStripe = false,
            isNetworkingUserFlow = null,
            saveToLinkWithStripeSucceeded = null,
            connectedAccountName = null,
            businessName = "Business Name",
            count = 7
        ) as PluralId
        assertEquals(R.plurals.stripe_success_pane_has_business_name, result.value)
        assertEquals(listOf("Business Name"), result.args)
        assertEquals(7, result.count)
    }

    @Test
    fun `getSuccessMessage - no link with stripe and no business name`() {
        val result = buildViewModel(SuccessState()).getSuccessMessages(
            isLinkWithStripe = false,
            isNetworkingUserFlow = null,
            saveToLinkWithStripeSucceeded = null,
            connectedAccountName = null,
            businessName = null,
            count = 8
        ) as PluralId
        assertEquals(R.plurals.stripe_success_pane_no_business_name, result.value)
        assertEquals(listOf<Any>(), result.args)
        assertEquals(8, result.count)
    }

    @Test
    fun `getFailedToLinkMessage - saveToLinkWithStripeSucceeded is true, should return null`() {
        val message = buildViewModel(SuccessState()).getFailedToLinkMessage(
            businessName = "Business",
            saveToLinkWithStripeSucceeded = true,
            count = 1
        )
        assertThat(message).isNull()
    }

    @Test
    fun `getFailedToLinkMessage - saveToLinkWithStripeSucceeded is false and businessName is not null`() {
        val message = buildViewModel(SuccessState()).getFailedToLinkMessage(
            businessName = "Business",
            saveToLinkWithStripeSucceeded = false,
            count = 1
        )
        require(message is PluralId)
        assertEquals(R.plurals.stripe_success_networking_save_to_link_failed, message.value)
        assertEquals(1, message.count)
        assertEquals(listOf("Business"), message.args)
    }

    @Test
    fun `getFailedToLinkMessage - saveToLinkWithStripeSucceeded is false and businessName is null`() {
        val message = buildViewModel(SuccessState()).getFailedToLinkMessage(
            businessName = null,
            saveToLinkWithStripeSucceeded = false,
            count = 2
        )
        require(message is PluralId)
        assertEquals(
            R.plurals.stripe_success_pane_networking_save_to_link_failed_no_business,
            message.value
        )
        assertEquals(2, message.count)
        assertEquals(emptyList(), message.args)
    }
}
