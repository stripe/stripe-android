package com.stripe.hcaptcha

import androidx.annotation.RestrictTo
import java.io.Serializable

/**
 * Enum with all possible hCaptcha errors.
 * It contains both errors related to the android sdk instance and js client errors.
 * More info about js client errors here: https://docs.hcaptcha.com/configuration#error-codes
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
enum class HCaptchaError(
    /**
     * @return the integer encoding of the enum
     */
    val errorId: Int,
    /**
     * @return the error message
     */
    val message: String
) : Serializable {
    /**
     * Internet connection is missing.
     *
     * Make sure AndroidManifest requires internet permission:
     * `{ <}uses-permission android:name="android.permission.INTERNET" /{ >}`
     */
    NETWORK_ERROR(7, "No internet connection"),

    /**
     * Invalid data is not accepted by endpoints.
     */
    INVALID_DATA(8, "Invalid data is not accepted by endpoints"),

    /**
     * User may need to select the checkbox or if invisible programmatically call execute to
     * initiate the challenge again.
     */
    CHALLENGE_ERROR(9, "Challenge encountered error on setup"),

    /**
     * User may need to select the checkbox or if invisible programmatically call execute to
     * initiate the challenge again.
     */
    INTERNAL_ERROR(10, "hCaptcha client encountered an internal error"),

    /**
     * hCaptcha challenge expired
     */
    SESSION_TIMEOUT(15, "Session Timeout"),

    /**
     * hCaptcha token expired
     */
    TOKEN_TIMEOUT(16, "Token Timeout"),

    /**
     * User closed the challenge by pressing `back` button or touching the outside of the dialog.
     */
    CHALLENGE_CLOSED(30, "Challenge Closed"),

    /**
     * Rate limited due to too many tries.
     */
    RATE_LIMITED(31, "Rate Limited"),

    /**
     * Invalid custom theme
     */
    INVALID_CUSTOM_THEME(32, "Invalid custom theme"),

    /**
     * Insecure HTTP request intercepted
     */
    INSECURE_HTTP_REQUEST_ERROR(33, "Insecure resource requested"),

    /**
     * Generic error for unknown situations - should never happen.
     */
    ERROR(29, "Unknown error");

    override fun toString(): String {
        return message
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    companion object {
        /**
         * Finds the enum based on the integer encoding
         *
         * @param errorId the integer encoding
         * @return the [HCaptchaError] object
         * @throws RuntimeException when no match
         */
        fun fromId(errorId: Int): HCaptchaError {
            val errors = values()
            for (error in errors) {
                if (error.errorId == errorId) {
                    return error
                }
            }
            throw RuntimeException("Unsupported error id: $errorId")
        }
    }
}
