package com.stripe.android.link

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavHostController

internal class LinkActivityViewModel : ViewModel() {
    lateinit var navController: NavHostController
    lateinit var dismissWithResult: (LinkActivityResult) -> Unit

    fun handleViewAction(action: LinkAction) {
        when (action) {
            LinkAction.BackPressed -> handleBackPressed()
        }
    }

    private fun handleBackPressed() {
        dismissWithResult(LinkActivityResult.Canceled())
    }

    internal class Factory : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return LinkActivityViewModel() as T
        }
    }
}
