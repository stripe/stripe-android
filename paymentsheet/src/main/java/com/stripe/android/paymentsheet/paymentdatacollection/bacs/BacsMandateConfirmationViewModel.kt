package com.stripe.android.paymentsheet.paymentdatacollection.bacs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.paymentsheet.R
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal class BacsMandateConfirmationViewModel constructor(
    args: Args
) : ViewModel() {
    private val _result = MutableSharedFlow<BacsMandateConfirmationResult>()
    val result: SharedFlow<BacsMandateConfirmationResult> = _result.asSharedFlow()

    private val _viewState = MutableStateFlow(
        BacsMandateConfirmationViewState(
            email = args.email,
            nameOnAccount = args.nameOnAccount,
            sortCode = args.sortCode.chunked(size = 2).joinToString(separator = "-"),
            accountNumber = args.accountNumber,
            payer = buildPayer(),
            supportAddressAsHtml = buildAddressAsHtml(),
            debitGuaranteeAsHtml = buildGuarantee()
        )
    )
    val viewState: StateFlow<BacsMandateConfirmationViewState> = _viewState.asStateFlow()

    fun handleViewAction(action: BacsMandateConfirmationViewAction) {
        when (action) {
            is BacsMandateConfirmationViewAction.OnConfirmPressed -> onConfirmPress()
            is BacsMandateConfirmationViewAction.OnModifyDetailsPressed -> onModifyDetailsPressed()
            is BacsMandateConfirmationViewAction.OnBackPressed -> onBackPress()
        }
    }

    private fun onConfirmPress() {
        viewModelScope.launch {
            _result.emit(BacsMandateConfirmationResult.Confirmed)
        }
    }

    private fun onModifyDetailsPressed() {
        viewModelScope.launch {
            _result.emit(BacsMandateConfirmationResult.ModifyDetails)
        }
    }

    private fun onBackPress() {
        viewModelScope.launch {
            _result.emit(BacsMandateConfirmationResult.Cancelled)
        }
    }

    private fun buildPayer(): ResolvableString {
        return resolvableString(R.string.stripe_paymentsheet_bacs_notice_default_payer)
    }

    private fun buildAddressAsHtml(): ResolvableString {
        return resolvableString(
            R.string.stripe_paymentsheet_bacs_support_address_format,
            resolvableString(R.string.stripe_paymentsheet_bacs_support_default_address_line_one),
            resolvableString(R.string.stripe_paymentsheet_bacs_support_default_address_line_two),
            resolvableString(R.string.stripe_paymentsheet_bacs_support_default_email),
            resolvableString(R.string.stripe_paymentsheet_bacs_support_default_email)
        )
    }

    private fun buildGuarantee(): ResolvableString {
        return resolvableString(
            R.string.stripe_paymentsheet_bacs_guarantee_format,
            resolvableString(R.string.stripe_paymentsheet_bacs_guarantee_url),
            resolvableString(R.string.stripe_paymentsheet_bacs_guarantee)
        )
    }

    data class Args(
        val email: String,
        val nameOnAccount: String,
        val sortCode: String,
        val accountNumber: String
    )

    class Factory(private val args: BacsMandateConfirmationContract.Args) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            return BacsMandateConfirmationViewModel(
                Args(
                    email = args.email,
                    nameOnAccount = args.nameOnAccount,
                    sortCode = args.sortCode,
                    accountNumber = args.accountNumber
                )
            ) as T
        }
    }
}
