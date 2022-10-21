package com.stripe.android.elements

import android.content.Context
import androidx.activity.result.ActivityResultCaller
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.injection.IOContext
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.ConfirmSetupIntentParams
import com.stripe.android.payments.paymentlauncher.PaymentLauncherContract
import com.stripe.android.payments.paymentlauncher.PaymentResult
import com.stripe.android.payments.paymentlauncher.StripePaymentLauncher
import com.stripe.android.payments.paymentlauncher.StripePaymentLauncherAssistedFactory
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.extensions.registerPollingAuthenticator
import com.stripe.android.paymentsheet.extensions.unregisterPollingAuthenticator
import com.stripe.android.paymentsheet.model.ClientSecret
import com.stripe.android.paymentsheet.model.ConfirmStripeIntentParamsFactory
import com.stripe.android.paymentsheet.model.PaymentIntentClientSecret
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.StripeIntentValidator
import com.stripe.android.paymentsheet.model.getPMsToAdd
import com.stripe.android.paymentsheet.repositories.StripeIntentRepository
import com.stripe.android.paymentsheet.repositories.initializeRepositoryAndGetStripeIntent
import com.stripe.android.ui.core.address.AddressRepository
import com.stripe.android.ui.core.forms.resources.LpmRepository
import com.stripe.android.ui.core.forms.resources.ResourceRepository
import com.stripe.android.ui.core.injection.NonFallbackInjector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Provider
import kotlin.coroutines.CoroutineContext

internal class DefaultPaymentElementController @Inject internal constructor(
    private val appContext: Context,
    @IOContext private val workContext: CoroutineContext,
    private val lifecycleScope: CoroutineScope,
    lifecycleOwner: LifecycleOwner,
    private val activityResultCaller: ActivityResultCaller,
    private val paymentResultCallback: PaymentElementResultCallback,
    private val lpmResourceRepository: ResourceRepository<LpmRepository>,
    private val addressResourceRepository: ResourceRepository<AddressRepository>,
    private val stripeIntentRepository: StripeIntentRepository,
    private val stripeIntentValidator: StripeIntentValidator,
    private val lazyPaymentConfiguration: Provider<PaymentConfiguration>,
    private val paymentLauncherFactory: StripePaymentLauncherAssistedFactory,
    private val eventReporter: EventReporter
) : PaymentElementController {
    lateinit var injector: NonFallbackInjector
    private var paymentLauncher: StripePaymentLauncher? = null


    private var paymentSheetConfig: PaymentSheet.Configuration? = null
    lateinit var clientSecret: ClientSecret

    init {
        lifecycleOwner.lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onCreate(owner: LifecycleOwner) {
                    paymentLauncher = paymentLauncherFactory.create(
                        { lazyPaymentConfiguration.get().publishableKey },
                        { lazyPaymentConfiguration.get().stripeAccountId },
                        activityResultCaller.registerForActivityResult(
                            PaymentLauncherContract(),
                            ::onPaymentResult
                        )
                    ).also {
                        it.registerPollingAuthenticator()
                    }
                }

                override fun onDestroy(owner: LifecycleOwner) {
                    paymentLauncher?.unregisterPollingAuthenticator()
                    paymentLauncher = null
                }
            }
        )
    }

    override fun configureWithPaymentIntent(
        paymentIntentClientSecret: String,
        paymentSheetConfig: PaymentSheet.Configuration?
    ) {
        this.paymentSheetConfig = paymentSheetConfig
        this.clientSecret = PaymentIntentClientSecret(paymentIntentClientSecret)
    }

    override suspend fun getViewModelFactory() = withContext(workContext) {
        lpmResourceRepository.waitUntilLoaded()
        addressResourceRepository.waitUntilLoaded()

        stripeIntentValidator.requireValid(
            initializeRepositoryAndGetStripeIntent(
                lpmResourceRepository,
                stripeIntentRepository,
                clientSecret,
                eventReporter
            )
        )
    }.let { stripeIntent ->
        val config = PaymentElementController.Config(
            paymentSheetConfig = paymentSheetConfig,
            stripeIntent = stripeIntent,
            merchantName = paymentSheetConfig?.merchantDisplayName
                ?: appContext.applicationInfo.loadLabel(appContext.packageManager).toString(),
            initialSelection = null
        )

        PaymentElementViewModel.Factory(
            supportedPaymentMethods = getPMsToAdd(
                stripeIntent,
                paymentSheetConfig,
                lpmResourceRepository.getRepository()
            ),
            paymentElementConfig = config,
            context = appContext,
            lifecycleScope = lifecycleScope,
            injector = injector
        )
    }

    internal fun onPaymentResult(paymentResult: PaymentResult) {
        lifecycleScope.launch {
            paymentResultCallback.onPaymentResult(paymentResult)
        }
    }

    override fun completePayment(paymentSelection: PaymentSelection) {
        val confirmParamsFactory =
            ConfirmStripeIntentParamsFactory.createFactory(clientSecret, null)

        when (paymentSelection) {
            is PaymentSelection.Saved -> {
                confirmParamsFactory.create(paymentSelection)
            }
            is PaymentSelection.New -> {
                confirmParamsFactory.create(paymentSelection)
            }
            else -> null
        }?.let { confirmParams ->
            lifecycleScope.launch {
                when (confirmParams) {
                    is ConfirmPaymentIntentParams -> {
                        paymentLauncher?.confirm(confirmParams)
                    }
                    is ConfirmSetupIntentParams -> {
                        paymentLauncher?.confirm(confirmParams)
                    }
                }
            }
        }
    }
}
