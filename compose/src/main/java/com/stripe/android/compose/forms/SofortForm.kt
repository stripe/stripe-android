@file:Suppress("UNNECESSARY_SAFE_CALL")

package com.stripe.android.compose.forms

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.ViewModel
import com.stripe.android.compose.elements.*
import compose.R

@Composable
fun SofortForm(
    viewModel: SofortFormViewModel,
) {
    // TODO: Do these need to be saved?
    val name = FocusRequester()
    val email = FocusRequester()

    val selectedCountry by viewModel.countryElement.value.observeAsState("")
    val nameErrorMessage by viewModel.nameElement.errorMessage.observeAsState(null)
    val emailErrorMessage by viewModel.emailElement.errorMessage.observeAsState(null)
    val countryErrorMessage by viewModel.countryElement.errorMessage.observeAsState(null)

    val countries = viewModel.countryElement.countries

    Column(modifier = Modifier.padding(top = 30.dp, start = 16.dp, end = 16.dp)) {
        Section(R.string.name, nameErrorMessage) {
            SimpleTextFieldElement(
                element = viewModel.nameElement,
                myFocus = name,
                nextFocus = email,
            )
        }
        Section(R.string.email, emailErrorMessage) {
            SimpleTextFieldElement(
                element = viewModel.emailElement,
                myFocus = email,
                nextFocus = null,
            )
        }

        Section(R.string.country, countryErrorMessage) {
            // TODO: THis should have my and next focus elements.
            AutoComplete(
                items = countries,
                selectedItem = selectedCountry,
                onValueChange = {viewModel.countryElement.onValueChange(it) }
            )
        }
    }
}

class SofortFormViewModel : ViewModel() {
    // Save the states of each fields and get observables setup
    // particularly for error message and error message states.

    var nameElement = Element(Name())
    var emailElement = Element(Email())
    var countryElement = CountryElement()

//    val nameAndEmailError = MediatorLiveData<Int>().apply {
//        addSource(nameElement.errorMessage) {
//            postValue(getDominantError(it, emailElement.errorMessage.value))
//        }
//        addSource(emailElement.errorMessage) {
//            postValue(getDominantError(nameElement.errorMessage.value, it))
//        }
//    }
//
//    private fun getDominantError(nameError: Int?, emailError: String?) =
//        nameError ?: (emailError ?: "")

    val params: LiveData<String?> = MediatorLiveData<String>().apply {
        addSource(nameElement.value) { postValue(getParams()) }
        addSource(nameElement.isComplete) { postValue(getParams()) }
        addSource(emailElement.value) { postValue(getParams()) }
        addSource(emailElement.isComplete) { postValue(getParams()) }
        addSource(countryElement.value) { postValue(getParams()) }
        addSource(countryElement.isComplete) { postValue(getParams()) }
    }

    private fun getParams() =
        "name: ${nameElement.value.value}, email: ${emailElement.value.value}, country: ${countryElement.value.value}".takeIf {
            Log.e(
                "APP",
                "name: ${nameElement.isComplete.value}, email: ${emailElement.isComplete.value}, country: ${countryElement.isComplete.value}"
            )
            (nameElement.isComplete.value ?: false && emailElement.isComplete.value ?: false && countryElement.isComplete.value ?: false)
        }
}