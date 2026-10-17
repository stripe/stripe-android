package com.stripe.android.common.nfcscan.scanner.apdu.pdol

import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata

/*
 * Indicates the amount authorized for purchase to the card. If no amount (such as `SetupIntent`), set to 0 by default.
 * Amount is encoded into binary-coded decimal format first.
 */
internal object AmountAuthorizedProducer : TagValueProducer {
    override fun produce(paymentMethodMetadata: PaymentMethodMetadata): TagValueProducer.Result {
        val amount = paymentMethodMetadata.amount()?.value ?: 0L

        return TagValueProducer.Result(
            tag = TAG_AMOUNT_AUTHORIZED,
            value = BcdEncoding.encode(amount, AMOUNT_LENGTH),
        )
    }

    private const val TAG_AMOUNT_AUTHORIZED = "9F02"
    private const val AMOUNT_LENGTH = 6
}
