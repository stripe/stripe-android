package com.stripe.android.link.ui.wallet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.stripe.android.core.Logger
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.link.LinkActivityResult
import com.stripe.android.link.LinkConfiguration
import com.stripe.android.link.LinkScreen
import com.stripe.android.link.account.LinkAccountManager
import com.stripe.android.link.injection.NativeLinkComponent
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.link.model.supportedPaymentMethodTypes
import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.SetupIntent
import com.stripe.android.model.StripeIntent
import com.stripe.android.ui.core.Amount
import com.stripe.android.ui.core.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

internal class WalletViewModel @Inject constructor(
    private val configuration: LinkConfiguration,
    private val linkAccount: LinkAccount,
    private val linkAccountManager: LinkAccountManager,
    private val logger: Logger,
    private val navigateAndClearStack: (route: LinkScreen) -> Unit,
    private val dismissWithResult: (LinkActivityResult) -> Unit
) : ViewModel() {
    private val stripeIntent = configuration.stripeIntent

    private val _uiState = MutableStateFlow(
        value = WalletUiState(
            paymentDetailsList = emptyList(),
            selectedItem = null,
            isProcessing = false,
            hasCompleted = false,
            primaryButtonLabel = completePaymentButtonLabel(configuration.stripeIntent)
        )
    )

    val uiState: StateFlow<WalletUiState> = _uiState

    init {
        loadPaymentDetails()
    }

    private fun loadPaymentDetails() {
        _uiState.update {
            it.setProcessing()
        }

        viewModelScope.launch {
            linkAccountManager.listPaymentDetails(
                paymentMethodTypes = stripeIntent.supportedPaymentMethodTypes(linkAccount)
            ).fold(
                onSuccess = { response ->
                    _uiState.update {
                        it.updateWithResponse(response)
                    }

                    if (response.paymentDetails.isEmpty()) {
                        navigateAndClearStack(LinkScreen.PaymentMethod)
                    }
                },
                // If we can't load the payment details there's nothing to see here
                onFailure = ::onFatal
            )
        }
    }

    private fun onFatal(fatalError: Throwable) {
        logger.error("WalletViewModel Fatal error: ", fatalError)
        dismissWithResult(LinkActivityResult.Failed(fatalError))
    }

    fun onItemSelected(item: ConsumerPaymentDetails.PaymentDetails) {
        if (item == uiState.value.selectedItem) return

        _uiState.update {
            it.copy(selectedItem = item)
        }
    }

    fun onPrimaryButtonClicked() = Unit

    fun onPayAnotherWayClicked() {
        dismissWithResult(LinkActivityResult.Canceled(LinkActivityResult.Canceled.Reason.PayAnotherWay))
    }

    private fun completePaymentButtonLabel(
        stripeIntent: StripeIntent,
    ) = when (stripeIntent) {
        is PaymentIntent -> {
            Amount(
                requireNotNull(stripeIntent.amount),
                requireNotNull(stripeIntent.currency)
            ).buildPayButtonLabel()
        }
        is SetupIntent -> R.string.stripe_setup_button_label.resolvableString
    }

    companion object {
        fun factory(
            parentComponent: NativeLinkComponent,
            linkAccount: LinkAccount,
            navigateAndClearStack: (route: LinkScreen) -> Unit,
            dismissWithResult: (LinkActivityResult) -> Unit
        ): ViewModelProvider.Factory {
            return viewModelFactory {
                initializer {
                    WalletViewModel(
                        configuration = parentComponent.configuration,
                        linkAccountManager = parentComponent.linkAccountManager,
                        logger = parentComponent.logger,
                        linkAccount = linkAccount,
                        navigateAndClearStack = navigateAndClearStack,
                        dismissWithResult = dismissWithResult
                    )
                }
            }
        }
    }
}
