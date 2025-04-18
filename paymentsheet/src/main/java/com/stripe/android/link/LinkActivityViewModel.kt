package com.stripe.android.link

import android.app.Application
import androidx.activity.result.ActivityResultCaller
import androidx.annotation.VisibleForTesting
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
import androidx.navigation.NavHostController
import com.stripe.android.link.LinkActivity.Companion.getArgs
import com.stripe.android.link.account.LinkAccountHolder
import com.stripe.android.link.account.LinkAccountManager
import com.stripe.android.link.account.linkAccountUpdate
import com.stripe.android.link.attestation.LinkAttestationCheck
import com.stripe.android.link.injection.DaggerNativeLinkComponent
import com.stripe.android.link.injection.NativeLinkComponent
import com.stripe.android.link.model.AccountStatus
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.link.ui.LinkAppBarState
import com.stripe.android.link.ui.signup.SignUpViewModel
import com.stripe.android.link.utils.LINK_DEFAULT_ANIMATION_DELAY_MILLIS
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentsheet.analytics.EventReporter
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@SuppressWarnings("TooManyFunctions")
internal class LinkActivityViewModel @Inject constructor(
    val activityRetainedComponent: NativeLinkComponent,
    confirmationHandlerFactory: ConfirmationHandler.Factory,
    private val linkAccountManager: LinkAccountManager,
    private val linkAccountHolder: LinkAccountHolder,
    val eventReporter: EventReporter,
    private val linkConfiguration: LinkConfiguration,
    private val linkAttestationCheck: LinkAttestationCheck,
    val savedStateHandle: SavedStateHandle,
    private val startWithVerificationDialog: Boolean
) : ViewModel(), DefaultLifecycleObserver {
    val confirmationHandler = confirmationHandlerFactory.create(viewModelScope)

    private val _linkAppBarState = MutableStateFlow(LinkAppBarState.initial())
    val linkAppBarState: StateFlow<LinkAppBarState> = _linkAppBarState.asStateFlow()

    private val _linkScreenState = MutableStateFlow<ScreenState>(ScreenState.Loading)
    val linkScreenState: StateFlow<ScreenState> = _linkScreenState

    val linkAccount: LinkAccount?
        get() = linkAccountManager.linkAccount.value

    @VisibleForTesting
    internal var navListenerJob: Job? = null
    var navController: NavHostController? = null
        set(value) {
            listenToNavController(value)
            field = value
        }
    var dismissWithResult: ((LinkActivityResult) -> Unit)? = null
    var launchWebFlow: ((LinkConfiguration) -> Unit)? = null

    fun handleViewAction(action: LinkAction) {
        when (action) {
            LinkAction.BackPressed -> handleBackPressed()
            LinkAction.LogoutClicked -> handleLogoutClicked()
        }
    }

    fun onVerificationSucceeded() {
        _linkScreenState.value = ScreenState.FullScreen
    }

    fun onDismissVerificationClicked() {
        dismissWithResult?.invoke(
            LinkActivityResult.Canceled(
                linkAccountUpdate = linkAccountManager.linkAccountUpdate
            )
        )
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun handleLogoutClicked() {
        GlobalScope.launch {
            linkAccountManager.logOut()
        }
        dismissWithResult?.invoke(
            LinkActivityResult.Canceled(
                reason = LinkActivityResult.Canceled.Reason.LoggedOut,
                linkAccountUpdate = LinkAccountUpdate.Value(null)
            )
        )
    }

    private fun listenToNavController(navController: NavHostController?) {
        cancelNavListenerJob()
        navController ?: return
        navListenerJob = viewModelScope.launch {
            navController.currentBackStackEntryFlow.collectLatest { entry ->
                val previousEntry = navController.previousBackStackEntry?.destination?.route
                val route = entry.destination.route

                _linkAppBarState.update {
                    LinkAppBarState.create(
                        route = route,
                        previousEntryRoute = previousEntry,
                        email = linkAccountManager.linkAccount.value?.email,
                        consumerIsSigningUp = linkAccount?.completedSignup == true,
                    )
                }
            }
        }
    }

    fun moveToWeb() {
        launchWebFlow?.let { launcher ->
            navigate(LinkScreen.Loading, clearStack = true)
            launcher.invoke(linkConfiguration)
        }
    }

    private fun handleBackPressed() {
        navController?.let { navController ->
            if (!navController.popBackStack()) {
                dismissWithResult?.invoke(
                    LinkActivityResult.Canceled(
                        linkAccountUpdate = linkAccountManager.linkAccountUpdate
                    )
                )
            }
        }
    }

    fun navigate(screen: LinkScreen, clearStack: Boolean) {
        navigate(screen, clearStack, launchSingleTop = false)
    }

    fun registerActivityForConfirmation(
        activityResultCaller: ActivityResultCaller,
        lifecycleOwner: LifecycleOwner,
    ) {
        confirmationHandler.register(activityResultCaller, lifecycleOwner)
    }

    private fun navigate(screen: LinkScreen, clearStack: Boolean, launchSingleTop: Boolean) {
        val navController = navController ?: return
        navController.navigate(screen.route) {
            this.launchSingleTop = launchSingleTop
            if (clearStack) {
                popUpTo(navController.graph.id) {
                    inclusive = true
                }
            }
        }
    }

    fun goBack() {
        if (navController?.popBackStack() == false) {
            dismissWithResult?.invoke(
                LinkActivityResult.Canceled(
                    linkAccountUpdate = linkAccountManager.linkAccountUpdate
                )
            )
        }
    }

    fun changeEmail() {
        savedStateHandle[SignUpViewModel.USE_LINK_CONFIGURATION_CUSTOMER_INFO] = false
        navigate(LinkScreen.SignUp, clearStack = true)
    }

    fun unregisterActivity() {
        cancelNavListenerJob()
        navController = null
        dismissWithResult = null
        launchWebFlow = null
    }

    private fun cancelNavListenerJob() {
        navListenerJob?.cancel()
        navListenerJob = null
    }

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        viewModelScope.launch {
            if (startWithVerificationDialog) return@launch updateScreenState()
            val attestationCheckResult = linkAttestationCheck.invoke()
            when (attestationCheckResult) {
                is LinkAttestationCheck.Result.AttestationFailed -> {
                    moveToWeb()
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

    fun linkScreenCreated() {
        viewModelScope.launch {
            val currentRoute = navController?.currentBackStackEntry?.destination?.route
            if (currentRoute == null || currentRoute == LinkScreen.Loading.route) {
                navigateToLinkScreen()
            }
        }
    }

    private suspend fun updateScreenState() {
        val accountStatus = linkAccountManager.accountStatus.first()
        val linkAccount = linkAccountManager.linkAccount.value
        when (accountStatus) {
            AccountStatus.Verified,
            AccountStatus.SignedOut,
            AccountStatus.Error -> {
                _linkScreenState.value = ScreenState.FullScreen
            }
            AccountStatus.NeedsVerification,
            AccountStatus.VerificationStarted -> {
                if (linkAccount != null && startWithVerificationDialog) {
                    _linkScreenState.value = ScreenState.VerificationDialog(linkAccount)
                } else {
                    _linkScreenState.value = ScreenState.FullScreen
                }
            }
        }
    }

    private suspend fun navigateToLinkScreen() {
        val accountStatus = linkAccountManager.accountStatus.first()

        // We add a tiny delay, which gives the loading screen a chance to fully inflate.
        // Otherwise, we get a weird scaling animation when we display the first non-loading screen.
        delay(LINK_DEFAULT_ANIMATION_DELAY_MILLIS)

        val screen = when (accountStatus) {
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
        navigate(screen, clearStack = true, launchSingleTop = true)
    }

    private suspend fun handleAccountError() {
        linkAccountManager.logOut()
        linkAccountHolder.set(null)
        updateScreenState()
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
                    .linkAccount(args.linkAccount)
                    .build()
                    .viewModel
            }
        }
    }
}

internal sealed interface ScreenState {
    data class VerificationDialog(val linkAccount: LinkAccount) : ScreenState
    data object FullScreen : ScreenState
    data object Loading : ScreenState
}

internal class NoArgsException : IllegalArgumentException("NativeLinkArgs not found")
