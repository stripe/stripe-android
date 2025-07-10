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
import com.stripe.android.core.Logger
import com.stripe.android.core.utils.requireApplication
import com.stripe.android.link.account.LinkAccountHolder
import com.stripe.android.link.confirmation.computeExpectedPaymentMethodType
import com.stripe.android.link.exceptions.LinkUnavailableException
import com.stripe.android.link.injection.DaggerLinkControllerViewModelComponent
import com.stripe.android.link.injection.LinkControllerComponent
import com.stripe.android.link.repositories.LinkApiRepository
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
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
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
    private val linkApiRepository: LinkApiRepository,
    val controllerComponentFactory: LinkControllerComponent.Factory,
) : AndroidViewModel(application) {

    private val tag = "LinkControllerViewModel"

    private var configuration: LinkController.Configuration = LinkController.Configuration.default(application)

    private val _account = linkAccountHolder.linkAccountInfo.mapAsStateFlow { it.account }

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

    fun state(context: Context): StateFlow<LinkController.State> {
        return combineAsStateFlow(_account, _state) { account, state ->
            LinkController.State(
                isConsumerVerified = account?.isVerified,
                selectedPaymentMethodPreview = state.selectedPaymentMethod?.toPreview(context),
                createdPaymentMethod = state.createdPaymentMethod,
            )
        }
    }

    fun configure(configuration: LinkController.Configuration) {
        logger.debug("$tag: updating configuration")
        this.configuration = configuration
        _state.update { State() }
        viewModelScope.launch {
            updateLinkConfiguration()
        }
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

            // Try to obtain a Link configuration before we present.
            val configuration = awaitLinkConfigurationResult()
                .mapCatching { updateLinkConfiguration().getOrThrow() }
                .onFailure { error ->
                    _presentPaymentMethodsResultFlow.emit(
                        LinkController.PresentPaymentMethodsResult.Failed(
                            LinkUnavailableException(error)
                        )
                    )
                }
                .getOrNull()
                ?.copy(
                    customerInfo = LinkConfiguration.CustomerInfo(
                        name = null,
                        email = email,
                        phone = null,
                        billingCountryCode = null,
                    )
                )
                ?: return@launch
            val state = _state.value
            val linkAccountInfo = linkAccountHolder.linkAccountInfo.value
                .takeIf { email == state.presentedForEmail && email == it.account?.email }
                ?: LinkAccountUpdate.Value(null)

            // If the account changed, clear account-related state.
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
                logger.debug("$tag: presentPaymentMethods canceled")
                viewModelScope.launch {
                    _presentPaymentMethodsResultFlow.emit(
                        LinkController.PresentPaymentMethodsResult.Canceled
                    )
                }
            }
            is LinkActivityResult.Completed -> {
                logger.debug("$tag: presentPaymentMethods completed: details=${result.selectedPayment?.details}")
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

        // Update account, clearing state if null.
        result.linkAccountUpdate?.let { update ->
            val value = update.asValue()
            linkAccountHolder.set(value)
            if (value.account == null) {
                _state.update {
                    it.copy(
                        selectedPaymentMethod = null,
                        createdPaymentMethod = null,
                    )
                }
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
        viewModelScope.launch {
            val paymentMethodResult = createPaymentMethod()
            _state.update { it.copy(createdPaymentMethod = paymentMethodResult.getOrNull()) }
            _createPaymentMethodResultFlow.emit(
                paymentMethodResult.fold(
                    onSuccess = { LinkController.CreatePaymentMethodResult.Success },
                    onFailure = { LinkController.CreatePaymentMethodResult.Failed(it) },
                )
            )
        }
    }

    private suspend fun updateLinkConfiguration(): Result<LinkConfiguration> {
        _state.update { it.copy(linkConfigurationResult = null) }
        val result = linkConfigurationLoader.load(configuration)
        _state.update { state ->
            state.copy(linkConfigurationResult = result)
        }
        return result
    }

    private suspend fun awaitLinkConfigurationResult(): Result<LinkConfiguration> {
        return _state
            .map { it.linkConfigurationResult }
            .filterNotNull()
            .first()
    }

    private suspend fun createPaymentMethod(): Result<PaymentMethod> {
        val state = _state.value
        val paymentMethod = state.selectedPaymentMethod
        val configuration = state.linkConfiguration
        val account = _account.value

        if (paymentMethod == null || configuration == null || account == null) {
            return Result.failure(IllegalStateException("Invalid state"))
        }

        return if (configuration.passthroughModeEnabled) {
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
        val linkConfigurationResult: Result<LinkConfiguration>? = null,
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
                    paymentElementCallbackIdentifier = "LinkController"
                )
                .viewModel as T
        }
    }
}
