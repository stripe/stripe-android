package com.stripe.android.paymentsheet.example.playground

import com.stripe.android.core.utils.FeatureFlags
import com.stripe.android.paymentsheet.example.playground.settings.LinkType

internal fun LinkType.applyFeatureFlags() {
    when (this) {
        LinkType.ServerControlled -> {
            FeatureFlags.nativeLinkEnabled.reset()
            FeatureFlags.nativeLinkAttestationEnabled.reset()
        }
        LinkType.Native -> {
            FeatureFlags.nativeLinkEnabled.setEnabled(true)
            FeatureFlags.nativeLinkAttestationEnabled.setEnabled(false)
        }
        LinkType.NativeAttest -> {
            FeatureFlags.nativeLinkEnabled.setEnabled(true)
            FeatureFlags.nativeLinkAttestationEnabled.setEnabled(true)
        }
        LinkType.Web -> {
            FeatureFlags.nativeLinkEnabled.setEnabled(false)
            FeatureFlags.nativeLinkAttestationEnabled.setEnabled(false)
        }
    }
}
