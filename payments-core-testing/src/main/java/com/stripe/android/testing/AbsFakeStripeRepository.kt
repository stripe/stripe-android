package com.stripe.android.testing

import com.stripe.android.cards.Bin
import com.stripe.android.core.ApiConfiguration
import com.stripe.android.core.model.StripeFile
import com.stripe.android.core.model.StripeFileParams
import com.stripe.android.core.networking.RequestCredentials
import com.stripe.android.core.networking.StripeResponse
import com.stripe.android.model.BankStatuses
import com.stripe.android.model.CancelCaptchaChallengeParams
import com.stripe.android.model.CardMetadata
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.ConfirmSetupIntentParams
import com.stripe.android.model.ConfirmationToken
import com.stripe.android.model.ConfirmationTokenParams
import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.model.ConsumerPaymentDetailsUpdateParams
import com.stripe.android.model.ConsumerSession
import com.stripe.android.model.ConsumerShippingAddresses
import com.stripe.android.model.CreateFinancialConnectionsSessionForDeferredPaymentParams
import com.stripe.android.model.CreateFinancialConnectionsSessionParams
import com.stripe.android.model.Customer
import com.stripe.android.model.FinancialConnectionsSession
import com.stripe.android.model.ListPaymentMethodsParams
import com.stripe.android.model.MobileCardElementConfig
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodMessage
import com.stripe.android.model.PaymentMethodMessagePromotionList
import com.stripe.android.model.PaymentMethodUpdateParams
import com.stripe.android.model.RadarSessionWithHCaptcha
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

abstract class AbsFakeStripeRepository : StripeRepository {

    override suspend fun retrieveStripeIntent(
        clientSecret: String,
        options: RequestCredentials,
        expandFields: List<String>,
        idempotencyKey: String?,
    ): Result<StripeIntent> {
        TODO("Not yet implemented")
    }

    override suspend fun confirmPaymentIntent(
        confirmPaymentIntentParams: ConfirmPaymentIntentParams,
        options: RequestCredentials,
        expandFields: List<String>,
        idempotencyKey: String?,
    ): Result<PaymentIntent> {
        TODO("Not yet implemented")
    }

    override suspend fun retrievePaymentIntent(
        clientSecret: String,
        options: RequestCredentials,
        expandFields: List<String>,
        idempotencyKey: String?,
    ): Result<PaymentIntent> {
        TODO("Not yet implemented")
    }

    override suspend fun refreshPaymentIntent(
        clientSecret: String,
        options: RequestCredentials,
        idempotencyKey: String?,
    ): Result<PaymentIntent> {
        TODO("Not yet implemented")
    }

    override suspend fun cancelPaymentIntentSource(
        paymentIntentId: String,
        sourceId: String,
        options: RequestCredentials,
        idempotencyKey: String?,
    ): Result<PaymentIntent> {
        TODO("Not yet implemented")
    }

    override suspend fun confirmSetupIntent(
        confirmSetupIntentParams: ConfirmSetupIntentParams,
        options: RequestCredentials,
        expandFields: List<String>,
        idempotencyKey: String?,
    ): Result<SetupIntent> {
        TODO("Not yet implemented")
    }

    override suspend fun retrieveSetupIntent(
        clientSecret: String,
        options: RequestCredentials,
        expandFields: List<String>,
        idempotencyKey: String?,
    ): Result<SetupIntent> {
        TODO("Not yet implemented")
    }

    override suspend fun refreshSetupIntent(
        clientSecret: String,
        options: RequestCredentials,
        idempotencyKey: String?,
    ): Result<SetupIntent> {
        TODO("Not yet implemented")
    }

    override suspend fun cancelSetupIntentSource(
        setupIntentId: String,
        sourceId: String,
        options: RequestCredentials,
        idempotencyKey: String?,
    ): Result<SetupIntent> {
        TODO("Not yet implemented")
    }

    override suspend fun createSource(
        sourceParams: SourceParams,
        options: RequestCredentials,
        idempotencyKey: String?,
    ): Result<Source> {
        TODO("Not yet implemented")
    }

    override suspend fun retrieveSource(
        sourceId: String,
        clientSecret: String,
        options: RequestCredentials,
        idempotencyKey: String?,
    ): Result<Source> {
        TODO("Not yet implemented")
    }

    override suspend fun createPaymentMethod(
        paymentMethodCreateParams: PaymentMethodCreateParams,
        options: RequestCredentials,
        idempotencyKey: String?,
    ): Result<PaymentMethod> {
        TODO("Not yet implemented")
    }

