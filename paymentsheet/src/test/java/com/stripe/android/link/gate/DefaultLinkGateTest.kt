package com.stripe.android.link.gate

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.utils.FeatureFlags
import com.stripe.android.link.TestFactory
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.SetupIntent
import com.stripe.android.testing.FeatureFlagTestRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class DefaultLinkGateTest {

    @get:Rule
    val suppressNativeLinkFeatureFlagTestRule = FeatureFlagTestRule(
        featureFlag = FeatureFlags.suppressNativeLink,
        isEnabled = false
    )

    // useNativeLink tests
    @Test
    fun `useNativeLink mirrors useAttestationEndpoints`() {
        val gate = gate(useAttestationEndpoints = true)
        assertThat(gate.useNativeLink).isTrue()

        val gateWithoutAttestation = gate(useAttestationEndpoints = false)
        assertThat(gateWithoutAttestation.useNativeLink).isFalse()
    }

    // useAttestationEndpoints tests for live mode
    @Test
    fun `useAttestationEndpoints - live mode - returns configuration value directly`() {
        val gate = gate(isLiveMode = true, useAttestationEndpoints = true)
        assertThat(gate.useAttestationEndpoints).isTrue()

        val gateWithoutAttestation = gate(isLiveMode = true, useAttestationEndpoints = false)
        assertThat(gateWithoutAttestation.useAttestationEndpoints).isFalse()
    }

    // useAttestationEndpoints tests for test mode
    @Test
    fun `useAttestationEndpoints - test mode - returns false when suppressNativeLink enabled`() {
        suppressNativeLinkFeatureFlagTestRule.setEnabled(true)
        val gate = gate(isLiveMode = false, useAttestationEndpoints = true)

        assertThat(gate.useAttestationEndpoints).isFalse()
    }

    @Test
    fun `useAttestationEndpoints - test mode - returns configuration value when suppressNativeLink disabled`() {
        suppressNativeLinkFeatureFlagTestRule.setEnabled(false)
        val gate = gate(isLiveMode = false, useAttestationEndpoints = true)
        assertThat(gate.useAttestationEndpoints).isTrue()

        val gateWithoutAttestation = gate(isLiveMode = false, useAttestationEndpoints = false)
        assertThat(gateWithoutAttestation.useAttestationEndpoints).isFalse()
    }

    // suppress2faModal tests
    @Test
    fun `suppress2faModal - returns true when native link is disabled`() {
        val gate = gate(useAttestationEndpoints = false, suppress2faModal = false)
        assertThat(gate.suppress2faModal).isTrue()
    }

    @Test
    fun `suppress2faModal - returns true when explicitly configured`() {
        val gate = gate(useAttestationEndpoints = true, suppress2faModal = true)
        assertThat(gate.suppress2faModal).isTrue()
    }

    @Test
    fun `suppress2faModal - returns false when native link enabled and not explicitly suppressed`() {
        val gate = gate(useAttestationEndpoints = true, suppress2faModal = false)
        assertThat(gate.suppress2faModal).isFalse()
    }

    private fun gate(
        isLiveMode: Boolean = true,
        useAttestationEndpoints: Boolean = true,
        suppress2faModal: Boolean = false
    ): DefaultLinkGate {
        val newIntent = when (val intent = TestFactory.LINK_CONFIGURATION.stripeIntent) {
            is PaymentIntent -> {
                intent.copy(isLiveMode = isLiveMode)
            }
            is SetupIntent -> {
                intent.copy(isLiveMode = isLiveMode)
            }
            else -> intent
        }
        return DefaultLinkGate(
            configuration = TestFactory.LINK_CONFIGURATION.copy(
                useAttestationEndpointsForLink = useAttestationEndpoints,
                suppress2faModal = suppress2faModal,
                stripeIntent = newIntent
            )
        )
    }
}
