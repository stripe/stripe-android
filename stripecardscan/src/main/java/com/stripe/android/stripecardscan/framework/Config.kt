package com.stripe.android.stripecardscan.framework

import com.stripe.android.stripecardscan.framework.api.Network
import com.stripe.android.stripecardscan.framework.api.StripeNetwork
import com.stripe.android.camera.framework.time.seconds
import kotlinx.serialization.json.Json

object Config {

    /**
     * If set to true, turns on debug information.
     */
    @JvmStatic
    var isDebug: Boolean = false

    /**
     * A log tag used by this library.
     */
    @JvmStatic
    var logTag: String = "StripeCardScan"

    /**
     * Whether or not to display the Stripe logo.
     */
    @JvmStatic
    var displayLogo: Boolean = true

    /**
     * Whether or not to display the "I cannot scan" button.
     */
    @JvmStatic
    var enableCannotScanButton: Boolean = true
}

object NetworkConfig {
    val CARD_SCAN_RETRY_STATUS_CODES: Iterable<Int> = 500..599
    private const val BASE_URL = "https://api.stripe.com/v1"

    /**
     * Whether or not to compress network request bodies.
     *
     * TODO(ccen): Remove this field as compression is not supported now.
     */
    @JvmStatic
    var useCompression: Boolean = false

    /**
     * The total number of times to try making a network request.
     */
    @JvmStatic
    var retryTotalAttempts: Int = 3

    /**
     * The delay between network request retries.
     *
     * TODO(ccen): support constant retry delay from stripe-core and use this value.
     */
    @JvmStatic
    var retryDelayMillis: Int = 5.seconds.inMilliseconds.toInt()

    /**
     * Status codes that should be retried from Stripe servers.
     */
    @JvmStatic
    var retryStatusCodes: Iterable<Int> = CARD_SCAN_RETRY_STATUS_CODES

    /**
     * The JSON configuration to use throughout this SDK.
     */
    @JvmStatic
    var json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    /**
     * The network interface to use
     */
    @JvmStatic
    internal var network: Network = StripeNetwork(
        baseUrl = BASE_URL,
        retryTotalAttempts = retryTotalAttempts,
        retryStatusCodes = retryStatusCodes,
    )
}
