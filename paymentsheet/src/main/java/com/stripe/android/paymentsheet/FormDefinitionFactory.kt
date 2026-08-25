package com.stripe.android.paymentsheet

import androidx.lifecycle.viewModelScope
import com.stripe.android.cards.CardAccountRangeRepository
import com.stripe.android.common.nfcscan.IsNfcScanningAvailable
import com.stripe.android.common.taptoadd.TapToAddHelper
import com.stripe.android.link.LinkConfigurationCoordinator
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.UiDefinitionFactory
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.paymentsheet.forms.FormArgumentsFactory
import com.stripe.android.paymentsheet.forms.FormFieldValues
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.paymentdatacollection.FormArguments
import com.stripe.android.paymentsheet.repositories.PaymentMethodMessagePromotionsHelper
import com.stripe.android.paymentsheet.ui.transformToPaymentMethodCreateParams
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel
import com.stripe.android.ui.core.elements.AutomaticallyLaunchedCardScanFormDataHelper
import com.stripe.android.uicore.elements.AutocompleteAddressInteractor
import com.stripe.android.uicore.elements.FormElement
import kotlinx.coroutines.CoroutineScope

internal interface FormDefinitionFactory {
    fun formElementsForCode(code: PaymentMethodCode): List<FormElement>

    fun createFormArguments(paymentMethodCode: PaymentMethodCode): FormArguments

    fun getPaymentMethodParams(
        formValues: FormFieldValues?,
        selectedPaymentMethodCode: PaymentMethodCode,
    ): PaymentMethodCreateParams?

    fun formTypeForCode(paymentMethodCode: PaymentMethodCode): FormHelper.FormType
}

internal class DefaultFormDefinitionFactory(
    private val coroutineScope: CoroutineScope,
    private val linkInlineHandler: LinkInlineHandler,
    private val cardAccountRangeRepositoryFactory: CardAccountRangeRepository.Factory,
    private val paymentMethodMetadata: PaymentMethodMetadata,
    private val newPaymentSelectionProvider: (PaymentMethodCode) -> NewPaymentOptionSelection?,
    private val linkConfigurationCoordinator: LinkConfigurationCoordinator?,
    private val setAsDefaultMatchesSaveForFutureUse: Boolean,
    private val autocompleteAddressInteractorFactory: AutocompleteAddressInteractor.Factory?,
    private val isLinkUI: Boolean,
    private val automaticallyLaunchedCardScanFormDataHelper: AutomaticallyLaunchedCardScanFormDataHelper?,
    private val tapToAddHelper: TapToAddHelper?,
    private val paymentMethodMessagePromotionsHelper: PaymentMethodMessagePromotionsHelper?,
    private val isNfcScanningAvailable: IsNfcScanningAvailable?,
) : FormDefinitionFactory {
    override fun formElementsForCode(code: PaymentMethodCode): List<FormElement> {
        return paymentMethodMetadata.formElementsForCode(
            code = code,
            uiDefinitionFactoryArgumentsFactory = createArgumentsFactory(code),
        ) ?: emptyList()
    }

    override fun createFormArguments(paymentMethodCode: PaymentMethodCode): FormArguments {
        return FormArgumentsFactory.create(
            paymentMethodCode = paymentMethodCode,
            metadata = paymentMethodMetadata,
        )
    }

    override fun getPaymentMethodParams(
        formValues: FormFieldValues?,
        selectedPaymentMethodCode: PaymentMethodCode,
    ): PaymentMethodCreateParams? {
        return formValues?.transformToPaymentMethodCreateParams(
            paymentMethodCode = selectedPaymentMethodCode,
            paymentMethodMetadata = paymentMethodMetadata,
        )
    }

    override fun formTypeForCode(paymentMethodCode: PaymentMethodCode): FormHelper.FormType {
        val formElements = formElementsForCode(paymentMethodCode)
        return if (requiresFormScreen(paymentMethodCode, formElements)) {
            FormHelper.FormType.UserInteractionRequired
        } else {
            val mandate = formElements.firstNotNullOfOrNull { it.mandateText }
            if (mandate == null) {
                FormHelper.FormType.Empty
            } else {
                FormHelper.FormType.MandateOnly(mandate)
            }
        }
    }

    private fun requiresFormScreen(paymentMethodCode: String, formElements: List<FormElement>): Boolean {
        val userInteractionAllowed = formElements.any { it.allowsUserInteraction }
        return userInteractionAllowed ||
            paymentMethodCode == PaymentMethod.Type.USBankAccount.code ||
            paymentMethodCode == PaymentMethod.Type.Link.code
    }

    private fun createArgumentsFactory(code: PaymentMethodCode): UiDefinitionFactory.Arguments.Factory {
        val currentSelection = newPaymentSelectionProvider(code)?.takeIf { it.getType() == code }

        return UiDefinitionFactory.Arguments.Factory.Default(
            coroutineScope = coroutineScope,
            cardAccountRangeRepositoryFactory = cardAccountRangeRepositoryFactory,
            linkConfigurationCoordinator = linkConfigurationCoordinator,
            linkInlineHandler = linkInlineHandler,
            onLinkInlineSignupStateChanged = linkInlineHandler::onStateUpdated,
            paymentMethodCreateParams = currentSelection?.getPaymentMethodCreateParams(),
            paymentMethodOptionsParams = currentSelection?.getPaymentMethodOptionParams(),
            paymentMethodExtraParams = currentSelection?.getPaymentMethodExtraParams(),
            initialLinkUserInput = when (val selection = currentSelection?.paymentSelection) {
                is PaymentSelection.New.Card -> selection.linkInput
                else -> null
            },
            previousLinkSignupCheckboxSelection = when (val selection = currentSelection?.paymentSelection) {
                is PaymentSelection.New.Card -> selection.linkInput != null
                else -> null
            },
            setAsDefaultMatchesSaveForFutureUse = setAsDefaultMatchesSaveForFutureUse,
            autocompleteAddressInteractorFactory = autocompleteAddressInteractorFactory,
            isLinkUI = isLinkUI,
            automaticallyLaunchedCardScanFormDataHelper = automaticallyLaunchedCardScanFormDataHelper,
            tapToAddHelper = tapToAddHelper,
            paymentMethodMessagingPromotionsHelper = paymentMethodMessagePromotionsHelper,
            isNfcScanningAvailable = isNfcScanningAvailable,
        )
    }

    companion object {
        fun create(
            viewModel: BaseSheetViewModel,
            paymentMethodMetadata: PaymentMethodMetadata,
        ): FormDefinitionFactory {
            return DefaultFormDefinitionFactory(
                coroutineScope = viewModel.viewModelScope,
                linkInlineHandler = LinkInlineHandler.create(),
                cardAccountRangeRepositoryFactory = viewModel.cardAccountRangeRepositoryFactory,
                paymentMethodMetadata = paymentMethodMetadata,
                newPaymentSelectionProvider = { viewModel.newPaymentSelection },
                linkConfigurationCoordinator = viewModel.linkHandler.linkConfigurationCoordinator,
                setAsDefaultMatchesSaveForFutureUse = viewModel.customerStateHolder.paymentMethods.value.isEmpty(),
                autocompleteAddressInteractorFactory = viewModel.autocompleteAddressInteractorFactory,
                isLinkUI = false,
                automaticallyLaunchedCardScanFormDataHelper = null,
                tapToAddHelper = viewModel.tapToAddHelper,
                paymentMethodMessagePromotionsHelper = null,
                isNfcScanningAvailable = viewModel.isNfcScanningAvailable,
            )
        }
    }
}
