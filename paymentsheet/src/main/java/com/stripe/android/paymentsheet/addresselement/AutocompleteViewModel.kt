package com.stripe.android.paymentsheet.addresselement

import android.app.Application
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.stripe.android.paymentsheet.R
import com.stripe.android.ui.core.injection.NonFallbackInjectable
import com.stripe.android.paymentsheet.injection.AutocompleteViewModelSubcomponent
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.lang.IllegalStateException
import javax.inject.Inject
import javax.inject.Provider

internal class AutocompleteViewModel @Inject constructor(
    val args: AddressElementActivityContract.Args,
    val navigator: AddressElementNavigator,
    private val autocompleteArgs: Args,
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
    val addressResult = MutableStateFlow<Result<AddressDetails?>?>(null)

    private val config = SimpleTextFieldConfig(
        label = R.string.address_label_address,
        trailingIcon = MutableStateFlow(null)
    )

    val textFieldController = SimpleTextFieldController(config)

    private val queryFlow = textFieldController.fieldValue
        .map { it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), "")

    private val debouncer = Debouncer()

    fun initialize(
        clientProvider: () -> PlacesClientProxy? = {
            args.config?.googlePlacesApiKey?.let {
                PlacesClientProxy.create(getApplication(), it)
            }
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
                            idRes = R.drawable.stripe_ic_clear,
                            isTintable = true,
                            onClick = { clearQuery() }
                        )
                    }
                }
            }
        }
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
                        AddressDetails(
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
                navigator.setResult(AddressDetails.KEY, it)
            },
            onFailure = {
                navigator.setResult(AddressDetails.KEY, null)
            }
        ) ?: run {
            navigator.setResult(AddressDetails.KEY, AddressDetails())
        }
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
        private val args: Args,
        private val applicationSupplier: () -> Application
    ) : ViewModelProvider.Factory, NonFallbackInjectable {

        @Inject
        lateinit var subComponentBuilderProvider:
            Provider<AutocompleteViewModelSubcomponent.Builder>

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            injector.inject(this)
            return subComponentBuilderProvider.get()
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
