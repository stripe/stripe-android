package com.stripe.android.paymentelement.embedded.sheet

import com.stripe.android.common.model.asCommonConfiguration
import com.stripe.android.core.injection.ViewModelScope
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.gpay.GooglePayBillingEmailOverrideProvider
import com.stripe.android.paymentelement.confirmation.toConfirmationOption
import com.stripe.android.paymentelement.embedded.EmbeddedSelectionHolder
import com.stripe.android.paymentelement.embedded.form.OnClickOverrideDelegate
import com.stripe.android.payments.core.injection.STATUS_BAR_COLOR
import com.stripe.android.paymentsheet.analytics.EventReporter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Named

internal interface SheetActivityConfirmationHelper {
    fun confirm()
}

internal class DefaultSheetActivityConfirmationHelper @Inject constructor(
    private val paymentMethodMetadata: PaymentMethodMetadata,
    private val confirmationHandler: ConfirmationHandler,
    private val configuration: EmbeddedPaymentElement.Configuration,
    private val selectionHolder: EmbeddedSelectionHolder,
    private val continueCoordinator: SheetActivityContinueCoordinator,
    private val onClickDelegate: OnClickOverrideDelegate,
    private val eventReporter: EventReporter,
    @ViewModelScope private val coroutineScope: CoroutineScope,
    @Named(STATUS_BAR_COLOR) private val statusBarColor: Int?,
) : SheetActivityConfirmationHelper {

    override fun confirm() {
        if (onClickDelegate.onClickOverride != null) {
            onClickDelegate.onClickOverride?.invoke()
        } else {
            selectionHolder.selection.value?.let { paymentSelection ->
                eventReporter.onPressConfirmButton(paymentSelection)
            }

            when (configuration.formSheetAction) {
                EmbeddedPaymentElement.FormSheetAction.Continue -> {
                    continueCoordinator.onContinue()
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

    private fun confirmationArgs(): ConfirmationHandler.Args? {
        val confirmationOption = selectionHolder.selection.value?.toConfirmationOption(
            configuration = configuration.asCommonConfiguration(),
            linkConfiguration = paymentMethodMetadata.linkState?.configuration,
            cardFundingFilter = paymentMethodMetadata.cardFundingFilter,
            googlePayBillingEmailOverride = GooglePayBillingEmailOverrideProvider.get(
                configuration = configuration.asCommonConfiguration(),
                paymentMethodMetadata = paymentMethodMetadata,
            ),
        ) ?: return null
        return ConfirmationHandler.Args(
            confirmationOption = confirmationOption,
            paymentMethodMetadata = paymentMethodMetadata,
            statusBarColor = statusBarColor,
        )
    }
}
