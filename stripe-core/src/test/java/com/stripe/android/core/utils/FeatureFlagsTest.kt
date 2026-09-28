package com.stripe.android.core.utils

import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Test

class FeatureFlagsTest {
    @After
    fun tearDown() {
        FeatureFlags.reset()
    }

    @Test
    fun `reset restores all feature flags to their defaults`() {
        FeatureFlags.nativeLinkEnabled.setEnabled(true)
        FeatureFlags.instantDebitsIncentives.setEnabled(false)
        FeatureFlags.forceTapToAddWithTerminal.setEnabled(true)

        assertThat(FeatureFlags.nativeLinkEnabled.value).isEqualTo(FeatureFlag.Flag.Enabled)
        assertThat(FeatureFlags.instantDebitsIncentives.value).isEqualTo(FeatureFlag.Flag.Disabled)
        assertThat(FeatureFlags.forceTapToAddWithTerminal.value).isEqualTo(FeatureFlag.Flag.Enabled)

        FeatureFlags.reset()

        assertThat(FeatureFlags.nativeLinkEnabled.value).isEqualTo(FeatureFlag.Flag.NotSet)
        assertThat(FeatureFlags.instantDebitsIncentives.value).isEqualTo(FeatureFlag.Flag.NotSet)
        assertThat(FeatureFlags.forceTapToAddWithTerminal.value).isEqualTo(FeatureFlag.Flag.NotSet)
    }
}
