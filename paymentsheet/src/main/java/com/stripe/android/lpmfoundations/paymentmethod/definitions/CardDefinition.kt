package com.stripe.android.lpmfoundations.paymentmethod.definitions

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.stripe.android.core.model.CountryUtils
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.link.ui.inline.InlineSignupViewState
import com.stripe.android.link.ui.inline.LinkSignupMode
import com.stripe.android.link.ui.replaceHyperlinks
import com.stripe.android.lpmfoundations.FormHeaderInformation
import com.stripe.android.lpmfoundations.luxe.SupportedPaymentMethod
import com.stripe.android.lpmfoundations.luxe.addSavePaymentOptionElements
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
import com.stripe.android.ui.core.elements.Mandate
import com.stripe.android.ui.core.elements.MandateTextElement
import com.stripe.android.ui.core.elements.RenderableFormElement
import com.stripe.android.uicore.elements.AutocompleteAddressInteractor
import com.stripe.android.uicore.elements.FormElement
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.PhoneNumberController
import com.stripe.android.uicore.elements.PhoneNumberElement
import com.stripe.android.uicore.elements.SameAsShippingController
import com.stripe.android.uicore.elements.SameAsShippingElement
import com.stripe.android.uicore.elements.SectionElement
import com.stripe.android.uicore.forms.FormFieldEntry
import com.stripe.android.uicore.utils.collectAsState
import com.stripe.android.uicore.utils.stateFlowOf
import kotlinx.coroutines.flow.StateFlow
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
        iconResourceNight = null,
        outlinedIconResource = PaymentsUiCoreR.drawable.stripe_ic_paymentsheet_pm_card_outlined,
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
            addContactInformationElement(
                arguments = arguments,
                billingDetailsCollectionConfiguration = metadata.billingDetailsCollectionConfiguration,
            )

            add(
                CardDetailsSectionElement(
                    cardAccountRangeRepositoryFactory = arguments.cardAccountRangeRepositoryFactory,
                    initialValues = arguments.initialValues,
                    identifier = IdentifierSpec.Generic("card_details"),
                    collectName = billingDetailsCollectionConfiguration.collectsName,
                    cbcEligibility = arguments.cbcEligibility,
                    cardBrandFilter = arguments.cardBrandFilter,
                    elementsSessionId = metadata.elementsSessionId
                )
            )

            addCardBillingElements(
                arguments = arguments,
                billingDetailsCollectionConfiguration = metadata.billingDetailsCollectionConfiguration,
            )

            val canChangeSaveForFutureUsage = isSaveForFutureUseValueChangeable(
                code = PaymentMethod.Type.Card.code,
                metadata = metadata
            )

            val linkSignupOptInEnabled =
                metadata.linkState?.configuration?.linkSignUpOptInFeatureEnabled == true

            // sign up opt in combines save for future usage and link signup acceptance
            if (canChangeSaveForFutureUsage && linkSignupOptInEnabled.not()) {
                addSavePaymentOptionElements(
                    metadata = metadata,
                    arguments = arguments,
                )
            }

            val signupMode = if (
                metadata.linkState?.signupMode != null && arguments.linkConfigurationCoordinator != null
            ) {
                add(
                    LinkFormElement(
                        signupMode = metadata.linkState.signupMode,
                        configuration = metadata.linkState.configuration,
                        linkConfigurationCoordinator = arguments.linkConfigurationCoordinator,
                        initialLinkUserInput = arguments.initialLinkUserInput,
                        onLinkInlineSignupStateChanged = arguments.onLinkInlineSignupStateChanged,
                    )
                )

                metadata.linkState.signupMode
            } else {
                null
            }

            if (linkSignupOptInEnabled && signupMode != null) {
                add(
                    CombinedLinkMandateElement(
                        identifier = IdentifierSpec.Generic("card_mandate"),
                        merchantName = metadata.merchantName,
                        signupMode = signupMode,
                        canChangeSaveForFutureUse = canChangeSaveForFutureUsage,
                        linkSignupStateFlow = arguments.linkInlineHandler?.linkInlineState ?: stateFlowOf(null)
                    )
                )
            } else if (metadata.hasIntentToSetup(CardDefinition.type.code)) {
                add(
                    MandateTextElement(
                        identifier = IdentifierSpec.Generic("card_mandate"),
                        stringResId = PaymentSheetR.string.stripe_paymentsheet_card_mandate,
                        topPadding = when {
                            signupMode == LinkSignupMode.AlongsideSaveForFutureUse -> 0.dp
                            signupMode == LinkSignupMode.InsteadOfSaveForFutureUse -> 4.dp
                            canChangeSaveForFutureUsage -> 6.dp
                            else -> 2.dp
                        },
                        args = listOf(metadata.merchantName),
                    )
                )
            }
        }
    }

    private fun MutableList<FormElement>.addContactInformationElement(
        arguments: UiDefinitionFactory.Arguments,
        billingDetailsCollectionConfiguration: PaymentSheet.BillingDetailsCollectionConfiguration,
    ): Boolean {
        val contactInformationElement = contactInformationElement(
            initialValues = arguments.initialValues,
            collectEmail = billingDetailsCollectionConfiguration.collectsEmail,
            collectPhone = billingDetailsCollectionConfiguration.collectsPhone,
        )

        return if (contactInformationElement != null) {
            add(contactInformationElement)
        } else {
            false
        }
    }

    private fun MutableList<FormElement>.addCardBillingElements(
        arguments: UiDefinitionFactory.Arguments,
        billingDetailsCollectionConfiguration: PaymentSheet.BillingDetailsCollectionConfiguration,
    ): Boolean {
        return if (billingDetailsCollectionConfiguration.address
            != PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Never
        ) {
            addAll(
                cardBillingElements(
                    billingDetailsCollectionConfiguration.address.toInternal(),
                    arguments.autocompleteAddressInteractorFactory,
                    arguments.initialValues,
                    arguments.shippingValues,
                )
            )
        } else {
            false
        }
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
    autocompleteAddressInteractorFactory: AutocompleteAddressInteractor.Factory?,
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
        autocompleteAddressInteractorFactory = autocompleteAddressInteractorFactory,
    )

    return listOfNotNull(
        SectionElement.wrap(
            addressElement,
            resolvableString(PaymentsUiCoreR.string.stripe_billing_details),
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
        label = resolvableString(PaymentsUiCoreR.string.stripe_contact_information),
        sectionFieldElements = elements,
    )
}

