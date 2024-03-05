package com.stripe.android.paymentsheet.forms

import com.stripe.android.customersheet.CustomerSheet
import com.stripe.android.customersheet.ExperimentalCustomerSheetApi
import com.stripe.android.link.LinkPaymentDetails
import com.stripe.android.lpmfoundations.luxe.SupportedPaymentMethod
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.getFormLayoutConfiguration
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.paymentdatacollection.FormArguments
import com.stripe.android.ui.core.Amount
import com.stripe.android.ui.core.cbc.CardBrandChoiceEligibility

internal object FormArgumentsFactory {

    fun create(
        paymentMethod: SupportedPaymentMethod,
        metadata: PaymentMethodMetadata,
        config: PaymentSheet.Configuration,
        merchantName: String,
        amount: Amount? = null,
        newLpm: PaymentSelection.New?,
        cbcEligibility: CardBrandChoiceEligibility,
    ): FormArguments {
        val formLayoutConfiguration = requireNotNull(
            paymentMethod.paymentMethodDefinition().getFormLayoutConfiguration(
                metadata = metadata,
                customerConfiguration = config.customer,
            )
        )

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
            formLayoutConfiguration.showCheckboxControlledFields
        }

        return FormArguments(
            paymentMethodCode = paymentMethod.code,
            showCheckbox = formLayoutConfiguration.showCheckbox,
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
        paymentMethod: SupportedPaymentMethod,
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
