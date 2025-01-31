package com.stripe.android.paymentelement.embedded.content

import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.ExperimentalEmbeddedPaymentElementApi
import com.stripe.android.paymentelement.embedded.EmbeddedSelectionHolder
import com.stripe.android.paymentelement.embedded.form.FormContract
import com.stripe.android.paymentelement.embedded.form.FormResult
import com.stripe.android.paymentelement.embedded.manage.ManageContract
import com.stripe.android.paymentelement.embedded.manage.ManageResult
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.paymentsheet.CustomerStateHolder
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.state.CustomerState
import javax.inject.Inject

@OptIn(ExperimentalEmbeddedPaymentElementApi::class)
internal interface EmbeddedSheetLauncher {
    fun launchForm(
        code: String,
        paymentMethodMetadata: PaymentMethodMetadata,
        hasSavedPaymentMethods: Boolean,
        configuration: EmbeddedPaymentElement.Configuration?
    )

    fun launchManage(
        paymentMethodMetadata: PaymentMethodMetadata,
        customerState: CustomerState,
        selection: PaymentSelection?,
    )
}

@OptIn(ExperimentalEmbeddedPaymentElementApi::class)
@EmbeddedPaymentElementScope
internal class DefaultEmbeddedSheetLauncher @Inject constructor(
    activityResultCaller: ActivityResultCaller,
    lifecycleOwner: LifecycleOwner,
    private val selectionHolder: EmbeddedSelectionHolder,
    private val customerStateHolder: CustomerStateHolder,
    private val sheetStateHolder: SheetStateHolder,
    private val errorReporter: ErrorReporter,
) : EmbeddedSheetLauncher {

    init {
        lifecycleOwner.lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onDestroy(owner: LifecycleOwner) {
                    formActivityLauncher.unregister()
                    manageActivityLauncher.unregister()
                    super.onDestroy(owner)
                }
            }
        )
    }

    private val formActivityLauncher: ActivityResultLauncher<FormContract.Args> =
        activityResultCaller.registerForActivityResult(FormContract) { result ->
            sheetStateHolder.sheetIsOpen = false
            if (result is FormResult.Complete) {
                selectionHolder.set(result.selection)
            }
        }

    private val manageActivityLauncher: ActivityResultLauncher<ManageContract.Args> =
        activityResultCaller.registerForActivityResult(ManageContract) { result ->
            sheetStateHolder.sheetIsOpen = false
            when (result) {
                is ManageResult.Cancelled -> {
                    if (result.customerState != null) {
                        customerStateHolder.setCustomerState(result.customerState)
                    }
                }
                is ManageResult.Complete -> {
                    customerStateHolder.setCustomerState(result.customerState)
                    selectionHolder.set(result.selection)
                }
            }
        }

    override fun launchForm(
        code: String,
        paymentMethodMetadata: PaymentMethodMetadata,
        hasSavedPaymentMethods: Boolean,
        configuration: EmbeddedPaymentElement.Configuration?
    ) {
        if (configuration == null) {
            errorReporter.report(
                ErrorReporter.UnexpectedErrorEvent.EMBEDDED_SHEET_LAUNCHER_CONFIRMATION_CONFIGURATION_IS_NULL
            )
            return
        }
        sheetStateHolder.sheetIsOpen = true
        val args = FormContract.Args(
            selectedPaymentMethodCode = code,
            paymentMethodMetadata = paymentMethodMetadata,
            hasSavedPaymentMethods = hasSavedPaymentMethods,
            configuration = configuration
        )
        formActivityLauncher.launch(args)
    }

    override fun launchManage(
        paymentMethodMetadata: PaymentMethodMetadata,
        customerState: CustomerState,
        selection: PaymentSelection?,
    ) {
        sheetStateHolder.sheetIsOpen = true
        val args = ManageContract.Args(
            paymentMethodMetadata = paymentMethodMetadata,
            customerState = customerState,
            selection = selection,
        )
        manageActivityLauncher.launch(args)
    }
}
