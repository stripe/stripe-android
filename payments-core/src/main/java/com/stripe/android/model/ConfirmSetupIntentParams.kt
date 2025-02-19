package com.stripe.android.model

import androidx.annotation.RestrictTo
import com.stripe.android.model.ConfirmStripeIntentParams.Companion.PARAM_CLIENT_SECRET
import com.stripe.android.model.ConfirmStripeIntentParams.Companion.PARAM_MANDATE_DATA
import com.stripe.android.model.ConfirmStripeIntentParams.Companion.PARAM_MANDATE_ID
import com.stripe.android.model.ConfirmStripeIntentParams.Companion.PARAM_PAYMENT_METHOD_DATA
import com.stripe.android.model.ConfirmStripeIntentParams.Companion.PARAM_PAYMENT_METHOD_ID
import com.stripe.android.model.ConfirmStripeIntentParams.Companion.PARAM_RETURN_URL
import com.stripe.android.model.ConfirmStripeIntentParams.Companion.PARAM_SET_AS_DEFAULT_PAYMENT_METHOD
import com.stripe.android.model.ConfirmStripeIntentParams.Companion.PARAM_USE_STRIPE_SDK
import kotlinx.parcelize.Parcelize

/**
 * Model representing parameters for [confirming a SetupIntent](https://stripe.com/docs/api/setup_intents/confirm).
 */
