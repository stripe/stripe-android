package com.stripe.onramp

import androidx.activity.ComponentActivity
import androidx.fragment.app.Fragment
import com.stripe.onramp.callback.ConfigCallback
import com.stripe.onramp.model.KycInfo
import com.stripe.onramp.model.OnRampConfiguration
import com.stripe.onramp.model.PrefillDetails
import com.stripe.onramp.result.OnRampKycResult
import com.stripe.onramp.result.OnRampVerificationResult

interface OnRampCoordinator {

    /**
     * Configure the LinkCoordinator with Link configuration.
     *
     * @param onRampConfiguration The OnRamp configuration to use.
     * @param callback called with the result of configuring the LinkCoordinator.
     */
    fun configure(
        onRampConfiguration: OnRampConfiguration,
        callback: ConfigCallback
    )

    /**
     * Present Link to the customer for authentication.
     *
     * @param emailAddress The email address for authentication.
     * @param prefillDetails Optional prefill details for signup if the email does not exist.
     */
    fun promptForLinkAuthentication(
        emailAddress: String,
        prefillDetails: PrefillDetails? = null,
    )

    /**
     * Submit KYC information for the customer.
     * Result will be delivered via the kycResultCallback provided during initialization.
     *
     * @param kycInfo The KYC information to submit.
     */
    fun submitKycInfo(kycInfo: KycInfo)

    /**
     * Builder utility to create [OnRampCoordinator] with callbacks.
     *
     * @param onRampCallbacks The callbacks for handling OnRamp events.
     */
    class Builder(
        internal val onRampCallbacks: OnRampCallbacks
    ) {

        /**
         * Returns a [OnRampCoordinator].
         *
         * @param activity The Activity that is presenting [OnRampCoordinator].
         */
        fun build(activity: ComponentActivity): OnRampCoordinator {
            return OnRampCoordinatorFactory(activity, onRampCallbacks).create()
        }

        /**
         * Returns a [OnRampCoordinator].
         *
         * @param fragment The Fragment that is presenting [OnRampCoordinator].
         */
        fun build(fragment: Fragment): OnRampCoordinator {
            return OnRampCoordinatorFactory(fragment, onRampCallbacks).create()
        }
    }

    /**
     * Callbacks for handling OnRamp events.
     */
@ConsistentCopyVisibility
data class OnRampCallbacks private constructor(
    val verificationResultCallback: (OnRampVerificationResult) -> Unit,
    val kycResultCallback: (OnRampKycResult) -> Unit
) {
    class Builder {
        private var verificationResultCallback: ((OnRampVerificationResult) -> Unit)? = null
        private var kycResultCallback: ((OnRampKycResult) -> Unit)? = null

        fun verificationResultCallback(callback: (OnRampVerificationResult) -> Unit) = apply {
            this.verificationResultCallback = callback
        }

        fun kycResultCallback(callback: (OnRampKycResult) -> Unit) = apply {
            this.kycResultCallback = callback
        }

        fun build(): OnRampCallbacks {
            return OnRampCallbacks(
                verificationResultCallback = verificationResultCallback
                    ?: throw IllegalArgumentException("verificationResultCallback is required"),
                kycResultCallback = kycResultCallback
                    ?: throw IllegalArgumentException("kycResultCallback is required")
            )
        }
    }
}


}
