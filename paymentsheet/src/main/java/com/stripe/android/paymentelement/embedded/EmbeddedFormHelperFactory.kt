package com.stripe.android.paymentelement.embedded

import androidx.lifecycle.SavedStateHandle
import com.stripe.android.cards.CardAccountRangeRepository
import com.stripe.android.common.nfcscan.IsNfcScanningAvailable
import com.stripe.android.common.taptoadd.TapToAddHelper
import com.stripe.android.link.LinkConfigurationCoordinator
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.paymentsheet.FormDefinitionFactory
import com.stripe.android.paymentsheet.FormHelper
import com.stripe.android.paymentsheet.LinkInlineHandler
import com.stripe.android.paymentsheet.NewPaymentOptionSelection
import com.stripe.android.paymentsheet.PaymentMethodFormFactory
import com.stripe.android.paymentsheet.addresselement.PaymentElementAutocompleteAddressInteractor
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.paymentMethodType
import com.stripe.android.paymentsheet.repositories.PaymentMethodMessagePromotionsHelper
import com.stripe.android.ui.core.elements.AutomaticallyLaunchedCardScanFormDataHelper
import com.stripe.android.ui.core.elements.FORM_ELEMENT_SET_DEFAULT_MATCHES_SAVE_FOR_FUTURE_DEFAULT_VALUE
import com.stripe.android.uicore.elements.AutocompleteAddressInteractor
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.CoroutineScope
import javax.inject.Inject

@Module
internal object EmbeddedFormHelperModule {
    @Provides
    fun provideAutocompleteAddressInteractorFactoryProvider(): AutocompleteAddressInteractorFactoryProvider {
        return AutocompleteAddressInteractorFactoryProvider { null }
    }
}

internal fun interface AutocompleteAddressInteractorFactoryProvider {
    fun get(): PaymentElementAutocompleteAddressInteractor.Factory?
}

