package com.stripe.android.model

import android.os.Parcelable
import androidx.annotation.RestrictTo
import com.stripe.android.core.model.StripeModel
import kotlinx.parcelize.Parcelize

@Parcelize
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
sealed class PaymentMethodMessage : StripeModel {
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    data class SinglePartner
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    constructor(
        val inlinePartnerPromotion: String,
        val lightImage: PaymentMethodMessageImage,
        val darkImage: PaymentMethodMessageImage,
        val flatImage: PaymentMethodMessageImage,
        val learnMore: PaymentMethodMessageLearnMore,
        val legalDisclosure: PaymentMethodMessageLegalDisclosure?,
        val paymentMethods: List<String>,
    ) : PaymentMethodMessage()

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    data class MultiPartner
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    constructor(
        val promotion: String,
        val lightImages: List<PaymentMethodMessageImage>,
        val darkImages: List<PaymentMethodMessageImage>,
        val flatImages: List<PaymentMethodMessageImage>,
        val learnMore: PaymentMethodMessageLearnMore,
        val legalDisclosure: PaymentMethodMessageLegalDisclosure?,
        val paymentMethods: List<String>
    ) : PaymentMethodMessage()

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    data class UnexpectedError
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    constructor(
        val message: String
    ) : PaymentMethodMessage()

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    data class NoContent
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    constructor(
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

@Parcelize
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class PaymentMethodMessageLegalDisclosure
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
constructor(
    val message: String
) : Parcelable
