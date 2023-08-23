package com.stripe.android.paymentsheet.state

import com.stripe.android.paymentsheet.BuildConfig
import javax.inject.Inject

internal fun interface CbcEnabledProvider {
    suspend operator fun invoke(): Boolean
}

internal class RealCbcEnabledProvider @Inject constructor() : CbcEnabledProvider {
    override suspend fun invoke(): Boolean = BuildConfig.DEBUG
}
