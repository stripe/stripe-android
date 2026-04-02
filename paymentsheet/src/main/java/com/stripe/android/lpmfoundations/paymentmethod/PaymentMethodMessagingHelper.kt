package com.stripe.android.lpmfoundations.paymentmethod

import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.Nullable
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.model.PaymentMethodMessagePromotion
import com.stripe.android.model.PaymentMethodMessagePromotionList
import kotlinx.parcelize.Parcelize
import java.util.Locale.getDefault

interface PaymentMethodMessagingHelper : Parcelable {
    fun getPromotion(code: PaymentMethodCode): PaymentMethodMessagePromotion?
    val promotions: PaymentMethodMessagePromotionList?
}

@Parcelize
class DefaultPaymentMethodMessagingHelper(
    override val promotions: PaymentMethodMessagePromotionList?
) : PaymentMethodMessagingHelper {
    override fun getPromotion(code: PaymentMethodCode): PaymentMethodMessagePromotion? {
        return promotions?.promotions?.find { it.paymentMethodType.lowercase(getDefault()) == code }
    }
}

@Parcelize
class NoopPaymentMethodMessagingHelper(
    override val promotions: PaymentMethodMessagePromotionList? = null
) : PaymentMethodMessagingHelper {
    override fun getPromotion(code: PaymentMethodCode): PaymentMethodMessagePromotion? {
        return null
    }
}