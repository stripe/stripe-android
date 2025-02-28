package com.stripe.android.googlepaylauncher

import android.content.Context
import android.os.Parcelable
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.IntDef
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.Fragment
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.stripe.android.CardBrandFilter
import com.stripe.android.DefaultCardBrandFilter
import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.networking.AnalyticsRequestExecutor
import com.stripe.android.core.networking.DefaultAnalyticsRequestExecutor
import com.stripe.android.googlepaylauncher.GooglePayPaymentMethodLauncher.Result
import com.stripe.android.model.PaymentMethod
import com.stripe.android.networking.PaymentAnalyticsEvent
import com.stripe.android.networking.PaymentAnalyticsRequestFactory
import com.stripe.android.payments.core.analytics.ErrorReporter
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import java.util.Locale

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
    @Assisted private val activityResultLauncher: ActivityResultLauncher<GooglePayPaymentMethodLauncherContractV2.Args>,
    @Assisted private val skipReadyCheck: Boolean,
    context: Context,
    private val googlePayRepositoryFactory: (GooglePayEnvironment) -> GooglePayRepository,
    @Assisted private val cardBrandFilter: CardBrandFilter,
    paymentAnalyticsRequestFactory: PaymentAnalyticsRequestFactory = PaymentAnalyticsRequestFactory(
        context,
        PaymentConfiguration.getInstance(context).publishableKey,
        setOf(PRODUCT_USAGE_TOKEN)
    ),
    analyticsRequestExecutor: AnalyticsRequestExecutor = DefaultAnalyticsRequestExecutor(),
) {
    private var isReady = false

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
        activity.registerForActivityResult(
            GooglePayPaymentMethodLauncherContractV2()
        ) {
            resultCallback.onResult(it)
        },
        config,
        readyCallback,
        DefaultCardBrandFilter
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
        fragment.registerForActivityResult(
            GooglePayPaymentMethodLauncherContractV2()
        ) {
            resultCallback.onResult(it)
        },
        config,
        readyCallback,
        DefaultCardBrandFilter
    )

    internal constructor(
        context: Context,
        lifecycleScope: CoroutineScope,
        activityResultLauncher: ActivityResultLauncher<GooglePayPaymentMethodLauncherContractV2.Args>,
        config: Config,
        readyCallback: ReadyCallback,
        cardBrandFilter: CardBrandFilter
    ) : this(
        lifecycleScope,
        config,
        readyCallback,
        activityResultLauncher,
        false,
        context,
        googlePayRepositoryFactory = {
            DefaultGooglePayRepository(
                context = context,
                environment = config.environment,
                billingAddressParameters = config.billingAddressConfig.convert(),
                existingPaymentMethodRequired = config.existingPaymentMethodRequired,
                allowCreditCards = config.allowCreditCards,
                errorReporter = ErrorReporter.createFallbackInstance(
                    context = context,
                    productUsage = setOf(PRODUCT_USAGE_TOKEN),
                )
            )
        },
        cardBrandFilter = cardBrandFilter
    )

    init {
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
    @Deprecated(
        message = "Use the present method that takes a Long as the amount instead.",
        replaceWith = ReplaceWith(
            expression = "present(currencyCode, amount.toLong(), transactionId)",
        ),
    )
    @JvmOverloads
    fun present(
        currencyCode: String,
        amount: Int,
        transactionId: String? = null
    ) {
        present(currencyCode, amount.toLong(), transactionId)
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
     * @param label An optional label to display with the amount. Google Pay may or may not display
     * this label depending on its own internal logic. Defaults to a generic label if none is
     * provided.
     */
    @JvmOverloads
    fun present(
        currencyCode: String,
        amount: Long = 0L,
        transactionId: String? = null,
        label: String? = null,
    ) {
        check(skipReadyCheck || isReady) {
            "present() may only be called when Google Pay is available on this device."
        }

        activityResultLauncher.launch(
            GooglePayPaymentMethodLauncherContractV2.Args(
                config = config,
                currencyCode = currencyCode,
                amount = amount,
                label = label,
                transactionId = transactionId,
                cardBrandFilter = cardBrandFilter
            )
        )
    }

    /**
     * Present the Google Pay UI without showing pricing labels.
     * Do not use this function when the transaction is processed in an EEA country.
     *
     * An [IllegalStateException] will be thrown if Google Pay is not available or ready for usage.
     *
     * @param currencyCode ISO 4217 alphabetic currency code. (e.g. "USD", "EUR")
     * @param transactionId A unique ID that identifies a transaction attempt. Merchants may use an
     * existing ID or generate a specific one for Google Pay transaction attempts.
     * This field is required when you send callbacks to the Google Transaction Events API.
     */
    @JvmOverloads
    fun presentWithUnknownPrice(
        currencyCode: String,
        transactionId: String? = null
    ) {
        check(skipReadyCheck || isReady) {
            "present() may only be called when Google Pay is available on this device."
        }

        activityResultLauncher.launch(
            GooglePayPaymentMethodLauncherContractV2.Args(
                config = config,
                currencyCode = currencyCode,
                transactionId = transactionId,
                cardBrandFilter = cardBrandFilter
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
        data object Canceled : Result()
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

        /**
         * Create a [GooglePayPaymentMethodLauncher] used for Jetpack Compose.
         *
         * This API uses Compose specific API [rememberLauncherForActivityResult] to register a
         * [ActivityResultLauncher] into current activity, it should be called as part of Compose
         * initialization path.
         * The GooglePayPaymentMethodLauncher created is remembered across recompositions.
         * Recomposition will always return the value produced by composition.
         */
        @Deprecated(
            message = "Use rememberGooglePayPaymentMethodLauncher() instead",
            replaceWith = ReplaceWith(
                expression = "rememberGooglePayPaymentMethodLauncher(config, readyCallback, resultCallback)",
            ),
        )
        @Composable
        fun rememberLauncher(
            config: Config,
            readyCallback: ReadyCallback,
            resultCallback: ResultCallback
        ): GooglePayPaymentMethodLauncher {
            return rememberGooglePayPaymentMethodLauncher(config, readyCallback, resultCallback)
        }
    }
}

/**
 * Creates a [GooglePayPaymentMethodLauncher] that is remembered across compositions.
 *
 * This *must* be called unconditionally, as part of the initialization path.
 *
 * @param config The [GooglePayPaymentMethodLauncher.Config] used to configure the integration.
 * @param readyCallback Called after determining whether Google Pay is available and ready to use.
 * [GooglePayPaymentMethodLauncher.present] may only be called if Google Pay is ready.
 * @param resultCallback Called with the result of the [GooglePayPaymentMethodLauncher] operation
 */
@Composable
fun rememberGooglePayPaymentMethodLauncher(
    config: GooglePayPaymentMethodLauncher.Config,
    readyCallback: GooglePayPaymentMethodLauncher.ReadyCallback,
    resultCallback: GooglePayPaymentMethodLauncher.ResultCallback
): GooglePayPaymentMethodLauncher {
    val currentReadyCallback by rememberUpdatedState(readyCallback)

    val context = LocalContext.current
    val lifecycleScope = LocalLifecycleOwner.current.lifecycleScope
    val activityResultLauncher = rememberLauncherForActivityResult(
        GooglePayPaymentMethodLauncherContractV2(),
        resultCallback::onResult
    )

    return remember(config) {
        GooglePayPaymentMethodLauncher(
            context = context,
            lifecycleScope = lifecycleScope,
            activityResultLauncher = activityResultLauncher,
            config = config,
            readyCallback = {
                currentReadyCallback.onReady(it)
            },
            cardBrandFilter = DefaultCardBrandFilter
        )
    }
}
