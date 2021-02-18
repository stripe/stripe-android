package com.stripe.android

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.annotation.Size
import androidx.annotation.UiThread
import androidx.annotation.WorkerThread
import androidx.fragment.app.Fragment
import com.stripe.android.exception.APIConnectionException
import com.stripe.android.exception.APIException
import com.stripe.android.exception.AuthenticationException
import com.stripe.android.exception.CardException
import com.stripe.android.exception.InvalidRequestException
import com.stripe.android.exception.StripeException
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
import com.stripe.android.model.SetupIntent
import com.stripe.android.model.Source
import com.stripe.android.model.SourceParams
import com.stripe.android.model.StripeFile
import com.stripe.android.model.StripeFileParams
import com.stripe.android.model.StripeIntent
import com.stripe.android.model.StripeModel
import com.stripe.android.model.Token
import com.stripe.android.model.TokenParams
import com.stripe.android.networking.ApiRequest
import com.stripe.android.networking.StripeApiRepository
import com.stripe.android.networking.StripeRepository
import com.stripe.android.view.AuthActivityStarter
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
    private val stripeRepository: StripeRepository,
    private val paymentController: PaymentController,
    publishableKey: String,
    private val stripeAccountId: String? = null,
    private val workContext: CoroutineContext = Dispatchers.IO
) {
    private val publishableKey: String = ApiKeyValidator().requireValid(publishableKey)

    /**
     * Constructor with publishable key and Stripe Connect account id.
     *
     * @param context Activity or application context
     * @param publishableKey the client's publishable key
     * @param stripeAccountId optional, the Stripe Connect account id to attach to [Stripe API requests](https://stripe.com/docs/connect/authentication#authentication-via-the-stripe-account-header)
     * @param enableLogging enable logging in the Stripe and Stripe 3DS2 SDKs; disabled by default.
     * It is recommended to disable logging in production. Logs can be accessed from the command line using
     * `adb logcat -s StripeSdk`
     */
    @JvmOverloads
    constructor(
        context: Context,
        publishableKey: String,
        stripeAccountId: String? = null,
        enableLogging: Boolean = false
    ) : this(
        context.applicationContext,
        StripeApiRepository(
            context.applicationContext,
            publishableKey,
            appInfo,
            Logger.getInstance(enableLogging)
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
            publishableKey,
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
     * @param activity the `Activity` that is launching the payment authentication flow
     * @param confirmPaymentIntentParams [ConfirmPaymentIntentParams] used to confirm the
     * [PaymentIntent]
     * @param stripeAccountId Optional, the Connect account to associate with this request.
     * By default, will use the Connect account that was used to instantiate the `Stripe` object, if specified.
     */
    @JvmOverloads
    @UiThread
    fun confirmPayment(
        activity: Activity,
        confirmPaymentIntentParams: ConfirmPaymentIntentParams,
        stripeAccountId: String? = this.stripeAccountId
    ) {
        paymentController.startConfirmAndAuth(
            AuthActivityStarter.Host.create(activity),
            confirmPaymentIntentParams,
            ApiRequest.Options(
                apiKey = publishableKey,
                stripeAccount = stripeAccountId
            )
        )
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
        paymentController.startConfirm(
            confirmPaymentIntentParams,
            ApiRequest.Options(
                apiKey = publishableKey,
                stripeAccount = stripeAccountId
            ),
            object : ApiResultCallback<StripeIntent> {
                override fun onSuccess(result: StripeIntent) {
                    paymentController.authenticateAlipay(
                        result,
                        stripeAccountId,
                        authenticator,
                        callback
                    )
                }

                override fun onError(e: Exception) = callback.onError(e)
            }
        )
    }

    /**
     * Confirm and, if necessary, authenticate a [PaymentIntent].
     * Used for [automatic confirmation](https://stripe.com/docs/payments/payment-intents/quickstart#automatic-confirmation-flow) flow.
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
        paymentController.startConfirmAndAuth(
            AuthActivityStarter.Host.create(fragment),
            confirmPaymentIntentParams,
            ApiRequest.Options(
                apiKey = publishableKey,
                stripeAccount = stripeAccountId
            )
        )
    }

    /**
     * Authenticate a [PaymentIntent].
     * Used for [manual confirmation](https://stripe.com/docs/payments/payment-intents/android-manual) flow.
     *
     * @param activity the `Activity` that is launching the payment authentication flow
     * @param clientSecret the [client_secret](https://stripe.com/docs/api/payment_intents/object#payment_intent_object-client_secret)
     * property of a confirmed [PaymentIntent] object
     */
    @Deprecated(
        "Rename to better reflect behavior and match iOS naming.",
        ReplaceWith("handleNextActionForPayment(activity, clientSecret)")
    )
    @UiThread
    fun authenticatePayment(activity: Activity, clientSecret: String) {
        paymentController.startAuth(
            AuthActivityStarter.Host.create(activity),
            PaymentIntent.ClientSecret(clientSecret).value,
            ApiRequest.Options(
                apiKey = publishableKey,
                stripeAccount = stripeAccountId
            ),
            PaymentController.StripeIntentType.PaymentIntent
        )
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
        activity: Activity,
        clientSecret: String,
        stripeAccountId: String? = this.stripeAccountId
    ) {
        paymentController.startAuth(
            AuthActivityStarter.Host.create(activity),
            PaymentIntent.ClientSecret(clientSecret).value,
            ApiRequest.Options(
                apiKey = publishableKey,
                stripeAccount = stripeAccountId
            ),
            PaymentController.StripeIntentType.PaymentIntent
        )
    }

    /**
     * Authenticate a [PaymentIntent].
     * Used for [manual confirmation](https://stripe.com/docs/payments/payment-intents/quickstart#manual-confirmation-flow) flow.
     *
     * @param fragment the `Fragment` that is launching the payment authentication flow
     * @param clientSecret the [client_secret](https://stripe.com/docs/api/payment_intents/object#payment_intent_object-client_secret)
     * property of a confirmed [PaymentIntent] object
     */
    @Deprecated(
        "Rename to better reflect behavior and match iOS naming.",
        ReplaceWith("handleNextActionForPayment(fragment, clientSecret)")
    )
    @UiThread
    fun authenticatePayment(fragment: Fragment, clientSecret: String) {
        paymentController.startAuth(
            AuthActivityStarter.Host.create(fragment),
            PaymentIntent.ClientSecret(clientSecret).value,
            ApiRequest.Options(
                apiKey = publishableKey,
                stripeAccount = stripeAccountId
            ),
            PaymentController.StripeIntentType.PaymentIntent
        )
    }

    /**
     * Handle the [next_action](https://stripe.com/docs/api/payment_intents/object#payment_intent_object-next_action)
     * for a previously confirmed [PaymentIntent].
     *
     * Used for [manual confirmation](https://stripe.com/docs/payments/payment-intents/android-manual) flow.
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
        paymentController.startAuth(
            AuthActivityStarter.Host.create(fragment),
            PaymentIntent.ClientSecret(clientSecret).value,
            ApiRequest.Options(
                apiKey = publishableKey,
                stripeAccount = stripeAccountId
            ),
            PaymentController.StripeIntentType.PaymentIntent
        )
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
        return if (data != null && paymentController.shouldHandlePaymentResult(requestCode, data)) {
            paymentController.handlePaymentResult(data, callback)
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
     * @param callback a [ApiResultCallback] to receive the result or error
     */
    @UiThread
    @JvmOverloads
    fun retrievePaymentIntent(
        clientSecret: String,
        stripeAccountId: String? = this.stripeAccountId,
        callback: ApiResultCallback<PaymentIntent>
    ) {
        executeAsync(callback) {
            stripeRepository.retrievePaymentIntent(
                clientSecret,
                ApiRequest.Options(
                    apiKey = publishableKey,
                    stripeAccount = stripeAccountId
                )
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
        stripeAccountId: String? = this.stripeAccountId
    ): PaymentIntent? {
        return runBlocking {
            stripeRepository.retrievePaymentIntent(
                PaymentIntent.ClientSecret(clientSecret).value,
                ApiRequest.Options(
                    apiKey = publishableKey,
                    stripeAccount = stripeAccountId
                )
            )
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
    ): PaymentIntent? {
        return runBlocking {
            stripeRepository.confirmPaymentIntent(
                confirmPaymentIntentParams,
                ApiRequest.Options(
                    apiKey = publishableKey,
                    stripeAccount = stripeAccountId,
                    idempotencyKey = idempotencyKey
                )
            )
        }
    }

    //
    // Setup Intents API - https://stripe.com/docs/api/setup_intents
    //

    /**
     * Confirm and, if necessary, authenticate a [SetupIntent].
     *
     * @param activity the `Activity` that is launching the payment authentication flow
     * @param stripeAccountId Optional, the Connect account to associate with this request.
     * By default, will use the Connect account that was used to instantiate the `Stripe` object, if specified.
     */
    @JvmOverloads
    fun confirmSetupIntent(
        activity: Activity,
        confirmSetupIntentParams: ConfirmSetupIntentParams,
        stripeAccountId: String? = this.stripeAccountId
    ) {
        paymentController.startConfirmAndAuth(
            AuthActivityStarter.Host.create(activity),
            confirmSetupIntentParams,
            ApiRequest.Options(
                apiKey = publishableKey,
                stripeAccount = stripeAccountId
            )
        )
    }

    /**
     * Confirm and, if necessary, authenticate a [SetupIntent].
     *
     * @param fragment the `Fragment` that is launching the payment authentication flow
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
        paymentController.startConfirmAndAuth(
            AuthActivityStarter.Host.create(fragment),
            confirmSetupIntentParams,
            ApiRequest.Options(
                apiKey = publishableKey,
                stripeAccount = stripeAccountId
            )
        )
    }

    /**
     * Authenticate a [SetupIntent]. Used for manual confirmation flow.
     *
     * @param activity the `Activity` that is launching the payment authentication flow
     * @param clientSecret the [client_secret](https://stripe.com/docs/api/setup_intents/object#setup_intent_object-client_secret)
     * property of a confirmed [SetupIntent] object
     */
    @Deprecated(
        "Rename to better reflect behavior and match iOS naming.",
        ReplaceWith("handleNextActionForSetupIntent(activity, clientSecret)")
    )
    @UiThread
    fun authenticateSetup(activity: Activity, clientSecret: String) {
        paymentController.startAuth(
            AuthActivityStarter.Host.create(activity),
            SetupIntent.ClientSecret(clientSecret).value,
            ApiRequest.Options(
                apiKey = publishableKey,
                stripeAccount = stripeAccountId
            ),
            PaymentController.StripeIntentType.SetupIntent
        )
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
        activity: Activity,
        clientSecret: String,
        stripeAccountId: String? = this.stripeAccountId
    ) {
        paymentController.startAuth(
            AuthActivityStarter.Host.create(activity),
            SetupIntent.ClientSecret(clientSecret).value,
            ApiRequest.Options(
                apiKey = publishableKey,
                stripeAccount = stripeAccountId
            ),
            PaymentController.StripeIntentType.SetupIntent
        )
    }

    /**
     * Authenticate a [SetupIntent]. Used for manual confirmation flow.
     *
     * @param fragment the `Fragment` launching the payment authentication flow
     * @param clientSecret the [client_secret](https://stripe.com/docs/api/setup_intents/object#setup_intent_object-client_secret)
     * property of a confirmed [SetupIntent] object
     */
    @Deprecated(
        "Rename to better reflect behavior and match iOS naming.",
        ReplaceWith("handleNextActionForSetupIntent(fragment, clientSecret)")
    )
    @UiThread
    fun authenticateSetup(fragment: Fragment, clientSecret: String) {
        paymentController.startAuth(
            AuthActivityStarter.Host.create(fragment),
            SetupIntent.ClientSecret(clientSecret).value,
            ApiRequest.Options(
                apiKey = publishableKey,
                stripeAccount = stripeAccountId
            ),
            PaymentController.StripeIntentType.SetupIntent
        )
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
        paymentController.startAuth(
            AuthActivityStarter.Host.create(fragment),
            SetupIntent.ClientSecret(clientSecret).value,
            ApiRequest.Options(
                apiKey = publishableKey,
                stripeAccount = stripeAccountId
            ),
            PaymentController.StripeIntentType.SetupIntent
        )
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
        return if (data != null && paymentController.shouldHandleSetupResult(requestCode, data)) {
            paymentController.handleSetupResult(data, callback)
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
        callback: ApiResultCallback<SetupIntent>
    ) {
        executeAsync(callback) {
            stripeRepository.retrieveSetupIntent(
                clientSecret,
                ApiRequest.Options(
                    apiKey = publishableKey,
                    stripeAccount = stripeAccountId
                )
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
        stripeAccountId: String? = this.stripeAccountId
    ): SetupIntent? {
        return runBlocking {
            stripeRepository.retrieveSetupIntent(
                SetupIntent.ClientSecret(clientSecret).value,
                ApiRequest.Options(
                    apiKey = publishableKey,
                    stripeAccount = stripeAccountId
                )
            )
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
    ): SetupIntent? {
        return runBlocking {
            stripeRepository.confirmSetupIntent(
                confirmSetupIntentParams,
                ApiRequest.Options(
                    apiKey = publishableKey,
                    stripeAccount = stripeAccountId,
                    idempotencyKey = idempotencyKey
                )
            )
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
        executeAsync(callback) {
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
    ): PaymentMethod? {
        return runBlocking {
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
        activity: Activity,
        source: Source,
        stripeAccountId: String? = this.stripeAccountId
    ) {
        paymentController.startAuthenticateSource(
            AuthActivityStarter.Host.create(activity),
            source,
            ApiRequest.Options(publishableKey, stripeAccountId)
        )
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
        paymentController.startAuthenticateSource(
            AuthActivityStarter.Host.create(fragment),
            source,
            ApiRequest.Options(publishableKey, stripeAccountId)
        )
    }

    /**
     * Should be called in `onActivityResult()` to determine if the result is for Source authentication
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
        paymentController.handleSourceResult(
            data,
            callback
        )
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
        executeAsync(callback) {
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
    ): Source? {
        return runBlocking {
            stripeRepository.createSource(
                params,
                ApiRequest.Options(
                    apiKey = publishableKey,
                    stripeAccount = stripeAccountId,
                    idempotencyKey = idempotencyKey
                )
            )
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
        executeAsync(callback) {
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
    ): Source? {
        return runBlocking {
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
    ): Token? {
        return try {
            runBlocking {
                stripeRepository.createToken(
                    accountParams,
                    ApiRequest.Options(
                        apiKey = publishableKey,
                        stripeAccount = stripeAccountId,
                        idempotencyKey = idempotencyKey
                    )
                )
            }
        } catch (exception: CardException) {
            // Should never occur. CardException is only for card related requests.
            null
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
            bankAccountTokenParams,
            stripeAccountId,
            idempotencyKey,
            callback
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
    ): Token? {
        return runBlocking {
            stripeRepository.createToken(
                bankAccountTokenParams,
                ApiRequest.Options(
                    apiKey = publishableKey,
                    stripeAccount = stripeAccountId,
                    idempotencyKey = idempotencyKey
                )
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
            PiiTokenParams(personalId),
            stripeAccountId,
            idempotencyKey,
            callback
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
    ): Token? {
        return runBlocking {
            stripeRepository.createToken(
                PiiTokenParams(personalId),
                ApiRequest.Options(
                    apiKey = publishableKey,
                    stripeAccount = stripeAccountId,
                    idempotencyKey = idempotencyKey
                )
            )
        }
    }

    /**
     * Create a Card token asynchronously.
     *
     * See [Create a card token](https://stripe.com/docs/api/tokens/create_card).
     * `POST /v1/tokens`
     *
     * @param card the [Card] used to create this payment token
     * @param idempotencyKey optional, see [Idempotent Requests](https://stripe.com/docs/api/idempotent_requests)
     * @param stripeAccountId Optional, the Connect account to associate with this request.
     * By default, will use the Connect account that was used to instantiate the `Stripe` object, if specified.
     * @param callback a [ApiResultCallback] to receive the result or error
     */
    @Deprecated("Use createCardToken(CardParams)")
    @UiThread
    @JvmOverloads
    fun createCardToken(
        card: Card,
        idempotencyKey: String? = null,
        stripeAccountId: String? = this.stripeAccountId,
        callback: ApiResultCallback<Token>
    ) {
        createToken(
            tokenParams = card,
            stripeAccountId = stripeAccountId,
            idempotencyKey = idempotencyKey,
            callback = callback
        )
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
            callback = callback
        )
    }

    /**
     * Blocking method to create a [Token]. Do not call this on the UI thread or your app
     * will crash.
     *
     * See [Create a card token](https://stripe.com/docs/api/tokens/create_card).
     * `POST /v1/tokens`
     *
     * @param card the [Card] to use for this token
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
    @Deprecated("Use createCardTokenSynchronous(CardParams)")
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
        card: Card,
        idempotencyKey: String? = null,
        stripeAccountId: String? = this.stripeAccountId
    ): Token? {
        return runBlocking {
            stripeRepository.createToken(
                card,
                ApiRequest.Options(
                    apiKey = publishableKey,
                    stripeAccount = stripeAccountId,
                    idempotencyKey = idempotencyKey
                )
            )
        }
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
    ): Token? {
        return runBlocking {
            stripeRepository.createToken(
                cardParams,
                ApiRequest.Options(
                    apiKey = publishableKey,
                    stripeAccount = stripeAccountId,
                    idempotencyKey = idempotencyKey
                )
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
            CvcTokenParams(cvc),
            stripeAccountId,
            idempotencyKey,
            callback
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
    ): Token? {
        return runBlocking {
            stripeRepository.createToken(
                CvcTokenParams(cvc),
                ApiRequest.Options(
                    apiKey = publishableKey,
                    stripeAccount = stripeAccountId,
                    idempotencyKey = idempotencyKey
                )
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
            params,
            stripeAccountId,
            idempotencyKey,
            callback
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
    ): Token? {
        return runBlocking {
            stripeRepository.createToken(
                params,
                ApiRequest.Options(
                    apiKey = publishableKey,
                    stripeAccount = stripeAccountId,
                    idempotencyKey = idempotencyKey
                )
            )
        }
    }

    private fun createToken(
        tokenParams: TokenParams,
        stripeAccountId: String?,
        idempotencyKey: String? = null,
        callback: ApiResultCallback<Token>
    ) {
        executeAsync(callback) {
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
        executeAsync(callback) {
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
            )
        }
    }

    private fun <T : StripeModel> executeAsync(
        callback: ApiResultCallback<T>,
        apiMethod: suspend () -> T?
    ) {
        CoroutineScope(workContext).launch {
            val result = runCatching {
                requireNotNull(apiMethod())
            }
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

        internal const val VERSION_NAME = "16.3.0"
        const val VERSION: String = "AndroidBindings/$VERSION_NAME"

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
