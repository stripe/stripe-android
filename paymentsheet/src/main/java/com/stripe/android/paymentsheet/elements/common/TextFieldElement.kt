package com.stripe.android.paymentsheet.elements.common

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.distinctUntilChanged
import com.stripe.android.paymentsheet.NotNullMutableLiveData
import com.stripe.android.paymentsheet.R

/**
 * This class will provide the onValueChanged and onFocusChanged functionality to the element's
 * composable.  These functions will update the observables as needed.  It is responsible for
 * exposing immutable observers for its data
 */
internal class TextFieldElement(
    private val textFieldConfig: TextFieldConfig,
    // This is here because it is useful to provide text to force the full/invalid/incomplete states
    private val shouldShowError: (TextFieldElementState, Boolean) -> Boolean = { state, hasFocus ->
        textFieldConfig.shouldShowError(state, hasFocus)
    },
    // This is here because it is useful to provide text to force the full/invalid/incomplete states
    private val determineState: (String) -> TextFieldElementState = {
        textFieldConfig.determineState(it)
    }
) {
    val debugLabel = textFieldConfig.debugLabel

    /** This is all the information that can be observed on the element */
    private val _input: NotNullMutableLiveData<String> = NotNullMutableLiveData("")
    val input: LiveData<String> = _input.distinctUntilChanged()

    private val _elementState: NotNullMutableLiveData<TextFieldElementState> =
        NotNullMutableLiveData(Error.ShowAlways)

    private val _hasFocus: NotNullMutableLiveData<Boolean> = NotNullMutableLiveData(false)

    private val _visibleError = MediatorLiveData<Boolean>().apply {
        addSource(_elementState) { postValue(shouldShowError(it, _hasFocus.value)) }
        addSource(_hasFocus) { postValue(shouldShowError(_elementState.value, it)) }
        postValue(false)
    }
    val visibleError: LiveData<Boolean> = _visibleError.distinctUntilChanged()
    val errorMessage: LiveData<Int> = Transformations.map(_visibleError) { visibleError ->
        _elementState.value.getErrorMessageResId()?.takeIf { visibleError }
    }
    val isFull = Transformations.map(_elementState) { it.isFull() }
    val isComplete = Transformations.map(_elementState) { it.isValid() }

    init {
        onValueChange("")
    }

    fun onValueChange(displayFormatted: String) {
        _input.value = textFieldConfig.filter(displayFormatted)

        val newState = determineState(_input.value)// Should be filtered value
        _elementState.value = newState
    }

    fun onFocusChange(newHasFocus: Boolean) {
        _hasFocus.value = newHasFocus
    }

    companion object {
        sealed class Valid : TextFieldElementState.TextFieldElementStateValid() {
            object Full : Valid() {
                override fun isFull() = true
            }
        }

        sealed class Error(stringResId: Int) :
            TextFieldElementState.TextFieldElementStateError(stringResId) {
            object ShowInFocus : Error(R.string.invalid)
            object ShowAlways : Error(R.string.invalid)
        }
    }
}