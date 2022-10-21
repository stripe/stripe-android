package com.stripe.android.elements

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidViewBinding
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.PaymentMethodsUI
import com.stripe.android.paymentsheet.databinding.FragmentAchBinding
import com.stripe.android.paymentsheet.forms.FormFieldValues
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.paymentdatacollection.FormFragmentArguments
import com.stripe.android.paymentsheet.ui.Loading
import com.stripe.android.paymentsheet.ui.PaymentMethodForm
import com.stripe.android.ui.core.PaymentsTheme
import com.stripe.android.ui.core.forms.resources.LpmRepository
import com.stripe.android.ui.core.injection.NonFallbackInjector
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.launch

val PaymentElementHorizontalPadding = 20.dp

@Composable
fun PaymentElement(
    controller: PaymentElementController,
    enabled: Boolean,
    onPaymentMethodSelected: (PaymentSelection?) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var factory by remember {
        mutableStateOf<ViewModelProvider.Factory?>(null)
    }

    LaunchedEffect(controller) {
        coroutineScope.launch {
            factory = controller.getViewModelFactory()
        }
    }

    PaymentsTheme {
        (factory as? PaymentElementViewModel.Factory)?.let { factory ->
            val viewModel: PaymentElementViewModel = viewModel(
                factory = factory
            )

            val paymentSelection by viewModel.paymentSelectionFlow.collectAsState()

            LaunchedEffect(paymentSelection) {
                onPaymentMethodSelected(paymentSelection)
            }

            PaymentElement(
                viewModel = viewModel,
                enabled = enabled,
                showCheckbox = false,
                injector = factory.injector
            )
        } ?: run {
            Loading()
        }
    }
}

@Composable
internal fun PaymentElement(
    viewModel: PaymentElementViewModel,
    enabled: Boolean,
    showCheckbox: Boolean,
    injector: NonFallbackInjector,
    modifier: Modifier = Modifier
) {
    val selectedItem by viewModel.selectedPaymentMethod.collectAsState()
    val formArguments by viewModel.formArgumentsFlow.collectAsState()

    PaymentElement(
        enabled = enabled,
        supportedPaymentMethods = viewModel.supportedPaymentMethods,
        selectedItem = selectedItem,
        showCheckbox = showCheckbox,
        onItemSelectedListener = viewModel::onPaymentMethodSelected,
        formArguments = formArguments,
        onFormFieldValuesChanged = viewModel::onFormFieldValuesChanged,
        injector = injector,
        modifier = modifier
    )
}

@OptIn(FlowPreview::class)
@Composable
internal fun PaymentElement(
    enabled: Boolean,
    supportedPaymentMethods: List<LpmRepository.SupportedPaymentMethod>,
    selectedItem: LpmRepository.SupportedPaymentMethod,
    showCheckbox: Boolean,
    onItemSelectedListener: (LpmRepository.SupportedPaymentMethod) -> Unit,
    formArguments: FormFragmentArguments,
    onFormFieldValuesChanged: (FormFieldValues?) -> Unit,
    injector: NonFallbackInjector,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        if (supportedPaymentMethods.size > 1) {
            PaymentMethodsUI(
                selectedIndex = supportedPaymentMethods.indexOf(
                    selectedItem
                ),
                isEnabled = enabled,
                paymentMethods = supportedPaymentMethods,
                onItemSelectedListener = onItemSelectedListener
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (selectedItem.code == PaymentMethod.Type.USBankAccount.code) {
            Column(modifier = Modifier.padding(horizontal = PaymentElementHorizontalPadding)) {
                AndroidViewBinding(FragmentAchBinding::inflate)
            }
        } else {
            PaymentMethodForm(
                args = formArguments,
                enabled = enabled,
                showCheckbox = showCheckbox,
                onFormFieldValuesChanged = onFormFieldValuesChanged,
                injector = injector,
                modifier = Modifier.padding(horizontal = PaymentElementHorizontalPadding)
            )
        }
    }
}
