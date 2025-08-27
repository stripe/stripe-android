package com.stripe.android.crypto.onramp.example

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.stripe.android.core.utils.FeatureFlags

internal class ConfigurationManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var attestationMode: AttestationMode
        get() {
            val modeString = prefs.getString(KEY_ATTESTATION_MODE, AttestationMode.NONE.name)
            return AttestationMode.valueOf(modeString ?: AttestationMode.NONE.name)
        }
        set(value) {
            prefs.edit { putString(KEY_ATTESTATION_MODE, value.name) }
            updateFeatureFlag(value)
        }

    private fun updateFeatureFlag(mode: AttestationMode) {
        when (mode) {
            AttestationMode.ENABLED -> FeatureFlags.nativeLinkAttestationEnabled.setEnabled(true)
            AttestationMode.DISABLED -> FeatureFlags.nativeLinkAttestationEnabled.setEnabled(false)
            AttestationMode.NONE -> FeatureFlags.nativeLinkAttestationEnabled.reset()
        }
    }

    fun restore() {
        updateFeatureFlag(attestationMode)
    }

    companion object {
        private const val PREFS_NAME = "onramp_config"
        private const val KEY_ATTESTATION_MODE = "attestation_mode"
    }
}
