package com.stripe.android.paymentsheet

import com.stripe.android.paymentelement.FlowControllerPaymentOptionResultPreview
import com.stripe.android.paymentsheet.model.PaymentOption
import dev.drewhamilton.poko.Poko

/**
 * Result containing an update to the user's payment option and the action is was received from.
 */
@FlowControllerPaymentOptionResultPreview
@Poko
class PaymentOptionResult(
    /**
     * The updated user payment option. Always update your internally tracked option irregardless of what action
     * it is received from.
     */
    val paymentOption: PaymentOption?,

    /**
     * Indicates the updated payment option was received when the user canceled selecting an option in the
     * sheet. Updated payment options can occur in this manner if the user updated details of their selected
     * payment option or removed their selected payment option without selecting a new one.
     */
    val didCancel: Boolean,
)
