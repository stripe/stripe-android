package com.stripe.android.cardverificationsheet.framework

import com.stripe.android.cardverificationsheet.framework.exception.InvalidBouncerApiKeyException
import com.stripe.android.cardverificationsheet.framework.time.Duration
import com.stripe.android.cardverificationsheet.framework.time.Rate
import com.stripe.android.cardverificationsheet.framework.time.seconds
import kotlinx.serialization.json.Json

private const val REQUIRED_API_KEY_LENGTH = 32

internal object Config {

    /**
     * If set to true, turns on debug information.
     */
    @JvmStatic
    var isDebug: Boolean = false

    /**
     * A log tag used by this library.
     */
    @JvmStatic
    var logTag: String = "Bouncer"

    /**
     * The API key to interface with Bouncer servers
     */
    @JvmStatic
    var apiKey: String? = null
        set(value) {
            if (value != null && value.length != REQUIRED_API_KEY_LENGTH) {
                throw InvalidBouncerApiKeyException
            }
            field = value
        }

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
     * Whether or not to track stats
     */
    @JvmStatic
    val trackStats: Boolean = true

    /**
     * Whether or not to upload stats
     */
    @JvmStatic
    var uploadStats: Boolean = true

    /**
     * Whether or not to display the Bouncer logo
     */
    @JvmStatic
    var displayLogo: Boolean = true

    /**
     * Whether or not to display the result of the scan to the user
     */
    @JvmStatic
    var displayScanResult: Boolean = true

    /**
     * If set to true, opt-in to beta versions of the ML models.
     */
    @JvmStatic
    var betaModelOptIn: Boolean = false

    /**
     * The frame rate of a device that is considered slow will be below this rate.
     */
    @JvmStatic
    var slowDeviceFrameRate = Rate(2, 1.seconds)

    /**
     * Allow downloading ML models.
     */
    @JvmStatic
    var downloadModels = true
}

internal object NetworkConfig {

    /**
     * The base URL where all network requests will be sent
     */
    @JvmStatic
    var baseUrl = "https://api.getbouncer.com"

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
     * Status codes that should be retried from bouncer servers.
     */
    @JvmStatic
    var retryStatusCodes: Iterable<Int> = 500..599
}
