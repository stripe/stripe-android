package com.stripe.android.paymentsheet.paymentdatacollection.bacs

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.stripe.android.paymentsheet.R
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

internal class BacsMandateConfirmationViewModel constructor(
    private val args: Args
) : ViewModel() {
    private val _effect = MutableSharedFlow<BacsMandateConfirmationEffect>()
    val effect: SharedFlow<BacsMandateConfirmationEffect>
        get() = _effect

    private val _viewState = MutableStateFlow(
        BacsMandateConfirmationViewState(
            email = args.email,
            nameOnAccount = args.nameOnAccount,
            sortCode = args.sortCode.chunked(size = 2).joinToString(separator = "-"),
            accountNumber = args.accountNumber,
            supportAddressAsHtml = buildAddressAsHtml(),
            debitGuaranteeAsHtml = buildGuarantee()
        )
    )
    val viewState: StateFlow<BacsMandateConfirmationViewState>
        get() = _viewState

    fun handleViewAction(action: BacsMandateConfirmationViewAction) {
        when (action) {
            is BacsMandateConfirmationViewAction.OnConfirmPressed -> onConfirmPress()
            is BacsMandateConfirmationViewAction.OnCancelPressed -> onCancelPress()
        }
    }

    private fun onConfirmPress() {
        viewModelScope.launch {
            _effect.emit(BacsMandateConfirmationEffect.CloseWithResult(BacsMandateConfirmationResult.Confirmed))
        }
    }

    private fun onCancelPress() {
        viewModelScope.launch {
            _effect.emit(BacsMandateConfirmationEffect.CloseWithResult(BacsMandateConfirmationResult.Cancelled))
        }
    }

    private fun buildAddressAsHtml(): String {
        val defaultAddress = args.defaultAddress

        return buildString {
            append(defaultAddress.lineOne)
            append("<br>")
            append(defaultAddress.lineTwo)
            append("<br>")
            append("<a href=\"mailto:${defaultAddress.supportEmail}\">")
            append(defaultAddress.supportEmail)
            append("</a>")
        }
    }

    private fun buildGuarantee(): String {
        return buildString {
            append("<a href=\"${args.guarantee.url}\">")
            append(args.guarantee.name)
            append("</a>")
        }
    }

    data class Args(
        val email: String,
        val nameOnAccount: String,
        val sortCode: String,
        val accountNumber: String,
        val guarantee: Guarantee,
        val defaultAddress: DefaultAddress,
        val defaultPayer: String
    ) {
        data class Guarantee(
            val name: String,
            val url: String
        )

        data class DefaultAddress(
            val lineOne: String,
            val lineTwo: String,
            val supportEmail: String,
        )
    }

    class Factory(
        private val application: Application,
        private val args: BacsMandateConfirmationContract.Args
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val context = application.applicationContext

            return BacsMandateConfirmationViewModel(
                Args(
                    email = args.email,
                    nameOnAccount = args.nameOnAccount,
                    sortCode = args.sortCode,
                    accountNumber = args.accountNumber,
                    guarantee = Args.Guarantee(
                        name = context.getString(R.string.stripe_paymentsheet_bacs_guarantee),
                        url = context.getString(R.string.stripe_paymentsheet_bacs_guarantee_url)
                    ),
                    defaultAddress = Args.DefaultAddress(
                        lineOne = context.getString(R.string.stripe_paymentsheet_bacs_support_default_address_line_one),
                        lineTwo = context.getString(R.string.stripe_paymentsheet_bacs_support_default_address_line_two),
                        supportEmail = context.getString(R.string.stripe_paymentsheet_bacs_support_default_email)
                    ),
                    defaultPayer = context.getString(R.string.stripe_paymentsheet_bacs_notice_default_payer)
                )
            ) as T
        }
    }
}
