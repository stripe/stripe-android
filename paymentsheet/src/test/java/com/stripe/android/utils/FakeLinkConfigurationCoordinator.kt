package com.stripe.android.utils

import com.stripe.android.core.model.CountryCode
import com.stripe.android.link.LinkConfiguration
import com.stripe.android.link.LinkConfigurationCoordinator
import com.stripe.android.link.LinkPaymentDetails
import com.stripe.android.link.attestation.FakeLinkAttestationCheck
import com.stripe.android.link.attestation.LinkAttestationCheck
import com.stripe.android.link.gate.FakeLinkGate
import com.stripe.android.link.gate.LinkGate
import com.stripe.android.link.injection.LinkComponent
import com.stripe.android.link.model.AccountStatus
import com.stripe.android.link.ui.inline.UserInput
import com.stripe.android.model.CardBrand
import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.model.ConsumerSession
import com.stripe.android.model.CvcCheck
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.uicore.utils.stateFlowOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import org.mockito.kotlin.mock

internal class FakeLinkConfigurationCoordinator(
    private val attachNewCardToAccountResult: Result<LinkPaymentDetails> = Result.success(
        LinkPaymentDetails.New(
            paymentDetails = ConsumerPaymentDetails.Card(
                id = "pm_123",
                last4 = "4242",
                expiryYear = 2024,
                expiryMonth = 4,
                brand = CardBrand.DinersClub,
                cvcCheck = CvcCheck.Fail,
                isDefault = false,
                networks = emptyList(),
                funding = "CREDIT",
                nickname = null,
                billingAddress = ConsumerPaymentDetails.BillingAddress(
                    name = null,
                    line1 = null,
                    line2 = null,
                    locality = null,
                    administrativeArea = null,
                    countryCode = CountryCode.US,
                    postalCode = "42424"
                )
            ),
            paymentMethodCreateParams = mock(),
            originalParams = mock(),
        )
    ),
    private val accountStatus: AccountStatus = AccountStatus.SignedOut,
    private val linkGate: LinkGate = FakeLinkGate(),
    private val linkAttestationCheck: LinkAttestationCheck = FakeLinkAttestationCheck(),
    private val email: String? = null,
    private val component: LinkComponent = mock()
) : LinkConfigurationCoordinator {

    override val emailFlow: StateFlow<String?>
        get() = stateFlowOf(email)

    override fun getComponent(configuration: LinkConfiguration): LinkComponent {
        return component
    }

    override fun getAccountStatusFlow(configuration: LinkConfiguration): Flow<AccountStatus> {
        return flowOf(accountStatus)
    }

    override fun linkGate(configuration: LinkConfiguration): LinkGate {
        return linkGate
    }

    override fun linkAttestationCheck(configuration: LinkConfiguration): LinkAttestationCheck {
        return linkAttestationCheck
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
                redactedPhoneNumber = "+1********55",
                redactedFormattedPhoneNumber = "(***) *** **55",
            )
        )
    }
}
