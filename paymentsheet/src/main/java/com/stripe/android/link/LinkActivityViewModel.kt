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
import androidx.navigation.NavHostController
import com.stripe.android.link.LinkActivity.Companion.getArgs
import com.stripe.android.link.account.LinkAccountManager
import com.stripe.android.link.account.LinkAuth
import com.stripe.android.link.account.LinkAuthResult
import com.stripe.android.link.account.linkAccountUpdate
import com.stripe.android.link.gate.LinkGate
import com.stripe.android.link.injection.DaggerNativeLinkComponent
import com.stripe.android.link.injection.NativeLinkComponent
import com.stripe.android.link.model.AccountStatus
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.link.ui.LinkAppBarState
import com.stripe.android.model.EmailSource
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.attestation.IntegrityRequestManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

internal class LinkActivityViewModel @Inject constructor(
    val activityRetainedComponent: NativeLinkComponent,
    confirmationHandlerFactory: ConfirmationHandler.Factory,
    private val linkAccountManager: LinkAccountManager,
    val eventReporter: EventReporter,
    private val integrityRequestManager: IntegrityRequestManager,
    private val linkGate: LinkGate,
    private val errorReporter: ErrorReporter,
    private val linkAuth: LinkAuth,
    private val linkConfiguration: LinkConfiguration,
    private val startWithVerificationDialog: Boolean
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

    private val _linkScreenState = MutableStateFlow<ScreenState>(ScreenState.Loading)
    val linkScreenState: StateFlow<ScreenState> = _linkScreenState

    val linkAccount: LinkAccount?
        get() = linkAccountManager.linkAccount.value

    var navController: NavHostController? = null
        set(value) {
            listenToNavController(value)
            field = value
        }
    var dismissWithResult: ((LinkActivityResult) -> Unit)? = null
    var launchWebFlow: ((LinkConfiguration) -> Unit)? = null

    private fun listenToNavController(navController: NavHostController?) {
        viewModelScope.launch {
            navController?.currentBackStackEntryFlow?.first { entry ->
                entry.destination.route == LinkScreen.Loading.route
            }
            linkScreenScreenCreated()
        }
    }

    fun handleViewAction(action: LinkAction) {
        when (action) {
            LinkAction.BackPressed -> handleBackPressed()
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

    fun unregisterActivity() {
        navController = null
        dismissWithResult = null
        launchWebFlow = null
    }

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        viewModelScope.launch {
            performAttestationCheck().fold(
                onSuccess = {
                    updateScreenState()
                },
                onFailure = {
                    moveToWeb()
                }
            )
        }
    }

    fun linkScreenScreenCreated() {
        viewModelScope.launch {
            navigateToLinkScreen()
        }
    }

    private suspend fun performAttestationCheck(): Result<Unit> {
        if (linkGate.useAttestationEndpoints.not()) return Result.success(Unit)
        return integrityRequestManager.prepare()
            .onFailure { error ->
                errorReporter.report(
                    errorEvent = ErrorReporter.UnexpectedErrorEvent.LINK_NATIVE_FAILED_TO_PREPARE_INTEGRITY_MANAGER,
                    stripeException = LinkEventException(error)
                )
            }
            .mapCatching {
                when (val lookupResult = lookupUser()) {
                    is LinkAuthResult.AttestationFailed -> {
                        errorReporter.report(
                            errorEvent = ErrorReporter.UnexpectedErrorEvent.LINK_NATIVE_FAILED_TO_ATTEST_REQUEST,
                            stripeException = LinkEventException(lookupResult.throwable)
                        )
                        throw lookupResult.throwable
                    }
                    is LinkAuthResult.Error,
                    LinkAuthResult.NoLinkAccountFound,
                    is LinkAuthResult.Success,
                    null -> Unit
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
        val screen = when (accountStatus) {
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
        navigate(screen, clearStack = true, launchSingleTop = true)
    }

    private suspend fun lookupUser(): LinkAuthResult? {
        val customerEmail = linkAccountManager.linkAccount.value?.email
            ?: linkConfiguration.customerInfo.email
            ?: return null

        return linkAuth.lookUp(
            email = customerEmail,
            emailSource = EmailSource.CUSTOMER_OBJECT,
            startSession = false
        )
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
                    .savedStateHandle(handle)
                    .context(app)
                    .application(app)
                    .linkAccount(args.linkAccount)
                    .startWithVerificationDialog(args.startWithVerificationDialog)
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
