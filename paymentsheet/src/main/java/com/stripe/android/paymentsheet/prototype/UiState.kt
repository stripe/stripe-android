package com.stripe.android.paymentsheet.prototype

import android.os.Parcelable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class UiState private constructor(
    private val _selected: MutableStateFlow<AddPaymentMethodUiDefinition>,
    private val available: List<AddPaymentMethodUiDefinition>,
    private val state: Map<Key<*>, MutableStateFlow<Value>>
) {
    interface Key<Value> {
        val identifier: String
    }

    interface Value : Parcelable {
        val key: Key<out Value>
    }

    val selected: StateFlow<AddPaymentMethodUiDefinition> = _selected.asStateFlow()

    fun updateSelected(selectedAddPaymentMethodUiDefinition: AddPaymentMethodUiDefinition) {
        _selected.value = selectedAddPaymentMethodUiDefinition
    }

    operator fun <V : Value> get(key: Key<V>): StateFlow<V> {
        @Suppress("UNCHECKED_CAST")
        return (state[key] as MutableStateFlow<V>).asStateFlow()
    }

    operator fun <V : Value> set(key: Key<V>, value: V) {
        @Suppress("UNCHECKED_CAST")
        (state[key] as MutableStateFlow<V>).value = value
    }

    fun <V : Value> update(key: Key<V>, block: (V) -> V) {
        @Suppress("UNCHECKED_CAST")
        val mutableStateFlow = state[key] as MutableStateFlow<V>
        mutableStateFlow.value = block(mutableStateFlow.value)
    }

    fun snapshot(): Snapshot {
        return Snapshot.create(this)
    }

    class Snapshot private constructor(
        private val state: Map<Key<*>, Value>
    ) {
        operator fun <V : Value> get(key: Key<V>): V {
            @Suppress("UNCHECKED_CAST")
            return state[key] as V
        }

        companion object {
            fun create(uiState: UiState): Snapshot {
                return Snapshot(uiState.state.map { it.key to it.value.value }.toMap())
            }
        }
    }

    companion object {
        // TODO: Need to setup default state for billing address collection stuff.
        fun create(initialStates: List<InitialAddPaymentMethodState>): UiState {
            return UiState(
                _selected = MutableStateFlow(initialStates.first().addPaymentMethodUiDefinition),
                available = initialStates.map { it.addPaymentMethodUiDefinition },
                state = initialStates.filter {
                    it.state != null
                }.associate {
                    it.state!!.key to MutableStateFlow(it.state)
                }
            )
        }
    }
}
