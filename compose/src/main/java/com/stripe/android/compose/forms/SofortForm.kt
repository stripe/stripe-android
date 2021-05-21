package com.stripe.android.compose.forms

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.ViewModel
import com.stripe.android.compose.elements.AutoComplete
import com.stripe.android.compose.elements.CountryElement
import com.stripe.android.compose.elements.Element
import com.stripe.android.compose.elements.Email
import com.stripe.android.compose.elements.Name
import com.stripe.android.compose.elements.Section
import com.stripe.android.compose.elements.SimpleTextFieldElement
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import compose.R

@Composable
fun SofortForm(
    viewModel: SofortFormViewModel,
) {
    // TODO: Do these need to be saved?
    val name = FocusRequester()
    val email = FocusRequester()

    val selectedCountry by viewModel.countryElement.input.observeAsState("")
    val nameErrorMessage by viewModel.nameElement.errorMessage.observeAsState(null)
    val emailErrorMessage by viewModel.emailElement.errorMessage.observeAsState(null)
    val countryErrorMessage by viewModel.countryElement.errorMessage.observeAsState(null)

    val countries = viewModel.countryElement.countries

    Column(modifier = Modifier.padding(top = 30.dp, start = 16.dp, end = 16.dp)) {
        Section(R.string.address_label_name, nameErrorMessage) {
            SimpleTextFieldElement(
                element = viewModel.nameElement,
                myFocus = name,
                nextFocus = email,
            )
        }
        Section(R.string.becs_widget_email, emailErrorMessage) {
            SimpleTextFieldElement(
                element = viewModel.emailElement,
                myFocus = email,
                nextFocus = null,
            )
        }

        Section(R.string.address_label_country, countryErrorMessage) {
            // TODO: THis should have my and next focus elements.
            AutoComplete(
                items = countries,
                selectedItem = selectedCountry,
                onValueChange = { viewModel.countryElement.onValueChange(it) }
            )
        }
    }
}

class SofortFormViewModel : ViewModel() {
    var nameElement = Element(Name())
    var emailElement = Element(Email())
    var countryElement = CountryElement()

    val params: LiveData<PaymentMethodCreateParams?> =
        MediatorLiveData<PaymentMethodCreateParams?>().apply {
            addSource(nameElement.input) { postValue(getParams()) }
            addSource(nameElement.isComplete) { postValue(getParams()) }
            addSource(emailElement.input) { postValue(getParams()) }
            addSource(emailElement.isComplete) { postValue(getParams()) }
            addSource(countryElement.input) { postValue(getParams()) }
            addSource(countryElement.isComplete) { postValue(getParams()) }
        }

    private fun getParams(): PaymentMethodCreateParams? {
        Log.d(
            "APP",
            "name: ${nameElement.isComplete.value}, email: ${emailElement.isComplete.value}, country: ${countryElement.isComplete.value}"
        )
        return if (nameElement.isComplete.value == true && emailElement.isComplete.value == true && countryElement.isComplete.value == true) {

            PaymentMethodCreateParams.create(
                PaymentMethodCreateParams.Sofort(requireNotNull(countryElement.input.value)),
                PaymentMethod.BillingDetails(
                    name = nameElement.input.value,
                    email = emailElement.input.value
                )
            )

        } else {
            null
        }
    }
}
