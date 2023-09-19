package com.stripe.android.view

import androidx.annotation.RestrictTo
import com.stripe.android.BuildConfig
import javax.inject.Inject

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun interface CbcEnabledProvider {
    suspend operator fun invoke(): Boolean
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class RealCbcEnabledProvider @Inject constructor() : CbcEnabledProvider {
    override suspend fun invoke(): Boolean = BuildConfig.DEBUG
}
