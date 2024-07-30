package com.stripe.android.financialconnections.navigation

import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import org.junit.Assert.assertThrows
import org.junit.Test

class DestinationMappersTest {

    // Panes that don't have a matching screen on the Android SDK side
    private val nonImplementedPanes = listOf(
        Pane.AUTH_OPTIONS,
        Pane.LINK_CONSENT,
    )

    @Test
    fun testPaneToDestination() {
        for (pane in Pane.entries) {
            if (!nonImplementedPanes.contains(pane)) {
                // This will throw if no destination exists for this pane
                pane.destination
            } else {
                assertThrows(IllegalArgumentException::class.java) { pane.destination }
            }
        }
    }
}
