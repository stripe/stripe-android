package com.stripe.android;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.Size;
import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.Fragment;

import com.stripe.android.exception.APIConnectionException;
import com.stripe.android.exception.APIException;
import com.stripe.android.exception.AuthenticationException;
import com.stripe.android.exception.CardException;
import com.stripe.android.exception.InvalidRequestException;
import com.stripe.android.exception.StripeException;
import com.stripe.android.model.AccountParams;
import com.stripe.android.model.BankAccount;
import com.stripe.android.model.Card;
import com.stripe.android.model.ConfirmPaymentIntentParams;
import com.stripe.android.model.ConfirmSetupIntentParams;
import com.stripe.android.model.CvcTokenParams;
import com.stripe.android.model.PaymentIntent;
import com.stripe.android.model.PaymentMethod;
import com.stripe.android.model.PaymentMethodCreateParams;
import com.stripe.android.model.PiiTokenParams;
import com.stripe.android.model.SetupIntent;
import com.stripe.android.model.Source;
import com.stripe.android.model.SourceParams;
import com.stripe.android.model.Token;
import com.stripe.android.view.AuthActivityStarter;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executor;

/**
 * Entry-point to the Stripe SDK that handles
 * - {@link Token} creation from charges, {@link Card}, and accounts
 * - {@link PaymentMethod} creation
 * - {@link PaymentIntent} retrieval and confirmation
 */
@SuppressWarnings("WeakerAccess")
public class Stripe {
    @NonNull public static final String API_VERSION = ApiVersion.get().getCode();

    @NonNull public static final String VERSION =
            String.format(Locale.ROOT, "AndroidBindings/%s", BuildConfig.VERSION_NAME);

    @Nullable private static AppInfo sAppInfo;

    @NonNull private final StripeRepository mStripeRepository;
    @NonNull private final StripeNetworkUtils mStripeNetworkUtils;
    @NonNull private final PaymentController mPaymentController;
    @NonNull private final TokenCreator mTokenCreator;
    @NonNull private final String mDefaultPublishableKey;
    @Nullable private final String mStripeAccountId;

    /**
     * Constructor with publishable key.
     *
     * @param context Activity or application context
     * @param publishableKey the client's publishable key
     */
    public Stripe(@NonNull Context context, @NonNull String publishableKey) {
        this(
                context.getApplicationContext(),
                new StripeApiRepository(context.getApplicationContext(), sAppInfo),
                new StripeNetworkUtils(context.getApplicationContext()),
                ApiKeyValidator.get().requireValid(publishableKey),
                null
        );
    }

    /**
     * Constructor with publishable key and Stripe Connect account id.
     *
     * @param context Activity or application context
     * @param publishableKey the client's publishable key
     * @param stripeAccountId the Stripe Connect account id to attach to
     *                        <a href="https://stripe.com/docs/connect/authentication#authentication-via-the-stripe-account-header">Stripe API requests</a>
     */
    public Stripe(@NonNull Context context,
                  @NonNull String publishableKey,
                  @NonNull String stripeAccountId) {
        this(
                context.getApplicationContext(),
                new StripeApiRepository(context.getApplicationContext(), sAppInfo),
                new StripeNetworkUtils(context.getApplicationContext()),
                ApiKeyValidator.get().requireValid(publishableKey),
                stripeAccountId
        );
    }

    private Stripe(@NonNull Context context,
                   @NonNull final StripeRepository stripeRepository,
                   @NonNull StripeNetworkUtils stripeNetworkUtils,
                   @NonNull String publishableKey,
                   @Nullable String stripeAccountId) {
        this(
                stripeRepository,
                stripeNetworkUtils,
                PaymentController.create(context.getApplicationContext(), stripeRepository),
                publishableKey,
                stripeAccountId
        );
    }

    Stripe(@NonNull final StripeRepository stripeRepository,
           @NonNull StripeNetworkUtils stripeNetworkUtils,
           @NonNull PaymentController paymentController,
           @NonNull String publishableKey,
           @Nullable String stripeAccountId) {
        this(
                stripeRepository,
                stripeNetworkUtils,
                paymentController,
                publishableKey,
                stripeAccountId,
                new TokenCreator() {
                    @Override
                    public void create(
                            @NonNull final Map<String, Object> tokenParams,
                            @NonNull final ApiRequest.Options options,
                            @NonNull @Token.TokenType final String tokenType,
                            @Nullable final Executor executor,
                            @NonNull final ApiResultCallback<Token> callback) {
                        executeTask(executor,
                                new CreateTokenTask(stripeRepository, tokenParams, options,
                                        tokenType, callback));
                    }
                }
        );
    }

