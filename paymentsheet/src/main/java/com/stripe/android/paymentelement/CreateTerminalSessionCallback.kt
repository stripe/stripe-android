package com.stripe.android.paymentelement

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@TapToAddPreview
fun interface CreateTerminalSessionCallback {
    suspend fun createTerminalSession(): CreateTerminalSessionResult
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@TapToAddPreview
sealed interface CreateTerminalSessionResult {
    class Success(
        internal val connectionToken: String,
        internal val locationId: String,
    ) : CreateTerminalSessionResult

    class Failure @JvmOverloads constructor(
        internal val cause: Exception,
        internal val displayMessage: String? = null,
    ) : CreateTerminalSessionResult
}