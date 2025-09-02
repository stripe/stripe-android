package com.stripe.android.hcaptcha

import android.content.Context
import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface HCaptchaService {
    suspend fun performPassiveHCaptcha(
        context: Context,
        siteKey: String,
        rqData: String?
    ): Result

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    sealed interface Result {
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        data class Success(val token: String) : Result

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        data class Failure(val error: Throwable) : Result
    }
}