    @VisibleForTesting
    Stripe(@NonNull StripeRepository stripeRepository,
           @NonNull StripeNetworkUtils stripeNetworkUtils,
           @NonNull PaymentController paymentController,
           @NonNull String publishableKey,
           @Nullable String stripeAccountId,
           @NonNull TokenCreator tokenCreator) {
        mStripeRepository = stripeRepository;
        mStripeNetworkUtils = stripeNetworkUtils;
        mPaymentController = paymentController;
        mTokenCreator = tokenCreator;
        mStripeAccountId = stripeAccountId;
        mDefaultPublishableKey = new ApiKeyValidator().requireValid(publishableKey);
    }

    /**
     * Setter for identifying your plug-in or library.
     *
     * See <a href="https://stripe.com/docs/building-plugins#setappinfo">
     *     Building Stripe Plug-ins and Libraries - Setting the API version</a>.
     */
    public static void setAppInfo(@Nullable AppInfo appInfo) {
        sAppInfo = appInfo;
    }

    @Nullable
    static AppInfo getAppInfo() {
        return sAppInfo;
    }

    /**
     * Confirm and, if necessary, authenticate a {@link SetupIntent}.
     *
     * @param activity the <code>Activity</code> that is launching the payment authentication flow
     */
    public void confirmSetupIntent(@NonNull Activity activity,
                                   @NonNull ConfirmSetupIntentParams confirmSetupIntentParams) {
        mPaymentController.startConfirmAndAuth(
                AuthActivityStarter.Host.create(activity),
                confirmSetupIntentParams,
                ApiRequest.Options.create(mDefaultPublishableKey, mStripeAccountId)
        );
    }

    /**
     * Confirm and, if necessary, authenticate a {@link SetupIntent}.
     *
     * @param fragment the <code>Fragment</code> that is launching the payment authentication flow
     */
    public void confirmSetupIntent(@NonNull Fragment fragment,
                                   @NonNull ConfirmSetupIntentParams confirmSetupIntentParams) {
        mPaymentController.startConfirmAndAuth(
                AuthActivityStarter.Host.create(fragment),
                confirmSetupIntentParams,
                ApiRequest.Options.create(mDefaultPublishableKey, mStripeAccountId)
        );
    }

    /**
     * Confirm and, if necessary, authenticate a {@link PaymentIntent}. Used for <a href=
     * "https://stripe.com/docs/payments/payment-intents/quickstart#automatic-confirmation-flow">
     * automatic confirmation</a> flow.
     *
     * @param activity the <code>Activity</code> that is launching the payment authentication flow
     * @param confirmPaymentIntentParams {@link ConfirmPaymentIntentParams} used to confirm the
     *                                   {@link PaymentIntent}
     */
    public void confirmPayment(@NonNull Activity activity,
                               @NonNull ConfirmPaymentIntentParams confirmPaymentIntentParams) {
        mPaymentController.startConfirmAndAuth(
                AuthActivityStarter.Host.create(activity),
                confirmPaymentIntentParams,
                ApiRequest.Options.create(mDefaultPublishableKey, mStripeAccountId)
        );
    }

    /**
     * Confirm and, if necessary, authenticate a {@link PaymentIntent}. Used for <a href=
     * "https://stripe.com/docs/payments/payment-intents/quickstart#automatic-confirmation-flow">
     * automatic confirmation</a> flow.
     *
     * @param fragment the <code>Fragment</code> that is launching the payment authentication flow
     * @param confirmPaymentIntentParams {@link ConfirmPaymentIntentParams} used to confirm the
     *                                   {@link PaymentIntent}
     */
    public void confirmPayment(@NonNull Fragment fragment,
                               @NonNull ConfirmPaymentIntentParams confirmPaymentIntentParams) {
        mPaymentController.startConfirmAndAuth(
                AuthActivityStarter.Host.create(fragment),
                confirmPaymentIntentParams,
                ApiRequest.Options.create(mDefaultPublishableKey, mStripeAccountId)
        );
    }

