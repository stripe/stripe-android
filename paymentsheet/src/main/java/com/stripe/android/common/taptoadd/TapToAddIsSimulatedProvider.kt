package com.stripe.android.common.taptoadd

import android.content.Context
import android.content.pm.ApplicationInfo
import javax.inject.Inject

internal interface TapToAddIsSimulatedProvider {
    fun get(isLiveMode: Boolean): Boolean
}

internal class DefaultTapToAddIsSimulatedProvider @Inject constructor(
    private val applicationContext: Context,
) : TapToAddIsSimulatedProvider {
    override fun get(isLiveMode: Boolean): Boolean {
        val isDebuggable = (applicationContext.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

        return !isLiveMode && isDebuggable
    }
}
