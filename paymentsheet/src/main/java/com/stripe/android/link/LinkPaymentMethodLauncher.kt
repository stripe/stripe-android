package com.stripe.android.link

import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.ViewModelProvider

interface LinkPaymentMethodLauncher {
    fun present(email: String?)

    companion object {
        fun create(activity: ComponentActivity): LinkPaymentMethodLauncher {
            val viewModelProvider = ViewModelProvider(
                owner = activity,
                factory = LinkPaymentMethodLauncherViewModel.Factory()
            )
            val viewModel = viewModelProvider[LinkPaymentMethodLauncherViewModel::class.java]
            return RealLinkPaymentMethodLauncher(activity, viewModel)
        }
    }
}

internal class RealLinkPaymentMethodLauncher(
    private val activity: ComponentActivity,
    private val viewModel: LinkPaymentMethodLauncherViewModel
) : LinkPaymentMethodLauncher {

    private var linkActivityResultLauncher: ActivityResultLauncher<LinkActivityContract.Args> =
        activity.registerForActivityResult(viewModel.linkActivityContract) { result ->
            viewModel.onResult(result)
        }

    override fun present(email: String?) {
        viewModel.onPresent(linkActivityResultLauncher, email)
    }
}
