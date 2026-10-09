package com.stripe.android.paymentsheet.example.playground.checkout.settings

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.utils.FeatureFlag
import com.stripe.android.core.utils.FeatureFlags
import com.stripe.android.testing.FeatureFlagTestRule
import org.junit.Rule
import org.junit.Test

class CheckoutFeatureFlagDefinitionsTest {
    @get:Rule
    val webAuthFeatureFlagRule = FeatureFlagTestRule(
        featureFlag = FeatureFlags.forceLinkWebAuth,
        isEnabled = false,
    )

    @Test
    fun `all feature flags have settings`() {
        val flagKeys = FeatureFlags::class.java.declaredFields
            .filter { it.type == FeatureFlag::class.java }
            .map { "feature_flags.${it.name}" }

        val settingKeys = CheckoutFeatureFlagDefinitions.configuration.values()
            .map { it.key }
            .filter { it.startsWith("feature_flags.") }

        assertThat(settingKeys).containsExactlyElementsIn(flagKeys)
    }

    @Test
    fun `flags default to SDK controlled behavior`() {
        webAuthFeatureFlagRule.setEnabled(true)

        CheckoutPlaygroundSettings.createInMemory().snapshot().applyFeatureFlags()

        assertThat(FeatureFlags.forceLinkWebAuth.value).isEqualTo(FeatureFlag.Flag.NotSet)
    }

    @Test
    fun `enabled flag survives export and is applied to checkout`() {
        val settings = CheckoutPlaygroundSettings.createInMemory()
        val definition = webAuthDefinition()
        settings.updateSerialized(definition, "true")

        val restored = CheckoutPlaygroundSettings.createInMemory(settings.asJsonString())
        restored.snapshot().applyFeatureFlags()

        assertThat(restored.serializedValue(definition)).isEqualTo("true")
        assertThat(FeatureFlags.forceLinkWebAuth.value).isEqualTo(FeatureFlag.Flag.Enabled)
    }

    @Test
    fun `disabled flag is applied to checkout`() {
        val settings = CheckoutPlaygroundSettings.createInMemory()
        settings.updateSerialized(webAuthDefinition(), "false")

        settings.snapshot().applyFeatureFlags()

        assertThat(FeatureFlags.forceLinkWebAuth.value).isEqualTo(FeatureFlag.Flag.Disabled)
    }

    @Test
    fun `reset clears a previously enabled flag override`() {
        val settings = CheckoutPlaygroundSettings.createInMemory()
        settings.updateSerialized(webAuthDefinition(), "true")
        settings.snapshot().applyFeatureFlags()
        assertThat(FeatureFlags.forceLinkWebAuth.value).isEqualTo(FeatureFlag.Flag.Enabled)

        settings.reset()
        settings.snapshot().applyFeatureFlags()

        assertThat(FeatureFlags.forceLinkWebAuth.value).isEqualTo(FeatureFlag.Flag.NotSet)
    }

    private fun webAuthDefinition(): CheckoutPlaygroundSettingDefinition.Value<*> {
        return CheckoutFeatureFlagDefinitions.configuration.values().single {
            it.key == "feature_flags.forceLinkWebAuth"
        }
    }
}
