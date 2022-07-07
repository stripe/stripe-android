package com.stripe.android.paymentsheet.addresselement

import android.app.Application
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.stripe.android.paymentsheet.R
import com.stripe.android.ui.core.injection.NonFallbackInjectable
import com.stripe.android.paymentsheet.injection.AutoCompleteViewModelSubcomponent
import com.stripe.android.ui.core.elements.SimpleTextFieldConfig
import com.stripe.android.ui.core.elements.SimpleTextFieldController
import com.stripe.android.ui.core.elements.TextFieldIcon
import com.stripe.android.ui.core.elements.autocomplete.PlacesClientProxy
import com.stripe.android.ui.core.elements.autocomplete.model.AutocompletePrediction
import com.stripe.android.ui.core.elements.autocomplete.model.transformGoogleToStripeAddress
import com.stripe.android.ui.core.injection.NonFallbackInjector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Provider

internal class AutocompleteViewModel @Inject constructor(
    val args: AddressElementActivityContract.Args,
    val navigator: AddressElementNavigator,
    application: Application
) : AndroidViewModel(application) {
    private var client: PlacesClientProxy? = null

    private val _predictions = MutableStateFlow<List<AutocompletePrediction>?>(null)
    val predictions: StateFlow<List<AutocompletePrediction>?>
        get() = _predictions

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean>
        get() = _loading

    @VisibleForTesting
    val addressResult = MutableStateFlow<Result<ShippingAddress?>?>(null)

    val textFieldController = SimpleTextFieldController(
        SimpleTextFieldConfig(
            label = R.string.address_label_address,
            trailingIcon = MutableStateFlow(
                TextFieldIcon.Trailing(
                    idRes = R.drawable.stripe_ic_clear,
                    isTintable = true,
                    onClick = { clearQuery() }
                )
            )
        )
    )

    private val queryFlow = textFieldController.fieldValue
        .map { it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), "")

    private val debouncer = Debouncer()

    fun initialize(
        clientProvider: () -> PlacesClientProxy? = {
            // TODO: Update the PaymentSheet Configuration to include api key
//            args.config?.googlePlacesApiKey?.let {
//                PlacesClientProxy.create(getApplication(), it)
//            }
            PlacesClientProxy.create(getApplication(), "")
        }
    ) {
        client = clientProvider()
        debouncer.startWatching(
            coroutineScope = viewModelScope,
            queryFlow = queryFlow,
            onValidQuery = {
                viewModelScope.launch {
                    client?.findAutocompletePredictions(
                        query = it,
                        country = "US",
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
    }

    fun selectPrediction(prediction: AutocompletePrediction) {
        viewModelScope.launch {
            _loading.value = true
            client?.fetchPlace(
                placeId = prediction.placeId
            )?.fold(
                onSuccess = {
                    _loading.value = false
                    val address = it.place.transformGoogleToStripeAddress(getApplication())
                    addressResult.value = Result.success(
                        ShippingAddress(
                            city = address.city,
                            country = address.country,
                            line1 = address.line1,
                            line2 = address.line2,
                            postalCode = address.postalCode,
                            state = address.state
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

    fun onEnterAddressManually() {
        setResultAndGoBack()
    }

    fun setResultAndGoBack() {
        addressResult.value?.fold(
            onSuccess = {
                navigator.setResult(ShippingAddress.KEY, it)
            },
            onFailure = {
                navigator.setResult(ShippingAddress.KEY, null)
            }
        )
        navigator.onBack()
    }

    private fun clearQuery() {
        textFieldController.onRawValueChange("")
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
        private val injector: NonFallbackInjector,
        private val applicationSupplier: () -> Application
    ) : ViewModelProvider.Factory, NonFallbackInjectable {

        @Inject
        lateinit var subComponentBuilderProvider:
            Provider<AutoCompleteViewModelSubcomponent.Builder>

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            injector.inject(this)
            return subComponentBuilderProvider.get()
                .application(applicationSupplier())
                .build().autoCompleteViewModel as T
        }
    }

    companion object {
        const val SEARCH_DEBOUNCE_MS = 1000L
        const val MAX_DISPLAYED_RESULTS = 4
        const val MIN_CHARS_AUTOCOMPLETE = 3
        val autocompleteSupportedCountries = setOf(
            "AU",
            "BE",
            "BR",
            "CA",
            "CH",
            "DE",
            "ES",
            "FR",
            "GB",
            "IE",
            "IN",
            "IT",
            "JP",
            "MX",
            "MY",
            "NO",
            "NL",
            "PH",
            "PL",
            "RU",
            "SE",
            "SG",
            "TR",
            "US",
            "ZA"
        )
    }
}