    /**
     * Authenticate a {@link PaymentIntent}. Used for <a href=
     * "https://stripe.com/docs/payments/payment-intents/quickstart#manual-confirmation-flow">
     * manual confirmation</a> flow.
     *
     * @param activity the <code>Activity</code> that is launching the payment authentication flow
     * @param clientSecret the <a href="https://stripe.com/docs/api/payment_intents/object#payment_intent_object-client_secret">client_secret</a>
     *                     property of a confirmed {@link PaymentIntent} object
     */
    public void authenticatePayment(@NonNull Activity activity,
                                    @NonNull String clientSecret) {
        mPaymentController.startAuth(
                AuthActivityStarter.Host.create(activity),
                clientSecret,
                ApiRequest.Options.create(mDefaultPublishableKey, mStripeAccountId)
        );
    }

    /**
     * Authenticate a {@link PaymentIntent}. Used for <a href=
     * "https://stripe.com/docs/payments/payment-intents/quickstart#manual-confirmation-flow">
     * manual confirmation</a> flow.
     *
     * @param fragment the <code>Fragment</code> that is launching the payment authentication flow
     * @param clientSecret the <a href="https://stripe.com/docs/api/payment_intents/object#payment_intent_object-client_secret">client_secret</a>
     *                     property of a confirmed {@link PaymentIntent} object
     */
    public void authenticatePayment(@NonNull Fragment fragment,
                                    @NonNull String clientSecret) {
        mPaymentController.startAuth(
                AuthActivityStarter.Host.create(fragment),
                clientSecret,
                ApiRequest.Options.create(mDefaultPublishableKey, mStripeAccountId)
        );
    }

    /**
     * Authenticate a {@link SetupIntent}. Used for manual confirmation flow.
     *
     * @param activity     the <code>Activity</code> that is launching the payment authentication
     *                     flow
     * @param clientSecret the <a href="https://stripe.com/docs/api/setup_intents/object#setup_intent_object-client_secret">client_secret</a>
     *                     property of a confirmed {@link SetupIntent} object
     */
    public void authenticateSetup(@NonNull Activity activity,
                                  @NonNull String clientSecret) {
        mPaymentController.startAuth(
                AuthActivityStarter.Host.create(activity),
                clientSecret,
                ApiRequest.Options.create(mDefaultPublishableKey, mStripeAccountId)
        );
    }

    /**
     * Authenticate a {@link SetupIntent}. Used for manual confirmation flow.
     *
     * @param fragment     the <code>Fragment</code> launching the payment authentication flow
     * @param clientSecret the <a href="https://stripe.com/docs/api/setup_intents/object#setup_intent_object-client_secret">client_secret</a>
     *                     property of a confirmed {@link SetupIntent} object
     */
    public void authenticateSetup(@NonNull Fragment fragment,
                                  @NonNull String clientSecret) {
        mPaymentController.startAuth(
                AuthActivityStarter.Host.create(fragment),
                clientSecret,
                ApiRequest.Options.create(mDefaultPublishableKey, mStripeAccountId)
        );
    }

    /**
     * Should be called via <code>Activity#onActivityResult(int, int, Intent)}}</code> to handle the
     * result of a PaymentIntent automatic confirmation
     * (see {@link #confirmPayment(Activity, ConfirmPaymentIntentParams)}) or manual
     * confirmation (see {@link #authenticatePayment(Activity, String)}})
     */
    public boolean onPaymentResult(int requestCode, @Nullable Intent data,
                                   @NonNull ApiResultCallback<PaymentIntentResult> callback) {
        if (data != null &&
                mPaymentController.shouldHandlePaymentResult(requestCode, data)) {
            mPaymentController.handlePaymentResult(
                    data,
                    ApiRequest.Options.create(mDefaultPublishableKey, mStripeAccountId),
                    callback);
            return true;
        }

        return false;
    }

    /**
     * Should be called via <code>Activity#onActivityResult(int, int, Intent)}}</code> to handle the
     * result of a SetupIntent confirmation
     * (see {@link #confirmSetupIntent(Activity, ConfirmSetupIntentParams)})
     */
    public boolean onSetupResult(int requestCode, @Nullable Intent data,
                                 @NonNull ApiResultCallback<SetupIntentResult> callback) {
        if (data != null &&
                mPaymentController.shouldHandleSetupResult(requestCode, data)) {
            mPaymentController.handleSetupResult(
                    data,
                    ApiRequest.Options.create(mDefaultPublishableKey, mStripeAccountId),
                    callback
            );
            return true;
        }

        return false;
    }

