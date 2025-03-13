package com.stripe.android.lpmfoundations.paymentmethod

import com.stripe.android.CardBrandFilter
import com.stripe.android.cards.CardAccountRangeRepository
import com.stripe.android.link.LinkConfigurationCoordinator
import com.stripe.android.link.ui.inline.InlineSignupViewState
import com.stripe.android.link.ui.inline.UserInput
import com.stripe.android.lpmfoundations.FormHeaderInformation
import com.stripe.android.lpmfoundations.luxe.InitialValuesFactory
import com.stripe.android.lpmfoundations.luxe.SupportedPaymentMethod
import com.stripe.android.lpmfoundations.luxe.TransformSpecToElements
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodExtraParams
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.addresselement.toIdentifierMap
import com.stripe.android.paymentsheet.model.PaymentMethodIncentive
import com.stripe.android.ui.core.cbc.CardBrandChoiceEligibility
import com.stripe.android.ui.core.elements.HAS_OTHER_PAYMENT_METHODS_DEFAULT_VALUE
import com.stripe.android.ui.core.elements.SharedDataSpec
import com.stripe.android.uicore.elements.FormElement
import com.stripe.android.uicore.elements.IdentifierSpec

internal sealed interface UiDefinitionFactory {
    class Arguments(
        val cardAccountRangeRepositoryFactory: CardAccountRangeRepository.Factory,
        val linkConfigurationCoordinator: LinkConfigurationCoordinator?,
        val initialValues: Map<IdentifierSpec, String?>,
        val initialLinkUserInput: UserInput?,
        val shippingValues: Map<IdentifierSpec, String?>?,
        val saveForFutureUseInitialValue: Boolean,
        val merchantName: String,
        val cbcEligibility: CardBrandChoiceEligibility,
        val billingDetailsCollectionConfiguration: PaymentSheet.BillingDetailsCollectionConfiguration,
        val requiresMandate: Boolean,
        val onLinkInlineSignupStateChanged: (InlineSignupViewState) -> Unit,
        val cardBrandFilter: CardBrandFilter,
        val hasOtherPaymentMethods: Boolean,
    ) {
        interface Factory {
            fun create(
                metadata: PaymentMethodMetadata,
                requiresMandate: Boolean,
            ): Arguments

            class Default(
                private val cardAccountRangeRepositoryFactory: CardAccountRangeRepository.Factory,
                private val linkConfigurationCoordinator: LinkConfigurationCoordinator?,
                private val onLinkInlineSignupStateChanged: (InlineSignupViewState) -> Unit,
                private val paymentMethodCreateParams: PaymentMethodCreateParams? = null,
                private val paymentMethodExtraParams: PaymentMethodExtraParams? = null,
                private val initialLinkUserInput: UserInput? = null,
                private val hasOtherPaymentMethods: Boolean = HAS_OTHER_PAYMENT_METHODS_DEFAULT_VALUE,
            ) : Factory {
                override fun create(
                    metadata: PaymentMethodMetadata,
                    requiresMandate: Boolean,
                ): Arguments {
                    return Arguments(
                        cardAccountRangeRepositoryFactory = cardAccountRangeRepositoryFactory,
                        linkConfigurationCoordinator = linkConfigurationCoordinator,
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
                        onLinkInlineSignupStateChanged = onLinkInlineSignupStateChanged,
                        cardBrandFilter = metadata.cardBrandFilter,
                        initialLinkUserInput = initialLinkUserInput,
                        hasOtherPaymentMethods = hasOtherPaymentMethods
                    )
                }
            }
        }
    }

    interface RequiresSharedDataSpec : UiDefinitionFactory {
        fun createSupportedPaymentMethod(
            sharedDataSpec: SharedDataSpec,
        ): SupportedPaymentMethod

        fun createFormHeaderInformation(
            sharedDataSpec: SharedDataSpec,
            incentive: PaymentMethodIncentive?,
        ): FormHeaderInformation {
            return createSupportedPaymentMethod(sharedDataSpec).asFormHeaderInformation(incentive)
        }

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

        fun createFormHeaderInformation(
            customerHasSavedPaymentMethods: Boolean,
            incentive: PaymentMethodIncentive?,
        ): FormHeaderInformation {
            return createSupportedPaymentMethod().asFormHeaderInformation(incentive)
        }

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

    fun formHeaderInformation(
        definition: PaymentMethodDefinition,
        metadata: PaymentMethodMetadata,
        sharedDataSpecs: List<SharedDataSpec>,
        customerHasSavedPaymentMethods: Boolean,
    ): FormHeaderInformation? = when (this) {
        is Simple -> {
            createFormHeaderInformation(
                customerHasSavedPaymentMethods = customerHasSavedPaymentMethods,
                incentive = metadata.paymentMethodIncentive,
            )
        }

        is RequiresSharedDataSpec -> {
            val sharedDataSpec = sharedDataSpecs.firstOrNull { it.type == definition.type.code }
            if (sharedDataSpec != null) {
                createFormHeaderInformation(
                    sharedDataSpec = sharedDataSpec,
                    incentive = metadata.paymentMethodIncentive,
                )
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
