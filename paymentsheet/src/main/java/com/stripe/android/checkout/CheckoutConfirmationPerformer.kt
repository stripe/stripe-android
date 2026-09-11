package com.stripe.android.checkout

import androidx.activity.result.ActivityResultCaller
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.stripe.android.core.injection.ViewModelScope
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.gpay.GooglePayBillingEmailOverrideProvider
import com.stripe.android.paymentelement.confirmation.toConfirmationOption
import com.stripe.android.payments.core.injection.STATUS_BAR_COLOR
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.ui.SepaMandateContract
import com.stripe.android.paymentsheet.ui.SepaMandateResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Named

@OptIn(CheckoutSessionPreview::class)
internal class CheckoutConfirmationPerformer @Inject constructor(
    private val confirmationHandler: ConfirmationHandler,
    private val stateHolder: CheckoutControllerStateHolder,
    private val operationCoordinator: CheckoutOperationCoordinator,
    private val analyticsPerformer: CheckoutAnalyticsPerformer,
    private val commonConfigurationFactory: CheckoutCommonConfigurationFactory,
    activityResultCaller: ActivityResultCaller,
    lifecycleOwner: LifecycleOwner,
    private val resultCallback: CheckoutController.ResultCallback,
    @Named(STATUS_BAR_COLOR) private val statusBarColor: Int?,
    @ViewModelScope private val viewModelScope: CoroutineScope,
) {
    private val sepaMandateActivityLauncher = activityResultCaller.registerForActivityResult(
        SepaMandateContract(),
        ::onSepaMandateResult,
    )

    init {
        lifecycleOwner.lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onDestroy(owner: LifecycleOwner) {
                    sepaMandateActivityLauncher.unregister()
                }
            }
        )
    }

    fun confirm() {
        val state = stateHolder.state ?: return
        val paymentSelection = state.paymentSelection ?: return

        if (paymentSelection is PaymentSelection.Saved &&
            paymentSelection.paymentMethod.type == PaymentMethod.Type.SepaDebit &&
            !paymentSelection.hasAcknowledgedSepaMandate &&
            state.embeddedConfiguration.embeddedViewDisplaysMandateText
        ) {
            val configuration = commonConfigurationFactory.createForPaymentElement(
                configuration = state.configuration,
                checkoutSessionResponse = state.checkoutSessionResponse,
                collectedDetails = state.collectedDetails,
            )
            sepaMandateActivityLauncher.launch(
                SepaMandateContract.Args(
                    merchantName = configuration.merchantDisplayName,
                    appearance = configuration.appearance,
                )
            )
            return
        }

        val arguments = operationCoordinator.tryBeginConfirmation {
            confirmationArgs(
                state = state,
                paymentSelection = paymentSelection,
            )
        } ?: return
        analyticsPerformer.onPaymentElementConfirmationStarted(paymentSelection)
        viewModelScope.launch {
            try {
                confirmationHandler.start(arguments)
            } catch (error: CancellationException) {
                throw error
            } catch (@Suppress("TooGenericExceptionCaught") error: Exception) {
                operationCoordinator.failConfirmation(error)
            }
        }
    }

    private fun onSepaMandateResult(result: SepaMandateResult) {
        when (result) {
            SepaMandateResult.Acknowledged -> {
                stateHolder.state?.paymentSelection?.hasAcknowledgedSepaMandate = true
                confirm()
            }
            SepaMandateResult.Canceled -> {
                resultCallback.onResult(CheckoutController.Result.Canceled())
            }
        }
    }

    private fun confirmationArgs(
        state: CheckoutControllerState,
        paymentSelection: PaymentSelection,
    ): ConfirmationHandler.Args? {
        val configuration = commonConfigurationFactory.createForPaymentElement(
            configuration = state.configuration,
            checkoutSessionResponse = state.checkoutSessionResponse,
            collectedDetails = state.collectedDetails,
        )
        val confirmationOption = paymentSelection.toConfirmationOption(
            configuration = configuration,
            linkConfiguration = state.paymentMethodMetadata.linkState?.configuration,
            cardFundingFilter = state.paymentMethodMetadata.cardFundingFilter,
            googlePayBillingEmailOverride = GooglePayBillingEmailOverrideProvider.get(
                configuration = configuration,
                paymentMethodMetadata = state.paymentMethodMetadata,
            ),
        ) ?: return null

        return ConfirmationHandler.Args(
            confirmationOption = confirmationOption,
            paymentMethodMetadata = state.paymentMethodMetadata,
            statusBarColor = statusBarColor,
        )
    }
}
