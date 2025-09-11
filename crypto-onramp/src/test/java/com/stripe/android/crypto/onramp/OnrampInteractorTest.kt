package com.stripe.android.crypto.onramp

import com.google.common.truth.Truth.assertThat
import com.stripe.android.crypto.onramp.model.CreatePaymentTokenResponse
import com.stripe.android.crypto.onramp.model.CryptoCustomerResponse
import com.stripe.android.crypto.onramp.model.CryptoNetwork
import com.stripe.android.crypto.onramp.model.GetPlatformSettingsResponse
import com.stripe.android.crypto.onramp.model.KycInfo
import com.stripe.android.crypto.onramp.model.LinkUserInfo
import com.stripe.android.crypto.onramp.model.OnrampAttachKycInfoResult
import com.stripe.android.crypto.onramp.model.OnrampAuthenticateResult
import com.stripe.android.crypto.onramp.model.OnrampAuthorizeResult
import com.stripe.android.crypto.onramp.model.OnrampCollectPaymentMethodResult
import com.stripe.android.crypto.onramp.model.OnrampConfiguration
import com.stripe.android.crypto.onramp.model.OnrampConfigurationResult
import com.stripe.android.crypto.onramp.model.OnrampCreateCryptoPaymentTokenResult
import com.stripe.android.crypto.onramp.model.OnrampHasLinkAccountResult
import com.stripe.android.crypto.onramp.model.OnrampLogOutResult
import com.stripe.android.crypto.onramp.model.OnrampRegisterLinkUserResult
import com.stripe.android.crypto.onramp.model.OnrampRegisterWalletAddressResult
import com.stripe.android.crypto.onramp.model.OnrampStartVerificationResult
import com.stripe.android.crypto.onramp.model.OnrampUpdatePhoneNumberResult
import com.stripe.android.crypto.onramp.model.OnrampVerifyIdentityResult
import com.stripe.android.crypto.onramp.model.StartIdentityVerificationResponse
import com.stripe.android.crypto.onramp.repositories.CryptoApiRepository
import com.stripe.android.identity.IdentityVerificationSheet.VerificationFlowResult
import com.stripe.android.link.LinkController
import com.stripe.android.link.LinkController.ConfigureResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class OnrampInteractorTest {
    private val linkController: LinkController = mock()
    private val cryptoApiRepository: CryptoApiRepository = mock()

    private val interactor: OnrampInteractor = OnrampInteractor(
        application = RuntimeEnvironment.getApplication(),
        linkController = linkController,
        cryptoApiRepository = cryptoApiRepository
    )

    @Test
    fun testConfigureIsSuccessful() = runTest {
        whenever(linkController.configure(any())).thenReturn(ConfigureResult.Success)

        val result = interactor.configure(
            OnrampConfiguration(
                merchantDisplayName = "merchant-display-name",
                publishableKey = "pk_test_12345",
                stripeAccountId = "acct_12345",
                appearance = mock()
            )
        )

        assert(result is OnrampConfigurationResult.Completed)
    }

    @Test
    fun testHasLinkAccountIsSuccessful() = runTest {
        val successResult = mock<LinkController.LookupConsumerResult.Success> {
            on { email } doReturn "test@email.com"
            on { isConsumer } doReturn true
        }

        whenever(linkController.lookupConsumer(any())).thenReturn(successResult)

        val result = interactor.hasLinkAccount("test@email.com")

        assert(result is OnrampHasLinkAccountResult.Completed)

        val completed = result as OnrampHasLinkAccountResult.Completed
        assertThat(completed.hasLinkAccount).isTrue()
        verify(linkController).lookupConsumer("test@email.com")
    }

    @Test
    fun testRegisterLinkUserIsSuccessful() = runTest {
        whenever(linkController.registerConsumer(any(), any(), any(), any())).thenReturn(
            LinkController.RegisterConsumerResult.Success
        )
        whenever(linkController.state(any())).thenReturn(MutableStateFlow(mockLinkStateWithAccount()))
        val permissionsResult = CryptoCustomerResponse(id = "customer_123")
        whenever(cryptoApiRepository.grantPartnerMerchantPermissions(any()))
            .thenReturn(Result.success(permissionsResult))
        val result = interactor.registerLinkUser(
            LinkUserInfo(
                email = "email",
                phone = "phone",
                country = "US",
                fullName = "Test User"
            )
        )
        assert(result is OnrampRegisterLinkUserResult.Completed)
        verify(linkController).registerConsumer("email", "phone", "US", "Test User")
    }

    @Test
    fun testUpdatePhoneNumberIsSuccessful() = runTest {
        whenever(linkController.updatePhoneNumber(any())).thenReturn(LinkController.UpdatePhoneNumberResult.Success)
        val result = interactor.updatePhoneNumber("+1234567890")

        assert(result is OnrampUpdatePhoneNumberResult.Completed)
        verify(linkController).updatePhoneNumber("+1234567890")
    }

    @Test
    fun testRegisterWalletAddressIsSuccessful() = runTest {
        whenever(linkController.state(any())).thenReturn(MutableStateFlow(mockLinkStateWithAccount()))
        whenever(cryptoApiRepository.setWalletAddress(any(), any(), any()))
            .thenReturn(Result.success(Unit))

        val result = interactor.registerWalletAddress(
            walletAddress = "0x1234567890abcdef",
            network = CryptoNetwork.Ethereum
        )

        assert(result is OnrampRegisterWalletAddressResult.Completed)
    }

    @Test
    fun testAttachKycInfoIsSuccessful() = runTest {
        whenever(linkController.state(any())).thenReturn(MutableStateFlow(mockLinkStateWithAccount()))
        whenever(cryptoApiRepository.collectKycData(any(), any()))
            .thenReturn(Result.success(Unit))
        val kycInfo = KycInfo(
            firstName = "Test",
            lastName = "User",
            idNumber = "999-88-7777",
            dateOfBirth = com.stripe.android.crypto.onramp.model.DateOfBirth(day = 1, month = 3, year = 1975),
            address = com.stripe.android.paymentsheet.PaymentSheet.Address(city = "Orlando", state = "FL")
        )
        val result = interactor.attachKycInfo(kycInfo)
        assert(result is OnrampAttachKycInfoResult.Completed)
    }

    @Test
    fun testStartIdentityVerificationIsSuccessful() = runTest {
        whenever(linkController.state(any())).thenReturn(MutableStateFlow(mockLinkStateWithAccount()))
        val response = StartIdentityVerificationResponse(
            id = "id_123",
            url = "https://stripe.com",
            ephemeralKey = "ek_test"
        )
        whenever(cryptoApiRepository.startIdentityVerification(any()))
            .thenReturn(Result.success(response))
        val result = interactor.startIdentityVerification()
        assert(result is OnrampStartVerificationResult.Completed)
    }

    @Test
    fun testCreateCryptoPaymentTokenIsSuccessful() = runTest {
        whenever(linkController.state(any())).thenReturn(MutableStateFlow(mockLinkStateWithAccount()))
        interactor.onLinkControllerState(mockLinkStateWithAccount())

        val mockPlatformSettings = mock<GetPlatformSettingsResponse>()
        doReturn("pk_platform_123").whenever(mockPlatformSettings).publishableKey
        whenever(cryptoApiRepository.getPlatformSettings(eq("secret_123"), anyOrNull()))
            .thenReturn(Result.success(mockPlatformSettings))

        val mockPaymentMethod = createCardPaymentMethod()

        val mockResult = mock<LinkController.CreatePaymentMethodResult.Success> {
            on { paymentMethod } doReturn mockPaymentMethod
        }
        whenever(linkController.createPaymentMethodForOnramp(any())).thenReturn(mockResult)

        val createPaymentTokenResponse = CreatePaymentTokenResponse(id = "crypto_token_123")
        whenever(
            cryptoApiRepository.createPaymentToken(any(), any())
        ).thenReturn(Result.success(createPaymentTokenResponse))

        val result = interactor.createCryptoPaymentToken()
        assert(result is OnrampCreateCryptoPaymentTokenResult.Completed)
    }

    @Test
    fun testLogOutIsSuccessful() = runTest {
        val mockLogOutSuccess = mock<LinkController.LogOutResult.Success>()
        whenever(linkController.logOut()).thenReturn(mockLogOutSuccess)

        val result = interactor.logOut()
        assert(result is OnrampLogOutResult.Completed)
    }

    @Test
    fun testHandleAuthenticationResultSuccess() = runTest {
        whenever(linkController.state(any())).thenReturn(MutableStateFlow(mockLinkStateWithAccount()))
        val permissionsResult = CryptoCustomerResponse(id = "customer_123")
        whenever(cryptoApiRepository.grantPartnerMerchantPermissions(any()))
            .thenReturn(Result.success(permissionsResult))

        val result = interactor.handleAuthenticationResult(LinkController.AuthenticationResult.Success)
        assert(result is OnrampAuthenticateResult.Completed)
    }

    @Test
    fun testHandleAuthorizeResultConsented() = runTest {
        whenever(linkController.state(any())).thenReturn(MutableStateFlow(mockLinkStateWithAccount()))
        val permissionsResult = CryptoCustomerResponse(id = "customer_123")
        whenever(cryptoApiRepository.grantPartnerMerchantPermissions(any()))
            .thenReturn(Result.success(permissionsResult))

        val result = interactor.handleAuthorizeResult(LinkController.AuthorizeResult.Consented)
        assert(result is OnrampAuthorizeResult.Consented)
    }

    @Test
    fun testHandleIdentityVerificationResultCompleted() {
        val result = interactor.handleIdentityVerificationResult(
            VerificationFlowResult.Completed
        )

        assert(result is OnrampVerifyIdentityResult.Completed)
    }

    @Test
    fun testHandlePresentPaymentMethodsResultSuccess() {
        val context = RuntimeEnvironment.getApplication()
        val paymentMethodPreview = LinkController.PaymentMethodPreview(
            iconRes = 1,
            label = "Visa",
            sublabel = "•••• 4242"
        )
        val mockState = LinkController.State(
            internalLinkAccount = null,
            merchantLogoUrl = null,
            selectedPaymentMethodPreview = paymentMethodPreview,
            createdPaymentMethod = null
        )
        whenever(linkController.state(any())).thenReturn(MutableStateFlow(mockState))

        val result = interactor.handlePresentPaymentMethodsResult(
            LinkController.PresentPaymentMethodsResult.Success,
            context
        )
        assert(result is OnrampCollectPaymentMethodResult.Completed)
    }

    private fun mockLinkAccount(): LinkController.LinkAccount = LinkController.LinkAccount(
        email = "test@email.com",
        redactedPhoneNumber = "***-***-1234",
        sessionState = LinkController.SessionState.LoggedIn,
        consumerSessionClientSecret = "secret_123"
    )

    private fun mockLinkStateWithAccount(): LinkController.State = LinkController.State(
        internalLinkAccount = mockLinkAccount(),
        merchantLogoUrl = null,
        selectedPaymentMethodPreview = null,
        createdPaymentMethod = null
    )
}
