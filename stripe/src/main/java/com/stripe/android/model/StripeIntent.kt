package com.stripe.android.model

import android.net.Uri
import android.os.Parcelable
import com.stripe.android.utils.StripeUrlUtils
import kotlinx.parcelize.Parcelize

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

    val clientSecret: String?

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
        DisplayOxxoDetails("display_oxxo_details"),
        AlipayRedirect("alipay_handle_redirect"),
        UpiAppRedirect("upi_app_redirect");

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
            val number: String? = null,

            /**
             * URL of a webpage containing the voucher for this OXXO payment.
             */
            val hostedVoucherUrl: String? = null
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
            val returnUrl: String?
        ) : NextActionData()

        /**
         * Contains instructions for authenticating by redirecting your customer to an
         * application which support UPI payment.
         */
        @Parcelize
        data class UpiAppRedirect(
            /**
             * Base64 encoded data returned by Billdesk
             * Decoding this gives 'upi://pay?pa=&pn=xxxxx&mc=NA&tr=refId&tn=Pay&am=100.00&mam=100.00&cu=INR'
             * This value needs to be passed to PSP app while transferring the intent
             */
            val native_data: String,
        ) : NextActionData()

        @Parcelize
        internal data class AlipayRedirect constructor(
            val data: String,
            val authCompleteUrl: String?,
            val webViewUrl: Uri,
            val returnUrl: String? = null
        ) : NextActionData() {

            internal constructor(data: String, webViewUrl: String, returnUrl: String? = null) :
                this(data, extractReturnUrl(data), Uri.parse(webViewUrl), returnUrl)

            private companion object {
                /**
                 * The alipay data string is formatted as query parameters.
                 * When authenticate is complete, we make a request to the
                 * return_url param, as a hint to the backend to ping Alipay for
                 * the updated state
                 */
                private fun extractReturnUrl(data: String): String? = runCatching {
                    Uri.parse("alipay://url?$data")
                        .getQueryParameter("return_url")?.takeIf {
                            StripeUrlUtils.isStripeUrl(it)
                        }
                }.getOrNull()
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
