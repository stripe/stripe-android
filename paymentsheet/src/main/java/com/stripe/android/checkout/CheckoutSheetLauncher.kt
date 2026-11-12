package com.stripe.android.checkout

import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.lifecycleScope
import com.stripe.android.checkout.injection.CheckoutElementsPresenterScope
import com.stripe.android.core.Logger
import com.stripe.android.core.injection.ViewModelScope
import com.stripe.android.link.account.LinkAccountHolder
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.PaymentMethodMessagePromotion
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackIdentifier
import com.stripe.android.paymentelement.embedded.EmbeddedActivityArgs
import com.stripe.android.paymentelement.embedded.EmbeddedActivityResult
import com.stripe.android.paymentelement.embedded.EmbeddedLaunchMode
import com.stripe.android.paymentelement.embedded.EmbeddedRowSelectionImmediateActionHandler
import com.stripe.android.paymentelement.embedded.content.EmbeddedContentHelperStateHolder
import com.stripe.android.paymentelement.embedded.content.EmbeddedSheetLauncher
import com.stripe.android.paymentelement.embedded.content.SheetStateHolder
import com.stripe.android.paymentelement.embedded.linkAccountInfoOrNull
import com.stripe.android.paymentelement.embedded.sheet.EmbeddedSheetContract
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.payments.core.injection.PRODUCT_USAGE
import com.stripe.android.payments.core.injection.STATUS_BAR_COLOR
import com.stripe.android.paymentsheet.CustomerStateHolder
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.paymentMethodType
import com.stripe.android.paymentsheet.repositories.PaymentMethodMessagePromotionsHelper
import com.stripe.android.paymentsheet.state.CustomerState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
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
@CheckoutElementsPresenterScope
internal class CheckoutSheetLauncher @Inject constructor(
    activityResultCaller: ActivityResultCaller,
    private val lifecycleOwner: LifecycleOwner,
    private val selectionHolder: CheckoutElementsControllerStateHolder,
    private val customerStateHolder: CustomerStateHolder,
    private val linkAccountHolder: LinkAccountHolder,
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
            result.linkAccountInfoOrNull?.let(linkAccountHolder::set)
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
                applyCompleteResult(
                    result = result,
                    invokeImmediateAction = !result.hasBeenConfirmed && result.selection != null,
                )
            }
            is EmbeddedActivityResult.Cancelled -> applyCustomerState(result.customerState)
            is EmbeddedActivityResult.Error -> Unit
        }
    }

    private fun handleManageResult(result: EmbeddedActivityResult) {
        when (result) {
            is EmbeddedActivityResult.Complete -> {
                applyCompleteResult(
                    result = result,
                    invokeImmediateAction = result.shouldInvokeSelectionCallback &&
                        result.selection is PaymentSelection.Saved,
                )
            }
            is EmbeddedActivityResult.Cancelled -> Unit
            is EmbeddedActivityResult.Error -> Unit
        }
    }

    private fun handlePaymentOptionsResult(result: EmbeddedActivityResult) {
        when (result) {
            is EmbeddedActivityResult.Complete -> {
                (result.selection as? PaymentSelection.Saved)?.let { selection ->
                    selectionHolder.state?.mandateState?.acknowledgeSepaMandate(selection.paymentMethod)
                }
                applyCompleteResult(result = result, invokeImmediateAction = false)
            }
            is EmbeddedActivityResult.Cancelled -> {
                applyCustomerState(result.customerState)
                reconcileSavedSelection()
            }
            is EmbeddedActivityResult.Error -> Unit
        }
    }

    private fun applyCompleteResult(
        result: EmbeddedActivityResult.Complete,
        invokeImmediateAction: Boolean,
    ) {
        applyCustomerState(result.customerState)
        selectionHolder.setPreviousNewSelections(result.previousNewSelections)
        // Apply the selection after any refresh commits so the selection and immediate action see the
        // sheet's response. Undispatched so results without a response still apply synchronously.
        coroutineScope.launch(start = CoroutineStart.UNDISPATCHED) {
            result.checkoutSessionResponse?.let { response ->
                operationCoordinator.runMutation {
                    runCatching { sessionRefresher.refresh(response) }
                }.onFailure {
                    if (it is CancellationException) throw it
                    logger.error("Failed to refresh the checkout session after the sheet closed.", it)
                }
            }
            selectionHolder.setSelection(result.selection)
            if (invokeImmediateAction) rowSelectionImmediateActionHandler.invoke()
        }
    }

    private fun applyCustomerState(customerState: CustomerState?) {
        customerState?.let { customerStateHolder.setCustomerState(it) }
    }

    private fun reconcileSavedSelection() {
        val currentSelection = selectionHolder.selection.value as? PaymentSelection.Saved ?: return
        val updatedPaymentMethod = customerStateHolder.paymentMethods.value.firstOrNull {
            it.id == currentSelection.paymentMethod.id
        }
        if (updatedPaymentMethod == null) {
            selectionHolder.setSelection(null)
        } else if (updatedPaymentMethod != currentSelection.paymentMethod) {
            selectionHolder.setSelection(currentSelection.copy(paymentMethod = updatedPaymentMethod))
        }
    }

    override fun launchForm(
        code: String,
        paymentMethodMetadata: PaymentMethodMetadata,
        configuration: EmbeddedPaymentElement.Configuration?,
        customerState: CustomerState?,
        promotion: PaymentMethodMessagePromotion?,
    ) {
        val launchConfiguration = getLaunchConfiguration(configuration) ?: return
        sheetStateHolder.sheetIsOpen = true
        selectionHolder.setTemporarySelection(code)
        val currentSelection = (selectionHolder.selection.value as? PaymentSelection.New?)
            .takeIf { it?.paymentMethodType == code }
            ?: selectionHolder.getPreviousNewSelection(code)
        val args = EmbeddedActivityArgs(
            productUsage = productUsage,
            paymentElementCallbackIdentifier = paymentElementCallbackIdentifier,
            statusBarColor = statusBarColor,
            selection = currentSelection,
            previousNewSelections = selectionHolder.previousNewSelections,
            customerState = customerState,
            linkAccountInfo = linkAccountHolder.linkAccountInfo.value,
            promotions = listOfNotNull(promotion),
            launchMode = EmbeddedLaunchMode.Form(
                selectedPaymentMethodCode = code,
            ),
            presentationState = EmbeddedActivityArgs.PresentationState.Ready(
                paymentMethodMetadata = paymentMethodMetadata,
                configuration = launchConfiguration,
            ),
        )
        activityLauncher.launch(args)
    }

    override fun launchManage(
        paymentMethodMetadata: PaymentMethodMetadata,
        customerState: CustomerState,
        selection: PaymentSelection?,
        configuration: EmbeddedPaymentElement.Configuration?,
    ) {
        val launchConfiguration = getLaunchConfiguration(configuration) ?: return
        sheetStateHolder.sheetIsOpen = true
        val args = EmbeddedActivityArgs(
            productUsage = productUsage,
            paymentElementCallbackIdentifier = paymentElementCallbackIdentifier,
            statusBarColor = statusBarColor,
            selection = selection,
            previousNewSelections = selectionHolder.previousNewSelections,
            customerState = customerState,
            linkAccountInfo = linkAccountHolder.linkAccountInfo.value,
            promotions = emptyList(),
            launchMode = EmbeddedLaunchMode.Manage,
            presentationState = EmbeddedActivityArgs.PresentationState.Ready(
                paymentMethodMetadata = paymentMethodMetadata,
                configuration = launchConfiguration,
            ),
        )
        activityLauncher.launch(args)
    }

    override fun launchPaymentOptions(
        paymentMethodMetadata: PaymentMethodMetadata?,
        customerState: CustomerState?,
        selection: PaymentSelection?,
        configuration: EmbeddedPaymentElement.Configuration?,
    ) {
        if (!lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.CREATED)) return
        if (sheetStateHolder.sheetIsOpen) return
        val status = operationCoordinator.status.value
        val presentationState = if (status == CheckoutOperationCoordinator.Status.Updating) {
            EmbeddedActivityArgs.PresentationState.Loading
        } else {
            if (paymentMethodMetadata == null || configuration == null) return
            EmbeddedActivityArgs.PresentationState.Ready(
                paymentMethodMetadata = paymentMethodMetadata,
                configuration = configuration,
            )
        }
        sheetStateHolder.sheetIsOpen = true
        val initialArgs = createPaymentOptionsArgs(
            selection = selection,
            customerState = customerState,
            presentationState = presentationState,
        )
        launcherState.isAwaitingPaymentOptionsReady =
            initialArgs.presentationState == EmbeddedActivityArgs.PresentationState.Loading
        activityLauncher.launch(initialArgs)

        resumePendingReadyLaunch()
    }

    private fun getLaunchConfiguration(
        configuration: EmbeddedPaymentElement.Configuration?,
    ): EmbeddedPaymentElement.Configuration? {
        if (!lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.CREATED)) return null
        if (configuration == null) {
            errorReporter.report(
                ErrorReporter.UnexpectedErrorEvent.EMBEDDED_SHEET_LAUNCHER_EMBEDDED_STATE_IS_NULL
            )
            return null
        }
        if (sheetStateHolder.sheetIsOpen) return null
        return configuration
    }

    private fun resumePendingReadyLaunch() {
        if (!launcherState.isAwaitingPaymentOptionsReady) return

        lifecycleOwner.lifecycleScope.launch {
            operationCoordinator.status.first { it == CheckoutOperationCoordinator.Status.Idle }
            if (!sheetStateHolder.sheetIsOpen) {
                launcherState.isAwaitingPaymentOptionsReady = false
                return@launch
            }

            val refreshedState = embeddedContentState.value
            activityLauncher.launch(
                createPaymentOptionsArgs(
                    selection = selectionHolder.selection.value,
                    customerState = customerStateHolder.customer.value,
                    presentationState = if (refreshedState == null) {
                        EmbeddedActivityArgs.PresentationState.Failed
                    } else {
                        EmbeddedActivityArgs.PresentationState.Ready(
                            paymentMethodMetadata = refreshedState.paymentMethodMetadata,
                            configuration = refreshedState.configuration,
                        )
                    },
                )
            )
            launcherState.isAwaitingPaymentOptionsReady = false
        }
    }

    private fun createPaymentOptionsArgs(
        customerState: CustomerState?,
        selection: PaymentSelection?,
        presentationState: EmbeddedActivityArgs.PresentationState,
    ): EmbeddedActivityArgs {
        return EmbeddedActivityArgs(
            productUsage = productUsage,
            paymentElementCallbackIdentifier = paymentElementCallbackIdentifier,
            statusBarColor = statusBarColor,
            selection = selection,
            previousNewSelections = selectionHolder.previousNewSelections,
            customerState = customerState,
            linkAccountInfo = linkAccountHolder.linkAccountInfo.value,
            promotions = paymentMethodMessagePromotionsHelper.getPromotions().orEmpty(),
            launchMode = EmbeddedLaunchMode.PaymentOptions,
            presentationState = presentationState,
        )
    }
}
