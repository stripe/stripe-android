package com.stripe.android.networking

import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import com.stripe.android.cards.Bin
import com.stripe.android.core.exception.APIConnectionException
import com.stripe.android.core.exception.APIException
import com.stripe.android.core.exception.AuthenticationException
import com.stripe.android.core.exception.InvalidRequestException
import com.stripe.android.core.model.StripeFile
import com.stripe.android.core.model.StripeFileParams
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.exception.CardException
import com.stripe.android.model.BankConnectionsLinkedAccountSession
import com.stripe.android.model.BankStatuses
import com.stripe.android.model.CardMetadata
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.ConfirmSetupIntentParams
import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.model.ConsumerSession
import com.stripe.android.model.ConsumerSessionLookup
import com.stripe.android.model.CreateLinkAccountSessionParams
import com.stripe.android.model.Customer
import com.stripe.android.model.ListPaymentMethodsParams
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.RadarSession
import com.stripe.android.model.SetupIntent
import com.stripe.android.model.ShippingInformation
import com.stripe.android.model.Source
import com.stripe.android.model.SourceParams
import com.stripe.android.model.Stripe3ds2AuthParams
import com.stripe.android.model.Stripe3ds2AuthResult
import com.stripe.android.model.StripeIntent
import com.stripe.android.model.Token
import com.stripe.android.model.TokenParams
import org.json.JSONException
import org.json.JSONObject
import java.util.Locale

