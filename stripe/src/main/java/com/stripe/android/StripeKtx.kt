package com.stripe.android

import android.content.Intent
import androidx.annotation.Size
import com.stripe.android.exception.APIConnectionException
import com.stripe.android.exception.APIException
import com.stripe.android.exception.AuthenticationException
import com.stripe.android.exception.CardException
import com.stripe.android.exception.InvalidRequestException
import com.stripe.android.exception.StripeException
import com.stripe.android.model.AccountParams
import com.stripe.android.model.BankAccountTokenParams
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
import com.stripe.android.model.StripeModel
import com.stripe.android.model.Token
import com.stripe.android.model.WeChatPayNextAction
import com.stripe.android.networking.ApiRequest

/**
 * Confirm and authenticate a [PaymentIntent] using the Alipay SDK
 * @see <a href="https://intl.alipay.com/docs/ac/app/sdk_integration">Alipay Documentation</a>
 *
 * @param confirmPaymentIntentParams [ConfirmPaymentIntentParams] used to confirm the
 * [PaymentIntent]
 * @param authenticator a [AlipayAuthenticator] used to interface with the Alipay SDK
 * @param stripeAccountId Optional, the Connect account to associate with this request.
 * By default, will use the Connect account that was used to instantiate the `Stripe` object, if specified.
 *
 * @return a [PaymentIntentResult] object
 *
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
suspend fun Stripe.confirmAlipayPayment(
    confirmPaymentIntentParams: ConfirmPaymentIntentParams,
    authenticator: AlipayAuthenticator,
    stripeAccountId: String? = this.stripeAccountId
): PaymentIntentResult = runApiRequest {
    paymentController.confirmAndAuthenticateAlipay(
        confirmPaymentIntentParams,
        authenticator,
        ApiRequest.Options(
            apiKey = publishableKey,
            stripeAccount = stripeAccountId
        )
    )
}

/**
 * Create a [PaymentMethod] from a coroutine.
 *
 * See [Create a PaymentMethod](https://stripe.com/docs/api/payment_methods/create).
 * `POST /v1/payment_methods`
 *
 * @param paymentMethodCreateParams the [PaymentMethodCreateParams] to be used
 * @param idempotencyKey optional, see [Idempotent Requests](https://stripe.com/docs/api/idempotent_requests)
 * @param stripeAccountId Optional, the Connect account to associate with this request.
 * By default, will use the Connect account that was used to instantiate the `Stripe` object, if specified.
 *
 * @return a [PaymentMethod] object
 *
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
suspend fun Stripe.createPaymentMethod(
    paymentMethodCreateParams: PaymentMethodCreateParams,
    idempotencyKey: String? = null,
    stripeAccountId: String? = this.stripeAccountId
): PaymentMethod = runApiRequest {
    stripeRepository.createPaymentMethod(
        paymentMethodCreateParams,
        ApiRequest.Options(
            apiKey = publishableKey,
            stripeAccount = stripeAccountId,
            idempotencyKey = idempotencyKey
        )
    )
}

/**
 * Create a [Source] from a coroutine.
 *
 * See [Create a source](https://stripe.com/docs/api/sources/create).
 * `POST /v1/sources`
 *
 * @param sourceParams the [SourceParams] to be used
 * @param idempotencyKey optional, see [Idempotent Requests](https://stripe.com/docs/api/idempotent_requests)
 * @param stripeAccountId Optional, the Connect account to associate with this request.
 * By default, will use the Connect account that was used to instantiate the `Stripe` object, if specified.
 *
 * @return a [Source] object
 *
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
suspend fun Stripe.createSource(
    sourceParams: SourceParams,
    idempotencyKey: String? = null,
    stripeAccountId: String? = this.stripeAccountId
): Source = runApiRequest {
    stripeRepository.createSource(
        sourceParams,
        ApiRequest.Options(
            apiKey = publishableKey,
            stripeAccount = stripeAccountId,
            idempotencyKey = idempotencyKey
        )
    )
}

/**
 * Create a [Token] from a coroutine.
 *
 * See [Create an account token](https://stripe.com/docs/api/tokens/create_account).
 * `POST /v1/tokens`
 *
 * @param accountParams the [AccountParams] used to create this token
 * @param idempotencyKey optional, see [Idempotent Requests](https://stripe.com/docs/api/idempotent_requests)
 * @param stripeAccountId Optional, the Connect account to associate with this request.
 * By default, will use the Connect account that was used to instantiate the `Stripe` object, if specified.
 *
 * @return a [Token] object
 *
 * @throws AuthenticationException failure to properly authenticate yourself (check your key)
 * @throws InvalidRequestException your request has invalid parameters
 * @throws APIConnectionException failure to connect to Stripe's API
 * @throws APIException any other type of problem (for instance, a temporary issue with Stripe's servers)
 */
