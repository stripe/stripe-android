package com.stripe.android.paymentsheet.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.stripe.android.link.LinkConfiguration
import com.stripe.android.link.LinkConfigurationCoordinator
import com.stripe.android.link.ui.inline.InlineSignupViewState
import com.stripe.android.link.ui.inline.LinkInlineSignup
import com.stripe.android.link.ui.inline.LinkOptionalInlineSignup
import com.stripe.android.link.ui.inline.LinkSignupMode
import com.stripe.android.lpmfoundations.luxe.SupportedPaymentMethod
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.PaymentMethodsUI
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.forms.FormFieldValues
import com.stripe.android.paymentsheet.paymentdatacollection.FormArguments
import com.stripe.android.paymentsheet.paymentdatacollection.ach.USBankAccountForm
import com.stripe.android.paymentsheet.paymentdatacollection.ach.USBankAccountFormArguments
import com.stripe.android.uicore.elements.FormElement
import com.stripe.android.uicore.image.StripeImageLoader
import java.util.UUID

@Composable
internal fun PaymentElement(
    enabled: Boolean,
    supportedPaymentMethods: List<SupportedPaymentMethod>,
    selectedItem: SupportedPaymentMethod,
    formElements: List<FormElement>,
    linkSignupMode: LinkSignupMode?,
    linkConfigurationCoordinator: LinkConfigurationCoordinator?,
    onItemSelectedListener: (SupportedPaymentMethod) -> Unit,
    onLinkSignupStateChanged: (LinkConfiguration, InlineSignupViewState) -> Unit,
    formArguments: FormArguments,
    usBankAccountFormArguments: USBankAccountFormArguments,
    onFormFieldValuesChanged: (FormFieldValues?) -> Unit,
    onInteractionEvent: () -> Unit = {},
) {
    val context = LocalContext.current
    val imageLoader = remember {
        StripeImageLoader(context.applicationContext)
    }

    val horizontalPadding = dimensionResource(
        id = R.dimen.stripe_paymentsheet_outer_spacing_horizontal
    )

    val selectedIndex = remember(selectedItem, supportedPaymentMethods) {
        supportedPaymentMethods.map { it.code }.indexOf(selectedItem.code)
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        if (supportedPaymentMethods.size > 1) {
            PaymentMethodsUI(
                selectedIndex = selectedIndex,
                isEnabled = enabled,
                paymentMethods = supportedPaymentMethods,
                onItemSelectedListener = onItemSelectedListener,
                imageLoader = imageLoader,
                modifier = Modifier.padding(bottom = 12.dp),
            )
        }

        FormElement(
            enabled = enabled,
            selectedItem = selectedItem,
            formElements = formElements,
            formArguments = formArguments,
            usBankAccountFormArguments = usBankAccountFormArguments,
            horizontalPadding = horizontalPadding,
            onFormFieldValuesChanged = onFormFieldValuesChanged,
            onInteractionEvent = onInteractionEvent,
        )

        LinkElement(
            linkConfigurationCoordinator = linkConfigurationCoordinator,
            linkSignupMode = linkSignupMode,
            enabled = enabled,
            horizontalPadding = horizontalPadding,
            onLinkSignupStateChanged = onLinkSignupStateChanged,
        )
    }
}

@Composable
private fun FormElement(
    enabled: Boolean,
    selectedItem: SupportedPaymentMethod,
    formElements: List<FormElement>,
    formArguments: FormArguments,
    usBankAccountFormArguments: USBankAccountFormArguments,
    horizontalPadding: Dp,
    onFormFieldValuesChanged: (FormFieldValues?) -> Unit,
    onInteractionEvent: () -> Unit,
) {
    // The PaymentMethodForm has a reference to a FormViewModel, which is scoped to a key. This is to ensure that
    // the FormViewModel is recreated when the PaymentElement is recomposed.
    val uuid = rememberSaveable { UUID.randomUUID().toString() }

    Box(
        modifier = Modifier
            .animateContentSize()
            .pointerInput("AddPaymentMethod") {
                awaitEachGesture {
                    val gesture = awaitPointerEvent()

                    when (gesture.type) {
                        PointerEventType.Press -> onInteractionEvent()
                        else -> Unit
                    }
                }
            }
            .onFocusChanged { state ->
                if (state.hasFocus) {
                    onInteractionEvent()
                }
            }
    ) {
        if (selectedItem.code == PaymentMethod.Type.USBankAccount.code) {
            USBankAccountForm(
                formArgs = formArguments,
                usBankAccountFormArgs = usBankAccountFormArguments,
                modifier = Modifier.padding(horizontal = horizontalPadding),
            )
        } else {
            PaymentMethodForm(
                uuid = uuid,
                args = formArguments,
                enabled = enabled,
                onFormFieldValuesChanged = onFormFieldValuesChanged,
                formElements = formElements,
                modifier = Modifier.padding(horizontal = horizontalPadding)
            )
        }
    }
}

@Composable
private fun LinkElement(
    linkConfigurationCoordinator: LinkConfigurationCoordinator?,
    linkSignupMode: LinkSignupMode?,
    enabled: Boolean,
    horizontalPadding: Dp,
    onLinkSignupStateChanged: (LinkConfiguration, InlineSignupViewState) -> Unit,
) {
    if (linkConfigurationCoordinator != null && linkSignupMode != null) {
        when (linkSignupMode) {
            LinkSignupMode.InsteadOfSaveForFutureUse -> {
                LinkInlineSignup(
                    linkConfigurationCoordinator = linkConfigurationCoordinator,
                    enabled = enabled,
                    onStateChanged = onLinkSignupStateChanged,
                    modifier = Modifier
                        .padding(horizontal = horizontalPadding, vertical = 6.dp)
                        .fillMaxWidth(),
                )
            }
            LinkSignupMode.AlongsideSaveForFutureUse -> {
                LinkOptionalInlineSignup(
                    linkConfigurationCoordinator = linkConfigurationCoordinator,
                    enabled = enabled,
                    onStateChanged = onLinkSignupStateChanged,
                    modifier = Modifier
                        .padding(horizontal = horizontalPadding, vertical = 6.dp)
                        .fillMaxWidth(),
                )
            }
        }
    }
}
