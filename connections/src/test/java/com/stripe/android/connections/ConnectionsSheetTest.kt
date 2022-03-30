package com.stripe.android.connections

import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

class ConnectionsSheetTest {
    private val connectionsSheetLauncher = mock<ConnectionsSheetLauncher>()
    private val configuration = ConnectionsSheet.Configuration(
        ApiKeyFixtures.DEFAULT_LINK_ACCOUNT_SESSION_SECRET,
        ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY
    )
    private val connectionsSheet = ConnectionsSheet(connectionsSheetLauncher)

    @Test
    fun `present() should launch the connection sheet with the given configuration`() {
        connectionsSheet.present(configuration)
        verify(connectionsSheetLauncher).present(configuration)
    }
}
