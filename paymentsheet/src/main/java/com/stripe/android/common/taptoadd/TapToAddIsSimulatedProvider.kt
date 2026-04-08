package com.stripe.android.common.taptoadd

import android.content.Context
import android.content.pm.ApplicationInfo
import com.stripe.android.PaymentConfiguration
import javax.inject.Inject
import javax.inject.Provider

internal interface TapToAddIsSimulatedProvider {
    fun get(): Boolean
}

internal class DefaultTapToAddIsSimulatedProvider @Inject constructor(
    private val applicationContext: Context,
    private val paymentConfiguration: Provider<PaymentConfiguration>,
) : TapToAddIsSimulatedProvider {
    override fun get(): Boolean {
        val isLiveMode = paymentConfiguration.get().isLiveMode()
        val isDebuggable = (applicationContext.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

        return !isLiveMode && isDebuggable
    }
}
