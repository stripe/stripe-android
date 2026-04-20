package com.stripe.android.paymentelement.embedded.content

import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.stripe.android.checkout.CheckoutInstances
import com.stripe.android.lpmfoundations.paymentmethod.IntegrationMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.PaymentMethodMessagePromotion
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackIdentifier
import com.stripe.android.paymentelement.embedded.EmbeddedResultCallbackHelper
import com.stripe.android.paymentelement.embedded.EmbeddedRowSelectionImmediateActionHandler
import com.stripe.android.paymentelement.embedded.EmbeddedSelectionHolder
import com.stripe.android.paymentelement.embedded.sheet.EmbeddedSheetContract
import com.stripe.android.paymentelement.embedded.sheet.EmbeddedSheetResult
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
        hasSavedPaymentMethods: Boolean,
        embeddedConfirmationState: EmbeddedConfirmationStateHolder.State?,
        customerState: CustomerState?,
        promotion: PaymentMethodMessagePromotion?,
    )

    fun launchManage(
        paymentMethodMetadata: PaymentMethodMetadata,
        customerState: CustomerState,
        selection: PaymentSelection?,
        embeddedConfirmationState: EmbeddedConfirmationStateHolder.State?,
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
    embeddedResultCallbackHelper: EmbeddedResultCallbackHelper,
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

    private var currentMode: EmbeddedSheetContract.Mode? = null

    private val activityLauncher: ActivityResultLauncher<EmbeddedSheetContract.Args> =
        activityResultCaller.registerForActivityResult(EmbeddedSheetContract) { result ->
            sheetStateHolder.sheetIsOpen = false
            val mode = currentMode
            currentMode = null

            if (mode == EmbeddedSheetContract.Mode.Form) {
                selectionHolder.setTemporary(null)
            }

            when (result) {
                is EmbeddedSheetResult.Complete -> {
                    result.customerState?.let { customerStateHolder.setCustomerState(it) }
                    selectionHolder.set(result.selection)

                    when {
                        result.hasBeenConfirmed -> {
                            embeddedResultCallbackHelper.setResult(
                                EmbeddedPaymentElement.Result.Completed()
                            )
                        }
                        result.shouldInvokeSelectionCallback &&
                            result.selection is PaymentSelection.Saved -> {
                            rowSelectionImmediateActionHandler.invoke()
                        }
                        mode == EmbeddedSheetContract.Mode.Form && result.selection != null -> {
                            rowSelectionImmediateActionHandler.invoke()
                        }
                    }
                }
                is EmbeddedSheetResult.Cancelled -> {
                    result.customerState?.let { customerStateHolder.setCustomerState(it) }
                    embeddedResultCallbackHelper.setResult(
                        EmbeddedPaymentElement.Result.Canceled()
                    )
                }
                is EmbeddedSheetResult.Error -> Unit
            }
        }

    override fun launchForm(
        code: String,
        paymentMethodMetadata: PaymentMethodMetadata,
        hasSavedPaymentMethods: Boolean,
        embeddedConfirmationState: EmbeddedConfirmationStateHolder.State?,
        customerState: CustomerState?,
        promotion: PaymentMethodMessagePromotion?,
    ) {
        val checkoutSession = paymentMethodMetadata.integrationMetadata as? IntegrationMetadata.CheckoutSession
        if (checkoutSession != null) {
            CheckoutInstances.ensureNoMutationInFlight(checkoutSession.instancesKey)
            CheckoutInstances.markIntegrationLaunched(checkoutSession.instancesKey)
        }
        if (embeddedConfirmationState == null) {
            errorReporter.report(
                ErrorReporter.UnexpectedErrorEvent.EMBEDDED_SHEET_LAUNCHER_EMBEDDED_STATE_IS_NULL
            )
            return
        }
        if (sheetStateHolder.sheetIsOpen) return
        sheetStateHolder.sheetIsOpen = true
        selectionHolder.setTemporary(code)
        val currentSelection = (selectionHolder.selection.value as? PaymentSelection.New?)
            .takeIf { it?.paymentMethodType == code }
            ?: selectionHolder.getPreviousNewSelection(code)
        currentMode = EmbeddedSheetContract.Mode.Form
        val args = EmbeddedSheetContract.Args(
            mode = EmbeddedSheetContract.Mode.Form,
            selectedPaymentMethodCode = code,
            paymentMethodMetadata = paymentMethodMetadata,
            hasSavedPaymentMethods = hasSavedPaymentMethods,
            configuration = embeddedConfirmationState.configuration,
            paymentElementCallbackIdentifier = paymentElementCallbackIdentifier,
            statusBarColor = statusBarColor,
            selection = currentSelection,
            customerState = customerState,
            promotion = promotion,
        )
        activityLauncher.launch(args)
    }

    override fun launchManage(
        paymentMethodMetadata: PaymentMethodMetadata,
        customerState: CustomerState,
        selection: PaymentSelection?,
        embeddedConfirmationState: EmbeddedConfirmationStateHolder.State?,
    ) {
        val checkoutSession = paymentMethodMetadata.integrationMetadata as? IntegrationMetadata.CheckoutSession
        if (checkoutSession != null) {
            CheckoutInstances.ensureNoMutationInFlight(checkoutSession.instancesKey)
            CheckoutInstances.markIntegrationLaunched(checkoutSession.instancesKey)
        }
        if (embeddedConfirmationState == null) {
            errorReporter.report(
                ErrorReporter.UnexpectedErrorEvent.EMBEDDED_SHEET_LAUNCHER_EMBEDDED_STATE_IS_NULL
            )
            return
        }
        if (sheetStateHolder.sheetIsOpen) return
        sheetStateHolder.sheetIsOpen = true
        currentMode = EmbeddedSheetContract.Mode.Manage
        val args = EmbeddedSheetContract.Args(
            mode = EmbeddedSheetContract.Mode.Manage,
            selectedPaymentMethodCode = selection?.paymentMethodType ?: "",
            paymentMethodMetadata = paymentMethodMetadata,
            hasSavedPaymentMethods = customerState.paymentMethods.isNotEmpty(),
            configuration = embeddedConfirmationState.configuration,
            paymentElementCallbackIdentifier = paymentElementCallbackIdentifier,
            statusBarColor = statusBarColor,
            selection = selection,
            customerState = customerState,
            promotion = null,
        )
        activityLauncher.launch(args)
    }
}
