package com.stripe.android.financialconnections.presentation

import com.google.common.truth.Truth.assertThat
import com.stripe.android.financialconnections.ApiKeyFixtures
import com.stripe.android.financialconnections.FinancialConnectionsSheet
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetNativeActivityArgs
import com.stripe.android.financialconnections.model.VisualUpdate
import org.junit.Test

internal class FinancialConnectionsSheetNativeStateTest {

    private val configuration = FinancialConnectionsSheet.Configuration(
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
                        merchantLogo = null
                    )
                )
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
                        merchantLogo = null
                    )
                )
            ).reducedBranding,
        ).isFalse()
    }

    @Test
    fun `init - reducedBranding includes reducedBranding when null`() {
        assertThat(
            FinancialConnectionsSheetNativeState(
                args = args(
                    visual = null
                )
            ).reducedBranding,
        ).isFalse()
    }

    fun args(visual: VisualUpdate?) = FinancialConnectionsSheetNativeActivityArgs(
        configuration = configuration,
        initialSyncResponse = ApiKeyFixtures.syncResponse().copy(
            visual = visual
        )
    )
}
