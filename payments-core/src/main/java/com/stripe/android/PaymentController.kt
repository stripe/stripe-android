package com.stripe.android

import android.app.Activity
import android.content.Intent
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.RestrictTo
import androidx.fragment.app.Fragment
import com.stripe.android.exception.APIConnectionException
import com.stripe.android.exception.APIException
import com.stripe.android.exception.AuthenticationException
import com.stripe.android.exception.InvalidRequestException
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.ConfirmStripeIntentParams
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.Source
import com.stripe.android.model.StripeIntent
import com.stripe.android.model.WeChat
import com.stripe.android.model.WeChatPayNextAction
import com.stripe.android.networking.ApiRequest
import com.stripe.android.payments.PaymentFlowResult
import com.stripe.android.view.AuthActivityStarterHost

// originally made public for paymentsheet
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface PaymentController {
    /**
     * Confirm the Stripe Intent and resolve any next actions
     */
    suspend fun startConfirmAndAuth(
        host: AuthActivityStarterHost,
        confirmStripeIntentParams: ConfirmStripeIntentParams,
        requestOptions: ApiRequest.Options
    )

    /**
     * Confirm a Alipay [PaymentIntent], authenticate it and return the [PaymentIntent].
     *
     * @param confirmPaymentIntentParams [ConfirmPaymentIntentParams] used to confirm the
     * [PaymentIntent]
     * @param authenticator a [AlipayAuthenticator] used to interface with the Alipay SDK
     * @param requestOptions a [ApiRequest.Options] to associate with this request
     *
     * @return a [PaymentIntentResult] object
     *
     * @throws AuthenticationException failure to properly authenticate yourself (check your key)
     * @throws InvalidRequestException your request has invalid parameters
     * @throws APIConnectionException failure to connect to Stripe's API
     * @throws APIException any other type of problem (for instance, a temporary issue with Stripe's servers)
     * @throws IllegalArgumentException if the PaymentIntent response's JsonParser returns null
     */
    @Throws(
        AuthenticationException::class,
        InvalidRequestException::class,
        APIConnectionException::class,
        APIException::class,
        IllegalArgumentException::class
    )
    suspend fun confirmAndAuthenticateAlipay(
        confirmPaymentIntentParams: ConfirmPaymentIntentParams,
        authenticator: AlipayAuthenticator,
        requestOptions: ApiRequest.Options
    ): PaymentIntentResult

    /**
     * Confirm the Stripe Intent for WeChat Pay, return WeChat Pay params from intent's next action
     *
     * @param confirmPaymentIntentParams params to confirm the intent
     * @param requestOptions options for [ApiRequest]
     * @return the [WeChatPayNextAction] object encapsulating [PaymentIntent] and [WeChat]
     *
     * @throws AuthenticationException failure to properly authenticate yourself (check your key)
     * @throws InvalidRequestException your request has invalid parameters
     * @throws APIConnectionException failure to connect to Stripe's API
     * @throws APIException any other type of problem (for instance, a temporary issue with Stripe's servers)
     * @throws IllegalArgumentException if the payment intent's next action data is not for WeChat Pay
     */
    @Throws(
        AuthenticationException::class,
        InvalidRequestException::class,
        APIConnectionException::class,
        APIException::class,
        IllegalArgumentException::class
    )
    suspend fun confirmWeChatPay(
        confirmPaymentIntentParams: ConfirmPaymentIntentParams,
        requestOptions: ApiRequest.Options
    ): WeChatPayNextAction

    suspend fun startAuth(
        host: AuthActivityStarterHost,
        clientSecret: String,
        requestOptions: ApiRequest.Options,
        type: StripeIntentType
    )

    suspend fun startAuthenticateSource(
        host: AuthActivityStarterHost,
        source: Source,
        requestOptions: ApiRequest.Options
    )

    /**
     * Decide whether [getPaymentIntentResult] should be called.
     */
    fun shouldHandlePaymentResult(requestCode: Int, data: Intent?): Boolean

    /**
     * Decide whether [getSetupIntentResult] should be called.
     */
    fun shouldHandleSetupResult(requestCode: Int, data: Intent?): Boolean

    /**
     * Decide whether [getAuthenticateSourceResult] should be called.
     */
    fun shouldHandleSourceResult(requestCode: Int, data: Intent?): Boolean

    /**
     * Get the PaymentIntent's client_secret from [data] and use to retrieve
     * the PaymentIntent object with updated status.
     *
     * @param data the result Intent
     * @return the [PaymentIntentResult] object
     *
     * @throws AuthenticationException failure to properly authenticate yourself (check your key)
     * @throws InvalidRequestException your request has invalid parameters
     * @throws APIConnectionException failure to connect to Stripe's API
     * @throws APIException any other type of problem (for instance, a temporary issue with Stripe's servers)
     * @throws IllegalArgumentException if the PaymentIntent response's JsonParser returns null
     */
    @Throws(
        AuthenticationException::class,
        InvalidRequestException::class,
        APIConnectionException::class,
        APIException::class,
        IllegalArgumentException::class
    )
    suspend fun getPaymentIntentResult(data: Intent): PaymentIntentResult

    /**
     * Get the SetupIntent's client_secret from [data] and use to retrieve
     * the SetupIntent object with updated status.
     *
     * @param data the result Intent
     * @return the [SetupIntentResult] object
     *
     * @throws AuthenticationException failure to properly authenticate yourself (check your key)
     * @throws InvalidRequestException your request has invalid parameters
     * @throws APIConnectionException failure to connect to Stripe's API
     * @throws APIException any other type of problem (for instance, a temporary issue with Stripe's servers)
     * @throws IllegalArgumentException if the SetupIntent response's JsonParser returns null
     */
    @Throws(
        AuthenticationException::class,
        InvalidRequestException::class,
        APIConnectionException::class,
        APIException::class,
        IllegalArgumentException::class
    )
    suspend fun getSetupIntentResult(data: Intent): SetupIntentResult

    /**
     * Get the Source's client_secret from [data] and use to retrieve
     * the Source object with updated status.
     *
     * @param data the result Intent
     * @return the [Source] object
     *
     * @throws AuthenticationException failure to properly authenticate yourself (check your key)
     * @throws InvalidRequestException your request has invalid parameters
     * @throws APIConnectionException failure to connect to Stripe's API
     * @throws APIException any other type of problem (for instance, a temporary issue with Stripe's servers)
     * @throws IllegalArgumentException if the Source response's JsonParser returns null
     */
    @Throws(
        AuthenticationException::class,
        InvalidRequestException::class,
        APIConnectionException::class,
        APIException::class,
        IllegalArgumentException::class
    )
    suspend fun getAuthenticateSourceResult(data: Intent): Source

    /**
     * Determine which authentication mechanism should be used, or bypass authentication
     * if it is not needed.
     */
    suspend fun handleNextAction(
        host: AuthActivityStarterHost,
        stripeIntent: StripeIntent,
        requestOptions: ApiRequest.Options
    )

    /**
     * Register the [ActivityResultLauncher]s with the new [ActivityResultCaller] and callback.
     *
     * [ActivityResultCaller] is usually an [Activity] or [Fragment] instance, this method needs
     * to be called from [Activity.onCreate] to make sure that during configuration change,
     * the new [Activity] instance correctly registers for launchers and the old instance is
     * correctly GC-ed.
     */
    fun registerLaunchersWithActivityResultCaller(
        activityResultCaller: ActivityResultCaller,
        activityResultCallback: ActivityResultCallback<PaymentFlowResult.Unvalidated>
    )

    /**
     * Unregister the [ActivityResultLauncher]s, releasing the reference to [Activity] or
     * [Fragment].
     */
    fun unregisterLaunchers()

    enum class StripeIntentType {
        PaymentIntent,
        SetupIntent
    }
}
