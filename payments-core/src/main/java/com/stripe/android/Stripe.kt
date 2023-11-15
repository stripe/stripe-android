package com.stripe.android

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.annotation.Size
import androidx.annotation.UiThread
import androidx.annotation.WorkerThread
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.stripe.android.core.ApiKeyValidator
import com.stripe.android.core.ApiVersion
import com.stripe.android.core.AppInfo
import com.stripe.android.core.Logger
import com.stripe.android.core.exception.APIConnectionException
import com.stripe.android.core.exception.APIException
import com.stripe.android.core.exception.AuthenticationException
import com.stripe.android.core.exception.InvalidRequestException
import com.stripe.android.core.exception.StripeException
import com.stripe.android.core.model.StripeFile
import com.stripe.android.core.model.StripeFileParams
import com.stripe.android.core.model.StripeModel
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.core.version.StripeSdkVersion
import com.stripe.android.exception.CardException
import com.stripe.android.model.AccountParams
import com.stripe.android.model.BankAccount
import com.stripe.android.model.BankAccountTokenParams
import com.stripe.android.model.Card
import com.stripe.android.model.CardParams
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.ConfirmSetupIntentParams
import com.stripe.android.model.CvcTokenParams
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PersonTokenParams
import com.stripe.android.model.PiiTokenParams
import com.stripe.android.model.PossibleBrands
import com.stripe.android.model.RadarSession
import com.stripe.android.model.SetupIntent
import com.stripe.android.model.Source
import com.stripe.android.model.SourceParams
import com.stripe.android.model.Token
import com.stripe.android.model.TokenParams
import com.stripe.android.model.WeChatPayNextAction
import com.stripe.android.networking.StripeApiRepository
import com.stripe.android.networking.StripeRepository
import com.stripe.android.view.AuthActivityStarterHost
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

/**
 * Entry-point to the Stripe SDK.
 *
 * Supports asynchronous and synchronous methods to access the following Stripe APIs.
 *
 *  * [Payment Intent API][PaymentIntent] - confirm and retrieve
 *  * [Setup Intents API][SetupIntent] - confirm and retrieve
 *  * [Payment Methods API][PaymentMethod] - create
 *  * [Sources API][Source] - create and retrieve
 *  * [Tokens API][Token] - create tokens for [Card], [BankAccount], [PiiTokenParams], and [AccountParams]
 *
 */
