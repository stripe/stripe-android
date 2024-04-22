package com.stripe.android.paymentsheet.forms

import com.stripe.android.customersheet.CustomerSheet
import com.stripe.android.customersheet.ExperimentalCustomerSheetApi
import com.stripe.android.lpmfoundations.luxe.SupportedPaymentMethod
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.paymentsheet.paymentdatacollection.FormArguments
import com.stripe.android.ui.core.cbc.CardBrandChoiceEligibility
import com.stripe.android.uicore.address.AddressSchemas

internal object FormArgumentsFactory {

    fun create(
        paymentMethod: SupportedPaymentMethod,
        metadata: PaymentMethodMetadata,
    ): FormArguments {
        return FormArguments(
            paymentMethodCode = paymentMethod.code,
            merchantName = metadata.merchantName,
            amount = metadata.amount(),
            billingDetails = metadata.defaultBillingDetails,
            shippingDetails = metadata.shippingDetails,
            billingDetailsCollectionConfiguration = metadata.billingDetailsCollectionConfiguration,
            cbcEligibility = metadata.cbcEligibility,
            addressSchemas = metadata.addressSchemas,
        )
    }

    @OptIn(ExperimentalCustomerSheetApi::class)
    fun create(
        paymentMethodCode: PaymentMethodCode,
        configuration: CustomerSheet.Configuration,
        merchantName: String,
        cbcEligibility: CardBrandChoiceEligibility,
        addressSchemas: AddressSchemas,
    ): FormArguments {
        return FormArguments(
            paymentMethodCode = paymentMethodCode,
            merchantName = merchantName,
            billingDetails = configuration.defaultBillingDetails,
            billingDetailsCollectionConfiguration = configuration.billingDetailsCollectionConfiguration,
            cbcEligibility = cbcEligibility,
            addressSchemas = addressSchemas,
        )
    }
}
