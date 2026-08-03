package com.stripe.android.paymentsheet.forms

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.stripe.android.common.nfcscan.NfcScanningAction
import com.stripe.android.common.taptoadd.TapToAddCardDetailsAction
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.link.ui.inline.InlineSignupViewState
import com.stripe.android.link.ui.inline.LinkSignupMode
import com.stripe.android.link.ui.replaceHyperlinks
import com.stripe.android.lpmfoundations.luxe.addSavePaymentOptionElements
import com.stripe.android.lpmfoundations.luxe.isSaveForFutureUseValueChangeable
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.UiDefinitionFactory
import com.stripe.android.lpmfoundations.paymentmethod.link.LinkFormElement
import com.stripe.android.model.LinkBrand
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.ui.core.BillingDetailsCollectionConfiguration
import com.stripe.android.ui.core.elements.CardBillingAddressElement
import com.stripe.android.ui.core.elements.CardDetailsAction
import com.stripe.android.ui.core.elements.CardDetailsSectionElement
import com.stripe.android.ui.core.elements.CardScanAction
import com.stripe.android.ui.core.elements.Mandate
import com.stripe.android.ui.core.elements.MandateTextElement
import com.stripe.android.ui.core.elements.RenderableFormElement
import com.stripe.android.uicore.elements.AutocompleteAddressInteractor
import com.stripe.android.uicore.elements.FormElement
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.SameAsShippingController
import com.stripe.android.uicore.elements.SameAsShippingElement
import com.stripe.android.uicore.elements.SectionElement
import com.stripe.android.uicore.forms.FormFieldEntry
import com.stripe.android.uicore.utils.collectAsState
import com.stripe.android.uicore.utils.stateFlowOf
import kotlinx.coroutines.flow.StateFlow
import com.stripe.android.paymentsheet.R as PaymentSheetR
import com.stripe.android.ui.core.R as PaymentsUiCoreR

/** Renders the atomic card-entry capability used by both local and Mint-owned forms. */
internal object CardDetailsRenderer {
    fun render(
        metadata: PaymentMethodMetadata,
        arguments: UiDefinitionFactory.Arguments,
        collectName: Boolean = metadata.billingDetailsCollectionConfiguration.collectsName,
    ): FormElement {
        return CardDetailsSectionElement(
            cardAccountRangeRepositoryFactory = arguments.cardAccountRangeRepositoryFactory,
            initialValues = arguments.initialValues,
            identifier = IdentifierSpec.Generic("card_details"),
            collectName = collectName,
            cbcEligibility = arguments.cbcEligibility,
            cardBrandFilter = arguments.cardBrandFilter,
            cardFundingFilter = arguments.cardFundingFilter,
            cardDetailsAction = createCardDetailsAction(metadata, arguments),
        )
    }

    private fun createCardDetailsAction(
        metadata: PaymentMethodMetadata,
        arguments: UiDefinitionFactory.Arguments,
    ): CardDetailsAction {
        return if (metadata.isTapToAddSupported && arguments.tapToAddHelper != null) {
            TapToAddCardDetailsAction(
                tapToAddHelper = arguments.tapToAddHelper,
                paymentMethodMetadata = metadata,
            )
        } else if (arguments.isNfcScanningAvailable?.get(metadata) == true) {
            NfcScanningAction(paymentMethodMetadata = metadata)
        } else {
            CardScanAction(
                isStripeCardScanAllowed = metadata.isStripeCardScanAllowed,
                enableMlKitCardScan = metadata.enableMlKitCardScan,
                disableSsdOcrCardScan = metadata.disableSsdOcrCardScan,
                automaticallyLaunchedCardScanFormDataHelper =
                    arguments.automaticallyLaunchedCardScanFormDataHelper,
            )
        }
    }
}

/** Atomic card capabilities that Mint can order and select independently. */
internal object CardFormElementRenderer {
    fun details(
        metadata: PaymentMethodMetadata,
        arguments: UiDefinitionFactory.Arguments,
    ): List<FormElement> = listOf(CardDetailsRenderer.render(metadata, arguments))

