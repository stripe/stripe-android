package com.stripe.android.paymentelement.embedded.sheet

import android.app.Activity
import android.content.Intent
import androidx.activity.result.ActivityResult
import com.stripe.android.link.account.LinkAccountHolder
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentelement.embedded.EmbeddedActivityResult
import com.stripe.android.paymentsheet.CustomerStateHolder
import com.stripe.android.paymentsheet.PaymentOptionsActivityResult
import com.stripe.android.paymentsheet.PaymentSheetContract
import com.stripe.android.paymentsheet.PaymentSheetResult
import com.stripe.android.paymentsheet.addresselement.PaymentElementAutocompleteAddressInteractor
import com.stripe.android.paymentsheet.model.PaymentSelection
import javax.inject.Inject

internal class SheetActivityResultHandler @Inject constructor(
    private val args: SheetActivityArgs,
    private val customerStateHolder: CustomerStateHolder,
    private val linkAccountHolder: LinkAccountHolder,
    private val autocompleteAddressInteractorFactory: PaymentElementAutocompleteAddressInteractor.Factory,
) {
    val shouldReportDismiss: Boolean
        get() = args is SheetActivityArgs.Embedded

    fun createResult(
        result: EmbeddedActivityResult,
        originalIntent: Intent,
    ): ActivityResult {
        return when (args) {
            is SheetActivityArgs.Embedded -> ActivityResult(
                Activity.RESULT_OK,
                EmbeddedActivityResult.toIntent(originalIntent, result),
            )
            is SheetActivityArgs.PaymentSheet -> ActivityResult(
                Activity.RESULT_OK,
                Intent().putExtras(PaymentSheetContract.Result(result.asPaymentSheetResult()).toBundle()),
            )
            is SheetActivityArgs.PaymentOptions -> result.asPaymentOptionsResult(args).let {
                ActivityResult(it.resultCode, Intent().putExtras(it.toBundle()))
            }
        }
    }

    private fun EmbeddedActivityResult.asPaymentSheetResult(): PaymentSheetResult {
        return when (this) {
            is EmbeddedActivityResult.Complete -> PaymentSheetResult.Completed()
            is EmbeddedActivityResult.Cancelled -> PaymentSheetResult.Canceled()
            is EmbeddedActivityResult.Error -> PaymentSheetResult.Failed(
                IllegalStateException("Failed to retrieve a PaymentSheet result.")
            )
        }
    }

    private fun EmbeddedActivityResult.asPaymentOptionsResult(
        args: SheetActivityArgs.PaymentOptions,
    ): PaymentOptionsActivityResult {
        val paymentMethods = customerStateHolder.paymentMethods.value
        return when (this) {
            is EmbeddedActivityResult.Complete ->
                selection
                    ?.withLinkDetails(args.args.state.paymentSelection)
                    ?.let { selection ->
                        PaymentOptionsActivityResult.Succeeded(
                            paymentSelection = selection,
                            linkAccountInfo = linkAccountHolder.linkAccountInfo.value,
                            paymentMethods = paymentMethods,
                            autocompleteFilledAddress = autocompleteAddressInteractorFactory.autocompleteFilledAddress,
                        )
                    }
                    ?: cancelledPaymentOptionsResult(args, paymentMethods)
            is EmbeddedActivityResult.Cancelled,
            is EmbeddedActivityResult.Error -> cancelledPaymentOptionsResult(args, paymentMethods)
        }
    }

    private fun cancelledPaymentOptionsResult(
        args: SheetActivityArgs.PaymentOptions,
        paymentMethods: List<PaymentMethod>,
    ): PaymentOptionsActivityResult.Canceled {
        val initialSelection = args.args.state.paymentSelection
            ?.withLinkDetails(args.args.state.paymentSelection)
        val validInitialSelection = if (initialSelection is PaymentSelection.Saved) {
            paymentMethods.firstOrNull { it.id == initialSelection.paymentMethod.id }?.let { paymentMethod ->
                initialSelection.copy(paymentMethod = paymentMethod)
            }
        } else {
            initialSelection
        }
        return PaymentOptionsActivityResult.Canceled(
            mostRecentError = null,
            paymentSelection = validInitialSelection,
            paymentMethods = paymentMethods,
            linkAccountInfo = linkAccountHolder.linkAccountInfo.value,
        )
    }

    private fun PaymentSelection.withLinkDetails(initialSelection: PaymentSelection?): PaymentSelection {
        return when (this) {
            is PaymentSelection.Link -> when (linkAccountHolder.linkAccountInfo.value.account) {
                null -> copy(selectedPayment = null)
                else -> copy(
                    selectedPayment = selectedPayment ?: (initialSelection as? PaymentSelection.Link)?.selectedPayment
                )
            }
            else -> this
        }
    }
}
