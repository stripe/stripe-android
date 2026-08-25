package com.stripe.android.link.ui.paymentmenthod

import androidx.lifecycle.viewModelScope
import com.stripe.android.link.injection.NativeLinkComponent
import com.stripe.android.paymentsheet.DefaultFormDefinitionFactory
import com.stripe.android.paymentsheet.DefaultFormHelper
import com.stripe.android.paymentsheet.FormHelper
import com.stripe.android.paymentsheet.LinkInlineHandler
import com.stripe.android.paymentsheet.addresselement.AUTOCOMPLETE_DEFAULT_COUNTRIES
import com.stripe.android.paymentsheet.addresselement.PaymentElementAutocompleteAddressInteractor
import com.stripe.android.paymentsheet.addresselement.analytics.NoOpAddressLauncherEventReporter
import com.stripe.android.ui.core.elements.FORM_ELEMENT_SET_DEFAULT_MATCHES_SAVE_FOR_FUTURE_DEFAULT_VALUE
import com.stripe.android.uicore.elements.AutocompleteAddressInteractor

internal class NativeLinkFormHelperFactory(
    private val parentComponent: NativeLinkComponent,
) {
    fun create(): FormHelper {
        val linkInlineHandler = LinkInlineHandler.create()
        val paymentMethodMetadata = parentComponent.paymentMethodMetadata

        return DefaultFormHelper(
            coroutineScope = parentComponent.viewModel.viewModelScope,
            linkInlineHandler = linkInlineHandler,
            paymentMethodMetadata = paymentMethodMetadata,
            selectionUpdater = {},
            eventReporter = parentComponent.eventReporter,
            savedStateHandle = parentComponent.viewModel.savedStateHandle,
            formDefinitionFactory = DefaultFormDefinitionFactory(
                coroutineScope = parentComponent.viewModel.viewModelScope,
                linkInlineHandler = linkInlineHandler,
                cardAccountRangeRepositoryFactory = parentComponent.cardAccountRangeRepositoryFactory,
                paymentMethodMetadata = paymentMethodMetadata,
                newPaymentSelectionProvider = { null },
                linkConfigurationCoordinator = null,
                setAsDefaultMatchesSaveForFutureUse =
                    FORM_ELEMENT_SET_DEFAULT_MATCHES_SAVE_FOR_FUTURE_DEFAULT_VALUE,
                autocompleteAddressInteractorFactory = createAutocompleteAddressInteractorFactory(),
                isLinkUI = true,
                automaticallyLaunchedCardScanFormDataHelper = null,
                tapToAddHelper = null,
                paymentMethodMessagePromotionsHelper = null,
                isNfcScanningAvailable = null,
            ),
        )
    }

    private fun createAutocompleteAddressInteractorFactory(): AutocompleteAddressInteractor.Factory {
        return PaymentElementAutocompleteAddressInteractor.Factory(
            launcher = parentComponent.autocompleteLauncher,
            autocompleteConfig = AutocompleteAddressInteractor.Config(
                googlePlacesApiKey = parentComponent.configuration.googlePlacesApiKey,
                autocompleteCountries = AUTOCOMPLETE_DEFAULT_COUNTRIES,
                isInlineAutocompleteEnabled = true,
            ),
            placesClient = null,
            stripeAutocompleteRepository = null,
            coroutineScope = null,
            shouldUseAutocompleteProxyEndpointsProvider = { false },
            eventReporter = NoOpAddressLauncherEventReporter,
        )
    }
}
