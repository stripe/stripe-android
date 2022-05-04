package com.stripe.android.paymentsheet.paymentdatacollection.ach

import androidx.annotation.StringRes
import com.stripe.android.model.ConfirmStripeIntentParams

sealed class USBankAccountFormScreenState {
    class NameAndEmailCollection(
        @StringRes val error: Int? = null,
        val primaryButtonText: String?,
        val primaryButtonOnClick: () -> Unit,
    ) : USBankAccountFormScreenState()

    data class MandateCollection(
        val bankName: String?,
        val displayName: String?,
        val last4: String?,
        val primaryButtonText: String?,
        val primaryButtonOnClick: () -> Unit,
        val mandateText: String
    ) : USBankAccountFormScreenState()

    data class VerifyWithMicrodeposits(
        val bankName: String?,
        val displayName: String?,
        val last4: String?,
        val primaryButtonText: String?,
        val primaryButtonOnClick: () -> Unit,
        val mandateText: String
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
