package com.stripe.android.payments.financialconnections

import com.stripe.android.core.utils.FeatureFlags.forceFinancialConnectionsLiteSdk
import com.stripe.android.financialconnections.FinancialConnectionsMode
import com.stripe.android.model.ElementsSession
import com.stripe.android.model.ElementsSessionFlags.FINANCIAL_CONNECTIONS_LITE_KILLSWITCH
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import kotlin.test.Test
import kotlin.test.assertEquals

class GetFinancialConnectionsModeTest {

    @Test
    fun `when client flag is enabled force lite`() {
        forceFinancialConnectionsLiteSdk.setEnabled(true)
        val elementsSession = createSession(emptyMap())
        assertEquals(
            FinancialConnectionsMode.Lite,
            GetFinancialConnectionsMode(
                elementsSession = elementsSession,
                isFinancialConnectionsFullSdkAvailable = isFinancialConnectionsFullSdkAvailable(true)
            )
        )
    }

    @Test
    fun `when full SDK available should return full`() {
        forceFinancialConnectionsLiteSdk.setEnabled(false)
        val elementsSession = createSession(emptyMap())
        assertEquals(
            FinancialConnectionsMode.Full,
            GetFinancialConnectionsMode(
                elementsSession = elementsSession,
                isFinancialConnectionsFullSdkAvailable = isFinancialConnectionsFullSdkAvailable(true)
            )
        )
    }

    @Test
    fun `when lite killswitch is enabled and full not available should return None`() {
        forceFinancialConnectionsLiteSdk.setEnabled(false)
        val elementsSession = createSession(
            mapOf(FINANCIAL_CONNECTIONS_LITE_KILLSWITCH.flagValue to true)
        )
        assertEquals(
            FinancialConnectionsMode.None,
            GetFinancialConnectionsMode(
                elementsSession = elementsSession,
                isFinancialConnectionsFullSdkAvailable = isFinancialConnectionsFullSdkAvailable(false)
            )
        )
    }

    @Test
    fun `when full not available and killswitch not enabled, should return Lite`() {
        forceFinancialConnectionsLiteSdk.setEnabled(false)
        val elementsSession = createSession(flags = emptyMap())
        assertEquals(
            FinancialConnectionsMode.Lite,
            GetFinancialConnectionsMode(
                elementsSession = elementsSession,
                isFinancialConnectionsFullSdkAvailable = isFinancialConnectionsFullSdkAvailable(false)
            )
        )
    }

    fun createSession(flags: Map<String, Boolean>): ElementsSession {
        return mock<ElementsSession> {
            on { this.flags } doReturn flags
        }
    }

    private fun isFinancialConnectionsFullSdkAvailable(available: Boolean): IsFinancialConnectionsFullSdkAvailable =
        object : IsFinancialConnectionsFullSdkAvailable {
            override fun invoke(): Boolean = available
        }
}
