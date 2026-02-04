package com.stripe.android.crypto.onramp

import com.google.common.truth.Truth.assertThat
import com.stripe.android.crypto.onramp.analytics.OnrampAnalyticsEvent
import com.stripe.android.crypto.onramp.analytics.OnrampAnalyticsService
import com.stripe.android.crypto.onramp.exception.MissingConsumerSecretException
import com.stripe.android.crypto.onramp.exception.MissingCryptoCustomerException
import com.stripe.android.crypto.onramp.model.CreatePaymentTokenResponse
import com.stripe.android.crypto.onramp.model.CryptoCustomerResponse
import com.stripe.android.crypto.onramp.model.CryptoNetwork
import com.stripe.android.crypto.onramp.model.GetPlatformSettingsResponse
import com.stripe.android.crypto.onramp.model.KycInfo
import com.stripe.android.crypto.onramp.model.KycRetrieveResponse
import com.stripe.android.crypto.onramp.model.LinkUserInfo
import com.stripe.android.crypto.onramp.model.OnrampAttachKycInfoResult
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
import com.stripe.android.crypto.onramp.model.OnrampVerifyKycInfoResult
import com.stripe.android.crypto.onramp.model.RefreshKycInfo
import com.stripe.android.crypto.onramp.model.StartIdentityVerificationResponse
import com.stripe.android.crypto.onramp.repositories.CryptoApiRepository
import com.stripe.android.crypto.onramp.ui.KycRefreshScreenAction
import com.stripe.android.crypto.onramp.ui.VerifyKycActivityResult
import com.stripe.android.identity.IdentityVerificationSheet.VerificationFlowResult
import com.stripe.android.link.LinkController
import com.stripe.android.link.LinkController.ConfigureResult
import com.stripe.android.model.DateOfBirth
import com.stripe.android.paymentsheet.PaymentSheet
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
    private val testAnalyticsService = TestOnrampAnalyticsService()
    private val analyticsServiceFactory: OnrampAnalyticsService.Factory = mock {
        on { create(any()) } doReturn testAnalyticsService
    }

    private val interactor: OnrampInteractor = OnrampInteractor(
        application = RuntimeEnvironment.getApplication(),
        linkController = linkController,
        cryptoApiRepository = cryptoApiRepository,
        analyticsServiceFactory = analyticsServiceFactory
    )

    @Test
    fun testConfigureIsSuccessful() = runTest {
        whenever(linkController.configure(any())).thenReturn(ConfigureResult.Success)

        val result = interactor.configure(createConfigurationState())

        assert(result is OnrampConfigurationResult.Completed)
    }

    @Test
    fun testHasLinkAccountIsSuccessful() = runTest {
        val successResult = mock<LinkController.LookupConsumerResult.Success> {
            on { email } doReturn "test@email.com"
            on { isConsumer } doReturn true
        }

        whenever(linkController.lookupConsumer(any())).thenReturn(successResult)

        interactor.onLinkControllerState(mockLinkStateWithAccount())

        val result = interactor.hasLinkAccount("test@email.com")

        assert(result is OnrampHasLinkAccountResult.Completed)

        val completed = result as OnrampHasLinkAccountResult.Completed
        assertThat(completed.hasLinkAccount).isTrue()
        verify(linkController).lookupConsumer("test@email.com")

        testAnalyticsService.assertContainsEvent(
            OnrampAnalyticsEvent.LinkAccountLookupCompleted(hasLinkAccount = true)
        )
    }

    @Test
    fun testRegisterLinkUserIsSuccessful() = runTest {
        whenever(linkController.registerConsumer(any(), any(), any(), any())).thenReturn(
            LinkController.RegisterConsumerResult.Success
        )
        whenever(linkController.state(any())).thenReturn(MutableStateFlow(mockLinkStateWithAccount()))
        val permissionsResult = CryptoCustomerResponse(id = "customer_123")
        whenever(cryptoApiRepository.createCryptoCustomer(any()))
            .thenReturn(Result.success(permissionsResult))

        interactor.onLinkControllerState(mockLinkStateWithAccount())

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

        testAnalyticsService.assertContainsEvent(OnrampAnalyticsEvent.LinkRegistrationCompleted)
    }

    @Test
    fun testUpdatePhoneNumberIsSuccessful() = runTest {
        whenever(linkController.updatePhoneNumber(any())).thenReturn(LinkController.UpdatePhoneNumberResult.Success)

        interactor.onLinkControllerState(mockLinkStateWithAccount())

        val result = interactor.updatePhoneNumber("+1234567890")

        assert(result is OnrampUpdatePhoneNumberResult.Completed)
        verify(linkController).updatePhoneNumber("+1234567890")

        testAnalyticsService.assertContainsEvent(OnrampAnalyticsEvent.LinkPhoneNumberUpdated)
    }

    @Test
    fun testRegisterWalletAddressIsSuccessful() = runTest {
        whenever(linkController.state(any())).thenReturn(MutableStateFlow(mockLinkStateWithAccount()))
        whenever(cryptoApiRepository.setWalletAddress(any(), any(), any()))
            .thenReturn(Result.success(Unit))

        interactor.onLinkControllerState(mockLinkStateWithAccount())

        val result = interactor.registerWalletAddress(
            walletAddress = "0x1234567890abcdef",
            network = CryptoNetwork.Ethereum
        )

        assert(result is OnrampRegisterWalletAddressResult.Completed)

        testAnalyticsService.assertContainsEvent(
            OnrampAnalyticsEvent.WalletRegistered(CryptoNetwork.Ethereum)
        )
    }

    @Test
    fun testAttachKycInfoIsSuccessful() = runTest {
        whenever(linkController.state(any())).thenReturn(MutableStateFlow(mockLinkStateWithAccount()))
        whenever(cryptoApiRepository.collectKycData(any(), any()))
            .thenReturn(Result.success(Unit))

        interactor.onLinkControllerState(mockLinkStateWithAccount())

        val kycInfo = KycInfo(
            firstName = "Test",
            lastName = "User",
            idNumber = "999-88-7777",
            dateOfBirth = DateOfBirth(day = 1, month = 3, year = 1975),
            address = PaymentSheet.Address(city = "Orlando", state = "FL")
        )
        val result = interactor.attachKycInfo(kycInfo)
        assert(result is OnrampAttachKycInfoResult.Completed)

        testAnalyticsService.assertContainsEvent(OnrampAnalyticsEvent.KycInfoSubmitted)
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

        interactor.onLinkControllerState(mockLinkStateWithAccount())

        val result = interactor.startIdentityVerification()
        assert(result is OnrampStartVerificationResult.Completed)

        testAnalyticsService.assertContainsEvent(OnrampAnalyticsEvent.IdentityVerificationStarted)
    }

    @Test
    fun testCreateCryptoPaymentTokenIsSuccessful() = runTest {
        whenever(linkController.state(any())).thenReturn(MutableStateFlow(mockLinkStateWithAccount()))
        interactor.onLinkControllerState(mockLinkStateWithAccount())

        whenever(linkController.configure(any())).thenReturn(ConfigureResult.Success)
        interactor.configure(createConfigurationState())
        interactor.updateCryptoCustomerId("cpt_123")

        val mockPlatformSettings = mock<GetPlatformSettingsResponse>()
        doReturn("pk_platform_123").whenever(mockPlatformSettings).publishableKey
        whenever(
            cryptoApiRepository.getPlatformSettings(
                cryptoCustomerId = eq("cpt_123"),
                countryHint = anyOrNull()
            )
        ).thenReturn(Result.success(mockPlatformSettings))

        val mockPaymentMethod = createCardPaymentMethod()

        val mockResult = mock<LinkController.CreatePaymentMethodResult.Success> {
            on { paymentMethod } doReturn mockPaymentMethod
        }
        whenever(linkController.createPaymentMethodForOnramp(any())).thenReturn(mockResult)

        val createPaymentTokenResponse = CreatePaymentTokenResponse(id = "crypto_token_123")
        whenever(
            cryptoApiRepository.createPaymentToken(cryptoCustomerId = any(), paymentMethod = any())
        ).thenReturn(Result.success(createPaymentTokenResponse))

        val result = interactor.createCryptoPaymentToken()
        assertThat(result).isInstanceOf(OnrampCreateCryptoPaymentTokenResult.Completed::class.java)

        testAnalyticsService.assertContainsEvent(OnrampAnalyticsEvent.CryptoPaymentTokenCreated(null))
    }

    @Test
    fun testLogOutIsSuccessful() = runTest {
        val mockLogOutSuccess = mock<LinkController.LogOutResult.Success>()
        whenever(linkController.logOut()).thenReturn(mockLogOutSuccess)

        interactor.onLinkControllerState(mockLinkStateWithAccount())

        val result = interactor.logOut()
        assert(result is OnrampLogOutResult.Completed)

        testAnalyticsService.assertContainsEvent(OnrampAnalyticsEvent.LinkLogout)
    }

    @Test
    fun testHandleAuthorizeResultConsented() = runTest {
        whenever(linkController.state(any())).thenReturn(MutableStateFlow(mockLinkStateWithAccount()))
        val permissionsResult = CryptoCustomerResponse(id = "customer_123")
        whenever(cryptoApiRepository.createCryptoCustomer(any()))
            .thenReturn(Result.success(permissionsResult))

        interactor.onLinkControllerState(mockLinkStateWithAccount())

        val result = interactor.handleAuthorizeResult(LinkController.AuthorizeResult.Consented)
        assert(result is OnrampAuthorizeResult.Consented)

        testAnalyticsService.assertContainsEvent(
            OnrampAnalyticsEvent.LinkAuthorizationCompleted(consented = true)
        )
    }

    @Test
    fun testHandleIdentityVerificationResultCompleted() {
        interactor.onLinkControllerState(mockLinkStateWithAccount())

        val result = interactor.handleIdentityVerificationResult(
            VerificationFlowResult.Completed
        )

        assert(result is OnrampVerifyIdentityResult.Completed)

        testAnalyticsService.assertContainsEvent(OnrampAnalyticsEvent.IdentityVerificationCompleted)
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

        interactor.onLinkControllerState(mockLinkStateWithAccount())

        val result = interactor.handlePresentPaymentMethodsResult(
            LinkController.PresentPaymentMethodsResult.Success,
            context
        )
        assert(result is OnrampCollectPaymentMethodResult.Completed)

        testAnalyticsService.assertContainsEvent(OnrampAnalyticsEvent.CollectPaymentMethodCompleted(null))
    }

    @Test
    fun testOnAuthorize() {
        interactor.onLinkControllerState(mockLinkStateWithAccount())

        interactor.onAuthorize()

        testAnalyticsService.assertContainsEvent(OnrampAnalyticsEvent.LinkAuthorizationStarted)
    }

    @Test
    fun testOnHandleNextActionError() = runTest {
        val error = RuntimeException("Payment failed")
        interactor.onLinkControllerState(mockLinkStateWithAccount())

        interactor.onHandleNextActionError(error)

        testAnalyticsService.assertContainsEvent(
            OnrampAnalyticsEvent.ErrorOccurred(
                operation = OnrampAnalyticsEvent.ErrorOccurred.Operation.PerformCheckout,
                error = error
            )
        )
    }

    @Test
    fun testAttachKycInfoFailsMissingSecret() = runTest {
        whenever(
            linkController.state(any())
        ).thenReturn(
            MutableStateFlow(mockLinkStateWithAccount(mockLinkAccountWithoutSecret()))
        )

        val kycInfo = KycInfo(
            firstName = "A",
            lastName = "B",
            idNumber = "111-22-3333",
            dateOfBirth = DateOfBirth(1, 1, 1990),
            address = PaymentSheet.Address(city = "City")
        )

        val result = interactor.attachKycInfo(kycInfo)

        assertThat(result).isInstanceOf(OnrampAttachKycInfoResult.Failed::class.java)
        val failed = result as OnrampAttachKycInfoResult.Failed
        assertThat(failed.error).isInstanceOf(MissingConsumerSecretException::class.java)
    }

    @Test
    fun testRegisterWalletAddressFailsMissingSecret() = runTest {
        whenever(
            linkController.state(any())
        ).thenReturn(
            MutableStateFlow(mockLinkStateWithAccount(mockLinkAccountWithoutSecret()))
        )

        val result = interactor.registerWalletAddress(
            walletAddress = "0xabc",
            network = CryptoNetwork.Bitcoin
        )

        assertThat(result).isInstanceOf(OnrampRegisterWalletAddressResult.Failed::class.java)
        val failed = result as OnrampRegisterWalletAddressResult.Failed
        assertThat(failed.error).isInstanceOf(MissingConsumerSecretException::class.java)
    }

    @Test
    fun testStartKycVerificationIsSuccessful() = runTest {
        whenever(linkController.state(any())).thenReturn(MutableStateFlow(mockLinkStateWithAccount()))

        val response = KycRetrieveResponse(
            firstName = "Test",
            lastName = "User",
            idNumberLastFour = "7777",
            idType = "SOCIAL_SECURITY_NUMBER",
            dateOfBirth = DateOfBirth(1, 1, 1990),
            address = PaymentSheet.Address(city = "City")
        )

        whenever(
            cryptoApiRepository.retrieveKycInfo(
                consumerSessionClientSecret = any()
            )
        ).thenReturn(Result.success(response))

        interactor.onLinkControllerState(mockLinkStateWithAccount())

        val result = interactor.startKycVerification(null)
        assert(result is OnrampStartKycVerificationResult.Completed)

        testAnalyticsService.assertContainsEvent(OnrampAnalyticsEvent.KycVerificationStarted)
    }

    @Test
    fun testHandleVerifyKycResultConfirmedSuccess() = runTest {
        interactor.onLinkControllerState(mockLinkStateWithAccount())

        val refreshInfo = RefreshKycInfo(
            firstName = "Jane",
            lastName = "Doe",
            idNumberLastFour = "9999",
            idType = "SOCIAL_SECURITY_NUMBER",
            dateOfBirth = DateOfBirth(2, 2, 1980),
            address = PaymentSheet.Address(city = "Tampa")
        )
        whenever(
            cryptoApiRepository.refreshKycData(
                kycInfo = any(),
                consumerSessionClientSecret = any()
            )
        ).thenReturn(Result.success(Unit))

        val result = interactor.handleVerifyKycResult(
            VerifyKycActivityResult(
                KycRefreshScreenAction.Confirm(info = refreshInfo)
            )
        )

        assertThat(result).isInstanceOf(OnrampVerifyKycInfoResult.Confirmed::class.java)
        testAnalyticsService.assertContainsEvent(OnrampAnalyticsEvent.KycVerificationCompleted)
    }

    @Test
    fun testHandleVerifyKycResultConfirmedFailure() = runTest {
        interactor.onLinkControllerState(mockLinkStateWithAccount())

        val error = RuntimeException("refresh failed")
        whenever(
            cryptoApiRepository.refreshKycData(
                kycInfo = any(),
                consumerSessionClientSecret = any()
            )
        ).thenReturn(Result.failure(error))

        val refreshInfo = RefreshKycInfo(
            firstName = "Jane",
            lastName = "Doe",
            idNumberLastFour = "9999",
            idType = "SOCIAL_SECURITY_NUMBER",
            dateOfBirth = DateOfBirth(2, 2, 1980),
            address = PaymentSheet.Address(city = "Tampa")
        )

        val result = interactor.handleVerifyKycResult(
            VerifyKycActivityResult(
                KycRefreshScreenAction.Confirm(info = refreshInfo)
            )
        )

        assertThat(result).isInstanceOf(OnrampVerifyKycInfoResult.Failed::class.java)
        val failed = result as OnrampVerifyKycInfoResult.Failed
        assertThat(failed.error.message).contains("refresh failed")
    }

    @Test
    fun testHandleVerifyKycResultConfirmedMissingSecret() = runTest {
        whenever(
            linkController.state(any())
        ).thenReturn(
            MutableStateFlow(mockLinkStateWithAccount(mockLinkAccountWithoutSecret()))
        )

        val refreshInfo = RefreshKycInfo(
            firstName = "Jane",
            lastName = "Doe",
            idNumberLastFour = "9999",
            idType = "SOCIAL_SECURITY_NUMBER",
            dateOfBirth = DateOfBirth(2, 2, 1980),
            address = PaymentSheet.Address(city = "Tampa")
        )

        val result = interactor.handleVerifyKycResult(
            VerifyKycActivityResult(
                KycRefreshScreenAction.Confirm(info = refreshInfo)
            )
        )

        assertThat(result).isInstanceOf(OnrampVerifyKycInfoResult.Failed::class.java)
        val failed = result as OnrampVerifyKycInfoResult.Failed
        assertThat(failed.error).isInstanceOf(MissingConsumerSecretException::class.java)
    }

    @Test
    fun testCreateCryptoPaymentTokenFailsMissingCryptoCustomer() = runTest {
        interactor.onLinkControllerState(mockLinkStateWithAccount())

        val result = interactor.createCryptoPaymentToken()

        assertThat(result).isInstanceOf(OnrampCreateCryptoPaymentTokenResult.Failed::class.java)
        val failed = result as OnrampCreateCryptoPaymentTokenResult.Failed
        assertThat(failed.error).isInstanceOf(MissingCryptoCustomerException::class.java)
    }

    private fun mockLinkStateWithAccount(
        account: LinkController.LinkAccount = mockLinkAccount()
    ): LinkController.State = LinkController.State(
        internalLinkAccount = account,
        merchantLogoUrl = null,
        selectedPaymentMethodPreview = null,
        createdPaymentMethod = null,
        elementsSessionId = "test-elements-session-id"
    )

    private fun mockLinkAccount(): LinkController.LinkAccount = LinkController.LinkAccount(
        email = "test@email.com",
        redactedPhoneNumber = "***-***-1234",
        sessionState = LinkController.SessionState.LoggedIn,
        consumerSessionClientSecret = "secret_123"
    )

    private fun mockLinkAccountWithoutSecret(): LinkController.LinkAccount = LinkController.LinkAccount(
        email = "test@email.com",
        redactedPhoneNumber = "***-***-1234",
        sessionState = LinkController.SessionState.LoggedIn,
        consumerSessionClientSecret = null
    )

    private fun createConfigurationState(): OnrampConfiguration.State =
        OnrampConfiguration()
            .merchantDisplayName("merchant-display-name")
            .publishableKey("pk_test_12345")
            .appearance(mock())
            .build()
}
