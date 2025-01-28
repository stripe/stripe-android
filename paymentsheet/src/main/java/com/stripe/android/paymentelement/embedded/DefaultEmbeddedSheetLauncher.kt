package com.stripe.android.paymentelement.embedded

import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.paymentelement.embedded.form.FormContract
import com.stripe.android.paymentelement.embedded.form.FormResult
import com.stripe.android.paymentelement.embedded.manage.ManageContract
import com.stripe.android.paymentelement.embedded.manage.ManageResult
import com.stripe.android.paymentsheet.CustomerStateHolder
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.state.CustomerState
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal interface EmbeddedSheetLauncher {
    fun launchForm(
        code: String,
        paymentMethodMetadata: PaymentMethodMetadata,
        hasSavedPaymentMethods: Boolean
    )

    fun launchManage(
        paymentMethodMetadata: PaymentMethodMetadata,
        customerState: CustomerState,
        selection: PaymentSelection?,
    )
}

internal fun interface EmbeddedSheetLauncherFactory {
    fun create(
        activityResultCaller: ActivityResultCaller,
        lifecycleOwner: LifecycleOwner,
    ): EmbeddedSheetLauncher
}

@AssistedFactory
internal interface DefaultEmbeddedSheetLauncherFactory : EmbeddedSheetLauncherFactory {
    override fun create(
        activityResultCaller: ActivityResultCaller,
        lifecycleOwner: LifecycleOwner,
    ): DefaultEmbeddedSheetLauncher
}

internal class DefaultEmbeddedSheetLauncher @AssistedInject constructor(
    @Assisted activityResultCaller: ActivityResultCaller,
    @Assisted lifecycleOwner: LifecycleOwner,
    private val selectionHolder: EmbeddedSelectionHolder,
    private val customerStateHolder: CustomerStateHolder,
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
            if (result is FormResult.Complete) {
                selectionHolder.set(result.selection)
            }
        }

    private val manageActivityLauncher: ActivityResultLauncher<ManageContract.Args> =
        activityResultCaller.registerForActivityResult(ManageContract) { result ->
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
        hasSavedPaymentMethods: Boolean
    ) {
        val args = FormContract.Args(
            selectedPaymentMethodCode = code,
            paymentMethodMetadata = paymentMethodMetadata,
            hasSavedPaymentMethods = hasSavedPaymentMethods
        )
        formActivityLauncher.launch(args)
    }

    override fun launchManage(
        paymentMethodMetadata: PaymentMethodMetadata,
        customerState: CustomerState,
        selection: PaymentSelection?,
    ) {
        val args = ManageContract.Args(
            paymentMethodMetadata = paymentMethodMetadata,
            customerState = customerState,
            selection = selection,
        )
        manageActivityLauncher.launch(args)
    }
}
