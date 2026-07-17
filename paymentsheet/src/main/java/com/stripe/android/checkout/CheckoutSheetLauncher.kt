package com.stripe.android.checkout

import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.stripe.android.checkout.injection.CheckoutPresenterScope
import com.stripe.android.lpmfoundations.paymentmethod.IntegrationMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.PaymentMethodMessagePromotion
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackIdentifier
import com.stripe.android.paymentelement.embedded.EmbeddedActivityArgs
import com.stripe.android.paymentelement.embedded.EmbeddedActivityResult
import com.stripe.android.paymentelement.embedded.EmbeddedLaunchMode
import com.stripe.android.paymentelement.embedded.EmbeddedRowSelectionImmediateActionHandler
import com.stripe.android.paymentelement.embedded.EmbeddedSelectionHolder
import com.stripe.android.paymentelement.embedded.content.EmbeddedConfirmationStateHolder
import com.stripe.android.paymentelement.embedded.content.EmbeddedSheetLauncher
import com.stripe.android.paymentelement.embedded.content.SheetStateHolder
import com.stripe.android.paymentelement.embedded.sheet.EmbeddedSheetContract
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.payments.core.injection.STATUS_BAR_COLOR
import com.stripe.android.paymentsheet.CustomerStateHolder
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.paymentMethodType
import com.stripe.android.paymentsheet.state.CustomerState
import javax.inject.Inject
import javax.inject.Named

/**
 * Checkout's [EmbeddedSheetLauncher]: launches the embedded payment-options / form / manage sheets
 * (via [EmbeddedSheetContract]) for the checkout integration and folds their results back into the
 * shared [EmbeddedSelectionHolder] / [CustomerStateHolder].
 *
 * This mirrors [com.stripe.android.paymentelement.embedded.content.DefaultEmbeddedSheetLauncher] but
 * deliberately omits the embedded result-callback plumbing: confirmation isn't wired for checkout
 * yet, so a confirmed result is not surfaced anywhere (see the `TODO(confirm)` markers).
 */
