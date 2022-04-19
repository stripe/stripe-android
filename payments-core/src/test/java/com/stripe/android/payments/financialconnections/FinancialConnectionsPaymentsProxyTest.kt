package com.stripe.android.payments.financialconnections

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.stripe.android.financialconnections.FinancialConnectionsSheet
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

    private val mockIsFinancialConnectionsAvailable: IsFinancialConnectionsAvailable = mock()
    private val mockFragment: Fragment = mock()
    private val mockActivity: AppCompatActivity = mock()

    private class FakeProxy : FinancialConnectionsPaymentsProxy {
        override fun present(linkAccountSessionClientSecret: String, publishableKey: String) {
            // noop
        }
    }

    @Test
    fun `financial connections SDK availability returns null when module is not loaded`() {
        whenever(mockIsFinancialConnectionsAvailable()).thenAnswer { false }

        assertTrue(
            FinancialConnectionsPaymentsProxy.create(
                fragment = mockFragment,
                onComplete = {},
                isFinancialConnectionsAvailable = mockIsFinancialConnectionsAvailable
            ) is UnsupportedFinancialConnectionsPaymentsProxy
        )
        assertTrue(
            FinancialConnectionsPaymentsProxy.create(
                activity = mockActivity,
                onComplete = {},
                isFinancialConnectionsAvailable = mockIsFinancialConnectionsAvailable
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
        whenever(mockIsFinancialConnectionsAvailable()).thenAnswer { false }

        assertFailsWith<IllegalStateException> {
            FinancialConnectionsPaymentsProxy.create(
                fragment = mockFragment,
                onComplete = {},
                isFinancialConnectionsAvailable = mockIsFinancialConnectionsAvailable
            ).present("", "")
        }
        assertFailsWith<IllegalStateException> {
            FinancialConnectionsPaymentsProxy.create(
                activity = mockActivity,
                onComplete = {},
                isFinancialConnectionsAvailable = mockIsFinancialConnectionsAvailable
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
