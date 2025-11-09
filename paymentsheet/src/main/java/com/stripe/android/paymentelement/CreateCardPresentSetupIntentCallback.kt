package com.stripe.android.paymentelement

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@TapToAddPreview
fun interface CreateCardPresentSetupIntentCallback {
    suspend fun createCardPresentSetupIntent(customerId: String): CreateCardPresentSetupIntentResult
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@TapToAddPreview
sealed interface CreateCardPresentSetupIntentResult {

    class Success(internal val clientSecret: String) : CreateCardPresentSetupIntentResult

    class Failure @JvmOverloads constructor(
        internal val cause: Exception,
        internal val displayMessage: String? = null,
    ) : CreateCardPresentSetupIntentResult
}
