package com.stripe.android.googlepaylauncher

import android.os.Parcelable
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.networking.AnalyticsRequestExecutor
import com.stripe.android.core.networking.DefaultAnalyticsRequestExecutor
import com.stripe.android.googlepaylauncher.GooglePayLauncher.Result
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.SetupIntent
import com.stripe.android.networking.PaymentAnalyticsEvent
import com.stripe.android.networking.PaymentAnalyticsRequestFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import java.util.Locale

/**
 * A drop-in class that presents a Google Pay sheet to collect customer payment details and use it
 * to confirm a [PaymentIntent] or [SetupIntent]. When successful, will return [Result.Completed].
 *
 * Use [GooglePayLauncherContract] for Jetpack Compose integrations.
 *
 * See the [Google Pay integration guide](https://stripe.com/docs/google-pay) for more details.
 */
class GooglePayLauncher internal constructor(
    lifecycleScope: CoroutineScope,
    private val config: Config,
    private val readyCallback: ReadyCallback,
    private val activityResultLauncher: ActivityResultLauncher<GooglePayLauncherContract.Args>,
    private val googlePayRepositoryFactory: (GooglePayEnvironment) -> GooglePayRepository,
    paymentAnalyticsRequestFactory: PaymentAnalyticsRequestFactory,
    analyticsRequestExecutor: AnalyticsRequestExecutor
) {
    private var isReady = false

    /**
     * Constructor to be used when launching [GooglePayLauncher] from an Activity.
     *
     * @param activity the Activity that is launching the [GooglePayLauncher]
     *
     * @param readyCallback called after determining whether Google Pay is available and ready on
     * the device. [presentForPaymentIntent] and [presentForSetupIntent] may only be called if
     * Google Pay is ready.
     *
     * @param resultCallback called with the result of the [GooglePayLauncher] operation
     */
    constructor(
        activity: ComponentActivity,
        config: Config,
        readyCallback: ReadyCallback,
        resultCallback: ResultCallback
    ) : this(
        activity.lifecycleScope,
        config,
        readyCallback,
        activity.registerForActivityResult(
            GooglePayLauncherContract()
        ) {
            resultCallback.onResult(it)
        },
        googlePayRepositoryFactory = {
            DefaultGooglePayRepository(
                context = activity.application,
                environment = config.environment,
                billingAddressParameters = config.billingAddressConfig.convert(),
                existingPaymentMethodRequired = config.existingPaymentMethodRequired,
                allowCreditCards = config.allowCreditCards
            )
        },
        PaymentAnalyticsRequestFactory(
            activity,
            PaymentConfiguration.getInstance(activity).publishableKey,
            setOf(PRODUCT_USAGE)
        ),
        DefaultAnalyticsRequestExecutor(context = activity)
    )

    /**
     * Constructor to be used when launching [GooglePayLauncher] from a Fragment.
     *
     * @param fragment the Fragment that is launching the [GooglePayLauncher]
     *
     * @param readyCallback called after determining whether Google Pay is available and ready on
     * the device. [presentForPaymentIntent] and [presentForSetupIntent] may only be called if
     * Google Pay is ready.
     *
     * @param resultCallback called with the result of the [GooglePayLauncher] operation
     */
    constructor(
        fragment: Fragment,
        config: Config,
        readyCallback: ReadyCallback,
        resultCallback: ResultCallback
    ) : this(
        lifecycleScope = fragment.lifecycleScope,
        config = config,
        readyCallback = readyCallback,
        activityResultLauncher = fragment.registerForActivityResult(
            GooglePayLauncherContract(),
            resultCallback::onResult,
        ),
        googlePayRepositoryFactory = {
            DefaultGooglePayRepository(
                context = fragment.requireActivity().application,
                environment = config.environment,
                billingAddressParameters = config.billingAddressConfig.convert(),
                existingPaymentMethodRequired = config.existingPaymentMethodRequired,
                allowCreditCards = config.allowCreditCards
            )
        },
        paymentAnalyticsRequestFactory = PaymentAnalyticsRequestFactory(
            context = fragment.requireContext(),
            publishableKey = PaymentConfiguration.getInstance(fragment.requireContext()).publishableKey,
            defaultProductUsageTokens = setOf(PRODUCT_USAGE),
        ),
        analyticsRequestExecutor = DefaultAnalyticsRequestExecutor(fragment.requireContext()),
    )

    init {
        analyticsRequestExecutor.executeAsync(
            paymentAnalyticsRequestFactory.createRequest(PaymentAnalyticsEvent.GooglePayLauncherInit)
        )

        lifecycleScope.launch {
            val repository = googlePayRepositoryFactory(config.environment)
            readyCallback.onReady(
                repository.isReady().first().also {
                    isReady = it
                }
            )
        }
    }

    /**
     * Present Google Pay to collect customer payment details and use it to confirm the
     * [PaymentIntent] represented by [clientSecret].
     *
     * [PaymentIntent.currency] and [PaymentIntent.amount] will be used to populate the Google
     * Pay [TransactionInfo](https://developers.google.com/pay/api/android/reference/request-objects#TransactionInfo)
     * object.
     *
     * @param clientSecret the PaymentIntent's [client secret](https://stripe.com/docs/api/payment_intents/object#payment_intent_object-client_secret)
     */
    fun presentForPaymentIntent(clientSecret: String) {
        check(isReady) {
            "presentForPaymentIntent() may only be called when Google Pay is available on this device."
        }

        activityResultLauncher.launch(
            GooglePayLauncherContract.PaymentIntentArgs(
                clientSecret = clientSecret,
                config = config
            )
        )
    }

    /**
     * Present Google Pay to collect customer payment details and use it to confirm the
     * [SetupIntent] represented by [clientSecret].
     *
     * The Google Pay API requires a [currencyCode](https://developers.google.com/pay/api/android/reference/request-objects#TransactionInfo).
     * [currencyCode] is required even though the SetupIntent API does not require it.
     *
     * @param clientSecret the SetupIntent's [client secret](https://stripe.com/docs/api/setup_intents/object#setup_intent_object-client_secret)
     * @param currencyCode The ISO 4217 alphabetic currency code.
     */
    fun presentForSetupIntent(
        clientSecret: String,
        currencyCode: String
    ) {
        check(isReady) {
            "presentForSetupIntent() may only be called when Google Pay is available on this device."
        }

        activityResultLauncher.launch(
            GooglePayLauncherContract.SetupIntentArgs(
                clientSecret = clientSecret,
                config = config,
                currencyCode = currencyCode
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
        var existingPaymentMethodRequired: Boolean = true,

        /**
         * Set to false if you don't support credit cards.
         *
         * Default: The credit card class is supported for the card networks specified.
         */
        var allowCreditCards: Boolean = true
    ) : Parcelable {

        internal val isJcbEnabled: Boolean
            get() = merchantCountryCode.equals(Locale.JAPAN.country, ignoreCase = true)
    }

    @Parcelize
    data class BillingAddressConfig @JvmOverloads constructor(
        internal val isRequired: Boolean = false,

        /**
         * Billing address format required to complete the transaction.
         */
        internal val format: Format = Format.Min,

        /**
         * Set to true if a phone number is required to process the transaction.
         */
        internal val isPhoneNumberRequired: Boolean = false
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
        @Parcelize
        object Completed : Result()

        @Parcelize
        data class Failed(
            val error: Throwable
        ) : Result()

        @Parcelize
        object Canceled : Result()
    }

    fun interface ReadyCallback {
        fun onReady(isReady: Boolean)
    }

    fun interface ResultCallback {
        fun onResult(result: Result)
    }

    companion object {
        internal const val PRODUCT_USAGE = "GooglePayLauncher"

        /**
         * Create a [GooglePayLauncher] used for Jetpack Compose.
         *
         * This API uses Compose specific API [rememberLauncherForActivityResult] to register a
         * [ActivityResultLauncher] into current activity, it should be called as part of Compose
         * initialization path.
         * The GooglePayLauncher created is remembered across recompositions. Recomposition will
         * always return the value produced by composition.
         */
        @Deprecated(
            message = "Use rememberGooglePayLauncher() instead",
            replaceWith = ReplaceWith(
                expression = "rememberGooglePayLauncher(config, readyCallback, resultCallback)",
            ),
        )
        @Composable
        fun rememberLauncher(
            config: Config,
            readyCallback: ReadyCallback,
            resultCallback: ResultCallback
        ): GooglePayLauncher {
            return rememberGooglePayLauncher(config, readyCallback, resultCallback)
        }
    }
}

/**
 * Creates a [GooglePayLauncher] that is remembered across compositions.
 *
 * This *must* be called unconditionally, as part of the initialization path.
 *
 * @param config The [GooglePayLauncher.Config] used to configure the integration.
 * @param readyCallback Called after determining whether Google Pay is available and ready to use.
 * [GooglePayLauncher.presentForPaymentIntent] and [GooglePayLauncher.presentForSetupIntent] may
 * only be called if Google Pay is ready.
 * @param resultCallback Called with the result of the [GooglePayLauncher] operation
 */
@Composable
fun rememberGooglePayLauncher(
    config: GooglePayLauncher.Config,
    readyCallback: GooglePayLauncher.ReadyCallback,
    resultCallback: GooglePayLauncher.ResultCallback
): GooglePayLauncher {
    val currentReadyCallback by rememberUpdatedState(readyCallback)

    val context = LocalContext.current
    val lifecycleScope = LocalLifecycleOwner.current.lifecycleScope
    val activityResultLauncher = rememberLauncherForActivityResult(
        GooglePayLauncherContract(),
        resultCallback::onResult
    )

    return remember(config) {
        GooglePayLauncher(
            lifecycleScope = lifecycleScope,
            config = config,
            readyCallback = {
                currentReadyCallback.onReady(it)
            },
            activityResultLauncher = activityResultLauncher,
            googlePayRepositoryFactory = {
                DefaultGooglePayRepository(
                    context = context,
                    environment = config.environment,
                    billingAddressParameters = config.billingAddressConfig.convert(),
                    existingPaymentMethodRequired = config.existingPaymentMethodRequired,
                    allowCreditCards = config.allowCreditCards
                )
            },
            PaymentAnalyticsRequestFactory(
                context,
                PaymentConfiguration.getInstance(context).publishableKey,
                setOf(GooglePayLauncher.PRODUCT_USAGE)
            ),
            DefaultAnalyticsRequestExecutor(context)
        )
    }
}
