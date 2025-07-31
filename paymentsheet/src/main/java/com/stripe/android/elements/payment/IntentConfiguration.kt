package com.stripe.android.elements.payment

import android.os.Parcelable
import com.stripe.android.SharedPaymentTokenSessionPreview
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentelement.PaymentMethodOptionsSetupFutureUsagePreview
import com.stripe.android.paymentelement.confirmation.intent.IntentConfirmationInterceptor
import dev.drewhamilton.poko.Poko
import kotlinx.parcelize.Parcelize

/**
 * Contains information needed to render [PaymentSheet]. The values are used to calculate
 * the payment methods displayed and influence the UI.
 *
 * **Note**: The [PaymentIntent] or [SetupIntent] you create on your server must have the same
 * values or the payment/setup will fail.
 *
 * @param mode Whether [PaymentSheet] should present a payment or setup flow.
 * @param paymentMethodTypes The payment methods types to display. If empty, we dynamically
 * determine the payment method types using your
 * [Stripe Dashboard settings](https://dashboard.stripe.com/settings/payment_methods).
 * @param paymentMethodConfigurationId The configuration ID (if any) for the selected payment method configuration.
 * See https://stripe.com/docs/payments/multiple-payment-method-configs for more information.
 * @param onBehalfOf The account (if any) for which the funds of the intent are intended. See
 * [our docs](https://stripe.com/docs/api/payment_intents/object#payment_intent_object-on_behalf_of) for more info.
 */
