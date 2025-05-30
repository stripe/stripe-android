package com.stripe.android.paymentelement.embedded.content

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stripe.android.common.model.asCommonConfiguration
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.ExperimentalEmbeddedPaymentElementApi
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.toConfirmationOption
import com.stripe.android.paymentelement.embedded.EmbeddedSelectionHolder
import com.stripe.android.paymentelement.embedded.form.FormActivityError
import com.stripe.android.paymentelement.embedded.form.FormActivityPrimaryButton
import com.stripe.android.paymentelement.embedded.form.FormActivityStateHelper
import com.stripe.android.paymentelement.embedded.form.FormResult
import com.stripe.android.paymentelement.embedded.form.OnClickOverrideDelegate
import com.stripe.android.paymentelement.embedded.form.USBankAccountMandate
import com.stripe.android.paymentsheet.PaymentSheet.Appearance.Embedded
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import com.stripe.android.paymentsheet.utils.DismissKeyboardOnProcessing
import com.stripe.android.paymentsheet.utils.EventReporterProvider
import com.stripe.android.paymentsheet.utils.PaymentSheetContentPadding
import com.stripe.android.paymentsheet.verticalmode.PaymentMethodEmbeddedLayoutUI
import com.stripe.android.paymentsheet.verticalmode.PaymentMethodVerticalLayoutInteractor
import com.stripe.android.paymentsheet.verticalmode.VerticalModeFormInteractor
import com.stripe.android.paymentsheet.verticalmode.VerticalModeFormUI
import com.stripe.android.uicore.StripeTheme
import com.stripe.android.uicore.utils.collectAsState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Immutable
@OptIn(ExperimentalEmbeddedPaymentElementApi::class)
internal sealed interface EmbeddedContent {
    @Composable
    fun Content()

    @Immutable
    class PaymentMethods(
        private val interactor: PaymentMethodVerticalLayoutInteractor,
        private val embeddedViewDisplaysMandateText: Boolean,
        private val rowStyle: Embedded.RowStyle
    ) : EmbeddedContent {
        @Composable
        override fun Content() {
            StripeTheme {
                Column(
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .animateContentSize()
                ) {
                    PaymentMethodEmbeddedLayoutUI(
                        interactor = interactor,
                        embeddedViewDisplaysMandateText = embeddedViewDisplaysMandateText,
                        modifier = Modifier.padding(bottom = 8.dp),
                        rowStyle = rowStyle
                    )
                }
            }
        }
    }

    @Immutable
    class AddPaymentMethod(
        private val interactor: VerticalModeFormInteractor,
        private val eventReporter: EventReporter,
        private val formActivityStateHelper: FormActivityStateHelper,
        private val onClickDelegate: OnClickOverrideDelegate,
        private val selectionHolder: EmbeddedSelectionHolder,
        private val paymentMethodMetadata: PaymentMethodMetadata,
        private val confirmationHandler: ConfirmationHandler,
        private val configuration: EmbeddedPaymentElement.Configuration,
        private val initializationMode: PaymentElementLoader.InitializationMode,
        private val coroutineScope: CoroutineScope,
        private val onResult: (FormResult) -> Unit,
    ) : EmbeddedContent {
        @Composable
        override fun Content() {
            StripeTheme {
                val state by formActivityStateHelper.state.collectAsState()

                val interactorState by interactor.state.collectAsState()

                DismissKeyboardOnProcessing(interactorState.isProcessing)

                EventReporterProvider(eventReporter) {
                    Column {
                        VerticalModeFormUI(
                            interactor = interactor,
                            showsWalletHeader = false
                        )
                        USBankAccountMandate(state)
                        FormActivityError(state)
                        PaymentSheetContentPadding()
                        FormActivityPrimaryButton(
                            state = state,
                            onClick = {
                                confirm()?.let {
                                    onResult(it)
                                }
                            },
                            onProcessingCompleted = {
                                onResult(FormResult.Complete(selection = null, hasBeenConfirmed = true))
                            },
                        )
                    }
                }
            }
        }

        private fun confirm(): FormResult? {
            if (onClickDelegate.onClickOverride != null) {
                onClickDelegate.onClickOverride?.invoke()
            } else {
                eventReporter.onPressConfirmButton(selectionHolder.selection.value)

                when (configuration.formSheetAction) {
                    EmbeddedPaymentElement.FormSheetAction.Continue -> {
                        return FormResult.Complete(selectionHolder.selection.value, false)
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

            return null
        }

        private fun confirmationArgs(): ConfirmationHandler.Args? {
            val confirmationOption = selectionHolder.selection.value?.toConfirmationOption(
                configuration = configuration.asCommonConfiguration(),
                linkConfiguration = paymentMethodMetadata.linkState?.configuration
            ) ?: return null
            return ConfirmationHandler.Args(
                intent = paymentMethodMetadata.stripeIntent,
                confirmationOption = confirmationOption,
                appearance = configuration.appearance,
                initializationMode = initializationMode,
                shippingDetails = paymentMethodMetadata.shippingDetails
            )
        }
    }
}
