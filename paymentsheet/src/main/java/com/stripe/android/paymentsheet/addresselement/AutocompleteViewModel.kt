package com.stripe.android.paymentsheet.addresselement

import android.app.Application
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.addresselement.analytics.AddressLauncherEventReporter
import com.stripe.android.paymentsheet.injection.AutocompleteViewModelSubcomponent
import com.stripe.android.ui.core.elements.autocomplete.PlacesClientProxy
import com.stripe.android.ui.core.elements.autocomplete.model.AutocompletePrediction
import com.stripe.android.ui.core.elements.autocomplete.model.transformGoogleToStripeAddress
import com.stripe.android.uicore.elements.SimpleTextFieldConfig
import com.stripe.android.uicore.elements.SimpleTextFieldController
import com.stripe.android.uicore.elements.TextFieldIcon
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Provider
import com.stripe.android.R as StripeR
import com.stripe.android.uicore.R as UiCoreR

internal class AutocompleteViewModel @Inject constructor(
    val args: AddressElementActivityContract.Args,
    val navigator: AddressElementNavigator,
    private val placesClient: PlacesClientProxy?,
    private val autocompleteArgs: Args,
    private val eventReporter: AddressLauncherEventReporter,
    application: Application
) : AndroidViewModel(application) {
    private val _predictions = MutableStateFlow<List<AutocompletePrediction>?>(null)
    val predictions: StateFlow<List<AutocompletePrediction>?>
        get() = _predictions

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean>
        get() = _loading

    @VisibleForTesting
    val addressResult = MutableStateFlow<Result<AddressDetails?>?>(null)

    private val config = SimpleTextFieldConfig(
        label = UiCoreR.string.stripe_address_label_address,
        trailingIcon = MutableStateFlow(null)
    )

    val textFieldController = SimpleTextFieldController(config)

    private val queryFlow = textFieldController.fieldValue
        .map { it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), "")

    private val debouncer = Debouncer()

    init {
        debouncer.startWatching(
            coroutineScope = viewModelScope,
            queryFlow = queryFlow,
            onValidQuery = {
                viewModelScope.launch {
                    placesClient?.findAutocompletePredictions(
                        query = it,
                        country = autocompleteArgs.country
                            ?: throw IllegalStateException("Country cannot be empty"),
                        limit = MAX_DISPLAYED_RESULTS
                    )?.fold(
                        onSuccess = {
                            _loading.value = false
                            _predictions.value = it.autocompletePredictions
                        },
                        onFailure = {
                            _loading.value = false
                            addressResult.value = Result.failure(it)
                        }
                    )
                }
            }
        )
        viewModelScope.launch {
            queryFlow.collect {
                if (it.isEmpty()) {
                    config.trailingIcon.update {
                        null
                    }
                } else {
                    config.trailingIcon.update {
                        TextFieldIcon.Trailing(
                            idRes = StripeR.drawable.stripe_ic_clear,
                            isTintable = true,
                            onClick = { clearQuery() }
                        )
                    }
                }
            }
        }
        autocompleteArgs.country?.let { country ->
            eventReporter.onShow(country)
        }
    }

    fun selectPrediction(prediction: AutocompletePrediction) {
        viewModelScope.launch {
            _loading.value = true
            placesClient?.fetchPlace(
                placeId = prediction.placeId
            )?.fold(
                onSuccess = {
                    _loading.value = false
                    val address = it.place.transformGoogleToStripeAddress(getApplication())
                    addressResult.value = Result.success(
                        AddressDetails(
                            address = PaymentSheet.Address(
                                city = address.city,
                                country = address.country,
                                line1 = address.line1,
                                line2 = address.line2,
                                postalCode = address.postalCode,
                                state = address.state
                            )
                        )
                    )
                    setResultAndGoBack()
                },
                onFailure = {
                    _loading.value = false
                    addressResult.value = Result.failure(it)
                    setResultAndGoBack()
                }
            )
        }
    }

    fun onBackPressed() {
        val result = if (queryFlow.value.isNotBlank()) {
            AddressDetails(
                address = PaymentSheet.Address(
                    line1 = queryFlow.value
                )
            )
        } else {
            null
        }
        setResultAndGoBack(result)
    }

    fun onEnterAddressManually() {
        setResultAndGoBack(
            AddressDetails(
                address = PaymentSheet.Address(
                    line1 = queryFlow.value
                )
            )
        )
    }

    private fun setResultAndGoBack(addressDetails: AddressDetails? = null) {
        if (addressDetails != null) {
            navigator.setResult(AddressDetails.KEY, addressDetails)
        } else {
            addressResult.value?.fold(
                onSuccess = {
                    navigator.setResult(AddressDetails.KEY, it)
                },
                onFailure = {
                    navigator.setResult(AddressDetails.KEY, null)
                }
            )
        }

        navigator.onBack()
    }

    fun clearQuery() {
        textFieldController.onRawValueChange("")
        _predictions.value = null
    }

    internal class Debouncer {
        private var searchJob: Job? = null

        fun startWatching(
            coroutineScope: CoroutineScope,
            queryFlow: StateFlow<String?>,
            onValidQuery: (String) -> Unit
        ) {
            coroutineScope.launch {
                queryFlow.collect { query ->
                    query?.let {
                        searchJob?.cancel()
                        if (query.length > MIN_CHARS_AUTOCOMPLETE) {
                            searchJob = launch {
                                delay(SEARCH_DEBOUNCE_MS)
                                if (isActive) {
                                    onValidQuery(it)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    internal class Factory(
        private val autoCompleteViewModelSubcomponentBuilderProvider:
            Provider<AutocompleteViewModelSubcomponent.Builder>,
        private val args: Args,
        private val applicationSupplier: () -> Application
    ) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return autoCompleteViewModelSubcomponentBuilderProvider.get()
                .application(applicationSupplier())
                .configuration(args)
                .build().autoCompleteViewModel as T
        }
    }

    data class Args(
        val country: String?
    )

    companion object {
        const val SEARCH_DEBOUNCE_MS = 1000L
        const val MAX_DISPLAYED_RESULTS = 4
        const val MIN_CHARS_AUTOCOMPLETE = 3
    }
}
