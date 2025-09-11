package com.stripe.android.link

import android.app.Application
import androidx.activity.result.ActivityResultCaller
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavBackStackEntry
import androidx.navigation.compose.NavHost
import com.stripe.android.link.LinkAccountUpdate.Value.UpdateReason.LoggedOut
import com.stripe.android.link.LinkAccountUpdate.Value.UpdateReason.PaymentConfirmed
import com.stripe.android.link.LinkActivity.Companion.getArgs
import com.stripe.android.link.account.LinkAccountHolder
import com.stripe.android.link.account.LinkAccountManager
import com.stripe.android.link.account.linkAccountUpdate
import com.stripe.android.link.attestation.LinkAttestationCheck
import com.stripe.android.link.confirmation.LinkConfirmationHandler
import com.stripe.android.link.injection.DaggerNativeLinkComponent
import com.stripe.android.link.injection.LINK_EXPRESS_MODE
import com.stripe.android.link.injection.NativeLinkComponent
import com.stripe.android.link.injection.NativeLinkScope
import com.stripe.android.link.model.AccountStatus
import com.stripe.android.link.model.ConsentPresentation
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.link.ui.LinkAppBarState
import com.stripe.android.link.ui.signup.SignUpViewModel
import com.stripe.android.link.ui.wallet.AddPaymentMethodOption
import com.stripe.android.link.ui.wallet.AddPaymentMethodOptions
import com.stripe.android.link.utils.LINK_DEFAULT_ANIMATION_DELAY_MILLIS
import com.stripe.android.model.PassiveCaptchaParams
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.bootstrap
import com.stripe.android.paymentsheet.addresselement.AutocompleteActivityLauncher
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.uicore.navigation.NavBackStackEntryUpdate
import com.stripe.android.uicore.navigation.NavigationManager
import com.stripe.android.uicore.navigation.PopUpToBehavior
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Named
import com.stripe.android.link.confirmation.Result as LinkConfirmationResult