internal class CombinedLinkMandateElement(
    identifier: IdentifierSpec,
    signupMode: LinkSignupMode?,
    canChangeSaveForFutureUse: Boolean,
    private val merchantName: String,
    private val linkSignupStateFlow: StateFlow<InlineSignupViewState?>,
) : RenderableFormElement(
    allowsUserInteraction = false,
    identifier = identifier
) {
    override fun getFormFieldValueFlow() = stateFlowOf(emptyList<Pair<IdentifierSpec, FormFieldEntry>>())

    private val topPadding = when {
        signupMode == LinkSignupMode.AlongsideSaveForFutureUse -> 0.dp
        signupMode == LinkSignupMode.InsteadOfSaveForFutureUse -> 4.dp
        canChangeSaveForFutureUse -> 6.dp
        else -> 2.dp
    }

    @Composable
    override fun ComposeUI(enabled: Boolean) {
        val linkState by linkSignupStateFlow.collectAsState()
        Mandate(
            mandateText = if (linkState?.isExpanded == true) {
                stringResource(
                    id = PaymentSheetR.string.stripe_paymentsheet_card_mandate_signup_toggle_on,
                    formatArgs = arrayOf(merchantName)
                ).replaceHyperlinks()
            } else {
                stringResource(
                    id = PaymentSheetR.string.stripe_paymentsheet_card_mandate_signup_toggle_off,
                    formatArgs = arrayOf(merchantName)
                ).replaceHyperlinks()
            },
            modifier = Modifier.padding(top = topPadding)
        )
    }
}