    fun billingDetails(
        metadata: PaymentMethodMetadata,
        arguments: UiDefinitionFactory.Arguments,
    ): List<FormElement> {
        val configuration = metadata.billingDetailsCollectionConfiguration
        if (
            configuration.address ==
            PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Never &&
            !configuration.collectsEmail &&
            !configuration.collectsPhone
        ) {
            return emptyList()
        }
        return cardBillingElements(
            allowedCountries = configuration.allowedBillingCountries,
            collectionConfiguration = configuration.toInternal(),
            autocompleteAddressInteractorFactory = arguments.autocompleteAddressInteractorFactory,
            initialValues = arguments.initialValues,
            shippingValues = arguments.shippingValues,
            requiresBillingAddressForAutomaticTax = arguments.requiresBillingAddressForAutomaticTax,
        )
    }

    fun savePaymentMethod(
        metadata: PaymentMethodMetadata,
        arguments: UiDefinitionFactory.Arguments,
    ): List<FormElement> {
        if (
            !canChangeSaveForFutureUse(metadata) ||
            metadata.forceSetupFutureUseBehaviorAndNewMandate
        ) {
            return emptyList()
        }
        return buildList {
            addSavePaymentOptionElements(metadata, arguments)
        }
    }

    fun linkInlineSignup(
        metadata: PaymentMethodMetadata,
        arguments: UiDefinitionFactory.Arguments,
    ): List<FormElement> {
        val linkState = metadata.linkState ?: return emptyList()
        val signupMode = linkState.signupMode ?: return emptyList()
        val coordinator = arguments.linkConfigurationCoordinator ?: return emptyList()
        return listOf(
            LinkFormElement(
                signupMode = signupMode,
                configuration = linkState.configuration,
                linkConfigurationCoordinator = coordinator,
                initialLinkUserInput = arguments.initialLinkUserInput,
                onLinkInlineSignupStateChanged = arguments.onLinkInlineSignupStateChanged,
                previousLinkSignupCheckboxSelection = arguments.previousLinkSignupCheckboxSelection,
            )
        )
    }

    fun mandate(
        metadata: PaymentMethodMetadata,
        arguments: UiDefinitionFactory.Arguments,
    ): List<FormElement> {
        val canChangeSaveForFutureUse = canChangeSaveForFutureUse(metadata)
        val signupMode = metadata.linkState?.signupMode
            ?.takeIf { arguments.linkConfigurationCoordinator != null }
        if (metadata.forceSetupFutureUseBehaviorAndNewMandate) {
            val linkBrand = metadata.linkState?.configuration?.linkBrand ?: return emptyList()
            return listOf(
                CombinedLinkMandateElement(
                    identifier = IdentifierSpec.Generic("card_mandate"),
                    merchantName = metadata.merchantName,
                    linkBrand = linkBrand,
                    signupMode = signupMode,
                    isLinkUI = arguments.isLinkUI,
                    canChangeSaveForFutureUse = canChangeSaveForFutureUse,
                    linkSignupStateFlow = arguments.linkInlineHandler?.linkInlineState ?: stateFlowOf(null),
                )
            )
        }
        if (!metadata.hasIntentToSetup(PaymentMethod.Type.Card.code) ||
            !metadata.mandateAllowed(PaymentMethod.Type.Card)
        ) {
            return emptyList()
        }
        return listOf(
            MandateTextElement(
                identifier = IdentifierSpec.Generic("card_mandate"),
                stringResId = PaymentSheetR.string.stripe_paymentsheet_card_mandate,
                topPadding = when {
                    signupMode == LinkSignupMode.AlongsideSaveForFutureUse -> 0.dp
                    signupMode == LinkSignupMode.InsteadOfSaveForFutureUse -> 4.dp
                    canChangeSaveForFutureUse -> 6.dp
                    else -> 2.dp
                },
                args = listOf(metadata.merchantName),
            )
        )
    }

    private fun canChangeSaveForFutureUse(metadata: PaymentMethodMetadata): Boolean {
        return isSaveForFutureUseValueChangeable(
            code = PaymentMethod.Type.Card.code,
            metadata = metadata,
        )
    }
}