    override suspend fun updatePaymentMethod(
        paymentMethodId: String,
        paymentMethodUpdateParams: PaymentMethodUpdateParams,
        options: RequestCredentials,
        idempotencyKey: String?,
    ): Result<PaymentMethod> {
        TODO("Not yet implemented")
    }

    override suspend fun createToken(
        tokenParams: TokenParams,
        options: RequestCredentials,
        idempotencyKey: String?,
    ): Result<Token> {
        TODO("Not yet implemented")
    }

    override suspend fun createConfirmationToken(
        confirmationTokenParams: ConfirmationTokenParams,
        options: RequestCredentials,
        idempotencyKey: String?,
    ): Result<ConfirmationToken> {
        TODO("Not yet implemented")
    }

    override suspend fun addCustomerSource(
        customerId: String,
        publishableKey: String,
        productUsageTokens: Set<String>,
        sourceId: String,
        sourceType: String,
        requestOptions: RequestCredentials,
        idempotencyKey: String?,
    ): Result<Source> {
        TODO("Not yet implemented")
    }

    override suspend fun deleteCustomerSource(
        customerId: String,
        publishableKey: String,
        productUsageTokens: Set<String>,
        sourceId: String,
        requestOptions: RequestCredentials,
        idempotencyKey: String?,
    ): Result<Source> {
        TODO("Not yet implemented")
    }

    override suspend fun attachPaymentMethod(
        customerId: String,
        productUsageTokens: Set<String>,
        paymentMethodId: String,
        requestOptions: RequestCredentials,
        idempotencyKey: String?,
    ): Result<PaymentMethod> {
        TODO("Not yet implemented")
    }

    override suspend fun detachPaymentMethod(
        productUsageTokens: Set<String>,
        paymentMethodId: String,
        requestOptions: RequestCredentials,
        idempotencyKey: String?,
    ): Result<PaymentMethod> {
        TODO("Not yet implemented")
    }

    override suspend fun detachPaymentMethod(
        customerSessionClientSecret: String,
        productUsageTokens: Set<String>,
        paymentMethodId: String,
        requestOptions: RequestCredentials,
        idempotencyKey: String?,
    ): Result<PaymentMethod> {
        TODO("Not yet implemented")
    }

    override suspend fun getPaymentMethods(
        listPaymentMethodsParams: ListPaymentMethodsParams,
        productUsageTokens: Set<String>,
        requestOptions: RequestCredentials,
        apiConfiguration: ApiConfiguration.State,
        idempotencyKey: String?,
    ): Result<List<PaymentMethod>> {
        TODO("Not yet implemented")
    }

    override suspend fun retrieveCustomerPaymentMethod(
        customerId: String,
        paymentMethodId: String,
        productUsageTokens: Set<String>,
        requestOptions: RequestCredentials,
        idempotencyKey: String?,
    ): Result<PaymentMethod> {
        TODO("Not yet implemented")
    }

    override suspend fun retrieveSavedPaymentMethodFromCardPresentPaymentMethod(
        cardPresentPaymentMethodId: String,
        customerId: String,
        options: RequestCredentials,
        idempotencyKey: String?,
    ): Result<PaymentMethod> {
        TODO("Not yet implemented")
    }

    override suspend fun setDefaultCustomerSource(
        customerId: String,
        publishableKey: String,
        productUsageTokens: Set<String>,
        sourceId: String,
        sourceType: String,
        requestOptions: RequestCredentials,
        idempotencyKey: String?,
    ): Result<Customer> {
        TODO("Not yet implemented")
    }

    override suspend fun setCustomerShippingInfo(
        customerId: String,
        publishableKey: String,
        productUsageTokens: Set<String>,
        shippingInformation: ShippingInformation,
        requestOptions: RequestCredentials,
        idempotencyKey: String?,
    ): Result<Customer> {
        TODO("Not yet implemented")
    }

    override suspend fun retrieveCustomer(
        customerId: String,
        productUsageTokens: Set<String>,
        requestOptions: RequestCredentials,
        idempotencyKey: String?,
    ): Result<Customer> {
        TODO("Not yet implemented")
    }

    override suspend fun retrieveIssuingCardPin(
        cardId: String,
        verificationId: String,
        userOneTimeCode: String,
        requestOptions: RequestCredentials,
        idempotencyKey: String?,
    ): Result<String> {
        TODO("Not yet implemented")
    }

