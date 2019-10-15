package com.stripe.android.model

import androidx.annotation.VisibleForTesting
import com.stripe.android.ObjectBuilder
import com.stripe.android.model.ConfirmStripeIntentParams.Companion.API_PARAM_CLIENT_SECRET
import com.stripe.android.model.ConfirmStripeIntentParams.Companion.API_PARAM_PAYMENT_METHOD_DATA
import com.stripe.android.model.ConfirmStripeIntentParams.Companion.API_PARAM_PAYMENT_METHOD_ID
import com.stripe.android.model.ConfirmStripeIntentParams.Companion.API_PARAM_RETURN_URL
import com.stripe.android.model.ConfirmStripeIntentParams.Companion.API_PARAM_USE_STRIPE_SDK

data class ConfirmSetupIntentParams internal constructor(
    override val clientSecret: String,
    private val paymentMethodId: String?,
    val paymentMethodCreateParams: PaymentMethodCreateParams?,
    private val returnUrl: String?,
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
        val params = mutableMapOf(
            API_PARAM_CLIENT_SECRET to clientSecret,
            API_PARAM_USE_STRIPE_SDK to useStripeSdk
        )

        if (paymentMethodCreateParams != null) {
            params[API_PARAM_PAYMENT_METHOD_DATA] = paymentMethodCreateParams.toParamMap()
            if (paymentMethodCreateParams.type.hasMandate) {
                params[MandateData.API_PARAM_MANDATE_DATA] = MandateData().toParamMap()
            }
        } else if (paymentMethodId != null) {
            params[API_PARAM_PAYMENT_METHOD_ID] = paymentMethodId
        }

        if (returnUrl != null) {
            params[API_PARAM_RETURN_URL] = returnUrl
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

    @VisibleForTesting
    internal class Builder internal constructor(
        private val clientSecret: String
    ) : ObjectBuilder<ConfirmSetupIntentParams> {
        private var paymentMethodId: String? = null
        private var paymentMethodCreateParams: PaymentMethodCreateParams? = null
        private var returnUrl: String? = null
        private var useStripeSdk: Boolean = false

        internal fun setPaymentMethodId(paymentMethodId: String): Builder {
            this.paymentMethodId = paymentMethodId
            return this
        }

        internal fun setPaymentMethodCreateParams(
            paymentMethodCreateParams: PaymentMethodCreateParams
        ): Builder {
            this.paymentMethodCreateParams = paymentMethodCreateParams
            return this
        }

        internal fun setReturnUrl(returnUrl: String?): Builder {
            this.returnUrl = returnUrl
            return this
        }

        internal fun setShouldUseSdk(useStripeSdk: Boolean): Builder {
            this.useStripeSdk = useStripeSdk
            return this
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
         * Create the parameters necessary for confirming a SetupIntent while attaching a
         * PaymentMethod that already exits.
         *
         * @param paymentMethodId the ID of the PaymentMethod that is being attached to the
         * SetupIntent being confirmed
         * @param clientSecret client secret from the SetupIntent being confirmed
         * @param returnUrl the URL the customer should be redirected to after the authorization process
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
         * @param clientSecret client secret from the SetupIntent being confirmed
         * @param returnUrl the URL the customer should be redirected to after the authorization
         * process
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
