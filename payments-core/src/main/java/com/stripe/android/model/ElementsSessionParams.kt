package com.stripe.android.model

import android.os.Parcelable
import androidx.annotation.RestrictTo
import kotlinx.parcelize.Parcelize
import java.util.Locale

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Parcelize
sealed interface ElementsSessionParams: Parcelable {
    val type: String
    val clientSecret: String?
    val locale: String?
    @Parcelize
    class PaymentIntentType(
        override val clientSecret: String?,
        override val type: String = "payment_intent",
        override val locale: String? = Locale.getDefault().toLanguageTag(),
    ) : ElementsSessionParams

    @Parcelize
    class SetupIntentType(
        override val clientSecret: String?,
        override val type: String = "setup_intent",
        override val locale: String? = Locale.getDefault().toLanguageTag(),
    ) : ElementsSessionParams

    @Parcelize
    class DeferredIntentType(
        override val clientSecret: String? = null,
        override val type: String = "deferred_intent",
        override val locale: String? = Locale.getDefault().toLanguageTag(),
        val deferredIntentParams: DeferredIntentParams
    ) : ElementsSessionParams
}
