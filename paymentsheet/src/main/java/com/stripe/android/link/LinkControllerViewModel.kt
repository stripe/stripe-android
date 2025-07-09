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
import com.stripe.android.link.ui.wallet.displayName
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.parsers.PaymentMethodJsonParser
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import com.stripe.android.uicore.utils.mapAsStateFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
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
            _state.update { State() }
            viewModelScope.launch {
                updateLinkConfiguration()
            }
        }

    private val tag = "LinkControllerViewModel"

    private val _state = MutableStateFlow(State())

    private val _presentPaymentMethodsResultFlow =
        MutableSharedFlow<LinkController.PresentPaymentMethodsResult>(extraBufferCapacity = 1)
    val presentPaymentMethodsResultFlow = _presentPaymentMethodsResultFlow.asSharedFlow()

    private val _lookupConsumerResultFlow =
        MutableSharedFlow<LinkController.LookupConsumerResult>(extraBufferCapacity = 1)
    val lookupConsumerResultFlow = _lookupConsumerResultFlow.asSharedFlow()

    private val _createPaymentMethodResultFlow =
        MutableSharedFlow<LinkController.CreatePaymentMethodResult>(extraBufferCapacity = 1)
    val createPaymentMethodResultFlow = _createPaymentMethodResultFlow.asSharedFlow()

    private var presentJob: Job? = null

    init {
        viewModelScope.launch {
            updateLinkConfiguration()
        }
    }

    fun state(context: Context): StateFlow<LinkController.State> {
        return _state.mapAsStateFlow { state ->
            LinkController.State(
                email = state.presentedForEmail,
                selectedPaymentMethodPreview = state.selectedPaymentMethod?.toPreview(context),
                createdPaymentMethod = state.createdPaymentMethod,
            )
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
                    _presentPaymentMethodsResultFlow.emit(
                        LinkController.PresentPaymentMethodsResult.Failed(
                            error = RuntimeException("Failed to configure Link.") // TODO: Better error.
                        )
                    )
                    return@launch
                }
            }
            val state = _state.value
            if (state.linkGate?.useNativeLink != true) {
                _presentPaymentMethodsResultFlow.emit(
                    LinkController.PresentPaymentMethodsResult.Failed(
                        error = RuntimeException("Attestation error.") // TODO: Better error.
                    )
                )
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
            val selectedPaymentMethod = state.selectedPaymentMethod
                .takeIf { linkAccountInfo.account != null }
            val createdPaymentMethod = state.createdPaymentMethod
                .takeIf { linkAccountInfo.account != null }

            _state.update {
                it.copy(
                    presentedForEmail = email,
                    selectedPaymentMethod = selectedPaymentMethod,
                    createdPaymentMethod = createdPaymentMethod,
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

    fun onPresentPaymentMethodsActivityResult(result: LinkActivityResult) {
        when (result) {
            is LinkActivityResult.Canceled -> {
                linkAccountHolder.set(result.linkAccountUpdate.asValue())
            }
            is LinkActivityResult.Completed -> {
                logger.debug("$tag: details=${result.selectedPayment?.details}")
                linkAccountHolder.set(result.linkAccountUpdate.asValue())
                _state.update {
                    it.copy(selectedPaymentMethod = result.selectedPayment)
                }
                viewModelScope.launch {
                    _presentPaymentMethodsResultFlow.emit(
                        LinkController.PresentPaymentMethodsResult.Success
                    )
                }
            }
            is LinkActivityResult.Failed -> {
                linkAccountHolder.set(result.linkAccountUpdate.asValue())
                viewModelScope.launch {
                    _presentPaymentMethodsResultFlow.emit(
                        LinkController.PresentPaymentMethodsResult.Failed(result.error)
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
            val result = linkApiRepository.lookupConsumer(email)
                .map { it.exists }
                .fold(
                    onSuccess = { LinkController.LookupConsumerResult.Success(email, it) },
                    onFailure = { LinkController.LookupConsumerResult.Failed(email, it) }
                )
            _lookupConsumerResultFlow.emit(result)
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
            _state.update { it.copy(createdPaymentMethod = apiResult.getOrNull()) }
            viewModelScope.launch {
                _createPaymentMethodResultFlow.emit(
                    apiResult.fold(
                        onSuccess = { LinkController.CreatePaymentMethodResult.Success },
                        onFailure = { LinkController.CreatePaymentMethodResult.Failed(it) },
                    )
                )
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

    private fun LinkPaymentMethod.toPreview(context: Context): LinkController.PaymentMethodPreview {
        val sublabel = buildString {
            append(details.displayName.resolve(context))
            append(" •••• ")
            append(details.last4)
        }
        return LinkController.PaymentMethodPreview(
            iconRes = R.drawable.stripe_ic_paymentsheet_link_arrow,
            label = context.getString(com.stripe.android.R.string.stripe_link),
            sublabel = sublabel
        )
    }

    internal data class State(
        val linkConfigurationResult: Result<LinkConfiguration?>? = null,
        val linkGate: LinkGate? = null,
        val presentedForEmail: String? = null,
        val selectedPaymentMethod: LinkPaymentMethod? = null,
        val createdPaymentMethod: PaymentMethod? = null,
    ) {
        val linkConfiguration: LinkConfiguration? = linkConfigurationResult?.getOrNull()
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
