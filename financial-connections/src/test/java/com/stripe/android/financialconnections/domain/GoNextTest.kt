package com.stripe.android.financialconnections.domain

import com.stripe.android.core.Logger
import com.stripe.android.financialconnections.ApiKeyFixtures
import com.stripe.android.financialconnections.ApiKeyFixtures.authorizationSession
import com.stripe.android.financialconnections.ApiKeyFixtures.sessionManifest
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.NextPane
import com.stripe.android.financialconnections.navigation.NavigationDirections
import com.stripe.android.financialconnections.navigation.NavigationManager
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

internal class GoNextTest {

    private val navigationManager = mock<NavigationManager>()
    private val getManifest = mock<GetManifest>()
    private val getAuthorizationSessionAccounts = mock<GetAuthorizationSessionAccounts>()

    val goNext = GoNext(
        navigationManager = navigationManager,
        logger = Logger.noop(),
        getManifest = getManifest,
        getAuthorizationSessionAccounts = getAuthorizationSessionAccounts
    )

    @Test
    fun `invoke - when current step is consent, navigates to manifest nextPane`() = runTest {
        // Given
        val expectedNextStep = NextPane.INSTITUTION_PICKER
        given(getManifest()).willReturn(
            sessionManifest().copy(
                nextPane = expectedNextStep,
                activeAuthSession = authorizationSession().copy(nextPane = NextPane.ACCOUNT_PICKER)
            )
        )
        given(getAuthorizationSessionAccounts(any())).willReturn(
            ApiKeyFixtures.partnerAccountList()
                .copy(nextPane = NextPane.CONSENT),
        )

        // When
        goNext(currentPane = NavigationDirections.consent)

        // Then
        verify(navigationManager).navigate(NavigationDirections.institutionPicker)
    }

    @Test
    fun `invoke - when current step is institution picker, navigates to authorizationSession nextPane`() =
        runTest {
            // Given
            given(getManifest()).willReturn(
                sessionManifest().copy(
                    nextPane = NextPane.CONSENT,
                    activeAuthSession = authorizationSession().copy(nextPane = NextPane.PARTNER_AUTH)
                )
            )
            given(getAuthorizationSessionAccounts(any())).willReturn(
                ApiKeyFixtures.partnerAccountList()
                    .copy(nextPane = NextPane.CONSENT),
            )
            // When
            goNext(currentPane = NavigationDirections.institutionPicker)

            // Then
            verify(navigationManager).navigate(NavigationDirections.partnerAuth)
        }

    @Test
    fun `invoke - when current step is partner auth, navigates to authorizationSession nextPane`() =
        runTest {
            // Given
            val expectedNextStep = NextPane.ACCOUNT_PICKER
            given(getManifest()).willReturn(
                sessionManifest().copy(
                    nextPane = NextPane.CONSENT,
                    activeAuthSession = authorizationSession().copy(nextPane = expectedNextStep)
                )
            )
            given(getAuthorizationSessionAccounts(any())).willReturn(
                ApiKeyFixtures.partnerAccountList()
                    .copy(nextPane = NextPane.CONSENT)
            )

            // When
            goNext(currentPane = NavigationDirections.partnerAuth)

            // Then
            verify(navigationManager).navigate(NavigationDirections.accountPicker)
        }
}
