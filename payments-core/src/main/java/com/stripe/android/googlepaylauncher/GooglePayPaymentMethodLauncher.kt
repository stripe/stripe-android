package com.stripe.android.googlepaylauncher

import android.os.Parcelable
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.IntDef
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.stripe.android.PaymentConfiguration
import com.stripe.android.googlepaylauncher.GooglePayPaymentMethodLauncher.Result
import com.stripe.android.model.PaymentMethod
import com.stripe.android.networking.AnalyticsEvent
import com.stripe.android.networking.AnalyticsRequestExecutor
import com.stripe.android.networking.AnalyticsRequestFactory
import com.stripe.android.networking.DefaultAnalyticsRequestExecutor
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
open class GooglePayPaymentMethodLauncher @AssistedInject internal constructor(
    @Assisted lifecycleScope: CoroutineScope,
    @Assisted private val config: Config,
    @Assisted private val readyCallback: ReadyCallback,
    @Assisted private val activityResultLauncher: ActivityResultLauncher<GooglePayPaymentMethodLauncherContract.Args>,
    private val googlePayRepositoryFactory: (GooglePayEnvironment) -> GooglePayRepository,
    analyticsRequestFactory: AnalyticsRequestFactory,
    analyticsRequestExecutor: AnalyticsRequestExecutor
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
        activity.lifecycleScope,
        config,
        readyCallback,
        activity.registerForActivityResult(
            GooglePayPaymentMethodLauncherContract()
        ) {
            resultCallback.onResult(it)
        },
        googlePayRepositoryFactory = {
            DefaultGooglePayRepository(
                activity.application,
                it
            )
        },
        AnalyticsRequestFactory(
            activity,
            PaymentConfiguration.getInstance(activity).publishableKey,
            setOf(PRODUCT_USAGE)
        ),
        DefaultAnalyticsRequestExecutor()
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
        fragment.viewLifecycleOwner.lifecycleScope,
        config,
        readyCallback,
        fragment.registerForActivityResult(
            GooglePayPaymentMethodLauncherContract()
        ) {
            resultCallback.onResult(it)
        },
        googlePayRepositoryFactory = {
            DefaultGooglePayRepository(
                fragment.requireActivity().application,
                it
            )
        },
        AnalyticsRequestFactory(
            fragment.requireContext(),
            PaymentConfiguration.getInstance(fragment.requireContext()).publishableKey,
            setOf(PRODUCT_USAGE)
        ),
        DefaultAnalyticsRequestExecutor()
    )

    init {
        analyticsRequestExecutor.executeAsync(
            analyticsRequestFactory.createRequest(AnalyticsEvent.GooglePayPaymentMethodLauncherInit)
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
    open fun present(
        currencyCode: String,
        amount: Int = 0,
        transactionId: String? = null
    ) {
        check(isReady) {
            "present() may only be called when Google Pay is available on this device."
        }

        activityResultLauncher.launch(
            GooglePayPaymentMethodLauncherContract.Args(
                config = config,
                currencyCode = currencyCode,
                amount = amount,
                transactionId = transactionId
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
        internal const val PRODUCT_USAGE = "GooglePayPaymentMethodLauncher"

        // Generic internal error
        const val INTERNAL_ERROR = 1

        // The application is misconfigured
        const val DEVELOPER_ERROR = 2

        // Error executing a network call
        const val NETWORK_ERROR = 3
    }
}