@Throws(
    AuthenticationException::class,
    InvalidRequestException::class,
    APIConnectionException::class,
    APIException::class,
)
suspend fun Stripe.createAccountToken(
    accountParams: AccountParams,
    idempotencyKey: String? = null,
    stripeAccountId: String? = this.stripeAccountId
): Token = runApiRequest {
    stripeRepository.createToken(
        accountParams,
        ApiRequest.Options(
            apiKey = publishableKey,
            stripeAccount = stripeAccountId,
            idempotencyKey = idempotencyKey
        )
    )
}

/**
 * Create a bank account token from a coroutine.
 *
 * See [Create a bank account token](https://stripe.com/docs/api/tokens/create_bank_account).
 * `POST /v1/tokens`
 *
 * @param bankAccountTokenParams the [BankAccountTokenParams] used to create this token
 * @param idempotencyKey optional, see [Idempotent Requests](https://stripe.com/docs/api/idempotent_requests)
 * @param stripeAccountId Optional, the Connect account to associate with this request.
 * By default, will use the Connect account that was used to instantiate the `Stripe` object, if specified.
 *
 * @return a [Token] object
 *
 * @throws AuthenticationException failure to properly authenticate yourself (check your key)
 * @throws InvalidRequestException your request has invalid parameters
 * @throws APIConnectionException failure to connect to Stripe's API
 * @throws APIException any other type of problem (for instance, a temporary issue with Stripe's servers)
 */
@Throws(
    AuthenticationException::class,
    InvalidRequestException::class,
    APIConnectionException::class,
    APIException::class,
)
suspend fun Stripe.createBankAccountToken(
    bankAccountTokenParams: BankAccountTokenParams,
    idempotencyKey: String? = null,
    stripeAccountId: String? = this.stripeAccountId
): Token = runApiRequest {
    stripeRepository.createToken(
        bankAccountTokenParams,
        ApiRequest.Options(
            apiKey = publishableKey,
            stripeAccount = stripeAccountId,
            idempotencyKey = idempotencyKey
        )
    )
}

/**
 * Create a PII token from a coroutine.
 *
 * See [Create a PII account token](https://stripe.com/docs/api/tokens/create_pii).
 * `POST /v1/tokens`
 *
 * @param personalId the personal id used to create this token
 * @param idempotencyKey optional, see [Idempotent Requests](https://stripe.com/docs/api/idempotent_requests)
 * @param stripeAccountId Optional, the Connect account to associate with this request.
 * By default, will use the Connect account that was used to instantiate the `Stripe` object, if specified.
 *
 * @return a [Token] object
 *
 * @throws AuthenticationException failure to properly authenticate yourself (check your key)
 * @throws InvalidRequestException your request has invalid parameters
 * @throws APIConnectionException failure to connect to Stripe's API
 * @throws APIException any other type of problem (for instance, a temporary issue with Stripe's servers)
 */
@Throws(
    AuthenticationException::class,
    InvalidRequestException::class,
    APIConnectionException::class,
    APIException::class,
)
suspend fun Stripe.createPiiToken(
    personalId: String,
    idempotencyKey: String? = null,
    stripeAccountId: String? = this.stripeAccountId
): Token = runApiRequest {
    stripeRepository.createToken(
        PiiTokenParams(personalId),
        ApiRequest.Options(
            apiKey = publishableKey,
            stripeAccount = stripeAccountId,
            idempotencyKey = idempotencyKey
        )
    )
}

/**
 * Create a Card token from a coroutine.
 *
 * See [Create a card token](https://stripe.com/docs/api/tokens/create_card).
 * `POST /v1/tokens`
 *
 * @param cardParams the [CardParams] used to create this payment token
 * @param idempotencyKey optional, see [Idempotent Requests](https://stripe.com/docs/api/idempotent_requests)
 * @param stripeAccountId Optional, the Connect account to associate with this request.
 * By default, will use the Connect account that was used to instantiate the `Stripe` object, if specified.
 *
 * @return a [Token] object
 *
 * @throws AuthenticationException failure to properly authenticate yourself (check your key)
 * @throws InvalidRequestException your request has invalid parameters
 * @throws APIConnectionException failure to connect to Stripe's API
 * @throws APIException any other type of problem (for instance, a temporary issue with Stripe's servers)
 * @throws CardException the card cannot be charged for some reason
 */
