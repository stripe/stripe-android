package com.stripe.android.lpmfoundations.paymentmethod.definitions

import com.stripe.android.core.model.CountryUtils
import com.stripe.android.lpmfoundations.luxe.SupportedPaymentMethod
import com.stripe.android.lpmfoundations.paymentmethod.AddPaymentMethodRequirement
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodDefinition
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.UiDefinitionFactory
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.ui.core.BillingDetailsCollectionConfiguration
import com.stripe.android.ui.core.R
import com.stripe.android.ui.core.elements.CardBillingAddressElement
import com.stripe.android.ui.core.elements.CardDetailsSectionElement
import com.stripe.android.ui.core.elements.EmailElement
import com.stripe.android.ui.core.elements.SaveForFutureUseElement
import com.stripe.android.uicore.address.AddressRepository
import com.stripe.android.uicore.elements.FormElement
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.PhoneNumberController
import com.stripe.android.uicore.elements.PhoneNumberElement
import com.stripe.android.uicore.elements.SameAsShippingController
import com.stripe.android.uicore.elements.SameAsShippingElement
import com.stripe.android.uicore.elements.SectionElement

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
        displayNameResource = R.string.stripe_paymentsheet_payment_method_card,
        iconResource = R.drawable.stripe_ic_paymentsheet_pm_card,
        tintIconOnSelection = true,
    )

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
                    context = arguments.context,
                    initialValues = arguments.initialValues,
                    identifier = IdentifierSpec.Generic("card_details"),
                    collectName = billingDetailsCollectionConfiguration.collectsName,
                    cbcEligibility = arguments.cbcEligibility,
                )
            )

            if (billingDetailsCollectionConfiguration.address
                != PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Never
            ) {
                add(
                    cardBillingElement(
                        billingDetailsCollectionConfiguration.address.toInternal(),
                        arguments.initialValues,
                        arguments.addressRepository,
                        arguments.shippingValues,
                    )
                )
            }
            add(SaveForFutureUseElement(arguments.saveForFutureUseInitialValue, arguments.merchantName))
        }
    }
}

internal fun PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode
.toInternal(): BillingDetailsCollectionConfiguration.AddressCollectionMode {
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

private fun cardBillingElement(
    collectionMode: BillingDetailsCollectionConfiguration.AddressCollectionMode,
    initialValues: Map<IdentifierSpec, String?>,
    addressRepository: AddressRepository,
    shippingValues: Map<IdentifierSpec, String?>?,
): FormElement {
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
        addressRepository = addressRepository,
        countryCodes = CountryUtils.supportedBillingCountries,
        rawValuesMap = initialValues,
        sameAsShippingElement = sameAsShippingElement,
        shippingValuesMap = shippingValues,
        collectionMode = collectionMode,
    )

    return SectionElement.wrap(
        listOfNotNull(
            addressElement,
            sameAsShippingElement
        ),
        R.string.stripe_billing_details
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
            controller = PhoneNumberController(initialValues[IdentifierSpec.Phone] ?: "")
        ).takeIf { collectPhone },
    )

    if (elements.isEmpty()) return null

    return SectionElement.wrap(
        label = R.string.stripe_contact_information,
        sectionFieldElements = elements,
    )
}
