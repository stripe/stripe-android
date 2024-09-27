package com.stripe.android.link

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavHostController

internal class LinkActivityViewModel : ViewModel() {
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

    internal class Factory : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return LinkActivityViewModel() as T
        }
    }
}
