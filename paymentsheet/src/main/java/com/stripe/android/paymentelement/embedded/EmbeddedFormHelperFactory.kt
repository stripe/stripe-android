package com.stripe.android.paymentelement.embedded

import androidx.lifecycle.SavedStateHandle
import com.stripe.android.cards.CardAccountRangeRepository
import com.stripe.android.common.nfcscan.IsNfcScanningAvailable
import com.stripe.android.common.taptoadd.TapToAddHelper
import com.stripe.android.link.LinkConfigurationCoordinator
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.paymentsheet.DefaultFormHelper
import com.stripe.android.paymentsheet.FormHelper
import com.stripe.android.paymentsheet.LinkInlineHandler
import com.stripe.android.paymentsheet.NewPaymentOptionSelection
import com.stripe.android.paymentsheet.addresselement.PaymentElementAutocompleteAddressInteractor
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.paymentMethodType
import com.stripe.android.paymentsheet.repositories.PaymentMethodMessagePromotionsHelper
import com.stripe.android.ui.core.elements.AutomaticallyLaunchedCardScanFormDataHelper
import com.stripe.android.ui.core.elements.FORM_ELEMENT_SET_DEFAULT_MATCHES_SAVE_FOR_FUTURE_DEFAULT_VALUE
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
        selectionUpdater: (PaymentSelection?) -> Unit,
    ): FormHelper {
        return DefaultFormHelper(
            coroutineScope = coroutineScope,
            linkInlineHandler = LinkInlineHandler.create(),
            cardAccountRangeRepositoryFactory = cardAccountRangeRepositoryFactory,
            paymentMethodMetadata = paymentMethodMetadata,
            newPaymentSelectionProvider = { code ->
                val currentSelection = embeddedSelectionHolder.selection.value
                    ?.takeIf { it.paymentMethodType == code }
                    ?: embeddedSelectionHolder.getPreviousNewSelection(code)
                when (currentSelection) {
                    is PaymentSelection.ExternalPaymentMethod -> {
                        NewPaymentOptionSelection.External(currentSelection)
                    }
                    is PaymentSelection.CustomPaymentMethod -> {
                        NewPaymentOptionSelection.Custom(currentSelection)
                    }
                    is PaymentSelection.New -> {
                        NewPaymentOptionSelection.New(currentSelection)
                    }
                    else -> null
                }
            },
            selectionUpdater = selectionUpdater,
            linkConfigurationCoordinator = linkConfigurationCoordinator,
            setAsDefaultMatchesSaveForFutureUse = setAsDefaultMatchesSaveForFutureUse,
            eventReporter = eventReporter,
            savedStateHandle = savedStateHandle,
            autocompleteAddressInteractorFactory = autocompleteAddressInteractorFactoryProvider.get(),
            automaticallyLaunchedCardScanFormDataHelper = automaticallyLaunchedCardScanFormDataHelper,
            tapToAddHelper = tapToAddHelper,
            paymentMethodMessagePromotionsHelper = paymentMethodMessagePromotionsHelper,
            isNfcScanningAvailable = isNfcScanningAvailable,
        )
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
