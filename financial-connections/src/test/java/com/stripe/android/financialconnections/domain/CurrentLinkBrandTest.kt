package com.stripe.android.financialconnections.domain

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.stripe.android.financialconnections.ApiKeyFixtures
import com.stripe.android.financialconnections.FinancialConnectionsSheetConfiguration
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetFlowType
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest
import com.stripe.android.financialconnections.networking.FakeFinancialConnectionsManifestRepository
import com.stripe.android.financialconnections.presentation.FinancialConnectionsSheetNativeState
import com.stripe.android.financialconnections.presentation.WebAuthFlowState
import com.stripe.android.financialconnections.repository.RealConsumerSessionRepository
import com.stripe.android.financialconnections.ui.theme.Theme
import com.stripe.android.model.LinkBrand
import org.junit.Test

internal class CurrentLinkBrandTest {

    private val initialState = FinancialConnectionsSheetNativeState(
        flowType = FinancialConnectionsSheetFlowType.ForData,
        webAuthFlow = WebAuthFlowState.Uninitialized,
        firstInit = true,
        configuration = FinancialConnectionsSheetConfiguration(
            financialConnectionsSessionClientSecret = ApiKeyFixtures.DEFAULT_FINANCIAL_CONNECTIONS_SESSION_SECRET,
            publishableKey = ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY,
        ),
        reducedBranding = false,
        testMode = false,
        viewEffect = null,
        completed = false,
        initialPane = FinancialConnectionsSessionManifest.Pane.CONSENT,
        theme = Theme.LinkLight,
        linkBrand = LinkBrand.Link,
        isLinkWithStripe = true,
        manualEntryUsesMicrodeposits = false,
        elementsSessionContext = null,
    )

    private val manifestRepository = FakeFinancialConnectionsManifestRepository()
    private val consumerSessionRepository = RealConsumerSessionRepository(SavedStateHandle())

    @Test
    fun `falls back to initial state linkBrand`() {
        val currentLinkBrand = currentLinkBrand()

        assertThat(currentLinkBrand()).isEqualTo(LinkBrand.Link)
    }

    @Test
    fun `uses manifest linkBrand over initial state linkBrand`() {
        manifestRepository.setSyncResponse(syncResponseWithLinkBrand(LinkBrand.Onelink))
        val currentLinkBrand = currentLinkBrand()

        assertThat(currentLinkBrand()).isEqualTo(LinkBrand.Onelink)
    }

    @Test
    fun `uses consumer session linkBrand over manifest linkBrand`() {
        manifestRepository.setSyncResponse(syncResponseWithLinkBrand(LinkBrand.Link))
        consumerSessionRepository.storeNewConsumerSession(
            consumerSession = ApiKeyFixtures.verifiedConsumerSession().copy(linkBrand = LinkBrand.Onelink),
            publishableKey = "pk_123",
        )
        val currentLinkBrand = currentLinkBrand()

        assertThat(currentLinkBrand()).isEqualTo(LinkBrand.Onelink)
    }

    @Test
    fun `updates when repositories change`() {
        val currentLinkBrand = currentLinkBrand()

        assertThat(currentLinkBrand.stateFlow.value).isEqualTo(LinkBrand.Link)

        manifestRepository.setSyncResponse(syncResponseWithLinkBrand(LinkBrand.Onelink))
        assertThat(currentLinkBrand.stateFlow.value).isEqualTo(LinkBrand.Onelink)

        consumerSessionRepository.storeNewConsumerSession(
            consumerSession = ApiKeyFixtures.verifiedConsumerSession().copy(linkBrand = LinkBrand.Link),
            publishableKey = "pk_123",
        )
        assertThat(currentLinkBrand.stateFlow.value).isEqualTo(LinkBrand.Link)
    }

    private fun currentLinkBrand(): CurrentLinkBrand {
        return RealCurrentLinkBrand(
            initialState = initialState,
            financialConnectionsManifestRepository = manifestRepository,
            consumerSessionRepository = consumerSessionRepository,
        )
    }

    private fun syncResponseWithLinkBrand(linkBrand: LinkBrand) = ApiKeyFixtures.syncResponse(
        manifest = ApiKeyFixtures.sessionManifest().copy(rawLinkBrand = linkBrand),
    )
}
