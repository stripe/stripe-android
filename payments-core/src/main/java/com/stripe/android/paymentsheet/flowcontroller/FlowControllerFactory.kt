package com.stripe.android.paymentsheet.flowcontroller

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.lifecycleScope
import com.stripe.android.StripePaymentController
import com.stripe.android.payments.DefaultReturnUrl
import com.stripe.android.payments.PaymentIntentFlowResultProcessor
import com.stripe.android.payments.SetupIntentFlowResultProcessor
import com.stripe.android.paymentsheet.DefaultGooglePayRepository
import com.stripe.android.paymentsheet.DefaultPrefsRepository
import com.stripe.android.paymentsheet.GooglePayRepository
import com.stripe.android.paymentsheet.PaymentOptionCallback
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResultCallback
import com.stripe.android.paymentsheet.analytics.DefaultEventReporter
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.analytics.SessionId
import com.stripe.android.paymentsheet.model.PaymentIntentClientSecret
import com.stripe.android.paymentsheet.model.PaymentOptionFactory
import com.stripe.android.paymentsheet.model.SetupIntentClientSecret
import com.stripe.android.view.AuthActivityStarter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first

internal class FlowControllerFactory(
    private val viewModelStoreOwner: ViewModelStoreOwner,
    private val lifecycleScope: CoroutineScope,
    private val appContext: Context,
    private val activityLauncherFactory: ActivityLauncherFactory,
    private val statusBarColor: () -> Int?,
    private val authHostSupplier: () -> AuthActivityStarter.Host,
    private val paymentOptionFactory: PaymentOptionFactory,
    private val paymentOptionCallback: PaymentOptionCallback,
    private val paymentResultCallback: PaymentSheetResultCallback
) {
    constructor(
        activity: ComponentActivity,
        paymentOptionCallback: PaymentOptionCallback,
        paymentResultCallback: PaymentSheetResultCallback
    ) : this(
        activity,
        activity.lifecycleScope,
        activity.applicationContext,
        ActivityLauncherFactory.ActivityHost(activity),
        { activity.window.statusBarColor },
        { AuthActivityStarter.Host.create(activity) },
        PaymentOptionFactory(activity.resources),
        paymentOptionCallback,
        paymentResultCallback
    )

    constructor(
        fragment: Fragment,
        paymentOptionCallback: PaymentOptionCallback,
        paymentResultCallback: PaymentSheetResultCallback
    ) : this(
        fragment,
        fragment.lifecycleScope,
        fragment.requireContext(),
        ActivityLauncherFactory.FragmentHost(fragment),
        { fragment.activity?.window?.statusBarColor },
        { AuthActivityStarter.Host.create(fragment) },
        PaymentOptionFactory(fragment.resources),
        paymentOptionCallback,
        paymentResultCallback
    )

    fun create(): PaymentSheet.FlowController {
        val sessionId = SessionId()

        val paymentControllerFactory =
            PaymentControllerFactory { publishableKeyProvider, stripeRepository, paymentRelayLauncher,
                paymentBrowserAuthLauncher, stripe3ds2ChallengeLauncher ->

                StripePaymentController(
                    appContext,
                    publishableKeyProvider,
                    stripeRepository,
                    enableLogging = true,
                    paymentRelayLauncher = paymentRelayLauncher,
                    paymentBrowserAuthLauncher = paymentBrowserAuthLauncher,
                    stripe3ds2ChallengeLauncher = stripe3ds2ChallengeLauncher
                )
            }

        val isGooglePayReadySupplier: suspend (PaymentSheet.GooglePayConfiguration.Environment?) -> Boolean =
            { environment ->
                val googlePayRepository = environment?.let {
                    DefaultGooglePayRepository(
                        appContext,
                        it
                    )
                } ?: GooglePayRepository.Disabled
                googlePayRepository.isReady().first()
            }

        val prefsRepositoryFactory = { customerId: String, isGooglePayReady: Boolean ->
            DefaultPrefsRepository(
                appContext,
                customerId,
                { isGooglePayReady },
                Dispatchers.IO
            )
        }

        return DefaultFlowController(
            appContext = appContext,
            viewModelStoreOwner = viewModelStoreOwner,
            lifecycleScope = lifecycleScope,
            activityLauncherFactory = activityLauncherFactory,
            statusBarColor = statusBarColor,
            authHostSupplier = authHostSupplier,
            paymentOptionFactory = paymentOptionFactory,
            flowControllerInitializer = DefaultFlowControllerInitializer(
                prefsRepositoryFactory = prefsRepositoryFactory,
                isGooglePayReadySupplier = isGooglePayReadySupplier,
                workContext = Dispatchers.IO
            ),
            paymentControllerFactory = paymentControllerFactory,
            paymentFlowResultProcessorFactory = { clientSecret, publishableKeyProvider, stripeApiRepository ->
                when (clientSecret) {
                    is PaymentIntentClientSecret -> PaymentIntentFlowResultProcessor(
                        appContext,
                        publishableKeyProvider,
                        stripeApiRepository,
                        enableLogging = false,
                        Dispatchers.IO
                    )
                    is SetupIntentClientSecret -> SetupIntentFlowResultProcessor(
                        appContext,
                        publishableKeyProvider,
                        stripeApiRepository,
                        enableLogging = false,
                        Dispatchers.IO
                    )
                }
            },
            eventReporter = DefaultEventReporter(
                mode = EventReporter.Mode.Custom,
                sessionId,
                appContext
            ),
            sessionId = sessionId,
            defaultReturnUrl = DefaultReturnUrl.create(appContext),
            paymentOptionCallback = paymentOptionCallback,
            paymentResultCallback = paymentResultCallback
        )
    }
}
