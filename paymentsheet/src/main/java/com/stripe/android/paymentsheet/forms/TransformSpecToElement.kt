package com.stripe.android.paymentsheet.forms

import android.content.Context
import com.stripe.android.paymentsheet.addresselement.toIdentifierMap
import com.stripe.android.paymentsheet.paymentdatacollection.FormArguments
import com.stripe.android.paymentsheet.paymentdatacollection.getInitialValuesMap
import com.stripe.android.ui.core.address.AddressRepository
import com.stripe.android.ui.core.elements.FormItemSpec
import com.stripe.android.ui.core.forms.TransformSpecToElements
import com.stripe.android.ui.core.forms.resources.ResourceRepository
import javax.inject.Inject

/**
 * Wrapper around [TransformSpecToElements] that uses the parameters from [FormArguments].
 */
internal class TransformSpecToElement @Inject constructor(
    addressResourceRepository: ResourceRepository<AddressRepository>,
    formArguments: FormArguments,
    context: Context
) {
    private val transformSpecToElements =
        TransformSpecToElements(
            addressResourceRepository = addressResourceRepository,
            initialValues = formArguments.getInitialValuesMap(),
            amount = formArguments.amount,
            saveForFutureUseInitialValue = formArguments.showCheckboxControlledFields,
            merchantName = formArguments.merchantName,
            context = context,
            shippingValues = formArguments.shippingDetails
                ?.toIdentifierMap(formArguments.billingDetails)
        )

    internal fun transform(list: List<FormItemSpec>) = transformSpecToElements.transform(list)
}
