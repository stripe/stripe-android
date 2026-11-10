package com.stripe.android.paymentsheet.example.playground.checkout.settings

import com.stripe.android.core.networking.RequestHeadersFactory
import com.stripe.android.core.utils.FeatureFlag
import com.stripe.android.core.utils.FeatureFlags

internal object CheckoutSdkBehaviorDefinitions {
    val nativeLinkEnabled = featureFlag("nativeLinkEnabled", FeatureFlags.nativeLinkEnabled)
    val nativeLinkAttestationEnabled = featureFlag(
        "nativeLinkAttestationEnabled",
        FeatureFlags.nativeLinkAttestationEnabled,
    )

    val sdkVersionOverride = value(
        key = "sdkVersionOverride",
        displayName = "SDK version override",
        defaultValue = "",
        encode = { it },
        decode = { Result.success(it) },
        applyFeatureFlags = { version ->
            RequestHeadersFactory.SDK_VERSION_NAME_OVERRIDE = version.trim().takeIf { it.isNotEmpty() }
        },
    )

    val configuration: CheckoutPlaygroundSettingDefinition.Configuration = configuration(
        key = "feature_flags",
        displayName = "SDK behavior",
        children = (listOf(sdkVersionOverride) + featureFlags()).toTypedArray(),
    )

    private fun featureFlags(): List<CheckoutPlaygroundSettingDefinition.Value<Boolean?>> {
        return FeatureFlags::class.java.declaredFields
            .filter { it.type == FeatureFlag::class.java }
            .sortedBy { it.name }
            .map { field ->
                field.isAccessible = true
                when (field.name) {
                    "nativeLinkEnabled" -> nativeLinkEnabled
                    "nativeLinkAttestationEnabled" -> nativeLinkAttestationEnabled
                    else -> featureFlag(field.name, field.get(FeatureFlags) as FeatureFlag)
                }
            }
    }

    private fun featureFlag(
        key: String,
        flag: FeatureFlag,
    ): CheckoutPlaygroundSettingDefinition.Value<Boolean?> = choice(
        key = "feature_flags.$key",
        displayName = flag.name,
        defaultValue = null,
        options = listOf("Default" to null, "On" to true, "Off" to false),
        applyFeatureFlags = { enabled ->
            if (enabled == null) {
                flag.reset()
            } else {
                flag.setEnabled(enabled)
            }
        },
    )
}