internal class EmbeddedFormHelperFactory @Inject constructor(
    private val linkConfigurationCoordinator: LinkConfigurationCoordinator,
    private val embeddedSelectionHolder: EmbeddedSelectionHolder,
    private val cardAccountRangeRepositoryFactory: CardAccountRangeRepository.Factory,
    private val savedStateHandle: SavedStateHandle,
    private val isNfcScanningAvailable: IsNfcScanningAvailable,
    private val autocompleteAddressInteractorFactoryProvider: AutocompleteAddressInteractorFactoryProvider,
) {
    private val formFactory = PaymentMethodFormFactory(
        linkConfigurationCoordinator = linkConfigurationCoordinator,
        cardAccountRangeRepositoryFactory = cardAccountRangeRepositoryFactory,
        savedStateHandle = savedStateHandle,
        isNfcScanningAvailable = isNfcScanningAvailable,
    )

    constructor(
        linkConfigurationCoordinator: LinkConfigurationCoordinator,
        embeddedSelectionHolder: EmbeddedSelectionHolder,
        cardAccountRangeRepositoryFactory: CardAccountRangeRepository.Factory,
        savedStateHandle: SavedStateHandle,
        isNfcScanningAvailable: IsNfcScanningAvailable,
    ) : this(
        linkConfigurationCoordinator = linkConfigurationCoordinator,
        embeddedSelectionHolder = embeddedSelectionHolder,
        cardAccountRangeRepositoryFactory = cardAccountRangeRepositoryFactory,
        savedStateHandle = savedStateHandle,
        isNfcScanningAvailable = isNfcScanningAvailable,
        autocompleteAddressInteractorFactoryProvider = AutocompleteAddressInteractorFactoryProvider { null },
    )

    fun create(
        coroutineScope: CoroutineScope,
        setAsDefaultMatchesSaveForFutureUse: Boolean,
        paymentMethodMetadata: PaymentMethodMetadata,
        eventReporter: EventReporter,
        automaticallyLaunchedCardScanFormDataHelper: AutomaticallyLaunchedCardScanFormDataHelper?,
        tapToAddHelper: TapToAddHelper?,
        paymentMethodMessagePromotionsHelper: PaymentMethodMessagePromotionsHelper?,
        autocompleteAddressInteractorFactory: AutocompleteAddressInteractor.Factory?,
        selectionUpdater: (PaymentSelection?) -> Unit,
    ): FormHelper {
        val linkInlineHandler = LinkInlineHandler.create()
        return formFactory.createFormHelper(
            PaymentMethodFormFactory.FormHelperArguments(
                coroutineScope = coroutineScope,
                linkInlineHandler = linkInlineHandler,
                paymentMethodMetadata = paymentMethodMetadata,
                newPaymentSelectionProvider = ::newPaymentSelection,
                selectionUpdater = selectionUpdater,
                eventReporter = eventReporter,
                setAsDefaultMatchesSaveForFutureUse = setAsDefaultMatchesSaveForFutureUse,
                automaticallyLaunchedCardScanFormDataHelper = automaticallyLaunchedCardScanFormDataHelper,
                tapToAddHelper = tapToAddHelper,
                paymentMethodMessagePromotionsHelper = paymentMethodMessagePromotionsHelper,
                autocompleteAddressInteractorFactory = autocompleteAddressInteractorFactory,
            )
        )
    }

    fun createFormDefinitionFactory(
        coroutineScope: CoroutineScope,
        setAsDefaultMatchesSaveForFutureUse: Boolean,
        paymentMethodMetadata: PaymentMethodMetadata,
        automaticallyLaunchedCardScanFormDataHelper: AutomaticallyLaunchedCardScanFormDataHelper?,
        tapToAddHelper: TapToAddHelper?,
        paymentMethodMessagePromotionsHelper: PaymentMethodMessagePromotionsHelper?,
        autocompleteAddressInteractorFactory: AutocompleteAddressInteractor.Factory?,
        linkInlineHandler: LinkInlineHandler,
    ): FormDefinitionFactory {
        return formFactory.createFormDefinitionFactory(
            PaymentMethodFormFactory.FormDefinitionArguments(
                coroutineScope = coroutineScope,
                linkInlineHandler = linkInlineHandler,
                paymentMethodMetadata = paymentMethodMetadata,
                newPaymentSelectionProvider = ::newPaymentSelection,
                setAsDefaultMatchesSaveForFutureUse = setAsDefaultMatchesSaveForFutureUse,
                automaticallyLaunchedCardScanFormDataHelper = automaticallyLaunchedCardScanFormDataHelper,
                tapToAddHelper = tapToAddHelper,
                paymentMethodMessagePromotionsHelper = paymentMethodMessagePromotionsHelper,
                autocompleteAddressInteractorFactory = autocompleteAddressInteractorFactory,
            )
        )
    }

    private fun newPaymentSelection(code: PaymentMethodCode): NewPaymentOptionSelection? {
        return when (
            val currentSelection = embeddedSelectionHolder.selection.value
                ?.takeIf { it.paymentMethodType == code }
                ?: embeddedSelectionHolder.getPreviousNewSelection(code)
        ) {
            is PaymentSelection.ExternalPaymentMethod -> NewPaymentOptionSelection.External(currentSelection)
            is PaymentSelection.CustomPaymentMethod -> NewPaymentOptionSelection.Custom(currentSelection)
            is PaymentSelection.New -> NewPaymentOptionSelection.New(currentSelection)
            else -> null
        }
    }

    /**
     * Creates a [FormHelper] for the vertical-layout payment method list rather than the form screen. Card scan
     * auto-launch and tap-to-add apply only to the form screen, and [setAsDefaultMatchesSaveForFutureUse] does not
     * affect form-type determination, so the default value is used.
     */
    fun createForVerticalLayout(
        coroutineScope: CoroutineScope,
        paymentMethodMetadata: PaymentMethodMetadata,
        eventReporter: EventReporter,
        paymentMethodMessagePromotionsHelper: PaymentMethodMessagePromotionsHelper?,
        selectionUpdater: (PaymentSelection?) -> Unit,
    ): FormHelper {
        return create(
            coroutineScope = coroutineScope,
            paymentMethodMetadata = paymentMethodMetadata,
            eventReporter = eventReporter,
            automaticallyLaunchedCardScanFormDataHelper = null,
            tapToAddHelper = null,
            selectionUpdater = selectionUpdater,
            setAsDefaultMatchesSaveForFutureUse = FORM_ELEMENT_SET_DEFAULT_MATCHES_SAVE_FOR_FUTURE_DEFAULT_VALUE,
            paymentMethodMessagePromotionsHelper = paymentMethodMessagePromotionsHelper,
            autocompleteAddressInteractorFactory = null,
        )
    }

    /**
     * Card scan auto-launch is only relevant in the form screen, so this is only built for that flow. We suppress
     * the automatic launch when the card form is being reopened with previously entered details (i.e. the user has
     * already seen it), and otherwise let it launch when configured to.
     */
    fun createAutomaticallyLaunchedCardScanFormDataHelper(
        selectedPaymentMethodCode: PaymentMethodCode,
        paymentMethodMetadata: PaymentMethodMetadata,
    ): AutomaticallyLaunchedCardScanFormDataHelper {
        val paymentSelection = embeddedSelectionHolder.selection.value as? PaymentSelection.New
        val isLaunchingEmptyCardForm =
            selectedPaymentMethodCode == PaymentMethod.Type.Card.code &&
                paymentSelection?.paymentMethodCreateParams == null
        return AutomaticallyLaunchedCardScanFormDataHelper(
            hasAutomaticallyLaunchedCardScanInitialValue = !isLaunchingEmptyCardForm,
            savedStateHandle = savedStateHandle,
            openCardScanAutomaticallyConfig = paymentMethodMetadata.openCardScanAutomatically,
        )
    }
}
