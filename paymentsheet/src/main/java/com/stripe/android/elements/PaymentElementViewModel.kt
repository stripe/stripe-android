package com.stripe.android.elements

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.forms.FormFieldValues
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.getPMAddForm
import com.stripe.android.paymentsheet.paymentdatacollection.FormFragmentArguments
import com.stripe.android.ui.core.Amount
import com.stripe.android.ui.core.FieldValuesToParamsMapConverter
import com.stripe.android.ui.core.elements.IdentifierSpec
import com.stripe.android.ui.core.forms.resources.LpmRepository
import com.stripe.android.ui.core.injection.NonFallbackInjector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * ViewModel for the PaymentElement.
 * Creates the parameters that define the form elements displayed by the [FormViewModel], based on
 * the payment method type selected. Also transforms the form values into a [PaymentSelection],
 * which can be used to confirm a payment.
 */
internal class PaymentElementViewModel internal constructor(
    val supportedPaymentMethods: List<LpmRepository.SupportedPaymentMethod>,
    private val paymentElementConfig: PaymentElementConfig,
    context: Context
) : AndroidViewModel(context.applicationContext as Application) {
    val selectedPaymentMethod = MutableStateFlow(getInitiallySelectedPaymentMethod())

    /**
     * Holds the last valid `PaymentSelection`, so that the value is remembered if the user switches to a different
     * payment method and comes back.
     */
    private var lastPaymentSelection = paymentElementConfig.initialSelection

    val formArgumentsFlow = selectedPaymentMethod.map { selectedItem ->
        getFormArgumentsForPaymentMethod(selectedItem)
    }.stateIn(
        viewModelScope,
        SharingStarted.Lazily,
        getFormArgumentsForPaymentMethod(selectedPaymentMethod.value)
    )

    private val _paymentSelectionFlow = MutableStateFlow<PaymentSelection.New?>(null)
    val paymentSelectionFlow: StateFlow<PaymentSelection.New?> = _paymentSelectionFlow

    internal fun onFormFieldValuesChanged(values: FormFieldValues?) {
        _paymentSelectionFlow.value = values?.let { formFieldValues ->
            FieldValuesToParamsMapConverter
                .transformToPaymentMethodCreateParams(
                    formFieldValues.fieldValuePairs
                        .filterNot { entry ->
                            entry.key == IdentifierSpec.SaveForFutureUse ||
                                entry.key == IdentifierSpec.CardBrand
                        },
                    selectedPaymentMethod.value.code,
                    selectedPaymentMethod.value.requiresMandate
                ).run {
                    if (selectedPaymentMethod.value.code == PaymentMethod.Type.Card.code) {
                        PaymentSelection.New.Card(
                            paymentMethodCreateParams = this,
                            brand = CardBrand.fromCode(
                                formFieldValues.fieldValuePairs[IdentifierSpec.CardBrand]?.value
                            ),
                            customerRequestedSave = formFieldValues.userRequestedReuse

                        )
                    } else {
                        PaymentSelection.New.GenericPaymentMethod(
                            getApplication<Application>()
                                .getString(selectedPaymentMethod.value.displayNameResource),
                            selectedPaymentMethod.value.iconResource,
                            this,
                            customerRequestedSave = formFieldValues.userRequestedReuse
                        )
                    }
                }
        }?.also {
            lastPaymentSelection = it
        }
    }

    fun onPaymentMethodSelected(supportedPaymentMethod: LpmRepository.SupportedPaymentMethod) {
        selectedPaymentMethod.value = supportedPaymentMethod
    }

    private fun getFormArgumentsForPaymentMethod(
        selectedItem: LpmRepository.SupportedPaymentMethod
    ) = selectedItem.getPMAddForm(
        paymentElementConfig.stripeIntent,
        paymentElementConfig.hasCustomerConfiguration,
        paymentElementConfig.allowsDelayedPaymentMethods
    ).let { layoutFormDescriptor ->
        FormFragmentArguments(
            paymentMethodCode = selectedItem.code,
            showCheckbox = layoutFormDescriptor.showCheckbox,
            showCheckboxControlledFields = lastPaymentSelection?.let {
                it.customerRequestedSave ==
                    PaymentSelection.CustomerRequestedSave.RequestReuse
            } ?: layoutFormDescriptor.showCheckboxControlledFields,
            merchantName = paymentElementConfig.merchantName,
            amount = if (paymentElementConfig.stripeIntent is PaymentIntent) {
                Amount(
                    requireNotNull(paymentElementConfig.stripeIntent.amount),
                    requireNotNull(paymentElementConfig.stripeIntent.currency)
                )
            } else {
                null
            },
            billingDetails = paymentElementConfig.defaultBillingDetails,
            shippingDetails = paymentElementConfig.shippingDetails,
            initialPaymentMethodCreateParams = lastPaymentSelection?.takeIf {
                it.paymentMethodCreateParams.typeCode == selectedItem.code ||
                    (
                        it is PaymentSelection.New.LinkInline &&
                            selectedItem.code == PaymentMethod.Type.Card.code
                        )
            }?.let {
                when (it) {
                    is PaymentSelection.New.LinkInline ->
                        it.linkPaymentDetails.originalParams
                    is PaymentSelection.New.GenericPaymentMethod ->
                        it.paymentMethodCreateParams
                    is PaymentSelection.New.Card ->
                        it.paymentMethodCreateParams
                    else -> null
                }
            }
        )
    }

    private fun getInitiallySelectedPaymentMethod() =
        supportedPaymentMethods.firstOrNull {
            it.code ==
                when (val selection = paymentElementConfig.initialSelection) {
                    is PaymentSelection.New.LinkInline -> PaymentMethod.Type.Card.code
                    is PaymentSelection.New.Card,
                    is PaymentSelection.New.USBankAccount,
                    is PaymentSelection.New.GenericPaymentMethod ->
                        selection.paymentMethodCreateParams.typeCode
                    else -> supportedPaymentMethods.first().code
                }
        } ?: supportedPaymentMethods.first()

    internal class Factory(
        private val supportedPaymentMethods: List<LpmRepository.SupportedPaymentMethod>,
        private val paymentElementConfig: PaymentElementController.Config,
        private val context: Context,
        val injector: NonFallbackInjector
    ) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PaymentElementViewModel(
                supportedPaymentMethods,
                paymentElementConfig,
                context
            ) as T
        }
    }
}
