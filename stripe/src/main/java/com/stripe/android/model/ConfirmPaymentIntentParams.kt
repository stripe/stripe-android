package com.stripe.android.model

import android.os.Parcelable
import com.stripe.android.model.ConfirmPaymentIntentParams.SetupFutureUsage
import com.stripe.android.model.ConfirmPaymentIntentParams.Shipping
import com.stripe.android.model.ConfirmStripeIntentParams.Companion.PARAM_CLIENT_SECRET
import com.stripe.android.model.ConfirmStripeIntentParams.Companion.PARAM_MANDATE_ID
import com.stripe.android.model.ConfirmStripeIntentParams.Companion.PARAM_PAYMENT_METHOD_DATA
import com.stripe.android.model.ConfirmStripeIntentParams.Companion.PARAM_PAYMENT_METHOD_ID
import com.stripe.android.model.ConfirmStripeIntentParams.Companion.PARAM_RETURN_URL
import com.stripe.android.model.ConfirmStripeIntentParams.Companion.PARAM_USE_STRIPE_SDK
import kotlinx.android.parcel.Parcelize

data class ConfirmPaymentIntentParams internal constructor(
    val paymentMethodCreateParams: PaymentMethodCreateParams? = null,

    /**
     * ID of the payment method (a PaymentMethod, Card, or compatible Source object) to attach to
     * this PaymentIntent.
     *
     * [payment_method](https://stripe.com/docs/api/payment_intents/confirm#confirm_payment_intent-payment_method)
     */
    val paymentMethodId: String? = null,
    val sourceParams: SourceParams? = null,
    val sourceId: String? = null,

    val extraParams: Map<String, Any>? = null,

    /**
     * The client secret of this PaymentIntent. Used for client-side retrieval using a
     * publishable key.
     *
     * The client secret can be used to complete a payment from your frontend. It should not be
     * stored, logged, embedded in URLs, or exposed to anyone other than the customer. Make sure
     * that you have TLS enabled on any page that includes the client secret.
     *
     * Refer to our docs to [accept a payment](https://stripe.com/docs/payments/accept-a-payment)
     * and learn about how `client_secret` should be handled.
     *
     * [client_secret](https://stripe.com/docs/api/payment_intents/object#payment_intent_object-client_secret)
     */
    override val clientSecret: String,

    /**
     * The URL to redirect your customer back to after they authenticate or cancel their payment on
     * the payment method’s app or site. If you’d prefer to redirect to a mobile application, you
     * can alternatively supply an application URI scheme. This parameter is only used for cards
     * and other redirect-based payment methods.
     *
     * [return_url](https://stripe.com/docs/api/payment_intents/confirm#confirm_payment_intent-return_url)
     */
    val returnUrl: String? = null,

    /**
     * If the PaymentIntent has a `payment_method` and a `customer` or if you’re attaching a payment
     * method to the PaymentIntent in this request, you can pass `save_payment_method=true` to save
     * the payment method to the customer. Defaults to `false`.
     *
     * If the payment method is already saved to a customer, this does nothing. If this type of
     * payment method cannot be saved to a customer, the request will error.
     *
     * [save_payment_method](https://stripe.com/docs/api/payment_intents/confirm#confirm_payment_intent-save_payment_method)
     */
    private val savePaymentMethod: Boolean? = null,

    /**
     * Set to `true` only when using manual confirmation and the iOS or Android SDKs to handle
     * additional authentication steps.
     *
     * [use_stripe_sdk](https://stripe.com/docs/api/payment_intents/confirm#confirm_payment_intent-use_stripe_sdk)
     */
    private val useStripeSdk: Boolean = false,

    /**
     * Payment-method-specific configuration for this PaymentIntent.
     *
     * [payment_method_options](https://stripe.com/docs/api/payment_intents/confirm#confirm_payment_intent-payment_method_options)
     */
    private val paymentMethodOptions: PaymentMethodOptionsParams? = null,

    /**
     * ID of the mandate to be used for this payment.
     *
     * [mandate](https://stripe.com/docs/api/payment_intents/confirm#confirm_payment_intent-mandate)
     */
    private val mandateId: String? = null,

    /**
     * This hash contains details about the Mandate to create.
     *
     * [mandate_data](https://stripe.com/docs/api/payment_intents/confirm#confirm_payment_intent-mandate_data)
     */
    private val mandateData: MandateDataParams? = null,

    /**
     * See [SetupFutureUsage]
     *
     * [setup_future_usage](https://stripe.com/docs/api/payment_intents/confirm#confirm_payment_intent-setup_future_usage)
     */
    private val setupFutureUsage: SetupFutureUsage? = null,

    /**
     * See [Shipping]
     *
     * [shipping](https://stripe.com/docs/api/payment_intents/confirm#confirm_payment_intent-shipping)
     */
    private val shipping: Shipping? = null
) : ConfirmStripeIntentParams {

    fun shouldSavePaymentMethod(): Boolean {
        return savePaymentMethod == true
    }

    override fun shouldUseStripeSdk(): Boolean {
        return useStripeSdk
    }

    override fun withShouldUseStripeSdk(shouldUseStripeSdk: Boolean): ConfirmPaymentIntentParams {
        return copy(useStripeSdk = shouldUseStripeSdk)
    }

    /**
     * Create a Map representing this object that is prepared for the Stripe API.
     */
    override fun toParamMap(): Map<String, Any> {
        return mapOf(
            PARAM_CLIENT_SECRET to clientSecret,
            PARAM_USE_STRIPE_SDK to useStripeSdk
        ).plus(
            savePaymentMethod?.let {
                mapOf(PARAM_SAVE_PAYMENT_METHOD to it)
            }.orEmpty()
        ).plus(
            mandateId?.let { mapOf(PARAM_MANDATE_ID to it) }.orEmpty()
        ).plus(
            mandateDataParams?.let {
                mapOf(ConfirmStripeIntentParams.PARAM_MANDATE_DATA to it)
            }.orEmpty()
        ).plus(
            returnUrl?.let { mapOf(PARAM_RETURN_URL to it) }.orEmpty()
        ).plus(
            paymentMethodOptions?.let {
                mapOf(PARAM_PAYMENT_METHOD_OPTIONS to it.toParamMap())
            }.orEmpty()
        ).plus(
            setupFutureUsage?.let {
                mapOf(PARAM_SETUP_FUTURE_USAGE to it.code)
            }.orEmpty()
        ).plus(
            shipping?.let {
                mapOf(PARAM_SHIPPING to shipping.toParamMap())
            }.orEmpty()
        ).plus(
            paymentMethodParamMap
        ).plus(
            extraParams.orEmpty()
        )
    }

    private val paymentMethodParamMap: Map<String, Any>
        get() {
            return when {
                paymentMethodCreateParams != null -> {
                    mapOf(PARAM_PAYMENT_METHOD_DATA to paymentMethodCreateParams.toParamMap())
                }
                paymentMethodId != null -> {
                    mapOf(PARAM_PAYMENT_METHOD_ID to paymentMethodId)
                }
                sourceParams != null -> {
                    mapOf(PARAM_SOURCE_DATA to sourceParams.toParamMap())
                }
                sourceId != null -> {
                    mapOf(PARAM_SOURCE_ID to sourceId)
                }
                else -> emptyMap()
            }
        }

    /**
     * Use the user-defined [MandateDataParams] if specified, otherwise create a default
     * [MandateDataParams] if necessary.
     */
    private val mandateDataParams: Map<String, Any>?
        get() {
            return mandateData?.toParamMap()
                ?: if (paymentMethodCreateParams?.type?.hasMandate == true && mandateId == null) {
                    // Populate with default "online" MandateData
                    MandateDataParams(MandateDataParams.Type.Online.DEFAULT).toParamMap()
                } else {
                    null
                }
        }

    /**
     * [setup_future_usage](https://stripe.com/docs/api/payment_intents/confirm#confirm_payment_intent-setup_future_usage)
     *
     * Use `on_session` if you intend to only reuse the payment method when your customer is
     * present in your checkout flow. Use `off_session` if your customer may or may not be in
     * your checkout flow.
     *
     * Stripe uses `setup_future_usage` to dynamically optimize your payment flow and comply with
     * regional legislation and network rules. For example, if your customer is impacted by
     * [SCA](https://stripe.com/docs/strong-customer-authentication), using `off_session` will
     * ensure that they are authenticated while processing this PaymentIntent. You will then be
     * able to collect [off-session payments](https://stripe.com/docs/payments/cards/charging-saved-cards#off-session-payments-with-saved-cards)
     * for this customer.
     *
     * If `setup_future_usage` is already set, you may only update the value
     * from `on_session` to `off_session`.
     */
    enum class SetupFutureUsage(
        internal val code: String
    ) {
        /**
         * Use `on_session` if you intend to only reuse the payment method when your customer
         * is present in your checkout flow.
         */
        OnSession("on_session"),

        /**
         * Use `off_session` if your customer may or may not be in your checkout flow.
         */
        OffSession("off_session")
    }

    /**
     * Shipping information for this PaymentIntent.
     *
     * [shipping](https://stripe.com/docs/api/payment_intents/confirm#confirm_payment_intent-shipping)
     */
    @Parcelize
    data class Shipping @JvmOverloads constructor(
        /**
         * [shipping.address](https://stripe.com/docs/api/payment_intents/confirm#confirm_payment_intent-shipping-address)
         *
         * Shipping address.
         */
        internal val address: Address,

        /**
         * [shipping.name](https://stripe.com/docs/api/payment_intents/confirm#confirm_payment_intent-shipping-name)
         *
         * Recipient name.
         */
        internal val name: String,

        /**
         * [shipping.carrier](https://stripe.com/docs/api/payment_intents/confirm#confirm_payment_intent-shipping-carrier)
         *
         * The delivery service that shipped a physical product, such as Fedex, UPS, USPS, etc.
         */
        internal val carrier: String? = null,

        /**
         * [shipping.phone](https://stripe.com/docs/api/payment_intents/confirm#confirm_payment_intent-shipping-phone)
         *
         * Recipient phone (including extension).
         */
        internal val phone: String? = null,

        /**
         * [shipping.tracking_number](https://stripe.com/docs/api/payment_intents/confirm#confirm_payment_intent-shipping-tracking_number)
         *
         * The tracking number for a physical product, obtained from the delivery service.
         * If multiple tracking numbers were generated for this purchase, please separate
         * them with commas.
         */
        internal val trackingNumber: String? = null
    ) : StripeParamsModel, Parcelable {
        override fun toParamMap(): Map<String, Any> {
            return listOf(
                PARAM_ADDRESS to address.toParamMap(),
                PARAM_NAME to name,
                PARAM_CARRIER to carrier,
                PARAM_PHONE to phone,
                PARAM_TRACKING_NUMBER to trackingNumber
            ).fold(emptyMap()) { acc, (key, value) ->
                acc.plus(
                    value?.let { mapOf(key to it) }.orEmpty()
                )
            }
        }

        private companion object {
            private const val PARAM_ADDRESS = "address"
            private const val PARAM_NAME = "name"
            private const val PARAM_CARRIER = "carrier"
            private const val PARAM_PHONE = "phone"
            private const val PARAM_TRACKING_NUMBER = "tracking_number"
        }
    }

    companion object {
        const val PARAM_SOURCE_DATA: String = "source_data"

        internal const val PARAM_SOURCE_ID = "source"
        internal const val PARAM_SAVE_PAYMENT_METHOD = "save_payment_method"
        internal const val PARAM_PAYMENT_METHOD_OPTIONS = "payment_method_options"
        private const val PARAM_SETUP_FUTURE_USAGE = "setup_future_usage"
        private const val PARAM_SHIPPING = "shipping"

        /**
         * Create a [ConfirmPaymentIntentParams] without a payment method.
         */
        @JvmOverloads
        @JvmStatic
        fun create(
            clientSecret: String,
            returnUrl: String? = null,
            extraParams: Map<String, Any>? = null,
            shipping: Shipping? = null
        ): ConfirmPaymentIntentParams {
            return ConfirmPaymentIntentParams(
                clientSecret = clientSecret,
                returnUrl = returnUrl,
                extraParams = extraParams,
                shipping = shipping
            )
        }

        /**
         * Create the parameters necessary for confirming a PaymentIntent while attaching a
         * PaymentMethod that already exits.
         *
         * @param paymentMethodId the ID of the PaymentMethod that is being attached to the
         * PaymentIntent being confirmed
         * @param clientSecret client secret from the PaymentIntent being confirmed
         * @param returnUrl the URL the customer should be redirected to after the authorization
         * process
         * @param savePaymentMethod Set to `true` to save this PaymentIntent’s payment method to
         * the associated Customer, if the payment method is not already
         * attached. This parameter only applies to the payment method passed
         * in the same request or the current payment method attached to the
         * PaymentIntent and must be specified again if a new payment method is
         * added.
         * @param paymentMethodOptions Optional [PaymentMethodOptionsParams]
         * @param mandateId Optional ID of the Mandate to be used for this payment.
         * @param mandateData Optional details about the Mandate to create.
         * @param setupFutureUsage Optional. See [SetupFutureUsage].
         * @param shipping Optional. See [Shipping].
         */
        @JvmOverloads
        @JvmStatic
        fun createWithPaymentMethodId(
            paymentMethodId: String,
            clientSecret: String,
            returnUrl: String? = null,
            savePaymentMethod: Boolean? = null,
            extraParams: Map<String, Any>? = null,
            paymentMethodOptions: PaymentMethodOptionsParams? = null,
            mandateId: String? = null,
            mandateData: MandateDataParams? = null,
            setupFutureUsage: SetupFutureUsage? = null,
            shipping: Shipping? = null
        ): ConfirmPaymentIntentParams {
            return ConfirmPaymentIntentParams(
                clientSecret = clientSecret,
                paymentMethodId = paymentMethodId,
                returnUrl = returnUrl,
                savePaymentMethod = savePaymentMethod,
                extraParams = extraParams,
                paymentMethodOptions = paymentMethodOptions,
                mandateId = mandateId,
                mandateData = mandateData,
                setupFutureUsage = setupFutureUsage,
                shipping = shipping
            )
        }

        /**
         * Create the parameters necessary for confirming a PaymentIntent while attaching
         * [PaymentMethodCreateParams] data
         *
         * @param paymentMethodCreateParams params for the PaymentMethod that will be attached to this
         * PaymentIntent
         * @param clientSecret client secret from the PaymentIntent that is to be confirmed
         * @param returnUrl the URL the customer should be redirected to after the authorization
         * process
         * @param savePaymentMethod Set to `true` to save this PaymentIntent’s payment method to
         * the associated Customer, if the payment method is not already
         * attached. This parameter only applies to the payment method passed
         * in the same request or the current payment method attached to the
         * PaymentIntent and must be specified again if a new payment method is
         * added.
         * @param mandateId optional ID of the Mandate to be used for this payment.
         * @param mandateData optional details about the Mandate to create.
         * @param setupFutureUsage Optional. See [SetupFutureUsage].
         * @param shipping Optional. See [Shipping].
         */
        @JvmOverloads
        @JvmStatic
        fun createWithPaymentMethodCreateParams(
            paymentMethodCreateParams: PaymentMethodCreateParams,
            clientSecret: String,
            returnUrl: String? = null,
            savePaymentMethod: Boolean? = null,
            extraParams: Map<String, Any>? = null,
            mandateId: String? = null,
            mandateData: MandateDataParams? = null,
            setupFutureUsage: SetupFutureUsage? = null,
            shipping: Shipping? = null
        ): ConfirmPaymentIntentParams {
            return ConfirmPaymentIntentParams(
                clientSecret = clientSecret,
                paymentMethodCreateParams = paymentMethodCreateParams,
                returnUrl = returnUrl,
                savePaymentMethod = savePaymentMethod,
                extraParams = extraParams,
                mandateId = mandateId,
                mandateData = mandateData,
                setupFutureUsage = setupFutureUsage,
                shipping = shipping
            )
        }

        /**
         * Create the parameters necessary for confirming a PaymentIntent with an
         * existing [Source].
         *
         * @param sourceId the ID of the source that is being attached to the PaymentIntent being
         * confirmed
         * @param clientSecret client secret from the PaymentIntent being confirmed
         * @param returnUrl the URL the customer should be redirected to after the authorization
         * process
         * @param savePaymentMethod Set to `true` to save this PaymentIntent’s source to the
         * associated Customer, if the source is not already attached.
         * This parameter only applies to the source passed in the same request
         * or the current source attached to the PaymentIntent and must be
         * specified again if a new source is added.
         * @param shipping Optional. See [Shipping].
         */
        @JvmOverloads
        @JvmStatic
        fun createWithSourceId(
            sourceId: String,
            clientSecret: String,
            returnUrl: String,
            savePaymentMethod: Boolean? = null,
            extraParams: Map<String, Any>? = null,
            shipping: Shipping? = null
        ): ConfirmPaymentIntentParams {
            return ConfirmPaymentIntentParams(
                clientSecret = clientSecret,
                sourceId = sourceId,
                returnUrl = returnUrl,
                savePaymentMethod = savePaymentMethod,
                extraParams = extraParams,
                shipping = shipping
            )
        }

        /**
         * Create the parameters necessary for confirming a PaymentIntent with [SourceParams]
         *
         * @param sourceParams params for the source that will be attached to this PaymentIntent
         * @param clientSecret client secret from the PaymentIntent that is to be confirmed
         * @param returnUrl the URL the customer should be redirected to after the authorization
         * process
         * @param savePaymentMethod Set to `true` to save this PaymentIntent’s source to the
         * associated Customer, if the source is not already attached.
         * This parameter only applies to the source passed in the same request
         * or the current source attached to the PaymentIntent and must be
         * specified again if a new source is added.
         * @param shipping Optional. See [Shipping].
         */
        @JvmOverloads
        @JvmStatic
        fun createWithSourceParams(
            sourceParams: SourceParams,
            clientSecret: String,
            returnUrl: String,
            savePaymentMethod: Boolean? = null,
            extraParams: Map<String, Any>? = null,
            shipping: Shipping? = null
        ): ConfirmPaymentIntentParams {
            return ConfirmPaymentIntentParams(
                clientSecret = clientSecret,
                sourceParams = sourceParams,
                returnUrl = returnUrl,
                savePaymentMethod = savePaymentMethod,
                extraParams = extraParams,
                shipping = shipping
            )
        }
    }
}
