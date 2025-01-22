package com.stripe.android.link.gate

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.utils.FeatureFlags
import com.stripe.android.link.TestFactory
import com.stripe.android.testing.FeatureFlagTestRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class DefaultLinkGateTest {

    @get:Rule
    val nativeLinkFeatureFlagTestRule = FeatureFlagTestRule(
        featureFlag = FeatureFlags.nativeLinkEnabled,
        isEnabled = false
    )

    @get:Rule
    val attestationFeatureFlagTestRule = FeatureFlagTestRule(
        featureFlag = FeatureFlags.nativeLinkAttestationEnabled,
        isEnabled = false
    )

    @Test
    fun `useNativeLink is true when nativeLinkFeatureFlag is enabled`() {
        nativeLinkFeatureFlagTestRule.setEnabled(true)
        val gate = gate()

        assertThat(gate.useNativeLink).isTrue()
    }

    @Test
    fun `useNativeLink is true when nativeLinkFeatureFlag is enabled regardless of other settings`() {
        nativeLinkFeatureFlagTestRule.setEnabled(true)
        attestationFeatureFlagTestRule.setEnabled(false)

        val gate = gate(useAttestationEndpoints = false)

        assertThat(gate.useNativeLink).isTrue()
    }

    @Test
    fun `useNativeLink reflects useAttestationEndpoints when nativeLinkFeatureFlag is disabled`() {
        nativeLinkFeatureFlagTestRule.setEnabled(false)
        attestationFeatureFlagTestRule.setEnabled(true)

        val gate = gate()

        assertThat(gate.useNativeLink).isEqualTo(gate.useAttestationEndpoints)
    }

    @Test
    fun `useAttestationEndpoints is true when attestationFeatureFlag is enabled`() {
        attestationFeatureFlagTestRule.setEnabled(true)

        val gate = gate()

        assertThat(gate.useAttestationEndpoints).isTrue()
    }

    @Test
    fun `useAttestationEndpoints is true when attestationFeatureFlag is enabled regardless of configuration`() {
        attestationFeatureFlagTestRule.setEnabled(true)

        val gate = gate(useAttestationEndpoints = false)

        assertThat(gate.useAttestationEndpoints).isTrue()
    }

    @Test
    fun `useAttestationEndpoints reflects configuration when attestationFeatureFlag is disabled`() {
        attestationFeatureFlagTestRule.setEnabled(false)

        val gateTrue = gate(useAttestationEndpoints = true)
        val gateFalse = gate(useAttestationEndpoints = false)

        assertThat(gateTrue.useAttestationEndpoints).isTrue()
        assertThat(gateFalse.useAttestationEndpoints).isFalse()
    }

    private fun gate(useAttestationEndpoints: Boolean = true): DefaultLinkGate {
        return DefaultLinkGate(
            configuration = TestFactory.LINK_CONFIGURATION.copy(
                useAttestationEndpointsForLink = useAttestationEndpoints
            )
        )
    }
}
