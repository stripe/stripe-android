package com.stripe.android.payments.financialconnections

import androidx.appcompat.app.AppCompatActivity
import com.stripe.android.financialconnections.ElementsSessionContext
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
    private val mockActivity: AppCompatActivity = mock()

    private class FakeProxy : FinancialConnectionsPaymentsProxy {
        override fun present(
            financialConnectionsSessionClientSecret: String,
            publishableKey: String,
            stripeAccountId: String?,
            elementsSessionContext: ElementsSessionContext?,
        ) {
            // noop
        }
    }

    @Test
    fun `financial connections SDK availability returns null when module is not loaded`() {
        whenever(mockIsFinancialConnectionsAvailable()).thenAnswer { false }
        assertTrue(
            FinancialConnectionsPaymentsProxy.createForACH(
                activity = mockActivity,
                onComplete = {},
                isFinancialConnectionsAvailable = mockIsFinancialConnectionsAvailable
            ) is UnsupportedFinancialConnectionsPaymentsProxy
        )
    }

    @Test
    fun `financial connections SDK availability returns sdk when module is loaded`() {
        assertTrue(
            FinancialConnectionsPaymentsProxy.createForACH(
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
            FinancialConnectionsPaymentsProxy.createForACH(
                activity = mockActivity,
                onComplete = {},
                isFinancialConnectionsAvailable = mockIsFinancialConnectionsAvailable
            ).present(
                financialConnectionsSessionClientSecret = "",
                publishableKey = "",
                stripeAccountId = null,
                elementsSessionContext = null,
            )
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
