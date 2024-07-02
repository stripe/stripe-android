package com.stripe.android.core.utils

import android.content.Context
import android.content.pm.ApplicationInfo
import androidx.annotation.RestrictTo
import com.stripe.android.core.BuildConfig
import com.stripe.android.core.Logger
import javax.inject.Inject

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface UserFacingLogger {
    fun logWarningWithoutPii(message: String)
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class RealUserFacingLogger @Inject constructor(context: Context) : UserFacingLogger {

    private val isDebuggable = 0 != context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE
    private val isDebugBuild = BuildConfig.DEBUG

    private val logger = Logger.getInstance(enableLogging = isDebuggable || isDebugBuild)

    override fun logWarningWithoutPii(message: String) {
        logger.warning(message)
    }
}
