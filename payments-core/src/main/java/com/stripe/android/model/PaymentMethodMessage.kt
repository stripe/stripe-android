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
    val inlinePartnerPromotion: String?,
    val promotion: String?,
    val lightImages: List<MessagingImage>,
    val darkImages: List<MessagingImage>,
    val flatImages: List<MessagingImage>,
    val learnMoreUrl: String?
) : StripeModel

@Parcelize
data class MessagingImage
constructor(
    val role: String,
    val url: String,
    val paymentMethodType: String,
    val text: String,
) : Parcelable

enum class MessagingImageType {
    LIGHT,
    FLAT,
    DARK
}

@Parcelize
data class MessagingLearnMore
constructor(
    val url: String,
    val message: String
) : Parcelable

internal fun PaymentMethodMessage.isNoContent() = paymentMethods.isEmpty()
internal fun PaymentMethodMessage.isSinglePartner() = !inlinePartnerPromotion.isNullOrBlank()