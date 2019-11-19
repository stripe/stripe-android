package com.stripe.android.model

import androidx.annotation.VisibleForTesting
import com.stripe.android.ObjectBuilder
import com.stripe.android.model.ConfirmStripeIntentParams.Companion.PARAM_CLIENT_SECRET
import com.stripe.android.model.ConfirmStripeIntentParams.Companion.PARAM_PAYMENT_METHOD_DATA
import com.stripe.android.model.ConfirmStripeIntentParams.Companion.PARAM_USE_STRIPE_SDK

data class ConfirmPaymentIntentParams private constructor(
    val paymentMethodCreateParams: PaymentMethodCreateParams?,
    val paymentMethodId: String?,
    val sourceParams: SourceParams?,
    val sourceId: String?,

    val extraParams: Map<String, Any>?,
    override val clientSecret: String,
    val returnUrl: String?,

    private val savePaymentMethod: Boolean,
    private val useStripeSdk: Boolean

) : ConfirmStripeIntentParams {

    fun shouldSavePaymentMethod(): Boolean {
        return savePaymentMethod
    }

    override fun shouldUseStripeSdk(): Boolean {
        return useStripeSdk
    }

    override fun withShouldUseStripeSdk(shouldUseStripeSdk: Boolean): ConfirmPaymentIntentParams {
        return toBuilder()
            .setShouldUseSdk(shouldUseStripeSdk)
            .build()
    }

    /**
     * Create a Map representing this object that is prepared for the Stripe API.
     */
    override fun toParamMap(): Map<String, Any> {
        val params: MutableMap<String, Any> = mutableMapOf(
            PARAM_CLIENT_SECRET to clientSecret,
            PARAM_SAVE_PAYMENT_METHOD to savePaymentMethod,
            PARAM_USE_STRIPE_SDK to useStripeSdk
        )

        if (paymentMethodCreateParams != null) {
            params[PARAM_PAYMENT_METHOD_DATA] = paymentMethodCreateParams.toParamMap().toMap()
            if (paymentMethodCreateParams.type.hasMandate) {
                params[MandateData.PARAM_MANDATE_DATA] = MandateData().toParamMap()
            }
        } else if (paymentMethodId != null) {
            params[ConfirmStripeIntentParams.PARAM_PAYMENT_METHOD_ID] = paymentMethodId
        } else if (sourceParams != null) {
            params[PARAM_SOURCE_DATA] = sourceParams.toParamMap().toMap()
        } else if (sourceId != null) {
            params[PARAM_SOURCE_ID] = sourceId
        }

        if (returnUrl != null) {
            params[ConfirmStripeIntentParams.PARAM_RETURN_URL] = returnUrl
        }
        if (extraParams != null) {
            params.putAll(extraParams)
        }

        return params.toMap()
    }

    @VisibleForTesting
    internal fun toBuilder(): Builder {
        return Builder(clientSecret)
            .setReturnUrl(returnUrl)
            .setSourceId(sourceId)
            .setSavePaymentMethod(savePaymentMethod)
            .setExtraParams(extraParams)
            .also { builder ->
                paymentMethodId?.let { builder.setPaymentMethodId(it) }
                paymentMethodCreateParams?.let { builder.setPaymentMethodCreateParams(it) }
                sourceParams?.let { builder.setSourceParams(sourceParams) }
            }
    }

    /**
     * Sets the client secret that is used to authenticate actions on this PaymentIntent
     * @param clientSecret client secret associated with this PaymentIntent
     */
    @VisibleForTesting
    internal class Builder internal constructor(
        internal val clientSecret: String
    ) : ObjectBuilder<ConfirmPaymentIntentParams> {
        private var paymentMethodCreateParams: PaymentMethodCreateParams? = null
        private var paymentMethodId: String? = null
        private var sourceParams: SourceParams? = null
        private var sourceId: String? = null

        private var extraParams: Map<String, Any>? = null
        private var returnUrl: String? = null

        private var savePaymentMethod: Boolean = false
        private var shouldUseSdk: Boolean = false

        /**
         * Sets the PaymentMethod data that will be included with this PaymentIntent
         *
         * @param paymentMethodCreateParams Params for the PaymentMethod that will be attached to
         * this PaymentIntent. Only one of PaymentMethodParam,
         * PaymentMethodId, SourceParam, SourceId should be used
         * at a time.
         */
        internal fun setPaymentMethodCreateParams(
            paymentMethodCreateParams: PaymentMethodCreateParams
        ): Builder {
            this.paymentMethodCreateParams = paymentMethodCreateParams
            return this
        }

        /**
         * Sets a pre-existing PaymentMethod that will be attached to this PaymentIntent
         *
         * @param paymentMethodId The ID of the PaymentMethod that is being attached to this
         * PaymentIntent. Only one of PaymentMethodParam, PaymentMethodId,
         * SourceParam, SourceId should be used at a time.
         */
        internal fun setPaymentMethodId(paymentMethodId: String): Builder {
            this.paymentMethodId = paymentMethodId
            return this
        }

        /**
         * Sets the source data that will be included with this PaymentIntent
         *
         * @param sourceParams params for the source that will be attached to this PaymentIntent.
         * Only one of SourceParam and SourceId should be used at at time.
         */
        internal fun setSourceParams(sourceParams: SourceParams): Builder {
            this.sourceParams = sourceParams
            return this
        }

        /**
         * Sets a pre-existing source that will be attached to this PaymentIntent
         * @param sourceId the ID of an existing Source that is being attached to this
         * PaymentIntent. Only one of SourceParam and SourceId should be used at a
         * time.
         */
        internal fun setSourceId(sourceId: String?): Builder {
            this.sourceId = sourceId
            return this
        }

        /**
         * @param returnUrl the URL the customer should be redirected to after the authentication
         * process
         */
        internal fun setReturnUrl(returnUrl: String?): Builder {
            this.returnUrl = returnUrl
            return this
        }

        /**
         * @param extraParams params that will be included in the request. Incorrect params may
         * result in errors when connecting to Stripe's API.
         */
        internal fun setExtraParams(extraParams: Map<String, Any>?): Builder {
            this.extraParams = extraParams
            return this
        }

        internal fun setSavePaymentMethod(savePaymentMethod: Boolean): Builder {
            this.savePaymentMethod = savePaymentMethod
            return this
        }

        internal fun setShouldUseSdk(shouldUseSdk: Boolean): Builder {
            this.shouldUseSdk = shouldUseSdk
            return this
        }

        override fun build(): ConfirmPaymentIntentParams {
            return ConfirmPaymentIntentParams(
                clientSecret = clientSecret,
                returnUrl = returnUrl,
                paymentMethodId = paymentMethodId,
                paymentMethodCreateParams = paymentMethodCreateParams,
                sourceId = sourceId,
                sourceParams = sourceParams,
                savePaymentMethod = savePaymentMethod,
                extraParams = extraParams,
                useStripeSdk = shouldUseSdk
            )
        }
    }

    companion object {
        const val PARAM_SOURCE_DATA: String = "source_data"

        internal const val PARAM_SOURCE_ID = "source"
        internal const val PARAM_SAVE_PAYMENT_METHOD = "save_payment_method"

        /**
         * Create a [ConfirmPaymentIntentParams] without a payment method.
         */
        @JvmOverloads
        @JvmStatic
        fun create(
            clientSecret: String,
            returnUrl: String? = null,
            extraParams: Map<String, Any>? = null
        ): ConfirmPaymentIntentParams {
            return Builder(clientSecret)
                .setReturnUrl(returnUrl)
                .setExtraParams(extraParams)
                .build()
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
         */
        @JvmOverloads
        @JvmStatic
        fun createWithPaymentMethodId(
            paymentMethodId: String,
            clientSecret: String,
            returnUrl: String? = null,
            savePaymentMethod: Boolean = false,
            extraParams: Map<String, Any>? = null
        ): ConfirmPaymentIntentParams {
            return Builder(clientSecret)
                .setPaymentMethodId(paymentMethodId)
                .setReturnUrl(returnUrl)
                .setSavePaymentMethod(savePaymentMethod)
                .setExtraParams(extraParams)
                .build()
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
         */
        @JvmOverloads
        @JvmStatic
        fun createWithPaymentMethodCreateParams(
            paymentMethodCreateParams: PaymentMethodCreateParams,
            clientSecret: String,
            returnUrl: String? = null,
            savePaymentMethod: Boolean = false,
            extraParams: Map<String, Any>? = null
        ): ConfirmPaymentIntentParams {
            return Builder(clientSecret)
                .setPaymentMethodCreateParams(paymentMethodCreateParams)
                .setReturnUrl(returnUrl)
                .setSavePaymentMethod(savePaymentMethod)
                .setExtraParams(extraParams)
                .build()
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
         */
        @JvmOverloads
        @JvmStatic
        fun createWithSourceId(
            sourceId: String,
            clientSecret: String,
            returnUrl: String,
            savePaymentMethod: Boolean = false,
            extraParams: Map<String, Any>? = null
        ): ConfirmPaymentIntentParams {
            return Builder(clientSecret)
                .setSourceId(sourceId)
                .setReturnUrl(returnUrl)
                .setSavePaymentMethod(savePaymentMethod)
                .setExtraParams(extraParams)
                .build()
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
         */
        @JvmOverloads
        @JvmStatic
        fun createWithSourceParams(
            sourceParams: SourceParams,
            clientSecret: String,
            returnUrl: String,
            savePaymentMethod: Boolean = false,
            extraParams: Map<String, Any>? = null
        ): ConfirmPaymentIntentParams {
            return Builder(clientSecret)
                .setSourceParams(sourceParams)
                .setReturnUrl(returnUrl)
                .setSavePaymentMethod(savePaymentMethod)
                .setExtraParams(extraParams)
                .build()
        }
    }
}
