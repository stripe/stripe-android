package com.stripe.android.paymentsheet.addresselement

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.Turbine
import com.stripe.android.uicore.elements.AutocompleteAddressInteractor

class TestAutocompleteAddressInteractor private constructor(
    override val autocompleteConfig: AutocompleteAddressInteractor.Config,
) : AutocompleteAddressInteractor {
    private val registerCalls = Turbine<Call.Register>()

    override fun register(onEvent: (AutocompleteAddressInteractor.Event) -> Unit) {
        registerCalls.add(Call.Register(onEvent))
    }

    sealed interface Call {
        class Register(
            val onEvent: (AutocompleteAddressInteractor.Event) -> Unit
        )
    }

    class Scenario(
        val interactor: AutocompleteAddressInteractor,
        val registerCalls: ReceiveTurbine<Call.Register>,
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

            test(
                Scenario(
                    interactor = interactor,
                    registerCalls = registerCalls,
                )
            )

            registerCalls.ensureAllEventsConsumed()
        }

        fun noOp(
            autocompleteConfig: AutocompleteAddressInteractor.Config = AutocompleteAddressInteractor.Config(
                autocompleteCountries = emptySet()
            ),
        ) = TestAutocompleteAddressInteractor(
            autocompleteConfig = autocompleteConfig,
        )

        fun noOpFactory() = AutocompleteAddressInteractor.Factory { noOp() }
    }
}
