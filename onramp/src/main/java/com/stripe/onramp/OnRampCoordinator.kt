package com.stripe.onramp

import android.os.Parcelable
import androidx.activity.ComponentActivity
import androidx.fragment.app.Fragment
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.model.PaymentOption
import kotlinx.parcelize.Parcelize

/**
 * A class that presents the individual steps of a Link payment flow.
 */
interface OnRampCoordinator {

    /**
     * Configure the LinkCoordinator with Link configuration.
     *
     * @param configuration The Link configuration to use.
     * @param callback called with the result of configuring the LinkCoordinator.
     */
    fun configure(
        configuration: Configuration,
        callback: ConfigCallback
    )

    /**
     * Check if the customer has an existing Link account with the provided email address.
     */
    suspend fun hasAccount(
        emailAddress: String,
    ): kotlin.Result<Boolean>

    /**
     * Present Link to the customer for authentication.
     */
    fun promptForLinkAuthentication()

    /**
     * Set KYC information for the customer.
     *
     * @param kycInfo The KYC information to submit.
     * @return Result indicating success or failure of the KYC submission.
     */
    suspend fun setKycInfo(kycInfo: KycInfo): kotlin.Result<Unit>

    /**
     * Represents a Link user after successful authentication.
     */
    @Parcelize
    data class User(
        val email: String,
        val phone: String?,
        val isVerified: Boolean,
        val completedSignup: Boolean
    ) : Parcelable

    @Parcelize
    data class Configuration(
        val stripeIntent: StripeIntent,
        val appearance: PaymentSheet.Appearance,
        val merchantName: String,
    ) : Parcelable

    sealed class Result {
        object Success : Result()

        class Failure(
            val error: Throwable
        ) : Result()
    }

    fun interface ConfigCallback {
        fun onConfigured(
            success: Boolean,
            error: Throwable?
        )
    }

    /**
     * Builder utility to set callbacks for [OnRampCoordinator].
     *
     * @param paymentOptionCallback Called when the customer's payment option changes.
     * @param userAuthenticatedCallback Called when a user has been successfully authenticated.
     */
    class Builder(
        internal val paymentOptionCallback: (PaymentOption?) -> Unit,
        internal val userAuthenticatedCallback: ((User) -> Unit)? = null
    ) {

        /**
         * Returns a [OnRampCoordinator].
         *
         * @param activity The Activity that is presenting [OnRampCoordinator].
         */
        fun build(activity: ComponentActivity): OnRampCoordinator {
            return OnRampCoordinatorFactory(activity, paymentOptionCallback, userAuthenticatedCallback).create()
        }

        /**
         * Returns a [OnRampCoordinator].
         *
         * @param fragment The Fragment that is presenting [OnRampCoordinator].
         */
        fun build(fragment: Fragment): OnRampCoordinator {
            return OnRampCoordinatorFactory(fragment, paymentOptionCallback, userAuthenticatedCallback).create()
        }
    }

    /**
     * Represents KYC information required for crypto operations.
     */
    @Parcelize
    data class KycInfo(
        val firstName: String,
        val lastName: String,
        val dateOfBirth: String, // ISO 8601 format (YYYY-MM-DD)
        val address: Address,
        val ssn: String? = null, // Optional, for US residents
        val governmentId: GovernmentId? = null
    ) : Parcelable

    /**
     * Represents an address for KYC purposes.
     */
    @Parcelize
    data class Address(
        val line1: String,
        val line2: String? = null,
        val city: String,
        val state: String,
        val postalCode: String,
        val country: String // ISO 3166-1 alpha-2 country code
    ) : Parcelable

    /**
     * Represents government ID information.
     */
    @Parcelize
    data class GovernmentId(
        val type: Type,
        val number: String,
        val expirationDate: String? = null // ISO 8601 format (YYYY-MM-DD)
    ) : Parcelable {
        enum class Type {
            DRIVERS_LICENSE,
            PASSPORT,
            STATE_ID
        }
    }
}
