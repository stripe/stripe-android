package com.stripe.android.utils

import com.stripe.android.link.LinkConfiguration
import com.stripe.android.link.LinkConfigurationCoordinator
import com.stripe.android.link.LinkPaymentDetails
import com.stripe.android.link.injection.LinkComponent
import com.stripe.android.link.model.AccountStatus
import com.stripe.android.link.ui.inline.UserInput
import com.stripe.android.model.CardBrand
import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.model.CvcCheck
import com.stripe.android.model.PaymentMethodCreateParams
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.mockito.kotlin.mock

class FakeLinkConfigurationCoordinator : LinkConfigurationCoordinator {

    override val component: LinkComponent?
        get() = mock()

    override val emailFlow: Flow<String?>
        get() = flowOf(null)

    override fun getAccountStatusFlow(configuration: LinkConfiguration): Flow<AccountStatus> {
        return flowOf(AccountStatus.SignedOut)
    }

    override suspend fun signInWithUserInput(configuration: LinkConfiguration, userInput: UserInput): Result<Boolean> {
        return Result.success(true)
    }

    override suspend fun attachNewCardToAccount(
        configuration: LinkConfiguration,
        paymentMethodCreateParams: PaymentMethodCreateParams
    ): Result<LinkPaymentDetails> {
        return Result.success(
            LinkPaymentDetails.New(
                paymentDetails = ConsumerPaymentDetails.Card(
                    id = "pm_123",
                    expiryYear = 2050,
                    expiryMonth = 4,
                    brand = CardBrand.Visa,
                    last4 = "4242",
                    cvcCheck = CvcCheck.Pass,
                ),
                paymentMethodCreateParams = mock(),
                originalParams = mock(),
            )
        )
    }
}
