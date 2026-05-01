package com.stripe.android.payments.samsungpay

import android.os.Bundle
import android.os.Parcelable
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.samsung.android.sdk.samsungpay.v2.PartnerInfo
import com.samsung.android.sdk.samsungpay.v2.SpaySdk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize

/**
 * A launcher for Samsung Pay payment flows.
 *
 * Use this to check Samsung Pay availability and present the Samsung Pay
 * payment sheet to collect payment details. After the user approves,
 * the credential is exchanged for a Stripe token via [TokenExchangeHandler]
 * and the PaymentIntent or SetupIntent is confirmed.
 *
 * ## Usage
 *
 * ```kotlin
 * val launcher = SamsungPayLauncher(
 *     activity = this,
 *     config = SamsungPayLauncher.Config(
 *         environment = SamsungPayEnvironment.Production,
 *         productId = "my_product_id",
 *         merchantName = "My Store",
 *     ),
 *     tokenExchangeHandler = { request ->
 *         // Call your server to exchange for a Stripe token
 *         myApi.exchangeSamsungPayToken(request.rawCredential)
 *     },
 *     listener = { event ->
 *         when (event) {
 *             is Event.Availability -> { /* show/hide button */ }
 *             is Event.PaymentResult -> { /* handle result */ }
 *         }
 *     }
 * )
 *
 * // When user taps "Pay with Samsung Pay":
 * launcher.present("pi_xxx_secret_yyy")
 * ```
 */
class SamsungPayLauncher internal constructor(
    private val lifecycleScope: CoroutineScope,
    private val config: Config,
    private val tokenExchangeHandler: TokenExchangeHandler,
    private val listener: EventListener,
    private val activityResultLauncher: ActivityResultLauncher<SamsungPayLauncherContract.Args>,
    private val repositoryFactory: () -> SamsungPayRepository,
) {
    /**
     * Create a [SamsungPayLauncher] from a [ComponentActivity].
     *
     * Must be called during or before `Activity.onCreate()`.
     */
    constructor(
        activity: ComponentActivity,
        config: Config,
        tokenExchangeHandler: TokenExchangeHandler,
        listener: EventListener,
    ) : this(
        lifecycleScope = activity.lifecycleScope,
        config = config,
        tokenExchangeHandler = tokenExchangeHandler,
        listener = listener,
        activityResultLauncher = activity.registerForActivityResult(
            SamsungPayLauncherContract()
        ) { result ->
            listener.onEvent(Event.PaymentResult(result))
        },
        repositoryFactory = {
            DefaultSamsungPayRepository(
                context = activity.applicationContext,
                partnerInfo = buildPartnerInfo(config),
            )
        },
    )

    /**
     * Create a [SamsungPayLauncher] from a [Fragment].
     *
     * Must be called during or before `Fragment.onCreate()`.
     */
    constructor(
        fragment: Fragment,
        config: Config,
        tokenExchangeHandler: TokenExchangeHandler,
        listener: EventListener,
    ) : this(
        lifecycleScope = fragment.lifecycleScope,
        config = config,
        tokenExchangeHandler = tokenExchangeHandler,
        listener = listener,
        activityResultLauncher = fragment.registerForActivityResult(
            SamsungPayLauncherContract()
        ) { result ->
            listener.onEvent(Event.PaymentResult(result))
        },
        repositoryFactory = {
            DefaultSamsungPayRepository(
                context = fragment.requireContext().applicationContext,
                partnerInfo = buildPartnerInfo(config),
            )
        },
    )

    init {
        lifecycleScope.launch {
            val result = repositoryFactory().checkAvailability()
            listener.onEvent(Event.Availability(result))
        }
    }

    /**
     * Present Samsung Pay to collect payment details and confirm the
     * PaymentIntent or SetupIntent identified by [clientSecret].
     *
     * The intent type is auto-detected from the client secret prefix:
     * - `"pi_"` → PaymentIntent
     * - `"seti_"` → SetupIntent
     *
     * For SetupIntents, a [currencyCode] must be provided for the Samsung Pay sheet.
     */
    @JvmOverloads
    fun present(clientSecret: String, currencyCode: String = "USD") {
        tokenExchangeHandlerHolder = tokenExchangeHandler

        val args = if (clientSecret.startsWith("seti_")) {
            SamsungPayLauncherContract.Args.SetupIntentArgs(
                clientSecret = clientSecret,
                config = config,
                currencyCode = currencyCode,
            )
        } else {
            SamsungPayLauncherContract.Args.PaymentIntentArgs(
                clientSecret = clientSecret,
                config = config,
            )
        }

        activityResultLauncher.launch(args)
    }

    // region Public types

    /**
     * Configuration for [SamsungPayLauncher].
     */
    @Parcelize
    class Config(
        val environment: SamsungPayEnvironment,
        val productId: String,
        val merchantName: String,
        val allowedCardBrands: Set<CardBrand> = CardBrand.DEFAULT_BRANDS,
        val addressConfig: AddressConfig = AddressConfig(),
        val cardHolderNameEnabled: Boolean = false,
    ) : Parcelable {

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Config) return false
            return environment == other.environment &&
                productId == other.productId &&
                merchantName == other.merchantName &&
                allowedCardBrands == other.allowedCardBrands &&
                addressConfig == other.addressConfig &&
                cardHolderNameEnabled == other.cardHolderNameEnabled
        }

        override fun hashCode(): Int {
            var result = environment.hashCode()
            result = 31 * result + productId.hashCode()
            result = 31 * result + merchantName.hashCode()
            result = 31 * result + allowedCardBrands.hashCode()
            result = 31 * result + addressConfig.hashCode()
            result = 31 * result + cardHolderNameEnabled.hashCode()
            return result
        }

        /**
         * Java-friendly builder for [Config].
         */
        class Builder(
            private val environment: SamsungPayEnvironment,
            private val productId: String,
            private val merchantName: String,
        ) {
            private var allowedCardBrands: Set<CardBrand> = CardBrand.DEFAULT_BRANDS
            private var addressConfig: AddressConfig = AddressConfig()
            private var cardHolderNameEnabled: Boolean = false

            fun setAllowedCardBrands(brands: Set<CardBrand>) = apply {
                this.allowedCardBrands = brands
            }

            fun setAddressConfig(config: AddressConfig) = apply {
                this.addressConfig = config
            }

            fun setCardHolderNameEnabled(enabled: Boolean) = apply {
                this.cardHolderNameEnabled = enabled
            }

            fun build(): Config = Config(
                environment = environment,
                productId = productId,
                merchantName = merchantName,
                allowedCardBrands = allowedCardBrands,
                addressConfig = addressConfig,
                cardHolderNameEnabled = cardHolderNameEnabled,
            )
        }
    }

    /**
     * The result of a Samsung Pay payment flow.
     */
    sealed interface Result : Parcelable {
        /** Payment was successfully confirmed. */
        @Parcelize
        data object Completed : Result

        /** Payment failed. */
        @Parcelize
        data class Failed(val error: SamsungPayException) : Result

        /** User canceled the Samsung Pay sheet. */
        @Parcelize
        data object Canceled : Result
    }

    /**
     * Events delivered to [EventListener].
     */
    sealed interface Event {
        /** Samsung Pay availability has been determined. */
        data class Availability(val result: AvailabilityResult) : Event

        /** The payment flow completed, failed, or was canceled. */
        data class PaymentResult(val result: Result) : Event
    }

    /**
     * Listener for [SamsungPayLauncher] events.
     */
    fun interface EventListener {
        fun onEvent(event: Event)
    }

    /**
     * The result of checking Samsung Pay availability.
     */
    sealed interface AvailabilityResult {
        /** Samsung Pay is ready. Safe to call [present]. */
        data object Ready : AvailabilityResult

        /** Device does not support Samsung Pay. */
        data object NotSupported : AvailabilityResult

        /** Samsung Pay app needs initial setup. Merchant may offer to launch setup. */
        data class SetupRequired(val launchSetup: () -> Unit) : AvailabilityResult

        /** Samsung Pay app needs an update. Merchant may offer to launch update. */
        data class UpdateRequired(val launchUpdate: () -> Unit) : AvailabilityResult
    }

    // endregion

    companion object {
        @Volatile
        internal var tokenExchangeHandlerHolder: TokenExchangeHandler? = null

        /**
         * Creates a [SamsungPayLauncher] that is remembered across compositions.
         * Must be called unconditionally during initialization (not inside conditionals).
         */
        @Composable
        fun rememberLauncher(
            config: Config,
            tokenExchangeHandler: TokenExchangeHandler,
            listener: EventListener,
        ): SamsungPayLauncher {
            val currentListener by rememberUpdatedState(listener)
            val currentHandler by rememberUpdatedState(tokenExchangeHandler)
            val context = LocalContext.current

            val activityResultLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                SamsungPayLauncherContract()
            ) { result ->
                currentListener.onEvent(Event.PaymentResult(result))
            }

            val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

            return remember(config) {
                SamsungPayLauncher(
                    lifecycleScope = lifecycleOwner.lifecycleScope,
                    config = config,
                    tokenExchangeHandler = object : TokenExchangeHandler {
                        override suspend fun exchangeForToken(request: SamsungPayTokenRequest): String {
                            return currentHandler.exchangeForToken(request)
                        }
                    },
                    listener = EventListener { event -> currentListener.onEvent(event) },
                    activityResultLauncher = activityResultLauncher,
                    repositoryFactory = {
                        DefaultSamsungPayRepository(
                            context = context.applicationContext,
                            partnerInfo = buildPartnerInfo(config),
                        )
                    },
                )
            }
        }

        private fun buildPartnerInfo(config: Config): PartnerInfo {
            val bundle = Bundle().apply {
                putString(
                    SpaySdk.PARTNER_SERVICE_TYPE,
                    SpaySdk.ServiceType.INAPP_PAYMENT.toString()
                )
            }
            return PartnerInfo(config.productId, bundle)
        }
    }
}
