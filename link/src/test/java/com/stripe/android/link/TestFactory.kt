package com.stripe.android.link

import com.stripe.android.model.CardParams
import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.model.ConsumerSession
import com.stripe.android.model.ConsumerSessionLookup
import com.stripe.android.model.ConsumerSessionSignup
import com.stripe.android.model.PaymentMethodCreateParams
import org.mockito.kotlin.mock

object TestFactory {

    fun consumerSessionLookUp(
        publishableKey: String,
        exists: Boolean
    ): ConsumerSessionLookup {
        return CONSUMER_SESSION_LOOKUP.copy(
            publishableKey = publishableKey,
            exists = exists
        )
    }

    const val EMAIL = "email@stripe.com"
    const val CLIENT_SECRET = "client_secret"
    const val PUBLISHABLE_KEY = "publishable_key"

    val VERIFIED_SESSION = ConsumerSession.VerificationSession(
        type = ConsumerSession.VerificationSession.SessionType.Sms,
        state = ConsumerSession.VerificationSession.SessionState.Verified
    )

    val CONSUMER_SESSION = ConsumerSession(
        emailAddress = EMAIL,
        clientSecret = CLIENT_SECRET,
        verificationSessions = listOf(VERIFIED_SESSION),
        redactedPhoneNumber = "+1********42",
        redactedFormattedPhoneNumber = "+1 (***) ***-**42",
    )

    val CONSUMER_SESSION_LOOKUP = ConsumerSessionLookup(
        exists = true,
        consumerSession = CONSUMER_SESSION,
        errorMessage = null,
        publishableKey = PUBLISHABLE_KEY
    )

    val CONSUMER_SESSION_SIGN_UP = ConsumerSessionSignup(
        consumerSession = CONSUMER_SESSION,
        publishableKey = PUBLISHABLE_KEY
    )

    val PAYMENT_METHOD_CREATE_PARAMS = PaymentMethodCreateParams.createCard(
        CardParams(
            number = "4242424242424242",
            expMonth = 1,
            expYear = 27,
            cvc = "123",
        )
    )

    val LINK_NEW_PAYMENT_DETAILS = LinkPaymentDetails.New(
        paymentDetails = ConsumerPaymentDetails.Card(
            id = "pm_123",
            last4 = "4242",
        ),
        paymentMethodCreateParams = PAYMENT_METHOD_CREATE_PARAMS,
        originalParams = mock()
    )

    val CONSUMER_PAYMENT_DETAILS: ConsumerPaymentDetails = ConsumerPaymentDetails(
        paymentDetails = listOf(
            ConsumerPaymentDetails.Card(
                id = "pm_123",
                last4 = "4242",
            )
        )
    )
}
