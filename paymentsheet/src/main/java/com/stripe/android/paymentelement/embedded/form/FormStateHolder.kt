package com.stripe.android.paymentelement.embedded.form

import com.stripe.android.core.injection.ViewModelScope
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.model.SetupIntent
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.ExperimentalEmbeddedPaymentElementApi
import com.stripe.android.paymentelement.embedded.EmbeddedFormHelperFactory
import com.stripe.android.paymentsheet.forms.FormFieldValues
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.ui.transformToPaymentMethodOptionsParams
import com.stripe.android.paymentsheet.ui.transformToPaymentSelection
import com.stripe.android.ui.core.Amount
import com.stripe.android.ui.core.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@OptIn(ExperimentalEmbeddedPaymentElementApi::class)
internal class FormStateHolder @Inject constructor(
    embeddedFormHelperFactory: EmbeddedFormHelperFactory,
    private val paymentMethodCode: PaymentMethodCode,
    private val paymentMethodMetadata: PaymentMethodMetadata,
    private val configuration: EmbeddedPaymentElement.Configuration,
    @ViewModelScope coroutineScope: CoroutineScope
) {

    private val formHelper = embeddedFormHelperFactory.create(
        coroutineScope = coroutineScope,
        paymentMethodMetadata = paymentMethodMetadata,
        selectionUpdater = { selection ->
            _formState.update {
                it.copy(
                    primaryButtonIsEnabled = selection != null,
                    paymentSelection = selection
                )
            }
        }
    )
    private val _formState: MutableStateFlow<FormState> = MutableStateFlow(
        FormState(
            code = paymentMethodCode,
            primaryButtonIsEnabled = false,
            primaryButtonLabel = if (configuration.primaryButtonLabel != null) {
                configuration.primaryButtonLabel.resolvableString
            } else {
                when (val stripeIntent = paymentMethodMetadata.stripeIntent) {
                    // This will always be Pay or Set up until two step is implemented
                    //
                    is PaymentIntent -> {
                        Amount(
                            requireNotNull(stripeIntent.amount),
                            requireNotNull(stripeIntent.currency)
                        ).buildPayButtonLabel()
                    }
                    is SetupIntent -> R.string.stripe_setup_button_label.resolvableString
                }
            }
        )
    )
    val formState: StateFlow<FormState> = _formState


    fun formValuesChanged(formValues: FormFieldValues?) {
        formHelper.onFormFieldValuesChanged(formValues, paymentMethodCode)
    }
}
