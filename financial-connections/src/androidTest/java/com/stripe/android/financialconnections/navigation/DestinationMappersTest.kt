package com.stripe.android.financialconnections.navigation

import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Test

class DestinationMappersTest {

    // Panes that don't have a matching screen on the Android SDK side
    private val nonImplementedPanes = listOf(
        Pane.UNEXPECTED_ERROR,
        Pane.AUTH_OPTIONS,
        Pane.LINK_CONSENT,
        Pane.LINK_LOGIN,
    )

    @Test
    fun testPaneToDestination() {
        for (pane in Pane.values()) {
            if (!nonImplementedPanes.contains(pane)) {
                assertNotNull("No matching destination for pane: $pane", pane.destination)
            } else {
                assertThrows(IllegalArgumentException::class.java) { pane.destination }
            }
        }
    }
}
