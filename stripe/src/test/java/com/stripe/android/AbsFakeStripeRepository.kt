package com.stripe.android

import com.stripe.android.cards.Bin
import com.stripe.android.exception.APIException
import com.stripe.android.model.BinFixtures
import com.stripe.android.model.CardMetadata
import com.stripe.android.model.Complete3ds2Result
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.ConfirmSetupIntentParams
import com.stripe.android.model.Customer
import com.stripe.android.model.FpxBankStatuses
import com.stripe.android.model.ListPaymentMethodsParams
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.SetupIntent
import com.stripe.android.model.ShippingInformation
import com.stripe.android.model.Source
import com.stripe.android.model.SourceParams
import com.stripe.android.model.Stripe3ds2AuthParams
import com.stripe.android.model.Stripe3ds2AuthResultFixtures
import com.stripe.android.model.StripeFile
import com.stripe.android.model.StripeFileParams
import com.stripe.android.model.StripeIntent
import com.stripe.android.model.Token
import com.stripe.android.model.TokenParams
import org.json.JSONObject

internal abstract class AbsFakeStripeRepository : StripeRepository {

    override fun confirmPaymentIntent(
        confirmPaymentIntentParams: ConfirmPaymentIntentParams,
        options: ApiRequest.Options,
        expandFields: List<String>
    ): PaymentIntent? {
        return null
    }

    override fun retrievePaymentIntent(
        clientSecret: String,
        options: ApiRequest.Options,
        expandFields: List<String>
    ): PaymentIntent? {
        return null
    }

    override fun cancelPaymentIntentSource(
        paymentIntentId: String,
        sourceId: String,
        options: ApiRequest.Options
    ): PaymentIntent? {
        return null
    }

    override fun confirmSetupIntent(
        confirmSetupIntentParams: ConfirmSetupIntentParams,
        options: ApiRequest.Options,
        expandFields: List<String>
    ): SetupIntent? {
        return null
    }

    override fun retrieveSetupIntent(
        clientSecret: String,
        options: ApiRequest.Options,
        expandFields: List<String>
    ): SetupIntent? {
        return null
    }

    override fun cancelSetupIntentSource(
        setupIntentId: String,
        sourceId: String,
        options: ApiRequest.Options
    ): SetupIntent? {
        return null
    }

    override fun retrieveIntent(
        clientSecret: String,
        options: ApiRequest.Options,
        expandFields: List<String>,
        callback: ApiResultCallback<StripeIntent>
    ) {
    }

    override fun cancelIntent(
        stripeIntent: StripeIntent,
        sourceId: String,
        options: ApiRequest.Options,
        callback: ApiResultCallback<StripeIntent>
    ) {
    }

    override fun createSource(
        sourceParams: SourceParams,
        options: ApiRequest.Options
    ): Source? {
        return null
    }

    override fun retrieveSource(
        sourceId: String,
        clientSecret: String,
        options: ApiRequest.Options
    ): Source? {
        return null
    }

    override fun retrieveSource(
        sourceId: String,
        clientSecret: String,
        options: ApiRequest.Options,
        callback: ApiResultCallback<Source>
    ) {
    }

    override fun createPaymentMethod(
        paymentMethodCreateParams: PaymentMethodCreateParams,
        options: ApiRequest.Options
    ): PaymentMethod? {
        return null
    }

    override fun createToken(
        tokenParams: TokenParams,
        options: ApiRequest.Options
    ): Token? {
        return null
    }

    @Throws(APIException::class)
    override fun addCustomerSource(
        customerId: String,
        publishableKey: String,
        productUsageTokens: Set<String>,
        sourceId: String,
        sourceType: String,
        requestOptions: ApiRequest.Options
    ): Source? {
        return null
    }

    @Throws(APIException::class)
    override fun deleteCustomerSource(
        customerId: String,
        publishableKey: String,
        productUsageTokens: Set<String>,
        sourceId: String,
        requestOptions: ApiRequest.Options
    ): Source? {
        return null
    }

    @Throws(APIException::class)
    override fun attachPaymentMethod(
        customerId: String,
        publishableKey: String,
        productUsageTokens: Set<String>,
        paymentMethodId: String,
        requestOptions: ApiRequest.Options
    ): PaymentMethod? {
        return null
    }

    @Throws(APIException::class)
    override fun detachPaymentMethod(
        publishableKey: String,
        productUsageTokens: Set<String>,
        paymentMethodId: String,
        requestOptions: ApiRequest.Options
    ): PaymentMethod? {
        return null
    }

    @Throws(APIException::class)
    override fun getPaymentMethods(
        listPaymentMethodsParams: ListPaymentMethodsParams,
        publishableKey: String,
        productUsageTokens: Set<String>,
        requestOptions: ApiRequest.Options
    ): List<PaymentMethod> {
        return emptyList()
    }

    @Throws(APIException::class)
    override fun setDefaultCustomerSource(
        customerId: String,
        publishableKey: String,
        productUsageTokens: Set<String>,
        sourceId: String,
        sourceType: String,
        requestOptions: ApiRequest.Options
    ): Customer? {
        return null
    }

    override fun setCustomerShippingInfo(
        customerId: String,
        publishableKey: String,
        productUsageTokens: Set<String>,
        shippingInformation: ShippingInformation,
        requestOptions: ApiRequest.Options
    ): Customer? {
        return null
    }

    override fun retrieveCustomer(
        customerId: String,
        productUsageTokens: Set<String>,
        requestOptions: ApiRequest.Options
    ): Customer? {
        return null
    }

    override fun retrieveIssuingCardPin(
        cardId: String,
        verificationId: String,
        userOneTimeCode: String,
        ephemeralKeySecret: String
    ): String {
        return ""
    }

    override fun updateIssuingCardPin(
        cardId: String,
        newPin: String,
        verificationId: String,
        userOneTimeCode: String,
        ephemeralKeySecret: String
    ) {
    }

    override suspend fun getFpxBankStatus(
        options: ApiRequest.Options
    ) = FpxBankStatuses()

    override suspend fun getCardMetadata(bin: Bin, options: ApiRequest.Options) =
        CardMetadata(
            BinFixtures.VISA,
            emptyList()
        )

    override suspend fun start3ds2Auth(
        authParams: Stripe3ds2AuthParams,
        stripeIntentId: String,
        requestOptions: ApiRequest.Options
    ) = Stripe3ds2AuthResultFixtures.ARES_CHALLENGE_FLOW

    override suspend fun complete3ds2Auth(
        sourceId: String,
        requestOptions: ApiRequest.Options
    ): Complete3ds2Result = Complete3ds2Result(true)

    override fun createFile(
        fileParams: StripeFileParams,
        requestOptions: ApiRequest.Options
    ): StripeFile {
        return StripeFile()
    }

    override fun retrieveObject(url: String, requestOptions: ApiRequest.Options): JSONObject {
        return JSONObject()
    }
}
