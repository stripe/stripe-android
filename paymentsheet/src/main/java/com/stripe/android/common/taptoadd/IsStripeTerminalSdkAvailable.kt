package com.stripe.android.common.taptoadd

import com.stripe.android.core.utils.FeatureFlags
import javax.inject.Inject

internal fun interface IsStripeTerminalSdkAvailable {
    operator fun invoke(): Boolean
}

internal class DefaultIsStripeTerminalSdkAvailable @Inject constructor() : IsStripeTerminalSdkAvailable {
    override fun invoke(): Boolean {
        if (!FeatureFlags.enableTapToAdd.isEnabled) {
            return false
        }

        return try {
            // If found, 'core' library was imported
            Class.forName("com.stripe.stripeterminal.TerminalApplicationDelegate")
            // If found, 'taptopay' library was imported
            Class.forName("com.stripe.stripeterminal.taptopay.TapToPay")
            true
        } catch (_: Exception) {
            false
        }
    }
}
