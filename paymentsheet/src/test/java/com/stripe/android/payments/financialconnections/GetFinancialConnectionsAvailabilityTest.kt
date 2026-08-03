package com.stripe.android.payments.financialconnections

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.utils.FeatureFlags.financialConnectionsFullSdkUnavailable
import com.stripe.android.model.ElementsSession
import com.stripe.android.model.ElementsSession.ExperimentAssignment.CONNECTIONS_FC_LITE_VS_NATIVE
import com.stripe.android.model.ElementsSession.Flag.ELEMENTS_DISABLE_FC_LITE
import com.stripe.android.model.ElementsSession.Flag.ELEMENTS_PREFER_FC_LITE
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.paymentsheet.forms.generated.ContractMetadataV1
import com.stripe.android.paymentsheet.forms.generated.MobilePaymentElementFeaturesV1
import com.stripe.android.paymentsheet.forms.generated.MobilePaymentElementV1
import com.stripe.android.paymentsheet.forms.generated.PaymentMethodAvailabilityV1
import com.stripe.android.testing.FeatureFlagTestRule
import org.junit.Rule
import kotlin.test.Test

class GetFinancialConnectionsAvailabilityTest {

    @get:Rule
    val financialConnectionsFullSdkUnavailableFeatureFlagTestRule = FeatureFlagTestRule(
        featureFlag = financialConnectionsFullSdkUnavailable,
        isEnabled = false
    )

    @Test
    fun `when prefer lite flag is enabled should return Lite regardless of full SDK availability`() {
        val elementsSession = createSession(
            flags = mapOf(ELEMENTS_PREFER_FC_LITE to true)
        )
        assertThat(
            GetFinancialConnectionsAvailability(
                elementsSession = elementsSession,
                isFullSdkAvailable = isFinancialConnectionsFullSdkAvailable(true)
            )
        ).isEqualTo(FinancialConnectionsAvailability.Lite)
    }

    @Test
    fun `when killswitch is enabled should take priority over prefer lite flag`() {
        val elementsSession = createSession(
            flags = mapOf(
                ELEMENTS_PREFER_FC_LITE to true,
                ELEMENTS_DISABLE_FC_LITE to true
            )
        )
        assertThat(
            GetFinancialConnectionsAvailability(
                elementsSession = elementsSession,
                isFullSdkAvailable = isFinancialConnectionsFullSdkAvailable(true)
            )
        ).isEqualTo(FinancialConnectionsAvailability.Full)
    }

    @Test
    fun `when full SDK available and not unavailable should return full`() {
        val elementsSession = createSession(emptyMap())
        assertThat(
            GetFinancialConnectionsAvailability(
                elementsSession = elementsSession,
                isFullSdkAvailable = isFinancialConnectionsFullSdkAvailable(true)
            )
        ).isEqualTo(FinancialConnectionsAvailability.Full)
    }

    @Test
    fun `when lite killswitch is enabled and full not available should return None`() {
        val elementsSession = createSession(
            flags = mapOf(ELEMENTS_DISABLE_FC_LITE to true)
        )
        assertThat(
            GetFinancialConnectionsAvailability(
                elementsSession = elementsSession,
                isFullSdkAvailable = isFinancialConnectionsFullSdkAvailable(false)
            )
        ).isNull()
    }

    @Test
    fun `when full not available and killswitch not enabled, should return Lite`() {
        val elementsSession = createSession(flags = emptyMap())
        assertThat(
            GetFinancialConnectionsAvailability(
                elementsSession = elementsSession,
                isFullSdkAvailable = isFinancialConnectionsFullSdkAvailable(false)
            )
        ).isEqualTo(FinancialConnectionsAvailability.Lite)
    }

    @Test
    fun `when full client flag on and killswitch not enabled, should return Lite`() {
        financialConnectionsFullSdkUnavailableFeatureFlagTestRule.setEnabled(true)
        val elementsSession = createSession(flags = emptyMap())
        assertThat(
            GetFinancialConnectionsAvailability(
                elementsSession = elementsSession,
                isFullSdkAvailable = isFinancialConnectionsFullSdkAvailable(true)
            )
        ).isEqualTo(FinancialConnectionsAvailability.Lite)
    }

    @Test
    fun `when experiment assignment is treatment should return Lite regardless of full SDK availability`() {
        val elementsSession = createSession(
            flags = emptyMap(),
            experimentAssignments = mapOf(CONNECTIONS_FC_LITE_VS_NATIVE to "treatment")
        )
        assertThat(
            GetFinancialConnectionsAvailability(
                elementsSession = elementsSession,
                isFullSdkAvailable = isFinancialConnectionsFullSdkAvailable(true)
            )
        ).isEqualTo(FinancialConnectionsAvailability.Lite)
    }

