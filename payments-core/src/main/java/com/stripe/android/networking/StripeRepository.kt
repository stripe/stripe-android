package com.stripe.android.networking

import androidx.annotation.RestrictTo
import com.stripe.android.cards.Bin
import com.stripe.android.core.ApiConfiguration
import com.stripe.android.core.model.StripeFile
import com.stripe.android.core.model.StripeFileParams
import com.stripe.android.core.networking.ApiRequest
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
        options: ApiRequest.Options,
        expandFields: List<String> = emptyList(),
        apiConfiguration: ApiConfiguration.State,
    ): Result<StripeIntent> {
        return withApiConfiguration(apiConfiguration).retrieveStripeIntent(
            clientSecret = clientSecret,
            options = options,
            expandFields = expandFields,
        )
    }

    suspend fun confirmPaymentIntent(
        confirmPaymentIntentParams: ConfirmPaymentIntentParams,
        options: ApiRequest.Options,
        expandFields: List<String> = emptyList(),
        apiConfiguration: ApiConfiguration.State,
    ): Result<PaymentIntent> {
        return withApiConfiguration(apiConfiguration).confirmPaymentIntent(
            confirmPaymentIntentParams = confirmPaymentIntentParams,
            options = options,
            expandFields = expandFields,
        )
    }

    suspend fun retrievePaymentIntent(
        clientSecret: String,
        options: ApiRequest.Options,
        expandFields: List<String> = emptyList(),
        apiConfiguration: ApiConfiguration.State,
    ): Result<PaymentIntent> {
        return withApiConfiguration(apiConfiguration).retrievePaymentIntent(
            clientSecret = clientSecret,
            options = options,
            expandFields = expandFields,
        )
    }

    suspend fun refreshPaymentIntent(
        clientSecret: String,
        options: ApiRequest.Options,
        apiConfiguration: ApiConfiguration.State,
    ): Result<PaymentIntent> {
        return withApiConfiguration(apiConfiguration).refreshPaymentIntent(
            clientSecret = clientSecret,
            options = options,
        )
    }

    suspend fun cancelPaymentIntentSource(
        paymentIntentId: String,
        sourceId: String,
        options: ApiRequest.Options,
        apiConfiguration: ApiConfiguration.State,
    ): Result<PaymentIntent> {
        return withApiConfiguration(apiConfiguration).cancelPaymentIntentSource(
            paymentIntentId = paymentIntentId,
            sourceId = sourceId,
            options = options,
        )
    }

    suspend fun confirmSetupIntent(
        confirmSetupIntentParams: ConfirmSetupIntentParams,
        options: ApiRequest.Options,
        expandFields: List<String> = emptyList(),
        apiConfiguration: ApiConfiguration.State,
    ): Result<SetupIntent> {
        return withApiConfiguration(apiConfiguration).confirmSetupIntent(
            confirmSetupIntentParams = confirmSetupIntentParams,
            options = options,
            expandFields = expandFields,
        )
    }

    suspend fun retrieveSetupIntent(
        clientSecret: String,
        options: ApiRequest.Options,
        expandFields: List<String> = emptyList(),
        apiConfiguration: ApiConfiguration.State,
    ): Result<SetupIntent> {
        return withApiConfiguration(apiConfiguration).retrieveSetupIntent(
            clientSecret = clientSecret,
            options = options,
            expandFields = expandFields,
        )
    }

    suspend fun refreshSetupIntent(
        clientSecret: String,
        options: ApiRequest.Options,
        apiConfiguration: ApiConfiguration.State,
    ): Result<SetupIntent> {
        return withApiConfiguration(apiConfiguration).refreshSetupIntent(clientSecret = clientSecret, options = options)
    }

    suspend fun cancelSetupIntentSource(
        setupIntentId: String,
        sourceId: String,
        options: ApiRequest.Options,
        apiConfiguration: ApiConfiguration.State,
    ): Result<SetupIntent> {
        return withApiConfiguration(apiConfiguration).cancelSetupIntentSource(
            setupIntentId = setupIntentId,
            sourceId = sourceId,
            options = options,
        )
    }

    suspend fun createSource(
        sourceParams: SourceParams,
        options: ApiRequest.Options,
        apiConfiguration: ApiConfiguration.State,
    ): Result<Source> {
        return withApiConfiguration(apiConfiguration).createSource(
            sourceParams = sourceParams,
            options = options,
        )
    }

    suspend fun retrieveSource(
        sourceId: String,
        clientSecret: String,
        options: ApiRequest.Options,
        apiConfiguration: ApiConfiguration.State,
    ): Result<Source> {
        return withApiConfiguration(apiConfiguration).retrieveSource(
            sourceId = sourceId,
            clientSecret = clientSecret,
            options = options,
        )
    }

    suspend fun createPaymentMethod(
        paymentMethodCreateParams: PaymentMethodCreateParams,
        options: ApiRequest.Options,
        apiConfiguration: ApiConfiguration.State,
    ): Result<PaymentMethod> {
        return withApiConfiguration(apiConfiguration).createPaymentMethod(
            paymentMethodCreateParams = paymentMethodCreateParams,
            options = options,
        )
    }

    suspend fun updatePaymentMethod(
        paymentMethodId: String,
        paymentMethodUpdateParams: PaymentMethodUpdateParams,
        options: ApiRequest.Options,
        apiConfiguration: ApiConfiguration.State,
    ): Result<PaymentMethod> {
        return withApiConfiguration(apiConfiguration).updatePaymentMethod(
            paymentMethodId = paymentMethodId,
            paymentMethodUpdateParams = paymentMethodUpdateParams,
            options = options,
        )
    }

    suspend fun setDefaultPaymentMethod(
        customerId: String,
        paymentMethodId: String?,
        options: ApiRequest.Options,
        apiConfiguration: ApiConfiguration.State,
    ): Result<Customer> {
        return withApiConfiguration(apiConfiguration).setDefaultPaymentMethod(
            customerId = customerId,
            paymentMethodId = paymentMethodId,
            options = options,
        )
    }

    suspend fun retrieveSavedPaymentMethodFromCardPresentPaymentMethod(
        cardPresentPaymentMethodId: String,
        customerId: String,
        options: ApiRequest.Options,
        apiConfiguration: ApiConfiguration.State,
    ): Result<PaymentMethod> {
        return withApiConfiguration(apiConfiguration).retrieveSavedPaymentMethodFromCardPresentPaymentMethod(
            cardPresentPaymentMethodId = cardPresentPaymentMethodId,
            customerId = customerId,
            options = options,
        )
    }

    suspend fun createToken(
        tokenParams: TokenParams,
        options: ApiRequest.Options,
        apiConfiguration: ApiConfiguration.State,
    ): Result<Token> {
        return withApiConfiguration(apiConfiguration).createToken(
            tokenParams = tokenParams,
            options = options,
        )
    }

    suspend fun createConfirmationToken(
        confirmationTokenParams: ConfirmationTokenParams,
        options: ApiRequest.Options,
        apiConfiguration: ApiConfiguration.State,
    ): Result<ConfirmationToken> {
        return withApiConfiguration(apiConfiguration).createConfirmationToken(
            confirmationTokenParams = confirmationTokenParams,
            options = options,
        )
    }

    suspend fun addCustomerSource(
        customerId: String,
        publishableKey: String,
        productUsageTokens: Set<String>,
        sourceId: String,
        @Source.SourceType sourceType: String,
        requestOptions: ApiRequest.Options,
        apiConfiguration: ApiConfiguration.State,
    ): Result<Source> {
        return withApiConfiguration(apiConfiguration).addCustomerSource(
            customerId = customerId,
            publishableKey = publishableKey,
            productUsageTokens = productUsageTokens,
            sourceId = sourceId,
            sourceType = sourceType,
            requestOptions = requestOptions,
        )
    }

    suspend fun deleteCustomerSource(
        customerId: String,
        publishableKey: String,
        productUsageTokens: Set<String>,
        sourceId: String,
        requestOptions: ApiRequest.Options,
        apiConfiguration: ApiConfiguration.State,
    ): Result<Source> {
        return withApiConfiguration(apiConfiguration).deleteCustomerSource(
            customerId = customerId,
            publishableKey = publishableKey,
            productUsageTokens = productUsageTokens,
            sourceId = sourceId,
            requestOptions = requestOptions,
        )
    }

    suspend fun attachPaymentMethod(
        customerId: String,
        productUsageTokens: Set<String>,
        paymentMethodId: String,
        requestOptions: ApiRequest.Options,
        apiConfiguration: ApiConfiguration.State,
    ): Result<PaymentMethod> {
        return withApiConfiguration(apiConfiguration).attachPaymentMethod(
            customerId = customerId,
            productUsageTokens = productUsageTokens,
            paymentMethodId = paymentMethodId,
            requestOptions = requestOptions,
        )
    }

    suspend fun detachPaymentMethod(
        productUsageTokens: Set<String>,
        paymentMethodId: String,
        requestOptions: ApiRequest.Options,
        apiConfiguration: ApiConfiguration.State,
    ): Result<PaymentMethod> {
        return withApiConfiguration(apiConfiguration).detachPaymentMethod(
            productUsageTokens = productUsageTokens,
            paymentMethodId = paymentMethodId,
            requestOptions = requestOptions,
        )
    }

    suspend fun detachPaymentMethod(
        customerSessionClientSecret: String,
        productUsageTokens: Set<String>,
        paymentMethodId: String,
        requestOptions: ApiRequest.Options,
        apiConfiguration: ApiConfiguration.State,
    ): Result<PaymentMethod> {
        return withApiConfiguration(apiConfiguration).detachPaymentMethod(
            customerSessionClientSecret = customerSessionClientSecret,
            productUsageTokens = productUsageTokens,
            paymentMethodId = paymentMethodId,
            requestOptions = requestOptions,
        )
    }

    suspend fun retrieveCustomerPaymentMethod(
        customerId: String,
        paymentMethodId: String,
        productUsageTokens: Set<String>,
        requestOptions: ApiRequest.Options,
        apiConfiguration: ApiConfiguration.State,
    ): Result<PaymentMethod> {
        return withApiConfiguration(apiConfiguration).retrieveCustomerPaymentMethod(
            customerId = customerId,
            paymentMethodId = paymentMethodId,
            productUsageTokens = productUsageTokens,
            requestOptions = requestOptions,
        )
    }

    suspend fun setDefaultCustomerSource(
        customerId: String,
        publishableKey: String,
        productUsageTokens: Set<String>,
        sourceId: String,
        @Source.SourceType sourceType: String,
        requestOptions: ApiRequest.Options,
        apiConfiguration: ApiConfiguration.State,
    ): Result<Customer> {
        return withApiConfiguration(apiConfiguration).setDefaultCustomerSource(
            customerId = customerId,
            publishableKey = publishableKey,
            productUsageTokens = productUsageTokens,
            sourceId = sourceId,
            sourceType = sourceType,
            requestOptions = requestOptions,
        )
    }

    suspend fun setCustomerShippingInfo(
        customerId: String,
        publishableKey: String,
        productUsageTokens: Set<String>,
        shippingInformation: ShippingInformation,
        requestOptions: ApiRequest.Options,
        apiConfiguration: ApiConfiguration.State,
    ): Result<Customer> {
        return withApiConfiguration(apiConfiguration).setCustomerShippingInfo(
            customerId = customerId,
            publishableKey = publishableKey,
            productUsageTokens = productUsageTokens,
            shippingInformation = shippingInformation,
            requestOptions = requestOptions,
        )
    }

    suspend fun retrieveCustomer(
        customerId: String,
        productUsageTokens: Set<String>,
        requestOptions: ApiRequest.Options,
        apiConfiguration: ApiConfiguration.State,
    ): Result<Customer> {
        return withApiConfiguration(apiConfiguration).retrieveCustomer(
            customerId = customerId,
            productUsageTokens = productUsageTokens,
            requestOptions = requestOptions,
        )
    }

    suspend fun retrieveIssuingCardPin(
        cardId: String,
        verificationId: String,
        userOneTimeCode: String,
        requestOptions: ApiRequest.Options,
        apiConfiguration: ApiConfiguration.State,
    ): Result<String> {
        return withApiConfiguration(apiConfiguration).retrieveIssuingCardPin(
            cardId = cardId,
            verificationId = verificationId,
            userOneTimeCode = userOneTimeCode,
            requestOptions = requestOptions,
        )
    }

    suspend fun updateIssuingCardPin(
        cardId: String,
        newPin: String,
        verificationId: String,
        userOneTimeCode: String,
        requestOptions: ApiRequest.Options,
        apiConfiguration: ApiConfiguration.State,
    ): Throwable? {
        return withApiConfiguration(apiConfiguration).updateIssuingCardPin(
            cardId = cardId,
            newPin = newPin,
            verificationId = verificationId,
            userOneTimeCode = userOneTimeCode,
            requestOptions = requestOptions,
        )
    }

    suspend fun getFpxBankStatus(
        options: ApiRequest.Options,
        apiConfiguration: ApiConfiguration.State,
    ): Result<BankStatuses> {
        return withApiConfiguration(apiConfiguration).getFpxBankStatus(
            options = options,
        )
    }

    suspend fun getCardMetadata(
        bin: Bin,
        options: ApiRequest.Options,
        apiConfiguration: ApiConfiguration.State,
    ): Result<CardMetadata> {
        return withApiConfiguration(apiConfiguration).getCardMetadata(
            bin = bin,
            options = options,
        )
    }

    suspend fun start3ds2Auth(
        authParams: Stripe3ds2AuthParams,
        requestOptions: ApiRequest.Options,
        apiConfiguration: ApiConfiguration.State,
    ): Result<Stripe3ds2AuthResult> {
        return withApiConfiguration(apiConfiguration).start3ds2Auth(
            authParams = authParams,
            requestOptions = requestOptions,
        )
    }

    suspend fun complete3ds2Auth(
        sourceId: String,
        requestOptions: ApiRequest.Options,
        apiConfiguration: ApiConfiguration.State,
    ): Result<Stripe3ds2AuthResult> {
        return withApiConfiguration(apiConfiguration).complete3ds2Auth(
            sourceId = sourceId,
            requestOptions = requestOptions,
        )
    }

    suspend fun createFile(
        fileParams: StripeFileParams,
        requestOptions: ApiRequest.Options,
        apiConfiguration: ApiConfiguration.State,
    ): Result<StripeFile> {
        return withApiConfiguration(apiConfiguration).createFile(
            fileParams = fileParams,
            requestOptions = requestOptions,
        )
    }

    suspend fun retrieveObject(
        url: String,
        requestOptions: ApiRequest.Options,
        apiConfiguration: ApiConfiguration.State,
    ): Result<StripeResponse<String>> {
        return withApiConfiguration(apiConfiguration).retrieveObject(
            url = url,
            requestOptions = requestOptions,
        )
    }

    suspend fun createRadarSession(
        requestOptions: ApiRequest.Options,
        apiConfiguration: ApiConfiguration.State,
    ): Result<RadarSessionWithHCaptcha> {
        return withApiConfiguration(apiConfiguration).createRadarSession(
            requestOptions = requestOptions,
        )
    }

    suspend fun attachHCaptchaToRadarSession(
        radarSessionToken: String,
        hcaptchaToken: String,
        hcaptchaEKey: String?,
        requestOptions: ApiRequest.Options,
        apiConfiguration: ApiConfiguration.State,
    ): Result<RadarSessionWithHCaptcha> {
        return withApiConfiguration(apiConfiguration).attachHCaptchaToRadarSession(
            radarSessionToken = radarSessionToken,
            hcaptchaToken = hcaptchaToken,
            hcaptchaEKey = hcaptchaEKey,
            requestOptions = requestOptions,
        )
    }

    suspend fun sharePaymentDetails(
        consumerSessionClientSecret: String,
        id: String,
        extraParams: Map<String, *>?,
        requestOptions: ApiRequest.Options,
        apiConfiguration: ApiConfiguration.State,
    ): Result<PaymentMethod> {
        return withApiConfiguration(apiConfiguration).sharePaymentDetails(
            consumerSessionClientSecret = consumerSessionClientSecret,
            id = id,
            extraParams = extraParams,
            requestOptions = requestOptions,
        )
    }

    suspend fun logOut(
        consumerSessionClientSecret: String,
        consumerAccountPublishableKey: String?,
        requestOptions: ApiRequest.Options,
        apiConfiguration: ApiConfiguration.State,
    ): Result<ConsumerSession> {
        return withApiConfiguration(apiConfiguration).logOut(
            consumerSessionClientSecret = consumerSessionClientSecret,
            consumerAccountPublishableKey = consumerAccountPublishableKey,
            requestOptions = requestOptions,
        )
    }

    suspend fun createFinancialConnectionsSessionForDeferredPayments(
        params: CreateFinancialConnectionsSessionForDeferredPaymentParams,
        requestOptions: ApiRequest.Options,
        apiConfiguration: ApiConfiguration.State,
    ): Result<FinancialConnectionsSession> {
        return withApiConfiguration(apiConfiguration).createFinancialConnectionsSessionForDeferredPayments(
            params = params,
            requestOptions = requestOptions,
        )
    }

    suspend fun createPaymentIntentFinancialConnectionsSession(
        paymentIntentId: String,
        params: CreateFinancialConnectionsSessionParams,
        requestOptions: ApiRequest.Options,
        apiConfiguration: ApiConfiguration.State,
    ): Result<FinancialConnectionsSession> {
        return withApiConfiguration(apiConfiguration).createPaymentIntentFinancialConnectionsSession(
            paymentIntentId = paymentIntentId,
            params = params,
            requestOptions = requestOptions,
        )
    }

    suspend fun createSetupIntentFinancialConnectionsSession(
        setupIntentId: String,
        params: CreateFinancialConnectionsSessionParams,
        requestOptions: ApiRequest.Options,
        apiConfiguration: ApiConfiguration.State,
    ): Result<FinancialConnectionsSession> {
        return withApiConfiguration(apiConfiguration).createSetupIntentFinancialConnectionsSession(
            setupIntentId = setupIntentId,
            params = params,
            requestOptions = requestOptions,
        )
    }

    suspend fun attachFinancialConnectionsSessionToPaymentIntent(
        clientSecret: String,
        paymentIntentId: String,
        financialConnectionsSessionId: String,
        requestOptions: ApiRequest.Options,
        expandFields: List<String>,
        apiConfiguration: ApiConfiguration.State,
    ): Result<PaymentIntent> {
        return withApiConfiguration(apiConfiguration).attachFinancialConnectionsSessionToPaymentIntent(
            clientSecret = clientSecret,
            paymentIntentId = paymentIntentId,
            financialConnectionsSessionId = financialConnectionsSessionId,
            requestOptions = requestOptions,
            expandFields = expandFields,
        )
    }

    suspend fun attachFinancialConnectionsSessionToSetupIntent(
        clientSecret: String,
        setupIntentId: String,
        financialConnectionsSessionId: String,
        requestOptions: ApiRequest.Options,
        expandFields: List<String>,
        apiConfiguration: ApiConfiguration.State,
    ): Result<SetupIntent> {
        return withApiConfiguration(apiConfiguration).attachFinancialConnectionsSessionToSetupIntent(
            clientSecret = clientSecret,
            setupIntentId = setupIntentId,
            financialConnectionsSessionId = financialConnectionsSessionId,
            requestOptions = requestOptions,
            expandFields = expandFields,
        )
    }

    suspend fun verifyPaymentIntentWithMicrodeposits(
        clientSecret: String,
        firstAmount: Int,
        secondAmount: Int,
        requestOptions: ApiRequest.Options,
        apiConfiguration: ApiConfiguration.State,
    ): Result<PaymentIntent> {
        return withApiConfiguration(apiConfiguration).verifyPaymentIntentWithMicrodeposits(
            clientSecret = clientSecret,
            firstAmount = firstAmount,
            secondAmount = secondAmount,
            requestOptions = requestOptions,
        )
    }

    suspend fun verifyPaymentIntentWithMicrodeposits(
        clientSecret: String,
        descriptorCode: String,
        requestOptions: ApiRequest.Options,
        apiConfiguration: ApiConfiguration.State,
    ): Result<PaymentIntent> {
        return withApiConfiguration(apiConfiguration).verifyPaymentIntentWithMicrodeposits(
            clientSecret = clientSecret,
            descriptorCode = descriptorCode,
            requestOptions = requestOptions,
        )
    }

    suspend fun verifySetupIntentWithMicrodeposits(
        clientSecret: String,
        firstAmount: Int,
        secondAmount: Int,
        requestOptions: ApiRequest.Options,
        apiConfiguration: ApiConfiguration.State,
    ): Result<SetupIntent> {
        return withApiConfiguration(apiConfiguration).verifySetupIntentWithMicrodeposits(
            clientSecret = clientSecret,
            firstAmount = firstAmount,
            secondAmount = secondAmount,
            requestOptions = requestOptions,
        )
    }

    suspend fun verifySetupIntentWithMicrodeposits(
        clientSecret: String,
        descriptorCode: String,
        requestOptions: ApiRequest.Options,
        apiConfiguration: ApiConfiguration.State,
    ): Result<SetupIntent> {
        return withApiConfiguration(apiConfiguration).verifySetupIntentWithMicrodeposits(
            clientSecret = clientSecret,
            descriptorCode = descriptorCode,
            requestOptions = requestOptions,
        )
    }

    suspend fun retrievePaymentMethodMessage(
        paymentMethods: List<String>,
        amount: Int,
        currency: String,
        country: String?,
        locale: String,
        requestOptions: ApiRequest.Options,
        apiConfiguration: ApiConfiguration.State,
    ): Result<PaymentMethodMessage> {
        return withApiConfiguration(apiConfiguration).retrievePaymentMethodMessage(
            paymentMethods = paymentMethods,
            amount = amount,
            currency = currency,
            country = country,
            locale = locale,
            requestOptions = requestOptions,
        )
    }

    suspend fun retrievePaymentMethodMessagePromotionsForPaymentSheet(
        amount: Int,
        currency: String,
        country: String?,
        locale: String,
        requestOptions: ApiRequest.Options,
        apiConfiguration: ApiConfiguration.State,
    ): Result<PaymentMethodMessagePromotionList> {
        return withApiConfiguration(apiConfiguration).retrievePaymentMethodMessagePromotionsForPaymentSheet(
            amount = amount,
            currency = currency,
            country = country,
            locale = locale,
            requestOptions = requestOptions,
        )
    }

    suspend fun retrieveCardMetadata(
        cardNumber: String,
        requestOptions: ApiRequest.Options,
        apiConfiguration: ApiConfiguration.State,
    ): Result<CardMetadata> {
        return withApiConfiguration(apiConfiguration).retrieveCardMetadata(
            cardNumber = cardNumber,
            requestOptions = requestOptions,
        )
    }

    suspend fun retrieveCardElementConfig(
        requestOptions: ApiRequest.Options,
        params: Map<String, String>? = null,
        apiConfiguration: ApiConfiguration.State,
    ): Result<MobileCardElementConfig> {
        return withApiConfiguration(apiConfiguration).retrieveCardElementConfig(
            requestOptions = requestOptions,
            params = params,
        )
    }

    suspend fun listPaymentDetails(
        clientSecret: String,
        paymentMethodTypes: Set<String>,
        requestOptions: ApiRequest.Options,
        apiConfiguration: ApiConfiguration.State,
    ): Result<ConsumerPaymentDetails> {
        return withApiConfiguration(apiConfiguration).listPaymentDetails(
            clientSecret = clientSecret,
            paymentMethodTypes = paymentMethodTypes,
            requestOptions = requestOptions,
        )
    }

    suspend fun listShippingAddresses(
        clientSecret: String,
        requestOptions: ApiRequest.Options,
        apiConfiguration: ApiConfiguration.State,
    ): Result<ConsumerShippingAddresses> {
        return withApiConfiguration(apiConfiguration).listShippingAddresses(
            clientSecret = clientSecret,
            requestOptions = requestOptions,
        )
    }

    suspend fun deletePaymentDetails(
        clientSecret: String,
        paymentDetailsId: String,
        requestOptions: ApiRequest.Options,
        apiConfiguration: ApiConfiguration.State,
    ): Result<Unit> {
        return withApiConfiguration(apiConfiguration).deletePaymentDetails(
            clientSecret = clientSecret,
            paymentDetailsId = paymentDetailsId,
            requestOptions = requestOptions,
        )
    }

    suspend fun updatePaymentDetails(
        clientSecret: String,
        paymentDetailsUpdateParams: ConsumerPaymentDetailsUpdateParams,
        requestOptions: ApiRequest.Options,
        apiConfiguration: ApiConfiguration.State,
    ): Result<ConsumerPaymentDetails> {
        return withApiConfiguration(apiConfiguration).updatePaymentDetails(
            clientSecret = clientSecret,
            paymentDetailsUpdateParams = paymentDetailsUpdateParams,
            requestOptions = requestOptions,
        )
    }

    suspend fun cancelPaymentIntentCaptchaChallenge(
        paymentIntentId: String,
        params: CancelCaptchaChallengeParams,
        requestOptions: ApiRequest.Options,
        apiConfiguration: ApiConfiguration.State,
    ): Result<PaymentIntent> {
        return withApiConfiguration(apiConfiguration).cancelPaymentIntentCaptchaChallenge(
            paymentIntentId = paymentIntentId,
            params = params,
            requestOptions = requestOptions,
        )
    }

    suspend fun cancelSetupIntentCaptchaChallenge(
        setupIntentId: String,
        params: CancelCaptchaChallengeParams,
        requestOptions: ApiRequest.Options,
        apiConfiguration: ApiConfiguration.State,
    ): Result<SetupIntent> {
        return withApiConfiguration(apiConfiguration).cancelSetupIntentCaptchaChallenge(
            setupIntentId = setupIntentId,
            params = params,
            requestOptions = requestOptions,
        )
    }

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
    suspend fun refreshPaymentIntent(clientSecret: String, options: ApiRequest.Options): Result<PaymentIntent>

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
    suspend fun refreshSetupIntent(clientSecret: String, options: ApiRequest.Options): Result<SetupIntent>

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
    suspend fun updatePaymentMethod(
        paymentMethodId: String,
        paymentMethodUpdateParams: PaymentMethodUpdateParams,
        options: ApiRequest.Options
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
        options: ApiRequest.Options,
    ): Result<Customer>

    suspend fun retrieveSavedPaymentMethodFromCardPresentPaymentMethod(
        cardPresentPaymentMethodId: String,
        customerId: String,
        options: ApiRequest.Options,
    ): Result<PaymentMethod>

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    suspend fun createToken(
        tokenParams: TokenParams,
        options: ApiRequest.Options
    ): Result<Token>

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    suspend fun createConfirmationToken(
        confirmationTokenParams: ConfirmationTokenParams,
        options: ApiRequest.Options
    ): Result<ConfirmationToken>

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
        productUsageTokens: Set<String>,
        paymentMethodId: String,
        requestOptions: ApiRequest.Options
    ): Result<PaymentMethod>

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    suspend fun detachPaymentMethod(
        productUsageTokens: Set<String>,
        paymentMethodId: String,
        requestOptions: ApiRequest.Options
    ): Result<PaymentMethod>

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    suspend fun detachPaymentMethod(
        customerSessionClientSecret: String,
        productUsageTokens: Set<String>,
        paymentMethodId: String,
        requestOptions: ApiRequest.Options
    ): Result<PaymentMethod>

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    suspend fun getPaymentMethods(
        listPaymentMethodsParams: ListPaymentMethodsParams,
        productUsageTokens: Set<String>,
        requestOptions: ApiRequest.Options,
        apiConfiguration: ApiConfiguration.State,
    ): Result<List<PaymentMethod>>

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    suspend fun retrieveCustomerPaymentMethod(
        customerId: String,
        paymentMethodId: String,
        productUsageTokens: Set<String>,
        requestOptions: ApiRequest.Options
    ): Result<PaymentMethod>

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
    suspend fun getFpxBankStatus(
        options: ApiRequest.Options
    ): Result<BankStatuses>

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
    ): Result<RadarSessionWithHCaptcha>

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    suspend fun attachHCaptchaToRadarSession(
        radarSessionToken: String,
        hcaptchaToken: String,
        hcaptchaEKey: String?,
        requestOptions: ApiRequest.Options
    ): Result<RadarSessionWithHCaptcha>

    // Link endpoints

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    suspend fun sharePaymentDetails(
        consumerSessionClientSecret: String,
        id: String,
        extraParams: Map<String, *>?,
        requestOptions: ApiRequest.Options
    ): Result<PaymentMethod>

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    suspend fun logOut(
        consumerSessionClientSecret: String,
        consumerAccountPublishableKey: String?,
        requestOptions: ApiRequest.Options,
    ): Result<ConsumerSession>

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
        country: String?,
        locale: String,
        requestOptions: ApiRequest.Options
    ): Result<PaymentMethodMessage>

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    suspend fun retrievePaymentMethodMessagePromotionsForPaymentSheet(
        amount: Int,
        currency: String,
        country: String?,
        locale: String,
        requestOptions: ApiRequest.Options
    ): Result<PaymentMethodMessagePromotionList>

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    suspend fun retrieveCardMetadata(
        cardNumber: String,
        requestOptions: ApiRequest.Options
    ): Result<CardMetadata>

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    suspend fun retrieveCardElementConfig(
        requestOptions: ApiRequest.Options,
        params: Map<String, String>? = null
    ): Result<MobileCardElementConfig>

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    suspend fun listPaymentDetails(
        clientSecret: String,
        paymentMethodTypes: Set<String>,
        requestOptions: ApiRequest.Options
    ): Result<ConsumerPaymentDetails>

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    suspend fun listShippingAddresses(
        clientSecret: String,
        requestOptions: ApiRequest.Options
    ): Result<ConsumerShippingAddresses>

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    suspend fun deletePaymentDetails(
        clientSecret: String,
        paymentDetailsId: String,
        requestOptions: ApiRequest.Options
    ): Result<Unit>

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    suspend fun updatePaymentDetails(
        clientSecret: String,
        paymentDetailsUpdateParams: ConsumerPaymentDetailsUpdateParams,
        requestOptions: ApiRequest.Options
    ): Result<ConsumerPaymentDetails>

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    suspend fun cancelPaymentIntentCaptchaChallenge(
        paymentIntentId: String,
        params: CancelCaptchaChallengeParams,
        requestOptions: ApiRequest.Options
    ): Result<PaymentIntent>

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    suspend fun cancelSetupIntentCaptchaChallenge(
        setupIntentId: String,
        params: CancelCaptchaChallengeParams,
        requestOptions: ApiRequest.Options
    ): Result<SetupIntent>

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun buildPaymentUserAgent(attribution: Set<String> = emptySet()): String

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    companion object {
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        val DEFAULT_REQUEST_SURFACE = RequestSurface.PaymentElement
    }
}
