package com.stripe.android.lpmfoundations

import android.os.Parcelable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The current UI state for the entire LPM form (including which LPM is selected, as well as the values in the form
 *  fields).
 *
 *  This class is mutable, but an immutable variant is available via the [snapshot] method.
 *
 *  This class acts a lot like a [MutableMap], but emits updates as a [StateFlow].
 */
internal class UiState private constructor(
    private val state: Map<Key<*>, MutableStateFlow<Value>>,
    private val mutableSelected: MutableStateFlow<AddPaymentMethodUiDefinition>,
    /**
     * The available [AddPaymentMethodUiDefinition] that can be used.
     *
     * Typically used by the UI to display all [AddPaymentMethodUiDefinition].
     *
     * When they user clicks on an associated [AddPaymentMethodUiDefinition], it would be updated by calling
     *  [updateSelected].
     */
    val available: List<AddPaymentMethodUiDefinition>,
) {
    /** The key used to reference an associated [Value]. */
    interface Key<Value> {
        /** The identifier of the [Key], used to save and restore from a [Parcelable]. */
        val identifier: String
    }

    /** Used to store arbitrary data related to the state of the UI. */
    interface Value : Parcelable {
        /** The associated [Key], used to store this [Value] in a [Map]. */
        val key: Key<out Value>
    }

    /**
     * A [StateFlow] of the selected [AddPaymentMethodUiDefinition].
     *
     * To be consumed by the UI for what to display.
     */
    val selected: StateFlow<AddPaymentMethodUiDefinition> = mutableSelected.asStateFlow()

    /**
     * Updates the selected [AddPaymentMethodUiDefinition].
     *
     * Typically called from the UI after a user clicks an associated payment method.
     */
    fun updateSelected(selectedAddPaymentMethodUiDefinition: AddPaymentMethodUiDefinition) {
        mutableSelected.value = selectedAddPaymentMethodUiDefinition
    }

    /**
     * Retrieves a [StateFlow] of the values associated with the given [Key].
     */
    operator fun <V : Value> get(key: Key<V>): StateFlow<V> {
        @Suppress("UNCHECKED_CAST")
        return (state[key] as MutableStateFlow<V>).asStateFlow()
    }

    /**
     * Updates the [Value], and emits it to any listeners.
     */
    fun <V : Value> set(value: V) {
        @Suppress("UNCHECKED_CAST")
        (state[value.key] as MutableStateFlow<V>).value = value
    }

    /**
     * A helper method to make updating [Value]s that are data classes easier.
     */
    fun <V : Value> update(key: Key<V>, block: V.() -> V) {
        @Suppress("UNCHECKED_CAST")
        val mutableStateFlow = state[key] as MutableStateFlow<V>
        mutableStateFlow.value = mutableStateFlow.value.block()
    }

    /**
     * Creates an immutable copy of the current state.
     */
    fun snapshot(): Snapshot {
        return Snapshot.create(this)
    }

    /**
     * An immutable "Snapshot" of the current [UiState].
     */
    class Snapshot private constructor(
        private val state: Map<Key<*>, Value>
    ) {
        /**
         * Retrieves the associated [Value] for a given [Key].
         */
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
        // TODO(jaynewstrom): Need to setup default state for billing address collection stuff.
        fun create(initialStates: List<InitialAddPaymentMethodState>): UiState {
            return UiState(
                state = initialStates.filter {
                    it.state != null
                }.associate {
                    it.state!!.key to MutableStateFlow(it.state)
                },
                mutableSelected = MutableStateFlow(initialStates.first().addPaymentMethodUiDefinition),
                available = initialStates.map { it.addPaymentMethodUiDefinition },
            )
        }
    }
}
