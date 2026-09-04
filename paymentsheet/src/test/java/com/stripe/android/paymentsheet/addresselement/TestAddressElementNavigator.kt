package com.stripe.android.paymentsheet.addresselement

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.Turbine
import kotlinx.coroutines.flow.Flow

internal class TestAddressElementNavigator private constructor() : AddressElementNavigator {
    private val navigateToCalls = Turbine<Call.NavigateTo>()
    private val setResultCalls = Turbine<Call.SetResult>()
    private val getResultFlowCalls = Turbine<Call.GetResultFlow>()
    private val onBackCalls = Turbine<Call.OnBack>()

    override fun navigateTo(target: AddressElementScreen) {
        navigateToCalls.add(Call.NavigateTo(target))
    }

    override fun setResult(key: String, value: Any?) {
        setResultCalls.add(Call.SetResult(key, value))
    }

    override fun <T> getResultFlow(key: String): Flow<T>? {
        getResultFlowCalls.add(Call.GetResultFlow(key))

        return null
    }

    override fun onBack(): Boolean {
        onBackCalls.add(Call.OnBack)
        return true
    }

    sealed interface Call {
        data class NavigateTo(val target: AddressElementScreen) : Call
        data class SetResult(val key: String, val value: Any?) : Call
        data class GetResultFlow(val key: String) : Call
        data object OnBack : Call
    }

    class Scenario(
        val navigator: AddressElementNavigator,
        val navigateToCalls: ReceiveTurbine<Call.NavigateTo>,
        val setResultCalls: ReceiveTurbine<Call.SetResult>,
        val getResultFlowCalls: ReceiveTurbine<Call.GetResultFlow>,
        val onBackCalls: ReceiveTurbine<Call.OnBack>,
    )

    companion object {
        suspend fun test(
            test: suspend Scenario.() -> Unit,
        ) {
            val navigator = TestAddressElementNavigator()

            test(
                Scenario(
                    navigator = navigator,
                    navigateToCalls = navigator.navigateToCalls,
                    setResultCalls = navigator.setResultCalls,
                    getResultFlowCalls = navigator.getResultFlowCalls,
                    onBackCalls = navigator.onBackCalls,
                )
            )

            navigator.navigateToCalls.ensureAllEventsConsumed()
            navigator.setResultCalls.ensureAllEventsConsumed()
            navigator.getResultFlowCalls.ensureAllEventsConsumed()
            navigator.onBackCalls.ensureAllEventsConsumed()
        }
    }
}
