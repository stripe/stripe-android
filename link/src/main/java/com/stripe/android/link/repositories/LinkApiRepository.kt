package com.stripe.android.link.repositories

import com.stripe.android.core.injection.IOContext
import com.stripe.android.core.injection.PUBLISHABLE_KEY
import com.stripe.android.core.injection.STRIPE_ACCOUNT_ID
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.link.LinkPaymentDetails
import com.stripe.android.link.injection.LinkScope
import com.stripe.android.model.ConsumerPaymentDetailsCreateParams
import com.stripe.android.model.ConsumerPaymentDetailsCreateParams.Card.Companion.extraConfirmationParams
import com.stripe.android.model.ConsumerSession
import com.stripe.android.model.ConsumerSessionLookup
import com.stripe.android.model.ConsumerSignUpConsentAction
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.StripeIntent
import com.stripe.android.networking.StripeRepository
import com.stripe.android.repository.ConsumersApiService
import kotlinx.coroutines.withContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Named
import kotlin.coroutines.CoroutineContext

/**
 * Repository that uses [StripeRepository] for Link services.
 */
@LinkScope
internal class LinkApiRepository @Inject constructor(
    @Named(PUBLISHABLE_KEY) private val publishableKeyProvider: () -> String,
    @Named(STRIPE_ACCOUNT_ID) private val stripeAccountIdProvider: () -> String?,
    private val stripeRepository: StripeRepository,
    private val consumersApiService: ConsumersApiService,
    @IOContext private val workContext: CoroutineContext,
    private val locale: Locale?
) : LinkRepository {

    override suspend fun lookupConsumer(
        email: String?,
        authSessionCookie: String?
    ): Result<ConsumerSessionLookup> = withContext(workContext) {
        runCatching {
            requireNotNull(
                consumersApiService.lookupConsumerSession(
                    email = email,
                    authSessionCookie = authSessionCookie,
                    requestSurface = REQUEST_SURFACE,
                    requestOptions = ApiRequest.Options(
                        publishableKeyProvider(),
                        stripeAccountIdProvider()
                    )
                )
            )
        }
    }

    override suspend fun consumerSignUp(
        email: String,
        phone: String,
        country: String,
        name: String?,
        authSessionCookie: String?,
        consentAction: ConsumerSignUpConsentAction
    ): Result<ConsumerSession> = withContext(workContext) {
        stripeRepository.consumerSignUp(
            email,
            phone,
            country,
            name,
            locale,
            authSessionCookie,
            consentAction,
            ApiRequest.Options(
                publishableKeyProvider(),
                stripeAccountIdProvider()
            )
        )
    }

    override suspend fun createCardPaymentDetails(
        paymentMethodCreateParams: PaymentMethodCreateParams,
        userEmail: String,
        stripeIntent: StripeIntent,
        consumerSessionClientSecret: String,
        consumerPublishableKey: String?
    ): Result<LinkPaymentDetails.New> = withContext(workContext) {
        stripeRepository.createPaymentDetails(
            consumerSessionClientSecret = consumerSessionClientSecret,
            paymentDetailsCreateParams = ConsumerPaymentDetailsCreateParams.Card(
                cardPaymentMethodCreateParamsMap = paymentMethodCreateParams.toParamMap(),
                email = userEmail,
            ),
            requestOptions = consumerPublishableKey?.let {
                ApiRequest.Options(it)
            } ?: ApiRequest.Options(
                apiKey = publishableKeyProvider(),
                stripeAccount = stripeAccountIdProvider(),
            ),
        ).mapCatching {
            val paymentDetails = it.paymentDetails.first()
            val extraParams = extraConfirmationParams(paymentMethodCreateParams)

            val createParams = PaymentMethodCreateParams.createLink(
                paymentDetailsId = paymentDetails.id,
                consumerSessionClientSecret = consumerSessionClientSecret,
                extraParams = extraParams,
            )

            LinkPaymentDetails.New(
                paymentDetails = paymentDetails,
                paymentMethodCreateParams = createParams,
                originalParams = paymentMethodCreateParams,
            )
        }
    }

    private companion object {
        const val REQUEST_SURFACE = "android_payment_element"
    }
}
