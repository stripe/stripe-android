package com.stripe.android.common.taptoadd

import android.content.Context
import android.content.pm.ApplicationInfo
import com.stripe.android.ApiConfiguration
import javax.inject.Inject
import javax.inject.Provider

internal interface TapToAddIsSimulatedProvider {
    fun get(): Boolean
}

internal class DefaultTapToAddIsSimulatedProvider @Inject constructor(
    private val applicationContext: Context,
    private val apiConfigProvider: Provider<ApiConfiguration.State>,
) : TapToAddIsSimulatedProvider {
    override fun get(): Boolean {
        val isLiveMode = apiConfigProvider.get().isLiveMode()
        val isDebuggable = (applicationContext.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

        return !isLiveMode && isDebuggable
    }
}
