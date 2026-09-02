package com.stripe.android.lpmfoundations.paymentmethod

import com.stripe.android.CardBrandFilter
import com.stripe.android.CardFundingFilter
import com.stripe.android.cards.CardAccountRangeRepository
import com.stripe.android.common.nfcscan.IsNfcScanningAvailable
import com.stripe.android.common.taptoadd.TapToAddHelper
import com.stripe.android.link.LinkConfigurationCoordinator
import com.stripe.android.link.ui.inline.InlineSignupViewState
import com.stripe.android.link.ui.inline.UserInput
import com.stripe.android.lpmfoundations.FormHeaderInformation
import com.stripe.android.lpmfoundations.luxe.FormElementsBuilder
import com.stripe.android.lpmfoundations.luxe.InitialValuesFactory
import com.stripe.android.lpmfoundations.luxe.SupportedPaymentMethod
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodExtraParams
import com.stripe.android.model.PaymentMethodOptionsParams
import com.stripe.android.model.setupFutureUsage
import com.stripe.android.paymentsheet.LinkInlineHandler
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.addresselement.toIdentifierMap
import com.stripe.android.paymentsheet.model.PaymentMethodIncentive
import com.stripe.android.paymentsheet.repositories.PaymentMethodMessagePromotionsHelper
import com.stripe.android.ui.core.cbc.CardBrandChoiceEligibility
import com.stripe.android.ui.core.elements.AutomaticallyLaunchedCardScanFormDataHelper
import com.stripe.android.ui.core.elements.FORM_ELEMENT_SET_DEFAULT_MATCHES_SAVE_FOR_FUTURE_DEFAULT_VALUE
import com.stripe.android.uicore.elements.AutocompleteAddressInteractor
import com.stripe.android.uicore.elements.FormElement
import com.stripe.android.uicore.elements.IdentifierSpec
import kotlinx.coroutines.CoroutineScope

internal sealed interface UiDefinitionFactory {
    data class Arguments(
        val coroutineScope: CoroutineScope,
        val cardAccountRangeRepositoryFactory: CardAccountRangeRepository.Factory,
        val linkConfigurationCoordinator: LinkConfigurationCoordinator?,
        val initialValues: Map<IdentifierSpec, String?>,
        val initialLinkUserInput: UserInput?,
        val shippingValues: Map<IdentifierSpec, String?>?,
        val saveForFutureUseInitialValue: Boolean,
        val merchantName: String,
        val cbcEligibility: CardBrandChoiceEligibility,
        val billingDetailsCollectionConfiguration: PaymentSheet.BillingDetailsCollectionConfiguration,
        val requiresBillingAddressForAutomaticTax: Boolean,
        val requiresMandate: Boolean,
        val onLinkInlineSignupStateChanged: (InlineSignupViewState) -> Unit,
        val cardBrandFilter: CardBrandFilter,
        val cardFundingFilter: CardFundingFilter,
        val setAsDefaultMatchesSaveForFutureUse: Boolean,
        val autocompleteAddressInteractorFactory: AutocompleteAddressInteractor.Factory?,
        val linkInlineHandler: LinkInlineHandler?,
        val isLinkUI: Boolean = false,
        val previousLinkSignupCheckboxSelection: Boolean? = null,
        val automaticallyLaunchedCardScanFormDataHelper: AutomaticallyLaunchedCardScanFormDataHelper? = null,
        val tapToAddHelper: TapToAddHelper? = null,
        val paymentMethodMessagingPromotionsHelper: PaymentMethodMessagePromotionsHelper? = null,
        val isNfcScanningAvailable: IsNfcScanningAvailable? = null,
    ) {
        interface Factory {
            fun create(
                metadata: PaymentMethodMetadata,
                requiresMandate: Boolean,
            ): Arguments

            class Default(
                private val coroutineScope: CoroutineScope,
                private val cardAccountRangeRepositoryFactory: CardAccountRangeRepository.Factory,
                private val linkConfigurationCoordinator: LinkConfigurationCoordinator?,
                private val linkInlineHandler: LinkInlineHandler?,
                private val onLinkInlineSignupStateChanged: (InlineSignupViewState) -> Unit,
                private val paymentMethodCreateParams: PaymentMethodCreateParams? = null,
                private val paymentMethodOptionsParams: PaymentMethodOptionsParams? = null,
                private val paymentMethodExtraParams: PaymentMethodExtraParams? = null,
                private val initialLinkUserInput: UserInput? = null,
                private val setAsDefaultMatchesSaveForFutureUse: Boolean =
                    FORM_ELEMENT_SET_DEFAULT_MATCHES_SAVE_FOR_FUTURE_DEFAULT_VALUE,
                private val autocompleteAddressInteractorFactory: AutocompleteAddressInteractor.Factory?,
                private val isLinkUI: Boolean = false,
                private val previousLinkSignupCheckboxSelection: Boolean? = null,
                private val automaticallyLaunchedCardScanFormDataHelper: AutomaticallyLaunchedCardScanFormDataHelper? =
                    null,
                private val tapToAddHelper: TapToAddHelper? = null,
                private val paymentMethodMessagingPromotionsHelper: PaymentMethodMessagePromotionsHelper? = null,
                private val isNfcScanningAvailable: IsNfcScanningAvailable? = null,
            ) : Factory {
                override fun create(
                    metadata: PaymentMethodMetadata,
                    requiresMandate: Boolean,
                ): Arguments {
                    return Arguments(
                        coroutineScope = coroutineScope,
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
                        saveForFutureUseInitialValue = getSaveForFutureUseInitialValue(),
                        billingDetailsCollectionConfiguration = metadata.billingDetailsCollectionConfiguration,
                        requiresBillingAddressForAutomaticTax = metadata.requiresBillingAddressForAutomaticTax,
                        requiresMandate = requiresMandate,
                        onLinkInlineSignupStateChanged = onLinkInlineSignupStateChanged,
                        cardBrandFilter = metadata.cardBrandFilter,
                        cardFundingFilter = metadata.cardFundingFilter,
                        initialLinkUserInput = initialLinkUserInput,
                        setAsDefaultMatchesSaveForFutureUse = setAsDefaultMatchesSaveForFutureUse,
                        autocompleteAddressInteractorFactory = autocompleteAddressInteractorFactory,
                        linkInlineHandler = linkInlineHandler,
                        isLinkUI = isLinkUI,
                        previousLinkSignupCheckboxSelection = previousLinkSignupCheckboxSelection,
                        automaticallyLaunchedCardScanFormDataHelper = automaticallyLaunchedCardScanFormDataHelper,
                        tapToAddHelper = tapToAddHelper,
                        paymentMethodMessagingPromotionsHelper = paymentMethodMessagingPromotionsHelper,
                        isNfcScanningAvailable = isNfcScanningAvailable,
                    )
                }

                private fun getSaveForFutureUseInitialValue(): Boolean {
                    return paymentMethodOptionsParams?.setupFutureUsage()?.let {
                        it != ConfirmPaymentIntentParams.SetupFutureUsage.Blank
                    } ?: false
                }
            }
        }
    }

