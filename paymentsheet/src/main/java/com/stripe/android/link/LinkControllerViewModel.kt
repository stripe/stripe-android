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
                    presentPaymentMethodsResult = LinkController.PresentPaymentMethodsResult.SelectionChanged(null),
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
    private var lastLaunchMode: LinkLaunchMode? = null

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
        presentJob = launchLink(
            launcher = launcher,
            email = email,
            launchMode = { selectedPaymentMethod -> LinkLaunchMode.PaymentMethodSelection(selectedPaymentMethod?.details) },
            onFailure = { error ->
                val result = LinkController.PresentPaymentMethodsResult.Failed(error)
                _state.update { it.copy(presentPaymentMethodsResult = result) }
            },
            onSetupState = { email, selectedPaymentMethod ->
                _state.update {
                    it.copy(
                        presentedForEmail = email,
                        selectedPaymentMethod = selectedPaymentMethod,
                    )
                }
            }
        )
    }

    fun onLinkActivityResult(context: Context, result: LinkActivityResult) {
        // Route to authentication result handler if this was an authentication launch
        if (lastLaunchMode == LinkLaunchMode.Authentication) {
            onAuthenticationResult(context, result)
            return
        }
        
        // Otherwise handle as payment method result
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
                        presentPaymentMethodsResult = LinkController.PresentPaymentMethodsResult.SelectionChanged(
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

    fun onAuthenticationResult(context: Context, result: LinkActivityResult) {
        when (result) {
            is LinkActivityResult.Canceled -> {
                linkAccountHolder.set(result.linkAccountUpdate.asValue())
                _state.update {
                    it.copy(presentForAuthenticationResult = LinkController.PresentForAuthenticationResult.Canceled)
                }
            }
            is LinkActivityResult.Completed -> {
                linkAccountHolder.set(result.linkAccountUpdate.asValue())
                val linkAccount = result.linkAccountUpdate.asValue()?.account
                if (linkAccount != null) {
                    val linkUser = LinkController.LinkUser(
                        email = linkAccount.email,
                        phone = linkAccount.unredactedPhoneNumber,
                        isVerified = linkAccount.isVerified,
                        completedSignup = linkAccount.completedSignup
                    )
                    _state.update {
                        it.copy(
                            presentForAuthenticationResult = LinkController.PresentForAuthenticationResult.Authenticated(linkUser)
                        )
                    }
                } else {
                    _state.update {
                        it.copy(
                            presentForAuthenticationResult = LinkController.PresentForAuthenticationResult.Failed(
                                RuntimeException("Authentication completed but no user account found")
                            )
                        )
                    }
                }
            }
            is LinkActivityResult.Failed -> {
                linkAccountHolder.set(result.linkAccountUpdate.asValue())
                _state.update {
                    it.copy(
                        presentForAuthenticationResult = LinkController.PresentForAuthenticationResult.Failed(
                            error = result.error,
                        ),
                    )
                }
            }
            is LinkActivityResult.PaymentMethodObtained -> {
                // TODO: Unexpected for authentication flow.
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

    fun onPresentForAuthentication(
        launcher: ActivityResultLauncher<LinkActivityContract.Args>,
        email: String
    ) {
        // Authentication doesn't need to track job like present method does
        launchLink(
            launcher = launcher,
            email = email,
            launchMode = { _ -> LinkLaunchMode.Authentication },
            onFailure = { error ->
                val result = LinkController.PresentForAuthenticationResult.Failed(error)
                _state.update { it.copy(presentForAuthenticationResult = result) }
            },
            onSetupState = { _, _ -> 
                // No additional state setup needed for authentication
            }
        )
    }

    fun onRegisterNewLinkUser(
        email: String,
        name: String?,
        phone: String,
        country: String
    ) {
        // TODO: Implementation - empty for now as requested
        val result = LinkController.RegisterNewLinkUserResult.Failed(
            error = RuntimeException("Registration not yet implemented")
        )
        _state.update {
            it.copy(registerNewLinkUserResult = result)
        }
    }

    private fun launchLink(
        launcher: ActivityResultLauncher<LinkActivityContract.Args>,
        email: String?,
        launchMode: (LinkPaymentMethod?) -> LinkLaunchMode,
        onFailure: (Throwable) -> Unit,
        onSetupState: (String?, LinkPaymentMethod?) -> Unit
    ): Job {
        return viewModelScope.launch {
            // Try to obtain a Link configuration before we present.
            if (awaitLinkConfigurationResult().isFailure) {
                if (updateLinkConfiguration().isFailure) {
                    onFailure(RuntimeException("Failed to configure Link.")) // TODO: Better error.
                    return@launch
                }
            }
            val state = _state.value
            if (state.linkGate?.useNativeLink != true) {
                onFailure(RuntimeException("Attestation error.")) // TODO: Better error.
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
                ?: run {
                    onFailure(RuntimeException("No Link configuration available."))
                    return@launch
                }

            logger.debug("$tag: Launching Link for email=$email")

            // Set up linkAccountInfo and selectedPaymentMethod based on mode
            val linkAccountInfo = when (launchMode(null)) {
                is LinkLaunchMode.Authentication -> {
                    // For authentication, use current linkAccountInfo as-is
                    linkAccountHolder.linkAccountInfo.value
                }
                is LinkLaunchMode.PaymentMethodSelection -> {
                    // For payment method selection, check if email matches
                    linkAccountHolder.linkAccountInfo.value
                        .takeIf { email == state.presentedForEmail && email == it.account?.email }
                        ?: LinkAccountUpdate.Value(null)
                }
                is LinkLaunchMode.Full -> {
                    // For full mode, similar to payment method selection
                    linkAccountHolder.linkAccountInfo.value
                        .takeIf { email == state.presentedForEmail && email == it.account?.email }
                        ?: LinkAccountUpdate.Value(null)
                }
                is LinkLaunchMode.Confirmation -> {
                    // For confirmation, use current linkAccountInfo
                    linkAccountHolder.linkAccountInfo.value
                }
            }
            
            val selectedPaymentMethod = when (launchMode(null)) {
                is LinkLaunchMode.Authentication -> null // No payment method needed for auth
                is LinkLaunchMode.PaymentMethodSelection -> {
                    state.selectedPaymentMethod.takeIf { linkAccountInfo.account != null }
                }
                is LinkLaunchMode.Full -> {
                    state.selectedPaymentMethod.takeIf { linkAccountInfo.account != null }
                }
                is LinkLaunchMode.Confirmation -> {
                    // Confirmation mode already has a selected payment method
                    state.selectedPaymentMethod
                }
            }

            // Allow caller to set up additional state
            onSetupState(email, selectedPaymentMethod)

            val finalLaunchMode = launchMode(selectedPaymentMethod)
            lastLaunchMode = finalLaunchMode
            launcher.launch(
                LinkActivityContract.Args(
                    configuration = configuration,
                    startWithVerificationDialog = true,
                    linkAccountInfo = linkAccountInfo,
                    launchMode = finalLaunchMode,
                )
            )
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
