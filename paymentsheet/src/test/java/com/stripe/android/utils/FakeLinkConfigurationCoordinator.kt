package com.stripe.android.utils

import com.stripe.android.link.LinkConfiguration
import com.stripe.android.link.LinkConfigurationCoordinator
import com.stripe.android.link.LinkPaymentDetails
import com.stripe.android.link.injection.LinkComponent
import com.stripe.android.link.model.AccountStatus
import com.stripe.android.link.ui.inline.UserInput
import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.model.ConsumerSession
import com.stripe.android.model.PaymentMethodCreateParams
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.mockito.kotlin.mock

class FakeLinkConfigurationCoordinator(
    private val attachNewCardToAccountResult: Result<LinkPaymentDetails> = Result.success(
        LinkPaymentDetails.New(
            paymentDetails = ConsumerPaymentDetails.Card(
                id = "pm_123",
                last4 = "4242",
            ),
            paymentMethodCreateParams = mock(),
            originalParams = mock(),
        )
    ),
    private val accountStatus: AccountStatus = AccountStatus.SignedOut,
) : LinkConfigurationCoordinator {

    override val component: LinkComponent?
        get() = mock()

    override val emailFlow: Flow<String?>
        get() = flowOf(null)

    override fun getAccountStatusFlow(configuration: LinkConfiguration): Flow<AccountStatus> {
        return flowOf(accountStatus)
    }

    override suspend fun signInWithUserInput(configuration: LinkConfiguration, userInput: UserInput): Result<Boolean> {
        return Result.success(true)
    }

    override suspend fun attachNewCardToAccount(
        configuration: LinkConfiguration,
        paymentMethodCreateParams: PaymentMethodCreateParams
    ): Result<LinkPaymentDetails> {
        return attachNewCardToAccountResult
    }

    override suspend fun logOut(configuration: LinkConfiguration): Result<ConsumerSession> {
        return Result.success(
            ConsumerSession(
                emailAddress = "email@email.com",
                redactedPhoneNumber = "+15555555555",
            )
        )
    }
}
