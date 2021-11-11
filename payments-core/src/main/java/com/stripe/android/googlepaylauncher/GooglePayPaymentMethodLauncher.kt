package com.stripe.android.googlepaylauncher

import android.content.Context
import android.os.Parcelable
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.IntDef
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.stripe.android.BuildConfig
import com.stripe.android.Logger
import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.networking.AnalyticsRequestExecutor
import com.stripe.android.core.networking.DefaultAnalyticsRequestExecutor
import com.stripe.android.googlepaylauncher.GooglePayPaymentMethodLauncher.Result
import com.stripe.android.googlepaylauncher.injection.DaggerGooglePayPaymentMethodLauncherComponent
import com.stripe.android.model.PaymentMethod
import com.stripe.android.networking.PaymentAnalyticsEvent
import com.stripe.android.networking.PaymentAnalyticsRequestFactory
import com.stripe.android.networking.StripeApiRepository
import com.stripe.android.networking.StripeRepository
import com.stripe.android.payments.core.injection.ENABLE_LOGGING
import com.stripe.android.payments.core.injection.IOContext
import com.stripe.android.payments.core.injection.Injectable
import com.stripe.android.payments.core.injection.Injector
import com.stripe.android.payments.core.injection.InjectorKey
import com.stripe.android.payments.core.injection.PRODUCT_USAGE
import com.stripe.android.payments.core.injection.PUBLISHABLE_KEY
import com.stripe.android.payments.core.injection.STRIPE_ACCOUNT_ID
import com.stripe.android.payments.core.injection.WeakMapInjectorRegistry
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import java.util.Locale
import javax.inject.Named
import kotlin.coroutines.CoroutineContext

/**
 * A drop-in class that presents a Google Pay sheet to collect a customer's payment details.
 * When successful, will return a [PaymentMethod] via [Result.Completed.paymentMethod].
 *
 * Use [GooglePayPaymentMethodLauncher] for Jetpack Compose integrations.
 *
 * See the [Google Pay integration guide](https://stripe.com/docs/google-pay) for more details.
 */
