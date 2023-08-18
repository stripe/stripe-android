package com.stripe.android.paymentsheet.forms

import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.getPMAddForm
import com.stripe.android.paymentsheet.paymentdatacollection.FormArguments
import com.stripe.android.ui.core.Amount
import com.stripe.android.ui.core.forms.resources.LpmRepository

internal object FormArgumentsFactory {

    fun create(
        paymentMethod: LpmRepository.SupportedPaymentMethod,
        stripeIntent: StripeIntent,
        config: PaymentSheet.Configuration?,
        merchantName: String,
        amount: Amount? = null,
        newLpm: PaymentSelection.New?,
    ): FormArguments {
        val layoutFormDescriptor = paymentMethod.getPMAddForm(stripeIntent, config)

        val initialParams = if (newLpm is PaymentSelection.New.LinkInline) {
            newLpm.linkPaymentDetails.originalParams
        } else {
            newLpm?.paymentMethodCreateParams?.typeCode?.takeIf {
                it == paymentMethod.code
            }?.let {
                when (newLpm) {
                    is PaymentSelection.New.GenericPaymentMethod ->
                        newLpm.paymentMethodCreateParams
                    is PaymentSelection.New.Card ->
                        newLpm.paymentMethodCreateParams
                    else -> null
                }
            }
        }

        val showCheckboxControlledFields = if (newLpm != null) {
            newLpm.customerRequestedSave == PaymentSelection.CustomerRequestedSave.RequestReuse
        } else {
            layoutFormDescriptor.showCheckboxControlledFields
        }

        return FormArguments(
            paymentMethodCode = paymentMethod.code,
            showCheckbox = layoutFormDescriptor.showCheckbox,
            showCheckboxControlledFields = showCheckboxControlledFields,
            merchantName = merchantName,
            amount = amount,
            billingDetails = config?.defaultBillingDetails,
            shippingDetails = config?.shippingDetails,
            initialPaymentMethodCreateParams = initialParams,
            billingDetailsCollectionConfiguration = config?.billingDetailsCollectionConfiguration
                ?: PaymentSheet.BillingDetailsCollectionConfiguration()
        )
    }
}
