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
import com.stripe.android.model.ConsumerSessionRefresh
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
     * Suggested email from the last lookup, when the user may have made a typo.
     */
    val suggestedEmail: StateFlow<String?>

    /**
     * Retrieves the Link account associated with the email if it exists.
     *
     * Optionally starts a user session, by storing the cookie for the account and starting a
     * verification if needed.
     *
     * @param customerId Optional customer ID to associate with the lookup. When provided, enables
     *                   retrieval of displayable payment details.
     */
    suspend fun lookupByEmail(
        email: String,
        emailSource: EmailSource,
        startSession: Boolean = true,
        customerId: String?
    ): Result<LinkAccount?>

    /**
     * Lookup consumer by auth intent ID.
     *
     * @param customerId Optional customer ID to associate with the lookup. When provided, enables
     *                   retrieval of displayable payment details.
     */
    suspend fun lookupByLinkAuthIntent(
        linkAuthIntentId: String?,
        customerId: String?
    ): Result<LinkAccount?>

    suspend fun lookupByLinkAuthTokenClientSecret(
        linkAuthTokenClientSecret: String
    ): Result<LinkAccount?>

    /**
     * Refresh the mobile consumer session.
     */
    suspend fun refreshConsumer(): Result<ConsumerSessionRefresh>

    /**
     * Registers the user for a new Link account.
     *
     * @param email The email for the new account
     * @param phoneNumber The phone number for the new account
     * @param country The country code
     * @param countryInferringMethod The method used to infer the country
     * @param name Optional name for the new account
     * @param consentAction The consent action taken by the user
     */
    suspend fun signUp(
        email: String,
        phoneNumber: String?,
        country: String?,
        countryInferringMethod: String,
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
        allowRedisplay: String? = null,
        apiKey: String? = null,
    ): Result<SharePaymentDetails>

    suspend fun createLinkAccountSession(): Result<LinkAccountSession>

    /**
     * Triggers sending a verification code to the user.
     */
    suspend fun startVerification(isResendSmsCode: Boolean = false): Result<LinkAccount>

    /**
     * Confirms a verification code sent to the user.
     */
    suspend fun confirmVerification(code: String, consentGranted: Boolean?): Result<LinkAccount>

    /**
     * Update consent status for the current Link account.
     */
    suspend fun postConsentUpdate(consentGranted: Boolean): Result<Unit>

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

    /**
     * Update the phone number for the signed in consumer.
     */
    suspend fun updatePhoneNumber(phoneNumber: String): Result<LinkAccount>
}

internal val LinkAccountManager.consumerPublishableKey: String?
    get() = linkAccountInfo.value.account?.consumerPublishableKey
