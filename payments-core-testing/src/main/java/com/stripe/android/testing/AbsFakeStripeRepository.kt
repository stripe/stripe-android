package com.stripe.android.testing

import com.stripe.android.cards.Bin
import com.stripe.android.core.exception.APIException
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
import com.stripe.android.networking.StripeRepository
import java.util.Locale

abstract class AbsFakeStripeRepository : StripeRepository {

    override suspend fun retrieveStripeIntent(
        clientSecret: String,
        options: ApiRequest.Options,
        expandFields: List<String>
    ): StripeIntent {
        TODO("Not yet implemented")
    }

    override suspend fun confirmPaymentIntent(
        confirmPaymentIntentParams: ConfirmPaymentIntentParams,
        options: ApiRequest.Options,
        expandFields: List<String>
    ): PaymentIntent? {
        TODO("Not yet implemented")
    }

    override suspend fun retrievePaymentIntent(
        clientSecret: String,
        options: ApiRequest.Options,
        expandFields: List<String>
    ): PaymentIntent? {
        TODO("Not yet implemented")
    }

    override suspend fun refreshPaymentIntent(
        clientSecret: String,
        options: ApiRequest.Options
    ): PaymentIntent? {
        TODO("Not yet implemented")
    }

    override suspend fun cancelPaymentIntentSource(
        paymentIntentId: String,
        sourceId: String,
        options: ApiRequest.Options
    ): PaymentIntent? {
        TODO("Not yet implemented")
    }

    override suspend fun confirmSetupIntent(
        confirmSetupIntentParams: ConfirmSetupIntentParams,
        options: ApiRequest.Options,
        expandFields: List<String>
    ): SetupIntent? {
        TODO("Not yet implemented")
    }

    override suspend fun retrieveSetupIntent(
        clientSecret: String,
        options: ApiRequest.Options,
        expandFields: List<String>
    ): SetupIntent? {
        TODO("Not yet implemented")
    }

    override suspend fun cancelSetupIntentSource(
        setupIntentId: String,
        sourceId: String,
        options: ApiRequest.Options
    ): SetupIntent? {
        TODO("Not yet implemented")
    }

    override suspend fun createSource(
        sourceParams: SourceParams,
        options: ApiRequest.Options
    ): Source? {
        TODO("Not yet implemented")
    }

    override suspend fun retrieveSource(
        sourceId: String,
        clientSecret: String,
        options: ApiRequest.Options
    ): Result<Source> {
        return Result.failure(NotImplementedError())
    }

    override suspend fun createPaymentMethod(
        paymentMethodCreateParams: PaymentMethodCreateParams,
        options: ApiRequest.Options
    ): PaymentMethod? {
        TODO("Not yet implemented")
    }

    override suspend fun createToken(
        tokenParams: TokenParams,
        options: ApiRequest.Options
    ): Token? {
        TODO("Not yet implemented")
    }

    override suspend fun addCustomerSource(
        customerId: String,
        publishableKey: String,
        productUsageTokens: Set<String>,
        sourceId: String,
        sourceType: String,
        requestOptions: ApiRequest.Options
    ): Result<Source> {
        return Result.failure(NotImplementedError())
    }

    override suspend fun deleteCustomerSource(
        customerId: String,
        publishableKey: String,
        productUsageTokens: Set<String>,
        sourceId: String,
        requestOptions: ApiRequest.Options
    ): Result<Source> {
        return Result.failure(NotImplementedError())
    }

    @Throws(APIException::class)
    override suspend fun attachPaymentMethod(
        customerId: String,
        publishableKey: String,
        productUsageTokens: Set<String>,
        paymentMethodId: String,
        requestOptions: ApiRequest.Options
    ): Result<PaymentMethod> {
        return Result.failure(NotImplementedError())
    }

    @Throws(APIException::class)
    override suspend fun detachPaymentMethod(
        publishableKey: String,
        productUsageTokens: Set<String>,
        paymentMethodId: String,
        requestOptions: ApiRequest.Options
    ): Result<PaymentMethod> {
        return Result.failure(NotImplementedError())
    }

    @Throws(APIException::class)
    override suspend fun getPaymentMethods(
        listPaymentMethodsParams: ListPaymentMethodsParams,
        publishableKey: String,
        productUsageTokens: Set<String>,
        requestOptions: ApiRequest.Options
    ): Result<List<PaymentMethod>> {
        return Result.failure(NotImplementedError())
    }

    override suspend fun setDefaultCustomerSource(
        customerId: String,
        publishableKey: String,
        productUsageTokens: Set<String>,
        sourceId: String,
        sourceType: String,
        requestOptions: ApiRequest.Options
    ): Result<Customer> {
        return Result.failure(NotImplementedError())
    }

    override suspend fun setCustomerShippingInfo(
        customerId: String,
        publishableKey: String,
        productUsageTokens: Set<String>,
        shippingInformation: ShippingInformation,
        requestOptions: ApiRequest.Options
    ): Result<Customer> {
        return Result.failure(NotImplementedError())
    }

    override suspend fun retrieveCustomer(
        customerId: String,
        productUsageTokens: Set<String>,
        requestOptions: ApiRequest.Options
    ): Result<Customer> {
        return Result.failure(NotImplementedError())
    }

    override suspend fun retrieveIssuingCardPin(
        cardId: String,
        verificationId: String,
        userOneTimeCode: String,
        requestOptions: ApiRequest.Options
    ): String? {
        TODO("Not yet implemented")
    }

