package com.stripe.android.financialconnections.navigation

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.financialconnections.navigation.Destination.Companion.KEY_NEXT_PANE_ON_DISABLE_NETWORKING
import com.stripe.android.financialconnections.navigation.Destination.Companion.KEY_REFERRER
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DestinationTest {

    @Test
    fun `fullRoute - full route with referrer and extra param`() {
        val destination = Destination.NetworkingLinkLoginWarmup
        val expectedRoute = buildString {
            append(Pane.NETWORKING_LINK_LOGIN_WARMUP.value)
            append("?")
            append("$KEY_REFERRER=%7B$KEY_REFERRER%7D")
            append("&")
            append("$KEY_NEXT_PANE_ON_DISABLE_NETWORKING=%7B$KEY_NEXT_PANE_ON_DISABLE_NETWORKING%7D")
        }
        assertEquals(expectedRoute, destination.fullRoute)
    }

    @Test
    fun `invoke - route with referrer`() {
        val destination = Destination.Consent
        val referrer = Pane.INSTITUTION_PICKER
        val expectedRoute = Pane.CONSENT.value + "?referrer=${referrer.value}"
        assertEquals(expectedRoute, destination(referrer))
    }

    @Test
    fun `invoke - route with referrer and extra param`() {
        val destination = Destination.NetworkingLinkLoginWarmup
        val referrer = Pane.CONSENT
        val expectedRoute = buildString {
            append(Pane.NETWORKING_LINK_LOGIN_WARMUP.value)
            append("?")
            append("$KEY_NEXT_PANE_ON_DISABLE_NETWORKING=next_pane")
            append("&")
            append("referrer=${referrer.value}")
        }
        assertEquals(
            expectedRoute,
            destination(
                referrer = referrer,
                extraArgs = mapOf(KEY_NEXT_PANE_ON_DISABLE_NETWORKING to "next_pane")
            )
        )
    }
}
