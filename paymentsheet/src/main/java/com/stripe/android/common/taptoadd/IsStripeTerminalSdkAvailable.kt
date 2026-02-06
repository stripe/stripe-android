package com.stripe.android.common.taptoadd

import com.stripe.android.core.utils.FeatureFlags
import javax.inject.Inject

internal fun interface IsStripeTerminalSdkAvailable {
    operator fun invoke(): Boolean
}

internal class DefaultIsStripeTerminalSdkAvailable @Inject constructor() : IsStripeTerminalSdkAvailable {
    override fun invoke(): Boolean {
        return true
    }
}
