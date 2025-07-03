package com.stripe.android.link

import android.app.Application
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.stripe.android.common.model.asCommonConfiguration
import com.stripe.android.core.utils.requireApplication
import com.stripe.android.link.gate.LinkGate
import com.stripe.android.link.injection.DaggerLinkPaymentMethodLauncherViewModelComponent
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

internal data class LinkPaymentMethodLauncherState(
    val paymentElementLoaderStateResult: Result<PaymentElementLoader.State>? = null,
    val linkGate: LinkGate? = null,
    val presentedForEmail: String? = null,
    val linkAccountUpdate: LinkAccountUpdate = LinkAccountUpdate.None,
    val selectedPaymentMethod: LinkPaymentMethod? = null,
) {
    private val paymentElementLoaderState get() = paymentElementLoaderStateResult?.getOrNull()

    val linkConfiguration: LinkConfiguration? =
        paymentElementLoaderState?.paymentMethodMetadata?.linkState?.configuration

    val canPresent: Boolean
        get() = linkConfiguration != null && linkGate?.useNativeLink == true
}

internal class LinkPaymentMethodLauncherViewModel @Inject constructor(
    application: Application,
    private val paymentElementLoader: PaymentElementLoader,
    private val linkGateFactory: LinkGate.Factory,
    val linkActivityContract: NativeLinkActivityContract,
) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(LinkPaymentMethodLauncherState())
    val state: StateFlow<LinkPaymentMethodLauncherState> = _state.asStateFlow()

    init {
        loadElementsSession()
    }

    fun onLoadSession() {
        loadElementsSession()
    }

    fun onPresent(
        launcher: ActivityResultLauncher<LinkActivityContract.Args>,
        email: String?
    ) {
        val state = _state.value.takeIf { it.canPresent }
            ?: return

        val configuration = state.linkConfiguration
            ?.copy(
                customerInfo = LinkConfiguration.CustomerInfo(
                    name = null,
                    email = email,
                    phone = null,
                    billingCountryCode = null
                )
            )
            ?: return

        val linkAccountInfo = (state.linkAccountUpdate as? LinkAccountUpdate.Value)
            ?.takeIf { email == state.presentedForEmail }
            ?: LinkAccountUpdate.Value(null)
        val selectedPaymentMethod = state.selectedPaymentMethod.takeIf { linkAccountInfo.account != null }

        _state.update {
            it.copy(
                presentedForEmail = email,
                selectedPaymentMethod = selectedPaymentMethod,
            )
        }

        launcher.launch(
            LinkActivityContract.Args(
                configuration = configuration,
                startWithVerificationDialog = true,
                linkAccountInfo = linkAccountInfo,
                launchMode = LinkLaunchMode.PaymentMethodSelection(selectedPaymentMethod?.details),
            )
        )
    }

    fun onResult(result: LinkActivityResult) {
        when (result) {
            is LinkActivityResult.Canceled -> {
                // TODO
            }
            is LinkActivityResult.Completed -> {
                _state.update {
                    it.copy(
                        selectedPaymentMethod = result.selectedPayment,
                        linkAccountUpdate = result.linkAccountUpdate,
                    )
                }
            }
            is LinkActivityResult.Failed -> {
                // TODO
            }
            is LinkActivityResult.PaymentMethodObtained -> {
                // TODO: Unexpected.
            }
        }
    }

    private fun loadElementsSession() {
        viewModelScope.launch {
            _state.update { it.copy(paymentElementLoaderStateResult = null) }
            val result = paymentElementLoader.load(
                initializationMode = PaymentElementLoader.InitializationMode.DeferredIntent(
                    PaymentSheet.IntentConfiguration(
                        mode = PaymentSheet.IntentConfiguration.Mode.Setup(),
                    ),
                ),
                configuration = PaymentSheet.Configuration.default(getApplication()).asCommonConfiguration(),
                metadata = PaymentElementLoader.Metadata(
                    isReloadingAfterProcessDeath = false, // TODO.
                    initializedViaCompose = false, // TODO.
                )
            )
            val linkConfiguration = result.getOrNull()?.paymentMethodMetadata?.linkState?.configuration
            _state.update { state ->
                state.copy(
                    paymentElementLoaderStateResult = result,
                    linkGate = linkConfiguration?.let { linkGateFactory.create(it) }
                )
            }
        }
    }

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            return DaggerLinkPaymentMethodLauncherViewModelComponent.factory()
                .build(
                    application = extras.requireApplication(),
                    savedStateHandle = extras.createSavedStateHandle(),
                )
                .viewModel as T
        }
    }
}
