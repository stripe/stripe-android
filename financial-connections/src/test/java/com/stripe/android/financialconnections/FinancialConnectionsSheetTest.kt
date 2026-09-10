package com.stripe.android.financialconnections

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
                ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY
            )
        )
    }

    @Test
    fun `present() without preCollectedConsent should launch with a null preCollectedConsent`() {
        financialConnectionsSheet.present(configuration, preCollectedConsent = null)
        verify(financialConnectionsSheetLauncher).present(
            configuration = FinancialConnectionsSheetConfiguration(
                financialConnectionsSessionClientSecret = ApiKeyFixtures.DEFAULT_FINANCIAL_CONNECTIONS_SESSION_SECRET,
                publishableKey = ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY,
                preCollectedConsent = null,
            ),
            elementsSessionContext = null,
        )
    }

    @Test
    fun `present() with preCollectedConsent should launch with the given preCollectedConsent`() {
        val preCollectedConsent = FinancialConnectionsPreCollectedConsent(consent = "fccons_123")

        financialConnectionsSheet.present(configuration, preCollectedConsent = preCollectedConsent)

        verify(financialConnectionsSheetLauncher).present(
            configuration = FinancialConnectionsSheetConfiguration(
                financialConnectionsSessionClientSecret = ApiKeyFixtures.DEFAULT_FINANCIAL_CONNECTIONS_SESSION_SECRET,
                publishableKey = ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY,
                preCollectedConsent = preCollectedConsent,
            ),
            elementsSessionContext = null,
        )
    }
}
