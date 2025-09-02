package com.stripe.android.link

import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.stripe.android.link.injection.LinkControllerPresenterScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@LinkControllerPresenterScope
internal class LinkControllerCoordinator @Inject constructor(
    private val interactor: LinkControllerInteractor,
    private val lifecycleOwner: LifecycleOwner,
    activityResultRegistryOwner: ActivityResultRegistryOwner,
    linkActivityContract: NativeLinkActivityContract,
    private val selectedPaymentMethodCallback: LinkController.PresentPaymentMethodsCallback,
    private val authenticationCallback: LinkController.AuthenticationCallback,
    private val authorizeCallback: LinkController.AuthorizeCallback,
) {
    val linkActivityResultLauncher: ActivityResultLauncher<LinkActivityContract.Args>

    init {
        check(lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.INITIALIZED))

        linkActivityResultLauncher = activityResultRegistryOwner.activityResultRegistry.register(
            key = "LinkController_LinkActivityResultLauncher",
            contract = linkActivityContract,
        ) { result ->
            interactor.onLinkActivityResult(result)
        }

        lifecycleOwner.lifecycleScope.launch {
            lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    interactor.presentPaymentMethodsResultFlow
                        .collect(selectedPaymentMethodCallback::onPresentPaymentMethodsResult)
                }
                launch {
                    interactor.authenticationResultFlow
                        .collect(authenticationCallback::onAuthenticationResult)
                }
                launch {
                    interactor.authorizeResultFlow
                        .collect(authorizeCallback::onAuthorizeResult)
                }
            }
        }

        lifecycleOwner.lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onDestroy(owner: LifecycleOwner) {
                    linkActivityResultLauncher.unregister()
                }
            }
        )
    }
}
