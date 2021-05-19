package com.stripe.android.compose.elements

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.distinctUntilChanged
import com.stripe.android.viewmodel.common.NotNullMutableLiveData
import compose.R

/**
 * This class will provide the onValueChanged and onFocusChanged functionality to the element's
 * composable.  These functions will update the observables as needed.  It is responsible for
 * exposing immutable observers for its data
 */
open class Element(
    private val config: ConfigInterface,
    val label: Int = config.label,
) {
    init {
        Log.d("APP", "creating new element.")
    }

    val debugLabel = config.debugLabel
    private val mutableElement: MutableElement = MutableElement(config)

    /** This is all the information that can be observed on the element */
    val value: LiveData<String> = mutableElement.value.distinctUntilChanged()
    val elementState: LiveData<ElementState> = mutableElement.elementState.distinctUntilChanged()
    val shouldShowError: LiveData<Boolean> = mutableElement.visibleError.distinctUntilChanged()
    private val hasFocus: LiveData<Boolean> = mutableElement.hasFocus.distinctUntilChanged()
    val isFull: LiveData<Boolean> = mutableElement.isFull.distinctUntilChanged()
    val errorMessage: LiveData<Int> = mutableElement.errorMessage.distinctUntilChanged()
    val isComplete: LiveData<Boolean> = mutableElement.isComplete.distinctUntilChanged()

    fun onValueChange(new: String) {
        mutableElement.onValueChange(new)
    }

    fun onFocusChange(newHasFocus: Boolean) {
        mutableElement.onFocusChange(newHasFocus)
    }
}

/**
 * This is the private class that has all the mutable live data.
 */
private data class MutableElement(val config: ConfigInterface) {
    val value: NotNullMutableLiveData<String> = NotNullMutableLiveData("")
    val hasFocus: NotNullMutableLiveData<Boolean> = NotNullMutableLiveData(false)
    val elementState: NotNullMutableLiveData<ElementState> = NotNullMutableLiveData(
        ElementState.ElementStateError(
            R.string.invalid
        )
    )
    val isDebug = true
    val isFull = Transformations.map(elementState) { it.isFull() }
    val isComplete = Transformations.map(elementState) { it.isValid() }
    val visibleError = MediatorLiveData<Boolean>().apply {
        addSource(elementState) { postValue(shouldShowError(it, hasFocus.value)) }
        addSource(hasFocus) { postValue(shouldShowError(elementState.value, it)) }
        postValue(false)
    }
    val errorMessage: LiveData<Int> = Transformations.map(visibleError) { visibleError ->
        elementState.value.getErrorMessageResId()?.takeIf { visibleError }
    }

    private val shouldShowErrorDebug: (ElementState, Boolean) -> Boolean = { state, hasFocus ->
        when (state) {
            is Valid.Full -> false
            is Error.ShowInFocus -> !hasFocus
            is Error.ShowAlways -> true
            else -> config.shouldShowError(state, hasFocus)
        }
    }

    private val shouldShowError: (ElementState, Boolean) -> Boolean =
        if (isDebug) {
            shouldShowErrorDebug
        } else { state, hasFocus ->
            config.shouldShowError(state, hasFocus)
        }

    private val determineStateDebug: (String) -> ElementState = { str ->
        when {
            str.contains("full") -> Valid.Full
            str.contains("focus") -> Error.ShowInFocus
            str.contains("always") -> Error.ShowAlways
            else -> config.determineState(str)
        }
    }

    private val determineState: (String) -> ElementState =
        if (isDebug) {
            determineStateDebug
        } else { str ->
            config.determineState(str)
        }

    init {
        onValueChange("")
    }

    fun onValueChange(new: String) {
        value.value = config.filter(new)
        val newState = determineState(value.value)// Should be filtered value
        elementState.value = newState
    }

    fun onFocusChange(newHasFocus: Boolean) {
        hasFocus.value = newHasFocus
    }

    companion object {
        sealed class Valid : ElementState.ElementStateValid() {
            object Full : Valid() {
                override fun isFull() = true
            }
        }

        sealed class Error(stringResId: Int) : ElementState.ElementStateError(stringResId) {
            object ShowInFocus : Error(R.string.invalid)
            object ShowAlways : Error(R.string.invalid)
        }
    }
}