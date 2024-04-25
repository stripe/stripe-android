package com.stripe.android.lpmfoundations.paymentmethod

import com.stripe.android.cards.CardAccountRangeRepository
import com.stripe.android.lpmfoundations.luxe.InitialValuesFactory
import com.stripe.android.lpmfoundations.luxe.SupportedPaymentMethod
import com.stripe.android.lpmfoundations.luxe.TransformSpecToElements
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodExtraParams
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.addresselement.toIdentifierMap
import com.stripe.android.ui.core.Amount
import com.stripe.android.ui.core.cbc.CardBrandChoiceEligibility
import com.stripe.android.ui.core.elements.SharedDataSpec
import com.stripe.android.uicore.elements.FormElement
import com.stripe.android.uicore.elements.IdentifierSpec

internal sealed interface UiDefinitionFactory {
    class Arguments(
        val cardAccountRangeRepositoryFactory: CardAccountRangeRepository.Factory,
        val initialValues: Map<IdentifierSpec, String?>,
        val shippingValues: Map<IdentifierSpec, String?>?,
        val amount: Amount?,
        val saveForFutureUseInitialValue: Boolean,
        val merchantName: String,
        val cbcEligibility: CardBrandChoiceEligibility,
        val billingDetailsCollectionConfiguration: PaymentSheet.BillingDetailsCollectionConfiguration,
        val requiresMandate: Boolean,
    ) {
        interface Factory {
            fun create(
                metadata: PaymentMethodMetadata,
                requiresMandate: Boolean,
            ): Arguments

            class Default(
                private val cardAccountRangeRepositoryFactory: CardAccountRangeRepository.Factory,
                private val paymentMethodCreateParams: PaymentMethodCreateParams? = null,
                private val paymentMethodExtraParams: PaymentMethodExtraParams? = null,
            ) : Factory {
                override fun create(
                    metadata: PaymentMethodMetadata,
                    requiresMandate: Boolean,
                ): Arguments {
                    return Arguments(
                        cardAccountRangeRepositoryFactory = cardAccountRangeRepositoryFactory,
                        amount = metadata.amount(),
                        merchantName = metadata.merchantName,
                        cbcEligibility = metadata.cbcEligibility,
                        initialValues = InitialValuesFactory.create(
                            defaultBillingDetails = metadata.defaultBillingDetails,
                            paymentMethodCreateParams = paymentMethodCreateParams,
                            paymentMethodExtraParams = paymentMethodExtraParams,
                        ),
                        shippingValues = metadata.shippingDetails?.toIdentifierMap(metadata.defaultBillingDetails),
                        saveForFutureUseInitialValue = false,
                        billingDetailsCollectionConfiguration = metadata.billingDetailsCollectionConfiguration,
                        requiresMandate = requiresMandate,
                    )
                }
            }
        }
    }

    interface RequiresSharedDataSpec : UiDefinitionFactory {
        fun createSupportedPaymentMethod(
            sharedDataSpec: SharedDataSpec,
        ): SupportedPaymentMethod

        fun createFormElements(
            metadata: PaymentMethodMetadata,
            sharedDataSpec: SharedDataSpec,
            transformSpecToElements: TransformSpecToElements,
        ): List<FormElement> {
            return transformSpecToElements.transform(
                specs = sharedDataSpec.fields,
            )
        }
    }

    interface Simple : UiDefinitionFactory {
        fun createSupportedPaymentMethod(): SupportedPaymentMethod
        fun createFormElements(metadata: PaymentMethodMetadata, arguments: Arguments): List<FormElement>
    }

    fun canBeDisplayedInUi(
        definition: PaymentMethodDefinition,
        sharedDataSpecs: List<SharedDataSpec>,
    ): Boolean = when (this) {
        is Simple -> {
            true
        }

        is RequiresSharedDataSpec -> {
            sharedDataSpecs.firstOrNull { it.type == definition.type.code } != null
        }
    }

    fun supportedPaymentMethod(
        definition: PaymentMethodDefinition,
        sharedDataSpecs: List<SharedDataSpec>,
    ): SupportedPaymentMethod? = when (this) {
        is Simple -> {
            createSupportedPaymentMethod()
        }

        is RequiresSharedDataSpec -> {
            val sharedDataSpec = sharedDataSpecs.firstOrNull { it.type == definition.type.code }
            if (sharedDataSpec != null) {
                createSupportedPaymentMethod(sharedDataSpec)
            } else {
                null
            }
        }
    }

    fun formElements(
        definition: PaymentMethodDefinition,
        metadata: PaymentMethodMetadata,
        sharedDataSpecs: List<SharedDataSpec>,
        arguments: Arguments,
    ): List<FormElement>? = when (this) {
        is Simple -> {
            createFormElements(
                metadata = metadata,
                arguments = arguments,
            )
        }

        is RequiresSharedDataSpec -> {
            val sharedDataSpec = sharedDataSpecs.firstOrNull { it.type == definition.type.code }
            if (sharedDataSpec != null) {
                createFormElements(
                    metadata = metadata,
                    sharedDataSpec = sharedDataSpec,
                    transformSpecToElements = TransformSpecToElements(arguments),
                )
            } else {
                null
            }
        }
    }
}
