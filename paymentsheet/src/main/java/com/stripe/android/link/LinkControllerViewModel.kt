package com.stripe.android.link

import android.app.Application
import android.content.Context
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
import com.stripe.android.link.injection.DaggerLinkControllerViewModelComponent
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

internal class LinkControllerViewModel @Inject constructor(
    application: Application,
    private val paymentElementLoader: PaymentElementLoader,
    private val linkGateFactory: LinkGate.Factory,
    val linkActivityContract: NativeLinkActivityContract,
) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(LinkControllerState())
    val state: StateFlow<LinkControllerState> = _state.asStateFlow()

    private var presentJob: Job? = null

    init {
        viewModelScope.launch {
            updateLinkConfiguration()
        }
    }

    fun onPresent(
        launcher: ActivityResultLauncher<LinkActivityContract.Args>,
        email: String?
    ) {
        if (presentJob?.isActive == true) {
            // TODO: Log.
            return
        }
        presentJob = viewModelScope.launch {
            // Try to obtain a Link configuration before we present.
            if (awaitLinkConfigurationResult().isFailure) {
                if (updateLinkConfiguration().isFailure) {
                    val result = LinkController.PresentPaymentMethodsResult.Failed(
                        RuntimeException("Failed to configure Link.") // TODO: Better error.
                    )
                    _state.update { it.copy(presentPaymentMethodsResult = result) }
                    return@launch
                }
            }
            val state = _state.value
            if (state.linkGate?.useNativeLink != true) {
                val result = LinkController.PresentPaymentMethodsResult.Failed(
                    RuntimeException("Attestation error.") // TODO: Better error.
                )
                _state.update { it.copy(presentPaymentMethodsResult = result) }
                return@launch
            }

            val configuration = state.linkConfiguration
                ?.copy(
                    customerInfo = LinkConfiguration.CustomerInfo(
                        name = null,
                        email = email,
                        phone = null,
                        billingCountryCode = null,
                    )
                )
                ?: return@launch

            val linkAccountInfo = (state.linkAccountUpdate as? LinkAccountUpdate.Value)
                ?.takeIf { email == state.presentedForEmail }
                ?: LinkAccountUpdate.Value(null)
            val selectedPaymentMethod = state.selectedPaymentMethod.takeIf { linkAccountInfo.account != null }

            _state.update {
                it.copy(
                    presentedForEmail = email,
                    linkAccountUpdate = linkAccountInfo,
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
    }

    fun onResult(context: Context, result: LinkActivityResult) {
        when (result) {
            is LinkActivityResult.Canceled -> {
                _state.update {
                    it.copy(
                        linkAccountUpdate = result.linkAccountUpdate,
                        presentPaymentMethodsResult = LinkController.PresentPaymentMethodsResult.Canceled,
                    )
                }
            }
            is LinkActivityResult.Completed -> {
                _state.update {
                    it.copy(
                        selectedPaymentMethod = result.selectedPayment,
                        presentPaymentMethodsResult = LinkController.PresentPaymentMethodsResult.Selected(
                            preview = result.selectedPayment!!.toPreview(context)
                        ),
                        linkAccountUpdate = result.linkAccountUpdate,
                    )
                }
            }
            is LinkActivityResult.Failed -> {
                _state.update {
                    it.copy(
                        linkAccountUpdate = result.linkAccountUpdate,
                        presentPaymentMethodsResult = LinkController.PresentPaymentMethodsResult.Failed(
                            error = result.error,
                        ),
                    )
                }
            }
            is LinkActivityResult.PaymentMethodObtained -> {
                // TODO: Unexpected.
            }
        }
    }

    private suspend fun updateLinkConfiguration(): Result<LinkConfiguration?> {
        _state.update { it.copy(linkConfigurationResult = null) }
        val result = loadLinkConfiguration()
        _state.update { state ->
            state.copy(
                linkConfigurationResult = result,
                linkGate = result.getOrNull()?.let { linkGateFactory.create(it) }
            )
        }
        return result
    }

    private suspend fun loadLinkConfiguration(): Result<LinkConfiguration?> {
        return paymentElementLoader.load(
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
        ).map { state ->
            state.paymentMethodMetadata.linkState?.configuration
        }
    }

    private suspend fun awaitLinkConfigurationResult(): Result<LinkConfiguration?> {
        return _state
            .map { it.linkConfigurationResult }
            .filterNotNull()
            .first()
    }

    fun onLookupConsumer(email: String) {
        _state.update {
            it.copy(lookupConsumerResult = LinkController.LookupConsumerResult.Success(email, true))
        }
    }

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            return DaggerLinkControllerViewModelComponent.factory()
                .build(
                    application = extras.requireApplication(),
                    savedStateHandle = extras.createSavedStateHandle(),
                )
                .viewModel as T
        }
    }
}
