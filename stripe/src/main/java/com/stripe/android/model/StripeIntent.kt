package com.stripe.android.model

import android.net.Uri
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

/**
 * An interface for methods available in [PaymentIntent] and [SetupIntent]
 */
interface StripeIntent : StripeModel {
    /**
     * Unique identifier for the object.
     */
    val id: String?

    /**
     * Time at which the object was created. Measured in seconds since the Unix epoch.
     */
    val created: Long

    /**
     * An arbitrary string attached to the object. Often useful for displaying to users.
     */
    val description: String?

    /**
     * Has the value true if the object exists in live mode or the value false if the object exists
     * in test mode.
     */
    val isLiveMode: Boolean

    /**
     * ID of the payment method used in this PaymentIntent.
     */
    val paymentMethodId: String?

    /**
     * The list of payment method types (e.g. card) that this PaymentIntent is allowed to use.
     */
    val paymentMethodTypes: List<String>

    val nextActionType: NextActionType?

    val redirectData: RedirectData?

    /**
     * The client secret of this PaymentIntent. Used for client-side retrieval using a
     * publishable key.
     *
     * The client secret can be used to complete a payment from your frontend. It should not be
     * stored, logged, embedded in URLs, or exposed to anyone other than the customer. Make sure
     * that you have TLS enabled on any page that includes the client secret.
     *
     * Refer to our docs to accept a payment and learn about how client_secret should be handled.
     */
    val clientSecret: String?

    val stripeSdkData: SdkData?

    val status: Status?

    fun requiresAction(): Boolean

    fun requiresConfirmation(): Boolean

    /**
     * See [payment_intent.next_action_type](https://stripe.com/docs/api/payment_intents/object#payment_intent_object-next_action-type)
     */
    enum class NextActionType(val code: String) {
        RedirectToUrl("redirect_to_url"),
        UseStripeSdk("use_stripe_sdk");

        override fun toString(): String {
            return code
        }

        internal companion object {
            internal fun fromCode(code: String?): NextActionType? {
                return values().firstOrNull { it.code == code }
            }
        }
    }

    /**
     * - [The Intent State Machine - Intent statuses](https://stripe.com/docs/payments/intents#intent-statuses)
     * - [PaymentIntent.status API reference](https://stripe.com/docs/api/payment_intents/object#payment_intent_object-status)
     * - [SetupIntent.status API reference](https://stripe.com/docs/api/setup_intents/object#setup_intent_object-status)
     */
    enum class Status(val code: String) {
        Canceled("canceled"),
        Processing("processing"),
        RequiresAction("requires_action"),
        RequiresConfirmation("requires_confirmation"),
        RequiresPaymentMethod("requires_payment_method"),
        Succeeded("succeeded"),

        // only applies to Payment Intents
        RequiresCapture("requires_capture");

        override fun toString(): String {
            return code
        }

        internal companion object {
            internal fun fromCode(code: String?): Status? {
                return values().firstOrNull { it.code == code }
            }
        }
    }

    /**
     * See [setup_intent.usage](https://stripe.com/docs/api/setup_intents/object#setup_intent_object-usage) and
     * [Reusing Cards](https://stripe.com/docs/payments/cards/reusing-cards).
     */
    enum class Usage(val code: String) {
        /**
         * Use on_session if you intend to only reuse the payment method when your customer is
         * present in your checkout flow.
         */
        OnSession("on_session"),

        /**
         * Use off_session if your customer may or may not be in your checkout flow.
         */
        OffSession("off_session"),

        OneTime("one_time");

        override fun toString(): String {
            return code
        }

        internal companion object {
            internal fun fromCode(code: String?): Usage? {
                return values().firstOrNull { it.code == code }
            }
        }
    }

    data class SdkData internal constructor(internal val data: Map<String, *>) {
        internal val type: String = data[FIELD_TYPE] as String

        val is3ds2: Boolean
            get() = TYPE_3DS2 == type

        val is3ds1: Boolean
            get() = TYPE_3DS1 == type

        private companion object {
            private const val FIELD_TYPE = "type"

            private const val TYPE_3DS2 = "stripe_3ds2_fingerprint"
            private const val TYPE_3DS1 = "three_d_secure_redirect"
        }
    }

    @Parcelize
    data class RedirectData internal constructor(
        /**
         * See [PaymentIntent.next_action.redirect_to_url.url](https://stripe.com/docs/api/payment_intents/object#payment_intent_object-next_action-redirect_to_url-url)
         */
        val url: Uri,

        /**
         * See [PaymentIntent.next_action.redirect_to_url.return_url](https://stripe.com/docs/api/payment_intents/object#payment_intent_object-next_action-redirect_to_url-return_url)
         */
        val returnUrl: String?
    ) : Parcelable {
        internal companion object {
            internal const val FIELD_URL = "url"
            internal const val FIELD_RETURN_URL = "return_url"

            @JvmSynthetic
            internal fun create(redirectToUrlHash: Map<*, *>): RedirectData? {
                val urlObj = redirectToUrlHash[FIELD_URL]
                val returnUrlObj = redirectToUrlHash[FIELD_RETURN_URL]
                val url = if (urlObj is String) {
                    urlObj.toString()
                } else {
                    null
                }
                val returnUrl = if (returnUrlObj is String) {
                    returnUrlObj.toString()
                } else {
                    null
                }
                return url?.let { RedirectData(Uri.parse(it), returnUrl) }
            }
        }
    }
}