@Throws(
    AuthenticationException::class,
    InvalidRequestException::class,
    APIConnectionException::class,
    APIException::class,
    CardException::class
)
suspend fun Stripe.createCardToken(
    cardParams: CardParams,
    idempotencyKey: String? = null,
    stripeAccountId: String? = this.stripeAccountId
): Token = runApiRequest {
    stripeRepository.createToken(
        cardParams,
        ApiRequest.Options(
            apiKey = publishableKey,
            stripeAccount = stripeAccountId,
            idempotencyKey = idempotencyKey
        )
    )
}

/**
 * Create a CVC update token from a coroutine.
 *
 * `POST /v1/tokens`
 *
 * @param cvc the CVC used to create this token
 * @param idempotencyKey optional, see [Idempotent Requests](https://stripe.com/docs/api/idempotent_requests)
 * @param stripeAccountId Optional, the Connect account to associate with this request.
 * By default, will use the Connect account that was used to instantiate the `Stripe` object, if specified.
 *
 * @return a [Token] object
 *
 * @throws AuthenticationException failure to properly authenticate yourself (check your key)
 * @throws InvalidRequestException your request has invalid parameters
 * @throws APIConnectionException failure to connect to Stripe's API
 * @throws APIException any other type of problem (for instance, a temporary issue with Stripe's servers)
 */
