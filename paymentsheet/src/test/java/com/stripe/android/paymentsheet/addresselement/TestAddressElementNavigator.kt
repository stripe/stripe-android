package com.stripe.android.paymentsheet.addresselement

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.Turbine
import com.stripe.android.elements.AddressLauncher
import com.stripe.android.elements.AddressLauncher.Result
import kotlinx.coroutines.flow.Flow

internal class TestAddressElementNavigator private constructor() : AddressElementNavigator {
    private val navigateToCalls = Turbine<Call.NavigateTo>()
    private val setResultCalls = Turbine<Call.SetResult>()
    private val getResultFlowCalls = Turbine<Call.GetResultFlow>()
    private val dismissCalls = Turbine<Call.Dismiss>()
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

    override fun dismiss(result: AddressLauncher.Result) {
        dismissCalls.add(Call.Dismiss(result))
    }

    override fun onBack() {
        onBackCalls.add(Call.OnBack)
    }

    sealed interface Call {
        data class NavigateTo(val target: AddressElementScreen) : Call
        data class SetResult(val key: String, val value: Any?) : Call
        data class GetResultFlow(val key: String) : Call
        data class Dismiss(val result: AddressLauncher.Result) : Call
        data object OnBack : Call
    }

    class Scenario(
        val navigator: AddressElementNavigator,
        val navigateToCalls: ReceiveTurbine<Call.NavigateTo>,
        val setResultCalls: ReceiveTurbine<Call.SetResult>,
        val getResultFlowCalls: ReceiveTurbine<Call.GetResultFlow>,
        val dismissCalls: ReceiveTurbine<Call.Dismiss>,
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
                    dismissCalls = navigator.dismissCalls,
                    onBackCalls = navigator.onBackCalls,
                )
            )

            navigator.navigateToCalls.ensureAllEventsConsumed()
            navigator.setResultCalls.ensureAllEventsConsumed()
            navigator.getResultFlowCalls.ensureAllEventsConsumed()
            navigator.dismissCalls.ensureAllEventsConsumed()
            navigator.onBackCalls.ensureAllEventsConsumed()
        }
    }
}
