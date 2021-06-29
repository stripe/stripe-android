package com.stripe.android.googlepaylauncher

import android.os.Parcelable
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.stripe.android.paymentsheet.DefaultGooglePayRepository
import com.stripe.android.paymentsheet.GooglePayRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import java.util.Locale

/**
 * A drop-in class that presents a Google Pay sheet to collect a customer's payment.
 */
class GooglePayLauncher internal constructor(
    lifecycleScope: CoroutineScope,
    private val config: Config,
    private val googlePayRepositoryFactory: (GooglePayEnvironment) -> GooglePayRepository,
    private val readyCallback: ReadyCallback,
    private val activityResultLauncher: ActivityResultLauncher<GooglePayLauncherContract.Args>
) {
    private var isReady = false

    /**
     * Constructor to be used when launching [GooglePayLauncher] from an Activity.
     *
     * @param activity the Activity that is launching the [GooglePayLauncher]
     *
     * @param readyCallback called after determining whether Google Pay is available and ready on
     * the device. [present] may only be called if Google Pay is ready.
     *
     * @param callback called with the result of the [GooglePayLauncher] operation
     */
    constructor(
        activity: ComponentActivity,
        config: Config,
        readyCallback: ReadyCallback,
        callback: ResultCallback
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
            GooglePayLauncherContract()
        ) {
            callback.onResult(it)
        }
    )

    /**
     * Constructor to be used when launching [GooglePayLauncher] from a Fragment.
     *
     * @param fragment the Fragment that is launching the [GooglePayLauncher]
     *
     * @param readyCallback called after determining whether Google Pay is available and ready on
     * the device. [present] may only be called if Google Pay is ready.
     *
     * @param callback called with the result of the [GooglePayLauncher] operation
     */
    constructor(
        fragment: Fragment,
        config: Config,
        readyCallback: ReadyCallback,
        callback: ResultCallback
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
            GooglePayLauncherContract()
        ) {
            callback.onResult(it)
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

    fun present(clientSecret: String) {
        check(isReady) {
            "present() may only be called when Google Pay is available on this device."
        }

        activityResultLauncher.launch(
            GooglePayLauncherContract.Args(
                clientSecret = clientSecret,
                config = config
            )
        )
    }

    @Parcelize
    data class Config @JvmOverloads constructor(
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
}
