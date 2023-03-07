package com.stripe.android

import androidx.annotation.RestrictTo
import com.stripe.android.CreateIntentCallback.Result
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@StripeDeferredIntentCreationBetaApi
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun interface CreateIntentCallback {

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    sealed interface Result {

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        data class Success(val clientSecret: String) : Result

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        data class Failure(val error: Throwable) : Result
    }

    /**
     * 🚧 Under construction 🚧
     * Called when the customer confirms payment.
     * Your implementation should create and confirm a [PaymentIntent] or [SetupIntent] on your
     * server and call [onIntentCreated] with its client secret or an error if one occurred.
     *
     * Note: You must create the PaymentIntent or SetupIntent with the same values used as the
     * `IntentConfiguration` e.g. the same amount, currency, etc.
     *
     * @param paymentMethodId the payment method ID to create the intent with
     *
     * @return a [Result] which contains the intent client secret
     */
    suspend fun onIntentCreated(
        paymentMethodId: String,
    ): Result
}

/**
 * 🚧 Under construction 🚧
 * The legacy version of the [CreateIntentCallback]. Callback style interface with java support.
 */
@StripeDeferredIntentCreationBetaApi
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun interface LegacyCreateIntentCallback : CreateIntentCallback {

    /**
     * 🚧 Under construction 🚧
     * Called when the customer confirms payment.
     * Your implementation should create and confirm a [PaymentIntent] or [SetupIntent] on your
     * server and call [onIntentCreated] with its client secret or an error if one occurred.
     *
     * Note: You must create the PaymentIntent or SetupIntent with the same values used as the
     * `IntentConfiguration` e.g. the same amount, currency, etc.
     *
     * @param paymentMethodId the payment method ID to create the intent with
     *
     * @return a [Result] which contains the intent client secret
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    override suspend fun onIntentCreated(paymentMethodId: String): Result {
        return suspendCoroutine { continuation ->
            onIntentCreated(
                paymentMethodId = paymentMethodId,
                resultListener = continuation::resume,
            )
        }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun onIntentCreated(
        paymentMethodId: String,
        resultListener: ResultListener,
    )

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun interface ResultListener {
        fun onResult(result: Result)
    }
}

@StripeDeferredIntentCreationBetaApi
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun interface CreateIntentCallbackForServerSideConfirmation {

    /**
     * 🚧 Under construction 🚧
     * For advanced users who need server-side confirmation.
     * Called when the customer confirms payment.
     * Your implementation should create and confirm a [PaymentIntent] or [SetupIntent] on your
     * server and call [onIntentCreated] with its client secret or an error if one occurred.
     *
     * Note: You must create the PaymentIntent or SetupIntent with the same values used as the
     * `IntentConfiguration` e.g. the same amount, currency, etc.
     *
     * @param paymentMethodId the payment method ID to create the intent with
     * @param customerRequestedSave whether or not the customer requested to save the payment
     * method. This is true if the customer selected the "Save this payment method for future use"
     * checkbox.
     *
     * @return a [Result] which contains the intent client secret
     */
    suspend fun onIntentCreated(
        paymentMethodId: String,
        customerRequestedSave: Boolean,
    ): Result
}

/**
 * 🚧 Under construction 🚧
 * The legacy version of the [CreateIntentCallbackForServerSideConfirmation].
 * Callback style interface with java support.
 */
@StripeDeferredIntentCreationBetaApi
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun interface LegacyCreateIntentCallbackForServerSideConfirmation : CreateIntentCallbackForServerSideConfirmation {

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    override suspend fun onIntentCreated(
        paymentMethodId: String,
        customerRequestedSave: Boolean
    ): Result {
        return suspendCoroutine { continuation ->
            onIntentCreated(
                paymentMethodId = paymentMethodId,
                customerRequestedSave = customerRequestedSave,
                resultListener = continuation::resume,
            )
        }
    }

    /**
     * 🚧 Under construction 🚧
     * For advanced users who need server-side confirmation.
     * Called when the customer confirms payment.
     * Your implementation should create and confirm a [PaymentIntent] or [SetupIntent] on your
     * server and call [onIntentCreated] with its client secret or an error if one occurred.
     *
     * Note: You must create the PaymentIntent or SetupIntent with the same values used as the
     * `IntentConfiguration` e.g. the same amount, currency, etc.
     *
     * @param paymentMethodId the payment method ID to create the intent with
     * @param customerRequestedSave whether or not the customer requested to save the payment
     * method. This is true if the customer selected the "Save this payment method for future use"
     * checkbox.
     *
     * @return a [Result] which contains the intent client secret
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun onIntentCreated(
        paymentMethodId: String,
        customerRequestedSave: Boolean,
        resultListener: ResultListener,
    )

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun interface ResultListener {
        fun onResult(result: Result)
    }
}
