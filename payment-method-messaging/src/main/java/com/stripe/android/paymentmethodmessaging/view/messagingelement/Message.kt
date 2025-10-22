package com.stripe.android.paymentmethodmessaging.view.messagingelement

import com.stripe.android.model.MessagingImage
import com.stripe.android.model.PaymentMethodMessage
import com.stripe.android.paymentmethodmessaging.view.messagingelement.Message.Empty
import com.stripe.android.paymentmethodmessaging.view.messagingelement.Message.MultiPartner
import com.stripe.android.paymentmethodmessaging.view.messagingelement.Message.SinglePartner

internal sealed class Message {
    data class SinglePartner(
        val lightImage: MessagingImage,
        val darkImage: MessagingImage,
        val flatImage: MessagingImage,
        val message: String
    ) : Message()

    data class MultiPartner(
        val lightImages: List<MessagingImage>,
        val darkImages: List<MessagingImage>,
        val flatImages: List<MessagingImage>,
        val message: String
    ) : Message()

    data object Empty : Message()
}

internal object MessageTransformer {
    fun transformPaymentMethodMessage(paymentMethodMessage: PaymentMethodMessage): Message {
        val singlePartner = buildSinglePartnerMessage(paymentMethodMessage)
        val multiPartner = buildMultiPartnerMessage(paymentMethodMessage)
        return singlePartner ?: multiPartner ?: Empty
    }

    private fun buildSinglePartnerMessage(paymentMethodMessage: PaymentMethodMessage): SinglePartner? {
        if (paymentMethodMessage.paymentMethods.size != 1) return null
        val inlinePromo = paymentMethodMessage.inlinePartnerPromotion ?: return null
        val lightImage = if (paymentMethodMessage.lightImages.isNotEmpty()) {
            paymentMethodMessage.lightImages[0]
        } else return null

        val darkImage = if (paymentMethodMessage.darkImages.isNotEmpty()) {
            paymentMethodMessage.darkImages[0]
        } else return null

        val flatImage = if (paymentMethodMessage.flatImages.isNotEmpty()) {
            paymentMethodMessage.flatImages[0]
        } else return null

        return SinglePartner(
            lightImage = lightImage,
            darkImage = darkImage,
            flatImage = flatImage,
            message = inlinePromo
        )
    }

    private fun buildMultiPartnerMessage(paymentMethodMessage: PaymentMethodMessage): MultiPartner? {
        val promo = paymentMethodMessage.promotion ?: return null
        return MultiPartner(
            lightImages = paymentMethodMessage.lightImages,
            darkImages = paymentMethodMessage.darkImages,
            flatImages = paymentMethodMessage.flatImages,
            message = promo
        )
    }
}