@Throws(
    AuthenticationException::class,
    InvalidRequestException::class,
    APIConnectionException::class,
    APIException::class,
)
suspend fun Stripe.createCvcUpdateToken(
    @Size(min = 3, max = 4) cvc: String,
    idempotencyKey: String? = null,
    stripeAccountId: String? = this.stripeAccountId
): Token = runApiRequest {
    stripeRepository.createToken(
        CvcTokenParams(cvc),
        ApiRequest.Options(
            apiKey = publishableKey,
            stripeAccount = stripeAccountId,
            idempotencyKey = idempotencyKey
        )
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
 * @return a [Token] object
 *
 * @throws AuthenticationException failure to properly authenticate yourself (check your key)
 * @throws InvalidRequestException your request has invalid parameters
 * @throws APIConnectionException failure to connect to Stripe's API
 * @throws APIException any other type of problem (for instance, a temporary issue with Stripe's servers)
 */
@Throws(
    AuthenticationException::class,
    InvalidRequestException::class,
    APIConnectionException::class,
    APIException::class,
)
suspend fun Stripe.createPersonToken(
    params: PersonTokenParams,
    idempotencyKey: String? = null,
    stripeAccountId: String? = this.stripeAccountId
): Token = runApiRequest {
    stripeRepository.createToken(
        params,
        ApiRequest.Options(
            apiKey = publishableKey,
            stripeAccount = stripeAccountId,
            idempotencyKey = idempotencyKey
        )
    )
}

/**
 * Create a [StripeFile] from a coroutine.
 *
 *  * See [Create a file](https://stripe.com/docs/api/files/create).
 * `POST /v1/files`
 *
 * @param fileParams the [StripeFileParams] used to create the [StripeFile]
 * @param idempotencyKey optional, see [Idempotent Requests](https://stripe.com/docs/api/idempotent_requests)
 * @param stripeAccountId Optional, the Connect account to associate with this request.
 * By default, will use the Connect account that was used to instantiate the `Stripe` object, if specified.
 *
 * @return a [StripeFile] object
 *
 * @throws AuthenticationException failure to properly authenticate yourself (check your key)
 * @throws InvalidRequestException your request has invalid parameters
 * @throws APIConnectionException failure to connect to Stripe's API
 * @throws APIException any other type of problem (for instance, a temporary issue with Stripe's servers)
 * @throws CardException the card cannot be charged for some reason
 */
@Throws(
    AuthenticationException::class,
    InvalidRequestException::class,
    APIConnectionException::class,
    APIException::class,
    CardException::class
)
suspend fun Stripe.createFile(
    fileParams: StripeFileParams,
    idempotencyKey: String? = null,
    stripeAccountId: String? = this.stripeAccountId,
): StripeFile = runApiRequest {
    stripeRepository.createFile(
        fileParams,
        ApiRequest.Options(
            apiKey = publishableKey,
            stripeAccount = stripeAccountId,
            idempotencyKey = idempotencyKey
        )
    )
}

/**
 * Retrieve a [PaymentIntent] from a coroutine.
 *
 * See [Retrieve a PaymentIntent](https://stripe.com/docs/api/payment_intents/retrieve).
 * `GET /v1/payment_intents/:id`
 *
 * @param clientSecret the client_secret with which to retrieve the [PaymentIntent]
 * @param stripeAccountId Optional, the Connect account to associate with this request.
 * By default, will use the Connect account that was used to instantiate the `Stripe` object, if specified.
 *
 * @return a [PaymentIntent] object
 *
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
suspend fun Stripe.retrievePaymentIntent(
    clientSecret: String,
    stripeAccountId: String? = this.stripeAccountId
): PaymentIntent = runApiRequest {
    stripeRepository.retrievePaymentIntent(
        clientSecret,
        ApiRequest.Options(
            apiKey = publishableKey,
            stripeAccount = stripeAccountId
        )
    )
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
 *
 * @return a [SetupIntent] object
 *
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
suspend fun Stripe.retrieveSetupIntent(
    clientSecret: String,
    stripeAccountId: String? = this.stripeAccountId
): SetupIntent = runApiRequest {
    stripeRepository.retrieveSetupIntent(
        clientSecret,
        ApiRequest.Options(
            apiKey = publishableKey,
            stripeAccount = stripeAccountId
        )
    )
}

/**
 * Retrieve a [Source] from a coroutine.
 *
 * See [Retrieve a source](https://stripe.com/docs/api/sources/retrieve).
 * `GET /v1/sources/:id`
 *
 * @param sourceId the [Source.id] field of the desired Source object
 * @param clientSecret the [Source.clientSecret] field of the desired Source object
 * @param stripeAccountId Optional, the Connect account to associate with this request.
 * By default, will use the Connect account that was used to instantiate the `Stripe` object, if specified.
 *
 * @return a [Source] object
 *
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
suspend fun Stripe.retrieveSource(
    @Size(min = 1) sourceId: String,
    @Size(min = 1) clientSecret: String,
    stripeAccountId: String? = this.stripeAccountId
): Source = runApiRequest {
    stripeRepository.retrieveSource(
        sourceId,
        clientSecret,
        ApiRequest.Options(
            apiKey = publishableKey,
            stripeAccount = stripeAccountId
        )
    )
}

/**
 * Suspend function to confirm a [SetupIntent] object.
 *
 * See [Confirm a SetupIntent](https://stripe.com/docs/api/setup_intents/confirm).
 * `POST /v1/setup_intents/:id/confirm`
 *
 * @param confirmSetupIntentParams a set of params with which to confirm the Setup Intent
 * @param idempotencyKey optional, see [Idempotent Requests](https://stripe.com/docs/api/idempotent_requests)
 *
 * @return a [SetupIntent] object
 *
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
suspend fun Stripe.confirmSetupIntent(
    confirmSetupIntentParams: ConfirmSetupIntentParams,
    idempotencyKey: String? = null
): SetupIntent = runApiRequest {
    stripeRepository.confirmSetupIntent(
        confirmSetupIntentParams,
        ApiRequest.Options(
            apiKey = publishableKey,
            stripeAccount = stripeAccountId,
            idempotencyKey = idempotencyKey
        )
    )
}

/**
 * Suspend function to confirm a [PaymentIntent] for WeChat Pay. Extract params from [WeChatPayNextAction] to pass to WeChat Pay SDK.
 * @see <a href="https://pay.weixin.qq.com/index.php/public/wechatpay">WeChat Pay Documentation</a>
 *
 * WeChat Pay API is still in beta, create a [Stripe] instance with [StripeApiBeta.WeChatPayV1] to enable this API.
 *
 * @param confirmPaymentIntentParams [ConfirmPaymentIntentParams] used to confirm the
 * [PaymentIntent]
 * @param stripeAccountId Optional, the Connect account to associate with this request.
 * By default, will use the Connect account that was used to instantiate the [Stripe] object, if specified.
 *
 * @throws AuthenticationException failure to properly authenticate yourself (check your key)
 * @throws InvalidRequestException your request has invalid parameters
 * @throws APIConnectionException failure to connect to Stripe's API
 * @throws APIException any other type of problem (for instance, a temporary issue with Stripe's servers)
 */
suspend fun Stripe.confirmWeChatPayPayment(
    confirmPaymentIntentParams: ConfirmPaymentIntentParams,
    stripeAccountId: String? = this.stripeAccountId,
): WeChatPayNextAction {
    return runCatching {
        paymentController.confirmWeChatPay(
            confirmPaymentIntentParams,
            ApiRequest.Options(
                apiKey = publishableKey,
                stripeAccount = stripeAccountId
            )
        )
    }.getOrElse {
        throw StripeException.create(it)
    }
}

/**
 * Suspend function to confirm a [PaymentIntent] object.
 *
 * See [Confirm a PaymentIntent](https://stripe.com/docs/api/payment_intents/confirm).
 * `POST /v1/payment_intents/:id/confirm`
 *
 * @param confirmPaymentIntentParams a set of params with which to confirm the PaymentIntent
 * @param idempotencyKey optional, see [Idempotent Requests](https://stripe.com/docs/api/idempotent_requests)
 *
 * @return a [PaymentIntent] object
 *
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
suspend fun Stripe.confirmPaymentIntent(
    confirmPaymentIntentParams: ConfirmPaymentIntentParams,
    idempotencyKey: String? = null
): PaymentIntent = runApiRequest {
    stripeRepository.confirmPaymentIntent(
        confirmPaymentIntentParams,
        ApiRequest.Options(
            apiKey = publishableKey,
            stripeAccount = stripeAccountId,
            idempotencyKey = idempotencyKey
        )
    )
}

/**
 * Consume the empty result from Stripe's internal Json Parser, throw [InvalidRequestException] for public API.
 *
 * @return the result if the API result and JSON parsing are successful; otherwise, throw an exception.
 */
private inline fun <reified ApiObject : StripeModel> runApiRequest(
    block: () -> ApiObject?
): ApiObject =
    runCatching {
        requireNotNull(block()) {
            "Failed to parse ${ApiObject::class.java.simpleName}."
        }
    }.getOrElse { throw StripeException.create(it) }

/**
 * Get the [PaymentIntentResult] from [Intent] returned via
 * Activity#onActivityResult(int, int, Intent)}} for PaymentIntent automatic confirmation
 * (see [confirmPayment]) or manual confirmation (see [handleNextActionForPayment]})
 *
 * @param requestCode [Int] code passed from Activity#onActivityResult
 * @param data [Intent] intent from Activity#onActivityResult
 *
 * @throws AuthenticationException failure to properly authenticate yourself (check your key)
 * @throws InvalidRequestException your request has invalid parameters
 * @throws APIConnectionException failure to connect to Stripe's API
 * @throws APIException any other type of problem (for instance, a temporary issue with Stripe's servers)
 */
@Throws(
    AuthenticationException::class,
    InvalidRequestException::class,
    APIConnectionException::class,
    APIException::class,
)
suspend fun Stripe.getPaymentIntentResult(
    requestCode: Int,
    data: Intent,
): PaymentIntentResult {
    return runApiRequest(
        isPaymentResult(
            requestCode,
            data
        )
    ) { paymentController.getPaymentIntentResult(data) }
}

/**
 * Get the [SetupIntentResult] from [Intent] returned via
 * Activity#onActivityResult(int, int, Intent)}} for SetupIntentResult confirmation.
 * (see [confirmSetupIntent])
 *
 * @param requestCode [Int] code passed from Activity#onActivityResult
 * @param data [Intent] intent from Activity#onActivityResult
 *
 * @throws AuthenticationException failure to properly authenticate yourself (check your key)
 * @throws InvalidRequestException your request has invalid parameters
 * @throws APIConnectionException failure to connect to Stripe's API
 * @throws APIException any other type of problem (for instance, a temporary issue with Stripe's servers)
 */
@Throws(
    AuthenticationException::class,
    InvalidRequestException::class,
    APIConnectionException::class,
    APIException::class,
    IllegalArgumentException::class
)
suspend fun Stripe.getSetupIntentResult(
    requestCode: Int,
    data: Intent,
): SetupIntentResult {
    return runApiRequest(
        isSetupResult(
            requestCode,
            data
        )
    ) { paymentController.getSetupIntentResult(data) }
}

/**
 * Get the [Source] from [Intent] returned via
 * Activity#onActivityResult(int, int, Intent)}} for [Source] authentication.
 * (see [authenticateSource])
 *
 * @param requestCode [Int] code passed from Activity#onActivityResult
 * @param data [Intent] intent from Activity#onActivityResult
 *
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
suspend fun Stripe.getAuthenticateSourceResult(
    requestCode: Int,
    data: Intent,
): Source {
    return runApiRequest(
        isAuthenticateSourceResult(
            requestCode,
            data
        )
    ) { paymentController.getAuthenticateSourceResult(data) }
}

/**
 * Consume the [IllegalArgumentException] caused by empty result from Stripe's internal Json Parser,
 * throw [InvalidRequestException] for public API.
 *
 * @return the result if the API result and JSON parsing are successful; otherwise, throw an exception.
 */
internal inline fun <reified ApiObject : StripeModel> runApiRequest(
    isValidParam: Boolean,
    block: () -> ApiObject
): ApiObject =
    runCatching {
        require(isValidParam) {
            "Incorrect requestCode and data for ${ApiObject::class.java.simpleName}."
        }
        block()
    }.getOrElse { throw StripeException.create(it) }