    /**
     * Create a {@link BankAccount} token asynchronously.
     *
     * <p>See <a href="https://stripe.com/docs/api/tokens/create_bank_account">Create a bank account token</a>.</p>
     *
     * @param bankAccount the {@link BankAccount} used to create this token
     * @param callback a {@link ApiResultCallback} to receive either the token or an error
     */
    public void createBankAccountToken(
            @NonNull final BankAccount bankAccount,
            @NonNull final ApiResultCallback<Token> callback) {
        final Map<String, Object> params = bankAccount.toParamMap();
        params.putAll(mStripeNetworkUtils.createUidParams());
        createTokenFromParams(
                params,
                mDefaultPublishableKey,
                Token.TokenType.BANK_ACCOUNT,
                null,
                callback
        );
    }

    /**
     * Create a PII token asynchronously.
     *
     * <p>See <a href="https://stripe.com/docs/api/tokens/create_pii">Create a PII account token</a>.</p>
     *
     * @param personalId the personal id used to create this token
     * @param callback a {@link ApiResultCallback} to receive either the token or an error
     */
    public void createPiiToken(
            @NonNull final String personalId,
            @NonNull final ApiResultCallback<Token> callback) {
        createTokenFromParams(
                new PiiTokenParams(personalId).toParamMap(),
                mDefaultPublishableKey,
                Token.TokenType.PII,
                null,
                callback);
    }

    /**
     * Blocking method to create a {@link Token} for a {@link BankAccount}. Do not call this on
     * the UI thread or your app will crash.
     *
     * <p>See <a href="https://stripe.com/docs/api/tokens/create_bank_account">Create a bank account token</a>.</p>
     *
     * @param bankAccount the {@link Card} to use for this token
     * @return a {@link Token} that can be used for this {@link BankAccount}
     * @throws AuthenticationException failure to properly authenticate yourself (check your key)
     * @throws InvalidRequestException your request has invalid parameters
     * @throws APIConnectionException failure to connect to Stripe's API
     * @throws CardException should not be thrown with this type of token, but is theoretically
     *         possible given the underlying methods called
     * @throws APIException any other type of problem (for instance, a temporary issue with
     *         Stripe's servers
     */
    @Nullable
    public Token createBankAccountTokenSynchronous(@NonNull final BankAccount bankAccount)
            throws AuthenticationException,
            InvalidRequestException,
            APIConnectionException,
            CardException,
            APIException {
        final Map<String, Object> params = bankAccount.toParamMap();
        params.putAll(mStripeNetworkUtils.createUidParams());
        return mStripeRepository.createToken(
                params,
                ApiRequest.Options.create(mDefaultPublishableKey, mStripeAccountId),
                Token.TokenType.BANK_ACCOUNT
        );
    }

    /**
     * Create a CVC update token asynchronously.
     *
     * @param cvc the CVC used to create this token
     * @param callback a {@link ApiResultCallback} to receive either the token or an error
     */
    public void createCvcUpdateToken(
            @NonNull @Size(min = 3, max = 4) final String cvc,
            @NonNull final ApiResultCallback<Token> callback) {
        createTokenFromParams(
                new CvcTokenParams(cvc).toParamMap(),
                mDefaultPublishableKey,
                Token.TokenType.CVC_UPDATE,
                null,
                callback
        );
    }

    /**
     * Create a {@link Source} asynchronously.
     *
     * <p>See <a href="https://stripe.com/docs/api/sources/create">Create a source</a>.</p>
     *
     * @param sourceParams the {@link SourceParams} to be used
     * @param callback a {@link ApiResultCallback} to receive a result or an error message
     */
    public void createSource(@NonNull SourceParams sourceParams,
                             @NonNull ApiResultCallback<Source> callback) {
        new CreateSourceTask(
                mStripeRepository, sourceParams, mDefaultPublishableKey, mStripeAccountId, callback
        ).execute();
    }

