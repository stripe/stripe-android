package com.stripe.android.paymentsheet.ui

import android.content.Context
import android.content.ContextWrapper
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidViewBinding
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.PaymentMethodsUI
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.databinding.FragmentAchBinding
import com.stripe.android.paymentsheet.forms.FormFieldValues
import com.stripe.android.paymentsheet.paymentdatacollection.FormArguments
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel
import com.stripe.android.ui.core.forms.resources.LpmRepository
import com.stripe.android.uicore.image.StripeImageLoader
import kotlinx.coroutines.flow.Flow

@Composable
internal fun PaymentElement(
    sheetViewModel: BaseSheetViewModel,
    enabled: Boolean,
    supportedPaymentMethods: List<LpmRepository.SupportedPaymentMethod>,
    selectedItem: LpmRepository.SupportedPaymentMethod,
    showCheckboxFlow: Flow<Boolean>,
    onItemSelectedListener: (LpmRepository.SupportedPaymentMethod) -> Unit,
    formArguments: FormArguments,
    onFormFieldValuesChanged: (FormFieldValues?) -> Unit,
) {
    val context = LocalContext.current
    val imageLoader = remember {
        StripeImageLoader(context.applicationContext)
    }

    val horizontalPadding = dimensionResource(
        id = R.dimen.stripe_paymentsheet_outer_spacing_horizontal
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        if (supportedPaymentMethods.size > 1) {
            PaymentMethodsUI(
                selectedIndex = supportedPaymentMethods.indexOf(selectedItem),
                isEnabled = enabled,
                paymentMethods = supportedPaymentMethods,
                onItemSelectedListener = onItemSelectedListener,
                imageLoader = imageLoader,
                modifier = Modifier.padding(top = 26.dp, bottom = 12.dp),
            )
        }

        if (selectedItem.code == PaymentMethod.Type.USBankAccount.code) {
            // A temporary workaround until the USBankAccountFragment is migrated to Compose
            val activity = context.requireActivity()
            (activity as BaseSheetActivity<*>).formArgs = formArguments

            Column(modifier = Modifier.padding(horizontal = horizontalPadding)) {
                AndroidViewBinding(FragmentAchBinding::inflate)
            }
        } else {
            PaymentMethodForm(
                args = formArguments,
                enabled = enabled,
                onFormFieldValuesChanged = onFormFieldValuesChanged,
                showCheckboxFlow = showCheckboxFlow,
                injector = sheetViewModel.injector,
                modifier = Modifier.padding(horizontal = horizontalPadding)
            )
        }
    }
}

private fun Context.requireActivity(): ComponentActivity {
    var currentContext = this
    while (currentContext is ContextWrapper) {
        if (currentContext is ComponentActivity) {
            return currentContext
        }
        currentContext = currentContext.baseContext
    }
    error("Failed to find an Activity from the current Context")
}
