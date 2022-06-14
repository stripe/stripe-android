package com.stripe.android.financialconnections.domain

import com.stripe.android.core.Logger
import com.stripe.android.financialconnections.ApiKeyFixtures.authorizationSession
import com.stripe.android.financialconnections.ApiKeyFixtures.sessionManifest
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.NextPane
import com.stripe.android.financialconnections.navigation.NavigationDirections
import com.stripe.android.financialconnections.navigation.NavigationManager
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

internal class GoNextTest {

    private val navigationManager = mock<NavigationManager>()

    val goNext = GoNext(
        navigationManager = navigationManager,
        logger = Logger.noop(),
    )

    @Test
    fun `invoke - when current step is consent, navigates to manifest nextPane`() {
        // Given
        val expectedNextStep = NextPane.INSTITUTION_PICKER

        // When
        goNext(
            currentPane = NavigationDirections.consent,
            manifest = sessionManifest().copy(nextPane = expectedNextStep),
            authorizationSession = authorizationSession().copy(nextPane = NextPane.ACCOUNT_PICKER)
        )

        // Then
        verify(navigationManager).navigate(NavigationDirections.institutionPicker)
    }

    @Test
    fun `invoke - when current step is institution picker, navigates to authorizationSession nextPane`() {
        // Given
        val expectedNextStep = NextPane.PARTNER_AUTH

        // When
        goNext(
            currentPane = NavigationDirections.institutionPicker,
            manifest = sessionManifest().copy(nextPane = NextPane.ACCOUNT_PICKER),
            authorizationSession = authorizationSession().copy(nextPane = expectedNextStep)
        )

        // Then
        verify(navigationManager).navigate(NavigationDirections.partnerAuth)
    }
}