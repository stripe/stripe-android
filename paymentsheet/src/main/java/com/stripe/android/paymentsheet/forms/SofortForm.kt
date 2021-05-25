package com.stripe.android.paymentsheet.forms

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
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.elements.DropdownElement
import com.stripe.android.paymentsheet.elements.EmailConfig
import com.stripe.android.paymentsheet.elements.NameConfig
import com.stripe.android.paymentsheet.elements.Section
import com.stripe.android.paymentsheet.elements.TextFieldComposable
import com.stripe.android.paymentsheet.elements.common.DropdownElement
import com.stripe.android.paymentsheet.elements.common.TextFieldElement
import com.stripe.android.paymentsheet.elements.country.CountryConfig

@Composable
fun SofortForm(
    viewModel: SofortFormViewModel,
) {
    // TODO: Do these need to be saved?
    val name = FocusRequester()
    val email = FocusRequester()

    val nameErrorMessage by viewModel.nameElement.errorMessage.observeAsState(null)
    val emailErrorMessage by viewModel.emailElement.errorMessage.observeAsState(null)

    Column(modifier = Modifier.padding(top = 30.dp, start = 16.dp, end = 16.dp)) {
        Section(R.string.address_label_name, nameErrorMessage) {
            TextFieldComposable(
                textFieldElement = viewModel.nameElement,
                myFocus = name,
                nextFocus = email,
            )
        }

        Section(R.string.becs_widget_email, emailErrorMessage) {
            TextFieldComposable(
                textFieldElement = viewModel.emailElement,
                myFocus = email,
                nextFocus = null,
            )
        }

        Section(R.string.address_label_country, null) {
            DropdownElement(
                element = viewModel.countryElement
            )
        }
    }
}

class SofortFormViewModel : ViewModel() {
    internal var nameElement = TextFieldElement(NameConfig())
    internal var emailElement = TextFieldElement(EmailConfig())
    internal var countryElement = DropdownElement(CountryConfig())

    val params: LiveData<PaymentMethodCreateParams?> =
        MediatorLiveData<PaymentMethodCreateParams?>().apply {
            addSource(nameElement.input) { postValue(getParams()) }
            addSource(nameElement.isComplete) { postValue(getParams()) }
            addSource(emailElement.input) { postValue(getParams()) }
            addSource(emailElement.isComplete) { postValue(getParams()) }

            // Country is a dropdown and so will always be complete.
            addSource(countryElement.paramValue) { postValue(getParams()) }
        }

    private fun getParams(): PaymentMethodCreateParams? {
        Log.d(
            "APP",
            "name: ${nameElement.isComplete.value}, email: ${emailElement.isComplete.value}, country: ${countryElement.isComplete}"
        )
        return if (nameElement.isComplete.value == true && emailElement.isComplete.value == true && countryElement.isComplete) {

            PaymentMethodCreateParams.create(
                PaymentMethodCreateParams.Sofort(requireNotNull(countryElement.paramValue.value)),
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
