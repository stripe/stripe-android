package com.stripe.android.networking

import androidx.annotation.RestrictTo
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

/**
 * An interface for data operations on Stripe API objects.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // originally made public for paymentsheet
@Suppress("LargeClass")
interface StripeRepository {
    /** Creates an operation-scoped repository carrying this immutable session configuration. */
    fun withApiConfiguration(apiConfiguration: ApiConfiguration.State): StripeRepository = this

    suspend fun retrieveStripeIntent(
        clientSecret: String,
        options: RequestCredentials,
        expandFields: List<String> = emptyList(),
        apiConfiguration: ApiConfiguration.State,
        idempotencyKey: String?,
    ): Result<StripeIntent> {
        return withApiConfiguration(apiConfiguration).retrieveStripeIntent(
            clientSecret = clientSecret,
            options = options,
            expandFields = expandFields,
            idempotencyKey = idempotencyKey,
        )
    }

    suspend fun confirmPaymentIntent(
        confirmPaymentIntentParams: ConfirmPaymentIntentParams,
        options: RequestCredentials,
        expandFields: List<String> = emptyList(),
        apiConfiguration: ApiConfiguration.State,
        idempotencyKey: String?,
    ): Result<PaymentIntent> {
        return withApiConfiguration(apiConfiguration).confirmPaymentIntent(
            confirmPaymentIntentParams = confirmPaymentIntentParams,
            options = options,
            expandFields = expandFields,
            idempotencyKey = idempotencyKey,
        )
    }

    suspend fun retrievePaymentIntent(
        clientSecret: String,
        options: RequestCredentials,
        expandFields: List<String> = emptyList(),
        apiConfiguration: ApiConfiguration.State,
        idempotencyKey: String?,
    ): Result<PaymentIntent> {
        return withApiConfiguration(apiConfiguration).retrievePaymentIntent(
            clientSecret = clientSecret,
            options = options,
            expandFields = expandFields,
            idempotencyKey = idempotencyKey,
        )
    }

    suspend fun refreshPaymentIntent(
        clientSecret: String,
        options: RequestCredentials,
        apiConfiguration: ApiConfiguration.State,
        idempotencyKey: String?,
    ): Result<PaymentIntent> {
        return withApiConfiguration(apiConfiguration).refreshPaymentIntent(
            clientSecret = clientSecret,
            options = options,
            idempotencyKey = idempotencyKey,
        )
    }

    suspend fun cancelPaymentIntentSource(
        paymentIntentId: String,
        sourceId: String,
        options: RequestCredentials,
        apiConfiguration: ApiConfiguration.State,
        idempotencyKey: String?,
    ): Result<PaymentIntent> {
        return withApiConfiguration(apiConfiguration).cancelPaymentIntentSource(
            paymentIntentId = paymentIntentId,
            sourceId = sourceId,
            options = options,
            idempotencyKey = idempotencyKey,
        )
    }

    suspend fun confirmSetupIntent(
        confirmSetupIntentParams: ConfirmSetupIntentParams,
        options: RequestCredentials,
        expandFields: List<String> = emptyList(),
        apiConfiguration: ApiConfiguration.State,
        idempotencyKey: String?,
    ): Result<SetupIntent> {
        return withApiConfiguration(apiConfiguration).confirmSetupIntent(
            confirmSetupIntentParams = confirmSetupIntentParams,
            options = options,
            expandFields = expandFields,
            idempotencyKey = idempotencyKey,
        )
    }

    suspend fun retrieveSetupIntent(
        clientSecret: String,
        options: RequestCredentials,
        expandFields: List<String> = emptyList(),
        apiConfiguration: ApiConfiguration.State,
        idempotencyKey: String?,
    ): Result<SetupIntent> {
        return withApiConfiguration(apiConfiguration).retrieveSetupIntent(
            clientSecret = clientSecret,
            options = options,
            expandFields = expandFields,
            idempotencyKey = idempotencyKey,
        )
    }

    suspend fun refreshSetupIntent(
        clientSecret: String,
        options: RequestCredentials,
        apiConfiguration: ApiConfiguration.State,
        idempotencyKey: String?,
    ): Result<SetupIntent> {
        return withApiConfiguration(apiConfiguration).refreshSetupIntent(
            clientSecret = clientSecret,
            options = options,
            idempotencyKey = idempotencyKey,
        )
    }

    suspend fun cancelSetupIntentSource(
        setupIntentId: String,
        sourceId: String,
        options: RequestCredentials,
        apiConfiguration: ApiConfiguration.State,
        idempotencyKey: String?,
    ): Result<SetupIntent> {
        return withApiConfiguration(apiConfiguration).cancelSetupIntentSource(
            setupIntentId = setupIntentId,
            sourceId = sourceId,
            options = options,
            idempotencyKey = idempotencyKey,
        )
    }

    suspend fun createSource(
        sourceParams: SourceParams,
        options: RequestCredentials,
        apiConfiguration: ApiConfiguration.State,
        idempotencyKey: String?,
    ): Result<Source> {
        return withApiConfiguration(apiConfiguration).createSource(
            sourceParams = sourceParams,
            options = options,
            idempotencyKey = idempotencyKey,
        )
    }

    suspend fun retrieveSource(
        sourceId: String,
        clientSecret: String,
        options: RequestCredentials,
        apiConfiguration: ApiConfiguration.State,
        idempotencyKey: String?,
    ): Result<Source> {
        return withApiConfiguration(apiConfiguration).retrieveSource(
            sourceId = sourceId,
            clientSecret = clientSecret,
            options = options,
            idempotencyKey = idempotencyKey,
        )
    }

    suspend fun createPaymentMethod(
        paymentMethodCreateParams: PaymentMethodCreateParams,
        options: RequestCredentials,
        apiConfiguration: ApiConfiguration.State,
        idempotencyKey: String?,
    ): Result<PaymentMethod> {
        return withApiConfiguration(apiConfiguration).createPaymentMethod(
            paymentMethodCreateParams = paymentMethodCreateParams,
            options = options,
            idempotencyKey = idempotencyKey,
        )
    }

    suspend fun updatePaymentMethod(
        paymentMethodId: String,
        paymentMethodUpdateParams: PaymentMethodUpdateParams,
        options: RequestCredentials,
        apiConfiguration: ApiConfiguration.State,
        idempotencyKey: String?,
    ): Result<PaymentMethod> {
        return withApiConfiguration(apiConfiguration).updatePaymentMethod(
            paymentMethodId = paymentMethodId,
            paymentMethodUpdateParams = paymentMethodUpdateParams,
            options = options,
            idempotencyKey = idempotencyKey,
        )
    }

    suspend fun setDefaultPaymentMethod(
        customerId: String,
        paymentMethodId: String?,
        options: RequestCredentials,
        apiConfiguration: ApiConfiguration.State,
        idempotencyKey: String?,
    ): Result<Customer> {
        return withApiConfiguration(apiConfiguration).setDefaultPaymentMethod(
            customerId = customerId,
            paymentMethodId = paymentMethodId,
            options = options,
            idempotencyKey = idempotencyKey,
        )
    }

    suspend fun retrieveSavedPaymentMethodFromCardPresentPaymentMethod(
        cardPresentPaymentMethodId: String,
        customerId: String,
        options: RequestCredentials,
        apiConfiguration: ApiConfiguration.State,
        idempotencyKey: String?,
    ): Result<PaymentMethod> {
        return withApiConfiguration(apiConfiguration).retrieveSavedPaymentMethodFromCardPresentPaymentMethod(
            cardPresentPaymentMethodId = cardPresentPaymentMethodId,
            customerId = customerId,
            options = options,
            idempotencyKey = idempotencyKey,
        )
    }

    suspend fun createToken(
        tokenParams: TokenParams,
        options: RequestCredentials,
        apiConfiguration: ApiConfiguration.State,
        idempotencyKey: String?,
    ): Result<Token> {
        return withApiConfiguration(apiConfiguration).createToken(
            tokenParams = tokenParams,
            options = options,
            idempotencyKey = idempotencyKey,
        )
    }

    suspend fun createConfirmationToken(
        confirmationTokenParams: ConfirmationTokenParams,
        options: RequestCredentials,
        apiConfiguration: ApiConfiguration.State,
        idempotencyKey: String?,
    ): Result<ConfirmationToken> {
        return withApiConfiguration(apiConfiguration).createConfirmationToken(
            confirmationTokenParams = confirmationTokenParams,
            options = options,
            idempotencyKey = idempotencyKey,
        )
    }

    suspend fun addCustomerSource(
        customerId: String,
        publishableKey: String,
        productUsageTokens: Set<String>,
        sourceId: String,
        @Source.SourceType sourceType: String,
        requestOptions: RequestCredentials,
        apiConfiguration: ApiConfiguration.State,
        idempotencyKey: String?,
    ): Result<Source> {
        return withApiConfiguration(apiConfiguration).addCustomerSource(
            customerId = customerId,
            publishableKey = publishableKey,
            productUsageTokens = productUsageTokens,
            sourceId = sourceId,
            sourceType = sourceType,
            requestOptions = requestOptions,
            idempotencyKey = idempotencyKey,
        )
    }

    suspend fun deleteCustomerSource(
        customerId: String,
        publishableKey: String,
        productUsageTokens: Set<String>,
        sourceId: String,
        requestOptions: RequestCredentials,
        apiConfiguration: ApiConfiguration.State,
        idempotencyKey: String?,
    ): Result<Source> {
        return withApiConfiguration(apiConfiguration).deleteCustomerSource(
            customerId = customerId,
            publishableKey = publishableKey,
            productUsageTokens = productUsageTokens,
            sourceId = sourceId,
            requestOptions = requestOptions,
            idempotencyKey = idempotencyKey,
        )
    }

    suspend fun attachPaymentMethod(
        customerId: String,
        productUsageTokens: Set<String>,
        paymentMethodId: String,
        requestOptions: RequestCredentials,
        apiConfiguration: ApiConfiguration.State,
        idempotencyKey: String?,
    ): Result<PaymentMethod> {
        return withApiConfiguration(apiConfiguration).attachPaymentMethod(
            customerId = customerId,
            productUsageTokens = productUsageTokens,
            paymentMethodId = paymentMethodId,
            requestOptions = requestOptions,
            idempotencyKey = idempotencyKey,
        )
    }

    suspend fun detachPaymentMethod(
        productUsageTokens: Set<String>,
        paymentMethodId: String,
        requestOptions: RequestCredentials,
        apiConfiguration: ApiConfiguration.State,
        idempotencyKey: String?,
    ): Result<PaymentMethod> {
        return withApiConfiguration(apiConfiguration).detachPaymentMethod(
            productUsageTokens = productUsageTokens,
            paymentMethodId = paymentMethodId,
            requestOptions = requestOptions,
            idempotencyKey = idempotencyKey,
        )
    }

    suspend fun detachPaymentMethod(
        customerSessionClientSecret: String,
        productUsageTokens: Set<String>,
        paymentMethodId: String,
        requestOptions: RequestCredentials,
        apiConfiguration: ApiConfiguration.State,
        idempotencyKey: String?,
    ): Result<PaymentMethod> {
        return withApiConfiguration(apiConfiguration).detachPaymentMethod(
            customerSessionClientSecret = customerSessionClientSecret,
            productUsageTokens = productUsageTokens,
            paymentMethodId = paymentMethodId,
            requestOptions = requestOptions,
            idempotencyKey = idempotencyKey,
        )
    }

    suspend fun retrieveCustomerPaymentMethod(
        customerId: String,
        paymentMethodId: String,
        productUsageTokens: Set<String>,
        requestOptions: RequestCredentials,
        apiConfiguration: ApiConfiguration.State,
        idempotencyKey: String?,
    ): Result<PaymentMethod> {
        return withApiConfiguration(apiConfiguration).retrieveCustomerPaymentMethod(
            customerId = customerId,
            paymentMethodId = paymentMethodId,
            productUsageTokens = productUsageTokens,
            requestOptions = requestOptions,
            idempotencyKey = idempotencyKey,
        )
    }

    suspend fun setDefaultCustomerSource(
        customerId: String,
        publishableKey: String,
        productUsageTokens: Set<String>,
        sourceId: String,
        @Source.SourceType sourceType: String,
        requestOptions: RequestCredentials,
        apiConfiguration: ApiConfiguration.State,
        idempotencyKey: String?,
    ): Result<Customer> {
        return withApiConfiguration(apiConfiguration).setDefaultCustomerSource(
            customerId = customerId,
            publishableKey = publishableKey,
            productUsageTokens = productUsageTokens,
            sourceId = sourceId,
            sourceType = sourceType,
            requestOptions = requestOptions,
            idempotencyKey = idempotencyKey,
        )
    }

    suspend fun setCustomerShippingInfo(
        customerId: String,
        publishableKey: String,
        productUsageTokens: Set<String>,
        shippingInformation: ShippingInformation,
        requestOptions: RequestCredentials,
        apiConfiguration: ApiConfiguration.State,
        idempotencyKey: String?,
    ): Result<Customer> {
        return withApiConfiguration(apiConfiguration).setCustomerShippingInfo(
            customerId = customerId,
            publishableKey = publishableKey,
            productUsageTokens = productUsageTokens,
            shippingInformation = shippingInformation,
            requestOptions = requestOptions,
            idempotencyKey = idempotencyKey,
        )
    }

    suspend fun retrieveCustomer(
        customerId: String,
        productUsageTokens: Set<String>,
        requestOptions: RequestCredentials,
        apiConfiguration: ApiConfiguration.State,
        idempotencyKey: String?,
    ): Result<Customer> {
        return withApiConfiguration(apiConfiguration).retrieveCustomer(
            customerId = customerId,
            productUsageTokens = productUsageTokens,
            requestOptions = requestOptions,
            idempotencyKey = idempotencyKey,
        )
    }

    suspend fun retrieveIssuingCardPin(
        cardId: String,
        verificationId: String,
        userOneTimeCode: String,
        requestOptions: RequestCredentials,
        apiConfiguration: ApiConfiguration.State,
        idempotencyKey: String?,
    ): Result<String> {
        return withApiConfiguration(apiConfiguration).retrieveIssuingCardPin(
            cardId = cardId,
            verificationId = verificationId,
            userOneTimeCode = userOneTimeCode,
            requestOptions = requestOptions,
            idempotencyKey = idempotencyKey,
        )
    }

    suspend fun updateIssuingCardPin(
        cardId: String,
        newPin: String,
        verificationId: String,
        userOneTimeCode: String,
        requestOptions: RequestCredentials,
        apiConfiguration: ApiConfiguration.State,
        idempotencyKey: String?,
    ): Throwable? {
        return withApiConfiguration(apiConfiguration).updateIssuingCardPin(
            cardId = cardId,
            newPin = newPin,
            verificationId = verificationId,
            userOneTimeCode = userOneTimeCode,
            requestOptions = requestOptions,
            idempotencyKey = idempotencyKey,
        )
    }

    suspend fun getFpxBankStatus(
        options: RequestCredentials,
        apiConfiguration: ApiConfiguration.State,
        idempotencyKey: String?,
    ): Result<BankStatuses> {
        return withApiConfiguration(apiConfiguration).getFpxBankStatus(
            options = options,
            idempotencyKey = idempotencyKey,
        )
    }

    suspend fun getCardMetadata(
        bin: Bin,
        options: RequestCredentials,
        apiConfiguration: ApiConfiguration.State,
        idempotencyKey: String?,
    ): Result<CardMetadata> {
        return withApiConfiguration(apiConfiguration).getCardMetadata(
            bin = bin,
            options = options,
            idempotencyKey = idempotencyKey,
        )
    }

    suspend fun start3ds2Auth(
        authParams: Stripe3ds2AuthParams,
        requestOptions: RequestCredentials,
        apiConfiguration: ApiConfiguration.State,
        idempotencyKey: String?,
    ): Result<Stripe3ds2AuthResult> {
        return withApiConfiguration(apiConfiguration).start3ds2Auth(
            authParams = authParams,
            requestOptions = requestOptions,
            idempotencyKey = idempotencyKey,
        )
    }

    suspend fun complete3ds2Auth(
        sourceId: String,
        requestOptions: RequestCredentials,
        apiConfiguration: ApiConfiguration.State,
        idempotencyKey: String?,
    ): Result<Stripe3ds2AuthResult> {
        return withApiConfiguration(apiConfiguration).complete3ds2Auth(
            sourceId = sourceId,
            requestOptions = requestOptions,
            idempotencyKey = idempotencyKey,
        )
    }

    suspend fun createFile(
        fileParams: StripeFileParams,
        requestOptions: RequestCredentials,
        apiConfiguration: ApiConfiguration.State,
        idempotencyKey: String?,
    ): Result<StripeFile> {
        return withApiConfiguration(apiConfiguration).createFile(
            fileParams = fileParams,
            requestOptions = requestOptions,
            idempotencyKey = idempotencyKey,
        )
    }

    suspend fun retrieveObject(
        url: String,
        requestOptions: RequestCredentials,
        apiConfiguration: ApiConfiguration.State,
        idempotencyKey: String?,
    ): Result<StripeResponse<String>> {
        return withApiConfiguration(apiConfiguration).retrieveObject(
            url = url,
            requestOptions = requestOptions,
            idempotencyKey = idempotencyKey,
        )
    }

    suspend fun createRadarSession(
        requestOptions: RequestCredentials,
        apiConfiguration: ApiConfiguration.State,
        idempotencyKey: String?,
    ): Result<RadarSessionWithHCaptcha> {
        return withApiConfiguration(apiConfiguration).createRadarSession(
            requestOptions = requestOptions,
            idempotencyKey = idempotencyKey,
        )
    }

    suspend fun attachHCaptchaToRadarSession(
        radarSessionToken: String,
        hcaptchaToken: String,
        hcaptchaEKey: String?,
        requestOptions: RequestCredentials,
        apiConfiguration: ApiConfiguration.State,
        idempotencyKey: String?,
    ): Result<RadarSessionWithHCaptcha> {
        return withApiConfiguration(apiConfiguration).attachHCaptchaToRadarSession(
            radarSessionToken = radarSessionToken,
            hcaptchaToken = hcaptchaToken,
            hcaptchaEKey = hcaptchaEKey,
            requestOptions = requestOptions,
            idempotencyKey = idempotencyKey,
        )
    }

    suspend fun sharePaymentDetails(
        consumerSessionClientSecret: String,
        id: String,
        extraParams: Map<String, *>?,
        requestOptions: RequestCredentials,
        apiConfiguration: ApiConfiguration.State,
        idempotencyKey: String?,
    ): Result<PaymentMethod> {
        return withApiConfiguration(apiConfiguration).sharePaymentDetails(
            consumerSessionClientSecret = consumerSessionClientSecret,
            id = id,
            extraParams = extraParams,
            requestOptions = requestOptions,
            idempotencyKey = idempotencyKey,
        )
    }

    suspend fun logOut(
        consumerSessionClientSecret: String,
        consumerAccountPublishableKey: String?,
        requestOptions: RequestCredentials,
        apiConfiguration: ApiConfiguration.State,
        idempotencyKey: String?,
    ): Result<ConsumerSession> {
        return withApiConfiguration(apiConfiguration).logOut(
            consumerSessionClientSecret = consumerSessionClientSecret,
            consumerAccountPublishableKey = consumerAccountPublishableKey,
            requestOptions = requestOptions,
            idempotencyKey = idempotencyKey,
        )
    }

    suspend fun createFinancialConnectionsSessionForDeferredPayments(
        params: CreateFinancialConnectionsSessionForDeferredPaymentParams,
        requestOptions: RequestCredentials,
        apiConfiguration: ApiConfiguration.State,
        idempotencyKey: String?,
    ): Result<FinancialConnectionsSession> {
        return withApiConfiguration(apiConfiguration).createFinancialConnectionsSessionForDeferredPayments(
            params = params,
            requestOptions = requestOptions,
            idempotencyKey = idempotencyKey,
        )
    }

    suspend fun createPaymentIntentFinancialConnectionsSession(
        paymentIntentId: String,
        params: CreateFinancialConnectionsSessionParams,
        requestOptions: RequestCredentials,
        apiConfiguration: ApiConfiguration.State,
        idempotencyKey: String?,
    ): Result<FinancialConnectionsSession> {
        return withApiConfiguration(apiConfiguration).createPaymentIntentFinancialConnectionsSession(
            paymentIntentId = paymentIntentId,
            params = params,
            requestOptions = requestOptions,
            idempotencyKey = idempotencyKey,
        )
    }

    suspend fun createSetupIntentFinancialConnectionsSession(
        setupIntentId: String,
        params: CreateFinancialConnectionsSessionParams,
        requestOptions: RequestCredentials,
        apiConfiguration: ApiConfiguration.State,
        idempotencyKey: String?,
    ): Result<FinancialConnectionsSession> {
        return withApiConfiguration(apiConfiguration).createSetupIntentFinancialConnectionsSession(
            setupIntentId = setupIntentId,
            params = params,
            requestOptions = requestOptions,
            idempotencyKey = idempotencyKey,
        )
    }

    suspend fun attachFinancialConnectionsSessionToPaymentIntent(
        clientSecret: String,
        paymentIntentId: String,
        financialConnectionsSessionId: String,
        requestOptions: RequestCredentials,
        expandFields: List<String>,
        apiConfiguration: ApiConfiguration.State,
        idempotencyKey: String?,
    ): Result<PaymentIntent> {
        return withApiConfiguration(apiConfiguration).attachFinancialConnectionsSessionToPaymentIntent(
            clientSecret = clientSecret,
            paymentIntentId = paymentIntentId,
            financialConnectionsSessionId = financialConnectionsSessionId,
            requestOptions = requestOptions,
            expandFields = expandFields,
            idempotencyKey = idempotencyKey,
        )
    }

    suspend fun attachFinancialConnectionsSessionToSetupIntent(
        clientSecret: String,
        setupIntentId: String,
        financialConnectionsSessionId: String,
        requestOptions: RequestCredentials,
        expandFields: List<String>,
        apiConfiguration: ApiConfiguration.State,
        idempotencyKey: String?,
    ): Result<SetupIntent> {
        return withApiConfiguration(apiConfiguration).attachFinancialConnectionsSessionToSetupIntent(
            clientSecret = clientSecret,
            setupIntentId = setupIntentId,
            financialConnectionsSessionId = financialConnectionsSessionId,
            requestOptions = requestOptions,
            expandFields = expandFields,
            idempotencyKey = idempotencyKey,
        )
    }

    suspend fun verifyPaymentIntentWithMicrodeposits(
        clientSecret: String,
        firstAmount: Int,
        secondAmount: Int,
        requestOptions: RequestCredentials,
        apiConfiguration: ApiConfiguration.State,
        idempotencyKey: String?,
    ): Result<PaymentIntent> {
        return withApiConfiguration(apiConfiguration).verifyPaymentIntentWithMicrodeposits(
            clientSecret = clientSecret,
            firstAmount = firstAmount,
            secondAmount = secondAmount,
            requestOptions = requestOptions,
            idempotencyKey = idempotencyKey,
        )
    }

    suspend fun verifyPaymentIntentWithMicrodeposits(
        clientSecret: String,
        descriptorCode: String,
        requestOptions: RequestCredentials,
        apiConfiguration: ApiConfiguration.State,
        idempotencyKey: String?,
    ): Result<PaymentIntent> {
        return withApiConfiguration(apiConfiguration).verifyPaymentIntentWithMicrodeposits(
            clientSecret = clientSecret,
            descriptorCode = descriptorCode,
            requestOptions = requestOptions,
            idempotencyKey = idempotencyKey,
        )
    }

    suspend fun verifySetupIntentWithMicrodeposits(
        clientSecret: String,
        firstAmount: Int,
        secondAmount: Int,
        requestOptions: RequestCredentials,
        apiConfiguration: ApiConfiguration.State,
        idempotencyKey: String?,
    ): Result<SetupIntent> {
        return withApiConfiguration(apiConfiguration).verifySetupIntentWithMicrodeposits(
            clientSecret = clientSecret,
            firstAmount = firstAmount,
            secondAmount = secondAmount,
            requestOptions = requestOptions,
            idempotencyKey = idempotencyKey,
        )
    }

    suspend fun verifySetupIntentWithMicrodeposits(
        clientSecret: String,
        descriptorCode: String,
        requestOptions: RequestCredentials,
        apiConfiguration: ApiConfiguration.State,
        idempotencyKey: String?,
    ): Result<SetupIntent> {
        return withApiConfiguration(apiConfiguration).verifySetupIntentWithMicrodeposits(
            clientSecret = clientSecret,
            descriptorCode = descriptorCode,
            requestOptions = requestOptions,
            idempotencyKey = idempotencyKey,
        )
    }

    suspend fun retrievePaymentMethodMessage(
        paymentMethods: List<String>,
        amount: Int,
        currency: String,
        country: String?,
        locale: String,
        requestOptions: RequestCredentials,
        apiConfiguration: ApiConfiguration.State,
        idempotencyKey: String?,
    ): Result<PaymentMethodMessage> {
        return withApiConfiguration(apiConfiguration).retrievePaymentMethodMessage(
            paymentMethods = paymentMethods,
            amount = amount,
            currency = currency,
            country = country,
            locale = locale,
            requestOptions = requestOptions,
            idempotencyKey = idempotencyKey,
        )
    }

    suspend fun retrievePaymentMethodMessagePromotionsForPaymentSheet(
        amount: Int,
        currency: String,
        country: String?,
        locale: String,
        requestOptions: RequestCredentials,
        apiConfiguration: ApiConfiguration.State,
        idempotencyKey: String?,
    ): Result<PaymentMethodMessagePromotionList> {
        return withApiConfiguration(apiConfiguration).retrievePaymentMethodMessagePromotionsForPaymentSheet(
            amount = amount,
            currency = currency,
            country = country,
            locale = locale,
            requestOptions = requestOptions,
            idempotencyKey = idempotencyKey,
        )
    }

    suspend fun retrieveCardMetadata(
        cardNumber: String,
        requestOptions: RequestCredentials,
        apiConfiguration: ApiConfiguration.State,
        idempotencyKey: String?,
    ): Result<CardMetadata> {
        return withApiConfiguration(apiConfiguration).retrieveCardMetadata(
            cardNumber = cardNumber,
            requestOptions = requestOptions,
            idempotencyKey = idempotencyKey,
        )
    }

    suspend fun retrieveCardElementConfig(
        requestOptions: RequestCredentials,
        params: Map<String, String>? = null,
        apiConfiguration: ApiConfiguration.State,
        idempotencyKey: String?,
    ): Result<MobileCardElementConfig> {
        return withApiConfiguration(apiConfiguration).retrieveCardElementConfig(
            requestOptions = requestOptions,
            params = params,
            idempotencyKey = idempotencyKey,
        )
    }

    suspend fun listPaymentDetails(
        clientSecret: String,
        paymentMethodTypes: Set<String>,
        requestOptions: RequestCredentials,
        apiConfiguration: ApiConfiguration.State,
        idempotencyKey: String?,
    ): Result<ConsumerPaymentDetails> {
        return withApiConfiguration(apiConfiguration).listPaymentDetails(
            clientSecret = clientSecret,
            paymentMethodTypes = paymentMethodTypes,
            requestOptions = requestOptions,
            idempotencyKey = idempotencyKey,
        )
    }

    suspend fun listShippingAddresses(
        clientSecret: String,
        requestOptions: RequestCredentials,
        apiConfiguration: ApiConfiguration.State,
        idempotencyKey: String?,
    ): Result<ConsumerShippingAddresses> {
        return withApiConfiguration(apiConfiguration).listShippingAddresses(
            clientSecret = clientSecret,
            requestOptions = requestOptions,
            idempotencyKey = idempotencyKey,
        )
    }

    suspend fun deletePaymentDetails(
        clientSecret: String,
        paymentDetailsId: String,
        requestOptions: RequestCredentials,
        apiConfiguration: ApiConfiguration.State,
        idempotencyKey: String?,
    ): Result<Unit> {
        return withApiConfiguration(apiConfiguration).deletePaymentDetails(
            clientSecret = clientSecret,
            paymentDetailsId = paymentDetailsId,
            requestOptions = requestOptions,
            idempotencyKey = idempotencyKey,
        )
    }

    suspend fun updatePaymentDetails(
        clientSecret: String,
        paymentDetailsUpdateParams: ConsumerPaymentDetailsUpdateParams,
        requestOptions: RequestCredentials,
        apiConfiguration: ApiConfiguration.State,
        idempotencyKey: String?,
    ): Result<ConsumerPaymentDetails> {
        return withApiConfiguration(apiConfiguration).updatePaymentDetails(
            clientSecret = clientSecret,
            paymentDetailsUpdateParams = paymentDetailsUpdateParams,
            requestOptions = requestOptions,
            idempotencyKey = idempotencyKey,
        )
    }

    suspend fun cancelPaymentIntentCaptchaChallenge(
        paymentIntentId: String,
        params: CancelCaptchaChallengeParams,
        requestOptions: RequestCredentials,
        apiConfiguration: ApiConfiguration.State,
        idempotencyKey: String?,
    ): Result<PaymentIntent> {
        return withApiConfiguration(apiConfiguration).cancelPaymentIntentCaptchaChallenge(
            paymentIntentId = paymentIntentId,
            params = params,
            requestOptions = requestOptions,
            idempotencyKey = idempotencyKey,
        )
    }

    suspend fun cancelSetupIntentCaptchaChallenge(
        setupIntentId: String,
        params: CancelCaptchaChallengeParams,
        requestOptions: RequestCredentials,
        apiConfiguration: ApiConfiguration.State,
        idempotencyKey: String?,
    ): Result<SetupIntent> {
        return withApiConfiguration(apiConfiguration).cancelSetupIntentCaptchaChallenge(
            setupIntentId = setupIntentId,
            params = params,
            requestOptions = requestOptions,
            idempotencyKey = idempotencyKey,
        )
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    suspend fun retrieveStripeIntent(
        clientSecret: String,
        options: RequestCredentials,
        expandFields: List<String> = emptyList(),
        idempotencyKey: String?,
    ): Result<StripeIntent>

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    suspend fun confirmPaymentIntent(
        confirmPaymentIntentParams: ConfirmPaymentIntentParams,
        options: RequestCredentials,
        expandFields: List<String> = emptyList(),
        idempotencyKey: String?,
    ): Result<PaymentIntent>

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    suspend fun retrievePaymentIntent(
        clientSecret: String,
        options: RequestCredentials,
        expandFields: List<String> = emptyList(),
        idempotencyKey: String?,
    ): Result<PaymentIntent>

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    suspend fun refreshPaymentIntent(
        clientSecret: String,
        options: RequestCredentials,
        idempotencyKey: String?,
    ): Result<PaymentIntent>

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    suspend fun cancelPaymentIntentSource(
        paymentIntentId: String,
        sourceId: String,
        options: RequestCredentials,
        idempotencyKey: String?,
    ): Result<PaymentIntent>

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    suspend fun confirmSetupIntent(
        confirmSetupIntentParams: ConfirmSetupIntentParams,
        options: RequestCredentials,
        expandFields: List<String> = emptyList(),
        idempotencyKey: String?,
    ): Result<SetupIntent>

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    suspend fun retrieveSetupIntent(
        clientSecret: String,
        options: RequestCredentials,
        expandFields: List<String> = emptyList(),
        idempotencyKey: String?,
    ): Result<SetupIntent>

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    suspend fun refreshSetupIntent(
        clientSecret: String,
        options: RequestCredentials,
        idempotencyKey: String?,
    ): Result<SetupIntent>

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    suspend fun cancelSetupIntentSource(
        setupIntentId: String,
        sourceId: String,
        options: RequestCredentials,
        idempotencyKey: String?,
    ): Result<SetupIntent>

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    suspend fun createSource(
        sourceParams: SourceParams,
        options: RequestCredentials,
        idempotencyKey: String?,
    ): Result<Source>

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    suspend fun retrieveSource(
        sourceId: String,
        clientSecret: String,
        options: RequestCredentials,
        idempotencyKey: String?,
    ): Result<Source>

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    suspend fun createPaymentMethod(
        paymentMethodCreateParams: PaymentMethodCreateParams,
        options: RequestCredentials,
        idempotencyKey: String?,
    ): Result<PaymentMethod>

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    suspend fun updatePaymentMethod(
        paymentMethodId: String,
        paymentMethodUpdateParams: PaymentMethodUpdateParams,
        options: RequestCredentials,
        idempotencyKey: String?,
    ): Result<PaymentMethod>

    /**
     * Set the customer's default payment method.
     *
     * @param customerId Id of the customer to update
     * @param paymentMethodId Id of the payment method to set as the default. If null, the user's existing default
     * payment method will be unset.
     * */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    suspend fun setDefaultPaymentMethod(
        customerId: String,
        paymentMethodId: String?,
        options: RequestCredentials,
        idempotencyKey: String?,
    ): Result<Customer>

    suspend fun retrieveSavedPaymentMethodFromCardPresentPaymentMethod(
        cardPresentPaymentMethodId: String,
        customerId: String,
        options: RequestCredentials,
        idempotencyKey: String?,
    ): Result<PaymentMethod>

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    suspend fun createToken(
        tokenParams: TokenParams,
        options: RequestCredentials,
        idempotencyKey: String?,
    ): Result<Token>

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    suspend fun createConfirmationToken(
        confirmationTokenParams: ConfirmationTokenParams,
        options: RequestCredentials,
        idempotencyKey: String?,
    ): Result<ConfirmationToken>

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    suspend fun addCustomerSource(
        customerId: String,
        publishableKey: String,
        productUsageTokens: Set<String>,
        sourceId: String,
        @Source.SourceType sourceType: String,
        requestOptions: RequestCredentials,
        idempotencyKey: String?,
    ): Result<Source>

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    suspend fun deleteCustomerSource(
        customerId: String,
        publishableKey: String,
        productUsageTokens: Set<String>,
        sourceId: String,
        requestOptions: RequestCredentials,
        idempotencyKey: String?,
    ): Result<Source>

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    suspend fun attachPaymentMethod(
        customerId: String,
        productUsageTokens: Set<String>,
        paymentMethodId: String,
        requestOptions: RequestCredentials,
        idempotencyKey: String?,
    ): Result<PaymentMethod>

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    suspend fun detachPaymentMethod(
        productUsageTokens: Set<String>,
        paymentMethodId: String,
        requestOptions: RequestCredentials,
        idempotencyKey: String?,
    ): Result<PaymentMethod>

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    suspend fun detachPaymentMethod(
        customerSessionClientSecret: String,
        productUsageTokens: Set<String>,
        paymentMethodId: String,
        requestOptions: RequestCredentials,
        idempotencyKey: String?,
    ): Result<PaymentMethod>

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    suspend fun getPaymentMethods(
        listPaymentMethodsParams: ListPaymentMethodsParams,
        productUsageTokens: Set<String>,
        requestOptions: RequestCredentials,
        apiConfiguration: ApiConfiguration.State,
        idempotencyKey: String?,
    ): Result<List<PaymentMethod>>

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    suspend fun retrieveCustomerPaymentMethod(
        customerId: String,
        paymentMethodId: String,
        productUsageTokens: Set<String>,
        requestOptions: RequestCredentials,
        idempotencyKey: String?,
    ): Result<PaymentMethod>

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    suspend fun setDefaultCustomerSource(
        customerId: String,
        publishableKey: String,
        productUsageTokens: Set<String>,
        sourceId: String,
        @Source.SourceType sourceType: String,
        requestOptions: RequestCredentials,
        idempotencyKey: String?,
    ): Result<Customer>

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    suspend fun setCustomerShippingInfo(
        customerId: String,
        publishableKey: String,
        productUsageTokens: Set<String>,
        shippingInformation: ShippingInformation,
        requestOptions: RequestCredentials,
        idempotencyKey: String?,
    ): Result<Customer>

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    suspend fun retrieveCustomer(
        customerId: String,
        productUsageTokens: Set<String>,
        requestOptions: RequestCredentials,
        idempotencyKey: String?,
    ): Result<Customer>

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    suspend fun retrieveIssuingCardPin(
        cardId: String,
        verificationId: String,
        userOneTimeCode: String,
        requestOptions: RequestCredentials,
        idempotencyKey: String?,
    ): Result<String>

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    suspend fun updateIssuingCardPin(
        cardId: String,
        newPin: String,
        verificationId: String,
        userOneTimeCode: String,
        requestOptions: RequestCredentials,
        idempotencyKey: String?,
    ): Throwable?

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    suspend fun getFpxBankStatus(
        options: RequestCredentials,
        idempotencyKey: String?,
    ): Result<BankStatuses>

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    suspend fun getCardMetadata(
        bin: Bin,
        options: RequestCredentials,
        idempotencyKey: String?,
    ): Result<CardMetadata>

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    suspend fun start3ds2Auth(
        authParams: Stripe3ds2AuthParams,
        requestOptions: RequestCredentials,
        idempotencyKey: String?,
    ): Result<Stripe3ds2AuthResult>

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    suspend fun complete3ds2Auth(
        sourceId: String,
        requestOptions: RequestCredentials,
        idempotencyKey: String?,
    ): Result<Stripe3ds2AuthResult>

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    suspend fun createFile(
        fileParams: StripeFileParams,
        requestOptions: RequestCredentials,
        idempotencyKey: String?,
    ): Result<StripeFile>

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    suspend fun retrieveObject(
        url: String,
        requestOptions: RequestCredentials,
        idempotencyKey: String?,
    ): Result<StripeResponse<String>>

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    suspend fun createRadarSession(
        requestOptions: RequestCredentials,
        idempotencyKey: String?,
    ): Result<RadarSessionWithHCaptcha>

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    suspend fun attachHCaptchaToRadarSession(
        radarSessionToken: String,
        hcaptchaToken: String,
        hcaptchaEKey: String?,
        requestOptions: RequestCredentials,
        idempotencyKey: String?,
    ): Result<RadarSessionWithHCaptcha>

    // Link endpoints

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    suspend fun sharePaymentDetails(
        consumerSessionClientSecret: String,
        id: String,
        extraParams: Map<String, *>?,
        requestOptions: RequestCredentials,
        idempotencyKey: String?,
    ): Result<PaymentMethod>

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    suspend fun logOut(
        consumerSessionClientSecret: String,
        consumerAccountPublishableKey: String?,
        requestOptions: RequestCredentials,
        idempotencyKey: String?,
    ): Result<ConsumerSession>

    // ACHv2 endpoints

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    suspend fun createFinancialConnectionsSessionForDeferredPayments(
        params: CreateFinancialConnectionsSessionForDeferredPaymentParams,
        requestOptions: RequestCredentials,
        idempotencyKey: String?,
    ): Result<FinancialConnectionsSession>

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    suspend fun createPaymentIntentFinancialConnectionsSession(
        paymentIntentId: String,
        params: CreateFinancialConnectionsSessionParams,
        requestOptions: RequestCredentials,
        idempotencyKey: String?,
    ): Result<FinancialConnectionsSession>

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    suspend fun createSetupIntentFinancialConnectionsSession(
        setupIntentId: String,
        params: CreateFinancialConnectionsSessionParams,
        requestOptions: RequestCredentials,
        idempotencyKey: String?,
    ): Result<FinancialConnectionsSession>

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    suspend fun attachFinancialConnectionsSessionToPaymentIntent(
        clientSecret: String,
        paymentIntentId: String,
        financialConnectionsSessionId: String,
        requestOptions: RequestCredentials,
        expandFields: List<String>,
        idempotencyKey: String?,
    ): Result<PaymentIntent>

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    suspend fun attachFinancialConnectionsSessionToSetupIntent(
        clientSecret: String,
        setupIntentId: String,
        financialConnectionsSessionId: String,
        requestOptions: RequestCredentials,
        expandFields: List<String>,
        idempotencyKey: String?,
    ): Result<SetupIntent>

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    suspend fun verifyPaymentIntentWithMicrodeposits(
        clientSecret: String,
        firstAmount: Int,
        secondAmount: Int,
        requestOptions: RequestCredentials,
        idempotencyKey: String?,
    ): Result<PaymentIntent>

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    suspend fun verifyPaymentIntentWithMicrodeposits(
        clientSecret: String,
        descriptorCode: String,
        requestOptions: RequestCredentials,
        idempotencyKey: String?,
    ): Result<PaymentIntent>

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    suspend fun verifySetupIntentWithMicrodeposits(
        clientSecret: String,
        firstAmount: Int,
        secondAmount: Int,
        requestOptions: RequestCredentials,
        idempotencyKey: String?,
    ): Result<SetupIntent>

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    suspend fun verifySetupIntentWithMicrodeposits(
        clientSecret: String,
        descriptorCode: String,
        requestOptions: RequestCredentials,
        idempotencyKey: String?,
    ): Result<SetupIntent>

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    suspend fun retrievePaymentMethodMessage(
        paymentMethods: List<String>,
        amount: Int,
        currency: String,
        country: String?,
        locale: String,
        requestOptions: RequestCredentials,
        idempotencyKey: String?,
    ): Result<PaymentMethodMessage>

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    suspend fun retrievePaymentMethodMessagePromotionsForPaymentSheet(
        amount: Int,
        currency: String,
        country: String?,
        locale: String,
        requestOptions: RequestCredentials,
        idempotencyKey: String?,
    ): Result<PaymentMethodMessagePromotionList>

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    suspend fun retrieveCardMetadata(
        cardNumber: String,
        requestOptions: RequestCredentials,
        idempotencyKey: String?,
    ): Result<CardMetadata>

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    suspend fun retrieveCardElementConfig(
        requestOptions: RequestCredentials,
        params: Map<String, String>? = null,
        idempotencyKey: String?,
    ): Result<MobileCardElementConfig>

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    suspend fun listPaymentDetails(
        clientSecret: String,
        paymentMethodTypes: Set<String>,
        requestOptions: RequestCredentials,
        idempotencyKey: String?,
    ): Result<ConsumerPaymentDetails>

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    suspend fun listShippingAddresses(
        clientSecret: String,
        requestOptions: RequestCredentials,
        idempotencyKey: String?,
    ): Result<ConsumerShippingAddresses>

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    suspend fun deletePaymentDetails(
        clientSecret: String,
        paymentDetailsId: String,
        requestOptions: RequestCredentials,
        idempotencyKey: String?,
    ): Result<Unit>

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    suspend fun updatePaymentDetails(
        clientSecret: String,
        paymentDetailsUpdateParams: ConsumerPaymentDetailsUpdateParams,
        requestOptions: RequestCredentials,
        idempotencyKey: String?,
    ): Result<ConsumerPaymentDetails>

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    suspend fun cancelPaymentIntentCaptchaChallenge(
        paymentIntentId: String,
        params: CancelCaptchaChallengeParams,
        requestOptions: RequestCredentials,
        idempotencyKey: String?,
    ): Result<PaymentIntent>

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    suspend fun cancelSetupIntentCaptchaChallenge(
        setupIntentId: String,
        params: CancelCaptchaChallengeParams,
        requestOptions: RequestCredentials,
        idempotencyKey: String?,
    ): Result<SetupIntent>

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun buildPaymentUserAgent(attribution: Set<String> = emptySet()): String

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    companion object {
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        val DEFAULT_REQUEST_SURFACE = RequestSurface.PaymentElement
    }
}