    /**
     * Create a {@link PaymentMethod} asynchronously.
     *
     * <p>See <a href="https://stripe.com/docs/api/payment_methods/create">Create a PaymentMethod</a>.</p>
     *
     * @param paymentMethodCreateParams the {@link PaymentMethodCreateParams} to be used
     * @param callback a {@link ApiResultCallback} to receive a result or an error
     *         message
     */
    public void createPaymentMethod(@NonNull PaymentMethodCreateParams paymentMethodCreateParams,
                                    @NonNull ApiResultCallback<PaymentMethod> callback) {
        new CreatePaymentMethodTask(
                mStripeRepository, paymentMethodCreateParams, mDefaultPublishableKey,
                mStripeAccountId, callback
        ).execute();
    }

    /**
     * Create a Card token asynchronously.
     *
     * <p>See <a href="https://stripe.com/docs/api/tokens/create_card">Create a card token</a>.</p>
     *
     * @param card the {@link Card} used to create this payment token
     * @param callback a {@link ApiResultCallback} to receive either the token or an error
     */
    public void createToken(@NonNull final Card card,
                            @NonNull final ApiResultCallback<Token> callback) {
        createTokenFromParams(
                mStripeNetworkUtils.createCardTokenParams(card),
                mDefaultPublishableKey,
                Token.TokenType.CARD,
                null,
                callback
        );
    }

    /**
     * Call to create a {@link Token} on a specific {@link Executor}.
     *
     * <p>See <a href="https://stripe.com/docs/api/tokens/create_card">Create a card token</a>.</p>
     *
     * @param card the {@link Card} to use for this token creation
     * @param executor An {@link Executor} on which to run this operation. If you don't wish to
     *         specify an executor, use one of the other createTokenFromParams methods.
     * @param callback a {@link ApiResultCallback} to receive the result of this operation
     */
    public void createToken(
            @NonNull final Card card,
            @NonNull final Executor executor,
            @NonNull final ApiResultCallback<Token> callback) {
        createTokenFromParams(
                mStripeNetworkUtils.createCardTokenParams(card),
                mDefaultPublishableKey,
                Token.TokenType.CARD,
                executor,
                callback
        );
    }

    /**
     * Blocking method to create a {@link Source} object.
     * Do not call this on the UI thread or your app will crash.
     *
     * <p>See <a href="https://stripe.com/docs/api/sources/create">Create a source</a>.</p>
     *
     * @param params a set of {@link SourceParams} with which to create the source
     * @return a {@link Source}, or {@code null} if a problem occurred
     * @throws AuthenticationException failure to properly authenticate yourself (check your key)
     * @throws InvalidRequestException your request has invalid parameters
     * @throws APIConnectionException failure to connect to Stripe's API
     * @throws APIException any other type of problem (for instance, a temporary issue with
     *         Stripe's servers
     */
    @Nullable
    public Source createSourceSynchronous(@NonNull SourceParams params)
            throws AuthenticationException,
            InvalidRequestException,
            APIConnectionException,
            APIException {
        return mStripeRepository.createSource(params,
                ApiRequest.Options.create(mDefaultPublishableKey, mStripeAccountId));
    }

    /**
     * Blocking method to retrieve a {@link PaymentIntent} object.
     * Do not call this on the UI thread or your app will crash.
     *
     * <p>See <a href="https://stripe.com/docs/api/payment_intents/retrieve">Retrieve a PaymentIntent</a>.</p>
     *
     * @param clientSecret the client_secret with which to retrieve the PaymentIntent
     * @return a {@link PaymentIntent} or {@code null} if a problem occurred
     */
    @Nullable
    public PaymentIntent retrievePaymentIntentSynchronous(@NonNull String clientSecret)
            throws APIException, AuthenticationException, InvalidRequestException,
            APIConnectionException {
        return mStripeRepository.retrievePaymentIntent(
                clientSecret,
                ApiRequest.Options.create(mDefaultPublishableKey, mStripeAccountId)
        );
    }

