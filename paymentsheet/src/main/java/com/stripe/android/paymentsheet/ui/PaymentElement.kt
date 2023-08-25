package com.stripe.android.paymentsheet.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.dp
import com.stripe.android.link.LinkConfiguration
import com.stripe.android.link.LinkConfigurationCoordinator
import com.stripe.android.link.ui.inline.InlineSignupViewState
import com.stripe.android.link.ui.inline.LinkInlineSignup
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.PaymentMethodsUI
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.forms.FormFieldValues
import com.stripe.android.paymentsheet.paymentdatacollection.FormArguments
import com.stripe.android.paymentsheet.paymentdatacollection.ach.USBankAccountForm
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
    showLinkInlineSignup: Boolean,
    linkConfigurationCoordinator: LinkConfigurationCoordinator,
    showCheckboxFlow: Flow<Boolean>,
    onItemSelectedListener: (LpmRepository.SupportedPaymentMethod) -> Unit,
    onLinkSignupStateChanged: (LinkConfiguration, InlineSignupViewState) -> Unit,
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

    val primaryButtonState = sheetViewModel.primaryButtonState.collectAsState()

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

        Box(modifier = Modifier.animateContentSize()) {
            if (selectedItem.code == PaymentMethod.Type.USBankAccount.code) {
                USBankAccountForm(
                    formArgs = formArguments,
                    sheetViewModel = sheetViewModel,
                    isProcessing = primaryButtonState.value?.isProcessing == true,
                    modifier = Modifier.padding(horizontal = horizontalPadding),
                )
            } else {
                PaymentMethodForm(
                    args = formArguments,
                    enabled = enabled,
                    onFormFieldValuesChanged = onFormFieldValuesChanged,
                    showCheckboxFlow = showCheckboxFlow,
                    formViewModelSubComponentBuilderProvider = sheetViewModel.formViewModelSubComponentBuilderProvider,
                    modifier = Modifier.padding(horizontal = horizontalPadding)
                )
            }
        }

        if (showLinkInlineSignup) {
            LinkInlineSignup(
                linkConfigurationCoordinator = linkConfigurationCoordinator,
                enabled = enabled,
                onStateChanged = onLinkSignupStateChanged,
                modifier = Modifier
                    .padding(horizontal = horizontalPadding, vertical = 6.dp)
                    .fillMaxWidth()
            )
        }
    }
}
