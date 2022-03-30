package com.stripe.android.payments.connections

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.stripe.android.connections.ConnectionsSheet
import com.stripe.android.payments.connections.reflection.IsConnectionsAvailable
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ConnectionsPaymentsProxyTest {
    companion object {
        private const val CONNECTIONS_SHEET_CANONICAL_NAME =
            "com.stripe.android.connections.ConnectionsSheet"
    }

    private val mockIsConnectionsAvailable: IsConnectionsAvailable = mock()
    private val mockFragment: Fragment = mock()
    private val mockActivity: AppCompatActivity = mock()

    private class FakeProxy : ConnectionsPaymentsProxy {
        override fun present(linkAccountSessionClientSecret: String, publishableKey: String) {
            // noop
        }
    }

    @Test
    fun `connections SDK availability returns null when connections module is not loaded`() {
        whenever(mockIsConnectionsAvailable()).thenAnswer { false }

        assertTrue(
            ConnectionsPaymentsProxy.create(
                fragment = mockFragment,
                onComplete = {},
                isConnectionsAvailable = mockIsConnectionsAvailable
            ) is UnsupportedConnectionsPaymentsProxy
        )
        assertTrue(
            ConnectionsPaymentsProxy.create(
                activity = mockActivity,
                onComplete = {},
                isConnectionsAvailable = mockIsConnectionsAvailable
            ) is UnsupportedConnectionsPaymentsProxy
        )
    }

    @Test
    fun `connections SDK availability returns sdk when connections module is loaded`() {
        assertTrue(
            ConnectionsPaymentsProxy.create(
                fragment = mockFragment,
                onComplete = {},
                provider = { FakeProxy() }
            ) is FakeProxy
        )
        assertTrue(
            ConnectionsPaymentsProxy.create(
                activity = mockActivity,
                onComplete = {},
                provider = { FakeProxy() }
            ) is FakeProxy
        )
    }

    @Test
    fun `calling present on UnsupportedConnectionsPaymentsProxy throws an exception`() {
        whenever(mockIsConnectionsAvailable()).thenAnswer { false }

        assertFailsWith<IllegalStateException> {
            ConnectionsPaymentsProxy.create(
                fragment = mockFragment,
                onComplete = {},
                isConnectionsAvailable = mockIsConnectionsAvailable
            ).present("", "")
        }
        assertFailsWith<IllegalStateException> {
            ConnectionsPaymentsProxy.create(
                activity = mockActivity,
                onComplete = {},
                isConnectionsAvailable = mockIsConnectionsAvailable
            ).present("", "")
        }
    }

    @Test
    fun `ensure ConnectionsSheet exists`() {
        assertEquals(CONNECTIONS_SHEET_CANONICAL_NAME, ConnectionsSheet::class.qualifiedName)
    }
}
