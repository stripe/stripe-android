package com.stripe.android.link.domain

import com.stripe.android.core.utils.FeatureFlags
import com.stripe.android.link.LinkConfiguration
import com.stripe.android.link.TestFactory
import com.stripe.android.link.gate.FakeLinkGate
import com.stripe.android.link.gate.LinkGate
import com.stripe.android.testing.FakeLogger
import com.stripe.android.testing.FeatureFlagTestRule
import junit.framework.TestCase.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertFalse

class LinkProminenceFeatureProviderTest {

    private val fakeLinkGate = FakeLinkGate()
    private lateinit var linkProminenceFeatureProvider: LinkProminenceFeatureProvider

    @get:Rule
    val prominenceFeatureFlagRule = FeatureFlagTestRule(
        featureFlag = FeatureFlags.linkProminenceInFlowController,
        isEnabled = false
    )

    @Before
    fun setUp() {
        val linkGateFactory = object : LinkGate.Factory {
            override fun create(configuration: LinkConfiguration): FakeLinkGate {
                return fakeLinkGate
            }
        }
        linkProminenceFeatureProvider = DefaultLinkProminenceFeatureProvider(linkGateFactory, FakeLogger())
    }

    @Test
    fun `shouldShowEarlyVerificationInFlowController returns false when feature flag disabled`() {
        // Given
        val state = linkConfiguration(
            suppress2faModal = false
        )
        fakeLinkGate.setUseNativeLink(true)

        // When
        val result = linkProminenceFeatureProvider.shouldShowEarlyVerificationInFlowController(state)

        // Then
        assertFalse(result)
    }

    @Test
    fun `shouldShowEarlyVerificationInFlowController returns false when suppress2faModal is true`() {
        // Given
        prominenceFeatureFlagRule.setEnabled(true)
        val state = linkConfiguration(
            suppress2faModal = true
        )
        fakeLinkGate.setUseNativeLink(true)

        // When
        val result = linkProminenceFeatureProvider.shouldShowEarlyVerificationInFlowController(state)

        // Then
        assertFalse(result)
    }

    @Test
    fun `shouldShowEarlyVerificationInFlowController returns false when useNativeLink is false`() {
        // Given
        prominenceFeatureFlagRule.setEnabled(true)
        val state = linkConfiguration(
            suppress2faModal = false
        )
        fakeLinkGate.setUseNativeLink(false)

        // When
        val result = linkProminenceFeatureProvider.shouldShowEarlyVerificationInFlowController(state)

        // Then
        assertFalse(result)
    }

    @Test
    fun `shouldShowEarlyVerificationInFlowController returns true when all conditions are met`() {
        // Given
        prominenceFeatureFlagRule.setEnabled(true)
        val state = linkConfiguration(
            suppress2faModal = false
        )
        fakeLinkGate.setUseNativeLink(true)

        // When
        val result = linkProminenceFeatureProvider.shouldShowEarlyVerificationInFlowController(state)

        // Then
        assertTrue(result)
    }

    private fun linkConfiguration(
        suppress2faModal: Boolean
    ) = TestFactory.LINK_CONFIGURATION.copy(
        suppress2faModal = suppress2faModal
    )
}
