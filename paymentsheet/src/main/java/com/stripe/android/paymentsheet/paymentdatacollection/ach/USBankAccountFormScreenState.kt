package com.stripe.android.paymentsheet.paymentdatacollection.ach

import androidx.annotation.StringRes
import com.stripe.android.model.ConfirmStripeIntentParams

sealed class USBankAccountFormScreenState {
    class NameAndEmailCollection(
        @StringRes val error: Int? = null
    ) : USBankAccountFormScreenState()
    data class MandateCollection(
        val intentId: String,
        val linkAccountId: String,
        val bankName: String?,
        val displayName: String?,
        val last4: String?,
        val saveForFutureUse: Boolean
    ) : USBankAccountFormScreenState()
    data class VerifyWithMicrodeposits(
        val intentId: String,
        val linkAccountId: String,
        val bankName: String?,
        val displayName: String?,
        val last4: String?,
        val saveForFutureUse: Boolean
    ) : USBankAccountFormScreenState()
    data class ConfirmIntent(
        val confirmIntentParams: ConfirmStripeIntentParams
    ) : USBankAccountFormScreenState()
    data class Finished(
        val linkAccountId: String,
        val last4: String,
        val bankName: String
    ) : USBankAccountFormScreenState()
}