    abstract class Simple : UiDefinitionFactory {
        protected open val supportsAutomaticTaxBillingAddress: Boolean = true

        abstract fun createSupportedPaymentMethod(
            metadata: PaymentMethodMetadata,
        ): SupportedPaymentMethod

        open fun createFormHeaderInformation(
            metadata: PaymentMethodMetadata,
            customerHasSavedPaymentMethods: Boolean,
            incentive: PaymentMethodIncentive?,
        ): FormHeaderInformation {
            return createSupportedPaymentMethod(metadata).asFormHeaderInformation(incentive)
        }
        fun createFormElements(metadata: PaymentMethodMetadata, arguments: Arguments): List<FormElement> {
            val builder = FormElementsBuilder(
                arguments = arguments,
                supportsAutomaticTaxBillingAddress = supportsAutomaticTaxBillingAddress,
            )

            buildFormElements(metadata, arguments, builder)

            return builder.build()
        }
        protected open fun buildFormElements(
            metadata: PaymentMethodMetadata,
            arguments: Arguments,
            builder: FormElementsBuilder,
        ) {}
    }

    interface Custom : UiDefinitionFactory {
        fun createSupportedPaymentMethod(
            metadata: PaymentMethodMetadata,
        ): SupportedPaymentMethod

        fun createFormHeaderInformation(
            metadata: PaymentMethodMetadata,
            customerHasSavedPaymentMethods: Boolean,
            incentive: PaymentMethodIncentive?,
        ): FormHeaderInformation {
            return createSupportedPaymentMethod(metadata).asFormHeaderInformation(incentive)
        }

        fun createFormElements(metadata: PaymentMethodMetadata, arguments: Arguments): List<FormElement>
    }

    fun supportedPaymentMethod(
        metadata: PaymentMethodMetadata,
    ): SupportedPaymentMethod = when (this) {
        is Simple -> createSupportedPaymentMethod(metadata)
        is Custom -> createSupportedPaymentMethod(metadata)
    }

    fun formHeaderInformation(
        metadata: PaymentMethodMetadata,
        customerHasSavedPaymentMethods: Boolean,
    ): FormHeaderInformation = when (this) {
        is Simple -> {
            createFormHeaderInformation(
                metadata = metadata,
                customerHasSavedPaymentMethods = customerHasSavedPaymentMethods,
                incentive = metadata.paymentMethodIncentive,
            )
        }

        is Custom -> {
            createFormHeaderInformation(
                customerHasSavedPaymentMethods = customerHasSavedPaymentMethods,
                incentive = metadata.paymentMethodIncentive,
                metadata = metadata,
            )
        }
    }

    fun formElements(
        metadata: PaymentMethodMetadata,
        arguments: Arguments,
    ): List<FormElement> = when (this) {
        is Simple -> {
            createFormElements(
                metadata = metadata,
                arguments = arguments,
            )
        }

        is Custom -> {
            createFormElements(
                metadata = metadata,
                arguments = arguments,
            )
        }
    }
}
