package com.stripe.android.link.ui.wallet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.stripe.android.core.Logger
import com.stripe.android.link.LinkScreen
import com.stripe.android.link.injection.LinkInjectable
import com.stripe.android.link.injection.LinkInjector
import com.stripe.android.link.injection.SignedInViewModelSubcomponent
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.link.model.Navigator
import com.stripe.android.link.repositories.LinkRepository
import com.stripe.android.model.ConsumerPaymentDetails
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Provider

internal class WalletViewModel @Inject constructor(
    private val linkRepository: LinkRepository,
    private val navigator: Navigator,
    private val logger: Logger,
    val linkAccount: LinkAccount
) : ViewModel() {

    private val _paymentDetails =
        MutableStateFlow<List<ConsumerPaymentDetails.PaymentDetails>>(emptyList())
    val paymentDetails: StateFlow<List<ConsumerPaymentDetails.PaymentDetails>> = _paymentDetails

    // TODO(brnunes-stripe): Use real value or "Set up" for Setup Intents.
    val payButtonLabel = "Pay $10.99"

    init {
        viewModelScope.launch {
            linkRepository.listPaymentDetails(linkAccount.clientSecret).fold(
                onSuccess = { response ->
                    response.paymentDetails.filterIsInstance<ConsumerPaymentDetails.Card>()
                        .takeIf { it.isNotEmpty() }?.let {
                            _paymentDetails.value = it
                        } ?: addNewPaymentMethod()
                },
                onFailure = ::onError
            )
        }
    }

    fun completePayment(selectedPaymentDetails: ConsumerPaymentDetails.PaymentDetails) {
        logger.debug("onPayButtonClick: $selectedPaymentDetails")
    }

    fun payAnotherWay() {
        navigator.dismiss()
    }

    fun addNewPaymentMethod() {
        navigator.navigateTo(LinkScreen.AddPaymentMethod)
    }

    private fun onError(error: Throwable) {
        logger.error(error.localizedMessage ?: "Internal error.")
        // TODO(brnunes-stripe): Add localized error messages, show them in UI.
    }

    internal class Factory(
        private val linkAccount: LinkAccount,
        private val injector: LinkInjector
    ) : ViewModelProvider.Factory, LinkInjectable {

        @Inject
        lateinit var subComponentBuilderProvider:
            Provider<SignedInViewModelSubcomponent.Builder>

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            injector.inject(this)
            return subComponentBuilderProvider.get()
                .linkAccount(linkAccount)
                .build().walletViewModel as T
        }
    }
}
