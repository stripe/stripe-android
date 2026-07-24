package com.stripe.android.common.taptoadd

import android.content.Context
import android.content.pm.ApplicationInfo

internal interface TapToAddIsSimulatedProvider {
    fun get(): Boolean
}

internal class DefaultTapToAddIsSimulatedProvider(
    private val applicationContext: Context,
    private val isLiveModeProvider: () -> Boolean,
) : TapToAddIsSimulatedProvider {
    override fun get(): Boolean {
        val isLiveMode = isLiveModeProvider()
        val isDebuggable = (applicationContext.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

        return !isLiveMode && isDebuggable
    }
}
