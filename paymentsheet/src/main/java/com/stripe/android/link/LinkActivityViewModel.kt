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
import androidx.navigation.compose.NavHost
import com.stripe.android.link.LinkActivity.Companion.getArgs
import com.stripe.android.link.account.LinkAccountHolder
import com.stripe.android.link.account.LinkAccountManager
import com.stripe.android.link.account.linkAccountUpdate
import com.stripe.android.link.attestation.LinkAttestationCheck
import com.stripe.android.link.confirmation.LinkConfirmationHandler
import com.stripe.android.link.injection.DaggerNativeLinkComponent
import com.stripe.android.link.injection.NativeLinkComponent
import com.stripe.android.link.model.AccountStatus
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.link.ui.LinkAppBarState
import com.stripe.android.link.ui.signup.SignUpViewModel
import com.stripe.android.link.utils.LINK_DEFAULT_ANIMATION_DELAY_MILLIS
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
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
import com.stripe.android.link.confirmation.Result as LinkConfirmationResult

@SuppressWarnings("TooManyFunctions")
internal class LinkActivityViewModel @Inject constructor(
    val activityRetainedComponent: NativeLinkComponent,
    confirmationHandlerFactory: ConfirmationHandler.Factory,
    linkConfirmationHandlerFactory: LinkConfirmationHandler.Factory,
    private val linkAccountManager: LinkAccountManager,
    private val linkAccountHolder: LinkAccountHolder,
    val eventReporter: EventReporter,
    private val linkConfiguration: LinkConfiguration,
    private val linkAttestationCheck: LinkAttestationCheck,
    val savedStateHandle: SavedStateHandle,
    private val startWithVerificationDialog: Boolean,
    private val navigationManager: NavigationManager,
    val linkLaunchMode: LinkLaunchMode,
) : ViewModel(), DefaultLifecycleObserver {
    val confirmationHandler = confirmationHandlerFactory.create(viewModelScope)
    val linkConfirmationHandler = linkConfirmationHandlerFactory.create(confirmationHandler)

    private val _linkAppBarState = MutableStateFlow(LinkAppBarState.initial())
    val linkAppBarState: StateFlow<LinkAppBarState> = _linkAppBarState.asStateFlow()

    private val _result = MutableSharedFlow<LinkActivityResult>(extraBufferCapacity = 1)
    val result: SharedFlow<LinkActivityResult> = _result.asSharedFlow()

    val navigationFlow = navigationManager.navigationFlow

    private val _linkScreenState = MutableStateFlow<ScreenState>(ScreenState.Loading)
    val linkScreenState: StateFlow<ScreenState> = _linkScreenState

    val linkAccount: LinkAccount?
        get() = linkAccountManager.linkAccount.value

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
            _linkScreenState.value = buildFullScreenState()
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

    @OptIn(DelicateCoroutinesApi::class)
    private fun handleLogoutClicked() {
        GlobalScope.launch {
            linkAccountManager.logOut()
        }

        dismissWithResult(
            LinkActivityResult.Canceled(
                reason = LinkActivityResult.Canceled.Reason.LoggedOut,
                linkAccountUpdate = LinkAccountUpdate.Value(null)
            )
        )
    }

    fun onNavEntryChanged(entry: NavBackStackEntryUpdate) {
        val route: String = entry.currentBackStackEntry?.destination?.route ?: return
        val previousEntry = entry.previousBackStackEntry?.destination?.route
        _linkAppBarState.update {
            LinkAppBarState.create(
                route = route,
                previousEntryRoute = previousEntry,
                consumerIsSigningUp = linkAccount?.completedSignup == true,
            )
        }
    }

    fun moveToWeb() {
        launchWebFlow?.let { launcher ->
            navigate(LinkScreen.Loading, clearStack = true)
            launcher.invoke(linkConfiguration)
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

    fun registerActivityForConfirmation(
        activityResultCaller: ActivityResultCaller,
        lifecycleOwner: LifecycleOwner,
    ) {
        confirmationHandler.register(activityResultCaller, lifecycleOwner)
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
        navigate(LinkScreen.SignUp, clearStack = true)
    }

    fun unregisterActivity() {
        launchWebFlow = null
    }

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        viewModelScope.launch {
            val confirmResult = confirmPreselectedLinkPaymentIfAvailable()
            when (confirmResult) {
                // if no confirmation required or something fails confirming, render Link normally.
                null,
                is LinkConfirmationResult.Canceled,
                is LinkConfirmationResult.Failed -> loadLink()
                // if successfully confirmed the preselected payment, complete before rendering.
                is LinkConfirmationResult.Succeeded -> dismissWithResult(
                    LinkActivityResult.Completed(linkAccountUpdate = LinkAccountUpdate.Value(null))
                )
            }
        }
    }

    private suspend fun loadLink() {
        if (startWithVerificationDialog) {
            updateScreenState()
        } else {
            val attestationCheckResult = linkAttestationCheck.invoke()
            when (attestationCheckResult) {
                is LinkAttestationCheck.Result.AttestationFailed -> {
                    launchWebFlow?.invoke(linkConfiguration)
                }
                LinkAttestationCheck.Result.Successful -> {
                    updateScreenState()
                }
                is LinkAttestationCheck.Result.Error,
                is LinkAttestationCheck.Result.AccountError -> {
                    handleAccountError()
                }
            }
        }
    }

    /**
     * Attempts to confirm payment eagerly if the Link flow was launched with an authenticated user and a preselected
     * Link payment method.
     * @return The result of the confirmation attempt, or null if no confirmation was attempted.
     */
    private suspend fun confirmPreselectedLinkPaymentIfAvailable(): LinkConfirmationResult? {
        val selectedPayment = (linkLaunchMode as? LinkLaunchMode.Full)?.selectedPayment
        val paymentReadyForConfirmation = selectedPayment?.takeIf { it.readyForConfirmation() }
        val account = linkAccount ?: return null
        return paymentReadyForConfirmation?.let {
            linkConfirmationHandler.confirm(
                paymentDetails = it.details,
                cvc = it.collectedCvc,
                linkAccount = account,
            )
        }
    }

    private suspend fun updateScreenState() {
        val accountStatus = linkAccountManager.accountStatus.first()
        val linkAccount = linkAccountManager.linkAccount.value
        when (accountStatus) {
            AccountStatus.Verified,
            AccountStatus.SignedOut,
            AccountStatus.Error -> {
                _linkScreenState.value = buildFullScreenState()
            }
            AccountStatus.NeedsVerification,
            AccountStatus.VerificationStarted -> {
                if (linkAccount != null && startWithVerificationDialog) {
                    _linkScreenState.value = ScreenState.VerificationDialog(linkAccount)
                } else {
                    _linkScreenState.value = buildFullScreenState()
                }
            }
        }
    }

    private suspend fun buildFullScreenState(): ScreenState.FullScreen {
        val accountStatus = linkAccountManager.accountStatus.first()

        // We add a tiny delay, which gives the loading screen a chance to fully inflate.
        // Otherwise, we get a weird scaling animation when we display the first non-loading screen.
        delay(LINK_DEFAULT_ANIMATION_DELAY_MILLIS)

        return ScreenState.FullScreen(
            initialDestination = when (accountStatus) {
                AccountStatus.Verified -> {
                    if (linkAccount?.completedSignup == true) {
                        // We just completed signup, but haven't added a payment method yet.
                        LinkScreen.PaymentMethod
                    } else {
                        LinkScreen.Wallet
                    }
                }
                AccountStatus.NeedsVerification, AccountStatus.VerificationStarted -> {
                    LinkScreen.Verification
                }
                AccountStatus.SignedOut, AccountStatus.Error -> {
                    LinkScreen.SignUp
                }
            }
        )
    }

    private suspend fun handleAccountError() {
        linkAccountManager.logOut()
        linkAccountHolder.set(null)
        updateScreenState()
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
                    .publishableKeyProvider { args.publishableKey }
                    .stripeAccountIdProvider { args.stripeAccountId }
                    .paymentElementCallbackIdentifier(args.paymentElementCallbackIdentifier)
                    .savedStateHandle(handle)
                    .context(app)
                    .application(app)
                    .startWithVerificationDialog(args.startWithVerificationDialog)
                    .linkLaunchMode(args.launchMode)
                    .linkAccount(args.linkAccount)
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
