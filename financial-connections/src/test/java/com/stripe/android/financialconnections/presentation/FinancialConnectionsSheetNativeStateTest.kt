package com.stripe.android.financialconnections.presentation

import com.google.common.truth.Truth.assertThat
import com.stripe.android.financialconnections.ApiKeyFixtures
import com.stripe.android.financialconnections.FinancialConnectionsSheet
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetNativeActivityArgs
import com.stripe.android.financialconnections.model.VisualUpdate
import com.stripe.android.financialconnections.ui.components.TopAppBarConfiguration
import org.junit.Test

internal class FinancialConnectionsSheetNativeStateTest {

    private val configuration = FinancialConnectionsSheet.Configuration(
        ApiKeyFixtures.DEFAULT_FINANCIAL_CONNECTIONS_SESSION_SECRET,
        ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY
    )

    @Test
    fun `init - TopAppBarConfiguration includes reducedBranding when true`() {
        assertThat(
            FinancialConnectionsSheetNativeState(
                args = args(
                    VisualUpdate(
                        reducedBranding = true,
                        merchantLogo = null
                    )
                )
            ).topAppBarConfiguration,
        ).isEqualTo(
            TopAppBarConfiguration(reducedBranding = true)
        )
    }

    @Test
    fun `init - TopAppBarConfiguration includes reducedBranding when false`() {
        assertThat(
            FinancialConnectionsSheetNativeState(
                args = args(
                    VisualUpdate(
                        reducedBranding = false,
                        merchantLogo = null
                    )
                )
            ).topAppBarConfiguration,
        ).isEqualTo(
            TopAppBarConfiguration(reducedBranding = false)
        )
    }

    @Test
    fun `init - TopAppBarConfiguration includes reducedBranding when null`() {
        assertThat(
            FinancialConnectionsSheetNativeState(
                args = args(
                    visual = null
                )
            ).topAppBarConfiguration,
        ).isEqualTo(
            TopAppBarConfiguration(reducedBranding = false)
        )
    }

    fun args(visual: VisualUpdate?) = FinancialConnectionsSheetNativeActivityArgs(
        configuration = configuration,
        initialSyncResponse = ApiKeyFixtures.syncResponse().copy(
            visual = visual
        )
    )
}
