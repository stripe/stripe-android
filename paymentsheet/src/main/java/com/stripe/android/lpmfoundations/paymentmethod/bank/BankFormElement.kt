package com.stripe.android.lpmfoundations.paymentmethod.bank

import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.paymentsheet.model.PaymentSelection.New
import com.stripe.android.paymentsheet.paymentdatacollection.FormArguments
import com.stripe.android.paymentsheet.paymentdatacollection.ach.USBankAccountForm
import com.stripe.android.paymentsheet.paymentdatacollection.ach.USBankAccountFormArguments
import com.stripe.android.paymentsheet.paymentdatacollection.ach.USBankAccountFormViewModel
import com.stripe.android.paymentsheet.ui.PrimaryButton
import com.stripe.android.ui.core.elements.RenderableFormElement
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.forms.FormFieldEntry
import com.stripe.android.uicore.utils.collectAsState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach

internal class BankFormElement(
    private val formArguments: FormArguments,
    private val usBankAccountFormArgs: USBankAccountFormArguments,
) : RenderableFormElement(
    allowsUserInteraction = true,
    identifier = IdentifierSpec.Generic("bank_form")
) {

    private val formFieldValues = MutableStateFlow<List<Pair<IdentifierSpec, FormFieldEntry>>>(emptyList())

    override fun getFormFieldValueFlow(): StateFlow<List<Pair<IdentifierSpec, FormFieldEntry>>> {
        return formFieldValues.asStateFlow()
    }

    @Composable
    override fun ComposeUI(enabled: Boolean) {
        val activityResultRegistryOwner = LocalActivityResultRegistryOwner.current

        val viewModel = viewModel<USBankAccountFormViewModel>(
            factory = USBankAccountFormViewModel.Factory {
                makeViewModelArgs()
            },
        )

        LaunchedEffect(viewModel) {
            viewModel
                .formFieldValues
                .onEach { formFieldValues ->
                    val state = viewModel.currentScreenState.value

                    handleFormFieldValuesChanged(
                        formFieldValues = formFieldValues,
                        label = state.primaryButtonText,
                        mandateText = state.mandateText,
                        collectBankAccount = viewModel::collectBankAccount,
                    )
                }
                .collect()
        }

        val screenState by viewModel.currentScreenState.collectAsState()
        val hasRequiredFields by viewModel.requiredFields.collectAsState()

        LaunchedEffect(screenState, hasRequiredFields) {
            usBankAccountFormArgs.onError(screenState.error)
            // TODO Get rid of this
//            usBankAccountFormArgs.onMandateTextChanged(screenState.mandateText, false)
        }

        DisposableEffect(Unit) {
            viewModel.register(activityResultRegistryOwner!!)

            onDispose {
                usBankAccountFormArgs.onUpdatePrimaryButtonUIState { null }
            }
        }

        USBankAccountForm(
            viewModel = viewModel,
            formArgs = formArguments,
            usBankAccountFormArgs = usBankAccountFormArgs,
        )
    }

    private fun makeViewModelArgs(): USBankAccountFormViewModel.Args {
        return USBankAccountFormViewModel.Args(
            instantDebits = usBankAccountFormArgs.instantDebits,
            linkMode = usBankAccountFormArgs.linkMode,
            formArgs = formArguments,
            hostedSurface = usBankAccountFormArgs.hostedSurface,
            showCheckbox = usBankAccountFormArgs.showCheckbox,
            isCompleteFlow = usBankAccountFormArgs.isCompleteFlow,
            isPaymentFlow = usBankAccountFormArgs.isPaymentFlow,
            stripeIntentId = usBankAccountFormArgs.stripeIntentId,
            clientSecret = usBankAccountFormArgs.clientSecret,
            onBehalfOf = usBankAccountFormArgs.onBehalfOf,
            savedPaymentMethod = usBankAccountFormArgs.draftPaymentSelection as? New.USBankAccount,
            shippingDetails = usBankAccountFormArgs.shippingDetails,
        )
    }

    private fun handleFormFieldValuesChanged(
        formFieldValues: List<Pair<IdentifierSpec, FormFieldEntry>>,
        mandateText: ResolvableString?,
        label: ResolvableString,
        collectBankAccount: () -> Unit,
    ) {
        val isComplete = formFieldValues.all { it.second.isComplete }

        val identifiers = formFieldValues.toMap().keys

        val hasResult = identifiers.contains(IdentifierSpec.LinkAccountSessionId) ||
            identifiers.contains(IdentifierSpec.LinkPaymentMethodId)

        if (hasResult) {
            // PaymentSheet will take over
            usBankAccountFormArgs.onUpdatePrimaryButtonUIState { null }
        } else {
            usBankAccountFormArgs.onUpdatePrimaryButtonUIState {
                PrimaryButton.UIState(
                    label = label,
                    onClick = collectBankAccount,
                    enabled = isComplete,
                    lockVisible = usBankAccountFormArgs.isCompleteFlow,
                )
            }
        }

        usBankAccountFormArgs.onMandateTextChanged(mandateText, false)

        this.formFieldValues.value = formFieldValues
    }
}