/**
 * An interface for data operations on Stripe API objects.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // originally made public for paymentsheet
abstract class StripeRepository {

    internal abstract suspend fun retrieveStripeIntent(
        clientSecret: String,
        options: ApiRequest.Options,
        expandFields: List<String> = emptyList()
    ): StripeIntent

    @Throws(
        AuthenticationException::class,
        InvalidRequestException::class,
        APIConnectionException::class,
        APIException::class
    )
    internal abstract suspend fun confirmPaymentIntent(
        confirmPaymentIntentParams: ConfirmPaymentIntentParams,
        options: ApiRequest.Options,
        expandFields: List<String> = emptyList()
    ): PaymentIntent?

    @Throws(
        AuthenticationException::class,
        InvalidRequestException::class,
        APIConnectionException::class,
        APIException::class
    )
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    abstract suspend fun retrievePaymentIntent(
        clientSecret: String,
        options: ApiRequest.Options,
        expandFields: List<String> = emptyList()
    ): PaymentIntent?

    @Throws(
        AuthenticationException::class,
        InvalidRequestException::class,
        APIConnectionException::class,
        APIException::class
    )
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    abstract suspend fun retrievePaymentIntentWithOrderedPaymentMethods(
        clientSecret: String,
        options: ApiRequest.Options,
        locale: Locale
    ): PaymentIntent?

    @Throws(
        AuthenticationException::class,
        InvalidRequestException::class,
        APIConnectionException::class,
        APIException::class
    )
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    internal abstract suspend fun refreshPaymentIntent(
        clientSecret: String,
        options: ApiRequest.Options
    ): PaymentIntent?

    @Throws(
        AuthenticationException::class,
        InvalidRequestException::class,
        APIConnectionException::class,
        APIException::class
    )
    internal abstract suspend fun cancelPaymentIntentSource(
        paymentIntentId: String,
        sourceId: String,
        options: ApiRequest.Options
    ): PaymentIntent?

    @Throws(
        AuthenticationException::class,
        InvalidRequestException::class,
        APIConnectionException::class,
        APIException::class
    )
    internal abstract suspend fun confirmSetupIntent(
        confirmSetupIntentParams: ConfirmSetupIntentParams,
        options: ApiRequest.Options,
        expandFields: List<String> = emptyList()
    ): SetupIntent?

    @Throws(
        AuthenticationException::class,
        InvalidRequestException::class,
        APIConnectionException::class,
        APIException::class
    )
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    abstract suspend fun retrieveSetupIntent(
        clientSecret: String,
        options: ApiRequest.Options,
        expandFields: List<String> = emptyList()
    ): SetupIntent?

    @Throws(
        AuthenticationException::class,
        InvalidRequestException::class,
        APIConnectionException::class,
        APIException::class
    )
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    abstract suspend fun retrieveSetupIntentWithOrderedPaymentMethods(
        clientSecret: String,
        options: ApiRequest.Options,
        locale: Locale
    ): SetupIntent?

    @Throws(
        AuthenticationException::class,
        InvalidRequestException::class,
        APIConnectionException::class,
        APIException::class
    )
    internal abstract suspend fun cancelSetupIntentSource(
        setupIntentId: String,
        sourceId: String,
        options: ApiRequest.Options
    ): SetupIntent?

    @Throws(
        AuthenticationException::class,
        InvalidRequestException::class,
        APIConnectionException::class,
        APIException::class
    )
    internal abstract suspend fun createSource(
        sourceParams: SourceParams,
        options: ApiRequest.Options
    ): Source?

    @Throws(
        AuthenticationException::class,
        InvalidRequestException::class,
        APIConnectionException::class,
        APIException::class
    )
    internal abstract suspend fun retrieveSource(
        sourceId: String,
        clientSecret: String,
        options: ApiRequest.Options
    ): Source?

    @Throws(
        AuthenticationException::class,
        InvalidRequestException::class,
        APIConnectionException::class,
        APIException::class
    )
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    abstract suspend fun createPaymentMethod(
        paymentMethodCreateParams: PaymentMethodCreateParams,
        options: ApiRequest.Options
    ): PaymentMethod?

    @Throws(
        AuthenticationException::class,
        InvalidRequestException::class,
        APIConnectionException::class,
        APIException::class,
        CardException::class
    )
    internal abstract suspend fun createToken(
        tokenParams: TokenParams,
        options: ApiRequest.Options
    ): Token?

    @Throws(
        AuthenticationException::class,
        InvalidRequestException::class,
        APIConnectionException::class,
        APIException::class,
        CardException::class
    )
    internal abstract suspend fun addCustomerSource(
        customerId: String,
        publishableKey: String,
        productUsageTokens: Set<String>,
        sourceId: String,
        @Source.SourceType sourceType: String,
        requestOptions: ApiRequest.Options
    ): Source?

    @Throws(
        AuthenticationException::class,
        InvalidRequestException::class,
        APIConnectionException::class,
        APIException::class,
        CardException::class
    )
    internal abstract suspend fun deleteCustomerSource(
        customerId: String,
        publishableKey: String,
        productUsageTokens: Set<String>,
        sourceId: String,
        requestOptions: ApiRequest.Options
    ): Source?

    @Throws(
        AuthenticationException::class,
        InvalidRequestException::class,
        APIConnectionException::class,
        APIException::class,
        CardException::class
    )
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @VisibleForTesting
    abstract suspend fun attachPaymentMethod(
        customerId: String,
        publishableKey: String,
        productUsageTokens: Set<String>,
        paymentMethodId: String,
        requestOptions: ApiRequest.Options
    ): PaymentMethod?

    @Throws(
        AuthenticationException::class,
        InvalidRequestException::class,
        APIConnectionException::class,
        APIException::class,
        CardException::class
    )
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    abstract suspend fun detachPaymentMethod(
        publishableKey: String,
        productUsageTokens: Set<String>,
        paymentMethodId: String,
        requestOptions: ApiRequest.Options
    ): PaymentMethod?

    @Throws(
        AuthenticationException::class,
        InvalidRequestException::class,
        APIConnectionException::class,
        APIException::class,
        CardException::class
    )
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    abstract suspend fun getPaymentMethods(
        listPaymentMethodsParams: ListPaymentMethodsParams,
        publishableKey: String,
        productUsageTokens: Set<String>,
        requestOptions: ApiRequest.Options
    ): List<PaymentMethod>

    @Throws(
        AuthenticationException::class,
        InvalidRequestException::class,
        APIConnectionException::class,
        APIException::class,
        CardException::class
    )
    internal abstract suspend fun setDefaultCustomerSource(
        customerId: String,
        publishableKey: String,
        productUsageTokens: Set<String>,
        sourceId: String,
        @Source.SourceType sourceType: String,
        requestOptions: ApiRequest.Options
    ): Customer?

    @Throws(
        AuthenticationException::class,
        InvalidRequestException::class,
        APIConnectionException::class,
        APIException::class,
        CardException::class
    )
    internal abstract suspend fun setCustomerShippingInfo(
        customerId: String,
        publishableKey: String,
        productUsageTokens: Set<String>,
        shippingInformation: ShippingInformation,
        requestOptions: ApiRequest.Options
    ): Customer?

    @Throws(
        AuthenticationException::class,
        InvalidRequestException::class,
        APIConnectionException::class,
        APIException::class,
        CardException::class
    )
    internal abstract suspend fun retrieveCustomer(
        customerId: String,
        productUsageTokens: Set<String>,
        requestOptions: ApiRequest.Options
    ): Customer?

    @Throws(
        AuthenticationException::class,
        InvalidRequestException::class,
        APIConnectionException::class,
        APIException::class,
        CardException::class,
        JSONException::class
    )
    internal abstract suspend fun retrieveIssuingCardPin(
        cardId: String,
        verificationId: String,
        userOneTimeCode: String,
        requestOptions: ApiRequest.Options
    ): String?

    @Throws(
        AuthenticationException::class,
        InvalidRequestException::class,
        APIConnectionException::class,
        APIException::class,
        CardException::class
    )
    internal abstract suspend fun updateIssuingCardPin(
        cardId: String,
        newPin: String,
        verificationId: String,
        userOneTimeCode: String,
        requestOptions: ApiRequest.Options
    )

    internal abstract suspend fun getFpxBankStatus(options: ApiRequest.Options): BankStatuses

    internal abstract suspend fun getCardMetadata(
        bin: Bin,
        options: ApiRequest.Options
    ): CardMetadata?

    internal abstract suspend fun start3ds2Auth(
        authParams: Stripe3ds2AuthParams,
        requestOptions: ApiRequest.Options
    ): Stripe3ds2AuthResult?

    internal abstract suspend fun complete3ds2Auth(
        sourceId: String,
        requestOptions: ApiRequest.Options
    ): Stripe3ds2AuthResult?

    internal abstract suspend fun createFile(
        fileParams: StripeFileParams,
        requestOptions: ApiRequest.Options
    ): StripeFile

    internal abstract suspend fun retrieveObject(
        url: String,
        requestOptions: ApiRequest.Options
    ): JSONObject

    internal abstract suspend fun createRadarSession(
        requestOptions: ApiRequest.Options
    ): RadarSession?

    // Link endpoints

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    abstract suspend fun lookupConsumerSession(
        email: String,
        authSessionCookie: String?,
        requestOptions: ApiRequest.Options
    ): ConsumerSessionLookup?

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    abstract suspend fun consumerSignUp(
        email: String,
        phoneNumber: String,
        country: String,
        authSessionCookie: String?,
        requestOptions: ApiRequest.Options
    ): ConsumerSession?

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    abstract suspend fun startConsumerVerification(
        consumerSessionClientSecret: String,
        locale: Locale,
        authSessionCookie: String?,
        requestOptions: ApiRequest.Options
    ): ConsumerSession?

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    abstract suspend fun confirmConsumerVerification(
        consumerSessionClientSecret: String,
        verificationCode: String,
        authSessionCookie: String?,
        requestOptions: ApiRequest.Options
    ): ConsumerSession?

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    abstract suspend fun logoutConsumer(
        consumerSessionClientSecret: String,
        authSessionCookie: String?,
        requestOptions: ApiRequest.Options
    ): ConsumerSession?

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    abstract suspend fun listPaymentDetails(
        consumerSessionClientSecret: String,
        paymentMethodTypes: Set<String>,
        requestOptions: ApiRequest.Options
    ): ConsumerPaymentDetails?

    internal abstract suspend fun createPaymentIntentLinkAccountSession(
        paymentIntentId: String,
        params: CreateLinkAccountSessionParams,
        requestOptions: ApiRequest.Options
    ): BankConnectionsLinkedAccountSession?

    internal abstract suspend fun createSetupIntentLinkAccountSession(
        setupIntentId: String,
        params: CreateLinkAccountSessionParams,
        requestOptions: ApiRequest.Options
    ): BankConnectionsLinkedAccountSession?

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    abstract suspend fun attachLinkAccountSessionToPaymentIntent(
        clientSecret: String,
        paymentIntentId: String,
        linkAccountSessionId: String,
        requestOptions: ApiRequest.Options
    ): PaymentIntent?

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    abstract suspend fun attachLinkAccountSessionToSetupIntent(
        clientSecret: String,
        setupIntentId: String,
        linkAccountSessionId: String,
        requestOptions: ApiRequest.Options
    ): SetupIntent?
}
