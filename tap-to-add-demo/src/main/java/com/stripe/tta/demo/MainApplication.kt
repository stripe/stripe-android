package com.stripe.tta.demo

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.stripe.android.core.utils.FeatureFlags

class MainApplication : Application() {
    init {
        FeatureFlags.nativeLinkEnabled.setEnabled(true)
        FeatureFlags.nativeLinkAttestationEnabled.setEnabled(false)

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
    }
}
