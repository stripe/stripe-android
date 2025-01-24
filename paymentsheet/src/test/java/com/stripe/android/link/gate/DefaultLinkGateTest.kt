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
    val nativeLinkFeatureFlagTestRule = FeatureFlagTestRule(
        featureFlag = FeatureFlags.nativeLinkEnabled,
        isEnabled = false
    )

    @get:Rule
    val attestationFeatureFlagTestRule = FeatureFlagTestRule(
        featureFlag = FeatureFlags.nativeLinkAttestationEnabled,
        isEnabled = false
    )

    // useNativeLink tests for test mode
    @Test
    fun `useNativeLink - test mode - returns true when feature flag enabled`() {
        nativeLinkFeatureFlagTestRule.setEnabled(true)
        val gate = gate(isLiveMode = false)

        assertThat(gate.useNativeLink).isTrue()
    }

    @Test
    fun `useNativeLink - test mode - returns false when feature flag disabled`() {
        nativeLinkFeatureFlagTestRule.setEnabled(false)
        val gate = gate(isLiveMode = false)

        assertThat(gate.useNativeLink).isFalse()
    }

    // useNativeLink tests for live mode
    @Test
    fun `useNativeLink - live mode - returns true when attestation enabled`() {
        val gate = gate(isLiveMode = true, useAttestationEndpoints = true)

        assertThat(gate.useNativeLink).isTrue()
    }

    @Test
    fun `useNativeLink - live mode - returns false when attestation disabled`() {
        val gate = gate(isLiveMode = true, useAttestationEndpoints = false)

        assertThat(gate.useNativeLink).isFalse()
    }

    // useAttestationEndpoints tests for test mode
    @Test
    fun `useAttestationEndpoints - test mode - returns true when feature flag enabled`() {
        attestationFeatureFlagTestRule.setEnabled(true)
        val gate = gate(isLiveMode = false)

        assertThat(gate.useAttestationEndpoints).isTrue()
    }

    @Test
    fun `useAttestationEndpoints - test mode - returns false when feature flag disabled`() {
        attestationFeatureFlagTestRule.setEnabled(false)
        val gate = gate(isLiveMode = false)

        assertThat(gate.useAttestationEndpoints).isFalse()
    }

    // useAttestationEndpoints tests for live mode
    @Test
    fun `useAttestationEndpoints - live mode - returns true when configuration enabled`() {
        val gate = gate(isLiveMode = true, useAttestationEndpoints = true)

        assertThat(gate.useAttestationEndpoints).isTrue()
    }

    @Test
    fun `useAttestationEndpoints - live mode - returns false when configuration disabled`() {
        val gate = gate(isLiveMode = true, useAttestationEndpoints = false)

        assertThat(gate.useAttestationEndpoints).isFalse()
    }

    // Feature flag independence tests
    @Test
    fun `useNativeLink - test mode - not affected by attestation feature flag`() {
        nativeLinkFeatureFlagTestRule.setEnabled(true)
        attestationFeatureFlagTestRule.setEnabled(false)
        val gate = gate(isLiveMode = false)

        assertThat(gate.useNativeLink).isTrue()
    }

    @Test
    fun `useAttestationEndpoints - test mode - not affected by native link feature flag`() {
        attestationFeatureFlagTestRule.setEnabled(true)
        nativeLinkFeatureFlagTestRule.setEnabled(false)
        val gate = gate(isLiveMode = false)

        assertThat(gate.useAttestationEndpoints).isTrue()
    }

    private fun gate(
        isLiveMode: Boolean = true,
        useAttestationEndpoints: Boolean = true
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
                stripeIntent = newIntent
            )
        )
    }
}
