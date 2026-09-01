package com.stripe.android.lpmfoundations.paymentmethod

import com.stripe.android.common.nfcscan.IsNfcScanningAvailable
import com.stripe.android.common.taptoadd.TapToAddHelper
import com.stripe.android.link.LinkConfigurationCoordinator
import com.stripe.android.link.ui.inline.UserInput
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodExtraParams
import com.stripe.android.model.PaymentMethodOptionsParams
import com.stripe.android.paymentsheet.repositories.PaymentMethodMessagePromotionsHelper
import com.stripe.android.ui.core.elements.AutomaticallyLaunchedCardScanFormDataHelper
import com.stripe.android.uicore.elements.AutocompleteAddressInteractor
import com.stripe.android.uicore.elements.FormElement
import com.stripe.android.uicore.elements.IdentifierSpec
import kotlinx.coroutines.CoroutineScope

internal fun PaymentMethodDefinition.formElements(
    metadata: PaymentMethodMetadata = PaymentMethodMetadataFactory.create(),
    paymentMethodCreateParams: PaymentMethodCreateParams? = null,
    paymentMethodOptionsParams: PaymentMethodOptionsParams? = null,
    paymentMethodExtraParams: PaymentMethodExtraParams? = null,
    initialValues: Map<IdentifierSpec, String?>? = null,
    initialLinkUserInput: UserInput? = null,
    linkConfigurationCoordinator: LinkConfigurationCoordinator? = null,
    setAsDefaultMatchesSaveForFutureUse: Boolean = false,
    autocompleteAddressInteractorFactory: AutocompleteAddressInteractor.Factory? = null,
    automaticallyLaunchedCardScanFormDataHelper: AutomaticallyLaunchedCardScanFormDataHelper? = null,
    tapToAddHelper: TapToAddHelper? = null,
    isNfcScanningAvailable: IsNfcScanningAvailable? = null,
    paymentMethodMessagePromotionsHelper: PaymentMethodMessagePromotionsHelper? = null
): List<FormElement> {
    return formElementsInternal(
        coroutineScope = null,
        metadata = metadata,
        paymentMethodCreateParams = paymentMethodCreateParams,
        paymentMethodOptionsParams = paymentMethodOptionsParams,
        paymentMethodExtraParams = paymentMethodExtraParams,
        initialValues = initialValues,
        initialLinkUserInput = initialLinkUserInput,
        linkConfigurationCoordinator = linkConfigurationCoordinator,
        setAsDefaultMatchesSaveForFutureUse = setAsDefaultMatchesSaveForFutureUse,
        autocompleteAddressInteractorFactory = autocompleteAddressInteractorFactory,
        automaticallyLaunchedCardScanFormDataHelper = automaticallyLaunchedCardScanFormDataHelper,
        tapToAddHelper = tapToAddHelper,
        isNfcScanningAvailable = isNfcScanningAvailable,
        paymentMethodMessagePromotionsHelper = paymentMethodMessagePromotionsHelper,
    )
}

internal fun PaymentMethodDefinition.formElements(
    coroutineScope: CoroutineScope,
    metadata: PaymentMethodMetadata = PaymentMethodMetadataFactory.create(),
    paymentMethodCreateParams: PaymentMethodCreateParams? = null,
    paymentMethodOptionsParams: PaymentMethodOptionsParams? = null,
    paymentMethodExtraParams: PaymentMethodExtraParams? = null,
    initialValues: Map<IdentifierSpec, String?>? = null,
    initialLinkUserInput: UserInput? = null,
    linkConfigurationCoordinator: LinkConfigurationCoordinator? = null,
    setAsDefaultMatchesSaveForFutureUse: Boolean = false,
    autocompleteAddressInteractorFactory: AutocompleteAddressInteractor.Factory? = null,
    automaticallyLaunchedCardScanFormDataHelper: AutomaticallyLaunchedCardScanFormDataHelper? = null,
    tapToAddHelper: TapToAddHelper? = null,
    isNfcScanningAvailable: IsNfcScanningAvailable? = null,
    paymentMethodMessagePromotionsHelper: PaymentMethodMessagePromotionsHelper? = null
): List<FormElement> {
    return formElementsInternal(
        coroutineScope = coroutineScope,
        metadata = metadata,
        paymentMethodCreateParams = paymentMethodCreateParams,
        paymentMethodOptionsParams = paymentMethodOptionsParams,
        paymentMethodExtraParams = paymentMethodExtraParams,
        initialValues = initialValues,
        initialLinkUserInput = initialLinkUserInput,
        linkConfigurationCoordinator = linkConfigurationCoordinator,
        setAsDefaultMatchesSaveForFutureUse = setAsDefaultMatchesSaveForFutureUse,
        autocompleteAddressInteractorFactory = autocompleteAddressInteractorFactory,
        automaticallyLaunchedCardScanFormDataHelper = automaticallyLaunchedCardScanFormDataHelper,
        tapToAddHelper = tapToAddHelper,
        isNfcScanningAvailable = isNfcScanningAvailable,
        paymentMethodMessagePromotionsHelper = paymentMethodMessagePromotionsHelper,
    )
}

