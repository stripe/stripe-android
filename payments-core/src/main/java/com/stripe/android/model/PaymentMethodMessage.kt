package com.stripe.android.model

import android.os.Parcelable
import androidx.annotation.RestrictTo
import com.stripe.android.core.model.StripeModel
import kotlinx.parcelize.Parcelize

@Parcelize
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class PaymentMethodMessage
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
constructor(
    val paymentMethods: List<String>,
    val singlePartner: PaymentMethodMessageSinglePartner?,
    val multiPartner: PaymentMethodMessageMultiPartner?,
) : StripeModel

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
data class PaymentMethodMessageSinglePartner
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
constructor(
    val inlinePartnerPromotion: String,
    val lightImage: PaymentMethodMessageImage,
    val darkImage: PaymentMethodMessageImage,
    val flatImage: PaymentMethodMessageImage,
    val learnMore: PaymentMethodMessageLearnMore
) : Parcelable

@Parcelize
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class PaymentMethodMessageMultiPartner
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
constructor(
    val promotion: String,
    val lightImages: List<PaymentMethodMessageImage>,
    val darkImages: List<PaymentMethodMessageImage>,
    val flatImages: List<PaymentMethodMessageImage>,
    val learnMore: PaymentMethodMessageLearnMore
) : Parcelable
