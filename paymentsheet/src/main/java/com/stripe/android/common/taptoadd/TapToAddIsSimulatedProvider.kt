package com.stripe.android.common.taptoadd

import android.content.Context
import android.content.pm.ApplicationInfo
import com.stripe.android.core.ApiConfiguration
import javax.inject.Inject

internal interface TapToAddIsSimulatedProvider {
    fun get(apiConfiguration: ApiConfiguration.State): Boolean
}

internal class DefaultTapToAddIsSimulatedProvider @Inject constructor(
    private val applicationContext: Context,
) : TapToAddIsSimulatedProvider {
    override fun get(apiConfiguration: ApiConfiguration.State): Boolean {
        val isDebuggable = (applicationContext.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

        return !apiConfiguration.isLiveMode() && isDebuggable
    }
}