@CheckoutPresenterScope
internal class CheckoutSheetLauncher @Inject constructor(
    activityResultCaller: ActivityResultCaller,
    lifecycleOwner: LifecycleOwner,
    private val selectionHolder: EmbeddedSelectionHolder,
    private val rowSelectionImmediateActionHandler: EmbeddedRowSelectionImmediateActionHandler,
    private val customerStateHolder: CustomerStateHolder,
    private val sheetStateHolder: SheetStateHolder,
    private val errorReporter: ErrorReporter,
    @Named(STATUS_BAR_COLOR) private val statusBarColor: Int?,
    @PaymentElementCallbackIdentifier private val paymentElementCallbackIdentifier: String,
) : EmbeddedSheetLauncher {

    init {
        lifecycleOwner.lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onDestroy(owner: LifecycleOwner) {
                    activityLauncher.unregister()
                    super.onDestroy(owner)
                }
            }
        )
    }

    private val activityLauncher: ActivityResultLauncher<EmbeddedActivityArgs> =
        activityResultCaller.registerForActivityResult(EmbeddedSheetContract) { result ->
            sheetStateHolder.sheetIsOpen = false
            when (result.launchMode) {
                is EmbeddedLaunchMode.Form -> {
                    selectionHolder.setTemporarySelection(null)
                    handleFormResult(result)
                }
                is EmbeddedLaunchMode.Manage -> handleManageResult(result)
                is EmbeddedLaunchMode.PaymentOptions -> handlePaymentOptionsResult(result)
            }
        }

    private fun handleFormResult(result: EmbeddedActivityResult) {
        when (result) {
            is EmbeddedActivityResult.Complete -> {
                result.customerState?.let { customerStateHolder.setCustomerState(it) }
                selectionHolder.setSelection(result.selection)
                if (result.hasBeenConfirmed) {
                    // TODO(confirm): surface a completed result once checkout confirmation is wired.
                } else {
                    result.selection?.let { rowSelectionImmediateActionHandler.invoke() }
                }
            }
            is EmbeddedActivityResult.Cancelled -> {
                result.customerState?.let { customerStateHolder.setCustomerState(it) }
                // TODO(confirm): surface a canceled result once checkout confirmation is wired.
            }
            is EmbeddedActivityResult.Error -> Unit
        }
    }

    private fun handleManageResult(result: EmbeddedActivityResult) {
        when (result) {
            is EmbeddedActivityResult.Complete -> {
                result.customerState?.let { customerStateHolder.setCustomerState(it) }
                selectionHolder.setSelection(result.selection)
                if (result.shouldInvokeSelectionCallback && result.selection is PaymentSelection.Saved) {
                    rowSelectionImmediateActionHandler.invoke()
                }
            }
            is EmbeddedActivityResult.Cancelled -> Unit
            is EmbeddedActivityResult.Error -> Unit
        }
    }

    private fun handlePaymentOptionsResult(result: EmbeddedActivityResult) {
        when (result) {
            is EmbeddedActivityResult.Complete -> {
                result.customerState?.let { customerStateHolder.setCustomerState(it) }
                selectionHolder.setSelection(result.selection)
                if (result.hasBeenConfirmed) {
                    // TODO(confirm): surface a completed result once checkout confirmation is wired.
                }
            }
            is EmbeddedActivityResult.Cancelled -> {
                result.customerState?.let { customerStateHolder.setCustomerState(it) }
                clearStaleSelection()
            }
            is EmbeddedActivityResult.Error -> Unit
        }
    }

    private fun clearStaleSelection() {
        val currentSelection = selectionHolder.selection.value
        if (currentSelection is PaymentSelection.Saved) {
            val paymentMethodId = currentSelection.paymentMethod.id
            val stillExists = customerStateHolder.paymentMethods.value.any { it.id == paymentMethodId }
            if (!stillExists) {
                selectionHolder.setSelection(null)
            }
        }
    }

    override fun launchForm(
        code: String,
        paymentMethodMetadata: PaymentMethodMetadata,
        embeddedConfirmationState: EmbeddedConfirmationStateHolder.State?,
        customerState: CustomerState?,
        promotion: PaymentMethodMessagePromotion?,
    ) {
        markIntegrationLaunched(paymentMethodMetadata)
        if (embeddedConfirmationState == null) {
            errorReporter.report(
                ErrorReporter.UnexpectedErrorEvent.EMBEDDED_SHEET_LAUNCHER_EMBEDDED_STATE_IS_NULL
            )
            return
        }
        if (sheetStateHolder.sheetIsOpen) return
        sheetStateHolder.sheetIsOpen = true
        selectionHolder.setTemporarySelection(code)
        val currentSelection = (selectionHolder.selection.value as? PaymentSelection.New?)
            .takeIf { it?.paymentMethodType == code }
            ?: selectionHolder.getPreviousNewSelection(code)
        val args = EmbeddedActivityArgs(
            paymentMethodMetadata = paymentMethodMetadata,
            configuration = embeddedConfirmationState.configuration,
            paymentElementCallbackIdentifier = paymentElementCallbackIdentifier,
            statusBarColor = statusBarColor,
            selection = currentSelection,
            customerState = customerState,
            promotion = promotion,
            launchMode = EmbeddedLaunchMode.Form(selectedPaymentMethodCode = code),
        )
        activityLauncher.launch(args)
    }

    override fun launchManage(
        paymentMethodMetadata: PaymentMethodMetadata,
        customerState: CustomerState,
        selection: PaymentSelection?,
        embeddedConfirmationState: EmbeddedConfirmationStateHolder.State?,
    ) {
        markIntegrationLaunched(paymentMethodMetadata)
        if (embeddedConfirmationState == null) {
            errorReporter.report(
                ErrorReporter.UnexpectedErrorEvent.EMBEDDED_SHEET_LAUNCHER_EMBEDDED_STATE_IS_NULL
            )
            return
        }
        if (sheetStateHolder.sheetIsOpen) return
        sheetStateHolder.sheetIsOpen = true
        val args = EmbeddedActivityArgs(
            paymentMethodMetadata = paymentMethodMetadata,
            configuration = embeddedConfirmationState.configuration,
            paymentElementCallbackIdentifier = paymentElementCallbackIdentifier,
            statusBarColor = statusBarColor,
            selection = selection,
            customerState = customerState,
            promotion = null,
            launchMode = EmbeddedLaunchMode.Manage,
        )
        activityLauncher.launch(args)
    }

    override fun launchPaymentOptions(
        paymentMethodMetadata: PaymentMethodMetadata,
        customerState: CustomerState?,
        selection: PaymentSelection?,
        embeddedConfirmationState: EmbeddedConfirmationStateHolder.State?,
    ) {
        markIntegrationLaunched(paymentMethodMetadata)
        if (embeddedConfirmationState == null) {
            errorReporter.report(
                ErrorReporter.UnexpectedErrorEvent.EMBEDDED_SHEET_LAUNCHER_EMBEDDED_STATE_IS_NULL
            )
            return
        }
        if (sheetStateHolder.sheetIsOpen) return
        sheetStateHolder.sheetIsOpen = true
        val args = EmbeddedActivityArgs(
            paymentMethodMetadata = paymentMethodMetadata,
            configuration = embeddedConfirmationState.configuration,
            paymentElementCallbackIdentifier = paymentElementCallbackIdentifier,
            statusBarColor = statusBarColor,
            selection = selection,
            customerState = customerState,
            promotion = null,
            launchMode = EmbeddedLaunchMode.PaymentOptions,
        )
        activityLauncher.launch(args)
    }

    private fun markIntegrationLaunched(paymentMethodMetadata: PaymentMethodMetadata) {
        val checkoutSession = paymentMethodMetadata.integrationMetadata as? IntegrationMetadata.CheckoutSession
        if (checkoutSession != null) {
            CheckoutInstances.ensureNoMutationInFlight(checkoutSession.instancesKey)
            CheckoutInstances.markIntegrationLaunched(checkoutSession.instancesKey)
        }
    }
}
