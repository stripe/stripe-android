package com.stripe.android.paymentsheet.elements.common

import com.stripe.android.paymentsheet.elements.common.TextFieldStateConstants.*
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
    private val textFieldConfig: TextFieldConfig
) {
    val label: Int = textFieldConfig.label
    val debugLabel = textFieldConfig.debugLabel

    /** This is all the information that can be observed on the element */
    private val _input = MutableStateFlow("")
    val input: Flow<String> = _input

    private val _elementState =
        MutableStateFlow<TextFieldState>(Invalid.AlwaysError)

    private val _hasFocus = MutableStateFlow(false)

    val visibleError: Flow<Boolean> = combine(_elementState, _hasFocus) { elementState, hasFocus ->
        elementState.shouldShowError(hasFocus)
    }
    val errorMessage: Flow<Int?> = visibleError.map { visibleError ->
        _elementState.value.getErrorMessageResId()?.takeIf { visibleError }
    }

    val isFull: Flow<Boolean> = _elementState.map { it.isFull() }

    val isComplete: Flow<Boolean> = _elementState.map { it.isValid() }

    init {
        onValueChange("")
    }

    fun onValueChange(displayFormatted: String) {
        _input.value = textFieldConfig.filter(displayFormatted)

        // Should be filtered value
        _elementState.value = textFieldConfig.determineState(_input.value)
    }

    fun onFocusChange(newHasFocus: Boolean) {
        _hasFocus.value = newHasFocus
    }
}