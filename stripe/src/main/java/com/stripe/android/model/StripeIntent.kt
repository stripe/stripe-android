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
     * @deprecated use {@link #nextActionData} instead
     *
     * Contains instructions for authenticating by redirecting your customer to another page
     * or application.
     */
    @Deprecated("use {@link #nextActionData}",
        replaceWith = ReplaceWith("nextActionData as? StripeIntent.NextActionData.RedirectToUrl"))
    val redirectData: RedirectData?

    val clientSecret: String?

    /**
     * @deprecated use {@link #nextActionData} instead
     *
     * When confirming a PaymentIntent with the Stripe SDK, the Stripe SDK depends on this property
     * to invoke authentication flows. The shape of the contents is subject to change and is only
     * intended to be used by the Stripe SDK.
     */
    @Deprecated("use {@link #nextActionData}",
        replaceWith = ReplaceWith("nextActionData as? StripeIntent.NextActionData.SdkData"))
    val stripeSdkData: SdkData?

    val status: Status?

    val nextActionData: NextActionData?

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
     * @deprecated use {@link StripeIntent.NextActionData.SdkData} instead
     *
     * When confirming a [PaymentIntent] or [SetupIntent] with the Stripe SDK, the Stripe SDK
     * depends on this property to invoke authentication flows. The shape of the contents is subject
     * to change and is only intended to be used by the Stripe SDK.
     */
    @Deprecated("use [StripeIntent.NextActionData.SdkData]")
    data class SdkData internal constructor(
        val is3ds1: Boolean,
        val is3ds2: Boolean
    )

    /**
     * @deprecated use {@link StripeIntent.NextActionData.RedirectToUrl} instead
     *
     * Contains instructions for authenticating by redirecting your customer to another
     * page or application.
     */
    @Deprecated("use {@link StripeIntent.NextActionData.RedirectToUrl}")
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

    sealed class NextActionData : StripeModel {
        @Parcelize
        data class DisplayOxxoDetails(
            /**
             * The timestamp after which the OXXO expires.
             */
            val expiresAfter: Int = 0,

            /**
             * The OXXO number.
             */
            val number: String? = null
        ) : NextActionData()

        /**
         * Contains instructions for authenticating by redirecting your customer to another
         * page or application.
         */
        @Parcelize
        data class RedirectToUrl(
            /**
             * The URL you must redirect your customer to in order to authenticate.
             */
            val url: Uri,
            /**
             * If the customer does not exit their browser while authenticating, they will be redirected
             * to this specified URL after completion.
             */
            val returnUrl: String?,
            val mobileData: MobileData?
        ) : NextActionData() {

            sealed class MobileData : StripeModel {
                @Parcelize
                data class Alipay(
                    val data: String
                ) : MobileData()

                internal enum class Type(
                    internal val code: String
                ) {
                    Alipay("alipay");

                    internal companion object {
                        internal fun fromCode(code: String?): Type? {
                            return values().firstOrNull { it.code == code }
                        }
                    }
                }
            }
        }

        /**
         * When confirming a [PaymentIntent] or [SetupIntent] with the Stripe SDK, the Stripe SDK
         * depends on this property to invoke authentication flows. The shape of the contents is subject
         * to change and is only intended to be used by the Stripe SDK.
         */
        sealed class SdkData : NextActionData() {
            @Parcelize
            data class Use3DS1(
                val url: String
            ) : SdkData()

            @Parcelize
            data class Use3DS2(
                val source: String,
                val serverName: String,
                val transactionId: String,
                val serverEncryption: DirectoryServerEncryption
            ) : SdkData() {
                @Parcelize
                data class DirectoryServerEncryption(
                    val directoryServerId: String,
                    val dsCertificateData: String,
                    val rootCertsData: List<String>,
                    val keyId: String?
                ) : Parcelable
            }
        }
    }
}
