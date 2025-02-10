package com.stripe.android.lpmfoundations.paymentmethod.definitions

import androidx.compose.ui.unit.dp
import com.stripe.android.core.model.CountryUtils
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.link.ui.inline.LinkSignupMode
import com.stripe.android.lpmfoundations.FormHeaderInformation
import com.stripe.android.lpmfoundations.luxe.SupportedPaymentMethod
import com.stripe.android.lpmfoundations.luxe.isSaveForFutureUseValueChangeable
import com.stripe.android.lpmfoundations.paymentmethod.AddPaymentMethodRequirement
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodDefinition
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.UiDefinitionFactory
import com.stripe.android.lpmfoundations.paymentmethod.link.LinkFormElement
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.model.PaymentMethodIncentive
import com.stripe.android.ui.core.BillingDetailsCollectionConfiguration
import com.stripe.android.ui.core.elements.CardBillingAddressElement
import com.stripe.android.ui.core.elements.CardDetailsSectionElement
import com.stripe.android.ui.core.elements.EmailElement
import com.stripe.android.ui.core.elements.MandateTextElement
import com.stripe.android.ui.core.elements.SaveForFutureUseElement
import com.stripe.android.uicore.elements.FormElement
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.PhoneNumberController
import com.stripe.android.uicore.elements.PhoneNumberElement
import com.stripe.android.uicore.elements.SameAsShippingController
import com.stripe.android.uicore.elements.SameAsShippingElement
import com.stripe.android.uicore.elements.SectionElement
import com.stripe.android.paymentsheet.R as PaymentSheetR
import com.stripe.android.ui.core.R as PaymentsUiCoreR

internal object CardDefinition : PaymentMethodDefinition {
    override val type: PaymentMethod.Type = PaymentMethod.Type.Card

    override val supportedAsSavedPaymentMethod: Boolean = true

    override fun requirementsToBeUsedAsNewPaymentMethod(
        hasIntentToSetup: Boolean
    ): Set<AddPaymentMethodRequirement> = setOf()

    override fun requiresMandate(metadata: PaymentMethodMetadata): Boolean = false

    override fun uiDefinitionFactory(): UiDefinitionFactory = CardUiDefinitionFactory
}

private object CardUiDefinitionFactory : UiDefinitionFactory.Simple {
    override fun createSupportedPaymentMethod() = SupportedPaymentMethod(
        paymentMethodDefinition = CardDefinition,
        displayNameResource = PaymentsUiCoreR.string.stripe_paymentsheet_payment_method_card,
        iconResource = PaymentsUiCoreR.drawable.stripe_ic_paymentsheet_pm_card,
        iconRequiresTinting = true,
    )

    override fun createFormHeaderInformation(
        customerHasSavedPaymentMethods: Boolean,
        incentive: PaymentMethodIncentive?,
    ): FormHeaderInformation {
        val displayName = if (customerHasSavedPaymentMethods) {
            PaymentsUiCoreR.string.stripe_paymentsheet_add_new_card
        } else {
            PaymentsUiCoreR.string.stripe_paymentsheet_add_card
        }
        return createSupportedPaymentMethod().asFormHeaderInformation(incentive).copy(
            displayName = displayName.resolvableString,
            shouldShowIcon = false,
        )
    }

    override fun createFormElements(
        metadata: PaymentMethodMetadata,
        arguments: UiDefinitionFactory.Arguments,
    ): List<FormElement> {
        val billingDetailsCollectionConfiguration = metadata.billingDetailsCollectionConfiguration
        return buildList {
            val contactInformationElement = contactInformationElement(
                initialValues = arguments.initialValues,
                collectEmail = billingDetailsCollectionConfiguration.collectsEmail,
                collectPhone = billingDetailsCollectionConfiguration.collectsPhone,
            )

            if (contactInformationElement != null) {
                add(contactInformationElement)
            }

            add(
                CardDetailsSectionElement(
                    cardAccountRangeRepositoryFactory = arguments.cardAccountRangeRepositoryFactory,
                    initialValues = arguments.initialValues,
                    identifier = IdentifierSpec.Generic("card_details"),
                    collectName = billingDetailsCollectionConfiguration.collectsName,
                    cbcEligibility = arguments.cbcEligibility,
                    cardBrandFilter = arguments.cardBrandFilter
                )
            )

            if (billingDetailsCollectionConfiguration.address
                != PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Never
            ) {
                addAll(
                    cardBillingElements(
                        billingDetailsCollectionConfiguration.address.toInternal(),
                        arguments.initialValues,
                        arguments.shippingValues,
                    )
                )
            }

            val canChangeSaveForFutureUsage = saveForFutureUsageIsChangeable(metadata)

            if (canChangeSaveForFutureUsage) {
                add(SaveForFutureUseElement(arguments.saveForFutureUseInitialValue, arguments.merchantName))
            }

            val signupMode = if (
                metadata.linkInlineConfiguration != null && arguments.linkConfigurationCoordinator != null
            ) {
                add(
                    LinkFormElement(
                        configuration = metadata.linkInlineConfiguration,
                        linkConfigurationCoordinator = arguments.linkConfigurationCoordinator,
                        initialLinkUserInput = arguments.initialLinkUserInput,
                        onLinkInlineSignupStateChanged = arguments.onLinkInlineSignupStateChanged,
                    )
                )

                metadata.linkInlineConfiguration.signupMode
            } else {
                null
            }

            if (metadata.hasIntentToSetup()) {
                add(createCardMandateElement(metadata.merchantName, signupMode, canChangeSaveForFutureUsage))
            }
        }
    }