private fun PaymentSheet.BillingDetailsCollectionConfiguration.toInternal(): BillingDetailsCollectionConfiguration {
    return BillingDetailsCollectionConfiguration(
        collectName = false,
        collectEmail = collectsEmail,
        collectPhone = collectsPhone,
        address = when (address) {
            PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Automatic -> {
                BillingDetailsCollectionConfiguration.AddressCollectionMode.Automatic
            }
            PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Never -> {
                BillingDetailsCollectionConfiguration.AddressCollectionMode.Never
            }
            PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full -> {
                BillingDetailsCollectionConfiguration.AddressCollectionMode.Full
            }
        },
    )
}

private fun cardBillingElements(
    allowedCountries: Set<String>,
    collectionConfiguration: BillingDetailsCollectionConfiguration,
    autocompleteAddressInteractorFactory: AutocompleteAddressInteractor.Factory?,
    initialValues: Map<IdentifierSpec, String?>,
    shippingValues: Map<IdentifierSpec, String?>?,
    requiresBillingAddressForAutomaticTax: Boolean,
): List<FormElement> {
    val sameAsShippingElement = shippingValues?.get(IdentifierSpec.SameAsShipping)
        ?.toBooleanStrictOrNull()
        ?.let {
            SameAsShippingElement(
                identifier = IdentifierSpec.SameAsShipping,
                controller = SameAsShippingController(it),
            )
        }
    val addressElement = CardBillingAddressElement(
        IdentifierSpec.Generic("credit_billing"),
        countryCodes = allowedCountries,
        rawValuesMap = initialValues,
        sameAsShippingElement = sameAsShippingElement,
        shippingValuesMap = shippingValues,
        collectionConfiguration = collectionConfiguration,
        autocompleteAddressInteractorFactory = autocompleteAddressInteractorFactory,
        requiresBillingAddressForAutomaticTax = requiresBillingAddressForAutomaticTax,
    )
    val title = when {
        collectionConfiguration.address == BillingDetailsCollectionConfiguration.AddressCollectionMode.Never &&
            (collectionConfiguration.collectPhone || collectionConfiguration.collectEmail) ->
            resolvableString(PaymentsUiCoreR.string.stripe_contact_information)
        else -> resolvableString(PaymentsUiCoreR.string.stripe_billing_details)
    }
    return listOfNotNull(
        SectionElement.wrap(addressElement, title),
        sameAsShippingElement,
    )
}

internal class CombinedLinkMandateElement(
    identifier: IdentifierSpec,
    signupMode: LinkSignupMode?,
    canChangeSaveForFutureUse: Boolean,
    private val merchantName: String,
    private val linkBrand: LinkBrand,
    private val linkSignupStateFlow: StateFlow<InlineSignupViewState?>,
    private val isLinkUI: Boolean,
) : RenderableFormElement(
    allowsUserInteraction = false,
    identifier = identifier,
) {
    override fun getFormFieldValueFlow() = stateFlowOf(emptyList<Pair<IdentifierSpec, FormFieldEntry>>())

    private val topPadding = when {
        signupMode == LinkSignupMode.AlongsideSaveForFutureUse -> 0.dp
        signupMode == LinkSignupMode.InsteadOfSaveForFutureUse -> 4.dp
        canChangeSaveForFutureUse -> 6.dp
        else -> 2.dp
    }

    @Composable
    override fun ComposeUI(
        enabled: Boolean,
        hiddenIdentifiers: Set<IdentifierSpec>,
        lastTextFieldIdentifier: IdentifierSpec?,
    ) {
        val linkState by linkSignupStateFlow.collectAsState()
        Mandate(
            mandateText = if (linkState?.isExpanded == true && !isLinkUI) {
                stringResource(
                    id = PaymentSheetR.string.stripe_paymentsheet_card_mandate_signup_toggle_on_v3_branded,
                    formatArgs = arrayOf(merchantName, linkBrand.brandName()),
                ).replaceHyperlinks(linkBrand)
            } else {
                stringResource(
                    id = PaymentSheetR.string.stripe_paymentsheet_card_mandate_signup_toggle_off,
                    formatArgs = arrayOf(merchantName),
                ).replaceHyperlinks(linkBrand)
            },
            textAlign = if (isLinkUI) TextAlign.Center else TextAlign.Start,
            modifier = Modifier.padding(top = topPadding),
        )
    }
}