    override suspend fun updateIssuingCardPin(
        cardId: String,
        newPin: String,
        verificationId: String,
        userOneTimeCode: String,
        requestOptions: ApiRequest.Options
    ) {
        TODO("Not yet implemented")
    }

    override suspend fun getFpxBankStatus(
        options: ApiRequest.Options
    ): BankStatuses {
        TODO("Not yet implemented")
    }

    override suspend fun getCardMetadata(bin: Bin, options: ApiRequest.Options): CardMetadata {
        TODO("Not yet implemented")
    }

    override suspend fun start3ds2Auth(
        authParams: Stripe3ds2AuthParams,
        requestOptions: ApiRequest.Options
    ): Result<Stripe3ds2AuthResult> {
        return Result.failure(NotImplementedError())
    }

    override suspend fun complete3ds2Auth(
        sourceId: String,
        requestOptions: ApiRequest.Options
    ): Result<Stripe3ds2AuthResult> {
        return Result.failure(NotImplementedError())
    }

    override suspend fun createFile(
        fileParams: StripeFileParams,
        requestOptions: ApiRequest.Options
    ): StripeFile {
        TODO("Not yet implemented")
    }

    override suspend fun retrieveObject(
        url: String,
        requestOptions: ApiRequest.Options
    ): StripeResponse<String> {
        TODO("Not yet implemented")
    }

    override suspend fun createRadarSession(
        requestOptions: ApiRequest.Options
    ): RadarSession {
        TODO("Not yet implemented")
    }

    override suspend fun consumerSignUp(
        email: String,
        phoneNumber: String,
        country: String,
        name: String?,
        locale: Locale?,
        authSessionCookie: String?,
        consentAction: ConsumerSignUpConsentAction,
        requestOptions: ApiRequest.Options
    ): Result<ConsumerSession> {
        return Result.failure(NotImplementedError())
    }

    override suspend fun createPaymentDetails(
        consumerSessionClientSecret: String,
        paymentDetailsCreateParams: ConsumerPaymentDetailsCreateParams,
        requestOptions: ApiRequest.Options
    ): Result<ConsumerPaymentDetails> {
        return Result.failure(NotImplementedError())
    }

    override suspend fun attachFinancialConnectionsSessionToPaymentIntent(
        clientSecret: String,
        paymentIntentId: String,
        financialConnectionsSessionId: String,
        requestOptions: ApiRequest.Options,
        expandFields: List<String>
    ): Result<PaymentIntent> {
        return Result.failure(NotImplementedError())
    }

    override suspend fun attachFinancialConnectionsSessionToSetupIntent(
        clientSecret: String,
        setupIntentId: String,
        financialConnectionsSessionId: String,
        requestOptions: ApiRequest.Options,
        expandFields: List<String>
    ): Result<SetupIntent> {
        return Result.failure(NotImplementedError())
    }

    override suspend fun createFinancialConnectionsSessionForDeferredPayments(
        params: CreateFinancialConnectionsSessionForDeferredPaymentParams,
        requestOptions: ApiRequest.Options
    ): Result<FinancialConnectionsSession> {
        return Result.failure(NotImplementedError())
    }

    override suspend fun createPaymentIntentFinancialConnectionsSession(
        paymentIntentId: String,
        params: CreateFinancialConnectionsSessionParams,
        requestOptions: ApiRequest.Options
    ): Result<FinancialConnectionsSession> {
        return Result.failure(NotImplementedError())
    }

    override suspend fun createSetupIntentFinancialConnectionsSession(
        setupIntentId: String,
        params: CreateFinancialConnectionsSessionParams,
        requestOptions: ApiRequest.Options
    ): Result<FinancialConnectionsSession> {
        return Result.failure(NotImplementedError())
    }

    override suspend fun verifyPaymentIntentWithMicrodeposits(
        clientSecret: String,
        firstAmount: Int,
        secondAmount: Int,
        requestOptions: ApiRequest.Options
    ): Result<PaymentIntent> {
        return Result.failure(NotImplementedError())
    }

    override suspend fun verifyPaymentIntentWithMicrodeposits(
        clientSecret: String,
        descriptorCode: String,
        requestOptions: ApiRequest.Options
    ): Result<PaymentIntent> {
        return Result.failure(NotImplementedError())
    }

    override suspend fun verifySetupIntentWithMicrodeposits(
        clientSecret: String,
        firstAmount: Int,
        secondAmount: Int,
        requestOptions: ApiRequest.Options
    ): Result<SetupIntent> {
        return Result.failure(NotImplementedError())
    }

    override suspend fun verifySetupIntentWithMicrodeposits(
        clientSecret: String,
        descriptorCode: String,
        requestOptions: ApiRequest.Options
    ): Result<SetupIntent> {
        return Result.failure(NotImplementedError())
    }

    override suspend fun retrievePaymentMethodMessage(
        paymentMethods: List<String>,
        amount: Int,
        currency: String,
        country: String,
        locale: String,
        logoColor: String,
        requestOptions: ApiRequest.Options
    ): Result<PaymentMethodMessage> {
        return Result.failure(NotImplementedError())
    }

    override suspend fun retrieveElementsSession(
        params: ElementsSessionParams,
        options: ApiRequest.Options
    ): Result<ElementsSession> {
        return Result.failure(NotImplementedError())
    }

    override suspend fun retrieveCardMetadata(
        cardNumber: String,
        requestOptions: ApiRequest.Options
    ): CardMetadata? {
        TODO("Not yet implemented")
    }

    override fun buildPaymentUserAgent(attribution: Set<String>): String {
        TODO("Not yet implemented")
    }
}