@SuppressWarnings("TooManyFunctions")
@NativeLinkScope
internal class LinkActivityViewModel @Inject constructor(
    val activityRetainedComponent: NativeLinkComponent,
    confirmationHandlerFactory: ConfirmationHandler.Factory,
    linkConfirmationHandlerFactory: LinkConfirmationHandler.Factory,
    private val linkAccountManager: LinkAccountManager,
    private val linkAccountHolder: LinkAccountHolder,
    val eventReporter: EventReporter,
    val linkConfiguration: LinkConfiguration,
    private val linkAttestationCheck: LinkAttestationCheck,
    val savedStateHandle: SavedStateHandle,
    @Named(LINK_EXPRESS_MODE) private val linkExpressMode: LinkExpressMode,
    private val navigationManager: NavigationManager,
    val linkLaunchMode: LinkLaunchMode,
    private val autocompleteLauncher: AutocompleteActivityLauncher,
    private val addPaymentMethodOptionsFactory: AddPaymentMethodOptions.Factory,
    private val passiveCaptchaParams: PassiveCaptchaParams?
) : ViewModel(), DefaultLifecycleObserver {
    val confirmationHandler = confirmationHandlerFactory.create(viewModelScope)
    val linkConfirmationHandler = linkConfirmationHandlerFactory.create(confirmationHandler)

    private val _linkAppBarState = MutableStateFlow(LinkAppBarState.initial())
    val linkAppBarState: StateFlow<LinkAppBarState> = _linkAppBarState.asStateFlow()

    // Enable replay because the result can be emitted during `onCreate` while observation occurs
    // in the Compose UI, which is invoked after `onCreate`.
    private val _result = MutableSharedFlow<LinkActivityResult>(replay = 1, extraBufferCapacity = 1)
    val result: SharedFlow<LinkActivityResult> = _result.asSharedFlow()

    val navigationFlow = navigationManager.navigationFlow

    private val _linkScreenState = MutableStateFlow<ScreenState>(ScreenState.Loading)
    val linkScreenState: StateFlow<ScreenState> = _linkScreenState

    val linkAccount: LinkAccount?
        get() = linkAccountManager.linkAccountInfo.value.account

    var launchWebFlow: ((LinkConfiguration) -> Unit)? = null

    val canDismissSheet: Boolean
        get() = activityRetainedComponent.dismissalCoordinator.canDismiss

    fun handleViewAction(action: LinkAction) {
        when (action) {
            LinkAction.BackPressed -> handleBackPressed()
            LinkAction.LogoutClicked -> handleLogoutClicked()
        }
    }

    fun onVerificationSucceeded() {
        viewModelScope.launch {
            updateScreenState(withAnimationDelay = false)
        }
    }

    fun onDismissVerificationClicked() {
        dismissWithResult(
            LinkActivityResult.Canceled(
                linkAccountUpdate = linkAccountManager.linkAccountUpdate
            )
        )
    }

    fun handleResult(result: LinkActivityResult) {
        dismissWithResult(result)
    }

    fun dismissSheet() {
        if (canDismissSheet) {
            dismissWithResult(
                LinkActivityResult.Canceled(
                    linkAccountUpdate = linkAccountManager.linkAccountUpdate
                )
            )
        }
    }

    fun onContentCanScrollBackwardChanged(canScrollBackward: Boolean) {
        _linkAppBarState.update {
            it.copy(isElevated = canScrollBackward)
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun handleLogoutClicked() {
        GlobalScope.launch {
            linkAccountManager.logOut()
        }

        dismissWithResult(
            LinkActivityResult.Canceled(
                reason = LinkActivityResult.Canceled.Reason.LoggedOut,
                linkAccountUpdate = LinkAccountUpdate.Value(null, LoggedOut)
            )
        )
    }

    fun onNavEntryChanged(entry: NavBackStackEntryUpdate) {
        val currentEntry: NavBackStackEntry = entry.currentBackStackEntry ?: return
        val previousEntry = entry.previousBackStackEntry?.destination?.route
        _linkAppBarState.update {
            LinkAppBarState.create(
                currentEntry = currentEntry,
                previousEntryRoute = previousEntry,
                consumerIsSigningUp = linkAccount?.completedSignup == true
            )
        }
    }

    fun moveToWeb(error: Throwable) {
        when (linkLaunchMode) {
            // Authentication flows with existing accounts -> dismiss with an error.
            is LinkLaunchMode.Authentication -> dismissWithResult(
                LinkActivityResult.Failed(
                    error = error,
                    linkAccountUpdate = LinkAccountUpdate.None
                )
            )
            is LinkLaunchMode.Authorization -> dismissWithResult(
                LinkActivityResult.Failed(
                    error = IllegalStateException("Authorization mode is not supported in web"),
                    linkAccountUpdate = LinkAccountUpdate.None
                )
            )
            // Payment selection flows -> dismiss selecting Link with no selected payment method.
            is LinkLaunchMode.PaymentMethodSelection -> dismissWithResult(
                LinkActivityResult.Completed(
                    linkAccountUpdate = LinkAccountUpdate.None,
                    selectedPayment = null,
                    shippingAddress = null
                )
            )
            // Flows that end up in confirmation -> we can launch the web flow.
            is LinkLaunchMode.Confirmation,
            LinkLaunchMode.Full -> launchWebFlow?.let { launcher ->
                navigate(LinkScreen.Loading, clearStack = true)
                launcher.invoke(linkConfiguration)
            }
        }
    }

    /**
     * [NavHost] handles back presses except for when backstack is empty, where it delegates
     * to the container activity. [onBackPressed] will be triggered on these empty backstack cases.
     */
    fun handleBackPressed() {
        dismissWithResult(
            LinkActivityResult.Canceled(
                linkAccountUpdate = linkAccountManager.linkAccountUpdate
            )
        )
    }

    fun registerForActivityResult(
        activityResultCaller: ActivityResultCaller,
        lifecycleOwner: LifecycleOwner,
    ) {
        autocompleteLauncher.register(activityResultCaller, lifecycleOwner)
        confirmationHandler.register(activityResultCaller, lifecycleOwner)
        if (passiveCaptchaParams != null) {
            confirmationHandler.bootstrap(passiveCaptchaParams, lifecycleOwner)
        }
    }

    fun navigate(screen: LinkScreen, clearStack: Boolean, launchSingleTop: Boolean = false) {
        navigationManager.tryNavigateTo(
            screen.route,
            isSingleTop = launchSingleTop,
            // If clearing the stack, pop to the start destination.
            popUpTo = if (clearStack) PopUpToBehavior.Start else null
        )
    }

    fun goBack() {
        if (canDismissSheet) {
            navigationManager.tryNavigateBack()
        }
    }

    fun changeEmail() {
        savedStateHandle[SignUpViewModel.USE_LINK_CONFIGURATION_CUSTOMER_INFO] = false
        if (linkScreenState.value is ScreenState.VerificationDialog) {
            linkAccountHolder.set(LinkAccountUpdate.Value(null))
            _linkScreenState.value = ScreenState.FullScreen(initialDestination = LinkScreen.SignUp)
        } else {
            navigate(LinkScreen.SignUp, clearStack = true)
        }
    }

    fun unregisterActivity() {
        launchWebFlow = null
    }

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        viewModelScope.launch {
            when (linkLaunchMode) {
                is LinkLaunchMode.Full,
                is LinkLaunchMode.PaymentMethodSelection,
                is LinkLaunchMode.Authentication,
                is LinkLaunchMode.Authorization ->
                    loadLink()
                is LinkLaunchMode.Confirmation ->
                    confirmLinkPayment(linkLaunchMode.selectedPayment)
            }
        }
    }

    private suspend fun loadLink() {
        val attestationCheckResult = linkAttestationCheck.invoke()
        when (attestationCheckResult) {
            is LinkAttestationCheck.Result.AttestationFailed -> {
                when (linkExpressMode) {
                    LinkExpressMode.DISABLED,
                    LinkExpressMode.ENABLED -> moveToWeb(attestationCheckResult.error)
                    LinkExpressMode.ENABLED_NO_WEB_FALLBACK -> updateScreenState(withAnimationDelay = true)
                }
            }
            LinkAttestationCheck.Result.Successful -> {
                updateScreenState(withAnimationDelay = true)
            }
            is LinkAttestationCheck.Result.Error,
            is LinkAttestationCheck.Result.AccountError -> {
                handleAccountError()
            }
        }
    }

    /**
     * Attempts to confirm payment eagerly if the Link flow was launched in confirmation mode.
     */
    private suspend fun confirmLinkPayment(selectedPayment: LinkPaymentMethod) = runCatching {
        require(selectedPayment.readyForConfirmation()) { "LinkPaymentMethod must be ready for confirmation" }
        val linkAccount = requireNotNull(linkAccount) { "LinkAccount must not be null for confirmation" }
        when (selectedPayment) {
            is LinkPaymentMethod.ConsumerPaymentDetails -> linkConfirmationHandler.confirm(
                paymentDetails = selectedPayment.details,
                cvc = selectedPayment.collectedCvc,
                billingPhone = selectedPayment.billingPhone,
                linkAccount = linkAccount,
            )
            is LinkPaymentMethod.LinkPaymentDetails -> linkConfirmationHandler.confirm(
                paymentDetails = selectedPayment.linkPaymentDetails,
                cvc = selectedPayment.collectedCvc,
                billingPhone = selectedPayment.billingPhone,
                linkAccount = linkAccount,
            )
        }
    }.onSuccess { confirmResult ->
        dismissWithResult(
            when (confirmResult) {
                LinkConfirmationResult.Canceled -> LinkActivityResult.Canceled(
                    reason = LinkActivityResult.Canceled.Reason.BackPressed,
                    linkAccountUpdate = LinkAccountUpdate.None
                )
                is LinkConfirmationResult.Failed -> LinkActivityResult.Failed(
                    IllegalStateException("Failed to confirm Link payment: ${confirmResult.message}"),
                    linkAccountUpdate = LinkAccountUpdate.None
                )
                LinkConfirmationResult.Succeeded -> LinkActivityResult.Completed(
                    linkAccountUpdate = LinkAccountUpdate.Value(null, PaymentConfirmed)
                )
            }
        )
    }.onFailure { error ->
        dismissWithResult(
            LinkActivityResult.Failed(
                error,
                linkAccountUpdate = LinkAccountUpdate.None
            )
        )
    }

    @Suppress("CyclomaticComplexMethod", "ComplexCondition")
    private suspend fun updateScreenState(withAnimationDelay: Boolean) {
        val accountStatus = linkAccountManager.accountStatus.first()

        // Get linkAccount after getting `accountStatus` because account may be updated.
        val linkAccount = this.linkAccount

        val authenticatingExistingAccount = (linkLaunchMode as? LinkLaunchMode.Authentication)?.existingOnly == true
        val authorizingAuthIntent = linkLaunchMode is LinkLaunchMode.Authorization
        val cannotChangeEmails = !linkConfiguration.allowUserEmailEdits
        val accountNotFound = accountStatus == AccountStatus.SignedOut || accountStatus == AccountStatus.Error
        val accountRequired = authorizingAuthIntent || authenticatingExistingAccount || cannotChangeEmails
        if (accountNotFound && accountRequired) {
            dismissWithResult(
                LinkActivityResult.Failed(
                    error = NoLinkAccountFoundException(),
                    linkAccountUpdate = LinkAccountUpdate.None
                )
            )
            return
        }

        if (authenticatingExistingAccount &&
            accountStatus is AccountStatus.Verified &&
            !accountStatus.hasVerifiedSMSSession &&
            linkAccount != null // Should always be non-null.
        ) {
            // Handle edge case where status is "verified" but don't have a verified SMS session.
            // This can happen after registering a new user without verifying their phone number.
            _linkScreenState.value = ScreenState.VerificationDialog(linkAccount)
            return
        }

        if (authorizingAuthIntent &&
            accountStatus is AccountStatus.Verified &&
            accountStatus.consentPresentation is ConsentPresentation.Inline
        ) {
            // Already completed verification with inline consent.
            dismissWithResult(LinkActivityResult.Completed(linkAccountManager.linkAccountUpdate))
            return
        }

        when (accountStatus) {
            is AccountStatus.Verified,
            AccountStatus.SignedOut,
            AccountStatus.Error -> {
                _linkScreenState.value = buildFullScreenState(withAnimationDelay)
            }
            AccountStatus.NeedsVerification,
            AccountStatus.VerificationStarted -> {
                if (linkAccount != null && linkExpressMode != LinkExpressMode.DISABLED) {
                    _linkScreenState.value = ScreenState.VerificationDialog(linkAccount)
                } else {
                    _linkScreenState.value = buildFullScreenState(withAnimationDelay)
                }
            }
        }
    }

    private suspend fun buildFullScreenState(withAnimationDelay: Boolean): ScreenState.FullScreen {
        val accountStatus = linkAccountManager.accountStatus.first()

        // We add a tiny delay, which gives the loading screen a chance to fully inflate.
        // Otherwise, we get a weird scaling animation when we display the first non-loading screen.
        if (withAnimationDelay) {
            delay(LINK_DEFAULT_ANIMATION_DELAY_MILLIS)
        }

        val linkAccount = this.linkAccount

        return ScreenState.FullScreen(
            initialDestination = when (accountStatus) {
                is AccountStatus.Verified -> {
                    if (linkLaunchMode is LinkLaunchMode.Authorization) {
                        LinkScreen.OAuthConsent
                    } else if (linkAccount?.completedSignup == true && linkLaunchMode.selectedPayment() == null) {
                        // We just completed signup, but haven't added a payment method yet.
                        when (addPaymentMethodOptionsFactory.create(linkAccount).default) {
                            is AddPaymentMethodOption.Bank -> LinkScreen.Wallet
                            // Default to previous behavior, even though this could be better.
                            else -> LinkScreen.PaymentMethod
                        }
                    } else {
                        // We have a verified account, or we're relaunching after signing up and adding a payment,
                        // then show the wallet.
                        LinkScreen.Wallet
                    }
                }
                AccountStatus.NeedsVerification,
                AccountStatus.VerificationStarted -> {
                    LinkScreen.Verification
                }
                AccountStatus.SignedOut,
                AccountStatus.Error -> {
                    LinkScreen.SignUp
                }
            }
        )
    }

    private suspend fun handleAccountError() {
        linkAccountManager.logOut()
        linkAccountHolder.set(LinkAccountUpdate.Value(account = null, lastUpdateReason = LoggedOut))
        updateScreenState(withAnimationDelay = true)
    }

    private fun dismissWithResult(result: LinkActivityResult) {
        viewModelScope.launch {
            _result.emit(result)
        }
    }

    companion object {
        fun factory(savedStateHandle: SavedStateHandle? = null): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val handle: SavedStateHandle = savedStateHandle ?: createSavedStateHandle()
                val app = this[APPLICATION_KEY] as Application
                val args: NativeLinkArgs = getArgs(handle) ?: throw NoArgsException()
                DaggerNativeLinkComponent
                    .builder()
                    .configuration(args.configuration)
                    .passiveCaptchaParams(args.passiveCaptchaParams)
                    .requestSurface(args.requestSurface)
                    .publishableKeyProvider { args.publishableKey }
                    .stripeAccountIdProvider { args.stripeAccountId }
                    .paymentElementCallbackIdentifier(args.paymentElementCallbackIdentifier)
                    .savedStateHandle(handle)
                    .context(app)
                    .application(app)
                    .linkExpressMode(args.linkExpressMode)
                    .linkLaunchMode(args.launchMode)
                    .linkAccountUpdate(args.linkAccountInfo)
                    .build()
                    .viewModel
            }
        }
    }
}

internal sealed interface ScreenState {
    data class VerificationDialog(val linkAccount: LinkAccount) : ScreenState
    data class FullScreen(val initialDestination: LinkScreen) : ScreenState
    data object Loading : ScreenState
}

internal class NoArgsException : IllegalArgumentException("NativeLinkArgs not found")
