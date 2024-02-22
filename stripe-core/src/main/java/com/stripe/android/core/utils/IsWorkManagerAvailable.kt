package com.stripe.android.core.utils

import androidx.annotation.RestrictTo
import androidx.work.WorkManager

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun interface IsWorkManagerAvailable {
    operator fun invoke(): Boolean
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object RealIsWorkManagerAvailable : IsWorkManagerAvailable {
    override fun invoke(): Boolean {
        val workManagerInClasspath = runCatching {
            Class.forName("androidx.work.WorkManager")
        }.isSuccess

        return workManagerInClasspath && WorkManager.isInitialized()
    }
}
