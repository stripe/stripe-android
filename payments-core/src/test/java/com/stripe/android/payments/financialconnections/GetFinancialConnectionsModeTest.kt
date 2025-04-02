package com.stripe.android.payments.financialconnections

import com.stripe.android.core.utils.FeatureFlags.financialConnectionsFullSdkUnavailable
import com.stripe.android.core.utils.FeatureFlags.financialConnectionsLiteKillswitch
import com.stripe.android.model.ElementsSession
import com.stripe.android.model.ElementsSession.Flag.ELEMENTS_DISABLE_FC_LITE
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import kotlin.test.Test
import kotlin.test.assertEquals

class GetFinancialConnectionsModeTest {

    @Test
    fun `when full SDK available and not unavailable should return full`() {
        financialConnectionsFullSdkUnavailable.setEnabled(false)
        val elementsSession = createSession(emptyMap())
        assertEquals(
            FinancialConnectionsAvailability.Full,
            GetFinancialConnectionsAvailability(
                elementsSession = elementsSession,
                isFullSdkAvailable = isFinancialConnectionsFullSdkAvailable(true)
            )
        )
    }

    @Test
    fun `when lite killswitch is enabled and full not available should return None`() {
        financialConnectionsLiteKillswitch.setEnabled(true)
        val elementsSession = createSession(
            mapOf(ELEMENTS_DISABLE_FC_LITE to true)
        )
        assertEquals(
            null,
            GetFinancialConnectionsAvailability(
                elementsSession = elementsSession,
                isFullSdkAvailable = isFinancialConnectionsFullSdkAvailable(false)
            )
        )
    }

    @Test
    fun `when full not available and killswitch not enabled, should return Lite`() {
        financialConnectionsLiteKillswitch.setEnabled(false)
        val elementsSession = createSession(flags = emptyMap())
        assertEquals(
            FinancialConnectionsAvailability.Lite,
            GetFinancialConnectionsAvailability(
                elementsSession = elementsSession,
                isFullSdkAvailable = isFinancialConnectionsFullSdkAvailable(false)
            )
        )
    }

    fun createSession(flags: Map<ElementsSession.Flag, Boolean>): ElementsSession {
        return mock<ElementsSession> {
            on { this.flags } doReturn flags
        }
    }

    private fun isFinancialConnectionsFullSdkAvailable(available: Boolean): IsFinancialConnectionsFullSdkAvailable =
        object : IsFinancialConnectionsFullSdkAvailable {
            override fun invoke(): Boolean = available
        }
}
