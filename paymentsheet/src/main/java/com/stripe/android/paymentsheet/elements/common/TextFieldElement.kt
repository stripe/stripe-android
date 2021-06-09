package com.stripe.android.paymentsheet.elements.common

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

/**
 * This class will provide the onValueChanged and onFocusChanged functionality to the element's
 * composable.  These functions will update the observables as needed.  It is responsible for
 * exposing immutable observers for its data
 */
internal class TextFieldElement(
    private val textFieldConfig: TextFieldConfig,
    // This is here because it is useful to provide text to force the full/invalid/incomplete states
    private val shouldShowError: (TextFieldElementState.Invalid, Boolean) -> Boolean = { invalidState, hasFocus ->
        invalidState.shouldShowError(invalidState, hasFocus)
    },
    // This is here because it is useful to provide text to force the full/invalid/incomplete states
    private val determineState: (String) -> TextFieldElementState = {
        textFieldConfig.determineState(it)
    }
) {
    val label: Int = textFieldConfig.label
    val debugLabel = textFieldConfig.debugLabel

    /** This is all the information that can be observed on the element */
    private val _input = MutableStateFlow("")
    val input: Flow<String> = _input

    private val _hasFocus = MutableStateFlow(false)

    private val _elementState = MutableStateFlow<TextFieldElementState>(Error.ShowAlways)
    private val _invalidState = _elementState.map {
        it as? TextFieldElementState.Invalid
    }

    val visibleError: Flow<Boolean> = combine(_invalidState, _hasFocus) { invalidState, hasFocus ->
        invalidState?.let { shouldShowError(invalidState, hasFocus) } ?: false
    }

    val errorMessage: Flow<Int?> =
        combine(visibleError, _invalidState) { visibleError, invalidState ->
            invalidState?.getErrorMessageResId()?.takeIf { visibleError }
        }

    val isFull: Flow<Boolean> = _elementState.map {
        (it as? TextFieldElementState.Valid)?.isFull() ?: false
    }

    val isComplete: Flow<Boolean> = _elementState.map { it is TextFieldElementState.Valid }

    init {
        onValueChange("")
    }

    fun onValueChange(displayFormatted: String) {
        _input.value = textFieldConfig.filter(displayFormatted)

        // Should be filtered value
        _elementState.value = determineState(_input.value)
    }

    fun onFocusChange(newHasFocus: Boolean) {
        _hasFocus.value = newHasFocus
    }
}