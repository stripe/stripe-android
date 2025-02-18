package com.stripe.android.model

import android.os.Parcelable
import androidx.annotation.RestrictTo
import kotlinx.parcelize.Parcelize
import java.util.Locale

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Parcelize
sealed interface ElementsSessionParams : Parcelable {

    val type: String
    val clientSecret: String?
    val customerSessionClientSecret: String?
    val locale: String?
    val expandFields: List<String>
    val savedPaymentMethodSelectionId: String?
    val externalPaymentMethods: List<String>
    val appId: String

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Parcelize
    data class PaymentIntentType(
        override val clientSecret: String,
        override val locale: String? = Locale.getDefault().toLanguageTag(),
        override val customerSessionClientSecret: String? = null,
        override val savedPaymentMethodSelectionId: String? = null,
        override val externalPaymentMethods: List<String>,
        override val appId: String,
    ) : ElementsSessionParams {

        override val type: String
            get() = "payment_intent"

        override val expandFields: List<String>
            get() = listOf("payment_method_preference.$type.payment_method")
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Parcelize
    data class SetupIntentType(
        override val clientSecret: String,
        override val locale: String? = Locale.getDefault().toLanguageTag(),
        override val customerSessionClientSecret: String? = null,
        override val savedPaymentMethodSelectionId: String? = null,
        override val externalPaymentMethods: List<String>,
        override val appId: String,
    ) : ElementsSessionParams {

        override val type: String
            get() = "setup_intent"

        override val expandFields: List<String>
            get() = listOf("payment_method_preference.$type.payment_method")
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Parcelize
    data class DeferredIntentType(
        override val locale: String? = Locale.getDefault().toLanguageTag(),
        val deferredIntentParams: DeferredIntentParams,
        override val externalPaymentMethods: List<String>,
        override val savedPaymentMethodSelectionId: String? = null,
        override val customerSessionClientSecret: String? = null,
        override val appId: String,
    ) : ElementsSessionParams {

        override val clientSecret: String?
            get() = null

        override val type: String
            get() = "deferred_intent"

        override val expandFields: List<String>
            get() = emptyList()
    }
}
