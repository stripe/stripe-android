package com.stripe.android.googlepaylauncher

import android.os.Parcelable
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.stripe.android.googlepaylauncher.GooglePayPaymentMethodLauncher.Result
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.DefaultGooglePayRepository
import com.stripe.android.paymentsheet.GooglePayRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize

/**
 * A drop-in class that presents a Google Pay sheet to collect a customer's payment.
 *
 * When successful, will return a [PaymentMethod] via [Result.Completed.paymentMethod].
 */
internal class GooglePayPaymentMethodLauncher internal constructor(
    lifecycleScope: CoroutineScope,
    private val config: Config,
    private val googlePayRepositoryFactory: (GooglePayEnvironment) -> GooglePayRepository,
    private val readyCallback: ReadyCallback,
    private val activityResultLauncher: ActivityResultLauncher<GooglePayPaymentMethodLauncherContract.Args>
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
    internal constructor(
        activity: ComponentActivity,
        config: Config,
        readyCallback: ReadyCallback,
        resultCallback: ResultCallback
    ) : this(
        activity.lifecycleScope,
        config,
        googlePayRepositoryFactory = {
            DefaultGooglePayRepository(
                activity.application,
                it
            )
        },
        readyCallback,
        activity.registerForActivityResult(
            GooglePayPaymentMethodLauncherContract()
        ) {
            resultCallback.onResult(it)
        }
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
    internal constructor(
        fragment: Fragment,
        config: Config,
        readyCallback: ReadyCallback,
        resultCallback: ResultCallback
    ) : this(
        fragment.viewLifecycleOwner.lifecycleScope,
        config,
        googlePayRepositoryFactory = {
            DefaultGooglePayRepository(
                fragment.requireActivity().application,
                it
            )
        },
        readyCallback,
        fragment.registerForActivityResult(
            GooglePayPaymentMethodLauncherContract()
        ) {
            resultCallback.onResult(it)
        }
    )

    init {
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
     * Present the Google Pay UI when the final amount of the transaction is not yet known.
     *
     * An [IllegalStateException] will be thrown if Google Pay is not available or ready for usage.
     */
    fun present(currencyCode: String) {
        check(isReady) {
            "present() may only be called when Google Pay is available on this device."
        }

        activityResultLauncher.launch(
            GooglePayPaymentMethodLauncherContract.Args(
                config = config,
                currencyCode = currencyCode,
                amount = null
            )
        )
    }

    /**
     * Present the Google Pay UI when the final amount of the transaction is known.
     *
     * An [IllegalStateException] will be thrown if Google Pay is not available or ready for usage.
     */
    fun present(
        currencyCode: String,
        amount: Int
    ) {
        check(isReady) {
            "present() may only be called when Google Pay is available on this device."
        }

        activityResultLauncher.launch(
            GooglePayPaymentMethodLauncherContract.Args(
                config = config,
                currencyCode = currencyCode,
                amount = amount
            )
        )
    }

    @Parcelize
    internal data class Config @JvmOverloads constructor(
        val environment: GooglePayEnvironment,
        val merchantCountryCode: String,
        val merchantName: String,

        /**
         * Flag to indicate whether Google Pay collect the customer's email address. Default to `false`.
         */
        var isEmailRequired: Boolean = false,

        /**
         * Billing address collection configuration.
         */
        var billingAddressConfig: BillingAddressConfig = BillingAddressConfig(),

        /**
         * If `true`, Google Pay is considered ready if the customer's Google Pay wallet
         * has existing payment methods.
         */
        var existingPaymentMethodRequired: Boolean = false
    ) : Parcelable

    @Parcelize
    internal data class BillingAddressConfig @JvmOverloads constructor(
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

    internal sealed class Result : Parcelable {
        @Parcelize
        data class Completed(
            val paymentMethod: PaymentMethod
        ) : Result()

        @Parcelize
        data class Failed(
            val error: Throwable
        ) : Result()

        @Parcelize
        object Canceled : Result()
    }

    internal fun interface ReadyCallback {
        fun onReady(isReady: Boolean)
    }

    internal fun interface ResultCallback {
        fun onResult(result: Result)
    }
}
