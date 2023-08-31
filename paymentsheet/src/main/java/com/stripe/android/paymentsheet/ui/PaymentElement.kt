package com.stripe.android.paymentsheet.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.dp
import com.stripe.android.link.LinkConfiguration
import com.stripe.android.link.LinkConfigurationCoordinator
import com.stripe.android.link.ui.inline.InlineSignupViewState
import com.stripe.android.link.ui.inline.LinkInlineSignup
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentsheet.PaymentMethodsUI
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.addresselement.AddressDetails
import com.stripe.android.paymentsheet.forms.FormViewModel
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.paymentdatacollection.FormArguments
import com.stripe.android.paymentsheet.paymentdatacollection.ach.USBankAccountForm
import com.stripe.android.ui.core.forms.resources.LpmRepository
import com.stripe.android.uicore.image.StripeImageLoader

@Composable
internal fun PaymentElement(
    primaryButtonState: PrimaryButton.State?,
    onBehalfOf: String?,
    stripeIntent: StripeIntent?,
    isCompleteFlow: Boolean,
    savedPaymentSelection: PaymentSelection?,
    shippingDetails: AddressDetails?,
    enabled: Boolean,
    supportedPaymentMethods: List<LpmRepository.SupportedPaymentMethod>,
    selectedItem: LpmRepository.SupportedPaymentMethod,
    showLinkInlineSignup: Boolean,
    linkConfigurationCoordinator: LinkConfigurationCoordinator?,
    onItemSelectedListener: (LpmRepository.SupportedPaymentMethod) -> Unit,
    onLinkSignupStateChanged: (LinkConfiguration, InlineSignupViewState) -> Unit,
    confirmUSBankAccount: (PaymentSelection.New.USBankAccount) -> Unit,
    updateCustomPrimaryButtonUiState: ((PrimaryButton.UIState?) -> PrimaryButton.UIState?) -> Unit,
    updatePrimaryButton: (text: String, enabled: Boolean, shouldShowProcessing: Boolean, onClick: () -> Unit) -> Unit,
    updateMandateText: (mandateText: String?) -> Unit,
    onError: (String?) -> Unit,
    formArguments: FormArguments,
    formViewData: FormViewModel.ViewData,
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

        Box(modifier = Modifier.animateContentSize()) {
            if (selectedItem.code == PaymentMethod.Type.USBankAccount.code) {
                USBankAccountForm(
                    formArgs = formArguments,
                    onBehalfOf = onBehalfOf,
                    isCompleteFlow = isCompleteFlow,
                    isPaymentFlow = stripeIntent is PaymentIntent,
                    stripeIntentId = stripeIntent?.id,
                    clientSecret = stripeIntent?.clientSecret,
                    savedPaymentSelection = savedPaymentSelection as? PaymentSelection.New.USBankAccount,
                    shippingDetails = shippingDetails,
                    isProcessing = primaryButtonState?.isProcessing == true,
                    confirmUSBankAccount = confirmUSBankAccount,
                    updateCustomPrimaryButtonUiState = updateCustomPrimaryButtonUiState,
                    updatePrimaryButton = updatePrimaryButton,
                    updateMandateText = updateMandateText,
                    onError = onError,
                    modifier = Modifier.padding(horizontal = horizontalPadding),
                )
            } else {
                PaymentMethodForm(
                    enabled = enabled,
                    hiddenIdentifiers = formViewData.hiddenIdentifiers,
                    elements = formViewData.elements,
                    lastTextFieldIdentifier = formViewData.lastTextFieldIdentifier,
                    modifier = Modifier.padding(horizontal = horizontalPadding),
                )
            }
        }

        if (showLinkInlineSignup && linkConfigurationCoordinator != null) {
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
