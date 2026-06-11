package com.stripe.android.paymentelement.embedded.sheet

import com.stripe.android.checkout.Checkout
import com.stripe.android.checkout.CheckoutInstances
import com.stripe.android.checkout.InSheetCheckoutSessionUpdater
import com.stripe.android.common.exception.stripeErrorMessage
import com.stripe.android.common.model.asCommonConfiguration
import com.stripe.android.core.injection.ViewModelScope
import com.stripe.android.lpmfoundations.paymentmethod.IntegrationMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.gpay.GooglePayDisplayItemsFactory
import com.stripe.android.paymentelement.confirmation.toConfirmationOption
import com.stripe.android.paymentelement.embedded.EmbeddedSelectionHolder
import com.stripe.android.paymentelement.embedded.form.FormResult
import com.stripe.android.paymentelement.embedded.form.OnClickOverrideDelegate
import com.stripe.android.paymentsheet.CustomerStateHolder
import com.stripe.android.paymentsheet.analytics.EventReporter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

internal interface SheetActivityConfirmationHelper {
    fun confirm()
}

internal class DefaultSheetActivityConfirmationHelper @Inject constructor(
    private val paymentMethodMetadata: PaymentMethodMetadata,
    private val confirmationHandler: ConfirmationHandler,
    private val configuration: EmbeddedPaymentElement.Configuration,
    private val selectionHolder: EmbeddedSelectionHolder,
    private val stateHelper: SheetActivityStateHolder,
    private val onClickDelegate: OnClickOverrideDelegate,
    private val eventReporter: EventReporter,
    private val customerStateHolder: CustomerStateHolder,
    @ViewModelScope private val coroutineScope: CoroutineScope,
) : SheetActivityConfirmationHelper {

    @OptIn(CheckoutSessionPreview::class)
    private val inSheetCheckoutSessionUpdater = InSheetCheckoutSessionUpdater(
        checkout = resolveCheckout(),
    )

    @OptIn(CheckoutSessionPreview::class)
    private fun resolveCheckout(): Checkout? {
        val metadata = paymentMethodMetadata.integrationMetadata
            as? IntegrationMetadata.CheckoutSession ?: return null
        return CheckoutInstances[metadata.instancesKey].firstOrNull()
    }

    override fun confirm() {
        if (onClickDelegate.onClickOverride != null) {
            onClickDelegate.onClickOverride?.invoke()
        } else {
            selectionHolder.selection.value?.let { paymentSelection ->
                eventReporter.onPressConfirmButton(paymentSelection)
            }

            when (configuration.formSheetAction) {
                EmbeddedPaymentElement.FormSheetAction.Continue -> {
                    if (inSheetCheckoutSessionUpdater.requiresUpdate()) {
                        coroutineScope.launch {
                            performInSheetTaxUpdateThenContinue()
                        }
                    } else {
                        emitContinueResult()
                    }
                }
                EmbeddedPaymentElement.FormSheetAction.Confirm -> {
                    confirmationArgs()?.let { args ->
                        coroutineScope.launch {
                            confirmationHandler.start(args)
                        }
                    }
                }
            }
        }
    }

    private suspend fun performInSheetTaxUpdateThenContinue() {
        val selection = selectionHolder.selection.value ?: run {
            emitContinueResult()
            return
        }
        stateHelper.setProcessing(true)
        inSheetCheckoutSessionUpdater.performUpdate(selection).fold(
            onSuccess = {
                stateHelper.setProcessing(false)
                emitContinueResult()
            },
            onFailure = { error ->
                stateHelper.setProcessing(false)
                stateHelper.updateError(error.stripeErrorMessage())
            }
        )
    }

    private fun emitContinueResult() {
        stateHelper.setResult(
            FormResult.Complete(
                selection = selectionHolder.selection.value,
                hasBeenConfirmed = false,
                customerState = customerStateHolder.customer.value
            )
        )
    }

    private fun confirmationArgs(): ConfirmationHandler.Args? {
        val confirmationOption = selectionHolder.selection.value?.toConfirmationOption(
            configuration = configuration.asCommonConfiguration(),
            linkConfiguration = paymentMethodMetadata.linkState?.configuration,
            cardFundingFilter = paymentMethodMetadata.cardFundingFilter,
            googlePayDisplayItems = GooglePayDisplayItemsFactory.create(paymentMethodMetadata),
        ) ?: return null
        return ConfirmationHandler.Args(
            confirmationOption = confirmationOption,
            paymentMethodMetadata = paymentMethodMetadata,
        )
    }
}