class Stripe internal constructor(
    internal val stripeRepository: StripeRepository,
    internal val paymentController: PaymentController,
    publishableKey: String,
    internal val stripeAccountId: String? = null,
    private val workContext: CoroutineContext = Dispatchers.IO
) {
    internal val publishableKey: String = ApiKeyValidator().requireValid(publishableKey)

    /**
     * Constructor with publishable key and Stripe Connect account id.
     *
     * @param context Activity or application context
     * @param publishableKey the client's publishable key
     * @param stripeAccountId optional, the Stripe Connect account id to attach to [Stripe API requests](https://stripe.com/docs/connect/authentication#authentication-via-the-stripe-account-header)
     * @param enableLogging enable logging in the Stripe and Stripe 3DS2 SDKs; disabled by default.
     * It is recommended to disable logging in production. Logs can be accessed from the command line using
     * `adb logcat -s StripeSdk`
     * @param betas optional, set of beta flags to pass to the Stripe API. Setting this property is
     * not sufficient to participate in a beta, and passing a beta you are not registered
     * in will result in API errors.
     */
    @JvmOverloads
    constructor(
        context: Context,
        publishableKey: String,
        stripeAccountId: String? = null,
        enableLogging: Boolean = false,
        betas: Set<StripeApiBeta> = emptySet()
    ) : this(
        context.applicationContext,
        StripeApiRepository(
            context.applicationContext,
            { publishableKey },
            appInfo,
            Logger.getInstance(enableLogging),
            betas = betas
        ),
        ApiKeyValidator.get().requireValid(publishableKey),
        stripeAccountId,
        enableLogging
    )

    private constructor(
        context: Context,
        stripeRepository: StripeRepository,
        publishableKey: String,
        stripeAccountId: String?,
        enableLogging: Boolean
    ) : this(
        stripeRepository,
        StripePaymentController(
            context.applicationContext,
            { publishableKey },
            stripeRepository,
            enableLogging
        ),
        publishableKey,
        stripeAccountId
    )

    internal constructor(
        stripeRepository: StripeRepository,
        paymentController: PaymentController,
        publishableKey: String,
        stripeAccountId: String?
    ) : this(
        stripeRepository,
        paymentController,
        publishableKey,
        stripeAccountId,
        Dispatchers.IO
    )

    //
    // Payment Intents API - https://stripe.com/docs/api/payment_intents
    //

    /**
     * Confirm and, if necessary, authenticate a [PaymentIntent].
     * Used for [automatic confirmation](https://stripe.com/docs/payments/payment-intents/quickstart#automatic-confirmation-flow) flow.
     *
     * For confirmation attempts that require 3DS1 authentication, the following logic will
     * be used:
     * - Use [Custom Tabs](https://developer.chrome.com/docs/android/custom-tabs/overview/) if they
     *   are supported on the device.
     * - Otherwise, use the device browser.
     *
     * @param activity the `Activity` that is launching the payment authentication flow
     * @param confirmPaymentIntentParams [ConfirmPaymentIntentParams] used to confirm the
     * [PaymentIntent]
     * @param stripeAccountId Optional, the Connect account to associate with this request.
     * By default, will use the Connect account that was used to instantiate the `Stripe` object, if specified.
     */
    @JvmOverloads
    @UiThread
    fun confirmPayment(
        activity: ComponentActivity,
        confirmPaymentIntentParams: ConfirmPaymentIntentParams,
        stripeAccountId: String? = this.stripeAccountId
    ) {
        activity.lifecycleScope.launch {
            paymentController.startConfirmAndAuth(
                AuthActivityStarterHost.create(activity),
                confirmPaymentIntentParams,
                ApiRequest.Options(
                    apiKey = publishableKey,
                    stripeAccount = stripeAccountId
                )
            )
        }
    }

    /**
     * Confirm and authenticate a [PaymentIntent] using the Alipay SDK
     * @see <a href="https://intl.alipay.com/docs/ac/app/sdk_integration">Alipay Documentation</a>
     *
     * @param confirmPaymentIntentParams [ConfirmPaymentIntentParams] used to confirm the
     * [PaymentIntent]
     * @param authenticator a [AlipayAuthenticator] used to interface with the Alipay SDK
     * @param stripeAccountId Optional, the Connect account to associate with this request.
     * By default, will use the Connect account that was used to instantiate the `Stripe` object, if specified.
     * @param callback a [ApiResultCallback] to receive the result or error
     */
    @JvmOverloads
    fun confirmAlipayPayment(
        confirmPaymentIntentParams: ConfirmPaymentIntentParams,
        authenticator: AlipayAuthenticator,
        stripeAccountId: String? = this.stripeAccountId,
        callback: ApiResultCallback<PaymentIntentResult>
    ) {
        executeAsyncForResult(callback) {
            paymentController.confirmAndAuthenticateAlipay(
                confirmPaymentIntentParams,
                authenticator,
                ApiRequest.Options(
                    apiKey = publishableKey,
                    stripeAccount = stripeAccountId
                )
            )
        }
    }

    /**
     * Confirm a [PaymentIntent] for WeChat Pay. Extract params from [WeChatPayNextAction] to pass to WeChat Pay SDK.
     * @see <a href="https://pay.weixin.qq.com/index.php/public/wechatpay">WeChat Pay Documentation</a>
     *
     * WeChat Pay API is still in beta, create a [Stripe] instance with [StripeApiBeta.WeChatPayV1] to enable this API.
     *
     * @param confirmPaymentIntentParams [ConfirmPaymentIntentParams] used to confirm the
     * [PaymentIntent]
     * @param stripeAccountId Optional, the Connect account to associate with this request.
     * By default, will use the Connect account that was used to instantiate the [Stripe] object, if specified.
     * @param callback a [ApiResultCallback] to receive the result or error
     *
     * Possible callback errors:
     * [AuthenticationException] failure to properly authenticate yourself (check your key)
     * [InvalidRequestException] your request has invalid parameters
     * [APIConnectionException] failure to connect to Stripe's API
     * [APIException] any other type of problem (for instance, a temporary issue with Stripe's servers)
     * [InvalidRequestException] if the payment intent's next action data is not for WeChat Pay
     *
     * To avoid interacting with WeChat Pay SDK directly, use WeChatPay module.
     * @see <a href="https://github.com/stripe/stripe-android/tree/master/wechatpay">WeChatPay module</a>
     */
    @JvmOverloads
    @Deprecated("Use the WeChat Pay module instead.")
    fun confirmWeChatPayPayment(
        confirmPaymentIntentParams: ConfirmPaymentIntentParams,
        stripeAccountId: String? = this.stripeAccountId,
        callback: ApiResultCallback<WeChatPayNextAction>
    ) {
        executeAsyncForResult(callback) {
            paymentController.confirmWeChatPay(
                confirmPaymentIntentParams,
                ApiRequest.Options(
                    apiKey = publishableKey,
                    stripeAccount = stripeAccountId
                )
            )
        }
    }

    /**
     * Confirm and, if necessary, authenticate a [PaymentIntent].
     * Used for [automatic confirmation](https://stripe.com/docs/payments/payment-intents/quickstart#automatic-confirmation-flow) flow.
     *
     * For confirmation attempts that require 3DS1 authentication, if the
     * [return_url](https://stripe.com/docs/api/payment_intents/confirm#confirm_payment_intent-return_url)
     * in the confirmation request is not set (i.e. set to `null`), then the following logic will
     * be used:
     * - Use [Custom Tabs](https://developer.chrome.com/docs/android/custom-tabs/overview/) if they
     *   are supported on the device.
     * - If Custom Tabs are not supported, use Chrome if it is available on the device.
     * - Otherwise, use a WebView.
     *
     * If a custom `return_url` value is set, a WebView will always be used.
     *
     * |                   | Custom Tabs available? | Chrome available? | Fallback |
     * |-------------------|------------------------|-------------------|----------|
     * | No return_url     | Custom Tabs            | Chrome            | WebView  |
     * | Custom return_url | WebView                | WebView           | WebView  |
     *
     * @param fragment the `Fragment` that is launching the payment authentication flow
     * @param confirmPaymentIntentParams [ConfirmPaymentIntentParams] used to confirm the [PaymentIntent]
     * @param stripeAccountId Optional, the Connect account to associate with this request.
     * By default, will use the Connect account that was used to instantiate the `Stripe` object, if specified.
     */
    @JvmOverloads
    @UiThread
    fun confirmPayment(
        fragment: Fragment,
        confirmPaymentIntentParams: ConfirmPaymentIntentParams,
        stripeAccountId: String? = this.stripeAccountId
    ) {
        fragment.lifecycleScope.launch {
            paymentController.startConfirmAndAuth(
                AuthActivityStarterHost.create(fragment),
                confirmPaymentIntentParams,
                ApiRequest.Options(
                    apiKey = publishableKey,
                    stripeAccount = stripeAccountId
                )
            )
        }
    }

    /**
     * Handle the [next_action](https://stripe.com/docs/api/payment_intents/object#payment_intent_object-next_action)
     * for a previously confirmed [PaymentIntent].
     *
     * Used for [manual confirmation](https://stripe.com/docs/payments/payment-intents/quickstart#manual-confirmation-flow) flow.
     *
     * @param activity the `Activity` that is launching the payment authentication flow
     * @param clientSecret the [client_secret](https://stripe.com/docs/api/payment_intents/object#payment_intent_object-client_secret)
     * property of a confirmed [PaymentIntent] object
     * @param stripeAccountId Optional, the Connect account to associate with this request.
     * By default, will use the Connect account that was used to instantiate the `Stripe` object, if specified.
     */
    @JvmOverloads
    @UiThread
    fun handleNextActionForPayment(
        activity: ComponentActivity,
        clientSecret: String,
        stripeAccountId: String? = this.stripeAccountId
    ) {
        activity.lifecycleScope.launch {
            paymentController.startAuth(
                AuthActivityStarterHost.create(activity),
                PaymentIntent.ClientSecret(clientSecret).value,
                ApiRequest.Options(
                    apiKey = publishableKey,
                    stripeAccount = stripeAccountId
                ),
                PaymentController.StripeIntentType.PaymentIntent
            )
        }
    }

    /**
     * Handle the [next_action](https://stripe.com/docs/api/payment_intents/object#payment_intent_object-next_action)
     * for a previously confirmed [PaymentIntent].
     *
     * Used for [manual confirmation](https://stripe.com/docs/payments/accept-a-payment-synchronously) flow.
     *
     * @param fragment the `Fragment` that is launching the payment authentication flow
     * @param clientSecret the [client_secret](https://stripe.com/docs/api/payment_intents/object#payment_intent_object-client_secret)
     * property of a confirmed [PaymentIntent] object
     * @param stripeAccountId Optional, the Connect account to associate with this request.
     * By default, will use the Connect account that was used to instantiate the `Stripe` object, if specified.
     */
    @JvmOverloads
    @UiThread
    fun handleNextActionForPayment(
        fragment: Fragment,
        clientSecret: String,
        stripeAccountId: String? = this.stripeAccountId
    ) {
        fragment.lifecycleScope.launch {
            paymentController.startAuth(
                AuthActivityStarterHost.create(fragment),
                PaymentIntent.ClientSecret(clientSecret).value,
                ApiRequest.Options(
                    apiKey = publishableKey,
                    stripeAccount = stripeAccountId
                ),
                PaymentController.StripeIntentType.PaymentIntent
            )
        }
    }

    /**
     * Check if the requestCode and [Intent] is for [PaymentIntentResult].
     * The [Intent] should be retrieved from the result from `Activity#onActivityResult(int, int, Intent)}}`
     * by [Activity] started with [confirmPayment] or [handleNextActionForPayment].
     *
     * @return whether the requestCode and intent is for [PaymentIntentResult].
     */
    fun isPaymentResult(
        requestCode: Int,
        data: Intent?
    ): Boolean {
        return data != null && paymentController.shouldHandlePaymentResult(requestCode, data)
    }

    /**
     * Should be called via `Activity#onActivityResult(int, int, Intent)}}` to handle the
     * result of a PaymentIntent automatic confirmation (see [confirmPayment]) or
     * manual confirmation (see [handleNextActionForPayment]})
     */
    @UiThread
    fun onPaymentResult(
        requestCode: Int,
        data: Intent?,
        callback: ApiResultCallback<PaymentIntentResult>
    ): Boolean {
        return if (data != null && isPaymentResult(requestCode, data)) {
            executeAsyncForResult(callback) {
                paymentController.getPaymentIntentResult(data)
            }
            true
        } else {
            false
        }
    }

    /**
     * Retrieve a [PaymentIntent] asynchronously.
     *
     * See [Retrieve a PaymentIntent](https://stripe.com/docs/api/payment_intents/retrieve).
     * `GET /v1/payment_intents/:id`
     *
     * @param clientSecret the client_secret with which to retrieve the [PaymentIntent]
     * @param stripeAccountId Optional, the Connect account to associate with this request.
     * By default, will use the Connect account that was used to instantiate the `Stripe` object, if specified.
     * @param expand Optional, a list of keys to expand on the returned `PaymentIntent` object.
     * @param callback a [ApiResultCallback] to receive the result or error
     */
    @UiThread
    @JvmOverloads
    fun retrievePaymentIntent(
        clientSecret: String,
        stripeAccountId: String? = this.stripeAccountId,
        expand: List<String> = emptyList(),
        callback: ApiResultCallback<PaymentIntent>
    ) {
        executeAsyncForResult(callback) {
            stripeRepository.retrievePaymentIntent(
                clientSecret,
                ApiRequest.Options(
                    apiKey = publishableKey,
                    stripeAccount = stripeAccountId
                ),
                expand,
            )
        }
    }

    /**
     * Blocking method to retrieve a [PaymentIntent] object.
     * Do not call this on the UI thread or your app will crash.
     *
     * See [Retrieve a PaymentIntent](https://stripe.com/docs/api/payment_intents/retrieve).
     * `GET /v1/payment_intents/:id`
     *
     * @param clientSecret the client_secret with which to retrieve the [PaymentIntent]
     * @param stripeAccountId Optional, the Connect account to associate with this request.
     * By default, will use the Connect account that was used to instantiate the `Stripe` object, if specified.
     * @param expand Optional, a list of keys to expand on the returned `PaymentIntent` object.
     * @return a [PaymentIntent] or `null` if a problem occurred
     */
    @Throws(
        APIException::class,
        AuthenticationException::class,
        InvalidRequestException::class,
        APIConnectionException::class
    )
    @WorkerThread
    @JvmOverloads
    fun retrievePaymentIntentSynchronous(
        clientSecret: String,
        stripeAccountId: String? = this.stripeAccountId,
        expand: List<String> = emptyList(),
    ): PaymentIntent {
        return runBlocking {
            stripeRepository.retrievePaymentIntent(
                PaymentIntent.ClientSecret(clientSecret).value,
                ApiRequest.Options(
                    apiKey = publishableKey,
                    stripeAccount = stripeAccountId
                ),
                expand,
            ).getOrElse { throw StripeException.create(it) }
        }
    }

    /**
     * Blocking method to confirm a [PaymentIntent] object.
     * Do not call this on the UI thread or your app will crash.
     *
     * See [Confirm a PaymentIntent](https://stripe.com/docs/api/payment_intents/confirm).
     * `POST /v1/payment_intents/:id/confirm`
     *
     * @param confirmPaymentIntentParams a set of params with which to confirm the PaymentIntent
     * @param idempotencyKey optional, see [Idempotent Requests](https://stripe.com/docs/api/idempotent_requests)
     *
     * @return a [PaymentIntent] or `null` if a problem occurred
     */
    @Deprecated("use {@link #confirmPayment(Activity, ConfirmPaymentIntentParams)}")
    @Throws(
        AuthenticationException::class,
        InvalidRequestException::class,
        APIConnectionException::class,
        APIException::class
    )
    @WorkerThread
    @JvmOverloads
    fun confirmPaymentIntentSynchronous(
        confirmPaymentIntentParams: ConfirmPaymentIntentParams,
        idempotencyKey: String? = null
    ): PaymentIntent {
        return runBlocking {
            stripeRepository.confirmPaymentIntent(
                confirmPaymentIntentParams,
                ApiRequest.Options(
                    apiKey = publishableKey,
                    stripeAccount = stripeAccountId,
                    idempotencyKey = idempotencyKey
                )
            ).getOrElse { throw StripeException.create(it) }
        }
    }

    //
    // Setup Intents API - https://stripe.com/docs/api/setup_intents
    //

    /**
     * Confirm and, if necessary, authenticate a [SetupIntent].
     *
     * For confirmation attempts that require 3DS1 authentication, if the
     * [return_url](https://stripe.com/docs/api/payment_intents/confirm#confirm_payment_intent-return_url)
     * in the confirmation request is not set (i.e. set to `null`), then the following logic will
     * be used:
     * - Use [Custom Tabs](https://developer.chrome.com/docs/android/custom-tabs/overview/) if they
     *   are supported on the device.
     * - If Custom Tabs are not supported, use Chrome if it is available on the device.
     * - Otherwise, use a WebView.
     *
     * If a custom `return_url` value is set, a WebView will always be used.
     *
     * |                   | Custom Tabs available? | Chrome available? | Fallback |
     * |-------------------|------------------------|-------------------|----------|
     * | No return_url     | Custom Tabs            | Chrome            | WebView  |
     * | Custom return_url | WebView                | WebView           | WebView  |
     *
     * @param activity the `Activity` that is launching the payment authentication flow
     * @param confirmSetupIntentParams a set of params with which to confirm the Setup Intent
     * @param stripeAccountId Optional, the Connect account to associate with this request.
     * By default, will use the Connect account that was used to instantiate the `Stripe` object, if specified.
     */
    @JvmOverloads
    fun confirmSetupIntent(
        activity: ComponentActivity,
        confirmSetupIntentParams: ConfirmSetupIntentParams,
        stripeAccountId: String? = this.stripeAccountId
    ) {
        activity.lifecycleScope.launch {
            paymentController.startConfirmAndAuth(
                AuthActivityStarterHost.create(activity),
                confirmSetupIntentParams,
                ApiRequest.Options(
                    apiKey = publishableKey,
                    stripeAccount = stripeAccountId
                )
            )
        }
    }

    /**
     * Confirm and, if necessary, authenticate a [SetupIntent].
     *
     * For confirmation attempts that require 3DS1 authentication, if the
     * [return_url](https://stripe.com/docs/api/payment_intents/confirm#confirm_payment_intent-return_url)
     * in the confirmation request is not set (i.e. set to `null`), then the following logic will
     * be used:
     * - Use [Custom Tabs](https://developer.chrome.com/docs/android/custom-tabs/overview/) if they
     *   are supported on the device.
     * - If Custom Tabs are not supported, use Chrome if it is available on the device.
     * - Otherwise, use a WebView.
     *
     * If a custom `return_url` value is set, a WebView will always be used.
     *
     * |                   | Custom Tabs available? | Chrome available? | Fallback |
     * |-------------------|------------------------|-------------------|----------|
     * | No return_url     | Custom Tabs            | Chrome            | WebView  |
     * | Custom return_url | WebView                | WebView           | WebView  |
     *
     * @param fragment the `Fragment` that is launching the payment authentication flow
     * @param confirmSetupIntentParams a set of params with which to confirm the Setup Intent
     * @param stripeAccountId Optional, the Connect account to associate with this request.
     * By default, will use the Connect account that was used to instantiate the `Stripe` object, if specified.
     */
    @UiThread
    @JvmOverloads
    fun confirmSetupIntent(
        fragment: Fragment,
        confirmSetupIntentParams: ConfirmSetupIntentParams,
        stripeAccountId: String? = this.stripeAccountId
    ) {
        fragment.lifecycleScope.launch {
            paymentController.startConfirmAndAuth(
                AuthActivityStarterHost.create(fragment),
                confirmSetupIntentParams,
                ApiRequest.Options(
                    apiKey = publishableKey,
                    stripeAccount = stripeAccountId
                )
            )
        }
    }

    /**
     * Handle [next_action](https://stripe.com/docs/api/setup_intents/object#setup_intent_object-next_action)
     * for a previously confirmed [SetupIntent]. Used for manual confirmation flow.
     *
     * @param activity the `Activity` that is launching the payment authentication flow
     * @param clientSecret the [client_secret](https://stripe.com/docs/api/setup_intents/object#setup_intent_object-client_secret)
     * property of a confirmed [SetupIntent] object
     * @param stripeAccountId Optional, the Connect account to associate with this request.
     * By default, will use the Connect account that was used to instantiate the `Stripe` object, if specified.
     */
    @UiThread
    @JvmOverloads
    fun handleNextActionForSetupIntent(
        activity: ComponentActivity,
        clientSecret: String,
        stripeAccountId: String? = this.stripeAccountId
    ) {
        activity.lifecycleScope.launch {
            paymentController.startAuth(
                AuthActivityStarterHost.create(activity),
                SetupIntent.ClientSecret(clientSecret).value,
                ApiRequest.Options(
                    apiKey = publishableKey,
                    stripeAccount = stripeAccountId
                ),
                PaymentController.StripeIntentType.SetupIntent
            )
        }
    }

    /**
     * Handle [next_action](https://stripe.com/docs/api/setup_intents/object#setup_intent_object-next_action)
     * for a previously confirmed [SetupIntent]. Used for manual confirmation flow.
     *
     * @param fragment the `Fragment` launching the payment authentication flow
     * @param clientSecret the [client_secret](https://stripe.com/docs/api/setup_intents/object#setup_intent_object-client_secret)
     * property of a confirmed [SetupIntent] object
     * @param stripeAccountId Optional, the Connect account to associate with this request.
     * By default, will use the Connect account that was used to instantiate the `Stripe` object, if specified.
     */
    @UiThread
    @JvmOverloads
    fun handleNextActionForSetupIntent(
        fragment: Fragment,
        clientSecret: String,
        stripeAccountId: String? = this.stripeAccountId
    ) {
        fragment.lifecycleScope.launch {
            paymentController.startAuth(
                AuthActivityStarterHost.create(fragment),
                SetupIntent.ClientSecret(clientSecret).value,
                ApiRequest.Options(
                    apiKey = publishableKey,
                    stripeAccount = stripeAccountId
                ),
                PaymentController.StripeIntentType.SetupIntent
            )
        }
    }

    /**
     * Check if the requestCode and [Intent] is for [SetupIntentResult].
     * The [Intent] should be retrieved from the result from `Activity#onActivityResult(int, int, Intent)}}`
     * by [Activity] started with [confirmSetupIntent].
     *
     * @return whether the requestCode and intent is for [SetupIntentResult].
     */
    fun isSetupResult(
        requestCode: Int,
        data: Intent?
    ): Boolean {
        return data != null && paymentController.shouldHandleSetupResult(requestCode, data)
    }

    /**
     * Should be called via `Activity#onActivityResult(int, int, Intent)}}` to handle the
     * result of a SetupIntent confirmation (see [confirmSetupIntent]).
     */
    @UiThread
    fun onSetupResult(
        requestCode: Int,
        data: Intent?,
        callback: ApiResultCallback<SetupIntentResult>
    ): Boolean {
        return if (data != null && isSetupResult(requestCode, data)) {
            executeAsyncForResult(callback) {
                paymentController.getSetupIntentResult(data)
            }
            true
        } else {
            false
        }
    }

    /**
     * Retrieve a [SetupIntent] asynchronously.
     *
     * See [Retrieve a SetupIntent](https://stripe.com/docs/api/setup_intents/retrieve).
     * `GET /v1/setup_intents/:id`
     *
     * @param clientSecret the client_secret with which to retrieve the [SetupIntent]
     * @param stripeAccountId Optional, the Connect account to associate with this request.
     * By default, will use the Connect account that was used to instantiate the `Stripe` object, if specified.
     * @param expand Optional, a list of keys to expand on the returned `SetupIntent` object.
     * @param callback a [ApiResultCallback] to receive the result or error
     */
    @Throws(
        APIException::class,
        AuthenticationException::class,
        InvalidRequestException::class,
        APIConnectionException::class
    )
    @WorkerThread
    @JvmOverloads
    fun retrieveSetupIntent(
        clientSecret: String,
        stripeAccountId: String? = this.stripeAccountId,
        expand: List<String> = emptyList(),
        callback: ApiResultCallback<SetupIntent>
    ) {
        executeAsyncForResult(callback) {
            stripeRepository.retrieveSetupIntent(
                clientSecret,
                ApiRequest.Options(
                    apiKey = publishableKey,
                    stripeAccount = stripeAccountId
                ),
                expand,
            )
        }
    }

    /**
     * Blocking method to retrieve a [SetupIntent] object.
     * Do not call this on the UI thread or your app will crash.
     *
     * See [Retrieve a SetupIntent](https://stripe.com/docs/api/setup_intents/retrieve).
     * `GET /v1/setup_intents/:id`
     *
     * @param clientSecret client_secret of the [SetupIntent] to retrieve
     * @param stripeAccountId Optional, the Connect account to associate with this request.
     * By default, will use the Connect account that was used to instantiate the `Stripe` object, if specified.
     * @param expand Optional, a list of keys to expand on the returned `SetupIntent` object.
     * @return a [SetupIntent] or `null` if a problem occurred
     */
    @Throws(
        APIException::class,
        AuthenticationException::class,
        InvalidRequestException::class,
        APIConnectionException::class
    )
    @WorkerThread
    @JvmOverloads
    fun retrieveSetupIntentSynchronous(
        clientSecret: String,
        stripeAccountId: String? = this.stripeAccountId,
        expand: List<String> = emptyList(),
    ): SetupIntent {
        return runBlocking {
            stripeRepository.retrieveSetupIntent(
                SetupIntent.ClientSecret(clientSecret).value,
                ApiRequest.Options(
                    apiKey = publishableKey,
                    stripeAccount = stripeAccountId
                ),
                expand,
            ).getOrElse { throw StripeException.create(it) }
        }
    }

    /**
     * Blocking method to confirm a [SetupIntent] object.
     * Do not call this on the UI thread or your app will crash.
     *
     * See [Confirm a SetupIntent](https://stripe.com/docs/api/setup_intents/confirm).
     * `POST /v1/setup_intents/:id/confirm`
     *
     * @param confirmSetupIntentParams a set of params with which to confirm the Setup Intent
     * @param idempotencyKey optional, see [Idempotent Requests](https://stripe.com/docs/api/idempotent_requests)
     *
     * @return a [SetupIntent] or `null` if a problem occurred
     */
    @Deprecated("use {@link #confirmSetupIntent(Activity, ConfirmSetupIntentParams)}")
    @Throws(
        AuthenticationException::class,
        InvalidRequestException::class,
        APIConnectionException::class,
        APIException::class
    )
    @WorkerThread
    @JvmOverloads
    fun confirmSetupIntentSynchronous(
        confirmSetupIntentParams: ConfirmSetupIntentParams,
        idempotencyKey: String? = null
    ): SetupIntent {
        return runBlocking {
            stripeRepository.confirmSetupIntent(
                confirmSetupIntentParams,
                ApiRequest.Options(
                    apiKey = publishableKey,
                    stripeAccount = stripeAccountId,
                    idempotencyKey = idempotencyKey
                )
            ).getOrElse { throw StripeException.create(it) }
        }
    }

    //
    // Payment Methods API - https://stripe.com/docs/api/payment_methods
    //

    /**
     * Create a [PaymentMethod] asynchronously.
     *
     * See [Create a PaymentMethod](https://stripe.com/docs/api/payment_methods/create).
     * `POST /v1/payment_methods`
     *
     * @param paymentMethodCreateParams the [PaymentMethodCreateParams] to be used
     * @param idempotencyKey optional, see [Idempotent Requests](https://stripe.com/docs/api/idempotent_requests)
     * @param stripeAccountId Optional, the Connect account to associate with this request.
     * By default, will use the Connect account that was used to instantiate the `Stripe` object, if specified.
     * @param callback a [ApiResultCallback] to receive the result or error
     */
    @UiThread
    @JvmOverloads
    fun createPaymentMethod(
        paymentMethodCreateParams: PaymentMethodCreateParams,
        idempotencyKey: String? = null,
        stripeAccountId: String? = this.stripeAccountId,
        callback: ApiResultCallback<PaymentMethod>
    ) {
        executeAsyncForResult(callback) {
            stripeRepository.createPaymentMethod(
                paymentMethodCreateParams,
                ApiRequest.Options(
                    apiKey = publishableKey,
                    stripeAccount = stripeAccountId,
                    idempotencyKey = idempotencyKey
                )
            )
        }
    }

    /**
     * Blocking method to create a [PaymentMethod] object.
     * Do not call this on the UI thread or your app will crash.
     *
     * See [Create a PaymentMethod](https://stripe.com/docs/api/payment_methods/create).
     * `POST /v1/payment_methods`
     *
     * @param paymentMethodCreateParams params with which to create the PaymentMethod
     * @param idempotencyKey optional, see [Idempotent Requests](https://stripe.com/docs/api/idempotent_requests)
     * @param stripeAccountId Optional, the Connect account to associate with this request.
     * By default, will use the Connect account that was used to instantiate the `Stripe` object, if specified.
     *
     * @return a [PaymentMethod] or `null` if a problem occurred
     */
    @Throws(
        APIException::class,
        AuthenticationException::class,
        InvalidRequestException::class,
        APIConnectionException::class
    )
    @WorkerThread
    @JvmOverloads
    fun createPaymentMethodSynchronous(
        paymentMethodCreateParams: PaymentMethodCreateParams,
        idempotencyKey: String? = null,
        stripeAccountId: String? = this.stripeAccountId
    ): PaymentMethod {
        return runBlocking {
            stripeRepository.createPaymentMethod(
                paymentMethodCreateParams,
                ApiRequest.Options(
                    apiKey = publishableKey,
                    stripeAccount = stripeAccountId,
                    idempotencyKey = idempotencyKey
                )
            ).getOrElse { throw StripeException.create(it) }
        }
    }

    //
    // Sources API - https://stripe.com/docs/api/sources
    //

    /**
     * Authenticate a [Source] that requires user action via a redirect (i.e. [Source.flow] is
     * [Source.Flow.Redirect].
     *
     * The result of this operation will be returned via `Activity#onActivityResult(int, int, Intent)}}`
     *
     * @param activity the `Activity` that is launching the [Source] authentication flow
     * @param source the [Source] to confirm
     * @param stripeAccountId Optional, the Connect account to associate with this request.
     * By default, will use the Connect account that was used to instantiate the `Stripe` object, if specified.
     */
    @JvmOverloads
    fun authenticateSource(
        activity: ComponentActivity,
        source: Source,
        stripeAccountId: String? = this.stripeAccountId
    ) {
        activity.lifecycleScope.launch {
            paymentController.startAuthenticateSource(
                AuthActivityStarterHost.create(activity),
                source,
                ApiRequest.Options(publishableKey, stripeAccountId)
            )
        }
    }

    /**
     * Authenticate a [Source] that requires user action via a redirect (i.e. [Source.flow] is
     * [Source.Flow.Redirect].
     *
     * The result of this operation will be returned via `Activity#onActivityResult(int, int, Intent)}}`
     *
     * @param fragment the `Fragment` that is launching the [Source] authentication flow
     * @param source the [Source] to confirm
     * @param stripeAccountId Optional, the Connect account to associate with this request.
     * By default, will use the Connect account that was used to instantiate the `Stripe` object, if specified.
     */
    @JvmOverloads
    fun authenticateSource(
        fragment: Fragment,
        source: Source,
        stripeAccountId: String? = this.stripeAccountId
    ) {
        fragment.lifecycleScope.launchWhenCreated {
            paymentController.startAuthenticateSource(
                AuthActivityStarterHost.create(fragment),
                source,
                ApiRequest.Options(publishableKey, stripeAccountId)
            )
        }
    }

    /**
     * Check if the requestCode and [Intent] is for [Source] authentication.
     * The [Intent] should be retrieved from the result from `Activity#onActivityResult(int, int, Intent)}}`
     * by [Activity] started with [authenticateSource].
     *
     * @return whether the requestCode and intent is for [Source] authentication
     */
    fun isAuthenticateSourceResult(
        requestCode: Int,
        data: Intent?
    ): Boolean {
        return data != null && paymentController.shouldHandleSourceResult(requestCode, data)
    }

    /**
     * The result of a call to [authenticateSource].
     *
     * Use [isAuthenticateSourceResult] before calling this method.
     */
    fun onAuthenticateSourceResult(
        data: Intent,
        callback: ApiResultCallback<Source>
    ) {
        executeAsyncForResult(callback) {
            paymentController.getAuthenticateSourceResult(data)
        }
    }

    /**
     * Create a [Source] asynchronously.
     *
     * See [Create a source](https://stripe.com/docs/api/sources/create).
     * `POST /v1/sources`
     *
     * @param sourceParams the [SourceParams] to be used
     * @param idempotencyKey optional, see [Idempotent Requests](https://stripe.com/docs/api/idempotent_requests)
     * @param stripeAccountId Optional, the Connect account to associate with this request.
     * By default, will use the Connect account that was used to instantiate the `Stripe` object, if specified.
     * @param callback a [ApiResultCallback] to receive the result or error
     */
    @UiThread
    @JvmOverloads
    fun createSource(
        sourceParams: SourceParams,
        idempotencyKey: String? = null,
        stripeAccountId: String? = this.stripeAccountId,
        callback: ApiResultCallback<Source>
    ) {
        executeAsyncForResult(callback) {
            stripeRepository.createSource(
                sourceParams,
                ApiRequest.Options(
                    apiKey = publishableKey,
                    stripeAccount = stripeAccountId,
                    idempotencyKey = idempotencyKey
                )
            )
        }
    }

    /**
     * Blocking method to create a [Source] object.
     * Do not call this on the UI thread or your app will crash.
     *
     * See [Create a source](https://stripe.com/docs/api/sources/create).
     * `POST /v1/sources`
     *
     * @param params a set of [SourceParams] with which to create the source
     * @param idempotencyKey optional, see [Idempotent Requests](https://stripe.com/docs/api/idempotent_requests)
     * @param stripeAccountId Optional, the Connect account to associate with this request.
     * By default, will use the Connect account that was used to instantiate the `Stripe` object, if specified.
     *
     * @return a [Source], or `null` if a problem occurred
     *
     * @throws AuthenticationException failure to properly authenticate yourself (check your key)
     * @throws InvalidRequestException your request has invalid parameters
     * @throws APIConnectionException failure to connect to Stripe's API
     * @throws APIException any other type of problem (for instance, a temporary issue with
     * Stripe's servers
     */
    @Throws(
        AuthenticationException::class,
        InvalidRequestException::class,
        APIConnectionException::class,
        APIException::class
    )
    @WorkerThread
    @JvmOverloads
    fun createSourceSynchronous(
        params: SourceParams,
        idempotencyKey: String? = null,
        stripeAccountId: String? = this.stripeAccountId
    ): Source {
        return runBlocking {
            stripeRepository.createSource(
                params,
                ApiRequest.Options(
                    apiKey = publishableKey,
                    stripeAccount = stripeAccountId,
                    idempotencyKey = idempotencyKey
                )
            ).getOrElse { throw StripeException.create(it) }
        }
    }

    /**
     * Retrieve a [Source] asynchronously.
     *
     * See [Retrieve a source](https://stripe.com/docs/api/sources/retrieve).
     * `GET /v1/sources/:id`
     *
     * @param sourceId the [Source.id] field of the desired Source object
     * @param clientSecret the [Source.clientSecret] field of the desired Source object
     * @param stripeAccountId Optional, the Connect account to associate with this request.
     * By default, will use the Connect account that was used to instantiate the `Stripe` object, if specified.
     * @param callback a [ApiResultCallback] to receive the result or error
     *
     * @throws AuthenticationException failure to properly authenticate yourself (check your key)
     * @throws InvalidRequestException your request has invalid parameters
     * @throws APIConnectionException failure to connect to Stripe's API
     * @throws APIException any other type of problem (for instance, a temporary issue with Stripe's servers)
     */
    @JvmOverloads
    @UiThread
    fun retrieveSource(
        @Size(min = 1) sourceId: String,
        @Size(min = 1) clientSecret: String,
        stripeAccountId: String? = this.stripeAccountId,
        callback: ApiResultCallback<Source>
    ) {
        executeAsyncForResult(callback) {
            stripeRepository.retrieveSource(
                sourceId,
                clientSecret,
                ApiRequest.Options(
                    apiKey = publishableKey,
                    stripeAccount = stripeAccountId
                )
            )
        }
    }

    /**
     * Retrieve an existing [Source] from the Stripe API. Do not call this on the UI thread
     * or your app will crash.
     *
     * See [Retrieve a source](https://stripe.com/docs/api/sources/retrieve).
     * `GET /v1/sources/:id`
     *
     * @param sourceId the [Source.id] field of the desired Source object
     * @param clientSecret the [Source.clientSecret] field of the desired Source object
     * @param stripeAccountId Optional, the Connect account to associate with this request.
     * By default, will use the Connect account that was used to instantiate the `Stripe` object, if specified.
     *
     * @return a [Source] if one could be found based on the input params, or `null` if
     * no such Source could be found.
     * @throws AuthenticationException failure to properly authenticate yourself (check your key)
     * @throws InvalidRequestException your request has invalid parameters
     * @throws APIConnectionException failure to connect to Stripe's API
     * @throws APIException any other type of problem (for instance, a temporary issue with Stripe's servers)
     */
    @Throws(
        AuthenticationException::class,
        InvalidRequestException::class,
        APIConnectionException::class,
        APIException::class
    )
    @WorkerThread
    @JvmOverloads
    fun retrieveSourceSynchronous(
        @Size(min = 1) sourceId: String,
        @Size(min = 1) clientSecret: String,
        stripeAccountId: String? = this.stripeAccountId
    ): Source {
        return runBlocking {
            stripeRepository.retrieveSource(
                sourceId,
                clientSecret,
                ApiRequest.Options(
                    apiKey = publishableKey,
                    stripeAccount = stripeAccountId
                )
            ).getOrElse { throw StripeException.create(it) }
        }
    }

    //
    // Tokens API - https://stripe.com/docs/api/tokens
    //

    /**
     * Create a [Token] asynchronously.
     *
     * See [Create an account token](https://stripe.com/docs/api/tokens/create_account).
     * `POST /v1/tokens`
     *
     * @param accountParams the [AccountParams] used to create this token
     * @param idempotencyKey optional, see [Idempotent Requests](https://stripe.com/docs/api/idempotent_requests)
     * @param stripeAccountId Optional, the Connect account to associate with this request.
     * By default, will use the Connect account that was used to instantiate the `Stripe` object, if specified.
     * @param callback a [ApiResultCallback] to receive the result or error
     */
    @UiThread
    @JvmOverloads
    fun createAccountToken(
        accountParams: AccountParams,
        idempotencyKey: String? = null,
        stripeAccountId: String? = this.stripeAccountId,
        callback: ApiResultCallback<Token>
    ) {
        createToken(
            tokenParams = accountParams,
            stripeAccountId = stripeAccountId,
            idempotencyKey = idempotencyKey,
            callback = callback
        )
    }

    /**
     * Blocking method to create a [Token] for a Connect Account. Do not call this on the UI
     * thread or your app will crash.
     *
     * See [Create an account token](https://stripe.com/docs/api/tokens/create_account).
     * `POST /v1/tokens`
     *
     * @param accountParams params to use for this token.
     * @param idempotencyKey optional, see [Idempotent Requests](https://stripe.com/docs/api/idempotent_requests)
     * @param stripeAccountId Optional, the Connect account to associate with this request.
     * By default, will use the Connect account that was used to instantiate the `Stripe` object, if specified.
     *
     * @return a [Token] that can be used for this account.
     *
     * @throws AuthenticationException failure to properly authenticate yourself (check your key)
     * @throws InvalidRequestException your request has invalid parameters
     * @throws APIConnectionException failure to connect to Stripe's API
     * @throws APIException any other type of problem (for instance, a temporary issue with
     * Stripe's servers)
     */
    @Throws(
        AuthenticationException::class,
        InvalidRequestException::class,
        APIConnectionException::class,
        APIException::class
    )
    @WorkerThread
    @JvmOverloads
    fun createAccountTokenSynchronous(
        accountParams: AccountParams,
        idempotencyKey: String? = null,
        stripeAccountId: String? = this.stripeAccountId
    ): Token {
        return runBlocking {
            createTokenOrThrow(
                tokenParams = accountParams,
                stripeAccountId = stripeAccountId,
                idempotencyKey = idempotencyKey,
            )
        }
    }

    /**
     * Create a [BankAccount] token asynchronously.
     *
     * See [Create a bank account token](https://stripe.com/docs/api/tokens/create_bank_account).
     * `POST /v1/tokens`
     *
     * @param bankAccountTokenParams the [BankAccountTokenParams] used to create this token
     * @param idempotencyKey optional, see [Idempotent Requests](https://stripe.com/docs/api/idempotent_requests)
     * @param stripeAccountId Optional, the Connect account to associate with this request.
     * By default, will use the Connect account that was used to instantiate the `Stripe` object, if specified.
     * @param callback a [ApiResultCallback] to receive the result or error
     */
    @UiThread
    @JvmOverloads
    fun createBankAccountToken(
        bankAccountTokenParams: BankAccountTokenParams,
        idempotencyKey: String? = null,
        stripeAccountId: String? = this.stripeAccountId,
        callback: ApiResultCallback<Token>
    ) {
        createToken(
            tokenParams = bankAccountTokenParams,
            stripeAccountId = stripeAccountId,
            idempotencyKey = idempotencyKey,
            callback = callback,
        )
    }

    /**
     * Blocking method to create a [Token] for a [BankAccount]. Do not call this on
     * the UI thread or your app will crash.
     *
     * See [Create a bank account token](https://stripe.com/docs/api/tokens/create_bank_account).
     * `POST /v1/tokens`
     *
     * @param bankAccountTokenParams the [BankAccountTokenParams] to use for this token
     * @param idempotencyKey optional, see [Idempotent Requests](https://stripe.com/docs/api/idempotent_requests)
     * @param stripeAccountId Optional, the Connect account to associate with this request.
     * By default, will use the Connect account that was used to instantiate the `Stripe` object, if specified.
     *
     * @return a [Token] that can be used for this [BankAccount]
     *
     * @throws AuthenticationException failure to properly authenticate yourself (check your key)
     * @throws InvalidRequestException your request has invalid parameters
     * @throws APIConnectionException failure to connect to Stripe's API
     * @throws CardException should not be thrown with this type of token, but is theoretically
     * possible given the underlying methods called
     * @throws APIException any other type of problem (for instance, a temporary issue with
     * Stripe's servers
     */
    @Throws(
        AuthenticationException::class,
        InvalidRequestException::class,
        APIConnectionException::class,
        CardException::class,
        APIException::class
    )
    @WorkerThread
    @JvmOverloads
    fun createBankAccountTokenSynchronous(
        bankAccountTokenParams: BankAccountTokenParams,
        idempotencyKey: String? = null,
        stripeAccountId: String? = this.stripeAccountId
    ): Token {
        return runBlocking {
            createTokenOrThrow(
                tokenParams = bankAccountTokenParams,
                stripeAccountId = stripeAccountId,
                idempotencyKey = idempotencyKey,
            )
        }
    }

    /**
     * Create a PII token asynchronously.
     *
     * See [Create a PII account token](https://stripe.com/docs/api/tokens/create_pii).
     * `POST /v1/tokens`
     *
     * @param personalId the personal id used to create this token
     * @param idempotencyKey optional, see [Idempotent Requests](https://stripe.com/docs/api/idempotent_requests)
     * @param callback a [ApiResultCallback] to receive the result or error
     */
    @UiThread
    @JvmOverloads
    fun createPiiToken(
        personalId: String,
        idempotencyKey: String? = null,
        stripeAccountId: String? = this.stripeAccountId,
        callback: ApiResultCallback<Token>
    ) {
        createToken(
            tokenParams = PiiTokenParams(personalId),
            stripeAccountId = stripeAccountId,
            idempotencyKey = idempotencyKey,
            callback = callback,
        )
    }

    /**
     * Blocking method to create a [Token] for PII. Do not call this on the UI thread
     * or your app will crash.
     *
     * See [Create a PII account token](https://stripe.com/docs/api/tokens/create_pii).
     * `POST /v1/tokens`
     *
     * @param personalId the personal ID to use for this token
     * @param idempotencyKey optional, see [Idempotent Requests](https://stripe.com/docs/api/idempotent_requests)
     * @param stripeAccountId Optional, the Connect account to associate with this request.
     * By default, will use the Connect account that was used to instantiate the `Stripe` object, if specified.
     *
     * @return a [Token] that can be used for this card
     *
     * @throws AuthenticationException failure to properly authenticate yourself (check your key)
     * @throws InvalidRequestException your request has invalid parameters
     * @throws APIConnectionException failure to connect to Stripe's API
     * @throws APIException any other type of problem (for instance, a temporary issue with
     * Stripe's servers)
     */
    @Throws(
        AuthenticationException::class,
        InvalidRequestException::class,
        APIConnectionException::class,
        CardException::class,
        APIException::class
    )
    @WorkerThread
    @JvmOverloads
    fun createPiiTokenSynchronous(
        personalId: String,
        idempotencyKey: String? = null,
        stripeAccountId: String? = this.stripeAccountId
    ): Token {
        return runBlocking {
            createTokenOrThrow(
                tokenParams = PiiTokenParams(personalId),
                stripeAccountId = stripeAccountId,
                idempotencyKey = idempotencyKey,
            )
        }
    }

    /**
     * Create a Card token asynchronously.
     *
     * See [Create a card token](https://stripe.com/docs/api/tokens/create_card).
     * `POST /v1/tokens`
     *
     * @param cardParams the [CardParams] used to create this payment token
     * @param idempotencyKey optional, see [Idempotent Requests](https://stripe.com/docs/api/idempotent_requests)
     * @param stripeAccountId Optional, the Connect account to associate with this request.
     * By default, will use the Connect account that was used to instantiate the `Stripe` object, if specified.
     * @param callback a [ApiResultCallback] to receive the result or error
     */
    @UiThread
    @JvmOverloads
    fun createCardToken(
        cardParams: CardParams,
        idempotencyKey: String? = null,
        stripeAccountId: String? = this.stripeAccountId,
        callback: ApiResultCallback<Token>
    ) {
        createToken(
            tokenParams = cardParams,
            stripeAccountId = stripeAccountId,
            idempotencyKey = idempotencyKey,
            callback = callback,
        )
    }

    /**
     * Blocking method to create a [Token]. Do not call this on the UI thread or your app
     * will crash.
     *
     * See [Create a card token](https://stripe.com/docs/api/tokens/create_card).
     * `POST /v1/tokens`
     *
     * @param cardParams the [CardParams] to use for this token
     * @param idempotencyKey optional, see [Idempotent Requests](https://stripe.com/docs/api/idempotent_requests)
     * @param stripeAccountId Optional, the Connect account to associate with this request.
     * By default, will use the Connect account that was used to instantiate the `Stripe` object, if specified.
     *
     * @return a [Token] that can be used for this card
     * @throws AuthenticationException failure to properly authenticate yourself (check your key)
     * @throws InvalidRequestException your request has invalid parameters
     * @throws APIConnectionException failure to connect to Stripe's API
     * @throws CardException the card cannot be charged for some reason
     * @throws APIException any other type of problem (for instance, a temporary issue with
     * Stripe's servers
     */
    @Throws(
        AuthenticationException::class,
        InvalidRequestException::class,
        APIConnectionException::class,
        CardException::class,
        APIException::class
    )
    @WorkerThread
    @JvmOverloads
    fun createCardTokenSynchronous(
        cardParams: CardParams,
        idempotencyKey: String? = null,
        stripeAccountId: String? = this.stripeAccountId
    ): Token {
        return runBlocking {
            createTokenOrThrow(
                tokenParams = cardParams,
                stripeAccountId = stripeAccountId,
                idempotencyKey = idempotencyKey,
            )
        }
    }

    /**
     * Create a CVC update token asynchronously.
     *
     * `POST /v1/tokens`
     *
     * @param cvc the CVC used to create this token
     * @param idempotencyKey optional, see [Idempotent Requests](https://stripe.com/docs/api/idempotent_requests)
     * @param stripeAccountId Optional, the Connect account to associate with this request.
     * By default, will use the Connect account that was used to instantiate the `Stripe` object, if specified.
     * @param callback a [ApiResultCallback] to receive the result or error
     */
    @UiThread
    @JvmOverloads
    fun createCvcUpdateToken(
        @Size(min = 3, max = 4) cvc: String,
        idempotencyKey: String? = null,
        stripeAccountId: String? = this.stripeAccountId,
        callback: ApiResultCallback<Token>
    ) {
        createToken(
            tokenParams = CvcTokenParams(cvc),
            stripeAccountId = stripeAccountId,
            idempotencyKey = idempotencyKey,
            callback = callback,
        )
    }

    /**
     * Blocking method to create a [Token] for CVC updating. Do not call this on the UI thread
     * or your app will crash.
     *
     * `POST /v1/tokens`
     *
     * @param cvc the CVC to use for this token
     * @param idempotencyKey optional, see [Idempotent Requests](https://stripe.com/docs/api/idempotent_requests)
     * @param stripeAccountId Optional, the Connect account to associate with this request.
     * By default, will use the Connect account that was used to instantiate the `Stripe` object, if specified.
     *
     * @return a [Token] that can be used for this card
     *
     * @throws AuthenticationException failure to properly authenticate yourself (check your key)
     * @throws InvalidRequestException your request has invalid parameters
     * @throws APIConnectionException failure to connect to Stripe's API
     * @throws APIException any other type of problem (for instance, a temporary issue with
     * Stripe's servers)
     */
    @Throws(
        AuthenticationException::class,
        InvalidRequestException::class,
        APIConnectionException::class,
        CardException::class,
        APIException::class
    )
    @WorkerThread
    @JvmOverloads
    fun createCvcUpdateTokenSynchronous(
        cvc: String,
        idempotencyKey: String? = null,
        stripeAccountId: String? = this.stripeAccountId
    ): Token {
        return runBlocking {
            createTokenOrThrow(
                tokenParams = CvcTokenParams(cvc),
                stripeAccountId = stripeAccountId,
                idempotencyKey = idempotencyKey,
            )
        }
    }

    /**
     * Creates a single-use token that represents the details for a person. Use this when creating or
     * updating persons associated with a Connect account.
     * See [the documentation](https://stripe.com/docs/connect/account-tokens) to learn more.
     *
     * See [Create a person token](https://stripe.com/docs/api/tokens/create_person)
     *
     * @param params the person token creation params
     * @param idempotencyKey optional, see [Idempotent Requests](https://stripe.com/docs/api/idempotent_requests)
     * @param stripeAccountId Optional, the Connect account to associate with this request.
     * By default, will use the Connect account that was used to instantiate the `Stripe` object, if specified.
     * @param callback a [ApiResultCallback] to receive the result or error
     */
    @UiThread
    @JvmOverloads
    fun createPersonToken(
        params: PersonTokenParams,
        idempotencyKey: String? = null,
        stripeAccountId: String? = this.stripeAccountId,
        callback: ApiResultCallback<Token>
    ) {
        createToken(
            tokenParams = params,
            stripeAccountId = stripeAccountId,
            idempotencyKey = idempotencyKey,
            callback = callback,
        )
    }

    /**
     * Creates a single-use token that represents the details for a person. Use this when creating or
     * updating persons associated with a Connect account.
     * See [the documentation](https://stripe.com/docs/connect/account-tokens) to learn more.
     *
     * See [Create a person token](https://stripe.com/docs/api/tokens/create_person)
     *
     * @param params the person token creation params
     * @param idempotencyKey optional, see [Idempotent Requests](https://stripe.com/docs/api/idempotent_requests)
     * @param stripeAccountId Optional, the Connect account to associate with this request.
     * By default, will use the Connect account that was used to instantiate the `Stripe` object, if specified.
     *
     * @return a [Token] representing the person
     */
    @Throws(
        AuthenticationException::class,
        InvalidRequestException::class,
        APIConnectionException::class,
        CardException::class,
        APIException::class
    )
    @WorkerThread
    @JvmOverloads
    fun createPersonTokenSynchronous(
        params: PersonTokenParams,
        idempotencyKey: String? = null,
        stripeAccountId: String? = this.stripeAccountId
    ): Token {
        return runBlocking {
            createTokenOrThrow(
                tokenParams = params,
                stripeAccountId = stripeAccountId,
                idempotencyKey = idempotencyKey,
            )
        }
    }

    internal suspend fun createTokenOrThrow(
        tokenParams: TokenParams,
        stripeAccountId: String?,
        idempotencyKey: String? = null,
    ): Token {
        return stripeRepository.createToken(
            tokenParams = tokenParams,
            options = ApiRequest.Options(
                apiKey = publishableKey,
                stripeAccount = stripeAccountId,
                idempotencyKey = idempotencyKey
            ),
        ).getOrElse { throw StripeException.create(it) }
    }

    private fun createToken(
        tokenParams: TokenParams,
        stripeAccountId: String?,
        idempotencyKey: String? = null,
        callback: ApiResultCallback<Token>
    ) {
        executeAsyncForResult(callback) {
            stripeRepository.createToken(
                tokenParams,
                ApiRequest.Options(
                    apiKey = publishableKey,
                    stripeAccount = stripeAccountId,
                    idempotencyKey = idempotencyKey
                )
            )
        }
    }

    /**
     * [Create a file](https://stripe.com/docs/api/files/create) asynchronously
     *
     * @param fileParams the [StripeFileParams] used to create the [StripeFile]
     * @param idempotencyKey optional, see [Idempotent Requests](https://stripe.com/docs/api/idempotent_requests)
     * @param stripeAccountId Optional, the Connect account to associate with this request.
     * By default, will use the Connect account that was used to instantiate the `Stripe` object, if specified.
     * @param callback a [ApiResultCallback] to receive the result or error
     */
    @UiThread
    @JvmOverloads
    fun createFile(
        fileParams: StripeFileParams,
        idempotencyKey: String? = null,
        stripeAccountId: String? = this.stripeAccountId,
        callback: ApiResultCallback<StripeFile>
    ) {
        executeAsyncForResult(callback) {
            stripeRepository.createFile(
                fileParams,
                ApiRequest.Options(
                    apiKey = publishableKey,
                    stripeAccount = stripeAccountId,
                    idempotencyKey = idempotencyKey
                )
            )
        }
    }

    /**
     * [Create a file](https://stripe.com/docs/api/files/create) synchronously
     *
     * @param fileParams the [StripeFileParams] used to create the [StripeFile]
     * @param idempotencyKey optional, see [Idempotent Requests](https://stripe.com/docs/api/idempotent_requests)
     * @param stripeAccountId Optional, the Connect account to associate with this request.
     * By default, will use the Connect account that was used to instantiate the `Stripe` object, if specified.
     */
    @WorkerThread
    @JvmOverloads
    fun createFileSynchronous(
        fileParams: StripeFileParams,
        idempotencyKey: String? = null,
        stripeAccountId: String? = this.stripeAccountId
    ): StripeFile {
        return runBlocking {
            stripeRepository.createFile(
                fileParams,
                ApiRequest.Options(
                    apiKey = publishableKey,
                    stripeAccount = stripeAccountId,
                    idempotencyKey = idempotencyKey
                )
            ).getOrElse { throw StripeException.create(it) }
        }
    }

    /**
     * Create a Radar Session asynchronously.
     *
     * [Stripe.advancedFraudSignalsEnabled] must be `true` to use this method.
     *
     * See the [Radar Session](https://stripe.com/docs/radar/radar-session) docs for more details.
     *
     * @param stripeAccountId Optional, the Connect account to associate with this request.
     * By default, will use the Connect account that was used to instantiate the `Stripe` object, if specified.
     * @param callback a [ApiResultCallback] to receive the result or error
     */
    @UiThread
    @JvmOverloads
    fun createRadarSession(
        stripeAccountId: String? = this.stripeAccountId,
        callback: ApiResultCallback<RadarSession>
    ) {
        executeAsyncForResult(callback) {
            stripeRepository.createRadarSession(
                ApiRequest.Options(
                    apiKey = publishableKey,
                    stripeAccount = stripeAccountId
                )
            )
        }
    }

    /**
     * Verify a customer's bank account with micro-deposits
     *
     * This function should only be called when the PaymentIntent is in the `requires_action` state
     * and `NextActionType` is VerifyWithMicrodeposits.
     *
     * See the [Verify bank account with micro-deposits](https://stripe.com/docs/payments/ach-debit/accept-a-payment#web-verify-with-microdeposits) docs for more details.
     *
     * @param clientSecret The client secret of the PaymentIntent
     * @param firstAmount The amount, in cents of USD, equal to the value of the first micro-deposit
     * sent to the bank account
     * @param secondAmount The amount, in cents of USD, equal to the value of the second micro-deposit
     * sent to the bank account
     * @param callback a [ApiResultCallback] to receive the result or error
     */
    @UiThread
    fun verifyPaymentIntentWithMicrodeposits(
        clientSecret: String,
        firstAmount: Int,
        secondAmount: Int,
        callback: ApiResultCallback<PaymentIntent>
    ) {
        executeAsyncForResult(callback) {
            stripeRepository.verifyPaymentIntentWithMicrodeposits(
                clientSecret = clientSecret,
                firstAmount = firstAmount,
                secondAmount = secondAmount,
                requestOptions = ApiRequest.Options(
                    apiKey = publishableKey,
                    stripeAccount = stripeAccountId
                )
            )
        }
    }

    /**
     * Verify a customer's bank account with micro-deposits
     *
     * This function should only be called when the PaymentIntent is in the `requires_action` state
     * and `NextActionType` is VerifyWithMicrodeposits.
     *
     * See the [Verify bank account with micro-deposits](https://stripe.com/docs/payments/ach-debit/accept-a-payment#web-verify-with-microdeposits) docs for more details.
     *
     * @param clientSecret The client secret of the PaymentIntent
     * @param descriptorCode A unique, 6-digit descriptor code that starts with SM that was sent as
     * statement descriptor to the bank account
     * @param callback a [ApiResultCallback] to receive the result or error
     */
    @UiThread
    fun verifyPaymentIntentWithMicrodeposits(
        clientSecret: String,
        descriptorCode: String,
        callback: ApiResultCallback<PaymentIntent>
    ) {
        executeAsyncForResult(callback) {
            stripeRepository.verifyPaymentIntentWithMicrodeposits(
                clientSecret = clientSecret,
                descriptorCode = descriptorCode,
                requestOptions = ApiRequest.Options(
                    apiKey = publishableKey,
                    stripeAccount = stripeAccountId
                )
            )
        }
    }

    /**
     * Verify a customer's bank account with micro-deposits
     *
     * This function should only be called when the SetupIntent is in the `requires_action` state
     * and `NextActionType` is VerifyWithMicrodeposits.
     *
     * See the [Verify bank account with micro-deposits](https://stripe.com/docs/payments/ach-debit/accept-a-payment#web-verify-with-microdeposits) docs for more details.
     *
     * @param clientSecret The client secret of the SetupIntent
     * @param firstAmount The amount, in cents of USD, equal to the value of the first micro-deposit
     * sent to the bank account
     * @param secondAmount The amount, in cents of USD, equal to the value of the second micro-deposit
     * sent to the bank account
     * @param callback a [ApiResultCallback] to receive the result or error
     */
    @UiThread
    fun verifySetupIntentWithMicrodeposits(
        clientSecret: String,
        firstAmount: Int,
        secondAmount: Int,
        callback: ApiResultCallback<SetupIntent>
    ) {
        executeAsyncForResult(callback) {
            stripeRepository.verifySetupIntentWithMicrodeposits(
                clientSecret = clientSecret,
                firstAmount = firstAmount,
                secondAmount = secondAmount,
                requestOptions = ApiRequest.Options(
                    apiKey = publishableKey,
                    stripeAccount = stripeAccountId
                )
            )
        }
    }

    /**
     * Verify a customer's bank account with micro-deposits
     *
     * This function should only be called when the SetupIntent is in the `requires_action` state
     * and `NextActionType` is VerifyWithMicrodeposits.
     *
     * See the [Verify bank account with micro-deposits](https://stripe.com/docs/payments/ach-debit/accept-a-payment#web-verify-with-microdeposits) docs for more details.
     *
     * @param clientSecret The client secret of the SetupIntent
     * @param descriptorCode A unique, 6-digit descriptor code that starts with SM that was sent as
     * statement descriptor to the bank account
     * @param callback a [ApiResultCallback] to receive the result or error
     */
    @UiThread
    fun verifySetupIntentWithMicrodeposits(
        clientSecret: String,
        descriptorCode: String,
        callback: ApiResultCallback<SetupIntent>
    ) {
        executeAsyncForResult(callback) {
            stripeRepository.verifySetupIntentWithMicrodeposits(
                clientSecret = clientSecret,
                descriptorCode = descriptorCode,
                requestOptions = ApiRequest.Options(
                    apiKey = publishableKey,
                    stripeAccount = stripeAccountId
                )
            )
        }
    }

    /**
     * Retrieve a list of possible brands for the given card number.
     * Returns an error if the cardNumber length is less than 6 characters.
     *
     * @param cardNumber the card number
     * @param callback a [ApiResultCallback] to receive the result or error
     */
    fun retrievePossibleBrands(
        cardNumber: String,
        callback: ApiResultCallback<PossibleBrands>
    ) {
        executeAsyncForResult(callback) {
            stripeRepository.retrieveCardMetadata(
                cardNumber = cardNumber,
                requestOptions = ApiRequest.Options(
                    apiKey = publishableKey,
                    stripeAccount = stripeAccountId
                )
            ).map { metadata ->
                val brands = metadata.accountRanges.map { it.brand }
                PossibleBrands(brands = brands.distinct())
            }
        }
    }

    private fun <T : StripeModel> executeAsyncForResult(
        callback: ApiResultCallback<T>,
        apiMethod: suspend () -> Result<T>,
    ) {
        CoroutineScope(workContext).launch {
            val result = apiMethod()
            dispatchResult(result, callback)
        }
    }

    private suspend fun <T : StripeModel> dispatchResult(
        result: Result<T>,
        callback: ApiResultCallback<T>
    ) = withContext(Dispatchers.Main) {
        result.fold(
            onSuccess = {
                callback.onSuccess(it)
            },
            onFailure = {
                callback.onError(StripeException.create(it))
            }
        )
    }

    companion object {
        @JvmField
        val API_VERSION: String = ApiVersion.get().code

        @Deprecated(
            message = "Use StripeSdkVersion.VERSION_NAME instead",
            replaceWith = ReplaceWith(
                expression = "StripeSdkVersion.VERSION_NAME",
                imports = ["com.stripe.android.core.version.StripeSdkVersion"]
            )
        )
        const val VERSION_NAME = StripeSdkVersion.VERSION_NAME

        @Deprecated(
            message = "Use StripeSdkVersion.VERSION instead",
            replaceWith = ReplaceWith(
                expression = "StripeSdkVersion.VERSION",
                imports = ["com.stripe.android.core.version.StripeSdkVersion"]
            )
        )
        const val VERSION: String = StripeSdkVersion.VERSION

        /**
         * Setter for identifying your plug-in or library.
         *
         * See [Building Stripe Plug-ins and Libraries - Setting the API version](https://stripe.com/docs/building-plugins#setappinfo).
         */
        @JvmStatic
        var appInfo: AppInfo? = null

        /**
         * [advancedFraudSignalsEnabled] determines whether additional device data is sent to Stripe
         * for fraud prevention. By default, this property is set to `true`.
         *
         * Disabling this setting will reduce Stripe's ability to protect your business from
         * fraudulent payments.
         *
         * For more details on the information we collect, visit
         * [https://stripe.com/docs/disputes/prevention/advanced-fraud-detection](https://stripe.com/docs/disputes/prevention/advanced-fraud-detection)
         */
        @JvmStatic
        var advancedFraudSignalsEnabled: Boolean = true
    }
}