    override suspend fun updateIssuingCardPin(
        cardId: String,
        newPin: String,
        verificationId: String,
        userOneTimeCode: String,
        requestOptions: RequestCredentials,
        idempotencyKey: String?,
    ): Throwable? {
        TODO("Not yet implemented")
    }

    override suspend fun getFpxBankStatus(
        options: RequestCredentials,
        idempotencyKey: String?,
    ): Result<BankStatuses> {
        TODO("Not yet implemented")
    }

    override suspend fun getCardMetadata(
        bin: Bin,
        options: RequestCredentials,
        idempotencyKey: String?,
    ): Result<CardMetadata> {
        TODO("Not yet implemented")
    }

    override suspend fun start3ds2Auth(
        authParams: Stripe3ds2AuthParams,
        requestOptions: RequestCredentials,
        idempotencyKey: String?,
    ): Result<Stripe3ds2AuthResult> {
        TODO("Not yet implemented")
    }

    override suspend fun complete3ds2Auth(
        sourceId: String,
        requestOptions: RequestCredentials,
        idempotencyKey: String?,
    ): Result<Stripe3ds2AuthResult> {
        TODO("Not yet implemented")
    }

    override suspend fun createFile(
        fileParams: StripeFileParams,
        requestOptions: RequestCredentials,
        idempotencyKey: String?,
    ): Result<StripeFile> {
        TODO("Not yet implemented")
    }

    override suspend fun retrieveObject(
        url: String,
        requestOptions: RequestCredentials,
        idempotencyKey: String?,
    ): Result<StripeResponse<String>> {
        TODO("Not yet implemented")
    }

    override suspend fun createRadarSession(
        requestOptions: RequestCredentials,
        idempotencyKey: String?,
    ): Result<RadarSessionWithHCaptcha> {
        TODO("Not yet implemented")
    }

    override suspend fun attachHCaptchaToRadarSession(
        radarSessionToken: String,
        hcaptchaToken: String,
        hcaptchaEKey: String?,
        requestOptions: RequestCredentials,
        idempotencyKey: String?,
    ): Result<RadarSessionWithHCaptcha> {
        TODO("Not yet implemented")
    }

    override suspend fun sharePaymentDetails(
        consumerSessionClientSecret: String,
        id: String,
        extraParams: Map<String, *>?,
        requestOptions: RequestCredentials,
        idempotencyKey: String?,
    ): Result<PaymentMethod> {
        TODO("Not yet implemented")
    }

    override suspend fun setDefaultPaymentMethod(
        customerId: String,
        paymentMethodId: String?,
        options: RequestCredentials,
        idempotencyKey: String?,
    ): Result<Customer> {
        TODO("Not yet implemented")
    }

    override suspend fun logOut(
        consumerSessionClientSecret: String,
        consumerAccountPublishableKey: String?,
        requestOptions: RequestCredentials,
        idempotencyKey: String?,
    ): Result<ConsumerSession> {
        TODO("Not yet implemented")
    }

    override suspend fun attachFinancialConnectionsSessionToPaymentIntent(
        clientSecret: String,
        paymentIntentId: String,
        financialConnectionsSessionId: String,
        requestOptions: RequestCredentials,
        expandFields: List<String>,
        idempotencyKey: String?,
    ): Result<PaymentIntent> {
        TODO("Not yet implemented")
    }

    override suspend fun attachFinancialConnectionsSessionToSetupIntent(
        clientSecret: String,
        setupIntentId: String,
        financialConnectionsSessionId: String,
        requestOptions: RequestCredentials,
        expandFields: List<String>,
        idempotencyKey: String?,
    ): Result<SetupIntent> {
        TODO("Not yet implemented")
    }

    override suspend fun createFinancialConnectionsSessionForDeferredPayments(
        params: CreateFinancialConnectionsSessionForDeferredPaymentParams,
        requestOptions: RequestCredentials,
        idempotencyKey: String?,
    ): Result<FinancialConnectionsSession> {
        TODO("Not yet implemented")
    }

    override suspend fun createPaymentIntentFinancialConnectionsSession(
        paymentIntentId: String,
        params: CreateFinancialConnectionsSessionParams,
        requestOptions: RequestCredentials,
        idempotencyKey: String?,
    ): Result<FinancialConnectionsSession> {
        TODO("Not yet implemented")
    }

