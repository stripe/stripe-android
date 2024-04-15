package com.stripe.android.paymentsheet.forms

import com.stripe.android.customersheet.CustomerSheet
import com.stripe.android.customersheet.ExperimentalCustomerSheetApi
import com.stripe.android.lpmfoundations.luxe.SupportedPaymentMethod
import com.stripe.android.lpmfoundations.luxe.isSaveForFutureUseValueChangeable
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.paymentdatacollection.FormArguments
import com.stripe.android.ui.core.cbc.CardBrandChoiceEligibility

internal object FormArgumentsFactory {

    fun create(
        paymentMethod: SupportedPaymentMethod,
        metadata: PaymentMethodMetadata,
        customerConfig: PaymentSheet.CustomerConfiguration?,
    ): FormArguments {
        val isSaveForFutureUseValueChangeable = isSaveForFutureUseValueChangeable(
            code = paymentMethod.code,
            metadata = metadata,
            customerConfiguration = customerConfig,
        )

        return FormArguments(
            paymentMethodCode = paymentMethod.code,
            showCheckbox = isSaveForFutureUseValueChangeable,
            merchantName = metadata.merchantName,
            amount = metadata.amount(),
            billingDetails = metadata.defaultBillingDetails,
            shippingDetails = metadata.shippingDetails,
            billingDetailsCollectionConfiguration = metadata.billingDetailsCollectionConfiguration,
            cbcEligibility = metadata.cbcEligibility,
        )
    }

    @OptIn(ExperimentalCustomerSheetApi::class)
    fun create(
        paymentMethodCode: PaymentMethodCode,
        configuration: CustomerSheet.Configuration,
        merchantName: String,
        cbcEligibility: CardBrandChoiceEligibility,
    ): FormArguments {
        return FormArguments(
            paymentMethodCode = paymentMethodCode,
            showCheckbox = false,
            merchantName = merchantName,
            billingDetails = configuration.defaultBillingDetails,
            billingDetailsCollectionConfiguration = configuration.billingDetailsCollectionConfiguration,
            cbcEligibility = cbcEligibility,
        )
    }
}
