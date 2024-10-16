package com.stripe.android.link

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavHostController
import com.stripe.android.link.LinkActivity.Companion.getArgs
import com.stripe.android.link.injection.DaggerNativeLinkComponent
import com.stripe.android.link.injection.NativeLinkComponent
import javax.inject.Inject

internal class LinkActivityViewModel @Inject constructor(
    val activityRetainedComponent: NativeLinkComponent
) : ViewModel() {
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

    fun unregisterActivity() {
        navController = null
        dismissWithResult = null
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val savedStateHandle: SavedStateHandle = createSavedStateHandle()
                val app = this[APPLICATION_KEY] as Application
                val args: NativeLinkArgs =
                    requireNotNull(getArgs(savedStateHandle))

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
