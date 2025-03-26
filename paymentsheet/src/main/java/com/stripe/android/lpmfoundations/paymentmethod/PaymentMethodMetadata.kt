package com.stripe.android.lpmfoundations.paymentmethod

import android.os.Parcelable
import com.stripe.android.CardBrandFilter
import com.stripe.android.common.configuration.ConfigurationDefaults
import com.stripe.android.common.model.CommonConfiguration
import com.stripe.android.customersheet.CustomerSheet
import com.stripe.android.link.LinkConfiguration
import com.stripe.android.lpmfoundations.FormHeaderInformation
import com.stripe.android.lpmfoundations.luxe.SupportedPaymentMethod
import com.stripe.android.lpmfoundations.paymentmethod.definitions.ExternalPaymentMethodUiDefinitionFactory
import com.stripe.android.lpmfoundations.paymentmethod.definitions.LinkCardBrandDefinition
import com.stripe.android.lpmfoundations.paymentmethod.link.LinkInlineConfiguration
import com.stripe.android.model.ElementsSession
import com.stripe.android.model.LinkMode
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.SetupIntent
import com.stripe.android.model.StripeIntent
import com.stripe.android.payments.financialconnections.FinancialConnectionsAvailability
import com.stripe.android.payments.financialconnections.GetFinancialConnectionsAvailability
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.addresselement.AddressDetails
import com.stripe.android.paymentsheet.model.PaymentMethodIncentive
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.toPaymentMethodIncentive
import com.stripe.android.paymentsheet.state.LinkState
import com.stripe.android.ui.core.Amount
import com.stripe.android.ui.core.cbc.CardBrandChoiceEligibility
import com.stripe.android.ui.core.elements.ExternalPaymentMethodSpec
import com.stripe.android.ui.core.elements.SharedDataSpec
import com.stripe.android.uicore.elements.FormElement
import kotlinx.parcelize.Parcelize