    override suspend fun createSetupIntentFinancialConnectionsSession(
        setupIntentId: String,
        params: CreateFinancialConnectionsSessionParams,
        requestOptions: RequestCredentials,
        idempotencyKey: String?,
    ): Result<FinancialConnectionsSession> {
        TODO("Not yet implemented")
    }

    override suspend fun verifyPaymentIntentWithMicrodeposits(
        clientSecret: String,
        firstAmount: Int,
        secondAmount: Int,
        requestOptions: RequestCredentials,
        idempotencyKey: String?,
    ): Result<PaymentIntent> {
        TODO("Not yet implemented")
    }

    override suspend fun verifyPaymentIntentWithMicrodeposits(
        clientSecret: String,
        descriptorCode: String,
        requestOptions: RequestCredentials,
        idempotencyKey: String?,
    ): Result<PaymentIntent> {
        TODO("Not yet implemented")
    }

    override suspend fun verifySetupIntentWithMicrodeposits(
        clientSecret: String,
        firstAmount: Int,
        secondAmount: Int,
        requestOptions: RequestCredentials,
        idempotencyKey: String?,
    ): Result<SetupIntent> {
        TODO("Not yet implemented")
    }

    override suspend fun verifySetupIntentWithMicrodeposits(
        clientSecret: String,
        descriptorCode: String,
        requestOptions: RequestCredentials,
        idempotencyKey: String?,
    ): Result<SetupIntent> {
        TODO("Not yet implemented")
    }

    override suspend fun retrievePaymentMethodMessage(
        paymentMethods: List<String>,
        amount: Int,
        currency: String,
        country: String?,
        locale: String,
        requestOptions: RequestCredentials,
        idempotencyKey: String?,
    ): Result<PaymentMethodMessage> {
        TODO("Not yet implemented")
    }

    override suspend fun retrievePaymentMethodMessagePromotionsForPaymentSheet(
        amount: Int,
        currency: String,
        country: String?,
        locale: String,
        requestOptions: RequestCredentials,
        idempotencyKey: String?,
    ): Result<PaymentMethodMessagePromotionList> {
        TODO("Not yet implemented")
    }

    override suspend fun retrieveCardMetadata(
        cardNumber: String,
        requestOptions: RequestCredentials,
        idempotencyKey: String?,
    ): Result<CardMetadata> {
        TODO("Not yet implemented")
    }

    override suspend fun retrieveCardElementConfig(
        requestOptions: RequestCredentials,
        params: Map<String, String>?,
        idempotencyKey: String?,
    ): Result<MobileCardElementConfig> {
        TODO("Not yet implemented")
    }

    override fun buildPaymentUserAgent(attribution: Set<String>): String {
        TODO("Not yet implemented")
    }

    override suspend fun listPaymentDetails(
        clientSecret: String,
        paymentMethodTypes: Set<String>,
        requestOptions: RequestCredentials,
        idempotencyKey: String?,
    ): Result<ConsumerPaymentDetails> {
        TODO("Not yet implemented")
    }

    override suspend fun listShippingAddresses(
        clientSecret: String,
        requestOptions: RequestCredentials,
        idempotencyKey: String?,
    ): Result<ConsumerShippingAddresses> {
        TODO("Not yet implemented")
    }

    override suspend fun deletePaymentDetails(
        clientSecret: String,
        paymentDetailsId: String,
        requestOptions: RequestCredentials,
        idempotencyKey: String?,
    ): Result<Unit> {
        TODO("Not yet implemented")
    }

    override suspend fun updatePaymentDetails(
        clientSecret: String,
        paymentDetailsUpdateParams: ConsumerPaymentDetailsUpdateParams,
        requestOptions: RequestCredentials,
        idempotencyKey: String?,
    ): Result<ConsumerPaymentDetails> {
        TODO("Not yet implemented")
    }

    override suspend fun cancelPaymentIntentCaptchaChallenge(
        paymentIntentId: String,
        params: CancelCaptchaChallengeParams,
        requestOptions: RequestCredentials,
        idempotencyKey: String?,
    ): Result<PaymentIntent> {
        TODO("Not yet implemented")
    }

    override suspend fun cancelSetupIntentCaptchaChallenge(
        setupIntentId: String,
        params: CancelCaptchaChallengeParams,
        requestOptions: RequestCredentials,
        idempotencyKey: String?,
    ): Result<SetupIntent> {
        TODO("Not yet implemented")
    }
}
