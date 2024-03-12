package com.stripe.android.lpmfoundations.paymentmethod

import android.os.Parcelable
import com.stripe.android.lpmfoundations.luxe.SupportedPaymentMethod
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.SetupIntent
import com.stripe.android.model.StripeIntent
import com.stripe.android.payments.financialconnections.DefaultIsFinancialConnectionsAvailable
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.ui.core.cbc.CardBrandChoiceEligibility
import com.stripe.android.ui.core.elements.SharedDataSpec
import kotlinx.parcelize.Parcelize

/**
 * The metadata we need to determine what payment methods are supported, as well as being able to display them.
 * The purpose of this is to be able to easily plumb this information into the locations it’s needed.
 */
@Parcelize
internal data class PaymentMethodMetadata(
    val stripeIntent: StripeIntent,
    val billingDetailsCollectionConfiguration: PaymentSheet.BillingDetailsCollectionConfiguration,
    val allowsDelayedPaymentMethods: Boolean,
    val allowsPaymentMethodsRequiringShippingAddress: Boolean,
    val paymentMethodOrder: List<String>,
    val cbcEligibility: CardBrandChoiceEligibility,
    val merchantName: String,
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
        }.filterNot {
            stripeIntent.isLiveMode &&
                stripeIntent.unactivatedPaymentMethods.contains(it.type.code)
        }.filter { paymentMethodDefinition ->
            sharedDataSpecs.firstOrNull { it.type == paymentMethodDefinition.type.code } != null
        }.run {
            if (paymentMethodOrder.isEmpty()) {
                // Optimization to early out if we don't have a client side order.
                this
            } else {
                val orderedPaymentMethodTypes = orderedPaymentMethodTypes().mapOrderToIndex()
                sortedBy { paymentMethodDefinition ->
                    orderedPaymentMethodTypes[paymentMethodDefinition.type.code]
                }
            }
        }
    }

    fun supportedSavedPaymentMethodTypes(): List<PaymentMethod.Type> {
        return supportedPaymentMethodDefinitions().filter { paymentMethodDefinition ->
            paymentMethodDefinition.supportedAsSavedPaymentMethod
        }.map {
            it.type
        }
    }

    fun supportedPaymentMethodForCode(code: String): SupportedPaymentMethod? {
        val definition = supportedPaymentMethodDefinitions().firstOrNull { it.type.code == code } ?: return null
        val sharedDataSpec = sharedDataSpecs.firstOrNull { it.type == code } ?: return null
        return definition.supportedPaymentMethod(this, sharedDataSpec)
    }

    fun sortedSupportedPaymentMethods(): List<SupportedPaymentMethod> {
        return supportedPaymentMethodDefinitions().mapNotNull { paymentMethodDefinition ->
            val sharedDataSpec = sharedDataSpecs.firstOrNull { it.type == paymentMethodDefinition.type.code }
            if (sharedDataSpec == null) {
                null
            } else {
                paymentMethodDefinition.supportedPaymentMethod(this, sharedDataSpec)
            }
        }
    }

    private fun orderedPaymentMethodTypes(): List<String> {
        val originalOrderedTypes = stripeIntent.paymentMethodTypes.toMutableList()
        val result = mutableListOf<String>()
        // 1. Add each PM in paymentMethodOrder first
        for (pm in paymentMethodOrder) {
            // Ignore the PM if it's not in originalOrderedTypes
            if (originalOrderedTypes.contains(pm)) {
                result += pm
                // 2. Remove each PM we add from originalOrderedTypes.
                originalOrderedTypes.remove(pm)
            }
        }
        // 3. Append the remaining PMs in originalOrderedTypes
        result.addAll(originalOrderedTypes)
        return result
    }

    private fun List<String>.mapOrderToIndex(): Map<String, Int> {
        return mapIndexed { index, s ->
            s to index
        }.toMap()
    }
}
