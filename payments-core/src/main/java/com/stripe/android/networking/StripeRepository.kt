package com.stripe.android.networking

import androidx.annotation.RestrictTo
import com.stripe.android.cards.Bin
import com.stripe.android.core.model.StripeFile
import com.stripe.android.core.model.StripeFileParams
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.core.networking.StripeResponse
import com.stripe.android.model.BankStatuses
import com.stripe.android.model.CardMetadata
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.ConfirmSetupIntentParams
import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.model.ConsumerPaymentDetailsCreateParams
import com.stripe.android.model.ConsumerSession
import com.stripe.android.model.ConsumerSignUpConsentAction
import com.stripe.android.model.CreateFinancialConnectionsSessionForDeferredPaymentParams
import com.stripe.android.model.CreateFinancialConnectionsSessionParams
import com.stripe.android.model.Customer
import com.stripe.android.model.ElementsSession
import com.stripe.android.model.ElementsSessionParams
import com.stripe.android.model.FinancialConnectionsSession
import com.stripe.android.model.ListPaymentMethodsParams
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodMessage
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
import java.util.Locale

/**
 * An interface for data operations on Stripe API objects.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // originally made public for paymentsheet
interface StripeRepository {

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    suspend fun retrieveStripeIntent(
        clientSecret: String,
        options: ApiRequest.Options,
        expandFields: List<String> = emptyList()
    ): Result<StripeIntent>

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    suspend fun confirmPaymentIntent(
        confirmPaymentIntentParams: ConfirmPaymentIntentParams,
        options: ApiRequest.Options,
        expandFields: List<String> = emptyList()
    ): Result<PaymentIntent>

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    suspend fun retrievePaymentIntent(
        clientSecret: String,
        options: ApiRequest.Options,
        expandFields: List<String> = emptyList()
    ): Result<PaymentIntent>

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    suspend fun refreshPaymentIntent(
        clientSecret: String,
        options: ApiRequest.Options
    ): Result<PaymentIntent>

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    suspend fun cancelPaymentIntentSource(
        paymentIntentId: String,
        sourceId: String,
        options: ApiRequest.Options
    ): Result<PaymentIntent>

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    suspend fun confirmSetupIntent(
        confirmSetupIntentParams: ConfirmSetupIntentParams,
        options: ApiRequest.Options,
        expandFields: List<String> = emptyList()
    ): Result<SetupIntent>

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    suspend fun retrieveSetupIntent(
        clientSecret: String,
        options: ApiRequest.Options,
        expandFields: List<String> = emptyList()
    ): Result<SetupIntent>

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    suspend fun cancelSetupIntentSource(
        setupIntentId: String,
        sourceId: String,
        options: ApiRequest.Options
    ): Result<SetupIntent>

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    suspend fun createSource(
        sourceParams: SourceParams,
        options: ApiRequest.Options
    ): Result<Source>

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    suspend fun retrieveSource(
        sourceId: String,
        clientSecret: String,
        options: ApiRequest.Options
    ): Result<Source>

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    suspend fun createPaymentMethod(
        paymentMethodCreateParams: PaymentMethodCreateParams,
        options: ApiRequest.Options
    ): Result<PaymentMethod>

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    suspend fun createToken(
        tokenParams: TokenParams,
        options: ApiRequest.Options
    ): Result<Token>

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    suspend fun addCustomerSource(
        customerId: String,
        publishableKey: String,
        productUsageTokens: Set<String>,
        sourceId: String,
        @Source.SourceType sourceType: String,
        requestOptions: ApiRequest.Options
    ): Result<Source>

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    suspend fun deleteCustomerSource(
        customerId: String,
        publishableKey: String,
        productUsageTokens: Set<String>,
        sourceId: String,
        requestOptions: ApiRequest.Options
    ): Result<Source>

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    suspend fun attachPaymentMethod(
        customerId: String,
        publishableKey: String,
        productUsageTokens: Set<String>,
        paymentMethodId: String,
        requestOptions: ApiRequest.Options
    ): Result<PaymentMethod>

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    suspend fun detachPaymentMethod(
        publishableKey: String,
        productUsageTokens: Set<String>,
        paymentMethodId: String,
        requestOptions: ApiRequest.Options
    ): Result<PaymentMethod>

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    suspend fun getPaymentMethods(
        listPaymentMethodsParams: ListPaymentMethodsParams,
        publishableKey: String,
        productUsageTokens: Set<String>,
        requestOptions: ApiRequest.Options
    ): Result<List<PaymentMethod>>

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    suspend fun setDefaultCustomerSource(
        customerId: String,
        publishableKey: String,
        productUsageTokens: Set<String>,
        sourceId: String,
        @Source.SourceType sourceType: String,
        requestOptions: ApiRequest.Options
    ): Result<Customer>

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    suspend fun setCustomerShippingInfo(
        customerId: String,
        publishableKey: String,
        productUsageTokens: Set<String>,
        shippingInformation: ShippingInformation,
        requestOptions: ApiRequest.Options
    ): Result<Customer>

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    suspend fun retrieveCustomer(
        customerId: String,
        productUsageTokens: Set<String>,
        requestOptions: ApiRequest.Options
    ): Result<Customer>

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    suspend fun retrieveIssuingCardPin(
        cardId: String,
        verificationId: String,
        userOneTimeCode: String,
        requestOptions: ApiRequest.Options
    ): Result<String>

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    suspend fun updateIssuingCardPin(
        cardId: String,
        newPin: String,
        verificationId: String,
        userOneTimeCode: String,
        requestOptions: ApiRequest.Options
    ): Throwable?

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    suspend fun getFpxBankStatus(options: ApiRequest.Options): Result<BankStatuses>

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    suspend fun getCardMetadata(
        bin: Bin,
        options: ApiRequest.Options
    ): Result<CardMetadata>

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    suspend fun start3ds2Auth(
        authParams: Stripe3ds2AuthParams,
        requestOptions: ApiRequest.Options
    ): Result<Stripe3ds2AuthResult>

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    suspend fun complete3ds2Auth(
        sourceId: String,
        requestOptions: ApiRequest.Options
    ): Result<Stripe3ds2AuthResult>

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    suspend fun createFile(
        fileParams: StripeFileParams,
        requestOptions: ApiRequest.Options
    ): Result<StripeFile>

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    suspend fun retrieveObject(
        url: String,
        requestOptions: ApiRequest.Options
    ): Result<StripeResponse<String>>

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    suspend fun createRadarSession(
        requestOptions: ApiRequest.Options
    ): Result<RadarSession>

    // Link endpoints
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Suppress("LongParameterList")
    suspend fun consumerSignUp(
        email: String,
        phoneNumber: String,
        country: String,
        name: String?,
        locale: Locale?,
        authSessionCookie: String?,
        consentAction: ConsumerSignUpConsentAction,
        requestOptions: ApiRequest.Options
    ): Result<ConsumerSession>

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    suspend fun createPaymentDetails(
        consumerSessionClientSecret: String,
        paymentDetailsCreateParams: ConsumerPaymentDetailsCreateParams,
        requestOptions: ApiRequest.Options
    ): Result<ConsumerPaymentDetails>

    // ACHv2 endpoints

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    suspend fun createFinancialConnectionsSessionForDeferredPayments(
        params: CreateFinancialConnectionsSessionForDeferredPaymentParams,
        requestOptions: ApiRequest.Options
    ): Result<FinancialConnectionsSession>

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    suspend fun createPaymentIntentFinancialConnectionsSession(
        paymentIntentId: String,
        params: CreateFinancialConnectionsSessionParams,
        requestOptions: ApiRequest.Options
    ): Result<FinancialConnectionsSession>

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    suspend fun createSetupIntentFinancialConnectionsSession(
        setupIntentId: String,
        params: CreateFinancialConnectionsSessionParams,
        requestOptions: ApiRequest.Options
    ): Result<FinancialConnectionsSession>

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    suspend fun attachFinancialConnectionsSessionToPaymentIntent(
        clientSecret: String,
        paymentIntentId: String,
        financialConnectionsSessionId: String,
        requestOptions: ApiRequest.Options,
        expandFields: List<String>
    ): Result<PaymentIntent>

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    suspend fun attachFinancialConnectionsSessionToSetupIntent(
        clientSecret: String,
        setupIntentId: String,
        financialConnectionsSessionId: String,
        requestOptions: ApiRequest.Options,
        expandFields: List<String>
    ): Result<SetupIntent>

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    suspend fun verifyPaymentIntentWithMicrodeposits(
        clientSecret: String,
        firstAmount: Int,
        secondAmount: Int,
        requestOptions: ApiRequest.Options
    ): Result<PaymentIntent>

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    suspend fun verifyPaymentIntentWithMicrodeposits(
        clientSecret: String,
        descriptorCode: String,
        requestOptions: ApiRequest.Options
    ): Result<PaymentIntent>

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    suspend fun verifySetupIntentWithMicrodeposits(
        clientSecret: String,
        firstAmount: Int,
        secondAmount: Int,
        requestOptions: ApiRequest.Options
    ): Result<SetupIntent>

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    suspend fun verifySetupIntentWithMicrodeposits(
        clientSecret: String,
        descriptorCode: String,
        requestOptions: ApiRequest.Options
    ): Result<SetupIntent>

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    suspend fun retrievePaymentMethodMessage(
        paymentMethods: List<String>,
        amount: Int,
        currency: String,
        country: String,
        locale: String,
        logoColor: String,
        requestOptions: ApiRequest.Options
    ): Result<PaymentMethodMessage>

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    suspend fun retrieveElementsSession(
        params: ElementsSessionParams,
        options: ApiRequest.Options,
    ): Result<ElementsSession>

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    suspend fun retrieveCardMetadata(
        cardNumber: String,
        requestOptions: ApiRequest.Options
    ): Result<CardMetadata>

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun buildPaymentUserAgent(attribution: Set<String> = emptySet()): String
}