@Poko
@Parcelize
class IntentConfiguration internal constructor(
    val mode: Mode,
    val paymentMethodTypes: List<String> = emptyList(),
    val paymentMethodConfigurationId: String? = null,
    val onBehalfOf: String? = null,
    internal val requireCvcRecollection: Boolean = false,
    internal val intentBehavior: IntentBehavior = IntentBehavior.Default,
) : Parcelable {
    @JvmOverloads
    constructor(
        mode: Mode,
        paymentMethodTypes: List<String> = emptyList(),
        paymentMethodConfigurationId: String? = null,
        onBehalfOf: String? = null,
        requireCvcRecollection: Boolean = false,
    ) : this(
        mode = mode,
        paymentMethodTypes = paymentMethodTypes,
        paymentMethodConfigurationId = paymentMethodConfigurationId,
        onBehalfOf = onBehalfOf,
        requireCvcRecollection = requireCvcRecollection,
        intentBehavior = IntentBehavior.Default,
    )

    @SharedPaymentTokenSessionPreview
    @JvmOverloads
    constructor(
        sharedPaymentTokenSessionWithMode: Mode,
        sellerDetails: SellerDetails?,
        paymentMethodTypes: List<String> = emptyList(),
        paymentMethodConfigurationId: String? = null,
        onBehalfOf: String? = null,
        requireCvcRecollection: Boolean = false,
    ) : this(
        mode = sharedPaymentTokenSessionWithMode,
        paymentMethodTypes = paymentMethodTypes,
        paymentMethodConfigurationId = paymentMethodConfigurationId,
        onBehalfOf = onBehalfOf,
        requireCvcRecollection = requireCvcRecollection,
        intentBehavior = IntentBehavior.SharedPaymentToken(sellerDetails),
    )

    /**
     * Contains information about the desired payment or setup flow.
     */
    sealed class Mode : Parcelable {

        internal abstract val setupFutureUse: SetupFutureUse?
        internal abstract val captureMethod: CaptureMethod?

        /**
         * Use this if your integration creates a [PaymentIntent].
         *
         * @param amount Amount intended to be collected in the smallest currency unit
         * (e.g. 100 cents to charge $1.00). Shown in Google Pay, Buy now pay later UIs, the Pay
         * button, and influences available payment methods.
         * See [our docs](https://stripe.com/docs/api/payment_intents/create#create_payment_intent-amount)
         * for more info.
         * @param currency Three-letter ISO currency code. Filters out payment methods based on
         * supported currency.
         * See [our docs](https://stripe.com/docs/api/payment_intents/create#create_payment_intent-currency)
         * for more info.
         * @param setupFutureUse Indicates that you intend to make future payments. See
         * [our docs](https://stripe.com/docs/api/payment_intents/create#create_payment_intent-setup_future_usage)
         * for more info.
         * @param captureMethod Controls when the funds will be captured from the customer's
         * account. See
         * [our docs](https://stripe.com/docs/api/payment_intents/create#create_payment_intent-capture_method)
         * for more info.
         * @param paymentMethodOptions Additional payment method options params. See
         * [our docs](https://docs.stripe.com/api/payment_intents/create#create_payment_intent-payment_method_options)
         * for more info.
         */
        @Poko
        @Parcelize
        @OptIn(PaymentMethodOptionsSetupFutureUsagePreview::class)
        class Payment @JvmOverloads constructor(
            val amount: Long,
            val currency: String,
            override val setupFutureUse: SetupFutureUse? = null,
            override val captureMethod: CaptureMethod = CaptureMethod.Automatic,
            internal val paymentMethodOptions: PaymentMethodOptions?
        ) : Mode() {

            constructor(
                amount: Long,
                currency: String,
                setupFutureUse: SetupFutureUse? = null,
                captureMethod: CaptureMethod = CaptureMethod.Automatic,
            ) : this(
                amount = amount,
                currency = currency,
                setupFutureUse = setupFutureUse,
                captureMethod = captureMethod,
                paymentMethodOptions = null
            )

            @Parcelize
            @Poko
            @PaymentMethodOptionsSetupFutureUsagePreview
            class PaymentMethodOptions(
                internal val setupFutureUsageValues: Map<PaymentMethod.Type, SetupFutureUse>
            ) : Parcelable
        }

        /**
         * Use this if your integration creates a [SetupIntent].
         *
         * @param currency Three-letter ISO currency code. Filters out payment methods based on
         * supported currency. See
         * [our docs](https://stripe.com/docs/api/payment_intents/create#create_payment_intent-currency) for more info.
         * @param setupFutureUse Indicates that you intend to make future payments. See
         * [our docs](https://stripe.com/docs/api/payment_intents/create#create_payment_intent-setup_future_usage)
         * for more info.
         */
        @Poko
        @Parcelize
        class Setup @JvmOverloads constructor(
            val currency: String? = null,
            override val setupFutureUse: SetupFutureUse = SetupFutureUse.OffSession,
        ) : Mode() {

            override val captureMethod: CaptureMethod?
                get() = null
        }
    }

    /**
     * Indicates that you intend to make future payments with this [PaymentIntent]'s payment
     * method. See
     * [our docs](https://stripe.com/docs/api/payment_intents/create#create_payment_intent-setup_future_usage)
     * for more info.
     */
    enum class SetupFutureUse {

        /**
         * Use this if you intend to only reuse the payment method when your customer is present
         * in your checkout flow.
         */
        OnSession,

        /**
         * Use this if your customer may or may not be present in your checkout flow.
         */
        OffSession,

        /**
         * Use none if you do not intend to reuse this payment method and want to override the top-level
         * setup_future_usage value for this payment method.
         */
        None
    }

    /**
     * Controls when the funds will be captured.
     *
     * See [docs](https://stripe.com/docs/api/payment_intents/create#create_payment_intent-capture_method).
     */
    enum class CaptureMethod {

        /**
         * Stripe automatically captures funds when the customer authorizes the payment.
         */
        Automatic,

        /**
         * Stripe asynchronously captures funds when the customer authorizes the payment.
         * Recommended over [CaptureMethod.Automatic] due to improved latency, but may require
         * additional integration changes.
         */
        AutomaticAsync,

        /**
         * Place a hold on the funds when the customer authorizes the payment, but don't capture
         * the funds until later.
         *
         * **Note**: Not all payment methods support this.
         */
        Manual,
    }

    @SharedPaymentTokenSessionPreview
    @Parcelize
    @Poko
    class SellerDetails(
        val networkId: String,
        val externalId: String,
    ) : Parcelable

    @OptIn(SharedPaymentTokenSessionPreview::class)
    internal sealed interface IntentBehavior : Parcelable {
        @Parcelize
        data object Default : IntentBehavior

        @Parcelize
        data class SharedPaymentToken(
            val sellerDetails: SellerDetails?,
        ) : IntentBehavior
    }

    companion object {

        /**
         * Pass this as the client secret into [CreateIntentCallback.Result.Success] to force
         * [PaymentSheet] to show success, dismiss the sheet without confirming the intent, and
         * return [PaymentSheetResult.Completed].
         *
         * **Note**: If provided, the SDK performs no action to complete the payment or setup.
         * It doesn't confirm a [PaymentIntent] or [SetupIntent] or handle next actions. You
         * should only use this if your integration can't create a [PaymentIntent] or
         * [SetupIntent]. It is your responsibility to ensure that you only pass this value if
         * the payment or setup is successful.
         */
        @DelicatePaymentSheetApi
        const val COMPLETE_WITHOUT_CONFIRMING_INTENT =
            IntentConfirmationInterceptor.COMPLETE_WITHOUT_CONFIRMING_INTENT
    }
}
