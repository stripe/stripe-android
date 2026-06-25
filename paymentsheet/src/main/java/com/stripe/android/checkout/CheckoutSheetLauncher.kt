package com.stripe.android.checkout

import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.PaymentMethodMessagePromotion
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentelement.embedded.content.EmbeddedConfirmationStateHolder
import com.stripe.android.paymentelement.embedded.content.EmbeddedSheetLauncher
import com.stripe.android.paymentelement.embedded.form.FormContract
import com.stripe.android.paymentelement.embedded.form.FormResult
import com.stripe.android.paymentelement.embedded.sheet.EmbeddedSheetContract
import com.stripe.android.paymentelement.embedded.sheet.EmbeddedSheetResult
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.paymentMethodType
import com.stripe.android.paymentsheet.state.CustomerState

@OptIn(CheckoutSessionPreview::class)
internal class CheckoutSheetLauncher(
    activityResultCaller: ActivityResultCaller,
    lifecycleOwner: LifecycleOwner,
    private val controller: CheckoutController,
) : EmbeddedSheetLauncher {

    init {
        lifecycleOwner.lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onDestroy(owner: LifecycleOwner) {
                    formActivityLauncher.unregister()
                    sheetActivityLauncher.unregister()
                }
            }
        )
    }

    private val formActivityLauncher: ActivityResultLauncher<FormContract.Args> =
        activityResultCaller.registerForActivityResult(FormContract) { result ->
            result.customerState?.let { controller.customerStateHolder.setCustomerState(it) }

            if (result is FormResult.Complete) {
                controller.updateSelection(result.selection)
            }
        }

    private val sheetActivityLauncher: ActivityResultLauncher<EmbeddedSheetContract.Args> =
        activityResultCaller.registerForActivityResult(EmbeddedSheetContract) { result ->
            when (result) {
                is EmbeddedSheetResult.Error -> Unit
                is EmbeddedSheetResult.Cancelled -> Unit
                is EmbeddedSheetResult.Complete -> {
                    result.customerState?.let { controller.customerStateHolder.setCustomerState(it) }
                    controller.updateSelection(result.selection)
                }
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
        val configuration = controller.embeddedConfiguration ?: return
        val args = FormContract.Args(
            selectedPaymentMethodCode = code,
            paymentMethodMetadata = paymentMethodMetadata,
            hasSavedPaymentMethods = hasSavedPaymentMethods,
            configuration = configuration,
            paymentElementCallbackIdentifier = "CheckoutController",
            statusBarColor = null,
            paymentSelection = controller.selectionFlow.value,
            customerState = customerState,
            promotion = promotion,
        )
        formActivityLauncher.launch(args)
    }

    override fun launchManage(
        paymentMethodMetadata: PaymentMethodMetadata,
        customerState: CustomerState,
        selection: PaymentSelection?,
        embeddedConfirmationState: EmbeddedConfirmationStateHolder.State?,
    ) {
        val configuration = controller.embeddedConfiguration ?: return
        val args = EmbeddedSheetContract.Args(
            selectedPaymentMethodCode = selection?.paymentMethodType ?: "",
            paymentMethodMetadata = paymentMethodMetadata,
            hasSavedPaymentMethods = customerState.paymentMethods.isNotEmpty(),
            configuration = configuration,
            paymentElementCallbackIdentifier = "CheckoutController",
            statusBarColor = null,
            selection = selection,
            customerState = customerState,
            promotion = null,
        )
        sheetActivityLauncher.launch(args)
    }
}
