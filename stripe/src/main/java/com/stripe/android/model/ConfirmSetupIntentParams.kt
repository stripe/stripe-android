package com.stripe.android.model

import androidx.annotation.VisibleForTesting
import com.stripe.android.ObjectBuilder
import com.stripe.android.model.ConfirmStripeIntentParams.Companion.PARAM_CLIENT_SECRET
import com.stripe.android.model.ConfirmStripeIntentParams.Companion.PARAM_PAYMENT_METHOD_DATA
import com.stripe.android.model.ConfirmStripeIntentParams.Companion.PARAM_PAYMENT_METHOD_ID
import com.stripe.android.model.ConfirmStripeIntentParams.Companion.PARAM_RETURN_URL
import com.stripe.android.model.ConfirmStripeIntentParams.Companion.PARAM_USE_STRIPE_SDK

data class ConfirmSetupIntentParams internal constructor(
    @get:JvmSynthetic override val clientSecret: String,
    @get:JvmSynthetic internal val paymentMethodId: String? = null,
    @get:JvmSynthetic internal val paymentMethodCreateParams: PaymentMethodCreateParams? = null,
    private val returnUrl: String? = null,
    private val useStripeSdk: Boolean
) : ConfirmStripeIntentParams {

    override fun shouldUseStripeSdk(): Boolean {
        return useStripeSdk
    }

    override fun withShouldUseStripeSdk(shouldUseStripeSdk: Boolean): ConfirmSetupIntentParams {
        return toBuilder()
            .setShouldUseSdk(shouldUseStripeSdk)
            .build()
    }

    /**
     * Create a string-keyed map representing this object that is
     * ready to be sent over the network.
     *
     * @return a String-keyed map
     */
    override fun toParamMap(): Map<String, Any> {
        val params = mapOf(
            PARAM_CLIENT_SECRET to clientSecret,
            PARAM_USE_STRIPE_SDK to useStripeSdk
        ).plus(
            returnUrl?.let { mapOf(PARAM_RETURN_URL to it) }.orEmpty()
        ).toMutableMap()

        if (paymentMethodCreateParams != null) {
            params[PARAM_PAYMENT_METHOD_DATA] = paymentMethodCreateParams.toParamMap()
            if (paymentMethodCreateParams.type.hasMandate) {
                params[MandateData.PARAM_MANDATE_DATA] = MandateData().toParamMap()
            }
        } else if (paymentMethodId != null) {
            params[PARAM_PAYMENT_METHOD_ID] = paymentMethodId
        }

        return params.toMap()
    }

    @VisibleForTesting
    internal fun toBuilder(): Builder {
        val builder = Builder(clientSecret)
            .setReturnUrl(returnUrl)
            .setShouldUseSdk(useStripeSdk)

        paymentMethodId?.let { builder.setPaymentMethodId(it) }
        paymentMethodCreateParams?.let { builder.setPaymentMethodCreateParams(it) }

        return builder
    }

    internal class Builder internal constructor(
        private val clientSecret: String
    ) : ObjectBuilder<ConfirmSetupIntentParams> {
        private var paymentMethodId: String? = null
        private var paymentMethodCreateParams: PaymentMethodCreateParams? = null
        private var returnUrl: String? = null
        private var useStripeSdk: Boolean = false

        internal fun setPaymentMethodId(paymentMethodId: String): Builder = apply {
            this.paymentMethodId = paymentMethodId
        }

        internal fun setPaymentMethodCreateParams(
            paymentMethodCreateParams: PaymentMethodCreateParams
        ): Builder = apply {
            this.paymentMethodCreateParams = paymentMethodCreateParams
        }

        internal fun setReturnUrl(returnUrl: String?): Builder = apply {
            this.returnUrl = returnUrl
        }

        internal fun setShouldUseSdk(useStripeSdk: Boolean): Builder = apply {
            this.useStripeSdk = useStripeSdk
        }

        override fun build(): ConfirmSetupIntentParams {
            return ConfirmSetupIntentParams(
                clientSecret = clientSecret,
                returnUrl = returnUrl,
                paymentMethodId = paymentMethodId,
                paymentMethodCreateParams = paymentMethodCreateParams,
                useStripeSdk = useStripeSdk
            )
        }
    }

    companion object {
        /**
         * Create the parameters necessary for confirming a SetupIntent, without specifying a payment method
         * to attach to the SetupIntent. Only use this if a payment method has already been attached
         * to the SetupIntent.
         *
         * @param clientSecret The client secret of this SetupIntent. Used for client-side retrieval using a publishable key.
         * @param returnUrl The URL to redirect your customer back to after they authenticate on the payment method’s app or site.
         * If you’d prefer to redirect to a mobile application, you can alternatively supply an application URI scheme.
         * This parameter is only used for cards and other redirect-based payment methods.
         *
         * @return params that can be use to confirm a SetupIntent
         */
        @JvmStatic
        @JvmOverloads
        fun createWithoutPaymentMethod(
            clientSecret: String,
            returnUrl: String? = null
        ): ConfirmSetupIntentParams {
            return Builder(clientSecret)
                .setReturnUrl(returnUrl)
                .build()
        }

        /**
         * Create the parameters necessary for confirming a SetupIntent while attaching a
         * PaymentMethod that already exits.
         *
         * @param paymentMethodId ID of the payment method (a PaymentMethod, Card, BankAccount, or
         * saved Source object) to attach to this SetupIntent.
         * @param clientSecret The client secret of this SetupIntent. Used for client-side retrieval using a publishable key.
         * @param returnUrl The URL to redirect your customer back to after they authenticate on the payment method’s app or site.
         * If you’d prefer to redirect to a mobile application, you can alternatively supply an application URI scheme.
         * This parameter is only used for cards and other redirect-based payment methods.
         *
         * @return params that can be use to confirm a SetupIntent
         */
        @JvmStatic
        @JvmOverloads
        fun create(
            paymentMethodId: String,
            clientSecret: String,
            returnUrl: String? = null
        ): ConfirmSetupIntentParams {
            return Builder(clientSecret)
                .setReturnUrl(returnUrl)
                .setPaymentMethodId(paymentMethodId)
                .build()
        }

        /**
         * Create the parameters necessary for confirming a SetupIntent with a new PaymentMethod
         *
         * @param paymentMethodCreateParams the params to create a new PaymentMethod that will be
         * attached to the SetupIntent being confirmed
         * @param clientSecret The client secret of this SetupIntent. Used for client-side retrieval using a publishable key.
         * @param returnUrl The URL to redirect your customer back to after they authenticate on the payment method’s app or site.
         * If you’d prefer to redirect to a mobile application, you can alternatively supply an application URI scheme.
         * This parameter is only used for cards and other redirect-based payment methods.
         *
         * @return params that can be use to confirm a SetupIntent
         */
        @JvmOverloads
        @JvmStatic
        fun create(
            paymentMethodCreateParams: PaymentMethodCreateParams,
            clientSecret: String,
            returnUrl: String? = null
        ): ConfirmSetupIntentParams {
            return Builder(clientSecret)
                .setPaymentMethodCreateParams(paymentMethodCreateParams)
                .setReturnUrl(returnUrl)
                .build()
        }
    }
}
