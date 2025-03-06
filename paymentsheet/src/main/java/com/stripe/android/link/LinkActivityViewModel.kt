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
import com.stripe.android.link.LinkActivity.Companion.getArgs
import com.stripe.android.link.account.LinkAccountHolder
import com.stripe.android.link.account.LinkAccountManager
import com.stripe.android.link.account.linkAccountUpdate
import com.stripe.android.link.attestation.LinkAttestationCheck
import com.stripe.android.link.injection.DaggerNativeLinkComponent
import com.stripe.android.link.injection.NativeLinkComponent
import com.stripe.android.link.model.AccountStatus
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.link.navigation.LinkNavigationManager
import com.stripe.android.link.ui.LinkAppBarState
import com.stripe.android.link.ui.signup.SignUpViewModel
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.uicore.navigation.PopUpToBehavior
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
    private val savedStateHandle: SavedStateHandle,
    private val startWithVerificationDialog: Boolean,
    val navigationManager: LinkNavigationManager,
) : ViewModel(), DefaultLifecycleObserver {
    val confirmationHandler = confirmationHandlerFactory.create(viewModelScope)
    private val _linkAppBarState = MutableStateFlow(
        value = LinkAppBarState(
            navigationIcon = R.drawable.stripe_link_close,
            showHeader = true,
            showOverflowMenu = false,
            email = null,
        )
    )
    val linkAppBarState: StateFlow<LinkAppBarState> = _linkAppBarState

    val linkAccount: LinkAccount?
        get() = linkAccountManager.linkAccount.value

    var dismissWithResult: ((LinkActivityResult) -> Unit)? = null
    var launchWebFlow: ((LinkConfiguration) -> Unit)? = null

    init {
        viewModelScope.launch {
            navigationManager.linkScreenState.first { it == ScreenState.FullScreen }
            delay(500)
            navigateToFirstScreen()
        }
    }

    fun handleViewAction(action: LinkAction) {
        when (action) {
            LinkAction.BackPressed -> handleBackPressed()
            LinkAction.LogoutClicked -> handleLogoutClicked()
        }
    }

    fun handleBackStackChanged(backStackEntry: NavBackStackEntry?) {
        val route = backStackEntry?.destination?.route ?: return

        _linkAppBarState.update {
            it.copy(
                showHeader = route in showHeaderRoutes,
                showOverflowMenu = route == LinkScreen.Wallet.route,
                // TODO: The back button should be handled differently
                navigationIcon = if (route == LinkScreen.PaymentMethod.route) {
                    R.drawable.stripe_link_back
                } else {
                    R.drawable.stripe_link_close
                },
                email = linkAccountManager.linkAccount.value?.email?.takeIf {
                    route == LinkScreen.Wallet.route
                }
            )
        }
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

    fun moveToWeb() {
        launchWebFlow?.let { launcher ->
            navigate(LinkScreen.Loading, clearStack = true)
            launcher.invoke(linkConfiguration)
        }
    }

    private fun handleBackPressed() {
        navigationManager.tryNavigateBack()
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
        navigationManager.tryNavigateTo(
            route = screen.route,
            popUpTo = if (clearStack) {
                // TODO
                PopUpToBehavior.Current(inclusive = true)
            } else {
                null
            },
            isSingleTop = launchSingleTop,
        )
    }

    fun changeEmail() {
        savedStateHandle[SignUpViewModel.USE_LINK_CONFIGURATION_CUSTOMER_INFO] = false
        navigate(LinkScreen.SignUp, clearStack = true)
    }

    fun unregisterActivity() {
        dismissWithResult = null
        launchWebFlow = null
    }

    // TODO: This… no…
    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        viewModelScope.launch {
            if (startWithVerificationDialog) {
                updateScreenState()
                return@launch
            }
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

    private suspend fun navigateToFirstScreen() {
        val accountStatus = retrieveAccountStatus()
        val firstScreen = accountStatus.determineFirstScreen()
        navigate(firstScreen, clearStack = true, launchSingleTop = true)
    }

    private suspend fun updateScreenState() {
        val accountStatus = retrieveAccountStatus()
        val linkAccount = linkAccountManager.linkAccount.value
        val screenState = accountStatus.determineScreenState(linkAccount)
        navigationManager.updateScreenState(screenState)
    }

    private suspend fun retrieveAccountStatus(): AccountStatus {
        return linkAccountManager.accountStatus.first()
    }

    private fun AccountStatus.determineScreenState(linkAccount: LinkAccount?): ScreenState {
        return when (this) {
            AccountStatus.Verified,
            AccountStatus.SignedOut,
            AccountStatus.Error -> {
                ScreenState.FullScreen
            }
            AccountStatus.NeedsVerification,
            AccountStatus.VerificationStarted -> {
                if (linkAccount != null && startWithVerificationDialog) {
                    ScreenState.VerificationDialog(linkAccount)
                } else {
                    ScreenState.FullScreen
                }
            }
        }
    }

    private fun AccountStatus.determineFirstScreen(): LinkScreen {
        return when (this) {
            AccountStatus.Verified -> {
                LinkScreen.Wallet
            }
            AccountStatus.NeedsVerification, AccountStatus.VerificationStarted -> {
                LinkScreen.Verification
            }
            AccountStatus.SignedOut, AccountStatus.Error -> {
                LinkScreen.SignUp
            }
        }
    }

    private suspend fun handleAccountError() {
        linkAccountManager.logOut()
        linkAccountHolder.set(null)
        updateScreenState()
    }

    fun onBackPressed() {
        dismissWithResult?.invoke(
            LinkActivityResult.Canceled(
                linkAccountUpdate = linkAccountManager.linkAccountUpdate
            )
        )
    }

    companion object {
        private val showHeaderRoutes = setOf(
            LinkScreen.Wallet.route,
            LinkScreen.SignUp.route,
            LinkScreen.Verification.route
        )

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
