package com.stripe.android.financialconnections.presentation

import com.google.common.truth.Truth.assertThat
import com.stripe.android.financialconnections.ApiKeyFixtures
import com.stripe.android.financialconnections.FinancialConnectionsSheetConfiguration
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetFlowType
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetNativeActivityArgs
import com.stripe.android.financialconnections.model.VisualUpdate
import com.stripe.android.model.LinkBrand
import org.junit.Test

internal class FinancialConnectionsSheetNativeStateTest {

    private val configuration = FinancialConnectionsSheetConfiguration(
        ApiKeyFixtures.DEFAULT_FINANCIAL_CONNECTIONS_SESSION_SECRET,
        ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY
    )

    @Test
    fun `init - reducedBranding includes reducedBranding when true`() {
        assertThat(
            FinancialConnectionsSheetNativeState(
                args = args(
                    VisualUpdate(
                        reducedBranding = true,
                        reducedManualEntryProminenceInErrors = false,
                        merchantLogos = emptyList()
                    )
                ),
                savedState = null
            ).reducedBranding,
        ).isTrue()
    }

    @Test
    fun `init - reducedBranding includes reducedBranding when false`() {
        assertThat(
            FinancialConnectionsSheetNativeState(
                args = args(
                    VisualUpdate(
                        reducedBranding = false,
                        reducedManualEntryProminenceInErrors = false,
                        merchantLogos = emptyList()
                    )
                ),
                savedState = null
            ).reducedBranding,
        ).isFalse()
    }

    @Test
    fun `init - linkBrand prefers configuration override over manifest`() {
        assertThat(
            FinancialConnectionsSheetNativeState(
                args = args(
                    visual = ApiKeyFixtures.visual(),
                    configuration = configuration.copy(linkBrand = LinkBrand.Notlink),
                    manifestLinkBrand = LinkBrand.Link,
                ),
                savedState = null
            ).linkBrand,
        ).isEqualTo(LinkBrand.Notlink)
    }

    @Test
    fun `init - linkBrand falls back to manifest when configuration override is absent`() {
        assertThat(
            FinancialConnectionsSheetNativeState(
                args = args(
                    visual = ApiKeyFixtures.visual(),
                    manifestLinkBrand = LinkBrand.Notlink,
                ),
                savedState = null
            ).linkBrand,
        ).isEqualTo(LinkBrand.Notlink)
    }

    @Test
    fun `init - linkBrand falls back to Link when configuration override and manifest are absent`() {
        assertThat(
            FinancialConnectionsSheetNativeState(
                args = args(
                    visual = ApiKeyFixtures.visual(),
                    manifestLinkBrand = null,
                ),
                savedState = null
            ).linkBrand,
        ).isEqualTo(LinkBrand.Link)
    }

    private fun args(
        visual: VisualUpdate,
        configuration: FinancialConnectionsSheetConfiguration = this.configuration,
        manifestLinkBrand: LinkBrand? = null,
    ) = FinancialConnectionsSheetNativeActivityArgs(
        flowType = FinancialConnectionsSheetFlowType.ForInstantDebits,
        configuration = configuration,
        initialSyncResponse = ApiKeyFixtures.syncResponse().copy(
            visual = visual,
            manifest = ApiKeyFixtures.sessionManifest().copy(linkBrand = manifestLinkBrand),
        )
    )
}