    private fun createCardMandateElement(
        merchantName: String,
        signupMode: LinkSignupMode?,
        canChangeSaveForFutureUse: Boolean,
    ): MandateTextElement {
        return MandateTextElement(
            identifier = IdentifierSpec.Generic("card_mandate"),
            stringResId = PaymentSheetR.string.stripe_paymentsheet_card_mandate,
            topPadding = when {
                signupMode == LinkSignupMode.AlongsideSaveForFutureUse -> 0.dp
                signupMode == LinkSignupMode.InsteadOfSaveForFutureUse -> 4.dp
                canChangeSaveForFutureUse -> 6.dp
                else -> 2.dp
            },
            args = listOf(merchantName),
        )
    }

    private fun saveForFutureUsageIsChangeable(metadata: PaymentMethodMetadata): Boolean {
        return isSaveForFutureUseValueChangeable(
            code = PaymentMethod.Type.Card.code,
            intent = metadata.stripeIntent,
            paymentMethodSaveConsentBehavior = metadata.paymentMethodSaveConsentBehavior,
            hasCustomerConfiguration = metadata.customerMetadata != null,
        )
    }
}

internal fun PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.toInternal(): BillingDetailsCollectionConfiguration.AddressCollectionMode {
    return when (this) {
        PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Automatic -> {
            BillingDetailsCollectionConfiguration.AddressCollectionMode.Automatic
        }

        PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Never -> {
            BillingDetailsCollectionConfiguration.AddressCollectionMode.Never
        }

        PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full -> {
            BillingDetailsCollectionConfiguration.AddressCollectionMode.Full
        }
    }
}

private fun cardBillingElements(
    collectionMode: BillingDetailsCollectionConfiguration.AddressCollectionMode,
    initialValues: Map<IdentifierSpec, String?>,
    shippingValues: Map<IdentifierSpec, String?>?,
): List<FormElement> {
    val sameAsShippingElement =
        shippingValues?.get(IdentifierSpec.SameAsShipping)
            ?.toBooleanStrictOrNull()
            ?.let {
                SameAsShippingElement(
                    identifier = IdentifierSpec.SameAsShipping,
                    controller = SameAsShippingController(it)
                )
            }
    val addressElement = CardBillingAddressElement(
        IdentifierSpec.Generic("credit_billing"),
        countryCodes = CountryUtils.supportedBillingCountries,
        rawValuesMap = initialValues,
        sameAsShippingElement = sameAsShippingElement,
        shippingValuesMap = shippingValues,
        collectionMode = collectionMode,
    )

    return listOfNotNull(
        SectionElement.wrap(
            addressElement,
            PaymentsUiCoreR.string.stripe_billing_details,
        ),
        sameAsShippingElement,
    )
}

private fun contactInformationElement(
    initialValues: Map<IdentifierSpec, String?>,
    collectEmail: Boolean,
    collectPhone: Boolean,
): FormElement? {
    val elements = listOfNotNull(
        EmailElement(
            initialValue = initialValues[IdentifierSpec.Email]
        ).takeIf { collectEmail },
        PhoneNumberElement(
            identifier = IdentifierSpec.Phone,
            controller = PhoneNumberController.createPhoneNumberController(
                initialValue = initialValues[IdentifierSpec.Phone] ?: "",
            )
        ).takeIf { collectPhone },
    )

    if (elements.isEmpty()) return null

    return SectionElement.wrap(
        label = PaymentsUiCoreR.string.stripe_contact_information,
        sectionFieldElements = elements,
    )
}
