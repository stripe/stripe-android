package com.stripe.android.paymentsheet.addresselement

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.core.utils.requireApplication
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.addresselement.analytics.AddressLauncherEventReporter
import com.stripe.android.paymentsheet.injection.AutocompleteViewModelSubcomponent
import com.stripe.android.paymentsheet.injection.DaggerAutocompleteViewModelFactoryComponent
import com.stripe.android.ui.core.elements.autocomplete.PlacesClientProxy
import com.stripe.android.ui.core.elements.autocomplete.model.AutocompletePrediction
import com.stripe.android.ui.core.elements.autocomplete.model.transformGoogleToStripeAddress
import com.stripe.android.uicore.elements.SimpleTextFieldConfig
import com.stripe.android.uicore.elements.SimpleTextFieldController
import com.stripe.android.uicore.elements.TextFieldIcon
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Provider
import com.stripe.android.R as StripeR
import com.stripe.android.uicore.R as UiCoreR

internal class AutocompleteViewModel @Inject constructor(
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

    private val _event = MutableSharedFlow<Event>()
    val event = _event.asSharedFlow()

    private val config = SimpleTextFieldConfig(
        label = resolvableString(UiCoreR.string.stripe_address_label_address),
        trailingIcon = MutableStateFlow(null)
    )

    val textFieldController = SimpleTextFieldController(config)

    private val queryFlow = textFieldController.fieldValue

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

                    _event.emit(
                        Event.GoBack(
                            addressDetails = AddressDetails(
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
                    )
                },
                onFailure = {
                    _loading.value = false
                    _event.emit(Event.GoBack(addressDetails = null))
                }
            )
        }
    }

    fun onBackPressed() {
        viewModelScope.launch {
            _event.emit(Event.GoBack(null))
        }
    }

    fun onEnterAddressManually() {
        viewModelScope.launch {
            _event.emit(
                Event.EnterManually(
                    if (queryFlow.value.isNotBlank()) {
                        AddressDetails(
                            address = PaymentSheet.Address(
                                line1 = queryFlow.value,
                            )
                        )
                    } else {
                        null
                    }
                )
            )
        }
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

    internal class Factory private constructor(
        private val type: Type,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            return type.create(modelClass, extras)
        }

        constructor(
            autoCompleteViewModelSubcomponentBuilderProvider:
            Provider<AutocompleteViewModelSubcomponent.Builder>,
            args: Args,
        ) : this(
            Type.WithinAddressElement(autoCompleteViewModelSubcomponentBuilderProvider, args)
        )

        constructor(
            args: AutocompleteContract.Args,
        ) : this(
            Type.Isolated(args)
        )

        sealed interface Type {
            fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T

            class WithinAddressElement(
                private val autoCompleteViewModelSubcomponentBuilderProvider:
                Provider<AutocompleteViewModelSubcomponent.Builder>,
                private val args: Args,
            ) : Type {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                    return autoCompleteViewModelSubcomponentBuilderProvider.get()
                        .application(extras.requireApplication())
                        .configuration(args)
                        .build().autoCompleteViewModel as T
                }
            }

            class Isolated(
                private val args: AutocompleteContract.Args,
            ) : Type {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                    return DaggerAutocompleteViewModelFactoryComponent
                        .factory()
                        .build(
                            application = extras.requireApplication(),
                            args = args,
                        )
                        .autocompleteViewModel as T
                }
            }
        }
    }

    sealed interface Event {
        val addressDetails: AddressDetails?

        data class EnterManually(override val addressDetails: AddressDetails?) : Event

        data class GoBack(override val addressDetails: AddressDetails?) : Event
    }

    data class Args(
        val country: String?
    )

    companion object {
        const val SEARCH_DEBOUNCE_MS = 400L
        const val MAX_DISPLAYED_RESULTS = 4
        const val MIN_CHARS_AUTOCOMPLETE = 3
    }
}
