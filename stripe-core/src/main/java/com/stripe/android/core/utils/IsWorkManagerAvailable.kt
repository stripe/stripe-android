package com.stripe.android.core.utils

import androidx.annotation.RestrictTo
import androidx.work.WorkManager
import javax.inject.Inject

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun interface IsWorkManagerAvailable {
    suspend operator fun invoke(): Boolean
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class RealIsWorkManagerAvailable @Inject constructor(
    private val isEnabledForMerchant: suspend () -> Boolean,
) : IsWorkManagerAvailable {
    override suspend fun invoke(): Boolean {
        val workManagerInClasspath = runCatching {
            Class.forName("androidx.work.WorkManager")
        }.isSuccess

        return isEnabledForMerchant() &&
            workManagerInClasspath &&
            WorkManager.isInitialized()
    }
}
