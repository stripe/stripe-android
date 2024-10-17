package com.stripe.android.model

import android.net.Uri
import android.os.Parcelable
import androidx.annotation.Keep
import androidx.annotation.RestrictTo
import com.stripe.android.core.model.StripeModel
import com.stripe.android.utils.StripeUrlUtils
import kotlinx.parcelize.Parcelize

/**
 * An interface for methods available in [PaymentIntent] and [SetupIntent]
 */
sealed interface StripeIntent : StripeModel {
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

    /**
     * Whether confirmation has succeeded and all required actions have been handled.
     */
    val isConfirmed: Boolean

    val lastErrorMessage: String?

    /**
     * Payment types that have not been activated in livemode, but have been activated in testmode.
     */
    val unactivatedPaymentMethods: List<String>

    /**
     * Payment types that are accepted when paying with Link.
     */
    val linkFundingSources: List<String>

    /**
     * Country code of the user.
     */
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    val countryCode: String?

    fun requiresAction(): Boolean

    fun requiresConfirmation(): Boolean

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun getPaymentMethodOptions(): Map<String, Any?>

    /**
     * Type of the next action to perform.
     */
    enum class NextActionType(val code: String) {
        RedirectToUrl("redirect_to_url"),
        UseStripeSdk("use_stripe_sdk"),
        DisplayOxxoDetails("oxxo_display_details"),
        DisplayPayNowDetails("paynow_display_qr_code"),
        AlipayRedirect("alipay_handle_redirect"),
        BlikAuthorize("blik_authorize"),
        WeChatPayRedirect("wechat_pay_redirect_to_android_app"),
        VerifyWithMicrodeposits("verify_with_microdeposits"),
        UpiAwaitNotification("upi_await_notification"),
        CashAppRedirect("cashapp_handle_redirect_or_display_qr_code"),
        DisplayBoletoDetails("boleto_display_details"),
        DisplayKonbiniDetails("konbini_display_details"),
        DisplayMultibancoDetails("multibanco_display_details"),
        SwishRedirect("swish_handle_redirect_or_display_qr_code");

        @Keep
        override fun toString(): String {
            return code
        }

        internal companion object {
            internal fun fromCode(code: String?): NextActionType? {
                return entries.firstOrNull { it.code == code }
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

        @Keep
        override fun toString(): String {
            return code
        }

        internal companion object {
            internal fun fromCode(code: String?): Status? {
                return entries.firstOrNull { it.code == code }
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

        @Keep
        override fun toString(): String {
            return code
        }

        internal companion object {
            internal fun fromCode(code: String?): Usage? {
                return entries.firstOrNull { it.code == code }
            }
        }
    }

    sealed class NextActionData : StripeModel {
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        interface DisplayVoucherDetails {
            val hostedVoucherUrl: String?

            val shouldCancelIntentOnUserNavigation: Boolean
                get() = false
        }

        @Parcelize
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
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
            override val hostedVoucherUrl: String? = null
        ) : NextActionData(), DisplayVoucherDetails

        @Parcelize
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        data class DisplayPayNowDetails(
            /**
             * URL of a webpage containing the voucher for this PayNow payment.
             */
            override val hostedVoucherUrl: String? = null
        ) : NextActionData(), DisplayVoucherDetails {
            override val shouldCancelIntentOnUserNavigation: Boolean
                get() = true
        }

        @Parcelize
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        data class DisplayBoletoDetails(
            /**
             * URL of a webpage containing the voucher for this payment.
             */
            override val hostedVoucherUrl: String? = null,
        ) : NextActionData(), DisplayVoucherDetails

        @Parcelize
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        data class DisplayKonbiniDetails(
            /**
             * URL of a webpage containing the voucher for this payment.
             */
            override val hostedVoucherUrl: String? = null,
        ) : NextActionData(), DisplayVoucherDetails

        @Parcelize
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        data class DisplayMultibancoDetails(
            /**
             * URL of a webpage containing the voucher for this payment.
             */
            override val hostedVoucherUrl: String? = null,
        ) : NextActionData(), DisplayVoucherDetails

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

            /**
             * Contains all parameters needed to perform a 3DS2 authentication.
             *
             * @param threeDS2IntentId The id of the PI/SI used to authenticate using 3DS2. When
             * non-null, indicates that a different PI/SI is used for authentication. That is the
             * case for payments using Link, for example, which use a global merchant for
             * authentication since the payment method is added to the consumer's global (not
             * merchant-specific) account.
             * @param publishableKey The publishable key that should be used to make 3DS2-related
             * API calls. It will only be non-null when the 3DS2 calls should be made with a key
             * different than the original merchant's key.
             */
            @Parcelize
            data class Use3DS2(
                val source: String,
                val serverName: String,
                val transactionId: String,
                val serverEncryption: DirectoryServerEncryption,
                val threeDS2IntentId: String?,
                val publishableKey: String?
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

        @Parcelize
        data object BlikAuthorize : NextActionData()

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @Parcelize
        data class WeChatPayRedirect(val weChat: WeChat) : NextActionData()

        @Parcelize
        data class VerifyWithMicrodeposits(
            val arrivalDate: Long,
            val hostedVerificationUrl: String,
            val microdepositType: MicrodepositType
        ) : NextActionData()

        @Parcelize
        data object UpiAwaitNotification : NextActionData()

        /**
         * Contains the authentication URL for redirecting your customer to Cash App.
         */
        @Parcelize
        data class CashAppRedirect(
            val mobileAuthUrl: String,
        ) : NextActionData()

        /**
         * Contains the authentication URL for redirecting your customer to Swish.
         */
        @Parcelize
        data class SwishRedirect(
            val mobileAuthUrl: String,
        ) : NextActionData()
    }
}
