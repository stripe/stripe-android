package com.stripe.android.paymentsheet.ui

import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.stripe.android.link.LinkPaymentLauncher
import com.stripe.android.link.model.AccountStatus
import com.stripe.android.link.ui.inline.InlineSignupViewState
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.paymentsheet.forms.FormFieldValues
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel
import com.stripe.android.ui.core.FieldValuesToParamsMapConverter
import com.stripe.android.ui.core.forms.resources.LpmRepository
import com.stripe.android.uicore.elements.IdentifierSpec
import kotlinx.coroutines.flow.MutableStateFlow

@Composable
internal fun AddPaymentMethod(
    sheetViewModel: BaseSheetViewModel,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val linkHandler = sheetViewModel.linkHandler
    val showCheckboxFlow = remember { MutableStateFlow(false) }

    val isRepositoryReady by sheetViewModel.isResourceRepositoryReady.collectAsState()
    val processing by sheetViewModel.processing.collectAsState(false)

    val linkConfig by linkHandler.linkConfiguration.collectAsState()
    val linkAccountStatus by linkConfig?.let {
        linkHandler.linkLauncher.getAccountStatusFlow(it).collectAsState(null)
    } ?: mutableStateOf(null)

    if (isRepositoryReady) {
        var selectedPaymentMethodCode: String by rememberSaveable {
            mutableStateOf(sheetViewModel.initiallySelectedPaymentMethodType)
        }

        val selectedItem = remember(selectedPaymentMethodCode) {
            sheetViewModel.supportedPaymentMethods.first {
                it.code == selectedPaymentMethodCode
            }
        }

        val showLinkInlineSignup = sheetViewModel.showLinkInlineSignupView(
            selectedPaymentMethodCode,
            linkAccountStatus
        )

        val arguments = remember(selectedItem, showLinkInlineSignup) {
            sheetViewModel.createFormArguments(selectedItem, showLinkInlineSignup)
        }

        LaunchedEffect(arguments) {
            showCheckboxFlow.emit(arguments.showCheckbox)
        }

        val paymentSelection by sheetViewModel.selection.collectAsState()
        val linkInlineSelection by linkHandler.linkInlineSelection.collectAsState()
        var linkSignupState by remember {
            mutableStateOf<InlineSignupViewState?>(null)
        }

        LaunchedEffect(paymentSelection, linkSignupState, linkInlineSelection) {
            val state = linkSignupState
            if (state != null) {
                sheetViewModel.onLinkSignupStateChanged(linkConfig!!, state, paymentSelection)
            } else if (linkInlineSelection != null) {
                (paymentSelection as? PaymentSelection.New.Card)?.let {
                    sheetViewModel.updatePrimaryButtonUIState(
                        PrimaryButton.UIState(
                            label = null,
                            onClick = {
                                sheetViewModel.payWithLinkInline(
                                    linkConfig!!,
                                    null
                                )
                            },
                            enabled = true,
                            visible = true
                        )
                    )
                }
            }
        }

        Column(modifier = modifier.fillMaxWidth()) {
            PaymentElement(
                sheetViewModel = sheetViewModel,
                enabled = !processing,
                supportedPaymentMethods = sheetViewModel.supportedPaymentMethods,
                selectedItem = selectedItem,
                showLinkInlineSignup = showLinkInlineSignup,
                linkPaymentLauncher = linkHandler.linkLauncher,
                showCheckboxFlow = showCheckboxFlow,
                onItemSelectedListener = { selectedLpm ->
                    if (selectedItem != selectedLpm) {
                        sheetViewModel.updatePrimaryButtonUIState(null)
                        selectedPaymentMethodCode = selectedLpm.code
                    }
                },
                onLinkSignupStateChanged = { _, inlineSignupViewState ->
                    linkSignupState = inlineSignupViewState
                },
                formArguments = arguments,
                onFormFieldValuesChanged = { formValues ->
                    val newSelection = formValues?.transformToPaymentSelection(
                        context = context,
                        paymentMethod = selectedItem,
                    )
                    sheetViewModel.updateSelection(newSelection)
                }
            )
        }
    } else {
        Loading()
    }
}

private val BaseSheetViewModel.initiallySelectedPaymentMethodType: PaymentMethodCode
    get() = when (val selection = newPaymentSelection) {
        is PaymentSelection.New.LinkInline -> PaymentMethod.Type.Card.code
        is PaymentSelection.New.Card,
        is PaymentSelection.New.USBankAccount,
        is PaymentSelection.New.GenericPaymentMethod -> selection.paymentMethodCreateParams.typeCode
        else -> supportedPaymentMethods.first().code
    }

private fun BaseSheetViewModel.showLinkInlineSignupView(
    paymentMethodCode: String,
    linkAccountStatus: AccountStatus?
): Boolean {
    val validStatusStates = setOf(
        AccountStatus.Verified,
        AccountStatus.NeedsVerification,
        AccountStatus.VerificationStarted,
        AccountStatus.SignedOut,
    )
    val linkInlineSelectionValid = linkHandler.linkInlineSelection.value != null
    return linkHandler.isLinkEnabled.value == true && stripeIntent.value
        ?.linkFundingSources?.contains(PaymentMethod.Type.Card.code) == true &&
        paymentMethodCode == PaymentMethod.Type.Card.code &&
        (linkAccountStatus in validStatusStates || linkInlineSelectionValid)
}

private fun BaseSheetViewModel.onLinkSignupStateChanged(
    config: LinkPaymentLauncher.Configuration,
    viewState: InlineSignupViewState,
    paymentSelection: PaymentSelection?
) {
    updatePrimaryButtonUIState(
        if (viewState.useLink) {
            val userInput = viewState.userInput
            if (userInput != null &&
                paymentSelection != null
            ) {
                PrimaryButton.UIState(
                    label = null,
                    onClick = { payWithLinkInline(config, userInput) },
                    enabled = true,
                    visible = true
                )
            } else {
                PrimaryButton.UIState(
                    label = null,
                    onClick = null,
                    enabled = false,
                    visible = true
                )
            }
        } else {
            null
        }
    )
}

@VisibleForTesting
internal fun FormFieldValues.transformToPaymentSelection(
    context: Context,
    paymentMethod: LpmRepository.SupportedPaymentMethod
): PaymentSelection.New {
    val params = FieldValuesToParamsMapConverter.transformToPaymentMethodCreateParams(
        fieldValuePairs = fieldValuePairs.filterNot { entry ->
            entry.key == IdentifierSpec.SaveForFutureUse || entry.key == IdentifierSpec.CardBrand
        },
        code = paymentMethod.code,
        requiresMandate = paymentMethod.requiresMandate,
    )

    return if (paymentMethod.code == PaymentMethod.Type.Card.code) {
        PaymentSelection.New.Card(
            paymentMethodCreateParams = params,
            brand = CardBrand.fromCode(fieldValuePairs[IdentifierSpec.CardBrand]?.value),
            customerRequestedSave = userRequestedReuse,
        )
    } else {
        PaymentSelection.New.GenericPaymentMethod(
            labelResource = context.getString(paymentMethod.displayNameResource),
            iconResource = paymentMethod.iconResource,
            lightThemeIconUrl = paymentMethod.lightThemeIconUrl,
            darkThemeIconUrl = paymentMethod.darkThemeIconUrl,
            paymentMethodCreateParams = params,
            customerRequestedSave = userRequestedReuse,
        )
    }
}
