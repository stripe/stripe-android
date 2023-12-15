package com.stripe.android.paymentsheet.forms

import com.stripe.android.customersheet.CustomerSheet
import com.stripe.android.customersheet.ExperimentalCustomerSheetApi
import com.stripe.android.link.LinkPaymentDetails
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.getPMAddForm
import com.stripe.android.paymentsheet.paymentdatacollection.FormArguments
import com.stripe.android.ui.core.Amount
import com.stripe.android.ui.core.cbc.CardBrandChoiceEligibility
import com.stripe.android.ui.core.forms.resources.LpmRepository

internal object FormArgumentsFactory {

    fun create(
        paymentMethod: LpmRepository.SupportedPaymentMethod,
        stripeIntent: StripeIntent,
        config: PaymentSheet.Configuration,
        merchantName: String,
        amount: Amount? = null,
        newLpm: PaymentSelection.New?,
        cbcEligibility: CardBrandChoiceEligibility,
    ): FormArguments {
        val layoutFormDescriptor = paymentMethod.getPMAddForm(stripeIntent, config)

        val initialParams = when (newLpm) {
            is PaymentSelection.New.LinkInline -> {
                (newLpm.linkPaymentDetails as? LinkPaymentDetails.New)?.originalParams
            }
            is PaymentSelection.New.GenericPaymentMethod,
            is PaymentSelection.New.Card -> {
                if (newLpm.paymentMethodCreateParams.typeCode == paymentMethod.code) {
                    newLpm.paymentMethodCreateParams
                } else {
                    null
                }
            }
            else -> null
        }

        val initialExtraParams = when (newLpm) {
            is PaymentSelection.New.GenericPaymentMethod,
            is PaymentSelection.New.Card -> {
                if (newLpm.paymentMethodExtraParams?.type?.code == paymentMethod.code) {
                    newLpm.paymentMethodExtraParams
                } else {
                    null
                }
            }
            else -> null
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
            billingDetails = config.defaultBillingDetails,
            shippingDetails = config.shippingDetails,
            initialPaymentMethodCreateParams = initialParams,
            initialPaymentMethodExtraParams = initialExtraParams,
            billingDetailsCollectionConfiguration = config.billingDetailsCollectionConfiguration,
            cbcEligibility = cbcEligibility,
            requiresMandate = paymentMethod.requiresMandate,
            requiredFields = paymentMethod.placeholderOverrideList,
        )
    }

    @OptIn(ExperimentalCustomerSheetApi::class)
    fun create(
        paymentMethod: LpmRepository.SupportedPaymentMethod,
        configuration: CustomerSheet.Configuration,
        merchantName: String,
        cbcEligibility: CardBrandChoiceEligibility,
    ): FormArguments {
        return FormArguments(
            paymentMethodCode = paymentMethod.code,
            showCheckbox = false,
            showCheckboxControlledFields = false,
            merchantName = merchantName,
            billingDetails = configuration.defaultBillingDetails,
            billingDetailsCollectionConfiguration = configuration.billingDetailsCollectionConfiguration,
            cbcEligibility = cbcEligibility,
        )
    }
}
