package com.stripe.android.financialconnections

import com.stripe.android.core.ApiConfiguration
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetLauncher
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

class FinancialConnectionsSheetTest {
    private val financialConnectionsSheetLauncher = mock<FinancialConnectionsSheetLauncher>()
    private val configuration = FinancialConnectionsSheet.Configuration(
        ApiKeyFixtures.DEFAULT_FINANCIAL_CONNECTIONS_SESSION_SECRET,
        ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY
    )
    private val financialConnectionsSheet =
        FinancialConnectionsSheet(financialConnectionsSheetLauncher)

    @Test
    fun `present() should launch the connection sheet with the given configuration`() {
        financialConnectionsSheet.present(configuration)
        verify(financialConnectionsSheetLauncher).present(
            FinancialConnectionsSheetConfiguration(
                ApiKeyFixtures.DEFAULT_FINANCIAL_CONNECTIONS_SESSION_SECRET,
                ApiConfiguration.State(ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY, null),
                preCollectedConsent = null,
            )
        )
    }

    @Test
    fun `present() preserves the connected account in API configuration`() {
        financialConnectionsSheet.present(configuration.copy(stripeAccountId = "acct_test"))

        verify(financialConnectionsSheetLauncher).present(
            configuration = FinancialConnectionsSheetConfiguration(
                financialConnectionsSessionClientSecret = ApiKeyFixtures.DEFAULT_FINANCIAL_CONNECTIONS_SESSION_SECRET,
                apiConfiguration = ApiConfiguration.State(ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY, "acct_test"),
                preCollectedConsent = null,
            ),
            elementsSessionContext = null,
        )
    }

    @Test
    fun `present() with preCollectedConsent should launch with the given preCollectedConsent`() {
        val preCollectedConsent = FinancialConnectionsPreCollectedConsent(
            consent = "fccons_123",
            collectedAt = 1_725_000_000L,
        )

        financialConnectionsSheet.present(configuration, preCollectedConsent = preCollectedConsent)

        verify(financialConnectionsSheetLauncher).present(
            configuration = FinancialConnectionsSheetConfiguration(
                financialConnectionsSessionClientSecret = ApiKeyFixtures.DEFAULT_FINANCIAL_CONNECTIONS_SESSION_SECRET,
                apiConfiguration = ApiConfiguration.State(ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY, null),
                preCollectedConsent = preCollectedConsent,
            ),
            elementsSessionContext = null,
        )
    }
}
