package com.stripe.android

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import androidx.annotation.Size
import androidx.annotation.VisibleForTesting
import androidx.fragment.app.Fragment
import com.stripe.android.exception.APIConnectionException
import com.stripe.android.exception.APIException
import com.stripe.android.exception.AuthenticationException
import com.stripe.android.exception.CardException
import com.stripe.android.exception.InvalidRequestException
import com.stripe.android.exception.StripeException
import com.stripe.android.model.AccountParams
import com.stripe.android.model.BankAccount
import com.stripe.android.model.Card
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.ConfirmSetupIntentParams
import com.stripe.android.model.CvcTokenParams
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PiiTokenParams
import com.stripe.android.model.SetupIntent
import com.stripe.android.model.Source
import com.stripe.android.model.SourceParams
import com.stripe.android.model.Token
import com.stripe.android.view.AuthActivityStarter
import java.util.concurrent.Executor

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
    private val stripeNetworkUtils: StripeNetworkUtils,
    private val paymentController: PaymentController,
    publishableKey: String,
    private val stripeAccountId: String?,
    private val tokenCreator: TokenCreator
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
            appInfo,
            Logger.getInstance(enableLogging)
        ),
        StripeNetworkUtils(context.applicationContext),
        ApiKeyValidator.get().requireValid(publishableKey),
        stripeAccountId,
        enableLogging
    )

    private constructor(
        context: Context,
        stripeRepository: StripeRepository,
        stripeNetworkUtils: StripeNetworkUtils,
        publishableKey: String,
        stripeAccountId: String?,
        enableLogging: Boolean
    ) : this(
        stripeRepository,
        stripeNetworkUtils,
        PaymentController.create(context.applicationContext, stripeRepository, enableLogging),
        publishableKey,
        stripeAccountId
    )

    internal constructor(
        stripeRepository: StripeRepository,
        stripeNetworkUtils: StripeNetworkUtils,
        paymentController: PaymentController,
        publishableKey: String,
        stripeAccountId: String?
    ) : this(
        stripeRepository,
        stripeNetworkUtils,
        paymentController,
        publishableKey,
        stripeAccountId,
        object : TokenCreator {
            override fun create(
                params: Map<String, Any>,
                options: ApiRequest.Options,
                @Token.TokenType tokenType: String,
                executor: Executor?,
                callback: ApiResultCallback<Token>
            ) {
                executeTask(executor,
                    CreateTokenTask(stripeRepository, params, options,
                        tokenType, callback))
            }
        }
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
     */
    fun confirmPayment(
        activity: Activity,
        confirmPaymentIntentParams: ConfirmPaymentIntentParams
    ) {
        paymentController.startConfirmAndAuth(
            AuthActivityStarter.Host.create(activity),
            confirmPaymentIntentParams,
            ApiRequest.Options.create(publishableKey, stripeAccountId)
        )
    }

    /**
     * Confirm and, if necessary, authenticate a [PaymentIntent].
     * Used for [automatic confirmation](https://stripe.com/docs/payments/payment-intents/quickstart#automatic-confirmation-flow) flow.
     *
     * @param fragment the `Fragment` that is launching the payment authentication flow
     * @param confirmPaymentIntentParams [ConfirmPaymentIntentParams] used to confirm the [PaymentIntent]
     */
    fun confirmPayment(
        fragment: Fragment,
        confirmPaymentIntentParams: ConfirmPaymentIntentParams
    ) {
        paymentController.startConfirmAndAuth(
            AuthActivityStarter.Host.create(fragment),
            confirmPaymentIntentParams,
            ApiRequest.Options.create(publishableKey, stripeAccountId)
        )
    }

    /**
     * Authenticate a [PaymentIntent].
     * Used for [manual confirmation](https://stripe.com/docs/payments/payment-intents/quickstart#manual-confirmation-flow) flow.
     *
     * @param activity the `Activity` that is launching the payment authentication flow
     * @param clientSecret the [client_secret](https://stripe.com/docs/api/payment_intents/object#payment_intent_object-client_secret)
     * property of a confirmed [PaymentIntent] object
     */
    fun authenticatePayment(activity: Activity, clientSecret: String) {
        paymentController.startAuth(
            AuthActivityStarter.Host.create(activity),
            clientSecret,
            ApiRequest.Options.create(publishableKey, stripeAccountId)
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
    fun authenticatePayment(fragment: Fragment, clientSecret: String) {
        paymentController.startAuth(
            AuthActivityStarter.Host.create(fragment),
            clientSecret,
            ApiRequest.Options.create(publishableKey, stripeAccountId)
        )
    }

    /**
     * Should be called via `Activity#onActivityResult(int, int, Intent)}}` to handle the
     * result of a PaymentIntent automatic confirmation (see [confirmPayment]) or
     * manual confirmation (see [authenticatePayment]})
     */
    fun onPaymentResult(
        requestCode: Int,
        data: Intent?,
        callback: ApiResultCallback<PaymentIntentResult>
    ): Boolean {
        return if (data != null && paymentController.shouldHandlePaymentResult(requestCode, data)) {
            paymentController.handlePaymentResult(
                data,
                ApiRequest.Options.create(publishableKey, stripeAccountId),
                callback
            )
            true
        } else {
            false
        }
    }

    /**
     * Blocking method to retrieve a [PaymentIntent] object.
     * Do not call this on the UI thread or your app will crash.
     *
     *
     * See [Retrieve a PaymentIntent](https://stripe.com/docs/api/payment_intents/retrieve).
     *
     * @param clientSecret the client_secret with which to retrieve the PaymentIntent
     * @return a [PaymentIntent] or `null` if a problem occurred
     */
    @Throws(APIException::class, AuthenticationException::class,
        InvalidRequestException::class, APIConnectionException::class)
    fun retrievePaymentIntentSynchronous(clientSecret: String): PaymentIntent? {
        return stripeRepository.retrievePaymentIntent(
            clientSecret,
            ApiRequest.Options.create(publishableKey, stripeAccountId)
        )
    }

    /**
     * Blocking method to confirm a [PaymentIntent] object.
     * Do not call this on the UI thread or your app will crash.
     *
     * See [Confirm a PaymentIntent](https://stripe.com/docs/api/payment_intents/confirm).
     *
     * @param confirmPaymentIntentParams a set of params with which to confirm the PaymentIntent
     * @return a [PaymentIntent] or `null` if a problem occurred
     *
     */
    @Deprecated("use {@link #confirmPayment(Activity, ConfirmPaymentIntentParams)}")
    @Throws(AuthenticationException::class, InvalidRequestException::class,
        APIConnectionException::class, APIException::class)
    fun confirmPaymentIntentSynchronous(
        confirmPaymentIntentParams: ConfirmPaymentIntentParams
    ): PaymentIntent? {
        return stripeRepository.confirmPaymentIntent(
            confirmPaymentIntentParams,
            ApiRequest.Options.create(publishableKey, stripeAccountId)
        )
    }

    //
    // Setup Intents API - https://stripe.com/docs/api/setup_intents
    //

    /**
     * Confirm and, if necessary, authenticate a [SetupIntent].
     *
     * @param activity the `Activity` that is launching the payment authentication flow
     */
    fun confirmSetupIntent(
        activity: Activity,
        confirmSetupIntentParams: ConfirmSetupIntentParams
    ) {
        paymentController.startConfirmAndAuth(
            AuthActivityStarter.Host.create(activity),
            confirmSetupIntentParams,
            ApiRequest.Options.create(publishableKey, stripeAccountId)
        )
    }

    /**
     * Confirm and, if necessary, authenticate a [SetupIntent].
     *
     * @param fragment the `Fragment` that is launching the payment authentication flow
     */
    fun confirmSetupIntent(
        fragment: Fragment,
        confirmSetupIntentParams: ConfirmSetupIntentParams
    ) {
        paymentController.startConfirmAndAuth(
            AuthActivityStarter.Host.create(fragment),
            confirmSetupIntentParams,
            ApiRequest.Options.create(publishableKey, stripeAccountId)
        )
    }

    /**
     * Authenticate a [SetupIntent]. Used for manual confirmation flow.
     *
     * @param activity the `Activity` that is launching the payment authentication flow
     * @param clientSecret the [client_secret](https://stripe.com/docs/api/setup_intents/object#setup_intent_object-client_secret)
     * property of a confirmed [SetupIntent] object
     */
    fun authenticateSetup(activity: Activity, clientSecret: String) {
        paymentController.startAuth(
            AuthActivityStarter.Host.create(activity),
            clientSecret,
            ApiRequest.Options.create(publishableKey, stripeAccountId)
        )
    }

    /**
     * Authenticate a [SetupIntent]. Used for manual confirmation flow.
     *
     * @param fragment the `Fragment` launching the payment authentication flow
     * @param clientSecret the [client_secret](https://stripe.com/docs/api/setup_intents/object#setup_intent_object-client_secret)
     * property of a confirmed [SetupIntent] object
     */
    fun authenticateSetup(fragment: Fragment, clientSecret: String) {
        paymentController.startAuth(
            AuthActivityStarter.Host.create(fragment),
            clientSecret,
            ApiRequest.Options.create(publishableKey, stripeAccountId)
        )
    }

    /**
     * Should be called via `Activity#onActivityResult(int, int, Intent)}}` to handle the
     * result of a SetupIntent confirmation (see [confirmSetupIntent]).
     */
    fun onSetupResult(
        requestCode: Int,
        data: Intent?,
        callback: ApiResultCallback<SetupIntentResult>
    ): Boolean {
        return if (data != null && paymentController.shouldHandleSetupResult(requestCode, data)) {
            paymentController.handleSetupResult(
                data,
                ApiRequest.Options.create(publishableKey, stripeAccountId),
                callback
            )
            true
        } else {
            false
        }
    }

    /**
     * Blocking method to retrieve a [SetupIntent] object.
     * Do not call this on the UI thread or your app will crash.
     *
     *
     * See [Retrieve a SetupIntent](https://stripe.com/docs/api/setup_intents/retrieve).
     *
     * @param clientSecret client_secret of the SetupIntent to retrieve
     * @return a [SetupIntent] or `null` if a problem occurred
     */
    @Throws(APIException::class, AuthenticationException::class, InvalidRequestException::class,
        APIConnectionException::class)
    fun retrieveSetupIntentSynchronous(clientSecret: String): SetupIntent? {
        return stripeRepository.retrieveSetupIntent(
            clientSecret,
            ApiRequest.Options.create(publishableKey, stripeAccountId)
        )
    }

    /**
     * Blocking method to confirm a [SetupIntent] object.
     * Do not call this on the UI thread or your app will crash.
     *
     *
     * See [Confirm a SetupIntent](https://stripe.com/docs/api/setup_intents/confirm).
     *
     * @param confirmSetupIntentParams a set of params with which to confirm the Setup Intent
     * @return a [SetupIntent] or `null` if a problem occurred
     *
     */
    @Deprecated("use {@link #confirmSetupIntent(Activity, ConfirmSetupIntentParams)}")
    @Throws(AuthenticationException::class, InvalidRequestException::class,
        APIConnectionException::class, APIException::class)
    fun confirmSetupIntentSynchronous(
        confirmSetupIntentParams: ConfirmSetupIntentParams
    ): SetupIntent? {
        return stripeRepository.confirmSetupIntent(
            confirmSetupIntentParams,
            ApiRequest.Options.create(publishableKey, stripeAccountId)
        )
    }

    //
    // Payment Methods API - https://stripe.com/docs/api/payment_methods
    //

    /**
     * Create a [PaymentMethod] asynchronously.
     *
     * See [Create a PaymentMethod](https://stripe.com/docs/api/payment_methods/create).
     *
     * @param paymentMethodCreateParams the [PaymentMethodCreateParams] to be used
     * @param callback a [ApiResultCallback] to receive the result or error
     */
    fun createPaymentMethod(
        paymentMethodCreateParams: PaymentMethodCreateParams,
        callback: ApiResultCallback<PaymentMethod>
    ) {
        CreatePaymentMethodTask(
            stripeRepository, paymentMethodCreateParams, publishableKey,
            stripeAccountId, callback
        ).execute()
    }

    /**
     * Blocking method to create a [PaymentMethod] object.
     * Do not call this on the UI thread or your app will crash.
     *
     * See [Create a PaymentMethod](https://stripe.com/docs/api/payment_methods/create).
     *
     * @param paymentMethodCreateParams params with which to create the PaymentMethod
     *
     * @return a [PaymentMethod] or `null` if a problem occurred
     */
    @Throws(APIException::class, AuthenticationException::class, InvalidRequestException::class,
        APIConnectionException::class)
    fun createPaymentMethodSynchronous(
        paymentMethodCreateParams: PaymentMethodCreateParams
    ): PaymentMethod? {
        return stripeRepository.createPaymentMethod(
            paymentMethodCreateParams,
            ApiRequest.Options.create(publishableKey, stripeAccountId)
        )
    }

    //
    // Sources API - https://stripe.com/docs/api/sources
    //

    /**
     * Create a [Source] asynchronously.
     *
     * See [Create a source](https://stripe.com/docs/api/sources/create).
     *
     * @param sourceParams the [SourceParams] to be used
     * @param callback a [ApiResultCallback] to receive the result or error
     */
    fun createSource(
        sourceParams: SourceParams,
        callback: ApiResultCallback<Source>
    ) {
        CreateSourceTask(
            stripeRepository, sourceParams, publishableKey, stripeAccountId, callback
        ).execute()
    }

    /**
     * Blocking method to create a [Source] object.
     * Do not call this on the UI thread or your app will crash.
     *
     *
     * See [Create a source](https://stripe.com/docs/api/sources/create).
     *
     * @param params a set of [SourceParams] with which to create the source
     * @return a [Source], or `null` if a problem occurred
     * @throws AuthenticationException failure to properly authenticate yourself (check your key)
     * @throws InvalidRequestException your request has invalid parameters
     * @throws APIConnectionException failure to connect to Stripe's API
     * @throws APIException any other type of problem (for instance, a temporary issue with
     * Stripe's servers
     */
    @Throws(AuthenticationException::class, InvalidRequestException::class,
        APIConnectionException::class, APIException::class)
    fun createSourceSynchronous(params: SourceParams): Source? {
        return stripeRepository.createSource(params,
            ApiRequest.Options.create(publishableKey, stripeAccountId))
    }

    /**
     * Retrieve an existing [Source] from the Stripe API. Note that this is a
     * synchronous method, and cannot be called on the main thread. Doing so will cause your app
     * to crash.
     *
     * See [Retrieve a source](https://stripe.com/docs/api/sources/retrieve).
     *
     * @param sourceId the [Source.id] field of the desired Source object
     * @param clientSecret the [Source.getClientSecret] field of the desired Source object
     * @return a [Source] if one could be found based on the input params, or `null` if
     * no such Source could be found.
     * @throws AuthenticationException failure to properly authenticate yourself (check your key)
     * @throws InvalidRequestException your request has invalid parameters
     * @throws APIConnectionException failure to connect to Stripe's API
     * @throws APIException any other type of problem (for instance, a temporary issue with Stripe's servers)
     */
    @Throws(AuthenticationException::class, InvalidRequestException::class,
        APIConnectionException::class, APIException::class)
    fun retrieveSourceSynchronous(
        @Size(min = 1) sourceId: String,
        @Size(min = 1) clientSecret: String
    ): Source? {
        return stripeRepository.retrieveSource(sourceId, clientSecret,
            ApiRequest.Options.create(publishableKey, stripeAccountId))
    }

    //
    // Tokens API - https://stripe.com/docs/api/tokens
    //

    /**
     * Create a [Token] asynchronously.
     *
     * See [Create an account token](https://stripe.com/docs/api/tokens/create_account).
     *
     * @param accountParams the [AccountParams] used to create this token
     * @param callback a [ApiResultCallback] to receive the result or error
     */
    fun createAccountToken(
        accountParams: AccountParams,
        callback: ApiResultCallback<Token>
    ) {
        val params = accountParams.toParamMap()
            .plus(stripeNetworkUtils.createUidParams())
        createTokenFromParams(
            params,
            Token.TokenType.ACCOUNT,
            callback
        )
    }

    /**
     * Blocking method to create a [Token] for a Connect Account. Do not call this on the UI
     * thread or your app will crash.
     *
     * See [Create an account token](https://stripe.com/docs/api/tokens/create_account).
     *
     * @param accountParams params to use for this token.
     * @return a [Token] that can be used for this account.
     * @throws AuthenticationException failure to properly authenticate yourself (check your key)
     * @throws InvalidRequestException your request has invalid parameters
     * @throws APIConnectionException failure to connect to Stripe's API
     * @throws APIException any other type of problem (for instance, a temporary issue with
     * Stripe's servers)
     */
    @Throws(AuthenticationException::class, InvalidRequestException::class,
        APIConnectionException::class, APIException::class)
    fun createAccountTokenSynchronous(accountParams: AccountParams): Token? {
        return try {
            stripeRepository.createToken(
                accountParams.toParamMap(),
                ApiRequest.Options.create(publishableKey, stripeAccountId),
                Token.TokenType.ACCOUNT
            )
        } catch (exception: CardException) {
            // Should never occur. CardException is only for card related requests.
            null
        }
    }

    /**
     * Create a [BankAccount] token asynchronously.
     *
     * See [Create a bank account token](https://stripe.com/docs/api/tokens/create_bank_account).
     *
     * @param bankAccount the [BankAccount] used to create this token
     * @param callback a [ApiResultCallback] to receive the result or error
     */
    fun createBankAccountToken(
        bankAccount: BankAccount,
        callback: ApiResultCallback<Token>
    ) {
        val params = bankAccount.toParamMap()
            .plus(stripeNetworkUtils.createUidParams())
        createTokenFromParams(
            params,
            Token.TokenType.BANK_ACCOUNT,
            callback
        )
    }

    /**
     * Blocking method to create a [Token] for a [BankAccount]. Do not call this on
     * the UI thread or your app will crash.
     *
     * See [Create a bank account token](https://stripe.com/docs/api/tokens/create_bank_account).
     *
     * @param bankAccount the [Card] to use for this token
     * @return a [Token] that can be used for this [BankAccount]
     * @throws AuthenticationException failure to properly authenticate yourself (check your key)
     * @throws InvalidRequestException your request has invalid parameters
     * @throws APIConnectionException failure to connect to Stripe's API
     * @throws CardException should not be thrown with this type of token, but is theoretically
     * possible given the underlying methods called
     * @throws APIException any other type of problem (for instance, a temporary issue with
     * Stripe's servers
     */
    @Throws(AuthenticationException::class, InvalidRequestException::class,
        APIConnectionException::class, CardException::class, APIException::class)
    fun createBankAccountTokenSynchronous(bankAccount: BankAccount): Token? {
        val params = bankAccount.toParamMap()
            .plus(stripeNetworkUtils.createUidParams())
        return stripeRepository.createToken(
            params,
            ApiRequest.Options.create(publishableKey, stripeAccountId),
            Token.TokenType.BANK_ACCOUNT
        )
    }

    /**
     * Create a PII token asynchronously.
     *
     *
     * See [Create a PII account token](https://stripe.com/docs/api/tokens/create_pii).
     *
     * @param personalId the personal id used to create this token
     * @param callback a [ApiResultCallback] to receive the result or error
     */
    fun createPiiToken(
        personalId: String,
        callback: ApiResultCallback<Token>
    ) {
        createTokenFromParams(
            PiiTokenParams(personalId).toParamMap(),
            Token.TokenType.PII,
            callback
        )
    }

    /**
     * Blocking method to create a [Token] for PII. Do not call this on the UI thread
     * or your app will crash.
     *
     * See [Create a PII account token](https://stripe.com/docs/api/tokens/create_pii).
     *
     * @param personalId the personal ID to use for this token
     * @return a [Token] that can be used for this card
     * @throws AuthenticationException failure to properly authenticate yourself (check your key)
     * @throws InvalidRequestException your request has invalid parameters
     * @throws APIConnectionException failure to connect to Stripe's API
     * @throws APIException any other type of problem (for instance, a temporary issue with
     * Stripe's servers)
     */
    @Throws(AuthenticationException::class, InvalidRequestException::class,
        APIConnectionException::class, CardException::class, APIException::class)
    fun createPiiTokenSynchronous(personalId: String): Token? {
        return stripeRepository.createToken(
            PiiTokenParams(personalId).toParamMap(),
            ApiRequest.Options.create(publishableKey, stripeAccountId),
            Token.TokenType.PII
        )
    }

    /**
     * Create a Card token asynchronously.
     *
     * See [Create a card token](https://stripe.com/docs/api/tokens/create_card).
     *
     * @param card the [Card] used to create this payment token
     * @param callback a [ApiResultCallback] to receive the result or error
     */
    fun createToken(card: Card, callback: ApiResultCallback<Token>) {
        createTokenFromParams(
            stripeNetworkUtils.createCardTokenParams(card),
            Token.TokenType.CARD,
            callback
        )
    }

    /**
     * Blocking method to create a [Token]. Do not call this on the UI thread or your app
     * will crash.
     *
     * See [Create a card token](https://stripe.com/docs/api/tokens/create_card).
     *
     * @param card the [Card] to use for this token
     * @return a [Token] that can be used for this card
     * @throws AuthenticationException failure to properly authenticate yourself (check your key)
     * @throws InvalidRequestException your request has invalid parameters
     * @throws APIConnectionException failure to connect to Stripe's API
     * @throws CardException the card cannot be charged for some reason
     * @throws APIException any other type of problem (for instance, a temporary issue with
     * Stripe's servers
     */
    @Throws(AuthenticationException::class, InvalidRequestException::class,
        APIConnectionException::class, CardException::class, APIException::class)
    fun createCardTokenSynchronous(card: Card): Token? {
        return stripeRepository.createToken(
            stripeNetworkUtils.createCardTokenParams(card),
            ApiRequest.Options.create(publishableKey, stripeAccountId),
            Token.TokenType.CARD
        )
    }

    /**
     * Blocking method to create a [Token]. Do not call this on the UI thread or your app
     * will crash.
     *
     * See [Create a card token](https://stripe.com/docs/api/tokens/create_card).
     *
     * @param card the [Card] to use for this token
     * @return a [Token] that can be used for this card
     * @throws AuthenticationException failure to properly authenticate yourself (check your key)
     * @throws InvalidRequestException your request has invalid parameters
     * @throws APIConnectionException failure to connect to Stripe's API
     * @throws CardException the card cannot be charged for some reason
     * @throws APIException any other type of problem (for instance, a temporary issue with
     * Stripe's servers
     *
     * @deprecated use [createCardTokenSynchronous]
     */
    @Deprecated("Use createCardTokenSynchronous()")
    @Throws(AuthenticationException::class, InvalidRequestException::class,
        APIConnectionException::class, CardException::class, APIException::class)
    fun createTokenSynchronous(card: Card): Token? {
        return stripeRepository.createToken(
            stripeNetworkUtils.createCardTokenParams(card),
            ApiRequest.Options.create(publishableKey, stripeAccountId),
            Token.TokenType.CARD
        )
    }

    /**
     * Create a CVC update token asynchronously.
     *
     * @param cvc the CVC used to create this token
     * @param callback a [ApiResultCallback] to receive the result or error
     */
    fun createCvcUpdateToken(
        @Size(min = 3, max = 4) cvc: String,
        callback: ApiResultCallback<Token>
    ) {
        createTokenFromParams(
            CvcTokenParams(cvc).toParamMap(),
            Token.TokenType.CVC_UPDATE,
            callback
        )
    }

    /**
     * Blocking method to create a [Token] for CVC updating. Do not call this on the UI thread
     * or your app will crash.
     *
     * @param cvc the CVC to use for this token
     * @return a [Token] that can be used for this card
     * @throws AuthenticationException failure to properly authenticate yourself (check your key)
     * @throws InvalidRequestException your request has invalid parameters
     * @throws APIConnectionException failure to connect to Stripe's API
     * @throws APIException any other type of problem (for instance, a temporary issue with
     * Stripe's servers)
     */
    @Throws(AuthenticationException::class, InvalidRequestException::class,
        APIConnectionException::class, CardException::class, APIException::class)
    fun createCvcUpdateTokenSynchronous(cvc: String): Token? {
        return stripeRepository.createToken(
            CvcTokenParams(cvc).toParamMap(),
            ApiRequest.Options.create(publishableKey, stripeAccountId),
            Token.TokenType.CVC_UPDATE
        )
    }

    private fun createTokenFromParams(
        tokenParams: Map<String, Any>,
        @Token.TokenType tokenType: String,
        callback: ApiResultCallback<Token>
    ) {
        tokenCreator.create(
            tokenParams,
            ApiRequest.Options.create(publishableKey, stripeAccountId),
            tokenType, null,
            callback
        )
    }

    @VisibleForTesting
    internal interface TokenCreator {
        fun create(
            params: Map<String, Any>,
            options: ApiRequest.Options,
            @Token.TokenType tokenType: String,
            executor: Executor?,
            callback: ApiResultCallback<Token>
        )
    }

    private class CreateSourceTask internal constructor(
        private val stripeRepository: StripeRepository,
        private val sourceParams: SourceParams,
        publishableKey: String,
        stripeAccount: String?,
        callback: ApiResultCallback<Source>
    ) : ApiOperation<Source>(callback) {
        private val options: ApiRequest.Options =
            ApiRequest.Options.create(publishableKey, stripeAccount)

        @Throws(StripeException::class)
        override fun getResult(): Source? {
            return stripeRepository.createSource(sourceParams, options)
        }
    }

    private class CreatePaymentMethodTask internal constructor(
        private val stripeRepository: StripeRepository,
        private val paymentMethodCreateParams: PaymentMethodCreateParams,
        publishableKey: String,
        stripeAccount: String?,
        callback: ApiResultCallback<PaymentMethod>
    ) : ApiOperation<PaymentMethod>(callback) {
        private val options: ApiRequest.Options =
            ApiRequest.Options.create(publishableKey, stripeAccount)

        @Throws(StripeException::class)
        override fun getResult(): PaymentMethod? {
            return stripeRepository.createPaymentMethod(paymentMethodCreateParams, options)
        }
    }

    private class CreateTokenTask internal constructor(
        private val stripeRepository: StripeRepository,
        private val tokenParams: Map<String, Any>,
        private val options: ApiRequest.Options,
        @param:Token.TokenType @field:Token.TokenType private val tokenType: String,
        callback: ApiResultCallback<Token>
    ) : ApiOperation<Token>(callback) {

        @Throws(StripeException::class)
        override fun getResult(): Token? {
            return stripeRepository.createToken(tokenParams, options, tokenType)
        }
    }

    companion object {
        @JvmField
        val API_VERSION: String = ApiVersion.get().code

        const val VERSION: String = "AndroidBindings/${BuildConfig.VERSION_NAME}"

        /**
         * Setter for identifying your plug-in or library.
         *
         * See [Building Stripe Plug-ins and Libraries - Setting the API version](https://stripe.com/docs/building-plugins#setappinfo).
         */
        private var appInfo: AppInfo? = null

        @JvmStatic
        fun setAppInfo(appInfo: AppInfo) {
            this.appInfo = appInfo
        }

        @JvmStatic
        fun getAppInfo(): AppInfo? {
            return this.appInfo
        }

        private fun executeTask(
            executor: Executor?,
            task: AsyncTask<Void, Void, *>
        ) {
            if (executor != null) {
                task.executeOnExecutor(executor)
            } else {
                task.execute()
            }
        }
    }
}