internal const val IS_PAYMENT_METHOD_SET_AS_DEFAULT_ENABLED_DEFAULT_VALUE = false

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
    val displayableCustomPaymentMethods: List<DisplayableCustomPaymentMethod>,
    val externalPaymentMethodSpecs: List<ExternalPaymentMethodSpec>,
    val customerMetadata: CustomerMetadata?,
    val isGooglePayReady: Boolean,
    val linkInlineConfiguration: LinkInlineConfiguration?,
    val paymentMethodSaveConsentBehavior: PaymentMethodSaveConsentBehavior,
    val linkMode: LinkMode?,
    val linkState: LinkState?,
    val paymentMethodIncentive: PaymentMethodIncentive?,
    val financialConnectionsAvailability: FinancialConnectionsAvailability?,
    val cardBrandFilter: CardBrandFilter,
    val elementsSessionId: String
) : Parcelable {
    fun hasIntentToSetup(): Boolean {
        return when (stripeIntent) {
            is PaymentIntent -> stripeIntent.setupFutureUsage != null
            is SetupIntent -> true
        }
    }

    fun requiresMandate(paymentMethodCode: String): Boolean {
        return PaymentMethodRegistry.definitionsByCode[paymentMethodCode]?.requiresMandate(this) ?: false
    }

    fun supportedPaymentMethodTypes(): List<String> {
        return supportedPaymentMethodDefinitions().map { paymentMethodDefinition ->
            paymentMethodDefinition.type.code
        }.plus(externalPaymentMethodTypes()).run {
            if (paymentMethodOrder.isEmpty()) {
                // Optimization to early out if we don't have a client side order.
                this
            } else {
                val orderedPaymentMethodTypes = orderedPaymentMethodTypes().mapOrderToIndex()
                sortedBy { code ->
                    orderedPaymentMethodTypes[code]
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
        return if (isExternalPaymentMethod(code)) {
            getUiDefinitionFactoryForExternalPaymentMethod(code)?.createSupportedPaymentMethod()
        } else {
            val definition = supportedPaymentMethodDefinitions().firstOrNull { it.type.code == code } ?: return null
            definition.uiDefinitionFactory().supportedPaymentMethod(this, definition, sharedDataSpecs)
        }
    }

    fun sortedSupportedPaymentMethods(): List<SupportedPaymentMethod> {
        return supportedPaymentMethodTypes().mapNotNull { supportedPaymentMethodForCode(it) }
    }

    private fun orderedPaymentMethodTypes(): List<String> {
        val originalOrderedTypes = stripeIntent.paymentMethodTypes.plus(externalPaymentMethodTypes()).toMutableList()
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

    private fun externalPaymentMethodTypes(): List<String> {
        return externalPaymentMethodSpecs.map { it.type }
    }

    fun isExternalPaymentMethod(code: String): Boolean {
        return externalPaymentMethodTypes().contains(code)
    }

    private fun getUiDefinitionFactoryForExternalPaymentMethod(code: String): UiDefinitionFactory.Simple? {
        val externalPaymentMethodSpecForCode = externalPaymentMethodSpecs.firstOrNull { it.type == code } ?: return null
        return ExternalPaymentMethodUiDefinitionFactory(externalPaymentMethodSpecForCode)
    }

    private fun supportedPaymentMethodDefinitions(): List<PaymentMethodDefinition> {
        val supportedPaymentMethodTypes = stripeIntent.paymentMethodTypes.mapNotNull {
            PaymentMethodRegistry.definitionsByCode[it]
        }.filter {
            it.isSupported(this)
        }

        val syntheticPaymentMethodTypes = listOf(LinkCardBrandDefinition).filter {
            it.isSupported(this)
        }

        val paymentMethodTypes = supportedPaymentMethodTypes + syntheticPaymentMethodTypes

        return paymentMethodTypes.filterNot {
            stripeIntent.isLiveMode &&
                stripeIntent.unactivatedPaymentMethods.contains(it.type.code)
        }.filter { paymentMethodDefinition ->
            paymentMethodDefinition.uiDefinitionFactory().canBeDisplayedInUi(paymentMethodDefinition, sharedDataSpecs)
        }
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

    fun formHeaderInformationForCode(
        code: String,
        customerHasSavedPaymentMethods: Boolean,
    ): FormHeaderInformation? {
        return if (isExternalPaymentMethod(code)) {
            getUiDefinitionFactoryForExternalPaymentMethod(code)?.createFormHeaderInformation(
                customerHasSavedPaymentMethods = customerHasSavedPaymentMethods,
                incentive = null,
            )
        } else {
            val definition = supportedPaymentMethodDefinitions().firstOrNull { it.type.code == code } ?: return null

            definition.uiDefinitionFactory().formHeaderInformation(
                metadata = this,
                definition = definition,
                sharedDataSpecs = sharedDataSpecs,
                customerHasSavedPaymentMethods = customerHasSavedPaymentMethods,
            )
        }
    }

    fun formElementsForCode(
        code: String,
        uiDefinitionFactoryArgumentsFactory: UiDefinitionFactory.Arguments.Factory,
    ): List<FormElement>? {
        return if (isExternalPaymentMethod(code)) {
            getUiDefinitionFactoryForExternalPaymentMethod(code)?.createFormElements(
                this,
                uiDefinitionFactoryArgumentsFactory.create(this, requiresMandate = false)
            )
        } else {
            val definition = supportedPaymentMethodDefinitions().firstOrNull { it.type.code == code } ?: return null

            definition.uiDefinitionFactory().formElements(
                metadata = this,
                definition = definition,
                sharedDataSpecs = sharedDataSpecs,
                arguments = uiDefinitionFactoryArgumentsFactory.create(
                    metadata = this,
                    requiresMandate = definition.requiresMandate(this),
                ),
            )
        }
    }

    fun allowRedisplay(
        customerRequestedSave: PaymentSelection.CustomerRequestedSave,
    ): PaymentMethod.AllowRedisplay {
        return paymentMethodSaveConsentBehavior.allowRedisplay(
            isSetupIntent = hasIntentToSetup(),
            customerRequestedSave = customerRequestedSave,
        )
    }

    internal companion object {
        internal fun createForPaymentElement(
            elementsSession: ElementsSession,
            configuration: CommonConfiguration,
            sharedDataSpecs: List<SharedDataSpec>,
            externalPaymentMethodSpecs: List<ExternalPaymentMethodSpec>,
            isGooglePayReady: Boolean,
            linkInlineConfiguration: LinkInlineConfiguration?,
            linkState: LinkState?,
        ): PaymentMethodMetadata {
            val linkSettings = elementsSession.linkSettings
            val customerMetadata =
                CustomerMetadata(
                    hasCustomerConfiguration = configuration.customer != null,
                    isPaymentMethodSetAsDefaultEnabled = getDefaultPaymentMethodsEnabled(elementsSession)
                )

            return PaymentMethodMetadata(
                stripeIntent = elementsSession.stripeIntent,
                billingDetailsCollectionConfiguration = configuration.billingDetailsCollectionConfiguration,
                allowsDelayedPaymentMethods = configuration.allowsDelayedPaymentMethods,
                allowsPaymentMethodsRequiringShippingAddress = configuration
                    .allowsPaymentMethodsRequiringShippingAddress,
                paymentMethodOrder = configuration.paymentMethodOrder,
                cbcEligibility = CardBrandChoiceEligibility.create(
                    isEligible = elementsSession.cardBrandChoice?.eligible ?: false,
                    preferredNetworks = configuration.preferredNetworks,
                ),
                merchantName = configuration.merchantDisplayName,
                defaultBillingDetails = configuration.defaultBillingDetails,
                shippingDetails = configuration.shippingDetails,
                customerMetadata = customerMetadata,
                sharedDataSpecs = sharedDataSpecs,
                externalPaymentMethodSpecs = externalPaymentMethodSpecs,
                paymentMethodSaveConsentBehavior = elementsSession.toPaymentSheetSaveConsentBehavior(),
                linkInlineConfiguration = linkInlineConfiguration,
                linkMode = linkSettings?.linkMode,
                linkState = linkState,
                paymentMethodIncentive = linkSettings?.linkConsumerIncentive?.toPaymentMethodIncentive(),
                isGooglePayReady = isGooglePayReady,
                displayableCustomPaymentMethods = elementsSession.toDisplayableCustomPaymentMethods(configuration),
                cardBrandFilter = PaymentSheetCardBrandFilter(configuration.cardBrandAcceptance),
                financialConnectionsAvailability = GetFinancialConnectionsAvailability(elementsSession),
                elementsSessionId = elementsSession.elementsSessionId
            )
        }

        internal fun createForCustomerSheet(
            elementsSession: ElementsSession,
            configuration: CustomerSheet.Configuration,
            paymentMethodSaveConsentBehavior: PaymentMethodSaveConsentBehavior,
            sharedDataSpecs: List<SharedDataSpec>,
            isGooglePayReady: Boolean,
            isPaymentMethodSyncDefaultEnabled: Boolean,
        ): PaymentMethodMetadata {
            return PaymentMethodMetadata(
                stripeIntent = elementsSession.stripeIntent,
                billingDetailsCollectionConfiguration = configuration.billingDetailsCollectionConfiguration,
                allowsDelayedPaymentMethods = true,
                allowsPaymentMethodsRequiringShippingAddress = false,
                paymentMethodOrder = configuration.paymentMethodOrder,
                cbcEligibility = CardBrandChoiceEligibility.create(
                    isEligible = elementsSession.cardBrandChoice?.eligible ?: false,
                    preferredNetworks = configuration.preferredNetworks,
                ),
                merchantName = configuration.merchantDisplayName,
                defaultBillingDetails = configuration.defaultBillingDetails,
                shippingDetails = null,
                customerMetadata = CustomerMetadata(
                    hasCustomerConfiguration = true,
                    isPaymentMethodSetAsDefaultEnabled = isPaymentMethodSyncDefaultEnabled,
                ),
                sharedDataSpecs = sharedDataSpecs,
                isGooglePayReady = isGooglePayReady,
                linkInlineConfiguration = null,
                paymentMethodSaveConsentBehavior = paymentMethodSaveConsentBehavior,
                linkMode = elementsSession.linkSettings?.linkMode,
                linkState = null,
                paymentMethodIncentive = null,
                externalPaymentMethodSpecs = emptyList(),
                displayableCustomPaymentMethods = emptyList(),
                cardBrandFilter = PaymentSheetCardBrandFilter(configuration.cardBrandAcceptance),
                elementsSessionId = elementsSession.elementsSessionId,
                financialConnectionsAvailability = GetFinancialConnectionsAvailability(elementsSession)
            )
        }

        internal fun createForNativeLink(
            configuration: LinkConfiguration,
        ): PaymentMethodMetadata {
            return PaymentMethodMetadata(
                stripeIntent = configuration.stripeIntent,
                billingDetailsCollectionConfiguration = ConfigurationDefaults.billingDetailsCollectionConfiguration,
                allowsDelayedPaymentMethods = false,
                allowsPaymentMethodsRequiringShippingAddress = false,
                paymentMethodOrder = ConfigurationDefaults.paymentMethodOrder,
                cbcEligibility = CardBrandChoiceEligibility.create(
                    isEligible = false,
                    preferredNetworks = emptyList(),
                ),
                merchantName = configuration.merchantName,
                defaultBillingDetails = null,
                shippingDetails = null,
                customerMetadata = CustomerMetadata(
                    hasCustomerConfiguration = true,
                    isPaymentMethodSetAsDefaultEnabled = false,
                ),
                sharedDataSpecs = emptyList(),
                externalPaymentMethodSpecs = emptyList(),
                paymentMethodSaveConsentBehavior = PaymentMethodSaveConsentBehavior.Disabled(null),
                linkInlineConfiguration = null,
                linkMode = null,
                linkState = null,
                paymentMethodIncentive = null,
                isGooglePayReady = false,
                displayableCustomPaymentMethods = emptyList(),
                cardBrandFilter = PaymentSheetCardBrandFilter(PaymentSheet.CardBrandAcceptance.all()),
                elementsSessionId = configuration.elementsSessionId,
                financialConnectionsAvailability = GetFinancialConnectionsAvailability(elementsSession = null)
            )
        }

        private fun getDefaultPaymentMethodsEnabled(elementsSession: ElementsSession): Boolean {
            val mobilePaymentElement = elementsSession.customer?.session?.components?.mobilePaymentElement
                as? ElementsSession.Customer.Components.MobilePaymentElement.Enabled
            return mobilePaymentElement?.isPaymentMethodSetAsDefaultEnabled
                ?: false
        }
    }
}
