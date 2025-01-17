package com.stripe.android.link.account

import com.stripe.android.link.LinkPaymentDetails
import com.stripe.android.link.model.AccountStatus
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.link.ui.inline.SignUpConsentAction
import com.stripe.android.link.ui.inline.UserInput
import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.model.ConsumerPaymentDetailsUpdateParams
import com.stripe.android.model.ConsumerSession
import com.stripe.android.model.ConsumerSessionLookup
import com.stripe.android.model.ConsumerSessionSignup
import com.stripe.android.model.PaymentMethodCreateParams
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

@SuppressWarnings("TooManyFunctions")
internal interface LinkAccountManager {
    val linkAccount: StateFlow<LinkAccount?>
    val accountStatus: Flow<AccountStatus>
    var consumerPublishableKey: String?

    /**
     * Retrieves the Link account associated with the email if it exists.
     *
     * Optionally starts a user session, by storing the cookie for the account and starting a
     * verification if needed.
     *
     * When the [email] parameter is null, will try to fetch the account for the currently stored
     * cookie.
     */
    suspend fun lookupConsumer(
        email: String,
        startSession: Boolean = true,
    ): Result<LinkAccount?>

    /**
     * Registers the user for a new Link account.
     */
    suspend fun signUp(
        email: String,
        phone: String,
        country: String,
        name: String?,
        consentAction: SignUpConsentAction
    ): Result<LinkAccount>

    suspend fun mobileSignUp(
        email: String,
        phoneNumber: String,
        country: String,
        verificationToken: String,
        appId: String,
        name: String?,
        consentAction: SignUpConsentAction
    ): Result<LinkAccount>

    /**
     * Use the user input in memory to sign in to an existing account or sign up for a new Link
     * account, starting verification if needed.
     */
    suspend fun signInWithUserInput(
        userInput: UserInput
    ): Result<LinkAccount>

    suspend fun logOut(): Result<ConsumerSession>

    suspend fun createCardPaymentDetails(
        paymentMethodCreateParams: PaymentMethodCreateParams
    ): Result<LinkPaymentDetails>

    fun setLinkAccountFromLookupResult(
        lookup: ConsumerSessionLookup,
        startSession: Boolean,
    ): LinkAccount?

    /**
     * Triggers sending a verification code to the user.
     */
    suspend fun startVerification(): Result<LinkAccount>

    /**
     * Confirms a verification code sent to the user.
     */
    suspend fun confirmVerification(code: String): Result<LinkAccount>

    /**
     * Fetch all saved payment methods for the signed in consumer.
     */
    suspend fun listPaymentDetails(paymentMethodTypes: Set<String>): Result<ConsumerPaymentDetails>

    /**
     * Delete the payment method from the signed in consumer account.
     */
    suspend fun deletePaymentDetails(paymentDetailsId: String): Result<Unit>

    /**
     * Update an existing payment method in the signed in consumer account.
     */
    suspend fun updatePaymentDetails(updateParams: ConsumerPaymentDetailsUpdateParams): Result<ConsumerPaymentDetails>
}
