package com.stripe.android.paymentelement.confirmation.intent

import android.os.Parcelable
import androidx.activity.result.ActivityResultLauncher
import com.stripe.android.payments.paymentlauncher.InternalPaymentResult
import com.stripe.android.payments.paymentlauncher.PaymentLauncherContract
import com.stripe.android.paymentsheet.paymentdatacollection.updatedtax.UpdatedTaxAmountContract
import com.stripe.android.paymentsheet.paymentdatacollection.updatedtax.UpdatedTaxAmountLauncher
import com.stripe.android.paymentsheet.paymentdatacollection.updatedtax.UpdatedTaxAmountResult
import kotlinx.parcelize.Parcelize

internal data class IntentConfirmationLauncher(
    val paymentLauncher: ActivityResultLauncher<PaymentLauncherContract.Args>,
    val updatedTaxAmountActivityLauncher: ActivityResultLauncher<UpdatedTaxAmountContract.Args>,
    val updatedTaxAmountLauncher: UpdatedTaxAmountLauncher,
)

internal sealed interface IntentConfirmationLauncherResult : Parcelable {
    @Parcelize
    data class Payment(val result: InternalPaymentResult) : IntentConfirmationLauncherResult

    @Parcelize
    data class UpdatedTaxAmount(val result: UpdatedTaxAmountResult) : IntentConfirmationLauncherResult
}
