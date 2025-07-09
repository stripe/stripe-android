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
import com.stripe.android.core.Logger
import com.stripe.android.core.utils.requireApplication
import com.stripe.android.link.account.LinkAccountHolder
import com.stripe.android.link.confirmation.computeExpectedPaymentMethodType
import com.stripe.android.link.gate.LinkGate
import com.stripe.android.link.injection.DaggerLinkControllerViewModelComponent
import com.stripe.android.link.repositories.LinkApiRepository
import com.stripe.android.model.parsers.PaymentMethodJsonParser
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
import org.json.JSONObject
import javax.inject.Inject

internal class LinkControllerViewModel @Inject constructor(
    application: Application,
    private val logger: Logger,
    private val paymentElementLoader: PaymentElementLoader,
    private val linkGateFactory: LinkGate.Factory,
    private val linkAccountHolder: LinkAccountHolder,
    private val linkApiRepository: LinkApiRepository,
    val linkActivityContract: NativeLinkActivityContract,
) : AndroidViewModel(application) {

    var configuration: PaymentSheet.Configuration = PaymentSheet.Configuration.default(application)
        set(value) {
            field = value
            _state.update {
                LinkControllerState(
                    presentPaymentMethodsResult = LinkController.PresentPaymentMethodsResult.Updated(null),
                )
            }
            viewModelScope.launch {
                updateLinkConfiguration()
            }
        }

    private val tag = "LinkControllerViewModel"

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

            logger.debug("$tag: configuration=$configuration")

            val linkAccountInfo = linkAccountHolder.linkAccountInfo.value
                .takeIf { email == state.presentedForEmail && email == it.account?.email }
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
    }

    fun onLinkActivityResult(context: Context, result: LinkActivityResult) {
        when (result) {
            is LinkActivityResult.Canceled -> {
                linkAccountHolder.set(result.linkAccountUpdate.asValue())
            }
            is LinkActivityResult.Completed -> {
                logger.debug("$tag: details=${result.selectedPayment?.details}")
                linkAccountHolder.set(result.linkAccountUpdate.asValue())
                _state.update {
                    it.copy(
                        selectedPaymentMethod = result.selectedPayment,
                        presentPaymentMethodsResult = LinkController.PresentPaymentMethodsResult.Updated(
                            preview = result.selectedPayment!!.toPreview(context)
                        ),
                    )
                }
            }
            is LinkActivityResult.Failed -> {
                linkAccountHolder.set(result.linkAccountUpdate.asValue())
                _state.update {
                    it.copy(
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

    fun onLookupConsumer(email: String) {
        viewModelScope.launch {
            val result =
                linkApiRepository.lookupConsumer(email).map { it.exists }
                    .fold(
                        onSuccess = { LinkController.LookupConsumerResult.Success(email, it) },
                        onFailure = { LinkController.LookupConsumerResult.Failed(email, it) },
                    )
            _state.update {
                it.copy(lookupConsumerResult = result)
            }
        }
    }

    fun onCreatePaymentMethod() {
        val state = _state.value
        // TODO: Error handling.
        val paymentMethod = state.selectedPaymentMethod
            ?: return
        val configuration = state.linkConfiguration
            ?: return
        val account = linkAccountHolder.linkAccountInfo.value.account
            ?: return
        viewModelScope.launch {
            val apiResult = if (configuration.passthroughModeEnabled) {
                linkApiRepository.sharePaymentDetails(
                    consumerSessionClientSecret = account.clientSecret,
                    paymentDetailsId = paymentMethod.details.id,
                    expectedPaymentMethodType = computeExpectedPaymentMethodType(configuration, paymentMethod.details),
                    cvc = paymentMethod.collectedCvc,
                    billingPhone = null,
                ).map { shareDetails ->
                    val json = JSONObject(shareDetails.encodedPaymentMethod)
                    PaymentMethodJsonParser().parse(json)
                }
            } else {
                linkApiRepository.createPaymentMethod(
                    consumerSessionClientSecret = account.clientSecret,
                    paymentMethod = paymentMethod,
                )
            }
            val result = apiResult.fold(
                onSuccess = { LinkController.CreatePaymentMethodResult.Success(it) },
                onFailure = { LinkController.CreatePaymentMethodResult.Failed(it) },
            )
            _state.update {
                it.copy(createPaymentMethodResult = result)
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
            configuration = configuration.asCommonConfiguration(),
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
