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
import com.stripe.android.link.gate.LinkGate
import com.stripe.android.link.injection.DaggerNativeLinkComponent
import com.stripe.android.link.injection.NativeLinkComponent
import com.stripe.android.link.model.AccountStatus
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.link.ui.LinkAppBarState
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

    private val _linkScreenState = MutableStateFlow<State>(State.Loading)
    val linkScreenState: StateFlow<State> = _linkScreenState

    val linkAccount: LinkAccount?
        get() = linkAccountManager.linkAccount.value

    var navController: NavHostController? = null
    var dismissWithResult: ((LinkActivityResult) -> Unit)? = null
    var launchWebFlow: ((LinkConfiguration) -> Unit)? = null

    fun handleViewAction(action: LinkAction) {
        when (action) {
            LinkAction.BackPressed -> handleBackPressed()
        }
    }

    private fun moveToWeb() {
        launchWebFlow?.let { launcher ->
            navigate(LinkScreen.Loading, clearStack = true)
            launcher.invoke(activityRetainedComponent.configuration)
        }
    }

    private fun handleBackPressed() {
        navController?.let { navController ->
            if (!navController.popBackStack()) {
                dismissWithResult?.invoke(LinkActivityResult.Canceled())
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
            dismissWithResult?.invoke(LinkActivityResult.Canceled())
        }
    }

    fun unregisterActivity() {
        navController = null
        dismissWithResult = null
        launchWebFlow = null
    }

    fun onVerificationSucceeded() {
        _linkScreenState.value = State.Link
    }

    fun onDismissClicked() {
        dismissWithResult?.invoke(LinkActivityResult.Canceled())
    }

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        viewModelScope.launch {
            warmUpIntegrityManager().fold(
                onSuccess = {
                    navigateToInitialState()
                },
                onFailure = { error ->
                    moveToWeb()
                    errorReporter.report(
                        errorEvent = ErrorReporter.UnexpectedErrorEvent.LINK_NATIVE_FAILED_TO_PREPARE_INTEGRITY_MANAGER,
                        stripeException = LinkEventException(error)
                    )
                }
            )
        }
    }

    fun linkScreenScreenCreated() {
        viewModelScope.launch {
            navigateToLinkScreen()
        }
    }

    private suspend fun navigateToInitialState() {
        activityRetainedComponent.configuration.customerInfo.email?.let {
            linkAccountManager.lookupConsumer(it).getOrThrow()
        }
        val linkAccount = linkAccountManager.linkAccount.value
        if (activityRetainedComponent.eagerLaunch && linkAccount != null && _linkScreenState.value is State.Loading) {
            _linkScreenState.value = State.VerificationDialog(linkAccount)
            return
        }

        _linkScreenState.value = State.Link
        navigateToLinkScreen()
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

    private suspend fun warmUpIntegrityManager(): Result<Unit> {
        if (linkGate.useAttestationEndpoints.not()) return Result.success(Unit)
        return integrityRequestManager.prepare()
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
                    .eagerLaunch(args.use2faDialog)
                    .publishableKeyProvider { args.publishableKey }
                    .stripeAccountIdProvider { args.stripeAccountId }
                    .savedStateHandle(handle)
                    .context(app)
                    .application(app)
                    .build()
                    .viewModel
            }
        }
    }
}

internal sealed interface State {
    data class VerificationDialog(val linkAccount: LinkAccount) : State
    data object Link : State
    data object Loading : State
}

internal class NoArgsException : IllegalArgumentException("NativeLinkArgs not found")
