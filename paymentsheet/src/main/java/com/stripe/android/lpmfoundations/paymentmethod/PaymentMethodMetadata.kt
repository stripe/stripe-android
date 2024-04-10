package com.stripe.android.lpmfoundations.paymentmethod

import android.content.Context
import android.os.Parcelable
import com.stripe.android.lpmfoundations.luxe.InitialValuesFactory
import com.stripe.android.lpmfoundations.luxe.SupportedPaymentMethod
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodExtraParams
import com.stripe.android.model.SetupIntent
import com.stripe.android.model.StripeIntent
import com.stripe.android.payments.financialconnections.DefaultIsFinancialConnectionsAvailable
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.addresselement.AddressDetails
import com.stripe.android.paymentsheet.addresselement.toIdentifierMap
import com.stripe.android.ui.core.Amount
import com.stripe.android.ui.core.cbc.CardBrandChoiceEligibility
import com.stripe.android.ui.core.elements.SharedDataSpec
import com.stripe.android.uicore.address.AddressRepository
import com.stripe.android.uicore.elements.FormElement
import kotlinx.coroutines.Dispatchers
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import java.util.concurrent.atomic.AtomicReference

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
    val defaultBillingDetails: PaymentSheet.BillingDetails?,
    val shippingDetails: AddressDetails?,
    val sharedDataSpecs: List<SharedDataSpec>,
    val financialConnectionsAvailable: Boolean = DefaultIsFinancialConnectionsAvailable(),
) : Parcelable {
    @IgnoredOnParcel
    private val addressRepositoryLock: Any = Any()

    @IgnoredOnParcel
    private val addressRepositoryReference = AtomicReference<AddressRepository?>()

    fun hasIntentToSetup(): Boolean {
        return when (stripeIntent) {
            is PaymentIntent -> stripeIntent.setupFutureUsage != null
            is SetupIntent -> true
        }
    }

    fun requiresMandate(paymentMethodCode: String): Boolean {
        return PaymentMethodRegistry.definitionsByCode[paymentMethodCode]?.requiresMandate(this) ?: false
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
            paymentMethodDefinition.uiDefinitionFactory().canBeDisplayedInUi(paymentMethodDefinition, sharedDataSpecs)
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

    fun supportedPaymentMethodForCode(
        code: String,
    ): SupportedPaymentMethod? {
        val definition = supportedPaymentMethodDefinitions().firstOrNull { it.type.code == code } ?: return null
        return definition.uiDefinitionFactory().supportedPaymentMethod(definition, sharedDataSpecs)
    }

    fun sortedSupportedPaymentMethods(): List<SupportedPaymentMethod> {
        return supportedPaymentMethodDefinitions().mapNotNull { definition ->
            definition.uiDefinitionFactory().supportedPaymentMethod(definition, sharedDataSpecs)
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

    fun amount(): Amount? {
        if (stripeIntent is PaymentIntent) {
            return Amount(
                requireNotNull(stripeIntent.amount),
                requireNotNull(stripeIntent.currency)
            )
        }
        return null
    }

    private fun uiDefinitionFactoryArguments(
        context: Context,
        requiresMandate: Boolean,
        paymentMethodCreateParams: PaymentMethodCreateParams? = null,
        paymentMethodExtraParams: PaymentMethodExtraParams? = null,
    ): UiDefinitionFactory.Arguments {
        var addressRepository = addressRepositoryReference.get()
        if (addressRepository == null) {
            synchronized(addressRepositoryLock) {
                addressRepository = addressRepositoryReference.get()
                if (addressRepository == null) {
                    addressRepository = AddressRepository(context.resources, Dispatchers.IO)
                    addressRepositoryReference.set(addressRepository)
                }
            }
        }

        return UiDefinitionFactory.Arguments(
            amount = amount(),
            merchantName = merchantName,
            cbcEligibility = cbcEligibility,
            initialValues = InitialValuesFactory.create(
                defaultBillingDetails = defaultBillingDetails,
                paymentMethodCreateParams = paymentMethodCreateParams,
                paymentMethodExtraParams = paymentMethodExtraParams,
            ),
            shippingValues = shippingDetails?.toIdentifierMap(defaultBillingDetails),
            saveForFutureUseInitialValue = false,
            context = context.applicationContext,
            addressRepository = requireNotNull(addressRepository),
            billingDetailsCollectionConfiguration = billingDetailsCollectionConfiguration,
            requiresMandate = requiresMandate,
        )
    }

    fun formElementsForCode(
        code: String,
        context: Context,
        paymentMethodCreateParams: PaymentMethodCreateParams?,
        paymentMethodExtraParams: PaymentMethodExtraParams?,
    ): List<FormElement>? {
        val definition = supportedPaymentMethodDefinitions().firstOrNull { it.type.code == code } ?: return null

        return definition.uiDefinitionFactory().formElements(
            metadata = this,
            definition = definition,
            sharedDataSpecs = sharedDataSpecs,
            arguments = uiDefinitionFactoryArguments(
                context = context,
                requiresMandate = definition.requiresMandate(this),
                paymentMethodCreateParams = paymentMethodCreateParams,
                paymentMethodExtraParams = paymentMethodExtraParams,
            ),
        )
    }
}