    @Test
    fun `when experiment assignment is control should not prefer Lite`() {
        val elementsSession = createSession(
            flags = emptyMap(),
            experimentAssignments = mapOf(CONNECTIONS_FC_LITE_VS_NATIVE to "control")
        )
        assertThat(
            GetFinancialConnectionsAvailability(
                elementsSession = elementsSession,
                isFullSdkAvailable = isFinancialConnectionsFullSdkAvailable(true)
            )
        ).isEqualTo(FinancialConnectionsAvailability.Full)
    }

    @Test
    fun `when experiment is treatment but killswitch is enabled should not use Lite`() {
        val elementsSession = createSession(
            flags = mapOf(ELEMENTS_DISABLE_FC_LITE to true),
            experimentAssignments = mapOf(CONNECTIONS_FC_LITE_VS_NATIVE to "treatment")
        )
        assertThat(
            GetFinancialConnectionsAvailability(
                elementsSession = elementsSession,
                isFullSdkAvailable = isFinancialConnectionsFullSdkAvailable(true)
            )
        ).isEqualTo(FinancialConnectionsAvailability.Full)
    }

    @Test
    fun `server preferred decision chooses Lite without consulting legacy killswitch`() {
        val elementsSession = createSession(
            flags = mapOf(ELEMENTS_DISABLE_FC_LITE to true),
            financialConnectionsLite = "preferred",
        )

        assertThat(
            GetFinancialConnectionsAvailability(
                elementsSession = elementsSession,
                isFullSdkAvailable = isFinancialConnectionsFullSdkAvailable(true),
            )
        ).isEqualTo(FinancialConnectionsAvailability.Lite)
    }

    @Test
    fun `server disabled decision chooses Full without consulting legacy preference`() {
        val elementsSession = createSession(
            flags = mapOf(ELEMENTS_PREFER_FC_LITE to true),
            experimentAssignments = mapOf(CONNECTIONS_FC_LITE_VS_NATIVE to "treatment"),
            financialConnectionsLite = "disabled",
        )

        assertThat(
            GetFinancialConnectionsAvailability(
                elementsSession = elementsSession,
                isFullSdkAvailable = isFinancialConnectionsFullSdkAvailable(true),
            )
        ).isEqualTo(FinancialConnectionsAvailability.Full)
    }

    @Test
    fun `server automatic decision chooses Full without consulting legacy preference`() {
        val elementsSession = createSession(
            flags = mapOf(ELEMENTS_PREFER_FC_LITE to true),
            experimentAssignments = mapOf(CONNECTIONS_FC_LITE_VS_NATIVE to "treatment"),
            financialConnectionsLite = "automatic",
        )

        assertThat(
            GetFinancialConnectionsAvailability(
                elementsSession = elementsSession,
                isFullSdkAvailable = isFinancialConnectionsFullSdkAvailable(true),
            )
        ).isEqualTo(FinancialConnectionsAvailability.Full)
    }

    @Test
    fun `legacy preference remains authoritative without Mobile Session`() {
        val elementsSession = createSession(
            flags = mapOf(ELEMENTS_PREFER_FC_LITE to true),
            financialConnectionsLite = null,
        )

        assertThat(
            GetFinancialConnectionsAvailability(
                elementsSession = elementsSession,
                isFullSdkAvailable = isFinancialConnectionsFullSdkAvailable(true),
            )
        ).isEqualTo(FinancialConnectionsAvailability.Lite)
    }

    private fun createSession(
        flags: Map<ElementsSession.Flag, Boolean>,
        experimentAssignments: Map<ElementsSession.ExperimentAssignment, String> = emptyMap(),
        financialConnectionsLite: String? = null,
    ): ElementsSession {
        val experimentsData = ElementsSession.ExperimentsData(
            arbId = "test_arb_id",
            experimentAssignments = experimentAssignments,
        ).takeIf { experimentAssignments.isNotEmpty() }
        val mobilePaymentElement = financialConnectionsLite?.let {
            MobilePaymentElementV1(
                contract = ContractMetadataV1(
                    major = 1,
                    revision = "test_revision",
                ),
                paymentMethodAvailability = PaymentMethodAvailabilityV1(),
                features = MobilePaymentElementFeaturesV1(financialConnectionsLite = it),
            )
        }
        return ElementsSession(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
            cardBrandChoice = null,
            merchantCountry = null,
            isGooglePayEnabled = false,
            customer = null,
            linkSettings = null,
            orderedPaymentMethodTypesAndWallets = emptyList(),
            customPaymentMethods = emptyList(),
            externalPaymentMethodData = null,
            paymentMethodSpecs = null,
            elementsSessionId = "session_1234",
            flags = flags,
            experimentsData = experimentsData,
            passiveCaptcha = null,
            merchantLogoUrl = null,
            elementsSessionConfigId = null,
            accountId = null,
            merchantId = null,
            mobilePaymentElement = mobilePaymentElement,
        )
    }

    private fun isFinancialConnectionsFullSdkAvailable(available: Boolean): IsFinancialConnectionsSdkAvailable =
        object : IsFinancialConnectionsSdkAvailable {
            override fun invoke(): Boolean = available
        }
}
