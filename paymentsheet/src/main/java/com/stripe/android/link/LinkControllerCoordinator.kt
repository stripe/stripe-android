package com.stripe.android.link

import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.stripe.android.link.injection.LinkControllerScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@LinkControllerScope
internal class LinkControllerCoordinator @Inject constructor(
    private val viewModel: LinkControllerViewModel,
    private val lifecycleOwner: LifecycleOwner,
    activityResultRegistryOwner: ActivityResultRegistryOwner,
    linkActivityContract: NativeLinkActivityContract,
    private val selectedPaymentMethodCallback: LinkController.PresentPaymentMethodsCallback,
    private val lookupConsumerCallback: LinkController.LookupConsumerCallback,
    private val createPaymentMethodCallback: LinkController.CreatePaymentMethodCallback,
    private val authenticationCallback: LinkController.AuthenticationCallback,
) {
    val linkActivityResultLauncher: ActivityResultLauncher<LinkActivityContract.Args>

    init {
        check(lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.CREATED))

        linkActivityResultLauncher = activityResultRegistryOwner.activityResultRegistry.register(
            key = "LinkController_LinkActivityResultLauncher",
            contract = linkActivityContract,
        ) { result ->
            viewModel.onLinkActivityResult(result)
        }

        lifecycleOwner.lifecycleScope.launch {
            lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.presentPaymentMethodsResultFlow
                        .collect(selectedPaymentMethodCallback::onPresentPaymentMethodsResult)
                }
                launch {
                    viewModel.lookupConsumerResultFlow
                        .collect(lookupConsumerCallback::onLookupConsumerResult)
                }
                launch {
                    viewModel.createPaymentMethodResultFlow
                        .collect(createPaymentMethodCallback::onCreatePaymentMethodResult)
                }
                launch {
                    viewModel.authenticationResultFlow
                        .collect(authenticationCallback::onAuthenticationResult)
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
