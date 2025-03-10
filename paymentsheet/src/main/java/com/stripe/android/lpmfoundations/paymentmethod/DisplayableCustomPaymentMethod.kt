package com.stripe.android.lpmfoundations.paymentmethod

import android.os.Parcelable
import com.stripe.android.common.model.CommonConfiguration
import com.stripe.android.model.ElementsSession
import com.stripe.android.paymentelement.ExperimentalCustomPaymentMethodsApi
import kotlinx.parcelize.Parcelize

@Parcelize
@OptIn(ExperimentalCustomPaymentMethodsApi::class)
internal data class DisplayableCustomPaymentMethod(
    val id: String,
    val displayName: String,
    val logoUrl: String,
    val subtitle: String?,
    val collectsBillingDetails: Boolean,
) : Parcelable {
    companion object {
        fun create(
            customPaymentMethod: ElementsSession.CustomPaymentMethod,
            configuration: CommonConfiguration
        ): DisplayableCustomPaymentMethod {
            val associatedType = configuration
                .customPaymentMethodConfiguration
                ?.customPaymentMethodTypes?.first { type ->
                    customPaymentMethod.id == type.id
                }

            return DisplayableCustomPaymentMethod(
                id = customPaymentMethod.id,
                displayName = customPaymentMethod.displayName,
                logoUrl = customPaymentMethod.logoUrl,
                subtitle = associatedType?.subtitle,
                collectsBillingDetails = associatedType?.shouldCollectBillingDetails ?: false
            )
        }
    }
}