private fun PaymentMethodDefinition.formElementsInternal(
    coroutineScope: CoroutineScope?,
    metadata: PaymentMethodMetadata,
    paymentMethodCreateParams: PaymentMethodCreateParams?,
    paymentMethodOptionsParams: PaymentMethodOptionsParams?,
    paymentMethodExtraParams: PaymentMethodExtraParams?,
    initialValues: Map<IdentifierSpec, String?>?,
    initialLinkUserInput: UserInput?,
    linkConfigurationCoordinator: LinkConfigurationCoordinator?,
    setAsDefaultMatchesSaveForFutureUse: Boolean,
    autocompleteAddressInteractorFactory: AutocompleteAddressInteractor.Factory?,
    automaticallyLaunchedCardScanFormDataHelper: AutomaticallyLaunchedCardScanFormDataHelper?,
    tapToAddHelper: TapToAddHelper?,
    isNfcScanningAvailable: IsNfcScanningAvailable?,
    paymentMethodMessagePromotionsHelper: PaymentMethodMessagePromotionsHelper?,
): List<FormElement> {
    return requireNotNull(
        metadata.formElementsForCode(
            code = type.code,
            uiDefinitionFactoryArgumentsFactory = if (coroutineScope == null) {
                TestUiDefinitionFactoryArgumentsFactory.create(
                    paymentMethodCreateParams = paymentMethodCreateParams,
                    paymentMethodOptionsParams = paymentMethodOptionsParams,
                    paymentMethodExtraParams = paymentMethodExtraParams,
                    initialValues = initialValues,
                    linkConfigurationCoordinator = linkConfigurationCoordinator,
                    autocompleteAddressInteractorFactory = autocompleteAddressInteractorFactory,
                    initialLinkUserInput = initialLinkUserInput,
                    setAsDefaultMatchesSaveForFutureUse = setAsDefaultMatchesSaveForFutureUse,
                    automaticallyLaunchedCardScanFormDataHelper = automaticallyLaunchedCardScanFormDataHelper,
                    tapToAddHelper = tapToAddHelper,
                    paymentMethodMessagePromotionsHelper = paymentMethodMessagePromotionsHelper,
                    isNfcScanningAvailable = isNfcScanningAvailable,
                )
            } else {
                TestUiDefinitionFactoryArgumentsFactory.create(
                    coroutineScope = coroutineScope,
                    paymentMethodCreateParams = paymentMethodCreateParams,
                    paymentMethodOptionsParams = paymentMethodOptionsParams,
                    paymentMethodExtraParams = paymentMethodExtraParams,
                    initialValues = initialValues,
                    linkConfigurationCoordinator = linkConfigurationCoordinator,
                    autocompleteAddressInteractorFactory = autocompleteAddressInteractorFactory,
                    initialLinkUserInput = initialLinkUserInput,
                    setAsDefaultMatchesSaveForFutureUse = setAsDefaultMatchesSaveForFutureUse,
                    automaticallyLaunchedCardScanFormDataHelper = automaticallyLaunchedCardScanFormDataHelper,
                    tapToAddHelper = tapToAddHelper,
                    paymentMethodMessagePromotionsHelper = paymentMethodMessagePromotionsHelper,
                    isNfcScanningAvailable = isNfcScanningAvailable,
                )
            }
        )
    )
}
