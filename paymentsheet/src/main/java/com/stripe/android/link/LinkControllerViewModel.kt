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
import com.stripe.android.core.utils.flatMapCatching
import com.stripe.android.core.utils.requireApplication
import com.stripe.android.link.LinkController.AuthenticationResult
import com.stripe.android.link.account.LinkAccountHolder
import com.stripe.android.link.account.LinkAuthResult
import com.stripe.android.link.attestation.LinkAttestationCheck
import com.stripe.android.link.confirmation.computeExpectedPaymentMethodType
import com.stripe.android.link.exceptions.AppAttestationException
import com.stripe.android.link.exceptions.MissingConfigurationException
import com.stripe.android.link.injection.DaggerLinkControllerViewModelComponent
import com.stripe.android.link.injection.LinkComponent
import com.stripe.android.link.injection.LinkControllerComponent
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.link.model.toLoginState
import com.stripe.android.link.ui.inline.SignUpConsentAction
import com.stripe.android.link.ui.wallet.displayName
import com.stripe.android.model.EmailSource
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.parsers.PaymentMethodJsonParser
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.state.LinkState
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
import javax.inject.Provider
import javax.inject.Singleton

@Singleton
internal class LinkControllerViewModel @Inject constructor(
    application: Application,
    private val logger: Logger,
    private val linkConfigurationLoader: LinkConfigurationLoader,
    private val linkAccountHolder: LinkAccountHolder,
    private val linkComponentBuilderProvider: Provider<LinkComponent.Builder>,
    val controllerComponentFactory: LinkControllerComponent.Factory,
) : AndroidViewModel(application) {

    private val tag = "LinkControllerViewModel"

    private val _account = linkAccountHolder.linkAccountInfo.mapAsStateFlow { it.account }

    private val _internalLinkAccount = _account.mapAsStateFlow {
        it?.let { account ->
            LinkController.LinkAccount(
                email = account.email,
                redactedPhoneNumber = account.redactedPhoneNumber,
                sessionState = when (account.accountStatus.toLoginState()) {
                    LinkState.LoginState.LoggedOut ->
                        LinkController.SessionState.LoggedOut
                    LinkState.LoginState.NeedsVerification ->
                        LinkController.SessionState.NeedsVerification
                    LinkState.LoginState.LoggedIn ->
                        LinkController.SessionState.LoggedIn
                },
                consumerSessionClientSecret = account.clientSecret
            )
        }
    }

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

    private val _authenticationResultFlow =
        MutableSharedFlow<AuthenticationResult>(replay = 1)
    val authenticationResultFlow = _authenticationResultFlow.asSharedFlow()

    private val _registerConsumerResultFlow =
        MutableSharedFlow<LinkController.RegisterConsumerResult>(replay = 1)
    val registerConsumerResultFlow = _registerConsumerResultFlow.asSharedFlow()

    private var presentJob: Job? = null

    fun state(context: Context): StateFlow<LinkController.State> {
        return combineAsStateFlow(_internalLinkAccount, _state) { account, state ->
            LinkController.State(
                internalLinkAccount = account,
                selectedPaymentMethodPreview = state.selectedPaymentMethod?.toPreview(context),
                createdPaymentMethod = state.createdPaymentMethod,
            )
        }
    }

    suspend fun configure(configuration: LinkController.Configuration): LinkController.ConfigureResult {
        logger.debug("$tag: updating configuration")
        updateState { State() }
        return linkConfigurationLoader.load(configuration)
            .flatMapCatching { config ->
                val component = linkComponentBuilderProvider.get()
                    .configuration(config)
                    .build()
                component.linkAttestationCheck.invoke()
                    .toResult()
                    .map { component }
            }
            .fold(
                onSuccess = { component ->
                    updateState { it.copy(linkComponent = component) }
                    LinkController.ConfigureResult.Success
                },
                onFailure = { error ->
                    LinkController.ConfigureResult.Failed(error)
                }
            )
    }

    fun onPresentPaymentMethods(
        launcher: ActivityResultLauncher<LinkActivityContract.Args>,
        email: String?,
        hint: String? = null,
    ) {
        present(
            launcher = launcher,
            email = email,
            onConfigurationError = { error ->
                _presentPaymentMethodsResultFlow.emit(
                    LinkController.PresentPaymentMethodsResult.Failed(error)
                )
            },
            getLaunchMode = { _, state ->
                LinkLaunchMode.PaymentMethodSelection(
                    selectedPayment = state.selectedPaymentMethod?.details,
                    sharePaymentDetailsImmediatelyAfterCreation = false,
                    hint = hint,
                )
            }
        )
    }

    fun onAuthenticate(
        launcher: ActivityResultLauncher<LinkActivityContract.Args>,
        email: String?
    ) {
        performAuthentication(launcher, email, existingOnly = false)
    }

    fun onAuthenticateExistingConsumer(
        launcher: ActivityResultLauncher<LinkActivityContract.Args>,
        email: String
    ) {
        performAuthentication(launcher, email, existingOnly = true)
    }

    private fun performAuthentication(
        launcher: ActivityResultLauncher<LinkActivityContract.Args>,
        email: String?,
        existingOnly: Boolean
    ) {
        present(
            launcher = launcher,
            email = email,
            onConfigurationError = { error ->
                _authenticationResultFlow.emit(
                    AuthenticationResult.Failed(error)
                )
            },
            getLaunchMode = { linkAccount, _ ->
                if (linkAccount?.isVerified == true) {
                    logger.debug("$tag: account is already verified, skipping authentication")
                    _authenticationResultFlow.emit(AuthenticationResult.Success)
                    null
                } else {
                    LinkLaunchMode.Authentication(existingOnly = existingOnly)
                }
            }
        )
    }

    private suspend fun withConfiguration(
        email: String?,
        onError: suspend (Throwable) -> Unit,
        onSuccess: suspend (LinkConfiguration) -> Unit
    ) {
        val configuration = requireLinkComponent()
            .map { it.configuration }
            .map { config ->
                email
                    ?.let { config.copy(customerInfo = config.customerInfo.copy(email = email)) }
                    ?: config
            }

        configuration.fold(
            onSuccess = { onSuccess(it) },
            onFailure = { error -> onError(error) }
        )
    }

    fun onLinkActivityResult(result: LinkActivityResult) {
        val currentLaunchMode = _state.value.currentLaunchMode
        updateState { it.copy(currentLaunchMode = null) }
        updateStateOnAccountUpdate(result.linkAccountUpdate)

        when (currentLaunchMode) {
            is LinkLaunchMode.PaymentMethodSelection ->
                handlePaymentMethodSelectionResult(result)
            is LinkLaunchMode.Authentication ->
                handleAuthenticationResult(result)
            else ->
                logger.warning("$tag: unexpected result for launch mode: $currentLaunchMode")
        }
    }

    private fun updateStateOnNewEmail(email: String?) {
        val currentAccountEmail = _account.value?.email
        // Keep state if...
        val keepState =
            // input email matches previous input email (to support user changing emails), or
            email == _state.value.emailInput ||
                // not previously logged in, or
                currentAccountEmail == null ||
                // input email matches current logged in account email
                email == currentAccountEmail
        if (!keepState) {
            linkAccountHolder.set(LinkAccountUpdate.Value(null))
        }
        updateState {
            it.copy(
                emailInput = email,
                selectedPaymentMethod = it.selectedPaymentMethod.takeIf { keepState },
                createdPaymentMethod = it.createdPaymentMethod.takeIf { keepState },
            )
        }
    }

    private fun updateStateOnAccountUpdate(update: LinkAccountUpdate?) {
        when (update) {
            is LinkAccountUpdate.Value -> {
                val currentAccountEmail = _account.value?.email
                val newAccountEmail = update.account?.email
                // Keep state if not previously logged in or new account email matches previous email
                val keepState = currentAccountEmail == null || newAccountEmail == currentAccountEmail
                linkAccountHolder.set(update)
                updateState {
                    it.copy(
                        selectedPaymentMethod = it.selectedPaymentMethod.takeIf { keepState },
                        createdPaymentMethod = it.createdPaymentMethod.takeIf { keepState },
                    )
                }
            }
            is LinkAccountUpdate.None, null -> {
                // Do nothing.
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
                logger.debug("$tag: authentication canceled")
                viewModelScope.launch {
                    _authenticationResultFlow.emit(AuthenticationResult.Canceled)
                }
            }
            is LinkActivityResult.Completed -> {
                logger.debug("$tag: authentication completed")
                viewModelScope.launch {
                    _authenticationResultFlow.emit(AuthenticationResult.Success)
                }
            }
            is LinkActivityResult.Failed -> {
                logger.debug("$tag: authentication failed")
                viewModelScope.launch {
                    _authenticationResultFlow.emit(
                        AuthenticationResult.Failed(result.error)
                    )
                }
            }
            is LinkActivityResult.PaymentMethodObtained -> {
                logger.warning("$tag: authentication unexpected result: $result")
            }
        }
    }

    fun onLookupConsumer(email: String) {
        viewModelScope.launch {
            val result = requireLinkComponent()
                .flatMapCatching { component ->
                    component.linkAuth.lookUp(
                        email = email,
                        emailSource = EmailSource.USER_ACTION,
                        startSession = false,
                        customerId = null,
                    )
                        .toResult()
                        .map { it != null }
                }
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

    fun onRegisterConsumer(
        email: String,
        phone: String,
        country: String,
        name: String?,
    ) {
        viewModelScope.launch {
            val result = requireLinkComponent()
                .flatMapCatching {
                    it.linkAuth.signUp(
                        email = email,
                        phoneNumber = phone,
                        country = country,
                        name = name,
                        consentAction = SignUpConsentAction.Implied
                    ).toResult()
                }
                .fold(
                    onSuccess = { account ->
                        updateStateOnAccountUpdate(LinkAccountUpdate.Value(account))
                        LinkController.RegisterConsumerResult.Success
                    },
                    onFailure = {
                        updateStateOnAccountUpdate(LinkAccountUpdate.Value(null))
                        LinkController.RegisterConsumerResult.Failed(it)
                    }
                )
            _registerConsumerResultFlow.emit(result)
        }
    }

    private fun requireLinkComponent(state: State = _state.value): Result<LinkComponent> {
        return state.linkComponent
            ?.let { Result.success(it) }
            ?: Result.failure(MissingConfigurationException())
    }

    private fun present(
        launcher: ActivityResultLauncher<LinkActivityContract.Args>,
        email: String?,
        onConfigurationError: suspend (Throwable) -> Unit,
        getLaunchMode: suspend (linkAccount: LinkAccount?, state: State) -> LinkLaunchMode?
    ) {
        if (presentJob?.isActive == true) {
            logger.debug("$tag: already presenting")
            return
        }
        presentJob = viewModelScope.launch {
            logger.debug("$tag: presenting")

            withConfiguration(
                email = email,
                onError = onConfigurationError,
                onSuccess = { configuration ->
                    updateStateOnNewEmail(email)

                    val launchMode = getLaunchMode(_account.value, _state.value)
                        ?: return@withConfiguration

                    updateState {
                        it.copy(
                            emailInput = email,
                            currentLaunchMode = launchMode,
                        )
                    }

                    launcher.launch(
                        LinkActivityContract.Args(
                            configuration = configuration,
                            startWithVerificationDialog = true,
                            linkAccountInfo = linkAccountHolder.linkAccountInfo.value,
                            launchMode = launchMode,
                        )
                    )
                }
            )
        }
    }

    private suspend fun createPaymentMethod(): Result<PaymentMethod> {
        val state = _state.value
        val component = requireLinkComponent(state)
            .getOrElse { return Result.failure(it) }
        val configuration = component.configuration
        val paymentMethod = state.selectedPaymentMethod
            ?: return Result.failure(IllegalStateException("No selected payment method"))

        return if (configuration.passthroughModeEnabled) {
            component.linkAccountManager.sharePaymentDetails(
                paymentDetailsId = paymentMethod.details.id,
                expectedPaymentMethodType = computeExpectedPaymentMethodType(configuration, paymentMethod.details),
                cvc = paymentMethod.collectedCvc,
                billingPhone = null,
            ).map { shareDetails ->
                val json = JSONObject(shareDetails.encodedPaymentMethod)
                PaymentMethodJsonParser().parse(json)
            }
        } else {
            component.linkAccountManager.createPaymentMethod(
                linkPaymentMethod = paymentMethod
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

    private fun LinkAttestationCheck.Result.toResult(): Result<Unit> =
        when (this) {
            is LinkAttestationCheck.Result.AccountError ->
                Result.failure(error)
            is LinkAttestationCheck.Result.AttestationFailed ->
                Result.failure(AppAttestationException(error))
            is LinkAttestationCheck.Result.Error ->
                Result.failure(error)
            LinkAttestationCheck.Result.Successful ->
                Result.success(Unit)
        }

    private fun LinkAuthResult.toResult(): Result<LinkAccount?> =
        when (this) {
            is LinkAuthResult.AccountError ->
                Result.failure(error)
            is LinkAuthResult.AttestationFailed ->
                Result.failure(AppAttestationException(error))
            is LinkAuthResult.Error ->
                Result.failure(error)
            LinkAuthResult.NoLinkAccountFound ->
                Result.success(null)
            is LinkAuthResult.Success ->
                Result.success(account)
        }

    @VisibleForTesting
    internal fun updateState(block: (State) -> State) {
        _state.update(block)
    }

    internal data class State(
        val linkComponent: LinkComponent? = null,
        val emailInput: String? = null,
        val selectedPaymentMethod: LinkPaymentMethod? = null,
        val createdPaymentMethod: PaymentMethod? = null,
        val currentLaunchMode: LinkLaunchMode? = null,
    ) {
        val linkConfiguration: LinkConfiguration?
            get() = linkComponent?.configuration
    }

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
