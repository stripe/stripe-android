package com.stripe.android.checkout

import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.lifecycleScope
import com.stripe.android.core.Logger
import com.stripe.android.core.injection.ViewModelScope
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.PaymentMethodMessagePromotion
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackIdentifier
import com.stripe.android.paymentelement.embedded.EmbeddedActivityArgs
import com.stripe.android.paymentelement.embedded.EmbeddedActivityResult
import com.stripe.android.paymentelement.embedded.EmbeddedLaunchMode
import com.stripe.android.paymentelement.embedded.EmbeddedRowSelectionImmediateActionHandler
import com.stripe.android.paymentelement.embedded.EmbeddedSelectionHolder
import com.stripe.android.paymentelement.embedded.content.EmbeddedContentHelperStateHolder
import com.stripe.android.paymentelement.embedded.content.EmbeddedSheetLauncher
import com.stripe.android.paymentelement.embedded.content.SheetStateHolder
import com.stripe.android.paymentelement.embedded.sheet.EmbeddedSheetContract
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.payments.core.injection.PRODUCT_USAGE
import com.stripe.android.payments.core.injection.STATUS_BAR_COLOR
import com.stripe.android.paymentsheet.CustomerStateHolder
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.paymentMethodType
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import com.stripe.android.paymentsheet.repositories.PaymentMethodMessagePromotionsHelper
import com.stripe.android.paymentsheet.state.CustomerState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
internal class CheckoutSheetLauncherState @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
) {
    var isAwaitingPaymentOptionsReady: Boolean
        get() = savedStateHandle.get<Boolean>(AWAITING_PAYMENT_OPTIONS_READY_KEY) == true
        set(value) {
            savedStateHandle[AWAITING_PAYMENT_OPTIONS_READY_KEY] = value
        }

    private companion object {
        const val AWAITING_PAYMENT_OPTIONS_READY_KEY =
            "CheckoutSheetLauncherState_AWAITING_PAYMENT_OPTIONS_READY"
    }
}

