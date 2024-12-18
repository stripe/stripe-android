package com.stripe.android.link

import android.app.Application
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
import com.stripe.android.link.injection.DaggerNativeLinkComponent
import com.stripe.android.link.injection.NativeLinkComponent
import com.stripe.android.link.model.AccountStatus
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.link.ui.LinkAppBarState
import com.stripe.android.paymentsheet.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

internal class LinkActivityViewModel @Inject constructor(
    val activityRetainedComponent: NativeLinkComponent,
    private val linkAccountManager: LinkAccountManager
) : ViewModel(), DefaultLifecycleObserver {
    private val _linkState = MutableStateFlow(
        value = LinkAppBarState(
            navigationIcon = R.drawable.stripe_link_close,
            showHeader = true,
            showOverflowMenu = false,
            email = null,
        )
    )
    val linkState: StateFlow<LinkAppBarState> = _linkState

    val linkAccount: LinkAccount?
        get() = linkAccountManager.linkAccount.value

    var navController: NavHostController? = null
    var dismissWithResult: ((LinkActivityResult) -> Unit)? = null

    fun handleViewAction(action: LinkAction) {
        when (action) {
            LinkAction.BackPressed -> handleBackPressed()
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
        val navController = navController ?: return
        navController.navigate(screen.route) {
            if (clearStack) {
                popUpTo(navController.graph.startDestinationId) {
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
    }

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        viewModelScope.launch {
            val accountStatus = linkAccountManager.accountStatus.first()
            val screen = when (accountStatus) {
                AccountStatus.Verified -> LinkScreen.Wallet
                AccountStatus.NeedsVerification, AccountStatus.VerificationStarted -> LinkScreen.Verification
                AccountStatus.SignedOut, AccountStatus.Error -> LinkScreen.SignUp
            }
            navController?.navigate(screen.route) {
                launchSingleTop = true
            }
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
                    .context(app)
                    .build()
                    .viewModel
            }
        }
    }
}

internal class NoArgsException : IllegalArgumentException("NativeLinkArgs not found")
