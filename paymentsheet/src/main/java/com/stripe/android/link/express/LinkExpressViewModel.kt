package com.stripe.android.link.express

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.stripe.android.link.LinkAccountUpdate
import com.stripe.android.link.NoArgsException
import com.stripe.android.link.express.LinkExpressActivity.Companion.getArgs
import com.stripe.android.link.injection.DaggerNativeLinkComponent
import com.stripe.android.link.injection.NativeLinkComponent
import com.stripe.android.link.model.LinkAccount
import javax.inject.Inject

internal class LinkExpressViewModel @Inject constructor(
    val activityRetainedComponent: NativeLinkComponent,
    val linkAccount: LinkAccount
) : ViewModel() {

    var dismissWithResult: ((LinkExpressResult) -> Unit)? = null

    fun onVerificationSucceeded(linkAccount: LinkAccount) {
        dismissWithResult?.invoke(
            LinkExpressResult.Authenticated(
                linkAccountUpdate = LinkAccountUpdate.Value(linkAccount)
            )
        )
    }

    fun onChangeEmailClicked() = Unit

    fun onDismissClicked() {
        dismissWithResult?.invoke(
            LinkExpressResult.Authenticated(
                linkAccountUpdate = LinkAccountUpdate.None
            )
        )
    }

    companion object {
        fun factory(
            savedStateHandle: SavedStateHandle? = null
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val handle: SavedStateHandle = savedStateHandle ?: createSavedStateHandle()
                val app = this[APPLICATION_KEY] as Application
                val args: LinkExpressArgs = getArgs(handle) ?: throw NoArgsException()

                DaggerNativeLinkComponent
                    .builder()
                    .configuration(args.configuration)
                    .publishableKeyProvider { args.publishableKey }
                    .stripeAccountIdProvider { args.stripeAccountId }
                    .savedStateHandle(handle)
                    .context(app)
                    .application(app)
                    .linkAccount(args.linkAccount)
                    .build()
                    .expressViewModel ?: throw IllegalStateException("Unable to create LinkExpressViewModel")
            }
        }
    }
}
