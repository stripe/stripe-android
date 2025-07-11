package com.stripe.android.link

import android.app.Application
import android.content.Context
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.stripe.android.core.Logger
import com.stripe.android.core.utils.requireApplication
import com.stripe.android.link.LinkController.PresentForAuthenticationResult
import com.stripe.android.link.account.LinkAccountHolder
import com.stripe.android.link.confirmation.computeExpectedPaymentMethodType
import com.stripe.android.link.exceptions.LinkUnavailableException
import com.stripe.android.link.exceptions.MissingConfigurationException
import com.stripe.android.link.injection.DaggerLinkControllerViewModelComponent
import com.stripe.android.link.injection.LinkControllerComponent
import com.stripe.android.link.repositories.LinkRepository
import com.stripe.android.link.ui.wallet.displayName
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.parsers.PaymentMethodJsonParser
import com.stripe.android.paymentsheet.R
import com.stripe.android.uicore.utils.combineAsStateFlow
import com.stripe.android.uicore.utils.mapAsStateFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class LinkControllerViewModel @Inject constructor(
    application: Application,
    private val logger: Logger,
    private val linkConfigurationLoader: LinkConfigurationLoader,
    private val linkAccountHolder: LinkAccountHolder,
    private val linkRepository: LinkRepository,
    val controllerComponentFactory: LinkControllerComponent.Factory,
) : AndroidViewModel(application) {

    private val tag = "LinkControllerViewModel"

    private val _account = linkAccountHolder.linkAccountInfo.mapAsStateFlow { it.account }

    private val _state = MutableStateFlow(State())

    private val _presentPaymentMethodsResultFlow =
        MutableSharedFlow<LinkController.PresentPaymentMethodsResult>(replay = 1)
    val presentPaymentMethodsResultFlow = _presentPaymentMethodsResultFlow.asSharedFlow()

    private val _lookupConsumerResultFlow =
        MutableSharedFlow<LinkController.LookupConsumerResult>(replay = 1)
    val lookupConsumerResultFlow = _lookupConsumerResultFlow.asSharedFlow()

    private val _createPaymentMethodResultFlow =
        MutableSharedFlow<LinkController.CreatePaymentMethodResult>(replay = 1)
    val createPaymentMethodResultFlow = _createPaymentMethodResultFlow.asSharedFlow()

    private val _presentForAuthenticationResultFlow =
        MutableSharedFlow<PresentForAuthenticationResult>(extraBufferCapacity = 1)
    val presentForAuthenticationResultFlow = _presentForAuthenticationResultFlow.asSharedFlow()

    private var presentJob: Job? = null

    fun state(context: Context): StateFlow<LinkController.State> {
        return combineAsStateFlow(_account, _state) { account, state ->
            LinkController.State(
                isConsumerVerified = account?.isVerified,
                selectedPaymentMethodPreview = state.selectedPaymentMethod?.toPreview(context),
                createdPaymentMethod = state.createdPaymentMethod,
            )
        }
    }

    suspend fun configure(configuration: LinkController.Configuration): LinkController.ConfigureResult {
        logger.debug("$tag: updating configuration")
        updateState { State() }
        return linkConfigurationLoader.load(configuration)
            .fold(
                onSuccess = { config ->
                    updateState { it.copy(linkConfiguration = config) }
                    LinkController.ConfigureResult.Success
                },
                onFailure = { error ->
                    LinkController.ConfigureResult.Failed(error)
                }
            )
    }

    fun onPresentPaymentMethods(
        launcher: ActivityResultLauncher<LinkActivityContract.Args>,
        email: String?
    ) {
        if (presentJob?.isActive == true) {
            logger.debug("$tag: already presenting")
            return
        }
        presentJob = viewModelScope.launch {
            logger.debug("$tag: presenting payment methods")

            withConfiguration(
                email = email,
                onError = { error ->
                    _presentPaymentMethodsResultFlow.emit(
                        LinkController.PresentPaymentMethodsResult.Failed(
                            LinkUnavailableException(error)
                        )
                    )
                },
                onSuccess = { configuration ->
                    val state = _state.value
                    val linkAccountInfo = getLinkAccountInfo(email)

                    // If the account changed, clear account-related state.
                    val selectedPaymentMethod = state.selectedPaymentMethod
                        .takeIf { linkAccountInfo.account != null }
                    val createdPaymentMethod = state.createdPaymentMethod
                        .takeIf { linkAccountInfo.account != null }

                    val launchMode = LinkLaunchMode.PaymentMethodSelection(selectedPaymentMethod?.details)

                    _state.update {
                        it.copy(
                            presentedForEmail = email,
                            selectedPaymentMethod = selectedPaymentMethod,
                            createdPaymentMethod = createdPaymentMethod,
                            currentLaunchMode = launchMode,
                        )
                    }

                    launcher.launch(
                        LinkActivityContract.Args(
                            configuration = configuration,
                            startWithVerificationDialog = true,
                            linkAccountInfo = linkAccountInfo,
                            launchMode = launchMode,
                        )
                    )
                }
            )
        }
    }

    internal fun onPresentForAuthentication(
        launcher: ActivityResultLauncher<LinkActivityContract.Args>,
        email: String?
    ) {
        if (presentJob?.isActive == true) {
            logger.debug("$tag: already presenting")
            return
        }

        presentJob = viewModelScope.launch {
            logger.debug("$tag: presenting for authentication")

            withConfiguration(
                email = email,
                onError = { error ->
                    _presentForAuthenticationResultFlow.emit(
                        PresentForAuthenticationResult.Failed(
                            LinkUnavailableException(error)
                        )
                    )
                },
                onSuccess = { configuration ->
                    val linkAccountInfo = getLinkAccountInfo(email)
                    val launchMode = LinkLaunchMode.Authentication

                    _state.update {
                        it.copy(
                            presentedForEmail = email,
                            currentLaunchMode = launchMode,
                        )
                    }

                    launcher.launch(
                        LinkActivityContract.Args(
                            configuration = configuration,
                            startWithVerificationDialog = true,
                            linkAccountInfo = linkAccountInfo,
                            launchMode = launchMode,
                        )
                    )
                }
            )
        }
    }

    private fun getLinkAccountInfo(email: String?): LinkAccountUpdate.Value {
        val currentAccountInfo = linkAccountHolder.linkAccountInfo.value

        // If we already have an authenticated account, preserve it
        if (currentAccountInfo.account != null) {
            return currentAccountInfo
        }

        // Otherwise, check if the email matches the previously presented email
        val state = _state.value
        return currentAccountInfo
            .takeIf { email == state.presentedForEmail && email == it.account?.email }
            ?: LinkAccountUpdate.Value(null)
    }

    private suspend fun withConfiguration(
        email: String?,
        onError: suspend (Throwable) -> Unit,
        onSuccess: suspend (LinkConfiguration) -> Unit
    ) {
        val configuration = requireConfiguration()
            .map {
                it.copy(
                    customerInfo = LinkConfiguration.CustomerInfo(
                        name = null,
                        email = email,
                        phone = null,
                        billingCountryCode = null,
                    )
                )
            }
            .onFailure { error ->
                onError(error)
            }
            .getOrNull()
            ?: return

        onSuccess(configuration)
    }

    fun onLinkActivityResult(result: LinkActivityResult) {
        val currentLaunchMode = _state.value.currentLaunchMode
        _state.update { it.copy(currentLaunchMode = null) }

        when (currentLaunchMode) {
            is LinkLaunchMode.PaymentMethodSelection -> handlePaymentMethodSelectionResult(result)
            is LinkLaunchMode.Authentication -> handleAuthenticationResult(result)
            else -> logger.warning("$tag: unexpected result for launch mode: $currentLaunchMode")
        }

        // Update account, clearing state if null.
        result.linkAccountUpdate?.let { update ->
            when (update) {
                is LinkAccountUpdate.Value -> {
                    linkAccountHolder.set(update)
                    if (update.account == null) {
                        updateState {
                            it.copy(
                                selectedPaymentMethod = null,
                                createdPaymentMethod = null,
                            )
                        }
                    }
                }
                LinkAccountUpdate.None -> {
                    // Do nothing.
                }
            }
        }
    }

    private fun handlePaymentMethodSelectionResult(result: LinkActivityResult) {
        when (result) {
            is LinkActivityResult.Canceled -> {
                logger.debug("$tag: presentPaymentMethods canceled")
                viewModelScope.launch {
                    _presentPaymentMethodsResultFlow.emit(
                        LinkController.PresentPaymentMethodsResult.Canceled
                    )
                }
            }
            is LinkActivityResult.Completed -> {
                logger.debug("$tag: presentPaymentMethods completed: details=${result.selectedPayment?.details}")
                updateState {
                    it.copy(selectedPaymentMethod = result.selectedPayment)
                }
                viewModelScope.launch {
                    _presentPaymentMethodsResultFlow.emit(LinkController.PresentPaymentMethodsResult.Success)
                }
            }
            is LinkActivityResult.Failed -> {
                logger.debug("$tag: presentPaymentMethods failed")
                viewModelScope.launch {
                    _presentPaymentMethodsResultFlow.emit(
                        LinkController.PresentPaymentMethodsResult.Failed(result.error)
                    )
                }
            }
            is LinkActivityResult.PaymentMethodObtained -> {
                logger.warning("$tag: presentPaymentMethods unexpected result: $result")
            }
        }
    }

    private fun handleAuthenticationResult(result: LinkActivityResult) {
        when (result) {
            is LinkActivityResult.Canceled -> {
                logger.debug("$tag: presentForAuthentication canceled")
                viewModelScope.launch {
                    _presentForAuthenticationResultFlow.emit(PresentForAuthenticationResult.Canceled)
                }
            }
            is LinkActivityResult.Completed -> {
                logger.debug("$tag: presentForAuthentication completed")
                viewModelScope.launch {
                    _presentForAuthenticationResultFlow.emit(PresentForAuthenticationResult.Success)
                }
            }
            is LinkActivityResult.Failed -> {
                logger.debug("$tag: presentForAuthentication failed")
                viewModelScope.launch {
                    _presentForAuthenticationResultFlow.emit(
                        PresentForAuthenticationResult.Failed(result.error)
                    )
                }
            }
            is LinkActivityResult.PaymentMethodObtained -> {
                logger.warning("$tag: presentForAuthentication unexpected result: $result")
            }
        }
    }

    fun onLookupConsumer(email: String) {
        viewModelScope.launch {
            val result = linkRepository.lookupConsumer(email)
                .map { it.exists }
                .fold(
                    onSuccess = { LinkController.LookupConsumerResult.Success(email, it) },
                    onFailure = { LinkController.LookupConsumerResult.Failed(email, it) }
                )
            _lookupConsumerResultFlow.emit(result)
        }
    }

    fun onCreatePaymentMethod() {
        viewModelScope.launch {
            val paymentMethodResult = createPaymentMethod()
            updateState { it.copy(createdPaymentMethod = paymentMethodResult.getOrNull()) }
            _createPaymentMethodResultFlow.emit(
                paymentMethodResult.fold(
                    onSuccess = { LinkController.CreatePaymentMethodResult.Success },
                    onFailure = { LinkController.CreatePaymentMethodResult.Failed(it) },
                )
            )
        }
    }

    private fun requireConfiguration(state: State = _state.value): Result<LinkConfiguration> {
        return state.linkConfiguration
            ?.let { Result.success(it) }
            ?: Result.failure(MissingConfigurationException())
    }

    private suspend fun createPaymentMethod(): Result<PaymentMethod> {
        val state = _state.value
        val configuration = requireConfiguration(state)
            .getOrElse { return Result.failure(it) }
        val paymentMethod = state.selectedPaymentMethod
        val account = _account.value

        if (paymentMethod == null || account == null) {
            return Result.failure(IllegalStateException("Invalid state"))
        }

        return if (configuration.passthroughModeEnabled) {
            linkRepository.sharePaymentDetails(
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
            linkRepository.createPaymentMethod(
                consumerSessionClientSecret = account.clientSecret,
                paymentMethod = paymentMethod,
            )
        }
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

    @VisibleForTesting
    internal fun updateState(block: (State) -> State) {
        _state.update(block)
    }

    internal data class State(
        val linkConfiguration: LinkConfiguration? = null,
        val presentedForEmail: String? = null,
        val selectedPaymentMethod: LinkPaymentMethod? = null,
        val createdPaymentMethod: PaymentMethod? = null,
        val currentLaunchMode: LinkLaunchMode? = null,
    )

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            return DaggerLinkControllerViewModelComponent.factory()
                .build(
                    application = extras.requireApplication(),
                    savedStateHandle = extras.createSavedStateHandle(),
                    paymentElementCallbackIdentifier = "LinkController"
                )
                .viewModel as T
        }
    }
}
