package com.stripe.android.paymentsheet.addresselement

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.Turbine
import com.stripe.android.uicore.elements.AutocompleteAddressInteractor

class TestAutocompleteAddressInteractor private constructor(
    override val autocompleteConfig: AutocompleteAddressInteractor.Config,
) : AutocompleteAddressInteractor {
    private val registerCalls = Turbine<Call.Register>()
    private val onAutocompleteCalls = Turbine<Call.OnAutocomplete>()

    override fun register(onEvent: (AutocompleteAddressInteractor.Event) -> Unit) {
        registerCalls.add(Call.Register(onEvent))
    }

    override fun onAutocomplete(country: String) {
        onAutocompleteCalls.add(Call.OnAutocomplete(country))
    }

    sealed interface Call {
        class Register(
            val onEvent: (AutocompleteAddressInteractor.Event) -> Unit
        )

        class OnAutocomplete(
            val country: String
        )
    }

    class Scenario(
        val interactor: AutocompleteAddressInteractor,
        val registerCalls: ReceiveTurbine<Call.Register>,
        val onAutocompleteCalls: ReceiveTurbine<Call.OnAutocomplete>,
    )

    companion object {
        suspend fun test(
            autocompleteConfig: AutocompleteAddressInteractor.Config,
            test: suspend Scenario.() -> Unit,
        ) {
            val interactor = TestAutocompleteAddressInteractor(
                autocompleteConfig = autocompleteConfig,
            )

            val registerCalls = interactor.registerCalls
            val onAutocompleteCalls = interactor.onAutocompleteCalls

            test(
                Scenario(
                    interactor = interactor,
                    registerCalls = registerCalls,
                    onAutocompleteCalls = onAutocompleteCalls,
                )
            )

            registerCalls.ensureAllEventsConsumed()
            onAutocompleteCalls.ensureAllEventsConsumed()
        }

        fun noOp(
            autocompleteConfig: AutocompleteAddressInteractor.Config = AutocompleteAddressInteractor.Config(
                googlePlacesApiKey = null,
                autocompleteCountries = emptySet()
            ),
        ) = TestAutocompleteAddressInteractor(
            autocompleteConfig = autocompleteConfig,
        )
    }
}
