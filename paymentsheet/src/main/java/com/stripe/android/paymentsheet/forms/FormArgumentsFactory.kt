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
        config: PaymentSheet.Configuration?,
        merchantName: String,
        amount: Amount? = null,
        newLpm: PaymentSelection.New?,
        cbcEligibility: CardBrandChoiceEligibility,
    ): FormArguments {
        val layoutFormDescriptor = paymentMethod.getPMAddForm(stripeIntent, config)

        val originalParams = (
            (newLpm as? PaymentSelection.New.LinkInline)
                ?.linkPaymentDetails as? LinkPaymentDetails.New
            )?.originalParams
        val initialParams = originalParams
            ?: newLpm?.paymentMethodCreateParams?.typeCode?.takeIf {
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
                ?: PaymentSheet.BillingDetailsCollectionConfiguration(),
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
    ): FormArguments {
        return FormArguments(
            paymentMethodCode = paymentMethod.code,
            showCheckbox = false,
            showCheckboxControlledFields = false,
            merchantName = merchantName,
            billingDetails = configuration.defaultBillingDetails,
            billingDetailsCollectionConfiguration = configuration.billingDetailsCollectionConfiguration,
            // TODO(tillh-stripe) Determine this based on /wallets-config response
            cbcEligibility = CardBrandChoiceEligibility.Ineligible,
        )
    }
}