@JvmSuppressWildcards
class GooglePayPaymentMethodLauncher @AssistedInject internal constructor(
    @Assisted lifecycleScope: CoroutineScope,
    @Assisted private val config: Config,
    @Assisted private val readyCallback: ReadyCallback,
    @Assisted private val activityResultLauncher: ActivityResultLauncher<GooglePayPaymentMethodLauncherContract.Args>,
    @Assisted private val skipReadyCheck: Boolean,
    context: Context,
    private val googlePayRepositoryFactory: (GooglePayEnvironment) -> GooglePayRepository,
    @Named(PRODUCT_USAGE) private val productUsage: Set<String>,
    @Named(PUBLISHABLE_KEY) private val publishableKeyProvider: () -> String,
    @Named(STRIPE_ACCOUNT_ID) private val stripeAccountIdProvider: () -> String?,
    // Default value for the following parameters is used only when instantiating from the public
    // constructors instead of dependency injection.
    @Named(ENABLE_LOGGING) private val enableLogging: Boolean = BuildConfig.DEBUG,
    @IOContext private val ioContext: CoroutineContext = Dispatchers.IO,
    paymentAnalyticsRequestFactory: PaymentAnalyticsRequestFactory = PaymentAnalyticsRequestFactory(
        context,
        PaymentConfiguration.getInstance(context).publishableKey,
        setOf(PRODUCT_USAGE_TOKEN)
    ),
    analyticsRequestExecutor: AnalyticsRequestExecutor = DefaultAnalyticsRequestExecutor(),
    stripeRepository: StripeRepository = StripeApiRepository(
        context,
        publishableKeyProvider,
        logger = Logger.getInstance(enableLogging),
        workContext = ioContext,
        productUsageTokens = setOf(PRODUCT_USAGE_TOKEN),
        paymentAnalyticsRequestFactory = paymentAnalyticsRequestFactory
    )
) {
    private var isReady = false

    private val launcherComponent = DaggerGooglePayPaymentMethodLauncherComponent.builder()
        .context(context)
        .ioContext(ioContext)
        .analyticsRequestFactory(paymentAnalyticsRequestFactory)
        .stripeRepository(stripeRepository)
        .googlePayConfig(config)
        .enableLogging(enableLogging)
        .publishableKeyProvider(publishableKeyProvider)
        .stripeAccountIdProvider(stripeAccountIdProvider)
        .build()

    @InjectorKey
    private val injectorKey: String = WeakMapInjectorRegistry.nextKey(
        requireNotNull(GooglePayPaymentMethodLauncher::class.simpleName)
    )

    private val injector = object : Injector {
        override fun inject(injectable: Injectable<*>) {
            when (injectable) {
                is GooglePayPaymentMethodLauncherViewModel.Factory -> {
                    launcherComponent.inject(injectable)
                }
                else -> {
                    throw IllegalArgumentException("invalid Injectable $injectable requested in $this")
                }
            }
        }
    }

    /**
     * Constructor to be used when launching [GooglePayPaymentMethodLauncher] from an Activity.
     * This constructor must be called no later than `Activity#onCreate()`.
     *
     * @param activity the Activity that is launching the [GooglePayPaymentMethodLauncher]
     *
     * @param readyCallback called after determining whether Google Pay is available and ready on
     * the device. [present] may only be called if Google Pay is ready.
     *
     * @param resultCallback called with the result of the [GooglePayPaymentMethodLauncher] operation
     */
    constructor(
        activity: ComponentActivity,
        config: Config,
        readyCallback: ReadyCallback,
        resultCallback: ResultCallback
    ) : this(
        activity,
        activity.lifecycleScope,
        activity,
        config,
        readyCallback,
        resultCallback
    )

    /**
     * Constructor to be used when launching [GooglePayPaymentMethodLauncher] from a Fragment.
     * This constructor must be called no later than `Fragment#onViewCreated()`.
     *
     * @param fragment the Fragment that is launching the [GooglePayPaymentMethodLauncher]
     *
     * @param readyCallback called after determining whether Google Pay is available and ready on
     * the device. [present] may only be called if Google Pay is ready.
     *
     * @param resultCallback called with the result of the [GooglePayPaymentMethodLauncher] operation
     */
    constructor(
        fragment: Fragment,
        config: Config,
        readyCallback: ReadyCallback,
        resultCallback: ResultCallback
    ) : this(
        fragment.requireContext(),
        fragment.viewLifecycleOwner.lifecycleScope,
        fragment,
        config,
        readyCallback,
        resultCallback
    )

    private constructor (
        context: Context,
        lifecycleScope: CoroutineScope,
        activityResultCaller: ActivityResultCaller,
        config: Config,
        readyCallback: ReadyCallback,
        resultCallback: ResultCallback
    ) : this(
        lifecycleScope,
        config,
        readyCallback,
        activityResultCaller.registerForActivityResult(
            GooglePayPaymentMethodLauncherContract()
        ) {
            resultCallback.onResult(it)
        },
        false,
        context,
        googlePayRepositoryFactory = {
            DefaultGooglePayRepository(
                context,
                config.environment,
                config.billingAddressConfig.convert(),
                config.existingPaymentMethodRequired
            )
        },
        productUsage = setOf(PRODUCT_USAGE_TOKEN),
        publishableKeyProvider = { PaymentConfiguration.getInstance(context).publishableKey },
        stripeAccountIdProvider = { PaymentConfiguration.getInstance(context).stripeAccountId }
    )

    init {
        WeakMapInjectorRegistry.register(injector, injectorKey)

        analyticsRequestExecutor.executeAsync(
            paymentAnalyticsRequestFactory.createRequest(PaymentAnalyticsEvent.GooglePayPaymentMethodLauncherInit)
        )

        if (!skipReadyCheck) {
            lifecycleScope.launch {
                val repository = googlePayRepositoryFactory(config.environment)
                readyCallback.onReady(
                    repository.isReady().first().also {
                        isReady = it
                    }
                )
            }
        }
    }

    /**
     * Present the Google Pay UI.
     *
     * An [IllegalStateException] will be thrown if Google Pay is not available or ready for usage.
     *
     * @param currencyCode ISO 4217 alphabetic currency code. (e.g. "USD", "EUR")
     * @param amount Amount intended to be collected. A positive integer representing how much to
     * charge in the smallest currency unit (e.g., 100 cents to charge $1.00 or 100 to charge ¥100,
     * a zero-decimal currency). If the amount is not yet known, use 0.
     * @param transactionId A unique ID that identifies a transaction attempt. Merchants may use an
     * existing ID or generate a specific one for Google Pay transaction attempts.
     * This field is required when you send callbacks to the Google Transaction Events API.
     */
    @JvmOverloads
    fun present(
        currencyCode: String,
        amount: Int = 0,
        transactionId: String? = null
    ) {
        check(skipReadyCheck || isReady) {
            "present() may only be called when Google Pay is available on this device."
        }

        activityResultLauncher.launch(
            GooglePayPaymentMethodLauncherContract.Args(
                config = config,
                currencyCode = currencyCode,
                amount = amount,
                transactionId = transactionId,
                injectionParams = GooglePayPaymentMethodLauncherContract.Args.InjectionParams(
                    injectorKey,
                    productUsage,
                    enableLogging,
                    publishableKeyProvider(),
                    stripeAccountIdProvider()
                )
            )
        )
    }

    @Parcelize
    data class Config @JvmOverloads constructor(
        val environment: GooglePayEnvironment,
        val merchantCountryCode: String,
        val merchantName: String,

        /**
         * Flag to indicate whether Google Pay collect the customer's email address.
         *
         * Default to `false`.
         */
        var isEmailRequired: Boolean = false,

        /**
         * Billing address collection configuration.
         */
        var billingAddressConfig: BillingAddressConfig = BillingAddressConfig(),

        /**
         * If `true`, Google Pay is considered ready if the customer's Google Pay wallet
         * has existing payment methods.
         *
         * Default to `true`.
         */
        var existingPaymentMethodRequired: Boolean = true
    ) : Parcelable {

        internal val isJcbEnabled: Boolean
            get() = merchantCountryCode.equals(Locale.JAPAN.country, ignoreCase = true)
    }

    @Parcelize
    data class BillingAddressConfig @JvmOverloads constructor(
        val isRequired: Boolean = false,

        /**
         * Billing address format required to complete the transaction.
         */
        val format: Format = Format.Min,

        /**
         * Set to true if a phone number is required to process the transaction.
         */
        val isPhoneNumberRequired: Boolean = false
    ) : Parcelable {
        /**
         * Billing address format required to complete the transaction.
         */
        enum class Format(internal val code: String) {
            /**
             * Name, country code, and postal code (default).
             */
            Min("MIN"),

            /**
             * Name, street address, locality, region, country code, and postal code.
             */
            Full("FULL")
        }
    }

    sealed class Result : Parcelable {
        /**
         * Represents a successful transaction.
         *
         * @param paymentMethod The resulting payment method.
         */
        @Parcelize
        data class Completed(
            val paymentMethod: PaymentMethod
        ) : Result()

        /**
         * Represents a failed transaction.
         *
         * @param error The failure reason.
         * @param errorCode The failure [ErrorCode].
         */
        @Parcelize
        data class Failed(
            val error: Throwable,
            @ErrorCode val errorCode: Int
        ) : Result()

        /**
         * Represents a transaction that was canceled by the user.
         */
        @Parcelize
        object Canceled : Result()
    }

    fun interface ReadyCallback {
        fun onReady(isReady: Boolean)
    }

    fun interface ResultCallback {
        fun onResult(result: Result)
    }

    /**
     * Error codes representing the possible error types for [Result.Failed].
     * See the corresponding [Result.Failed.error] message for more details.
     */
    @Target(AnnotationTarget.PROPERTY, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.TYPE)
    @IntDef(INTERNAL_ERROR, DEVELOPER_ERROR, NETWORK_ERROR)
    annotation class ErrorCode

    companion object {
        internal const val PRODUCT_USAGE_TOKEN = "GooglePayPaymentMethodLauncher"

        // Generic internal error
        const val INTERNAL_ERROR = 1

        // The application is misconfigured
        const val DEVELOPER_ERROR = 2

        // Error executing a network call
        const val NETWORK_ERROR = 3
    }
}
