package com.stripe.android.link.account

import com.stripe.android.link.ConsumerState
import com.stripe.android.link.LinkAccountUpdate
import com.stripe.android.link.LinkPaymentDetails
import com.stripe.android.link.LinkPaymentMethod
import com.stripe.android.link.model.AccountStatus
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.link.ui.inline.SignUpConsentAction
import com.stripe.android.link.ui.inline.UserInput
import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.model.ConsumerPaymentDetailsUpdateParams
import com.stripe.android.model.ConsumerSession
import com.stripe.android.model.ConsumerSessionLookup
import com.stripe.android.model.ConsumerShippingAddresses
import com.stripe.android.model.EmailSource
import com.stripe.android.model.LinkAccountSession
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.SharePaymentDetails
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

@SuppressWarnings("TooManyFunctions")
internal interface LinkAccountManager {
    val linkAccountInfo: StateFlow<LinkAccountUpdate.Value>
    val accountStatus: Flow<AccountStatus>

    /**
     * Cached payment details for the current Link account.
     * [listPaymentDetails] calls will refresh this value.
     * [updatePaymentDetails] calls will refresh the edited payment details on the list.
     */
    val consumerState: StateFlow<ConsumerState?>

    /**
     * Cached shipping addresses for the current Link account.
     */
    var cachedShippingAddresses: ConsumerShippingAddresses?

    /**
     * Retrieves the Link account associated with the email if it exists.
     *
     * Optionally starts a user session, by storing the cookie for the account and starting a
     * verification if needed.
     *
     * @param customerId Optional customer ID to associate with the lookup. When provided, enables
     *                   retrieval of displayable payment details.
     */
    suspend fun lookupConsumer(
        email: String,
        startSession: Boolean = true,
        customerId: String?
    ): Result<LinkAccount?>

    /**
     * Retrieves the Link account associated with the email if it exists
     *
     * Optionally starts a user session, by storing the cookie for the account and starting a
     * verification if needed.
     *
     * @param customerId Optional customer ID to associate with the lookup. When provided, enables
     *                   retrieval of displayable payment details.
     */
    suspend fun mobileLookupConsumer(
        email: String,
        emailSource: EmailSource,
        verificationToken: String,
        appId: String,
        startSession: Boolean,
        customerId: String?
    ): Result<LinkAccount?>

    /**
     * Registers the user for a new Link account.
     */
    suspend fun signUp(
        email: String,
        phone: String?,
        country: String?,
        name: String?,
        consentAction: SignUpConsentAction
    ): Result<LinkAccount>

    /**
     * Registers the user for a new Link account.
     */
    suspend fun mobileSignUp(
        email: String,
        phone: String,
        country: String,
        name: String?,
        verificationToken: String,
        appId: String,
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

    suspend fun createPaymentMethod(
        linkPaymentMethod: LinkPaymentMethod
    ): Result<PaymentMethod>

    suspend fun createCardPaymentDetails(
        paymentMethodCreateParams: PaymentMethodCreateParams
    ): Result<LinkPaymentDetails.New>

    suspend fun createBankAccountPaymentDetails(
        bankAccountId: String,
    ): Result<ConsumerPaymentDetails.PaymentDetails>

    suspend fun shareCardPaymentDetails(
        cardPaymentDetails: LinkPaymentDetails.New,
    ): Result<LinkPaymentDetails.Saved>

    suspend fun sharePaymentDetails(
        paymentDetailsId: String,
        expectedPaymentMethodType: String,
        billingPhone: String?,
        cvc: String?,
    ): Result<SharePaymentDetails>

    suspend fun setLinkAccountFromLookupResult(
        lookup: ConsumerSessionLookup,
        startSession: Boolean,
    ): LinkAccount?

    suspend fun createLinkAccountSession(): Result<LinkAccountSession>

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
     * Fetch all shipping addresses for the signed in consumer.
     */
    suspend fun listShippingAddresses(): Result<ConsumerShippingAddresses>

    /**
     * Delete the payment method from the signed in consumer account.
     */
    suspend fun deletePaymentDetails(paymentDetailsId: String): Result<Unit>

    /**
     * Update an existing payment method in the signed in consumer account.
     */
    suspend fun updatePaymentDetails(
        updateParams: ConsumerPaymentDetailsUpdateParams,
        phone: String? = null
    ): Result<ConsumerPaymentDetails>
}

internal val LinkAccountManager.consumerPublishableKey: String?
    get() = linkAccountInfo.value.account?.consumerPublishableKey