    /**
     * Blocking method to confirm a {@link PaymentIntent} object.
     * Do not call this on the UI thread or your app will crash.
     *
     * <p>See <a href="https://stripe.com/docs/api/payment_intents/confirm">Confirm a PaymentIntent</a>.</p>
     *
     * @param confirmPaymentIntentParams a set of params with which to confirm the PaymentIntent
     * @return a {@link PaymentIntent} or {@code null} if a problem occurred
     *
     * @deprecated use {@link #confirmPayment(Activity, ConfirmPaymentIntentParams)}
     */
    @Nullable
    public PaymentIntent confirmPaymentIntentSynchronous(
            @NonNull ConfirmPaymentIntentParams confirmPaymentIntentParams)
            throws AuthenticationException, InvalidRequestException, APIConnectionException,
            APIException {
        return mStripeRepository.confirmPaymentIntent(
                confirmPaymentIntentParams,
                ApiRequest.Options.create(mDefaultPublishableKey, mStripeAccountId)
        );
    }

    /**
     * Blocking method to retrieve a {@link SetupIntent} object.
     * Do not call this on the UI thread or your app will crash.
     *
     * <p>See <a href="https://stripe.com/docs/api/setup_intents/retrieve">Retrieve a SetupIntent</a>.</p>
     *
     * @param clientSecret client_secret of the SetupIntent to retrieve
     * @return a {@link SetupIntent} or {@code null} if a problem occurred
     */
    @Nullable
    public SetupIntent retrieveSetupIntentSynchronous(@NonNull String clientSecret)
            throws APIException, AuthenticationException, InvalidRequestException,
            APIConnectionException {
        return mStripeRepository.retrieveSetupIntent(
                clientSecret,
                ApiRequest.Options.create(mDefaultPublishableKey, mStripeAccountId)
        );
    }

    /**
     * Blocking method to confirm a {@link SetupIntent} object.
     * Do not call this on the UI thread or your app will crash.
     *
     * <p>See <a href="https://stripe.com/docs/api/setup_intents/confirm">Confirm a SetupIntent</a>.</p>
     *
     * @param confirmSetupIntentParams a set of params with which to confirm the Setup Intent
     * @return a {@link SetupIntent} or {@code null} if a problem occurred
     *
     * @deprecated use {@link #confirmSetupIntent(Activity, ConfirmSetupIntentParams)}
     */
    @Nullable
    public SetupIntent confirmSetupIntentSynchronous(
            @NonNull ConfirmSetupIntentParams confirmSetupIntentParams)
            throws AuthenticationException,
            InvalidRequestException,
            APIConnectionException,
            APIException {
        return mStripeRepository.confirmSetupIntent(
                confirmSetupIntentParams,
                ApiRequest.Options.create(mDefaultPublishableKey, mStripeAccountId)
        );
    }

    /**
     * Blocking method to create a {@link PaymentMethod} object.
     * Do not call this on the UI thread or your app will crash.
     *
     * <p>See <a href="https://stripe.com/docs/api/payment_methods/create">Create a PaymentMethod</a>.</p>
     *
     * @param paymentMethodCreateParams params with which to create the PaymentMethod
     *
     * @return a {@link PaymentMethod} or {@code null} if a problem occurred
     */
    @Nullable
    public PaymentMethod createPaymentMethodSynchronous(
            @NonNull PaymentMethodCreateParams paymentMethodCreateParams)
            throws APIException, AuthenticationException, InvalidRequestException,
            APIConnectionException {
        return mStripeRepository.createPaymentMethod(paymentMethodCreateParams,
                ApiRequest.Options.create(mDefaultPublishableKey, mStripeAccountId));
    }

    /**
     * Blocking method to create a {@link Token}. Do not call this on the UI thread or your app
     * will crash.
     *
     * <p>See <a href="https://stripe.com/docs/api/tokens/create_card">Create a card token</a>.</p>
     *
     * @param card the {@link Card} to use for this token
     * @return a {@link Token} that can be used for this card
     * @throws AuthenticationException failure to properly authenticate yourself (check your key)
     * @throws InvalidRequestException your request has invalid parameters
     * @throws APIConnectionException failure to connect to Stripe's API
     * @throws CardException the card cannot be charged for some reason
     * @throws APIException any other type of problem (for instance, a temporary issue with
     *         Stripe's servers
     */
    @Nullable
    public Token createTokenSynchronous(@NonNull final Card card)
            throws AuthenticationException,
            InvalidRequestException,
            APIConnectionException,
            CardException,
            APIException {
        return mStripeRepository.createToken(
                mStripeNetworkUtils.createCardTokenParams(card),
                ApiRequest.Options.create(mDefaultPublishableKey, mStripeAccountId),
                Token.TokenType.CARD
        );
    }

