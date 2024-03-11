package com.stripe.android.paymentsheet.forms

import com.stripe.android.customersheet.CustomerSheet
import com.stripe.android.customersheet.ExperimentalCustomerSheetApi
import com.stripe.android.link.LinkPaymentDetails
import com.stripe.android.lpmfoundations.luxe.SupportedPaymentMethod
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.getSetupFutureUsageFieldConfiguration
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
    ): FormArguments {
        val setupFutureUsageFieldConfiguration =
            paymentMethod.paymentMethodDefinition().getSetupFutureUsageFieldConfiguration(
                metadata = metadata,
                customerConfiguration = config.customer,
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

        val saveForFutureUseInitialValue = if (newLpm != null) {
            newLpm.customerRequestedSave == PaymentSelection.CustomerRequestedSave.RequestReuse
        } else {
            setupFutureUsageFieldConfiguration?.saveForFutureUseInitialValue == true
        }

        return FormArguments(
            paymentMethodCode = paymentMethod.code,
            showCheckbox = setupFutureUsageFieldConfiguration?.isSaveForFutureUseValueChangeable == true,
            saveForFutureUseInitialValue = saveForFutureUseInitialValue,
            merchantName = merchantName,
            amount = amount,
            billingDetails = config.defaultBillingDetails,
            shippingDetails = config.shippingDetails,
            initialPaymentMethodCreateParams = initialParams,
            initialPaymentMethodExtraParams = initialExtraParams,
            billingDetailsCollectionConfiguration = config.billingDetailsCollectionConfiguration,
            cbcEligibility = metadata.cbcEligibility,
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
            saveForFutureUseInitialValue = false,
            merchantName = merchantName,
            billingDetails = configuration.defaultBillingDetails,
            billingDetailsCollectionConfiguration = configuration.billingDetailsCollectionConfiguration,
            cbcEligibility = cbcEligibility,
        )
    }
}