@Parcelize
data class ConfirmSetupIntentParams
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
constructor(
    @get:JvmSynthetic override val clientSecret: String,

    /**
     * ID of the payment method (a PaymentMethod, Card, or saved Source object) to attach to this
     * SetupIntent.
     */
    @get:JvmSynthetic internal val paymentMethodId: String? = null,

    @get:JvmSynthetic internal val paymentMethodCreateParams: PaymentMethodCreateParams? = null,

    /**
     * The URL to redirect your customer back to after they authenticate on the payment method’s
     * app or site. If you’d prefer to redirect to a mobile application, you can alternatively
     * supply an application URI scheme. This parameter is only used for cards and other
     * redirect-based payment methods.
     */
    override var returnUrl: String? = null,

    private val useStripeSdk: Boolean = false,

    /**
     * ID of the mandate to be used for this payment.
     */
    var mandateId: String? = null,

    /**
     * This hash contains details about the Mandate to create.
     *
     * See [mandate_data](https://stripe.com/docs/api/setup_intents/confirm#confirm_setup_intent-mandate_data).
     */
    var mandateData: MandateDataParams? = null,

    /**
     * Indicates that this should be the default payment method going forward
     */
    internal val setAsDefaultPaymentMethod: Boolean? = null,
    
) : ConfirmStripeIntentParams {

    override fun shouldUseStripeSdk(): Boolean {
        return useStripeSdk
    }

    override fun withShouldUseStripeSdk(shouldUseStripeSdk: Boolean): ConfirmSetupIntentParams {
        return copy(useStripeSdk = shouldUseStripeSdk)
    }

    override fun toParamMap(): Map<String, Any> {
        return mapOf(
            PARAM_CLIENT_SECRET to clientSecret,
            PARAM_USE_STRIPE_SDK to useStripeSdk
        ).plus(
            returnUrl?.let {
                mapOf(PARAM_RETURN_URL to it)
            }.orEmpty()
        ).plus(
            mandateId?.let {
                mapOf(PARAM_MANDATE_ID to it)
            }.orEmpty()
        ).plus(
            mandateDataParams?.let {
                mapOf(PARAM_MANDATE_DATA to it)
            }.orEmpty()
        ).plus(
            setAsDefaultPaymentMethod?.let {
                mapOf(PARAM_SET_AS_DEFAULT_PAYMENT_METHOD to it)
            }.orEmpty()
        ).plus(paymentMethodParamMap)
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
                else -> {
                    emptyMap()
                }
            }
        }

    /**
     * Use the user-defined [MandateDataParams] if specified, otherwise create a default
     * [MandateDataParams] if necessary.
     */
    private val mandateDataParams: Map<String, Any>?
        get() {
            return mandateData?.toParamMap()
                ?: if (paymentMethodCreateParams?.requiresMandate == true && mandateId == null) {
                    // Populate with default "online" MandateData
                    MandateDataParams(MandateDataParams.Type.Online.DEFAULT).toParamMap()
                } else {
                    null
                }
        }

    companion object {

        /**
         * Create the parameters necessary for confirming a [SetupIntent] based on its [clientSecret]
         * and [paymentMethodType]
         *
         * Use this initializer for SetupIntents that already have a PaymentMethod attached.
         *
         * @param clientSecret client secret from the PaymentIntent that is to be confirmed
         * @param paymentMethodType the known type of the SetupIntent's attached PaymentMethod
         */
        @JvmStatic
        fun create(
            clientSecret: String,
            paymentMethodType: PaymentMethod.Type
        ): ConfirmSetupIntentParams {
            return ConfirmSetupIntentParams(
                clientSecret = clientSecret,
                // infers default [MandateDataParams] based on the attached [paymentMethodType]
                mandateData = MandateDataParams(MandateDataParams.Type.Online.DEFAULT)
                    .takeIf { paymentMethodType.requiresMandate }
            )
        }

        /**
         * Create the parameters necessary for confirming a SetupIntent, without specifying a payment method
         * to attach to the SetupIntent. Only use this if a payment method has already been attached
         * to the SetupIntent.
         *
         * @param clientSecret The client secret of this SetupIntent. Used for client-side retrieval using a publishable key.
         *
         * @return params that can be use to confirm a SetupIntent
         */
        @JvmStatic
        fun createWithoutPaymentMethod(
            clientSecret: String
        ): ConfirmSetupIntentParams {
            return ConfirmSetupIntentParams(
                clientSecret = clientSecret
            )
        }

        /**
         * Create the parameters necessary for confirming a SetupIntent while attaching a
         * PaymentMethod that already exits.
         *
         * @param paymentMethodId ID of the payment method (a PaymentMethod, Card, BankAccount, or
         * saved Source object) to attach to this SetupIntent.
         * @param clientSecret The client secret of this SetupIntent. Used for client-side retrieval using a publishable key.
         * @param mandateData optional details about the Mandate to create.
         * @param mandateId optional ID of the Mandate to be used for this payment.
         *
         * @return params that can be use to confirm a SetupIntent
         */
        @JvmStatic
        @JvmOverloads
        fun create(
            paymentMethodId: String,
            clientSecret: String,
            mandateData: MandateDataParams? = null,
            mandateId: String? = null
        ): ConfirmSetupIntentParams {
            return ConfirmSetupIntentParams(
                clientSecret = clientSecret,
                paymentMethodId = paymentMethodId,
                mandateId = mandateId,
                mandateData = mandateData
            )
        }

        /**
         * Create the parameters necessary for confirming a SetupIntent with a new PaymentMethod
         *
         * @param paymentMethodCreateParams the params to create a new PaymentMethod that will be
         * attached to the SetupIntent being confirmed
         * @param clientSecret The client secret of this SetupIntent. Used for client-side retrieval using a publishable key.
         * @param mandateData optional details about the Mandate to create.
         * @param mandateId optional ID of the Mandate to be used for this payment.
         *
         * @return params that can be use to confirm a SetupIntent
         */
        @JvmOverloads
        @JvmStatic
        fun create(
            paymentMethodCreateParams: PaymentMethodCreateParams,
            clientSecret: String,
            mandateData: MandateDataParams? = null,
            mandateId: String? = null,
            setAsDefaultPaymentMethod: Boolean? = null,
        ): ConfirmSetupIntentParams {
            return ConfirmSetupIntentParams(
                clientSecret = clientSecret,
                paymentMethodCreateParams = paymentMethodCreateParams,
                mandateId = mandateId,
                mandateData = mandateData,
                setAsDefaultPaymentMethod = setAsDefaultPaymentMethod
            )
        }
    }
}
