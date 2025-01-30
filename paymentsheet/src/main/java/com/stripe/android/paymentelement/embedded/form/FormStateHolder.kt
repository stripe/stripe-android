package com.stripe.android.paymentelement.embedded.form

import com.stripe.android.core.injection.ViewModelScope
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.model.SetupIntent
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

internal class FormStateHolder @Inject constructor(
    embeddedFormHelperFactory: EmbeddedFormHelperFactory,
    private val paymentMethodCode: PaymentMethodCode,
    private val paymentMethodMetadata: PaymentMethodMetadata,
    @ViewModelScope coroutineScope: CoroutineScope
) {

    private val formHelper = embeddedFormHelperFactory.create(
        coroutineScope = coroutineScope,
        paymentMethodMetadata = paymentMethodMetadata,
        selectionUpdater = {}
    )
    private val _formState: MutableStateFlow<FormState> = MutableStateFlow(
        FormState(
            code = paymentMethodCode,
            primaryButtonIsEnabled = false,
            primaryButtonLabel = when (val stripeIntent = paymentMethodMetadata.stripeIntent) {
                is PaymentIntent -> {
                    Amount(
                        requireNotNull(stripeIntent.amount),
                        requireNotNull(stripeIntent.currency)
                    ).buildPayButtonLabel()
                }
                is SetupIntent -> R.string.stripe_setup_button_label.resolvableString
            }
        )
    )
    val formState: StateFlow<FormState> = _formState


    fun formValuesChanged(formValues: FormFieldValues?) {
        val params = formHelper.getPaymentMethodParams(
            formValues = formValues,
            selectedPaymentMethodCode = paymentMethodCode
        )

        val options = formValues?.transformToPaymentMethodOptionsParams(paymentMethodCode)
        var paymentSelection: PaymentSelection? = null
        paymentMethodMetadata.supportedPaymentMethodForCode(paymentMethodCode)?.let {
            paymentSelection = formValues?.transformToPaymentSelection(it, paymentMethodMetadata)
        }

        _formState.update {
            it.copy(
                paymentMethodCreateParams = params,
                paymentOptionsParams = options,
                primaryButtonIsEnabled = params != null,
                paymentSelection = paymentSelection
            )
        }
    }
}