    /**
     * Blocking method to create a {@link Token} for PII. Do not call this on the UI thread
     * or your app will crash.
     *
     * <p>See <a href="https://stripe.com/docs/api/tokens/create_pii">Create a PII account token</a>.</p>
     *
     * @param personalId the personal ID to use for this token
     * @return a {@link Token} that can be used for this card
     * @throws AuthenticationException failure to properly authenticate yourself (check your key)
     * @throws InvalidRequestException your request has invalid parameters
     * @throws APIConnectionException failure to connect to Stripe's API
     * @throws APIException any other type of problem (for instance, a temporary issue with
     *         Stripe's servers)
     */
    @Nullable
    public Token createPiiTokenSynchronous(@NonNull String personalId)
            throws AuthenticationException,
            InvalidRequestException,
            APIConnectionException,
            CardException,
            APIException {
        return mStripeRepository.createToken(
                new PiiTokenParams(personalId).toParamMap(),
                ApiRequest.Options.create(mDefaultPublishableKey, mStripeAccountId),
                Token.TokenType.PII
        );
    }

    /**
     * Blocking method to create a {@link Token} for CVC updating. Do not call this on the UI thread
     * or your app will crash.
     *
     * @param cvc the CVC to use for this token
     * @return a {@link Token} that can be used for this card
     * @throws AuthenticationException failure to properly authenticate yourself (check your key)
     * @throws InvalidRequestException your request has invalid parameters
     * @throws APIConnectionException failure to connect to Stripe's API
     * @throws APIException any other type of problem (for instance, a temporary issue with
     *         Stripe's servers)
     */
    @Nullable
    public Token createCvcUpdateTokenSynchronous(@NonNull String cvc)
            throws AuthenticationException,
            InvalidRequestException,
            APIConnectionException,
            CardException,
            APIException {
        return mStripeRepository.createToken(
                new CvcTokenParams(cvc).toParamMap(),
                ApiRequest.Options.create(mDefaultPublishableKey, mStripeAccountId),
                Token.TokenType.CVC_UPDATE
        );
    }

    /**
     * Blocking method to create a {@link Token} for a Connect Account. Do not call this on the UI
     * thread or your app will crash.
     *
     * <p>See <a href="https://stripe.com/docs/api/tokens/create_account">Create an account token</a>.</p>
     *
     * @param accountParams params to use for this token.
     * @return a {@link Token} that can be used for this account.
     * @throws AuthenticationException failure to properly authenticate yourself (check your key)
     * @throws InvalidRequestException your request has invalid parameters
     * @throws APIConnectionException failure to connect to Stripe's API
     * @throws APIException any other type of problem (for instance, a temporary issue with
     *         Stripe's servers)
     */
    @Nullable
    public Token createAccountTokenSynchronous(@NonNull final AccountParams accountParams)
            throws AuthenticationException,
            InvalidRequestException,
            APIConnectionException,
            APIException {
        try {
            return mStripeRepository.createToken(
                    accountParams.toParamMap(),
                    ApiRequest.Options.create(mDefaultPublishableKey, mStripeAccountId),
                    Token.TokenType.ACCOUNT
            );
        } catch (CardException exception) {
            // Should never occur. CardException is only for card related requests.
        }
        return null;
    }

    /**
     * Retrieve an existing {@link Source} from the Stripe API. Note that this is a
     * synchronous method, and cannot be called on the main thread. Doing so will cause your app
     * to crash.
     *
     * <p>See <a href="https://stripe.com/docs/api/sources/retrieve">Retrieve a source</a>.</p>
     *
     * @param sourceId the {@link Source#getId()} field of the desired Source object
     * @param clientSecret the {@link Source#getClientSecret()} field of the desired Source object
     * @return a {@link Source} if one could be found based on the input params, or {@code null} if
     *         no such Source could be found.
     * @throws AuthenticationException failure to properly authenticate yourself (check your key)
     * @throws InvalidRequestException your request has invalid parameters
     * @throws APIConnectionException failure to connect to Stripe's API
     * @throws APIException any other type of problem (for instance, a temporary issue with
     *         Stripe's servers)
     */
    @Nullable
    public Source retrieveSourceSynchronous(
            @NonNull @Size(min = 1) String sourceId,
            @NonNull @Size(min = 1) String clientSecret)
            throws AuthenticationException,
            InvalidRequestException,
            APIConnectionException,
            APIException {
        return mStripeRepository.retrieveSource(sourceId, clientSecret,
                ApiRequest.Options.create(mDefaultPublishableKey, mStripeAccountId));
    }