@OptIn(CheckoutSessionPreview::class)
internal class CheckoutSheetLauncher @Inject constructor(
    activityResultCaller: ActivityResultCaller,
    private val lifecycleOwner: LifecycleOwner,
    private val selectionHolder: EmbeddedSelectionHolder,
    private val customerStateHolder: CustomerStateHolder,
    private val sheetStateHolder: SheetStateHolder,
    private val errorReporter: ErrorReporter,
    private val sessionRefresher: CheckoutSessionRefresher,
    private val operationCoordinator: CheckoutOperationCoordinator,
    private val launcherState: CheckoutSheetLauncherState,
    private val embeddedContentState: StateFlow<EmbeddedContentHelperStateHolder.State?>,
    private val logger: Logger,
    @ViewModelScope private val coroutineScope: CoroutineScope,
    @Named(PRODUCT_USAGE) private val productUsage: Set<String>,
    @Named(STATUS_BAR_COLOR) private val statusBarColor: Int?,
    @PaymentElementCallbackIdentifier private val paymentElementCallbackIdentifier: String,
    private val rowSelectionImmediateActionHandler: EmbeddedRowSelectionImmediateActionHandler,
    private val paymentMethodMessagePromotionsHelper: PaymentMethodMessagePromotionsHelper,
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
            launcherState.isAwaitingPaymentOptionsReady = false
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

    init {
        resumePendingReadyLaunch()
    }

    private fun handleFormResult(result: EmbeddedActivityResult) {
        when (result) {
            is EmbeddedActivityResult.Complete -> {
                applyCompleteResult(result)
                if (!result.hasBeenConfirmed) {
                    result.selection?.let { rowSelectionImmediateActionHandler.invoke() }
                }
                refreshCheckoutSession(result.checkoutSessionResponse)
            }
            is EmbeddedActivityResult.Cancelled -> applyCustomerState(result.customerState)
            is EmbeddedActivityResult.Error -> Unit
        }
    }

    private fun handleManageResult(result: EmbeddedActivityResult) {
        when (result) {
            is EmbeddedActivityResult.Complete -> {
                applyCompleteResult(result)
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
                applyCompleteResult(result)
                refreshCheckoutSession(result.checkoutSessionResponse)
            }
            is EmbeddedActivityResult.Cancelled -> {
                applyCustomerState(result.customerState)
                clearStaleSelection()
            }
            is EmbeddedActivityResult.Error -> Unit
        }
    }

    private fun applyCompleteResult(result: EmbeddedActivityResult.Complete) {
        applyCustomerState(result.customerState)
        selectionHolder.setPreviousNewSelections(result.previousNewSelections)
        selectionHolder.setSelection(result.selection)
    }

    private fun refreshCheckoutSession(response: CheckoutSessionResponse?) {
        response ?: return
        coroutineScope.launch {
            operationCoordinator.runMutation {
                runCatching { sessionRefresher.refresh(response) }
            }.onFailure {
                logger.error("Failed to refresh the checkout session after the sheet closed.", it)
            }
        }
    }

    private fun applyCustomerState(customerState: CustomerState?) {
        customerState?.let { customerStateHolder.setCustomerState(it) }
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
                ErrorReporter.UnexpectedErrorEvent.EMBEDDED_SHEET_LAUNCHER_EMBEDDED_STATE_IS_NULL,
                additionalNonPiiParams = mapOf(
                    "launcher" to "checkout_sheet",
                    "operation" to "launch_form",
                    "payment_method_code" to code,
                ),
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
            productUsage = productUsage,
            paymentElementCallbackIdentifier = paymentElementCallbackIdentifier,
            statusBarColor = statusBarColor,
            selection = currentSelection,
            previousNewSelections = selectionHolder.previousNewSelections,
            customerState = customerState,
            promotions = listOfNotNull(promotion),
            launchMode = EmbeddedLaunchMode.Form(
                selectedPaymentMethodCode = code,
            ),
            presentationState = EmbeddedActivityArgs.PresentationState.Ready,
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
                ErrorReporter.UnexpectedErrorEvent.EMBEDDED_SHEET_LAUNCHER_EMBEDDED_STATE_IS_NULL,
                additionalNonPiiParams = mapOf(
                    "launcher" to "checkout_sheet",
                    "operation" to "launch_manage",
                    "selection_type" to (selection?.javaClass?.name ?: "null"),
                ),
            )
            return
        }
        if (sheetStateHolder.sheetIsOpen) return
        sheetStateHolder.sheetIsOpen = true
        val args = EmbeddedActivityArgs(
            paymentMethodMetadata = paymentMethodMetadata,
            configuration = configuration,
            productUsage = productUsage,
            paymentElementCallbackIdentifier = paymentElementCallbackIdentifier,
            statusBarColor = statusBarColor,
            selection = selection,
            previousNewSelections = selectionHolder.previousNewSelections,
            customerState = customerState,
            promotions = emptyList(),
            launchMode = EmbeddedLaunchMode.Manage,
            presentationState = EmbeddedActivityArgs.PresentationState.Ready,
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
                ErrorReporter.UnexpectedErrorEvent.EMBEDDED_SHEET_LAUNCHER_EMBEDDED_STATE_IS_NULL,
                additionalNonPiiParams = mapOf(
                    "launcher" to "checkout_sheet",
                    "operation" to "launch_payment_options",
                    "selection_type" to (selection?.javaClass?.name ?: "null"),
                ),
            )
            return
        }
        if (sheetStateHolder.sheetIsOpen) return
        sheetStateHolder.sheetIsOpen = true
        val initialArgs = createPaymentOptionsArgs(
            paymentMethodMetadata = paymentMethodMetadata,
            configuration = configuration,
            selection = selection,
            customerState = customerState,
            presentationState = if (operationCoordinator.isUpdating.value) {
                EmbeddedActivityArgs.PresentationState.Loading
            } else {
                EmbeddedActivityArgs.PresentationState.Ready
            },
        )
        launcherState.isAwaitingPaymentOptionsReady =
            initialArgs.presentationState == EmbeddedActivityArgs.PresentationState.Loading
        activityLauncher.launch(initialArgs)

        resumePendingReadyLaunch()
    }

    private fun resumePendingReadyLaunch() {
        if (!launcherState.isAwaitingPaymentOptionsReady) return

        lifecycleOwner.lifecycleScope.launch {
            operationCoordinator.isUpdating.first { isUpdating -> !isUpdating }
            if (!sheetStateHolder.sheetIsOpen) {
                launcherState.isAwaitingPaymentOptionsReady = false
                return@launch
            }

            val refreshedState = embeddedContentState.value
            if (refreshedState == null) {
                errorReporter.report(
                    ErrorReporter.UnexpectedErrorEvent.EMBEDDED_SHEET_LAUNCHER_EMBEDDED_STATE_IS_NULL,
                    additionalNonPiiParams = mapOf(
                        "launcher" to "checkout_sheet",
                        "operation" to "resume_pending_ready_launch",
                    ),
                )
                return@launch
            }
            activityLauncher.launch(
                createPaymentOptionsArgs(
                    paymentMethodMetadata = refreshedState.paymentMethodMetadata,
                    configuration = refreshedState.configuration,
                    selection = selectionHolder.selection.value,
                    customerState = customerStateHolder.customer.value,
                    presentationState = EmbeddedActivityArgs.PresentationState.Ready,
                )
            )
            launcherState.isAwaitingPaymentOptionsReady = false
        }
    }

    private fun createPaymentOptionsArgs(
        paymentMethodMetadata: PaymentMethodMetadata,
        customerState: CustomerState?,
        selection: PaymentSelection?,
        configuration: EmbeddedPaymentElement.Configuration,
        presentationState: EmbeddedActivityArgs.PresentationState,
    ): EmbeddedActivityArgs {
        return EmbeddedActivityArgs(
            paymentMethodMetadata = paymentMethodMetadata,
            configuration = configuration,
            productUsage = productUsage,
            paymentElementCallbackIdentifier = paymentElementCallbackIdentifier,
            statusBarColor = statusBarColor,
            selection = selection,
            previousNewSelections = selectionHolder.previousNewSelections,
            customerState = customerState,
            promotions = paymentMethodMessagePromotionsHelper.getPromotions().orEmpty(),
            launchMode = EmbeddedLaunchMode.PaymentOptions,
            presentationState = presentationState,
        )
    }
}
