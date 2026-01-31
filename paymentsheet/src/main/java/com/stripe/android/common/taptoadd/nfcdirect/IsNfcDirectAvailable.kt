package com.stripe.android.common.taptoadd.nfcdirect

import android.content.Context
import android.nfc.NfcAdapter
import android.os.Build
import com.stripe.android.core.utils.FeatureFlags
import javax.inject.Inject

/**
 * Checks if NFC Direct card reading is available on this device.
 *
 * Requirements:
 * - Feature flag enabled
 * - Android 4.4 (KitKat) or higher for IsoDep support
 * - NFC hardware present
 * - NFC enabled in device settings
 */
internal fun interface IsNfcDirectAvailable {
    operator fun invoke(): Boolean
}

internal class DefaultIsNfcDirectAvailable @Inject constructor(
    private val context: Context,
) : IsNfcDirectAvailable {

    override fun invoke(): Boolean {
        // Check feature flag
        if (!FeatureFlags.nfcDirect.isEnabled) {
            return false
        }

        // Require KitKat for proper IsoDep support
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            return false
        }

        // Check NFC hardware and enabled state
        val nfcAdapter = NfcAdapter.getDefaultAdapter(context)
        return nfcAdapter?.isEnabled == true
    }
}
