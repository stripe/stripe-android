package com.stripe.android;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.stripe.android.exception.APIConnectionException;
import com.stripe.android.exception.APIException;
import com.stripe.android.exception.AuthenticationException;
import com.stripe.android.exception.CardException;
import com.stripe.android.exception.InvalidRequestException;
import com.stripe.android.model.ConfirmPaymentIntentParams;
import com.stripe.android.model.ConfirmSetupIntentParams;
import com.stripe.android.model.Customer;
import com.stripe.android.model.PaymentIntent;
import com.stripe.android.model.PaymentMethod;
import com.stripe.android.model.PaymentMethodCreateParams;
import com.stripe.android.model.SetupIntent;
import com.stripe.android.model.ShippingInformation;
import com.stripe.android.model.Source;
import com.stripe.android.model.SourceParams;
import com.stripe.android.model.Stripe3ds2AuthResult;
import com.stripe.android.model.Token;

import org.json.JSONException;

import java.util.List;
import java.util.Map;

/**
 * An interface for data operations on Stripe API objects.
 */
interface StripeRepository {

    @Nullable
    PaymentIntent confirmPaymentIntent(
            @NonNull ConfirmPaymentIntentParams confirmPaymentIntentParams,
            @NonNull ApiRequest.Options options)
            throws AuthenticationException, InvalidRequestException, APIConnectionException,
            APIException;

    @Nullable
    PaymentIntent retrievePaymentIntent(
            @NonNull String clientSecret,
            @NonNull ApiRequest.Options options)
            throws AuthenticationException, InvalidRequestException, APIConnectionException,
            APIException;

    @Nullable
    SetupIntent confirmSetupIntent(
            @NonNull ConfirmSetupIntentParams confirmSetupIntentParams,
            @NonNull ApiRequest.Options options)
            throws AuthenticationException, InvalidRequestException, APIConnectionException,
            APIException;

    @Nullable
    SetupIntent retrieveSetupIntent(
            @NonNull String clientSecret,
            @NonNull ApiRequest.Options options)
            throws AuthenticationException, InvalidRequestException, APIConnectionException,
            APIException;

    @Nullable
    Source createSource(
            @NonNull SourceParams sourceParams,
            @NonNull ApiRequest.Options options)
            throws AuthenticationException, InvalidRequestException, APIConnectionException,
            APIException;

    @Nullable
    Source retrieveSource(
            @NonNull String sourceId,
            @NonNull String clientSecret,
            @NonNull ApiRequest.Options options)
            throws AuthenticationException, InvalidRequestException, APIConnectionException,
            APIException;

    @Nullable
    PaymentMethod createPaymentMethod(
            @NonNull PaymentMethodCreateParams paymentMethodCreateParams,
            @NonNull ApiRequest.Options options)
            throws AuthenticationException, InvalidRequestException, APIConnectionException,
            APIException;

    @Nullable
    Token createToken(
            @NonNull Map<String, ?> tokenParams,
            @NonNull ApiRequest.Options options,
            @NonNull @Token.TokenType String tokenType)
            throws AuthenticationException, InvalidRequestException, APIConnectionException,
            APIException, CardException;

    @Nullable
    Source addCustomerSource(
            @NonNull String customerId,
            @NonNull String publishableKey,
            @NonNull List<String> productUsageTokens,
            @NonNull String sourceId,
            @NonNull @Source.SourceType String sourceType,
            @NonNull ApiRequest.Options requestOptions)
            throws AuthenticationException, InvalidRequestException, APIConnectionException,
            APIException, CardException;

    @Nullable
    Source deleteCustomerSource(
            @NonNull String customerId,
            @NonNull String publishableKey,
            @NonNull List<String> productUsageTokens,
            @NonNull String sourceId,
            @NonNull ApiRequest.Options requestOptions)
            throws AuthenticationException, InvalidRequestException, APIConnectionException,
            APIException, CardException;

    @Nullable
    PaymentMethod attachPaymentMethod(
            @NonNull String customerId,
            @NonNull String publishableKey,
            @NonNull List<String> productUsageTokens,
            @NonNull String paymentMethodId,
            @NonNull ApiRequest.Options requestOptions)
            throws AuthenticationException, InvalidRequestException, APIConnectionException,
            APIException, CardException;

    @Nullable
    PaymentMethod detachPaymentMethod(
            @NonNull String publishableKey,
            @NonNull List<String> productUsageTokens,
            @NonNull String paymentMethodId,
            @NonNull ApiRequest.Options requestOptions)
            throws AuthenticationException, InvalidRequestException, APIConnectionException,
            APIException, CardException;

    @NonNull
    List<PaymentMethod> getPaymentMethods(
            @NonNull String customerId,
            @NonNull String paymentMethodType,
            @NonNull String publishableKey,
            @NonNull List<String> productUsageTokens,
            @NonNull ApiRequest.Options requestOptions)
            throws AuthenticationException, InvalidRequestException, APIConnectionException,
            APIException, CardException;

    @Nullable
    Customer setDefaultCustomerSource(
            @NonNull String customerId,
            @NonNull String publishableKey,
            @NonNull List<String> productUsageTokens,
            @NonNull String sourceId,
            @NonNull @Source.SourceType String sourceType,
            @NonNull ApiRequest.Options requestOptions)
            throws AuthenticationException, InvalidRequestException, APIConnectionException,
            APIException, CardException;

    @Nullable
    Customer setCustomerShippingInfo(
            @NonNull String customerId,
            @NonNull String publishableKey,
            @NonNull List<String> productUsageTokens,
            @NonNull ShippingInformation shippingInformation,
            @NonNull ApiRequest.Options requestOptions)
            throws AuthenticationException, InvalidRequestException, APIConnectionException,
            APIException, CardException;

    @Nullable
    Customer retrieveCustomer(@NonNull String customerId,
                              @NonNull ApiRequest.Options requestOptions)
            throws AuthenticationException, InvalidRequestException, APIConnectionException,
            APIException, CardException;

    @NonNull
    String retrieveIssuingCardPin(
            @NonNull String cardId,
            @NonNull String verificationId,
            @NonNull String userOneTimeCode,
            @NonNull String ephemeralKeySecret)
            throws AuthenticationException, InvalidRequestException, APIConnectionException,
            APIException, CardException, JSONException;

    void updateIssuingCardPin(
            @NonNull String cardId,
            @NonNull String newPin,
            @NonNull String verificationId,
            @NonNull String userOneTimeCode,
            @NonNull String ephemeralKeySecret)
            throws AuthenticationException, InvalidRequestException, APIConnectionException,
            APIException, CardException;

    void start3ds2Auth(@NonNull Stripe3ds2AuthParams authParams,
                       @NonNull String stripeIntentId,
                       @NonNull ApiRequest.Options requestOptions,
                       @NonNull ApiResultCallback<Stripe3ds2AuthResult> callback);

    void complete3ds2Auth(@NonNull String sourceId,
                          @NonNull ApiRequest.Options requestOptions,
                          @NonNull ApiResultCallback<Boolean> callback);
}
