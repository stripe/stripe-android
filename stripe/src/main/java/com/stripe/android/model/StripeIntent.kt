package com.stripe.android.model

import android.net.Uri
import android.os.Parcelable
import com.stripe.android.utils.Either
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
     * The expanded [PaymentMethod] represented by [paymentMethodId].
     */
    val paymentMethod: PaymentMethod?

    val paymentMethodId: String?

    /**
     * The list of payment method types (e.g. card) that this PaymentIntent is allowed to use.
     */
    val paymentMethodTypes: List<String>

    val nextActionType: NextActionType?

    /**
     * Contains instructions for authenticating by redirecting your customer to another page
     * or application.
     */
    val redirectData: RedirectData?

    val clientSecret: String?

    /**
     * When confirming a PaymentIntent with the Stripe SDK, the Stripe SDK depends on this property
     * to invoke authentication flows. The shape of the contents is subject to change and is only
     * intended to be used by the Stripe SDK.
     */
    val stripeSdkData: SdkData?

    val status: Status?

    fun requiresAction(): Boolean

    fun requiresConfirmation(): Boolean

    /**
     * Type of the next action to perform.
     */
    enum class NextActionType(val code: String) {
        RedirectToUrl("redirect_to_url"),
        UseStripeSdk("use_stripe_sdk"),
        DisplayOxxoDetails("display_oxxo_details");

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

    /**
     * When confirming a [PaymentIntent] or [SetupIntent] with the Stripe SDK, the Stripe SDK
     * depends on this property to invoke authentication flows. The shape of the contents is subject
     * to change and is only intended to be used by the Stripe SDK.
     */
    data class SdkData internal constructor(
        val is3ds1: Boolean,
        val is3ds2: Boolean,
        internal val data: Either<Map<String, *>, NextActionData.SdkData>
    )

    /**
     * Contains instructions for authenticating by redirecting your customer to another
     * page or application.
     */
    @Parcelize
    data class RedirectData internal constructor(
        /**
         * The URL you must redirect your customer to in order to authenticate.
         */
        val url: Uri,

        /**
         * If the customer does not exit their browser while authenticating, they will be redirected
         * to this specified URL after completion.
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

    companion object {
        internal sealed class NextActionData : StripeModel {
            @Parcelize
            internal data class DisplayOxxoDetails(
                /**
                 * The timestamp after which the OXXO expires.
                 */
                val expiresAfter: Int = 0,

                /**
                 * The OXXO number.
                 */
                val number: String? = null
            ) : NextActionData()

            @Parcelize
            internal data class RedirectToUrl(
                val url: Uri,
                val returnUrl: String?
            ) : NextActionData()

            internal sealed class SdkData : NextActionData() {
                @Parcelize
                internal data class `3DS1`(
                    val url: String
                ) : SdkData()

                @Parcelize
                internal data class `3DS2`(
                    val source: String,
                    val serverName: String,
                    val transactionId: String,
                    val serverEncryption: DirectoryServerEncryption
                ) : SdkData() {
                    @Parcelize
                    internal data class DirectoryServerEncryption(
                        val directoryServerId: String,
                        val dsCertificateData: String,
                        val rootCertsData: List<String>,
                        val keyId: String?
                    ) : Parcelable
                }
            }
        }
    }
}
