package com.stripe.android.payments.connections

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.stripe.android.financialconnections.FinancialConnectionsSheet
import com.stripe.android.payments.connections.reflection.IsConnectionsAvailable
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class FinancialConnectionsPaymentsProxyTest {
    companion object {
        private const val FINANCIAL_CONNECTIONS_SHEET_CANONICAL_NAME =
            "com.stripe.android.financialconnections.FinancialConnectionsSheet"
    }

    private val mockIsConnectionsAvailable: IsConnectionsAvailable = mock()
    private val mockFragment: Fragment = mock()
    private val mockActivity: AppCompatActivity = mock()

    private class FakeProxy : FinancialConnectionsPaymentsProxy {
        override fun present(linkAccountSessionClientSecret: String, publishableKey: String) {
            // noop
        }
    }

    @Test
    fun `financial connections SDK availability returns null when module is not loaded`() {
        whenever(mockIsConnectionsAvailable()).thenAnswer { false }

        assertTrue(
            FinancialConnectionsPaymentsProxy.create(
                fragment = mockFragment,
                onComplete = {},
                isConnectionsAvailable = mockIsConnectionsAvailable
            ) is UnsupportedFinancialConnectionsPaymentsProxy
        )
        assertTrue(
            FinancialConnectionsPaymentsProxy.create(
                activity = mockActivity,
                onComplete = {},
                isConnectionsAvailable = mockIsConnectionsAvailable
            ) is UnsupportedFinancialConnectionsPaymentsProxy
        )
    }

    @Test
    fun `financial connections SDK availability returns sdk when module is loaded`() {
        assertTrue(
            FinancialConnectionsPaymentsProxy.create(
                fragment = mockFragment,
                onComplete = {},
                provider = { FakeProxy() }
            ) is FakeProxy
        )
        assertTrue(
            FinancialConnectionsPaymentsProxy.create(
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
            FinancialConnectionsPaymentsProxy.create(
                fragment = mockFragment,
                onComplete = {},
                isConnectionsAvailable = mockIsConnectionsAvailable
            ).present("", "")
        }
        assertFailsWith<IllegalStateException> {
            FinancialConnectionsPaymentsProxy.create(
                activity = mockActivity,
                onComplete = {},
                isConnectionsAvailable = mockIsConnectionsAvailable
            ).present("", "")
        }
    }

    @Test
    fun `ensure FinancialConnectionsSheet exists`() {
        assertEquals(
            FINANCIAL_CONNECTIONS_SHEET_CANONICAL_NAME,
            FinancialConnectionsSheet::class.qualifiedName
        )
    }
}
