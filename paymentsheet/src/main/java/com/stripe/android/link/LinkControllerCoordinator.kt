package com.stripe.android.link

import android.app.Activity
import android.app.Application
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.stripe.android.core.utils.StatusBarCompat
import com.stripe.android.link.injection.LinkControllerPresenterScope
import com.stripe.android.model.ConfirmSetupIntentParams
import com.stripe.android.model.MandateDataParams
import com.stripe.android.payments.paymentlauncher.PaymentLauncher
import com.stripe.android.payments.paymentlauncher.PaymentLauncherFactory
import kotlinx.coroutines.launch
import javax.inject.Inject

@LinkControllerPresenterScope
internal class LinkControllerCoordinator @Inject constructor(
    private val application: Application,
    activity: Activity,
    private val interactor: LinkControllerInteractor,
    private val lifecycleOwner: LifecycleOwner,
    activityResultRegistryOwner: ActivityResultRegistryOwner,
    linkActivityContract: NativeLinkActivityContract,
    private val selectedPaymentMethodCallback: LinkController.PresentPaymentMethodsCallback,
    private val authenticationCallback: LinkController.AuthenticationCallback,
    private val authorizeCallback: LinkController.AuthorizeCallback,
    private val presentCallback: LinkController.PresentCallback,
    private val confirmSetupIntentCallback: LinkController.ConfirmSetupIntentCallback,
) {
    val linkActivityResultLauncher: ActivityResultLauncher<LinkActivityContract.Args>
    private val paymentLauncher: PaymentLauncher

    init {
        check(lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.INITIALIZED))

        linkActivityResultLauncher = activityResultRegistryOwner.activityResultRegistry.register(
            key = "LinkController_LinkActivityResultLauncher",
            contract = linkActivityContract,
        ) { result ->
            interactor.onLinkActivityResult(result)
        }

        paymentLauncher = PaymentLauncherFactory(
            activityResultRegistryOwner = activityResultRegistryOwner,
            lifecycleOwner = lifecycleOwner,
            statusBarColor = StatusBarCompat.color(activity),
            callback = PaymentLauncher.InternalPaymentResultCallback { result ->
                interactor.onSetupIntentConfirmationResult(result)
            }
        ).create(application)

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
                launch {
                    interactor.presentResultFlow
                        .collect(presentCallback::onPresentResult)
                }
                launch {
                    interactor.confirmSetupIntentResultFlow
                        .collect(confirmSetupIntentCallback::onConfirmSetupIntentResult)
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

    fun confirmSetupIntent(clientSecret: String) {
        lifecycleOwner.lifecycleScope.launch {
            val paymentMethod = interactor.lastCreatedPaymentMethod
            if (paymentMethod?.id == null) {
                interactor.emitConfirmSetupIntentResult(
                    LinkController.ConfirmSetupIntentResult.Failed(
                        IllegalStateException("No payment method available. Call present() first.")
                    )
                )
                return@launch
            }

            paymentLauncher.confirm(
                ConfirmSetupIntentParams.create(
                    paymentMethodId = paymentMethod.id,
                    clientSecret = clientSecret,
                    mandateData = MandateDataParams(MandateDataParams.Type.Online.DEFAULT),
                )
            )
        }
    }
}
