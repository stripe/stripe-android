package com.stripe.android.payments.financialconnections

import com.stripe.android.core.utils.FeatureFlags.financialConnectionsFullSdkUnavailable
import com.stripe.android.core.utils.FeatureFlags.financialConnectionsLiteEnabled
import com.stripe.android.model.ElementsSession
import com.stripe.android.model.ElementsSession.Flag.ELEMENTS_DISABLE_FC_LITE
import com.stripe.android.testing.FeatureFlagTestRule
import org.junit.Rule
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import kotlin.test.Test
import kotlin.test.assertEquals

class GetFinancialConnectionsAvailabilityTest {

    @get:Rule
    val financialConnectionsFullSdkUnavailableFeatureFlagTestRule = FeatureFlagTestRule(
        featureFlag = financialConnectionsFullSdkUnavailable,
        isEnabled = false
    )

    @get:Rule
    val financialConnectionsLiteEnabledFeatureFlagTestRule = FeatureFlagTestRule(
        featureFlag = financialConnectionsLiteEnabled,
        isEnabled = false
    )

    @Test
    fun `when full SDK available and not unavailable should return full`() {
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
        financialConnectionsLiteEnabledFeatureFlagTestRule.setEnabled(true)
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

    private fun isFinancialConnectionsFullSdkAvailable(available: Boolean): IsFinancialConnectionsSdkAvailable =
        object : IsFinancialConnectionsSdkAvailable {
            override fun invoke(): Boolean = available
        }
}