    private void createTokenFromParams(
            @NonNull final Map<String, Object> tokenParams,
            @NonNull @Size(min = 1) final String publishableKey,
            @NonNull @Token.TokenType final String tokenType,
            @Nullable final Executor executor,
            @NonNull final ApiResultCallback<Token> callback) {
        Objects.requireNonNull(callback,
                    "Required Parameter: 'callback' is required to use the created " +
                            "token and handle errors");
        mTokenCreator.create(
                tokenParams,
                ApiRequest.Options.create(publishableKey, mStripeAccountId),
                tokenType,
                executor, callback);
    }

    private static void executeTask(@Nullable Executor executor,
                                    @NonNull AsyncTask<Void, Void, ?> task) {
        if (executor != null) {
            task.executeOnExecutor(executor);
        } else {
            task.execute();
        }
    }

    @VisibleForTesting
    interface TokenCreator {
        void create(@NonNull Map<String, Object> params,
                    @NonNull ApiRequest.Options options,
                    @NonNull @Token.TokenType String tokenType,
                    @Nullable Executor executor,
                    @NonNull ApiResultCallback<Token> callback);
    }

    private static class CreateSourceTask extends ApiOperation<Source> {
        @NonNull private final StripeRepository mStripeRepository;
        @NonNull private final SourceParams mSourceParams;
        @NonNull private final ApiRequest.Options mOptions;

        CreateSourceTask(@NonNull StripeRepository stripeRepository,
                         @NonNull SourceParams sourceParams,
                         @NonNull String publishableKey,
                         @Nullable String stripeAccount,
                         @NonNull ApiResultCallback<Source> callback) {
            super(callback);
            mStripeRepository = stripeRepository;
            mSourceParams = sourceParams;
            mOptions = ApiRequest.Options.create(publishableKey, stripeAccount);
        }

        @Nullable
        @Override
        Source getResult() throws StripeException {
                return mStripeRepository.createSource(mSourceParams, mOptions);
        }
    }

    private static class CreatePaymentMethodTask extends ApiOperation<PaymentMethod> {
        @NonNull private final StripeRepository mStripeRepository;
        @NonNull private final PaymentMethodCreateParams mPaymentMethodCreateParams;
        @NonNull private final ApiRequest.Options mOptions;

        CreatePaymentMethodTask(@NonNull StripeRepository stripeRepository,
                                @NonNull PaymentMethodCreateParams paymentMethodCreateParams,
                                @NonNull String publishableKey,
                                @Nullable String stripeAccount,
                                @NonNull ApiResultCallback<PaymentMethod> callback) {
            super(callback);
            mStripeRepository = stripeRepository;
            mPaymentMethodCreateParams = paymentMethodCreateParams;
            mOptions = ApiRequest.Options.create(publishableKey, stripeAccount);
        }

        @Nullable
        @Override
        PaymentMethod getResult() throws StripeException {
            return mStripeRepository.createPaymentMethod(mPaymentMethodCreateParams, mOptions);
        }
    }

    private static class CreateTokenTask extends ApiOperation<Token> {
        @NonNull private final StripeRepository mStripeRepository;
        @NonNull private final Map<String, Object> mTokenParams;
        @NonNull private final ApiRequest.Options mOptions;
        @NonNull @Token.TokenType private final String mTokenType;

        CreateTokenTask(
                @NonNull final StripeRepository stripeRepository,
                @NonNull final Map<String, Object> tokenParams,
                @NonNull final ApiRequest.Options options,
                @NonNull @Token.TokenType final String tokenType,
                @NonNull final ApiResultCallback<Token> callback) {
            super(callback);
            mStripeRepository = stripeRepository;
            mTokenParams = tokenParams;
            mTokenType = tokenType;
            mOptions = options;
        }

        @Nullable
        @Override
        Token getResult() throws StripeException {
            return mStripeRepository.createToken(mTokenParams, mOptions, mTokenType);
        }
    }
}
