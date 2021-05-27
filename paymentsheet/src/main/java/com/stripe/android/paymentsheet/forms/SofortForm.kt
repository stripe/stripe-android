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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.elements.EmailConfig
import com.stripe.android.paymentsheet.elements.NameConfig
import com.stripe.android.paymentsheet.elements.Section
import com.stripe.android.paymentsheet.elements.common.DropDown
import com.stripe.android.paymentsheet.elements.common.DropdownElement
import com.stripe.android.paymentsheet.elements.common.TextField
import com.stripe.android.paymentsheet.elements.common.TextFieldElement
import com.stripe.android.paymentsheet.elements.country.CountryConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

@Composable
internal fun SofortForm(
    viewModel: SofortFormViewModel,
) {
    val name = FocusRequester()
    val email = FocusRequester()

    val nameErrorMessage by viewModel.nameElement.errorMessage.asLiveData().observeAsState(null)
    val emailErrorMessage by viewModel.emailElement.errorMessage.asLiveData().observeAsState(null)

    Column(modifier = Modifier.padding(top = 30.dp, start = 16.dp, end = 16.dp)) {
        Section(R.string.address_label_name, nameErrorMessage) {
            TextField(
                textFieldElement = viewModel.nameElement,
                myFocus = name,
                nextFocus = email,
            )
        }

        Section(R.string.becs_widget_email, emailErrorMessage) {
            TextField(
                textFieldElement = viewModel.emailElement,
                myFocus = email,
                nextFocus = null,
            )
        }

        Section(R.string.address_label_country, null) {
            DropDown(
                element = viewModel.countryElement
            )
        }
    }
}

internal class SofortFormViewModel : ViewModel() {
    internal var nameElement = TextFieldElement(NameConfig())
    internal var emailElement = TextFieldElement(EmailConfig())
    internal var countryElement = DropdownElement(CountryConfig())

    val params: Flow<PaymentMethodCreateParams?> = combine(
        nameElement.input,
        nameElement.isComplete,
        emailElement.input,
        emailElement.isComplete,
        countryElement.paymentMethodParams
    ) { nameInput, nameComplete, emailInput, emailComplete, countryPaymentMethodParams ->
        getParams(nameInput, nameComplete, emailInput, emailComplete, countryPaymentMethodParams)
    }

    private fun getParams(
        nameInput: String,
        nameComplete: Boolean,
        emailInput: String,
        emailComplete: Boolean,
        countryPaymentMethodParams: String
    ): PaymentMethodCreateParams? {
        Log.d(
            "Stripe",
            "name: $nameComplete, email: $emailComplete}"
        )
        return if (nameComplete && emailComplete) {

            PaymentMethodCreateParams.create(
                PaymentMethodCreateParams.Sofort(countryPaymentMethodParams),
                PaymentMethod.BillingDetails(
                    name = nameInput,
                    email = emailInput
                )
            )
        } else {
            null
        }
    }
}
