package com.stripe.android.paymentsheet.addresselement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.stripe.android.ui.core.injection.NonFallbackInjectable
import com.stripe.android.paymentsheet.injection.InputAddressViewModelSubcomponent
import com.stripe.android.ui.core.FormController
import com.stripe.android.ui.core.elements.AddressSpec
import com.stripe.android.ui.core.elements.AddressType
import com.stripe.android.ui.core.elements.IdentifierSpec
import com.stripe.android.ui.core.elements.LayoutSpec
import com.stripe.android.ui.core.injection.FormControllerSubcomponent
import com.stripe.android.ui.core.injection.NonFallbackInjector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Provider

internal class InputAddressViewModel @Inject constructor(
    val args: AddressElementActivityContract.Args,
    val navigator: AddressElementNavigator,
    formControllerProvider: Provider<FormControllerSubcomponent.Builder>
) : ViewModel() {
    private val _collectedAddress = MutableStateFlow<AddressDetails?>(null)
    val collectedAddress: StateFlow<AddressDetails?> = _collectedAddress

    private val _formController = MutableStateFlow<FormController?>(null)
    val formController: StateFlow<FormController?> = _formController

    private val _formEnabled = MutableStateFlow(true)
    val formEnabled: StateFlow<Boolean> = _formEnabled

    init {
        viewModelScope.launch {
            navigator.getResultFlow<AddressDetails?>(AddressDetails.KEY)?.collect {
                val oldShippingAddress = _collectedAddress.value
                _collectedAddress.emit(
                    AddressDetails(
                        name = oldShippingAddress?.name ?: it?.name,
                        company = oldShippingAddress?.company ?: it?.company,
                        phoneNumber = oldShippingAddress?.phoneNumber ?: it?.phoneNumber,
                        city = it?.city,
                        country = it?.country,
                        line1 = it?.line1,
                        line2 = it?.line2,
                        state = it?.state,
                        postalCode = it?.postalCode
                    )
                )
            }
        }

        viewModelScope.launch {
            collectedAddress.collect { shippingAddress ->
                val initialValues: Map<IdentifierSpec, String?> = shippingAddress?.let {
                    mapOf(
                        IdentifierSpec.Name to shippingAddress.name,
                        IdentifierSpec.Line1 to shippingAddress.line1,
                        IdentifierSpec.Line2 to shippingAddress.line2,
                        IdentifierSpec.City to shippingAddress.city,
                        IdentifierSpec.State to shippingAddress.state,
                        IdentifierSpec.PostalCode to shippingAddress.postalCode,
                        IdentifierSpec.Country to shippingAddress.country,
                        IdentifierSpec.Phone to shippingAddress.phoneNumber
                    )
                } ?: emptyMap()

                _formController.value = formControllerProvider.get()
                    .viewOnlyFields(emptySet())
                    .viewModelScope(viewModelScope)
                    .stripeIntent(null)
                    .merchantName("")
                    .formSpec(buildFormSpec(shippingAddress == null))
                    .initialValues(initialValues)
                    .build().formController
            }
        }
    }

    private fun buildFormSpec(condensedForm: Boolean): LayoutSpec {
        return LayoutSpec(
            listOf(
                if (condensedForm) {
                    AddressSpec(
                        showLabel = false,
                        type = AddressType.ShippingCondensed(
                            googleApiKey = args.config?.googlePlacesApiKey
                        ) {
                            viewModelScope.launch {
                                val country = _formController
                                    .value
                                    ?.formValues
                                    ?.stateIn(viewModelScope)
                                    ?.value
                                    ?.get(IdentifierSpec.Country)
                                    ?.value
                                country?.let {
                                    navigator.navigateTo(
                                        AddressElementScreen.Autocomplete(
                                            country = country
                                        )
                                    )
                                }
                            }
                        }
                    )
                } else {
                    AddressSpec(
                        showLabel = false,
                        type = AddressType.ShippingExpanded
                    )
                }
            )
        )
    }

    fun expandAddressForm() {
        viewModelScope.launch {
            formController.value?.let { controller ->
                controller.formValues.collect {
                    _collectedAddress.value = AddressDetails(
                        name = it[IdentifierSpec.Name]?.value,
                        phoneNumber = it[IdentifierSpec.Phone]?.value,
                        country = it[IdentifierSpec.Country]?.value
                    )
                }
            }
        }
    }

    fun clickPrimaryButton() {
        _formEnabled.value = false
        viewModelScope.launch {
            formController.value?.let { controller ->
                controller.formValues.collect {
                    val result = AddressLauncherResult.Succeeded(
                        AddressDetails(
                            name = it[IdentifierSpec.Name]?.value,
                            city = it[IdentifierSpec.City]?.value,
                            country = it[IdentifierSpec.Country]?.value,
                            line1 = it[IdentifierSpec.Line1]?.value,
                            line2 = it[IdentifierSpec.Line2]?.value,
                            postalCode = it[IdentifierSpec.PostalCode]?.value,
                            state = it[IdentifierSpec.State]?.value,
                            phoneNumber = it[IdentifierSpec.Phone]?.value
                        )
                    )
                    navigator.dismiss(result)
                }
            }
        }
    }

    internal class Factory(
        private val injector: NonFallbackInjector
    ) : ViewModelProvider.Factory, NonFallbackInjectable {

        @Inject
        lateinit var subComponentBuilderProvider:
            Provider<InputAddressViewModelSubcomponent.Builder>

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            injector.inject(this)
            return subComponentBuilderProvider.get()
                .build().inputAddressViewModel as T
        }
    }
}
