package com.stripe.android;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.stripe.android.exception.APIException;
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

import java.util.Collections;
import java.util.List;
import java.util.Map;

abstract class AbsFakeStripeRepository implements StripeRepository {

    @Nullable
    @Override
    public PaymentIntent confirmPaymentIntent(
            @NonNull ConfirmPaymentIntentParams confirmPaymentIntentParams,
            @NonNull ApiRequest.Options options) {
        return null;
    }

    @Nullable
    @Override
    public PaymentIntent retrievePaymentIntent(
            @NonNull String clientSecret,
            @NonNull ApiRequest.Options options) {
        return null;
    }

    @Nullable
    @Override
    public SetupIntent confirmSetupIntent(
            @NonNull ConfirmSetupIntentParams confirmSetupIntentParams,
            @NonNull ApiRequest.Options options) {
        return null;
    }

    @Nullable
    @Override
    public SetupIntent retrieveSetupIntent(
            @NonNull String clientSecret, @NonNull ApiRequest.Options options) {
        return null;
    }

    @Nullable
    @Override
    public Source createSource(
            @NonNull SourceParams sourceParams, @NonNull ApiRequest.Options options) {
        return null;
    }

    @Nullable
    @Override
    public Source retrieveSource(
            @NonNull String sourceId, @NonNull String clientSecret,
            @NonNull ApiRequest.Options options) {
        return null;
    }

    @Nullable
    @Override
    public PaymentMethod createPaymentMethod(
            @NonNull PaymentMethodCreateParams paymentMethodCreateParams,
            @NonNull ApiRequest.Options options) {
        return null;
    }

    @Nullable
    @Override
    public Token createToken(
            @NonNull Map<String, ?> tokenParams, @NonNull ApiRequest.Options options,
            @NonNull String tokenType) {
        return null;
    }

    @Nullable
    @Override
    public Source addCustomerSource(
            @NonNull String customerId, @NonNull String publishableKey,
            @NonNull List<String> productUsageTokens, @NonNull String sourceId,
            @NonNull String sourceType, @NonNull ApiRequest.Options requestOptions) throws APIException {
        return null;
    }

    @Nullable
    @Override
    public Source deleteCustomerSource(
            @NonNull String customerId, @NonNull String publishableKey,
            @NonNull List<String> productUsageTokens, @NonNull String sourceId,
            @NonNull ApiRequest.Options requestOptions) throws APIException {
        return null;
    }

    @Nullable
    @Override
    public PaymentMethod attachPaymentMethod(
            @NonNull String customerId, @NonNull String publishableKey,
            @NonNull List<String> productUsageTokens, @NonNull String paymentMethodId,
            @NonNull ApiRequest.Options requestOptions) throws APIException {
        return null;
    }

    @Nullable
    @Override
    public PaymentMethod detachPaymentMethod(
            @NonNull String publishableKey, @NonNull List<String> productUsageTokens,
            @NonNull String paymentMethodId, @NonNull ApiRequest.Options requestOptions) throws APIException {
        return null;
    }

    @NonNull
    @Override
    public List<PaymentMethod> getPaymentMethods(
            @NonNull String customerId, @NonNull String paymentMethodType,
            @NonNull String publishableKey, @NonNull List<String> productUsageTokens,
            @NonNull ApiRequest.Options requestOptions) throws APIException {
        return Collections.emptyList();
    }

    @Nullable
    @Override
    public Customer setDefaultCustomerSource(
            @NonNull String customerId, @NonNull String publishableKey,
            @NonNull List<String> productUsageTokens, @NonNull String sourceId,
            @NonNull String sourceType, @NonNull ApiRequest.Options requestOptions) throws APIException {
        return null;
    }

    @Nullable
    @Override
    public Customer setCustomerShippingInfo(
            @NonNull String customerId,
            @NonNull String publishableKey,
            @NonNull List<String> productUsageTokens,
            @NonNull ShippingInformation shippingInformation,
            @NonNull ApiRequest.Options requestOptions) {
        return null;
    }

    @Nullable
    @Override
    public Customer retrieveCustomer(
            @NonNull String customerId, @NonNull ApiRequest.Options requestOptions) {
        return null;
    }

    @NonNull
    @Override
    public String retrieveIssuingCardPin(
            @NonNull String cardId, @NonNull String verificationId,
            @NonNull String userOneTimeCode, @NonNull String ephemeralKeySecret) {
        return "";
    }

    @Override
    public void updateIssuingCardPin(
            @NonNull String cardId, @NonNull String newPin, @NonNull String verificationId,
            @NonNull String userOneTimeCode, @NonNull String ephemeralKeySecret) {
    }

    @Override
    public void start3ds2Auth(
            @NonNull Stripe3ds2AuthParams authParams, @NonNull String stripeIntentId,
            @NonNull ApiRequest.Options requestOptions,
            @NonNull ApiResultCallback<Stripe3ds2AuthResult> callback) {
    }

    @Override
    public void complete3ds2Auth(
            @NonNull String sourceId, @NonNull ApiRequest.Options requestOptions,
            @NonNull ApiResultCallback<Boolean> callback) {
    }
}
