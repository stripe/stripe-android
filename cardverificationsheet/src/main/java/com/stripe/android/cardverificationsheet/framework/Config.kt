package com.stripe.android.cardverificationsheet.framework

import com.stripe.android.cardverificationsheet.framework.api.Network
import com.stripe.android.cardverificationsheet.framework.api.StripeNetwork
import com.stripe.android.cardverificationsheet.framework.time.Duration
import com.stripe.android.cardverificationsheet.framework.time.seconds
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.properties.Properties

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
    var logTag: String = "CardVerificationSheet"

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

    /**
     * Whether or not to compress network request bodies.
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
     */
    @JvmStatic
    var retryDelay: Duration = 5.seconds

    /**
     * Status codes that should be retried from Stripe servers.
     */
    @JvmStatic
    var retryStatusCodes: Iterable<Int> = 500..599

    /**
     * The JSON configuration to use throughout this SDK.
     */
    @JvmStatic
    var json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    @JvmStatic
    @ExperimentalSerializationApi
    var form: Properties = Properties

    /**
     * The network interface to use
     */
    @JvmStatic
    var network: Network = StripeNetwork(
        baseUrl = "https://api.stripe.com/v1",
        retryDelay = retryDelay,
        retryTotalAttempts = retryTotalAttempts,
        retryStatusCodes = retryStatusCodes,
    )
}
