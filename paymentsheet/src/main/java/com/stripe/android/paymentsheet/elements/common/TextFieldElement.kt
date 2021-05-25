package com.stripe.android.paymentsheet.elements.common

import android.util.Log
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
internal open class TextFieldElement(private val textFieldConfig: TextFieldConfigInterface) {
    val debugLabel = textFieldConfig.debugLabel
    private val isDebug = true

    /** This is all the information that can be observed on the element */
    private val _input: NotNullMutableLiveData<String> = NotNullMutableLiveData("")
    val input: LiveData<String> = _input.distinctUntilChanged()

    private val _elementState: NotNullMutableLiveData<TextFieldElementState> =
        NotNullMutableLiveData(
            TextFieldElementState.TextFieldElementStateError(
                R.string.invalid
            )
        )

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

    private val shouldShowErrorDebug: (TextFieldElementState, Boolean) -> Boolean =
        { state, hasFocus ->
            when (state) {
                is Valid.Full -> false
                is Error.ShowInFocus -> !hasFocus
                is Error.ShowAlways -> true
                else -> textFieldConfig.shouldShowError(state, hasFocus)
            }
        }

    private val shouldShowError: (TextFieldElementState, Boolean) -> Boolean =
        if (isDebug) {
            shouldShowErrorDebug
        } else { state, hasFocus ->
            textFieldConfig.shouldShowError(state, hasFocus)
        }

    private val determineStateDebug: (String?) -> TextFieldElementState = { str ->
        when {
            str == null -> textFieldConfig.determineState(str)
            str.contains("full") -> Valid.Full
            str.contains("focus") -> Error.ShowInFocus
            str.contains("always") -> Error.ShowAlways
            else -> textFieldConfig.determineState(str)
        }
    }

    private val determineState: (String?) -> TextFieldElementState =
        if (isDebug) {
            determineStateDebug
        } else { str ->
            textFieldConfig.determineState(str)
        }

    init {
        Log.d("APP", "creating new element.")
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
