package com.stripe.android.model

import android.os.Parcelable
import androidx.annotation.RestrictTo
import com.stripe.android.core.model.StripeModel
import kotlinx.parcelize.Parcelize

@Parcelize
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
sealed class PaymentMethodMessage : StripeModel {
    data class SinglePartner(
        val inlinePartnerPromotion: String,
        val lightImage: PaymentMethodMessageImage,
        val darkImage: PaymentMethodMessageImage,
        val flatImage: PaymentMethodMessageImage,
        val learnMore: PaymentMethodMessageLearnMore,
        val paymentMethods: List<String>,
    ) : PaymentMethodMessage()

    data class MultiPartner(
        val promotion: String,
        val lightImages: List<PaymentMethodMessageImage>,
        val darkImages: List<PaymentMethodMessageImage>,
        val flatImages: List<PaymentMethodMessageImage>,
        val learnMore: PaymentMethodMessageLearnMore,
        val paymentMethods: List<String>
    ) : PaymentMethodMessage()

    data class UnexpectedError(
        val message: String
    ) : PaymentMethodMessage()

    data class NoContent(
        val paymentMethods: List<String>
    ) : PaymentMethodMessage()
}

@Parcelize
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class PaymentMethodMessageImage
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
constructor(
    val role: String,
    val url: String,
    val paymentMethodType: String,
    val text: String,
) : Parcelable

@Parcelize
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class PaymentMethodMessageLearnMore
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
constructor(
    val url: String,
    val message: String
) : Parcelable
