@file:OptIn(LinkControllerPreview::class)

package com.stripe.android.identity.networked

import androidx.activity.ComponentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import com.stripe.android.identity.BuildConfig
import com.stripe.android.identity.IdentityVerificationSheet
import com.stripe.android.link.LinkController
import com.stripe.android.link.LinkControllerPreview
import com.stripe.android.networking.RequestSurface
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first

/**
 * Owns the Identity activity's [LinkController]. Link's own screens need a [LinkController.Presenter], which
 * registers for activity results and so must be created in the activity's onCreate, before the
 * VerificationPage that enables Networked Identity has loaded.
 */
internal class NetworkedIdentityLinkHost(
    val linkController: LinkController,
) : ViewModel() {
    private val authenticationResults = MutableSharedFlow<LinkController.AuthenticationResult>(extraBufferCapacity = 1)
    private var presenter: LinkController.Presenter? = null

    /** Call from the activity's onCreate. Results reach whichever activity instance is current. */
    fun attach(activity: ComponentActivity) {
        val created = linkController.createPresenter(
            activity = activity,
            presentPaymentMethodsCallback = {},
            authenticationCallback = { authenticationResults.tryEmit(it) },
            authorizeCallback = {},
        )
        presenter = created
        activity.lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onDestroy(owner: LifecycleOwner) {
                    if (presenter === created) presenter = null
                }
            }
        )
    }

    /** Presents Link's sign-in and sign-up screens. Returns null when no activity is attached. */
    suspend fun authenticate(
        email: String?,
        content: LinkController.AuthenticationContent,
    ): LinkController.AuthenticationResult? {
        val presenter = presenter ?: return null
        return coroutineScope {
            // Subscribe first: an already verified account completes without presenting anything.
            val result = async(start = CoroutineStart.UNDISPATCHED) { authenticationResults.first() }
            presenter.authenticate(email = email, phoneNumber = null, content = content)
            result.await()
        }
    }

    internal object Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            val application = checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY])
            return NetworkedIdentityLinkHost(
                LinkController.create(
                    application = application,
                    savedStateHandle = extras.createSavedStateHandle(),
                    requestSurface = RequestSurface.Identity,
                )
            ) as T
        }
    }

    internal companion object {
        const val KEY = "NetworkedIdentityLinkHost"

        /** Host options stand in for the VerificationPage fields until the backend returns them. */
        fun isEnabled(options: IdentityVerificationSheet.Configuration.NetworkedIdentityOptions?): Boolean =
            options != null || BuildConfig.NETWORKED_IDENTITY_PREVIEW

        fun of(activity: ComponentActivity): NetworkedIdentityLinkHost =
            ViewModelProvider(activity, Factory)[KEY, NetworkedIdentityLinkHost::class.java]
    }
}
