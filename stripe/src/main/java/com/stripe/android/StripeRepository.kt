package com.stripe.android

import com.stripe.android.exception.APIConnectionException
import com.stripe.android.exception.APIException
import com.stripe.android.exception.AuthenticationException
import com.stripe.android.exception.CardException
import com.stripe.android.exception.InvalidRequestException
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.ConfirmSetupIntentParams
import com.stripe.android.model.Customer
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.SetupIntent
import com.stripe.android.model.ShippingInformation
import com.stripe.android.model.Source
import com.stripe.android.model.SourceParams
import com.stripe.android.model.Stripe3ds2AuthResult
import com.stripe.android.model.Token
import org.json.JSONException

/**
 * An interface for data operations on Stripe API objects.
 */
internal interface StripeRepository {

    @Throws(AuthenticationException::class, InvalidRequestException::class,
        APIConnectionException::class, APIException::class)
    fun confirmPaymentIntent(
        confirmPaymentIntentParams: ConfirmPaymentIntentParams,
        options: ApiRequest.Options
    ): PaymentIntent?

    @Throws(AuthenticationException::class, InvalidRequestException::class,
        APIConnectionException::class, APIException::class)
    fun retrievePaymentIntent(
        clientSecret: String,
        options: ApiRequest.Options
    ): PaymentIntent?

    @Throws(AuthenticationException::class, InvalidRequestException::class,
        APIConnectionException::class, APIException::class)
    fun confirmSetupIntent(
        confirmSetupIntentParams: ConfirmSetupIntentParams,
        options: ApiRequest.Options
    ): SetupIntent?

    @Throws(AuthenticationException::class, InvalidRequestException::class,
        APIConnectionException::class, APIException::class)
    fun retrieveSetupIntent(
        clientSecret: String,
        options: ApiRequest.Options
    ): SetupIntent?

    @Throws(AuthenticationException::class, InvalidRequestException::class,
        APIConnectionException::class, APIException::class)
    fun createSource(
        sourceParams: SourceParams,
        options: ApiRequest.Options
    ): Source?

    @Throws(AuthenticationException::class, InvalidRequestException::class,
        APIConnectionException::class, APIException::class)
    fun retrieveSource(
        sourceId: String,
        clientSecret: String,
        options: ApiRequest.Options
    ): Source?

    @Throws(AuthenticationException::class, InvalidRequestException::class,
        APIConnectionException::class, APIException::class)
    fun createPaymentMethod(
        paymentMethodCreateParams: PaymentMethodCreateParams,
        options: ApiRequest.Options
    ): PaymentMethod?

    @Throws(AuthenticationException::class, InvalidRequestException::class,
        APIConnectionException::class, APIException::class, CardException::class)
    fun createToken(
        tokenParams: Map<String, *>,
        options: ApiRequest.Options,
        @Token.TokenType tokenType: String
    ): Token?

    @Throws(AuthenticationException::class, InvalidRequestException::class,
        APIConnectionException::class, APIException::class, CardException::class)
    fun addCustomerSource(
        customerId: String,
        publishableKey: String,
        productUsageTokens: List<String>,
        sourceId: String,
        @Source.SourceType sourceType: String,
        requestOptions: ApiRequest.Options
    ): Source?

    @Throws(AuthenticationException::class, InvalidRequestException::class,
        APIConnectionException::class, APIException::class, CardException::class)
    fun deleteCustomerSource(
        customerId: String,
        publishableKey: String,
        productUsageTokens: List<String>,
        sourceId: String,
        requestOptions: ApiRequest.Options
    ): Source?

    @Throws(AuthenticationException::class, InvalidRequestException::class,
        APIConnectionException::class, APIException::class, CardException::class)
    fun attachPaymentMethod(
        customerId: String,
        publishableKey: String,
        productUsageTokens: List<String>,
        paymentMethodId: String,
        requestOptions: ApiRequest.Options
    ): PaymentMethod?

    @Throws(AuthenticationException::class, InvalidRequestException::class,
        APIConnectionException::class, APIException::class, CardException::class)
    fun detachPaymentMethod(
        publishableKey: String,
        productUsageTokens: List<String>,
        paymentMethodId: String,
        requestOptions: ApiRequest.Options
    ): PaymentMethod?

    @Throws(AuthenticationException::class, InvalidRequestException::class,
        APIConnectionException::class, APIException::class, CardException::class)
    fun getPaymentMethods(
        customerId: String,
        paymentMethodType: String,
        publishableKey: String,
        productUsageTokens: List<String>,
        requestOptions: ApiRequest.Options
    ): List<PaymentMethod>

    @Throws(AuthenticationException::class, InvalidRequestException::class,
        APIConnectionException::class, APIException::class, CardException::class)
    fun setDefaultCustomerSource(
        customerId: String,
        publishableKey: String,
        productUsageTokens: List<String>,
        sourceId: String,
        @Source.SourceType sourceType: String,
        requestOptions: ApiRequest.Options
    ): Customer?

    @Throws(AuthenticationException::class, InvalidRequestException::class,
        APIConnectionException::class, APIException::class, CardException::class)
    fun setCustomerShippingInfo(
        customerId: String,
        publishableKey: String,
        productUsageTokens: List<String>,
        shippingInformation: ShippingInformation,
        requestOptions: ApiRequest.Options
    ): Customer?

    @Throws(AuthenticationException::class, InvalidRequestException::class,
        APIConnectionException::class, APIException::class, CardException::class)
    fun retrieveCustomer(customerId: String, requestOptions: ApiRequest.Options): Customer?

    @Throws(AuthenticationException::class, InvalidRequestException::class,
        APIConnectionException::class, APIException::class, CardException::class,
        JSONException::class)
    fun retrieveIssuingCardPin(
        cardId: String,
        verificationId: String,
        userOneTimeCode: String,
        ephemeralKeySecret: String
    ): String

    @Throws(AuthenticationException::class, InvalidRequestException::class,
        APIConnectionException::class, APIException::class, CardException::class)
    fun updateIssuingCardPin(
        cardId: String,
        newPin: String,
        verificationId: String,
        userOneTimeCode: String,
        ephemeralKeySecret: String
    )

    fun start3ds2Auth(
        authParams: Stripe3ds2AuthParams,
        stripeIntentId: String,
        requestOptions: ApiRequest.Options,
        callback: ApiResultCallback<Stripe3ds2AuthResult>
    )

    fun complete3ds2Auth(
        sourceId: String,
        requestOptions: ApiRequest.Options,
        callback: ApiResultCallback<Boolean>
    )
}
