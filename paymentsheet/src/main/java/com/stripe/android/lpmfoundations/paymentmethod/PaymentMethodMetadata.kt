package com.stripe.android.lpmfoundations.paymentmethod

import android.os.Parcelable
import com.stripe.android.lpmfoundations.luxe.SupportedPaymentMethod
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.SetupIntent
import com.stripe.android.model.StripeIntent
import com.stripe.android.payments.financialconnections.DefaultIsFinancialConnectionsAvailable
import com.stripe.android.ui.core.BillingDetailsCollectionConfiguration
import com.stripe.android.ui.core.elements.SharedDataSpec
import kotlinx.parcelize.Parcelize

/**
 * The metadata we need to determine what payment methods are supported, as well as being able to display them.
 * The purpose of this is to be able to easily plumb this information into the locations it’s needed.
 */
@Parcelize
internal data class PaymentMethodMetadata(
    val stripeIntent: StripeIntent,
    val billingDetailsCollectionConfiguration: BillingDetailsCollectionConfiguration,
    val allowsDelayedPaymentMethods: Boolean,
    val allowsPaymentMethodsRequiringShippingAddress: Boolean,
    val sharedDataSpecs: List<SharedDataSpec>,
    val financialConnectionsAvailable: Boolean = DefaultIsFinancialConnectionsAvailable(),
) : Parcelable {
    fun hasIntentToSetup(): Boolean {
        return when (stripeIntent) {
            is PaymentIntent -> stripeIntent.setupFutureUsage != null
            is SetupIntent -> true
        }
    }

    fun supportedPaymentMethodDefinitions(): List<PaymentMethodDefinition> {
        return stripeIntent.paymentMethodTypes.mapNotNull {
            PaymentMethodRegistry.definitionsByCode[it]
        }.filter {
            it.isSupported(this)
        }.filter { paymentMethodDefinition ->
            sharedDataSpecs.firstOrNull { it.type == paymentMethodDefinition.type.code } != null
        }
    }

    fun supportedPaymentMethodForCode(code: String): SupportedPaymentMethod? {
        val definition = supportedPaymentMethodDefinitions().firstOrNull { it.type.code == code } ?: return null
        val sharedDataSpec = sharedDataSpecs.firstOrNull { it.type == code } ?: return null
        return definition.supportedPaymentMethod(this, sharedDataSpec)
    }
}
