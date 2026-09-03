package com.stripe.android.paymentsheet

import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import com.stripe.android.link.LinkAccountUpdate
import com.stripe.android.model.PaymentMethodMessagePromotion
import com.stripe.android.paymentelement.embedded.sheet.EmbeddedSheetActivity
import com.stripe.android.paymentelement.embedded.sheet.SheetActivityArgs
import com.stripe.android.paymentsheet.state.PaymentSheetState
import com.stripe.android.view.ActivityStarter
import kotlinx.parcelize.Parcelize

internal class PaymentOptionContract :
    ActivityResultContract<PaymentOptionContract.Args, PaymentOptionsActivityResult?>() {
    override fun createIntent(
        context: Context,
        input: Args
    ): Intent {
        return Intent(context, EmbeddedSheetActivity::class.java)
            .putExtra(
                SheetActivityArgs.EXTRA_ARGS,
                SheetActivityArgs.PaymentOptions(input),
            )
    }

    override fun parseResult(
        resultCode: Int,
        intent: Intent?
    ): PaymentOptionsActivityResult? {
        return PaymentOptionsActivityResult.fromIntent(intent)
    }

    @Parcelize
    internal data class Args(
        val state: PaymentSheetState.Full,
        val configuration: PaymentSheet.Configuration,
        val linkAccountInfo: LinkAccountUpdate.Value,
        val enableLogging: Boolean,
        val walletButtonsRendered: Boolean,
        val productUsage: Set<String>,
        val paymentElementCallbackIdentifier: String,
        val promotions: List<PaymentMethodMessagePromotion>?
    ) : ActivityStarter.Args
}
