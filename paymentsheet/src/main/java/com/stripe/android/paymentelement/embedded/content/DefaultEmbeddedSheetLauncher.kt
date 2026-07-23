package com.stripe.android.paymentelement.embedded.content

import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.PaymentMethodMessagePromotion
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackIdentifier
import com.stripe.android.paymentelement.embedded.EmbeddedActivityArgs
import com.stripe.android.paymentelement.embedded.EmbeddedActivityResult
import com.stripe.android.paymentelement.embedded.EmbeddedLaunchMode
import com.stripe.android.paymentelement.embedded.EmbeddedResultCallbackHelper
import com.stripe.android.paymentelement.embedded.EmbeddedRowSelectionImmediateActionHandler
import com.stripe.android.paymentelement.embedded.EmbeddedSelectionHolder
import com.stripe.android.paymentelement.embedded.sheet.EmbeddedSheetContract
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.payments.core.injection.STATUS_BAR_COLOR
import com.stripe.android.paymentsheet.CustomerStateHolder
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.paymentMethodType
import com.stripe.android.paymentsheet.state.CustomerState
import javax.inject.Inject
import javax.inject.Named

internal interface EmbeddedSheetLauncher {
    fun launchForm(
        code: String,
        paymentMethodMetadata: PaymentMethodMetadata,
        configuration: EmbeddedPaymentElement.Configuration?,
        customerState: CustomerState?,
        promotion: PaymentMethodMessagePromotion?,
    )

    fun launchManage(
        paymentMethodMetadata: PaymentMethodMetadata,
        customerState: CustomerState,
        selection: PaymentSelection?,
        configuration: EmbeddedPaymentElement.Configuration?,
    )

    fun launchPaymentOptions(
        paymentMethodMetadata: PaymentMethodMetadata,
        customerState: CustomerState?,
        selection: PaymentSelection?,
        configuration: EmbeddedPaymentElement.Configuration?,
    )
}

@EmbeddedPaymentElementScope
internal class DefaultEmbeddedSheetLauncher @Inject constructor(
    activityResultCaller: ActivityResultCaller,
    lifecycleOwner: LifecycleOwner,
    private val selectionHolder: EmbeddedSelectionHolder,
    private val rowSelectionImmediateActionHandler: EmbeddedRowSelectionImmediateActionHandler,
    private val customerStateHolder: CustomerStateHolder,
    private val sheetStateHolder: SheetStateHolder,
    private val errorReporter: ErrorReporter,
    @Named(STATUS_BAR_COLOR) private val statusBarColor: Int?,
    @PaymentElementCallbackIdentifier private val paymentElementCallbackIdentifier: String,
    private val embeddedResultCallbackHelper: EmbeddedResultCallbackHelper,
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
                    embeddedResultCallbackHelper.setResult(
                        EmbeddedPaymentElement.Result.Completed()
                    )
                } else {
                    result.selection?.let { rowSelectionImmediateActionHandler.invoke() }
                }
            }
            is EmbeddedActivityResult.Cancelled -> {
                result.customerState?.let { customerStateHolder.setCustomerState(it) }
                embeddedResultCallbackHelper.setResult(
                    EmbeddedPaymentElement.Result.Canceled()
                )
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
                    embeddedResultCallbackHelper.setResult(
                        EmbeddedPaymentElement.Result.Completed()
                    )
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
        configuration: EmbeddedPaymentElement.Configuration?,
        customerState: CustomerState?,
        promotion: PaymentMethodMessagePromotion?,
    ) {
        if (configuration == null) {
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
            configuration = configuration,
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
        configuration: EmbeddedPaymentElement.Configuration?,
    ) {
        if (configuration == null) {
            errorReporter.report(
                ErrorReporter.UnexpectedErrorEvent.EMBEDDED_SHEET_LAUNCHER_EMBEDDED_STATE_IS_NULL
            )
            return
        }
        if (sheetStateHolder.sheetIsOpen) return
        sheetStateHolder.sheetIsOpen = true
        val args = EmbeddedActivityArgs(
            paymentMethodMetadata = paymentMethodMetadata,
            configuration = configuration,
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
        configuration: EmbeddedPaymentElement.Configuration?,
    ) {
        if (configuration == null) {
            errorReporter.report(
                ErrorReporter.UnexpectedErrorEvent.EMBEDDED_SHEET_LAUNCHER_EMBEDDED_STATE_IS_NULL
            )
            return
        }
        if (sheetStateHolder.sheetIsOpen) return
        sheetStateHolder.sheetIsOpen = true
        val args = EmbeddedActivityArgs(
            paymentMethodMetadata = paymentMethodMetadata,
            configuration = configuration,
            paymentElementCallbackIdentifier = paymentElementCallbackIdentifier,
            statusBarColor = statusBarColor,
            selection = selection,
            customerState = customerState,
            promotion = null,
            launchMode = EmbeddedLaunchMode.PaymentOptions,
        )
        activityLauncher.launch(args)
    }
}
